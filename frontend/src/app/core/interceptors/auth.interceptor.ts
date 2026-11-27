import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

/**
 * Rotas que não usam o token de acesso: login em 2 passos, as que usam o cookie de renovação e o cadastro
 * público do paciente (que tem tokens próprios — num quiosque, o aparelho pode estar logado pela recepção).
 */
const ROTAS_SEM_TOKEN = [
  '/api/v1/publico/',
  '/api/v1/auth/login',
  '/api/v1/auth/2fa/',
  '/api/v1/auth/renovar',
  '/api/v1/auth/logout',
  '/api/v1/auth/senha/',
];

function comToken(requisicao: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token
    ? requisicao.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : requisicao;
}

/**
 * Coloca o token de acesso nas chamadas à API. Se a API responder 401 (token expirado), renova a sessão
 * uma vez e repete a chamada; se não der para renovar, volta para o login.
 */
export const authInterceptor: HttpInterceptorFn = (requisicao, proximo) => {
  if (
    !requisicao.url.startsWith('/api/') ||
    ROTAS_SEM_TOKEN.some((rota) => requisicao.url.startsWith(rota))
  ) {
    return proximo(requisicao);
  }
  const auth = inject(AuthService);
  const router = inject(Router);
  return proximo(comToken(requisicao, auth.token())).pipe(
    catchError((erro: unknown) => {
      if (!(erro instanceof HttpErrorResponse) || erro.status !== 401) {
        return throwError(() => erro);
      }
      return auth.renovar().pipe(
        switchMap((sessao) => proximo(comToken(requisicao, sessao.tokenAcesso))),
        catchError((falha: unknown) => {
          auth.limparSessao();
          void router.navigate(['/login']);
          return throwError(() => falha);
        }),
      );
    }),
  );
};
