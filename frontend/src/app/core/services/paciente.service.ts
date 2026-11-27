import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { TipoTermo } from '../models/cadastro.model';
import {
  Consentimento,
  ContatoPaciente,
  Convite,
  ConviteCriado,
  DocumentoPaciente,
  EntregaCriada,
  NovoConvite,
  NovoPaciente,
  Paciente,
  PacienteResumo,
  PacienteVersao,
} from '../models/paciente.model';

/** Pacientes nas telas internas (recepção e MASTER). As regras de acesso ficam no backend. */
@Injectable({ providedIn: 'root' })
export class PacienteService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/v1/pacientes';

  /** Busca por nome, CPF ou data de nascimento (dd/mm/aaaa). */
  pesquisar(busca: string): Observable<PacienteResumo[]> {
    return this.http.get<PacienteResumo[]>(this.api, {
      params: new HttpParams().set('busca', busca),
    });
  }

  preCadastrar(paciente: NovoPaciente): Observable<Paciente> {
    return this.http.post<Paciente>(this.api, paciente);
  }

  /** Ficha cadastral (leitura auditada no backend). */
  buscar(id: string): Observable<Paciente> {
    return this.http.get<Paciente>(`${this.api}/${id}`);
  }

  /** Só contato e endereço; nome, CPF e nascimento mudam apenas por nova ficha assinada (D-092). */
  atualizarContato(id: string, contato: ContatoPaciente): Observable<Paciente> {
    return this.http.patch<Paciente>(`${this.api}/${id}/contato`, contato);
  }

  historico(id: string): Observable<PacienteVersao[]> {
    return this.http.get<PacienteVersao[]>(`${this.api}/${id}/historico`);
  }

  listarConvites(id: string): Observable<Convite[]> {
    return this.http.get<Convite[]>(`${this.api}/${id}/convites`);
  }

  /** O link e o QR code só aparecem nesta resposta. */
  criarConvite(id: string, convite: NovoConvite): Observable<ConviteCriado> {
    return this.http.post<ConviteCriado>(`${this.api}/${id}/convites`, convite);
  }

  cancelarConvite(id: string, conviteId: string): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/convites/${conviteId}/cancelar`, null);
  }

  consentimentos(id: string): Observable<Consentimento[]> {
    return this.http.get<Consentimento[]>(`${this.api}/${id}/consentimentos`);
  }

  revogarConsentimento(
    id: string,
    tipo: TipoTermo,
    justificativa: string | null,
  ): Observable<void> {
    return this.http.post<void>(`${this.api}/${id}/consentimentos/${tipo}/revogar`, {
      justificativa,
    });
  }

  documentos(id: string): Observable<DocumentoPaciente[]> {
    return this.http.get<DocumentoPaciente[]>(`${this.api}/${id}/documentos`);
  }

  /** Novo link de download (QR na tela; e-mail se pedido). A recepção nunca recebe o PDF. */
  gerarEntrega(id: string, documentoId: string, enviarEmail: boolean): Observable<EntregaCriada> {
    return this.http.post<EntregaCriada>(`${this.api}/${id}/documentos/${documentoId}/entregas`, {
      enviarEmail,
    });
  }
}
