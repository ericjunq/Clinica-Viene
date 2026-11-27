import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PerguntaAnamnese, TipoTermo } from '../models/cadastro.model';
import { ModeloAnamnese, Termo } from '../models/configuracao.model';
import { AreaAtuacao } from '../models/usuario.model';

/** Termos e modelos de anamnese — só o MASTER (D-089). Rascunho edita; publicado não muda. */
@Injectable({ providedIn: 'root' })
export class ConfiguracaoService {
  private readonly http = inject(HttpClient);
  private readonly termosApi = '/api/v1/termos';
  private readonly modelosApi = '/api/v1/modelos-anamnese';

  termos(): Observable<Termo[]> {
    return this.http.get<Termo[]>(this.termosApi);
  }

  criarTermo(tipo: TipoTermo, titulo: string, texto: string): Observable<Termo> {
    return this.http.post<Termo>(this.termosApi, { tipo, titulo, texto });
  }

  editarTermo(id: string, titulo: string, texto: string): Observable<Termo> {
    return this.http.put<Termo>(`${this.termosApi}/${id}`, { titulo, texto });
  }

  publicarTermo(id: string): Observable<Termo> {
    return this.http.post<Termo>(`${this.termosApi}/${id}/publicar`, null);
  }

  carregarTermosExemplo(): Observable<Termo[]> {
    return this.http.post<Termo[]>(`${this.termosApi}/exemplos`, null);
  }

  modelos(): Observable<ModeloAnamnese[]> {
    return this.http.get<ModeloAnamnese[]>(this.modelosApi);
  }

  criarModelo(
    areaAtuacao: AreaAtuacao,
    titulo: string,
    perguntas: PerguntaAnamnese[],
  ): Observable<ModeloAnamnese> {
    return this.http.post<ModeloAnamnese>(this.modelosApi, { areaAtuacao, titulo, perguntas });
  }

  editarModelo(
    id: string,
    titulo: string,
    perguntas: PerguntaAnamnese[],
  ): Observable<ModeloAnamnese> {
    return this.http.put<ModeloAnamnese>(`${this.modelosApi}/${id}`, { titulo, perguntas });
  }

  publicarModelo(id: string): Observable<ModeloAnamnese> {
    return this.http.post<ModeloAnamnese>(`${this.modelosApi}/${id}/publicar`, null);
  }

  /** Nova versão em rascunho a partir de outra (é assim que se "altera" um modelo publicado). */
  duplicarModelo(id: string): Observable<ModeloAnamnese> {
    return this.http.post<ModeloAnamnese>(`${this.modelosApi}/${id}/duplicar`, null);
  }

  carregarModeloExemploOdontologia(): Observable<ModeloAnamnese> {
    return this.http.post<ModeloAnamnese>(`${this.modelosApi}/exemplos/odontologia`, null);
  }
}
