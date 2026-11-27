import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Agendamento,
  CategoriaProcedimento,
  Consultorio,
  DiaSemana,
  ExcecaoAgenda,
  HorarioFixo,
  HorariosProfissional,
  IntervaloDisponivel,
  ItemAgendamento,
  NovoAgendamento,
  Profissional,
  StatusAgendamento,
  TipoExcecao,
} from '../models/agenda.model';
import { AreaAtuacao } from '../models/usuario.model';

/** Agenda: consultórios, categorias, horários dos profissionais e agendamentos. Regras no backend. */
@Injectable({ providedIn: 'root' })
export class AgendaService {
  private readonly http = inject(HttpClient);

  // --- configuração ---

  consultorios(): Observable<Consultorio[]> {
    return this.http.get<Consultorio[]>('/api/v1/consultorios');
  }

  criarConsultorio(nome: string): Observable<Consultorio> {
    return this.http.post<Consultorio>('/api/v1/consultorios', { nome });
  }

  atualizarConsultorio(id: string, nome: string, ativo: boolean): Observable<Consultorio> {
    return this.http.put<Consultorio>(`/api/v1/consultorios/${id}`, { nome, ativo });
  }

  categorias(area?: AreaAtuacao): Observable<CategoriaProcedimento[]> {
    const params = area ? new HttpParams().set('area', area) : undefined;
    return this.http.get<CategoriaProcedimento[]>('/api/v1/categorias-procedimento', { params });
  }

  criarCategoria(
    areaAtuacao: AreaAtuacao,
    nome: string,
    codigo: string | null,
  ): Observable<CategoriaProcedimento> {
    return this.http.post<CategoriaProcedimento>('/api/v1/categorias-procedimento', {
      areaAtuacao,
      nome,
      codigo,
    });
  }

  atualizarCategoria(id: string, nome: string, ativa: boolean): Observable<CategoriaProcedimento> {
    return this.http.put<CategoriaProcedimento>(`/api/v1/categorias-procedimento/${id}`, {
      nome,
      ativa,
    });
  }

  carregarCategoriasOdontologia(): Observable<CategoriaProcedimento[]> {
    return this.http.post<CategoriaProcedimento[]>(
      '/api/v1/categorias-procedimento/exemplos/odontologia',
      null,
    );
  }

  // --- profissionais e horários ---

  profissionais(): Observable<Profissional[]> {
    return this.http.get<Profissional[]>('/api/v1/profissionais');
  }

  horarios(profissionalId: string): Observable<HorariosProfissional> {
    return this.http.get<HorariosProfissional>(`/api/v1/profissionais/${profissionalId}/horarios`);
  }

  criarHorarioFixo(
    profissionalId: string,
    horario: {
      consultorioId: string;
      diaSemana: DiaSemana;
      horaInicio: string;
      horaFim: string;
      vigenciaInicio: string;
      vigenciaFim: string | null;
    },
  ): Observable<HorarioFixo> {
    return this.http.post<HorarioFixo>(
      `/api/v1/profissionais/${profissionalId}/horarios-fixos`,
      horario,
    );
  }

  encerrarHorarioFixo(
    profissionalId: string,
    horarioId: string,
    ultimoDia: string,
  ): Observable<void> {
    return this.http.post<void>(
      `/api/v1/profissionais/${profissionalId}/horarios-fixos/${horarioId}/encerrar`,
      { ultimoDia },
    );
  }

  criarExcecao(
    profissionalId: string,
    excecao: {
      tipo: TipoExcecao;
      consultorioId: string | null;
      inicio: string;
      fim: string;
      motivo: string | null;
    },
  ): Observable<ExcecaoAgenda> {
    return this.http.post<ExcecaoAgenda>(
      `/api/v1/profissionais/${profissionalId}/excecoes`,
      excecao,
    );
  }

  cancelarExcecao(profissionalId: string, excecaoId: string): Observable<void> {
    return this.http.post<void>(
      `/api/v1/profissionais/${profissionalId}/excecoes/${excecaoId}/cancelar`,
      null,
    );
  }

  disponibilidade(
    profissionalId: string,
    de: string,
    ate: string,
  ): Observable<IntervaloDisponivel[]> {
    return this.http.get<IntervaloDisponivel[]>(
      `/api/v1/profissionais/${profissionalId}/disponibilidade`,
      {
        params: new HttpParams().set('de', de).set('ate', ate),
      },
    );
  }

  // --- agendamentos ---

  agendamentos(de: string, ate: string, profissionalId?: string): Observable<Agendamento[]> {
    let params = new HttpParams().set('de', de).set('ate', ate);
    if (profissionalId) {
      params = params.set('profissionalId', profissionalId);
    }
    return this.http.get<Agendamento[]>('/api/v1/agendamentos', { params });
  }

  marcar(agendamento: NovoAgendamento): Observable<Agendamento> {
    return this.http.post<Agendamento>('/api/v1/agendamentos', agendamento);
  }

  alterarItens(
    id: string,
    itens: ItemAgendamento[],
    observacao: string | null,
  ): Observable<Agendamento> {
    return this.http.put<Agendamento>(`/api/v1/agendamentos/${id}/itens`, { itens, observacao });
  }

  mudarStatus(id: string, status: StatusAgendamento): Observable<Agendamento> {
    return this.http.post<Agendamento>(`/api/v1/agendamentos/${id}/status`, { status });
  }

  cancelar(id: string, motivo: string): Observable<Agendamento> {
    return this.http.post<Agendamento>(`/api/v1/agendamentos/${id}/cancelar`, { motivo });
  }

  remarcar(
    id: string,
    pedido: { inicio: string; duracaoMinutos: number | null; motivo: string },
  ): Observable<Agendamento> {
    return this.http.post<Agendamento>(`/api/v1/agendamentos/${id}/remarcar`, pedido);
  }
}
