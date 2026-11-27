import { DatePipe } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { Agendamento, ROTULO_STATUS, StatusAgendamento } from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AgendaService } from '../../core/services/agenda.service';
import {
  dataNaClinica,
  formatarHora,
  instanteDaClinica,
  minutosDaHora,
  minutosNoDia,
} from './calendario';

export interface DadosDetalheAgendamento {
  agendamento: Agendamento;
  /** Recepção/MASTER: confirma, check-in, falta, cancela e remarca. */
  gestao: boolean;
  /** Consulta do próprio profissional logado: inicia e conclui o atendimento. */
  doProprio: boolean;
}

/** Detalhes da consulta e ações conforme a situação e o perfil (D-106). Devolve o agendamento alterado. */
@Component({
  selector: 'app-detalhe-agendamento',
  imports: [
    DatePipe,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './detalhe-agendamento.dialog.html',
  styleUrl: './agenda.scss',
})
export class DetalheAgendamentoDialog {
  protected readonly dados = inject<DadosDetalheAgendamento>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<DetalheAgendamentoDialog, Agendamento>);
  private readonly agendaService = inject(AgendaService);

  protected readonly rotuloStatus = ROTULO_STATUS;
  protected readonly a = signal(this.dados.agendamento);
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  /** Formulário aberto: cancelar ou remarcar. */
  protected readonly modo = signal<'ver' | 'cancelar' | 'remarcar'>('ver');

  protected motivo = '';
  protected novaData = dataNaClinica(this.dados.agendamento.inicio);
  protected novaHora = formatarHora(minutosNoDia(this.dados.agendamento.inicio, this.novaData));
  protected novaDuracao =
    minutosNoDia(this.dados.agendamento.fim, this.novaData) -
    minutosNoDia(this.dados.agendamento.inicio, this.novaData);
  protected readonly duracoes = [15, 30, 45, 60, 75, 90, 120, 150, 180, 240];

  protected pode(
    acao: 'confirmar' | 'chegou' | 'faltou' | 'cancelar' | 'remarcar' | 'iniciar' | 'concluir',
  ): boolean {
    const s = this.a().status;
    const aberto = s === 'AGENDADO' || s === 'CONFIRMADO';
    const atende = this.dados.gestao || this.dados.doProprio;
    switch (acao) {
      case 'confirmar':
        return this.dados.gestao && s === 'AGENDADO';
      case 'chegou':
      case 'faltou':
      case 'remarcar':
        return this.dados.gestao && aberto;
      case 'cancelar':
        return this.dados.gestao && (aberto || s === 'PACIENTE_CHEGOU');
      case 'iniciar':
        return atende && s === 'PACIENTE_CHEGOU';
      case 'concluir':
        return atende && s === 'EM_ATENDIMENTO';
    }
  }

  protected mudar(status: StatusAgendamento): void {
    this.executar(this.agendaService.mudarStatus(this.a().id, status));
  }

  protected cancelar(): void {
    if (!this.motivo.trim()) {
      this.erro.set('Informe o motivo do cancelamento.');
      return;
    }
    this.executar(this.agendaService.cancelar(this.a().id, this.motivo.trim()));
  }

  protected remarcar(): void {
    if (!this.motivo.trim()) {
      this.erro.set('Informe o motivo da remarcação.');
      return;
    }
    this.executar(
      this.agendaService.remarcar(this.a().id, {
        inicio: instanteDaClinica(this.novaData, minutosDaHora(this.novaHora)).toISOString(),
        duracaoMinutos: this.novaDuracao,
        motivo: this.motivo.trim(),
      }),
      true,
    );
  }

  /** Mudanças de situação atualizam o diálogo; remarcar fecha (a consulta nova aparece na agenda). */
  private executar(pedido: Observable<Agendamento>, fechar = false): void {
    this.ocupado.set(true);
    this.erro.set(null);
    pedido.subscribe({
      next: (atualizado) => {
        this.ocupado.set(false);
        if (fechar) {
          this.ref.close(atualizado);
          return;
        }
        this.a.set(atualizado);
        this.modo.set('ver');
        this.motivo = '';
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  protected fechar(): void {
    this.ref.close(this.a());
  }
}
