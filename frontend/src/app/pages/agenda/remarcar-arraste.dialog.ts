import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Agendamento } from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AgendaService } from '../../core/services/agenda.service';
import {
  dataNaClinica,
  formatarHora,
  instanteDaClinica,
  minutosNoDia,
  rotuloDia,
} from './calendario';

/** Para onde a consulta foi arrastada (hora da clínica). */
export interface DestinoArraste {
  dia: string;
  minutos: number;
  duracaoMinutos: number;
  profissional: { id: string; nome: string };
  consultorio: { id: string; nome: string };
}

export interface DadosRemarcarArraste {
  agendamento: Agendamento;
  destino: DestinoArraste;
}

/**
 * Confirmação de "arrastar para remarcar" (D-085, D-110): mostra de onde e para onde a consulta vai e pede o motivo.
 * Só remarca ao confirmar; conflitos e horários fora do atendimento são recusados pelo backend. Devolve a consulta nova.
 */
@Component({
  selector: 'app-remarcar-arraste',
  imports: [FormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  templateUrl: './remarcar-arraste.dialog.html',
  styleUrl: './agenda.scss',
})
export class RemarcarArrasteDialog {
  protected readonly dados = inject<DadosRemarcarArraste>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<RemarcarArrasteDialog, Agendamento>);
  private readonly agendaService = inject(AgendaService);

  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected motivo = '';

  protected readonly de = (() => {
    const a = this.dados.agendamento;
    const dia = dataNaClinica(a.inicio);
    return {
      quando: `${rotuloDia(dia)} ${formatarHora(minutosNoDia(a.inicio, dia))}–${formatarHora(minutosNoDia(a.fim, dia))}`,
      onde: `${a.profissional.nome} · ${a.consultorio.nome}`,
    };
  })();

  protected readonly para = (() => {
    const d = this.dados.destino;
    return {
      quando: `${rotuloDia(d.dia)} ${formatarHora(d.minutos)}–${formatarHora(d.minutos + d.duracaoMinutos)}`,
      onde: `${d.profissional.nome} · ${d.consultorio.nome}`,
    };
  })();

  protected confirmar(): void {
    if (!this.motivo.trim()) {
      this.erro.set('Informe o motivo da remarcação.');
      return;
    }
    const a = this.dados.agendamento;
    const d = this.dados.destino;
    this.ocupado.set(true);
    this.erro.set(null);
    this.agendaService
      .remarcar(a.id, {
        inicio: instanteDaClinica(d.dia, d.minutos).toISOString(),
        duracaoMinutos: null,
        motivo: this.motivo.trim(),
        // só manda o que mudou (ausente = o mesmo)
        profissionalId: d.profissional.id !== a.profissional.id ? d.profissional.id : null,
        consultorioId: d.consultorio.id !== a.consultorio.id ? d.consultorio.id : null,
      })
      .subscribe({
        next: (nova) => this.ref.close(nova),
        error: (e: unknown) => {
          this.ocupado.set(false);
          this.erro.set(mensagemDeErro(e));
        },
      });
  }
}
