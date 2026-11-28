import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import {
  Agendamento,
  Consultorio,
  GRADE_MINUTOS,
  IntervaloDisponivel,
  Profissional,
  ROTULO_STATUS,
} from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';
import { AgendaService } from '../../core/services/agenda.service';
import { AvisosService } from '../../core/services/avisos.service';
import {
  cabeEm,
  dataNaClinica,
  diasDaSemana,
  distribuirRaias,
  faixaDoDia,
  formatarHora,
  hojeNaClinica,
  minutosNoDia,
  naGrade,
  posicao,
  rotuloDia,
  somarDias,
} from './calendario';
import { DadosDetalheAgendamento, DetalheAgendamentoDialog } from './detalhe-agendamento.dialog';
import { DadosNovoAgendamento, NovoAgendamentoDialog } from './novo-agendamento.dialog';

/** Pixels por minuto na grade (15 min = 24 px). */
const PX_POR_MINUTO = 1.6;

type Visao = 'consultorio' | 'profissional' | 'semana' | 'lista';

interface Trecho {
  inicio: number;
  fim: number;
}

interface Bloco extends Trecho {
  agendamento: Agendamento;
  raia: number;
  raias: number;
  topo: number;
  altura: number;
}

interface Coluna {
  id: string;
  titulo: string;
  /** Dia (aaaa-mm-dd, hora da clínica) mostrado na coluna. */
  dia: string;
  /** Coluna de um consultório (visão por consultório) ou de um profissional (demais visões). */
  consultorioId: string | null;
  profissionalId: string | null;
  /** Trechos (em minutos do dia) em que há atendimento nesta coluna. */
  livres: (Trecho & { topo: number; altura: number })[];
  blocos: Bloco[];
}

/**
 * Agenda (D-085, D-107): o dia com uma coluna por consultório ou por profissional, a semana de um profissional (uma
 * coluna por dia) e a lista do dia. Horários indisponíveis esmaecidos, cor por situação. Clicar num horário abre a
 * marcação; clicar numa consulta abre os detalhes e as ações.
 */
@Component({
  selector: 'app-agenda-page',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './agenda.page.html',
  styleUrls: ['./agenda.scss', './agenda.page.scss'],
})
export class AgendaPage implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly dialogo = inject(MatDialog);
  private readonly avisosTela = inject(MatSnackBar);
  private readonly usuario = inject(AuthService).usuario;
  private readonly avisos = inject(AvisosService);
  private readonly destruir = inject(DestroyRef);

  protected readonly rotuloStatus = ROTULO_STATUS;
  protected readonly gestao = computed(() =>
    ['ADMIN', 'MASTER'].includes(this.usuario()?.perfil ?? ''),
  );
  protected readonly ehProfissional = computed(() => this.usuario()?.perfil === 'PROFISSIONAL');

  protected readonly data = signal(hojeNaClinica());
  protected readonly visao = signal<Visao>('consultorio');
  protected readonly mostrarCancelados = signal(false);
  protected readonly carregando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly hoje = signal(hojeNaClinica());

  private readonly consultorios = signal<Consultorio[]>([]);
  private readonly profissionais = signal<Profissional[]>([]);
  private readonly agendamentos = signal<Agendamento[]>([]);
  private readonly disponibilidade = signal<Record<string, IntervaloDisponivel[]>>({});
  /** Profissional escolhido para a visão da semana (vazio: o primeiro da lista). */
  private readonly escolhidoSemana = signal<string | null>(null);

  /** Profissionais mostrados: o profissional logado só vê a própria agenda. */
  protected readonly profissionaisVisiveis = computed(() => {
    const eu = this.usuario();
    return this.ehProfissional()
      ? this.profissionais().filter((p) => p.id === eu?.id)
      : this.profissionais();
  });

  protected readonly profissionalSemana = computed(() => {
    const visiveis = this.profissionaisVisiveis();
    return visiveis.find((p) => p.id === this.escolhidoSemana()) ?? visiveis[0] ?? null;
  });

  protected readonly semana = computed(() => this.visao() === 'semana');

  /** Dias carregados e mostrados: a semana (segunda a domingo) ou só o dia escolhido. */
  protected readonly dias = computed(() =>
    this.semana() ? diasDaSemana(this.data()) : [this.data()],
  );

  protected readonly periodo = computed(() => {
    const dias = this.dias();
    return dias.length > 1 ? `${rotuloDia(dias[0])} a ${rotuloDia(dias[dias.length - 1])}` : '';
  });

  protected readonly faixa = computed(() => {
    const trechos: Trecho[] = [];
    Object.values(this.disponibilidade()).forEach((lista) =>
      lista.forEach((i) => trechos.push(this.trecho(i, this.diaDe(i)))),
    );
    this.agendamentos().forEach((a) => trechos.push(this.trecho(a, this.diaDe(a))));
    return faixaDoDia(trechos);
  });

  protected readonly alturaTotal = computed(
    () => (this.faixa().fim - this.faixa().inicio) * PX_POR_MINUTO,
  );

  protected readonly horas = computed(() => {
    const lista: { rotulo: string; topo: number }[] = [];
    for (let m = this.faixa().inicio; m < this.faixa().fim; m += 60) {
      lista.push({ rotulo: formatarHora(m), topo: (m - this.faixa().inicio) * PX_POR_MINUTO });
    }
    return lista;
  });

  protected readonly colunas = computed<Coluna[]>(() => {
    const visiveis = this.agendamentos().filter(
      (a) => this.mostrarCancelados() || a.status !== 'CANCELADO',
    );
    const disp = this.disponibilidade();
    const dia = this.data();
    if (this.semana()) {
      const p = this.profissionalSemana();
      if (!p) return [];
      return this.dias().map((d) =>
        this.coluna(
          { id: d, titulo: rotuloDia(d), dia: d, consultorioId: null, profissionalId: p.id },
          (disp[p.id] ?? []).filter((i) => dataNaClinica(i.inicio) === d),
          visiveis.filter((a) => a.profissional.id === p.id && dataNaClinica(a.inicio) === d),
        ),
      );
    }
    if (this.visao() === 'consultorio' && !this.ehProfissional()) {
      return this.consultorios()
        .filter((c) => c.ativo)
        .map((c) =>
          this.coluna(
            { id: c.id, titulo: c.nome, dia, consultorioId: c.id, profissionalId: null },
            Object.values(disp).flatMap((l) => l.filter((i) => i.consultorioId === c.id)),
            visiveis.filter((a) => a.consultorio.id === c.id),
          ),
        );
    }
    return this.profissionaisVisiveis().map((p) =>
      this.coluna(
        { id: p.id, titulo: p.nomeCompleto, dia, consultorioId: null, profissionalId: p.id },
        disp[p.id] ?? [],
        visiveis.filter((a) => a.profissional.id === p.id),
      ),
    );
  });

  /** Lista do dia (check-in rápido): consultas em ordem de horário. */
  protected readonly lista = computed(() =>
    this.agendamentos()
      .filter((a) => this.mostrarCancelados() || a.status !== 'CANCELADO')
      .filter((a) => !this.ehProfissional() || a.profissional.id === this.usuario()?.id)
      .sort((a, b) => a.inicio.localeCompare(b.inicio)),
  );

  ngOnInit(): void {
    if (this.ehProfissional()) {
      this.visao.set('profissional');
    }
    // outra recepcionista (ou o profissional) mudou a agenda de um dia mostrado: recarrega
    this.avisos.agendaAlterada
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe((dia) => this.dias().includes(dia) && this.carregarDia());
    forkJoin({
      consultorios: this.agenda.consultorios(),
      profissionais: this.agenda.profissionais(),
    }).subscribe({
      next: ({ consultorios, profissionais }) => {
        this.consultorios.set(consultorios);
        this.profissionais.set(profissionais);
        this.carregarDia();
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected irPara(data: string): void {
    if (!data) return;
    this.data.set(data);
    this.carregarDia();
  }

  /** Anda um dia (ou uma semana, na visão da semana) para trás ou para a frente. */
  protected avancar(passos: number): void {
    this.irPara(somarDias(this.data(), passos * (this.semana() ? 7 : 1)));
  }

  protected irParaHoje(): void {
    this.hoje.set(hojeNaClinica());
    this.irPara(this.hoje());
  }

  protected mudarVisao(visao: Visao): void {
    this.visao.set(visao);
    this.carregarDia();
  }

  protected escolherProfissionalSemana(id: string): void {
    this.escolhidoSemana.set(id);
    this.carregarDia();
  }

  /** Clique num espaço da coluna: abre a marcação no horário (encaixado na grade). */
  protected clicarColuna(coluna: Coluna, evento: MouseEvent): void {
    if (!this.gestao() || (evento.target as HTMLElement).closest('.bloco')) return;
    const alvo = evento.currentTarget as HTMLElement;
    const y = evento.clientY - alvo.getBoundingClientRect().top;
    const minutos = naGrade(this.faixa().inicio + y / PX_POR_MINUTO, GRADE_MINUTOS);
    if (!cabeEm(coluna.livres, minutos, minutos + GRADE_MINUTOS)) {
      this.avisosTela.open(
        'Fora dos horários de atendimento. Cadastre um horário extra para encaixar.',
        'OK',
        {
          duration: 4000,
        },
      );
      return;
    }
    this.abrirMarcacao(coluna, minutos);
  }

  protected abrirMarcacao(coluna: Coluna | null, minutos: number): void {
    const disp = this.disponibilidade();
    const dia = coluna?.dia ?? this.data();
    const atendeNoHorario = (i: IntervaloDisponivel) =>
      cabeEm([this.trecho(i, dia)], minutos, minutos + GRADE_MINUTOS);
    let profissionalId: string | null = null;
    let consultorioId: string | null = null;
    if (coluna?.consultorioId) {
      consultorioId = coluna.consultorioId;
      // se só um profissional atende nesse consultório nesse horário, já vem escolhido
      const atendendo = Object.entries(disp)
        .filter(([, lista]) =>
          lista.some((i) => i.consultorioId === coluna.consultorioId && atendeNoHorario(i)),
        )
        .map(([id]) => id);
      profissionalId = atendendo.length === 1 ? atendendo[0] : null;
    } else if (coluna?.profissionalId) {
      profissionalId = coluna.profissionalId;
      consultorioId = (disp[profissionalId] ?? []).find(atendeNoHorario)?.consultorioId ?? null;
    }
    const dados: DadosNovoAgendamento = {
      data: dia,
      minutos,
      profissionalId,
      consultorioId,
      profissionais: this.profissionais(),
      consultorios: this.consultorios(),
    };
    this.dialogo
      .open<NovoAgendamentoDialog, DadosNovoAgendamento, Agendamento>(NovoAgendamentoDialog, {
        data: dados,
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((criado) => {
        if (criado) {
          this.avisosTela.open('Consulta marcada.', 'OK', { duration: 3000 });
          this.carregarDia();
        }
      });
  }

  protected abrirDetalhe(a: Agendamento): void {
    const dados: DadosDetalheAgendamento = {
      agendamento: a,
      gestao: this.gestao(),
      doProprio: this.ehProfissional() && a.profissional.id === this.usuario()?.id,
    };
    this.dialogo
      .open<DetalheAgendamentoDialog, DadosDetalheAgendamento, Agendamento>(
        DetalheAgendamentoDialog,
        {
          data: dados,
          maxWidth: '95vw',
        },
      )
      .afterClosed()
      .subscribe(() => this.carregarDia());
  }

  /** Check-in em um clique, na lista do dia. */
  protected chegou(a: Agendamento): void {
    this.agenda.mudarStatus(a.id, 'PACIENTE_CHEGOU').subscribe({
      next: () => {
        this.avisos.agendaAlterada.next(dataNaClinica(a.inicio));
        this.avisosTela.open(`Chegada de ${a.paciente.nomeCompleto} registrada.`, 'OK', {
          duration: 3000,
        });
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected podeCheckIn(a: Agendamento): boolean {
    return this.gestao() && (a.status === 'AGENDADO' || a.status === 'CONFIRMADO');
  }

  protected horaDe(a: Agendamento): string {
    const dia = dataNaClinica(a.inicio);
    return `${formatarHora(minutosNoDia(a.inicio, dia))}–${formatarHora(minutosNoDia(a.fim, dia))}`;
  }

  protected carregarDia(): void {
    const dias = this.dias();
    const de = dias[0];
    const ate = dias[dias.length - 1];
    // na semana, só o profissional escolhido; no dia, todos os visíveis
    const doProfissional = this.semana() ? this.profissionalSemana() : null;
    const profissionais = this.semana()
      ? doProfissional
        ? [doProfissional]
        : []
      : this.profissionaisVisiveis();
    if (this.semana() && !doProfissional) {
      this.agendamentos.set([]);
      this.disponibilidade.set({});
      return;
    }
    this.carregando.set(true);
    this.erro.set(null);
    const disponibilidades = profissionais.length
      ? forkJoin(
          Object.fromEntries(
            profissionais.map((p) => [p.id, this.agenda.disponibilidade(p.id, de, ate)]),
          ),
        )
      : of({} as Record<string, IntervaloDisponivel[]>);
    forkJoin({
      agendamentos: this.agenda.agendamentos(de, ate, doProfissional?.id),
      disponibilidade: disponibilidades,
    }).subscribe({
      next: ({ agendamentos, disponibilidade }) => {
        this.agendamentos.set(agendamentos);
        this.disponibilidade.set(disponibilidade);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.carregando.set(false);
      },
    });
  }

  /** Dia de referência para posicionar um trecho: o próprio dia na semana, o dia escolhido no dia. */
  private diaDe(i: { inicio: string }): string {
    return this.semana() ? dataNaClinica(i.inicio) : this.data();
  }

  private trecho(i: { inicio: string; fim: string }, dia: string): Trecho {
    return { inicio: minutosNoDia(i.inicio, dia), fim: minutosNoDia(i.fim, dia) };
  }

  private coluna(
    base: Omit<Coluna, 'livres' | 'blocos'>,
    disponiveis: IntervaloDisponivel[],
    lista: Agendamento[],
  ): Coluna {
    const faixa = this.faixa();
    const livres = disponiveis.map((i) => {
      const t = this.trecho(i, base.dia);
      return { ...t, ...posicao(t.inicio, t.fim, faixa, PX_POR_MINUTO) };
    });
    const blocos = distribuirRaias(
      lista.map((a) => ({ ...this.trecho(a, base.dia), agendamento: a })),
    ).map((b) => ({
      ...b,
      ...posicao(b.inicio, b.fim, faixa, PX_POR_MINUTO),
    }));
    return { ...base, livres, blocos };
  }
}
