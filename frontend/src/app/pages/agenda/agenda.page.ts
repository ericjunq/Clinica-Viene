import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
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
  distribuirRaias,
  faixaDoDia,
  formatarHora,
  hojeNaClinica,
  minutosNoDia,
  naGrade,
  posicao,
  somarDias,
} from './calendario';
import { DadosDetalheAgendamento, DetalheAgendamentoDialog } from './detalhe-agendamento.dialog';
import { DadosNovoAgendamento, NovoAgendamentoDialog } from './novo-agendamento.dialog';

/** Pixels por minuto na grade (15 min = 24 px). */
const PX_POR_MINUTO = 1.6;

type Visao = 'consultorio' | 'profissional' | 'lista';

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
  /** Trechos (em minutos do dia) em que há atendimento nesta coluna. */
  livres: (Trecho & { topo: number; altura: number })[];
  blocos: Bloco[];
}

/**
 * Agenda do dia (D-085): uma coluna por consultório ou por profissional, horários indisponíveis esmaecidos, cor por
 * situação. Clicar num horário abre a marcação; clicar numa consulta abre os detalhes e as ações.
 */
@Component({
  selector: 'app-agenda-page',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatButtonToggleModule,
    MatCheckboxModule,
    MatIconModule,
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

  private readonly consultorios = signal<Consultorio[]>([]);
  private readonly profissionais = signal<Profissional[]>([]);
  private readonly agendamentos = signal<Agendamento[]>([]);
  private readonly disponibilidade = signal<Record<string, IntervaloDisponivel[]>>({});

  /** Profissionais mostrados: o profissional logado só vê a própria agenda. */
  private readonly profissionaisVisiveis = computed(() => {
    const eu = this.usuario();
    return this.ehProfissional()
      ? this.profissionais().filter((p) => p.id === eu?.id)
      : this.profissionais();
  });

  protected readonly faixa = computed(() => {
    const trechos: Trecho[] = [];
    Object.values(this.disponibilidade()).forEach((lista) =>
      lista.forEach((i) => trechos.push(this.trecho(i))),
    );
    this.agendamentos().forEach((a) => trechos.push(this.trecho(a)));
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
    if (this.visao() === 'consultorio' && !this.ehProfissional()) {
      return this.consultorios()
        .filter((c) => c.ativo)
        .map((c) =>
          this.coluna(
            c.id,
            c.nome,
            Object.values(disp).flatMap((l) => l.filter((i) => i.consultorioId === c.id)),
            visiveis.filter((a) => a.consultorio.id === c.id),
          ),
        );
    }
    return this.profissionaisVisiveis().map((p) =>
      this.coluna(
        p.id,
        p.nomeCompleto,
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
    // outra recepcionista (ou o profissional) mudou a agenda deste dia: recarrega
    this.avisos.agendaAlterada
      .pipe(takeUntilDestroyed(this.destruir))
      .subscribe((dia) => dia === this.data() && this.carregarDia());
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

  protected avancar(dias: number): void {
    this.irPara(somarDias(this.data(), dias));
  }

  protected hoje(): void {
    this.irPara(hojeNaClinica());
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
    let profissionalId: string | null = null;
    let consultorioId: string | null = null;
    if (coluna && this.visao() === 'consultorio') {
      consultorioId = coluna.id;
      // se só um profissional atende nesse consultório nesse horário, já vem escolhido
      const atendendo = Object.entries(disp)
        .filter(([, lista]) =>
          lista.some(
            (i) =>
              i.consultorioId === coluna.id &&
              cabeEm([this.trecho(i)], minutos, minutos + GRADE_MINUTOS),
          ),
        )
        .map(([id]) => id);
      profissionalId = atendendo.length === 1 ? atendendo[0] : null;
    } else if (coluna) {
      profissionalId = coluna.id;
      consultorioId =
        (disp[coluna.id] ?? []).find((i) =>
          cabeEm([this.trecho(i)], minutos, minutos + GRADE_MINUTOS),
        )?.consultorioId ?? null;
    }
    const dados: DadosNovoAgendamento = {
      data: this.data(),
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
        this.avisos.agendaAlterada.next(this.data());
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
    return `${formatarHora(minutosNoDia(a.inicio, this.data()))}–${formatarHora(minutosNoDia(a.fim, this.data()))}`;
  }

  protected carregarDia(): void {
    const dia = this.data();
    const profissionais = this.profissionaisVisiveis();
    this.carregando.set(true);
    this.erro.set(null);
    const disponibilidades = profissionais.length
      ? forkJoin(
          Object.fromEntries(
            profissionais.map((p) => [p.id, this.agenda.disponibilidade(p.id, dia, dia)]),
          ),
        )
      : of({} as Record<string, IntervaloDisponivel[]>);
    forkJoin({
      agendamentos: this.agenda.agendamentos(dia, dia),
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

  private trecho(i: { inicio: string; fim: string }): Trecho {
    return { inicio: minutosNoDia(i.inicio, this.data()), fim: minutosNoDia(i.fim, this.data()) };
  }

  private coluna(
    id: string,
    titulo: string,
    disponiveis: IntervaloDisponivel[],
    lista: Agendamento[],
  ): Coluna {
    const faixa = this.faixa();
    const livres = disponiveis.map((i) => {
      const t = this.trecho(i);
      return { ...t, ...posicao(t.inicio, t.fim, faixa, PX_POR_MINUTO) };
    });
    const blocos = distribuirRaias(lista.map((a) => ({ ...this.trecho(a), agendamento: a }))).map(
      (b) => ({
        ...b,
        ...posicao(b.inicio, b.fim, faixa, PX_POR_MINUTO),
      }),
    );
    return { id, titulo, livres, blocos };
  }
}
