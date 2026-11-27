import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import {
  Consultorio,
  DIAS_SEMANA,
  DiaSemana,
  ExcecaoAgenda,
  HorariosProfissional,
  Profissional,
  TipoExcecao,
} from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AgendaService } from '../../core/services/agenda.service';
import { hojeNaClinica, instanteDaClinica, minutosDaHora, somarDias } from './calendario';

/**
 * Horários dos profissionais (D-106), cadastrados pela recepção ou pelo MASTER com o que o profissional passar:
 * fixos semanais, horários extras (brechas fora da rotina) e ausências. Fora deles não se marca consulta.
 */
@Component({
  selector: 'app-horarios-page',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './horarios.page.html',
  styleUrl: './agenda.scss',
})
export class HorariosPage implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly dias = DIAS_SEMANA;
  protected readonly profissionais = signal<Profissional[]>([]);
  protected readonly consultorios = signal<Consultorio[]>([]);
  protected readonly ativos = computed(() => this.consultorios().filter((c) => c.ativo));
  protected readonly horarios = signal<HorariosProfissional | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected profissionalId: string | null = null;

  protected fixo = {
    consultorioId: '',
    diaSemana: 'MONDAY' as DiaSemana,
    horaInicio: '08:00',
    horaFim: '12:00',
    vigenciaInicio: hojeNaClinica(),
    vigenciaFim: '',
  };

  protected excecao = {
    tipo: 'HORARIO_EXTRA' as TipoExcecao,
    consultorioId: '',
    dataInicio: hojeNaClinica(),
    horaInicio: '08:00',
    dataFim: hojeNaClinica(),
    horaFim: '12:00',
    motivo: '',
  };

  ngOnInit(): void {
    this.agenda.profissionais().subscribe({
      next: (lista) => this.profissionais.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
    this.agenda.consultorios().subscribe({
      next: (lista) => {
        this.consultorios.set(lista);
        const primeiro = lista.find((c) => c.ativo)?.id ?? '';
        this.fixo.consultorioId = primeiro;
        this.excecao.consultorioId = primeiro;
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected nomeConsultorio(id: string | null): string {
    if (!id) return 'todos os consultórios';
    return this.consultorios().find((c) => c.id === id)?.nome ?? '—';
  }

  protected rotuloDia(dia: DiaSemana): string {
    return this.dias.find((d) => d.valor === dia)?.rotulo ?? dia;
  }

  protected escolherProfissional(id: string): void {
    this.profissionalId = id;
    this.carregar();
  }

  protected adicionarFixo(): void {
    if (!this.profissionalId) return;
    const f = this.fixo;
    this.executar(
      this.agenda.criarHorarioFixo(this.profissionalId, {
        consultorioId: f.consultorioId,
        diaSemana: f.diaSemana,
        horaInicio: f.horaInicio,
        horaFim: f.horaFim,
        vigenciaInicio: f.vigenciaInicio,
        vigenciaFim: f.vigenciaFim || null,
      }),
      'Horário fixo adicionado.',
    );
  }

  /** Encerra hoje (vale até hoje); se ainda não começou, o backend remove. */
  protected encerrarFixo(horarioId: string, vigenciaInicio: string): void {
    if (!this.profissionalId) return;
    const hoje = hojeNaClinica();
    const ultimoDia = vigenciaInicio > hoje ? somarDias(vigenciaInicio, -1) : hoje;
    if (
      !window.confirm(
        'Encerrar este horário fixo? As consultas já marcadas precisam continuar dentro dos horários.',
      )
    ) {
      return;
    }
    this.executar(
      this.agenda.encerrarHorarioFixo(this.profissionalId, horarioId, ultimoDia),
      'Horário encerrado.',
    );
  }

  protected adicionarExcecao(): void {
    if (!this.profissionalId) return;
    const e = this.excecao;
    const inicio = instanteDaClinica(e.dataInicio, minutosDaHora(e.horaInicio)).toISOString();
    const fim = instanteDaClinica(e.dataFim, minutosDaHora(e.horaFim)).toISOString();
    this.executar(
      this.agenda.criarExcecao(this.profissionalId, {
        tipo: e.tipo,
        consultorioId: e.consultorioId || null,
        inicio,
        fim,
        motivo: e.motivo.trim() || null,
      }),
      e.tipo === 'HORARIO_EXTRA' ? 'Horário extra adicionado.' : 'Ausência registrada.',
    );
  }

  protected cancelarExcecao(ex: ExcecaoAgenda): void {
    if (!this.profissionalId || !window.confirm('Cancelar esta exceção?')) return;
    this.executar(this.agenda.cancelarExcecao(this.profissionalId, ex.id), 'Exceção cancelada.');
  }

  protected mudouTipoExcecao(tipo: TipoExcecao): void {
    this.excecao.tipo = tipo;
    this.excecao.consultorioId = tipo === 'AUSENCIA' ? '' : (this.ativos()[0]?.id ?? '');
  }

  private executar(pedido: Observable<unknown>, sucesso: string): void {
    this.ocupado.set(true);
    this.erro.set(null);
    pedido.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.avisos.open(sucesso, 'OK', { duration: 3000 });
        this.carregar();
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  private carregar(): void {
    if (!this.profissionalId) return;
    this.agenda.horarios(this.profissionalId).subscribe({
      next: (h) => this.horarios.set(h),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
