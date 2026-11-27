import { HttpClient, HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, from, switchMap, throwError } from 'rxjs';

/** Download do documento assinado pelo link enviado ao paciente (/api/v1/publico/documentos — D-094). */
@Injectable({ providedIn: 'root' })
export class DocumentoPublicoService {
  private readonly http = inject(HttpClient);

  baixar(token: string, cpf: string): Observable<Blob> {
    return this.http
      .post(
        '/api/v1/publico/documentos/baixar',
        { cpf },
        { headers: new HttpHeaders({ 'X-Entrega-Documento': token }), responseType: 'blob' },
      )
      .pipe(
        catchError((erro: unknown) =>
          from(comErroLegivel(erro)).pipe(switchMap((e) => throwError(() => e))),
        ),
      );
  }
}

/** Com responseType 'blob', o Problem Details também chega como Blob: converte para JSON. */
export async function comErroLegivel(erro: unknown): Promise<unknown> {
  if (erro instanceof HttpErrorResponse && erro.error instanceof Blob) {
    try {
      return new HttpErrorResponse({
        error: JSON.parse(await erro.error.text()),
        headers: erro.headers,
        status: erro.status,
        statusText: erro.statusText,
        url: erro.url ?? undefined,
      });
    } catch {
      return erro;
    }
  }
  return erro;
}
