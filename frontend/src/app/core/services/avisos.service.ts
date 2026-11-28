import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Client } from '@stomp/stompjs';
import { Subject } from 'rxjs';
import { AgendaService } from './agenda.service';
import { AuthService } from './auth.service';

/** Aviso pessoal: o paciente da consulta chegou (D-023). */
export interface AvisoChegada {
  tipo: 'PACIENTE_CHEGOU';
  agendamentoId: string;
  pacienteNome: string;
  consultorioNome: string;
  inicio: string;
  chegadaEm: string;
}

/** Endereço do WebSocket na mesma origem do app (o nginx/proxy encaminha /api ao backend). */
export function enderecoWebSocket(
  local: Pick<Location, 'protocol' | 'host'> = window.location,
): string {
  return `${local.protocol === 'https:' ? 'wss' : 'ws'}://${local.host}/api/v1/ws`;
}

export function textoDoAviso(aviso: AvisoChegada): string {
  const hora = new Date(aviso.inicio).toLocaleTimeString('pt-BR', {
    hour: '2-digit',
    minute: '2-digit',
    timeZone: 'America/Bahia',
  });
  return `Paciente chegou: ${aviso.pacienteNome} — ${aviso.consultorioNome} (consulta das ${hora})`;
}

/**
 * Avisos em tempo real (STOMP sobre WebSocket — D-108). Conecta ao entrar na área logada com o token de acesso
 * atual (a cada reconexão pega o token renovado) e desconecta ao sair.
 */
@Injectable({ providedIn: 'root' })
export class AvisosService {
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(MatSnackBar);
  private readonly agenda = inject(AgendaService);
  private cliente: Client | null = null;

  /** Data (aaaa-mm-dd) de um dia da agenda que mudou: as telas abertas recarregam. */
  readonly agendaAlterada = new Subject<string>();

  conectar(): void {
    const perfil = this.auth.usuario()?.perfil;
    if (this.cliente || !perfil) return;
    const usaAgenda = perfil === 'ADMIN' || perfil === 'MASTER' || perfil === 'PROFISSIONAL';
    const cliente = new Client({
      brokerURL: enderecoWebSocket(),
      reconnectDelay: 5000,
      heartbeatIncoming: 20000,
      heartbeatOutgoing: 20000,
      beforeConnect: async (c) => {
        c.connectHeaders = { Authorization: `Bearer ${this.auth.token() ?? ''}` };
      },
      onConnect: () => {
        cliente.subscribe('/user/queue/avisos', (m) =>
          this.mostrar(JSON.parse(m.body) as AvisoChegada),
        );
        if (usaAgenda) {
          cliente.subscribe('/topic/agenda', (m) =>
            this.agendaAlterada.next((JSON.parse(m.body) as { dia: string }).dia),
          );
        }
      },
    });
    cliente.activate();
    this.cliente = cliente;
  }

  desconectar(): void {
    void this.cliente?.deactivate();
    this.cliente = null;
  }

  /** Mostra o aviso até "Ciente"; ao clicar, registra no sistema que o profissional viu (D-111). */
  private mostrar(aviso: AvisoChegada): void {
    if (aviso.tipo !== 'PACIENTE_CHEGOU') return;
    this.tocarSom();
    this.avisos
      .open(textoDoAviso(aviso), 'Ciente', { panelClass: 'aviso-chegada' })
      .onAction()
      .subscribe(() =>
        // a recepção é avisada pelo próprio backend (agenda alterada); se falhar (sem conexão), o aviso já fechou e
        // só fica sem a confirmação registrada
        this.agenda.avisoChegadaCiente(aviso.agendamentoId).subscribe({ error: () => undefined }),
      );
  }

  /** Dois toques curtos (Web Audio, sem arquivo de som). Navegadores podem bloquear antes da 1ª interação. */
  private tocarSom(): void {
    try {
      const contexto = new AudioContext();
      [0, 0.25].forEach((atraso) => {
        const osc = contexto.createOscillator();
        const ganho = contexto.createGain();
        osc.frequency.value = 880;
        ganho.gain.value = 0.15;
        osc.connect(ganho).connect(contexto.destination);
        osc.start(contexto.currentTime + atraso);
        osc.stop(contexto.currentTime + atraso + 0.15);
      });
    } catch {
      // sem áudio disponível: o aviso visual basta
    }
  }
}
