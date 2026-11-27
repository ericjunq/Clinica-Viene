import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import {
  Agendamento,
  CategoriaProcedimento,
  Consultorio,
  DURACAO_PADRAO_MINUTOS,
  Profissional,
} from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { PacienteResumo } from '../../core/models/paciente.model';
import { AgendaService } from '../../core/services/agenda.service';
import { PacienteService } from '../../core/services/paciente.service';
import { formatarHora, instanteDaClinica, minutosDaHora } from './calendario';

export interface DadosNovoAgendamento {
  data: string;
  minutos: number;
  profissionalId: string | null;
  consultorioId: string | null;
  profissionais: Profissional[];
  consultorios: Consultorio[];
}

interface ItemEditavel {
  categoriaId: string;
  descricao: string;
}

/** Marcar consulta (D-106): paciente, profissional, consultório, horário, duração e itens (categoria + descrição). */
@Component({
  selector: 'app-novo-agendamento',
  imports: [
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './novo-agendamento.dialog.html',
  styleUrl: './agenda.scss',
})
export class NovoAgendamentoDialog implements OnInit {
  protected readonly dados = inject<DadosNovoAgendamento>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<NovoAgendamentoDialog, Agendamento>);
  private readonly agenda = inject(AgendaService);
  private readonly pacientes = inject(PacienteService);

  protected readonly duracoes = [15, 30, 45, 60, 75, 90, 120, 150, 180, 240];
  protected readonly ativos = this.dados.consultorios.filter((c) => c.ativo);

  protected busca = '';
  protected readonly resultados = signal<PacienteResumo[]>([]);
  protected readonly buscou = signal(false);
  protected readonly paciente = signal<PacienteResumo | null>(null);

  protected readonly profissionalId = signal<string | null>(this.dados.profissionalId);
  protected consultorioId = this.dados.consultorioId ?? this.ativos[0]?.id ?? '';
  protected data = this.dados.data;
  protected hora = formatarHora(this.dados.minutos);
  protected duracao = DURACAO_PADRAO_MINUTOS;
  protected observacao = '';
  protected itens: ItemEditavel[] = [{ categoriaId: '', descricao: '' }];

  private readonly todasCategorias = signal<CategoriaProcedimento[]>([]);
  /** Categorias ativas da área do profissional escolhido. */
  protected readonly categorias = computed(() => {
    const area = this.dados.profissionais.find((p) => p.id === this.profissionalId())?.areaAtuacao;
    return this.todasCategorias().filter((c) => c.ativa && c.areaAtuacao === area);
  });

  protected readonly erro = signal<string | null>(null);
  protected readonly salvando = signal(false);

  ngOnInit(): void {
    this.agenda.categorias().subscribe({
      next: (lista) => this.todasCategorias.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected pesquisar(): void {
    const texto = this.busca.trim();
    if (texto.length < 2) return;
    this.pacientes.pesquisar(texto).subscribe({
      next: (lista) => {
        this.resultados.set(lista);
        this.buscou.set(true);
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected escolher(p: PacienteResumo): void {
    this.paciente.set(p);
    this.resultados.set([]);
  }

  protected mudouProfissional(id: string): void {
    this.profissionalId.set(id);
    // categorias são da área do profissional: limpa as escolhidas
    this.itens = this.itens.map((i) => ({ ...i, categoriaId: '' }));
  }

  protected adicionarItem(): void {
    this.itens = [...this.itens, { categoriaId: '', descricao: '' }];
  }

  protected removerItem(i: number): void {
    this.itens = this.itens.filter((_, j) => j !== i);
  }

  protected salvar(): void {
    const paciente = this.paciente();
    const profissionalId = this.profissionalId();
    if (!paciente || !profissionalId || !this.consultorioId) {
      this.erro.set('Escolha o paciente, o profissional e o consultório.');
      return;
    }
    if (!this.itens.length || this.itens.some((i) => !i.categoriaId)) {
      this.erro.set('Escolha a categoria de cada item.');
      return;
    }
    this.salvando.set(true);
    this.erro.set(null);
    this.agenda
      .marcar({
        pacienteId: paciente.id,
        profissionalId,
        consultorioId: this.consultorioId,
        inicio: instanteDaClinica(this.data, minutosDaHora(this.hora)).toISOString(),
        duracaoMinutos: this.duracao,
        itens: this.itens.map((i) => ({
          categoriaId: i.categoriaId,
          descricao: i.descricao.trim() || null,
        })),
        observacao: this.observacao.trim() || null,
      })
      .subscribe({
        next: (criado) => this.ref.close(criado),
        error: (e: unknown) => {
          this.salvando.set(false);
          this.erro.set(mensagemDeErro(e));
        },
      });
  }
}
