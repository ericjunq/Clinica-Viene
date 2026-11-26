import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import {
  Observable,
  finalize,
  firstValueFrom,
  retry,
  shareReplay,
  switchMap,
  tap,
  throwError,
  timer,
} from 'rxjs';
import {
  CadastroDoisFatoresResponse,
  DesafioAtual,
  EnvioCodigoResponse,
  LoginResponse,
  MensagemResponse,
  MetodoDoisFatores,
  SessaoResponse,
} from '../models/auth.model';
import { Usuario } from '../models/usuario.model';

/**
 * Login em 2 passos e sessão (D-071, D-072).
 *
 * - O token de acesso fica só em memória (nunca em localStorage).
 * - O token de renovação fica num cookie HttpOnly que o JavaScript não lê; o navegador o envia sozinho
 *   para /api/v1/auth/renovar e /api/v1/auth/logout.
 * - Ao abrir/recarregar a página, a sessão é restaurada chamando /renovar.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/v1/auth';

  private tokenAcesso: string | null = null;
  private renovacaoEmAndamento: Observable<SessaoResponse> | null = null;

  readonly usuario = signal<Usuario | null>(null);
  readonly autenticado = computed(() => this.usuario() !== null);
  readonly desafio = signal<DesafioAtual | null>(null);
  /** Códigos de recuperação recém-gerados (mostrados uma única vez, depois descartados). */
  readonly codigosRecuperacao = signal<string[] | null>(null);

  token(): string | null {
    return this.tokenAcesso;
  }

  login(cpf: string, senha: string): Observable<LoginResponse> {
    return this.http
      .post<LoginResponse>(`${this.api}/login`, { cpf, senha })
      .pipe(
        tap((r) => this.desafio.set({ token: r.desafio, etapa: r.etapa, expiraEm: r.expiraEm })),
      );
  }

  iniciarCadastroDoisFatores(): Observable<CadastroDoisFatoresResponse> {
    return this.http.post<CadastroDoisFatoresResponse>(`${this.api}/2fa/cadastro`, {
      desafio: this.tokenDesafio(),
    });
  }

  confirmarCadastroDoisFatores(codigo: string): Observable<SessaoResponse> {
    return this.http
      .post<SessaoResponse>(`${this.api}/2fa/confirmar`, { desafio: this.tokenDesafio(), codigo })
      .pipe(
        tap((sessao) => {
          this.iniciarSessao(sessao);
          this.codigosRecuperacao.set(sessao.codigosRecuperacao);
        }),
      );
  }

  verificarDoisFatores(metodo: MetodoDoisFatores, codigo: string): Observable<SessaoResponse> {
    return this.http
      .post<SessaoResponse>(`${this.api}/2fa/verificar`, {
        desafio: this.tokenDesafio(),
        metodo,
        codigo,
      })
      .pipe(tap((sessao) => this.iniciarSessao(sessao)));
  }

  enviarCodigoEmail(): Observable<EnvioCodigoResponse> {
    return this.http.post<EnvioCodigoResponse>(`${this.api}/2fa/email`, {
      desafio: this.tokenDesafio(),
    });
  }

  /**
   * Renova a sessão com o cookie HttpOnly. Chamadas simultâneas compartilham a mesma renovação; se outra aba
   * acabou de renovar (409), tenta de novo com o cookie atualizado.
   */
  renovar(): Observable<SessaoResponse> {
    if (!this.renovacaoEmAndamento) {
      this.renovacaoEmAndamento = this.http.post<SessaoResponse>(`${this.api}/renovar`, {}).pipe(
        retry({
          count: 2,
          delay: (erro: unknown) =>
            erro instanceof HttpErrorResponse && erro.status === 409
              ? timer(300)
              : throwError(() => erro),
        }),
        tap((sessao) => this.iniciarSessao(sessao)),
        finalize(() => (this.renovacaoEmAndamento = null)),
        shareReplay(1),
      );
    }
    return this.renovacaoEmAndamento;
  }

  /** Ao abrir o app: tenta recuperar a sessão pelo cookie. Sem sessão, segue deslogado. */
  async restaurarSessao(): Promise<void> {
    try {
      await firstValueFrom(this.renovar());
    } catch {
      this.limparSessao();
    }
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.api}/logout`, {}).pipe(finalize(() => this.limparSessao()));
  }

  recarregarUsuario(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.api}/eu`).pipe(tap((u) => this.usuario.set(u)));
  }

  trocarSenha(senhaAtual: string, novaSenha: string): Observable<Usuario> {
    return this.http
      .post<void>('/api/v1/usuarios/eu/senha', { senhaAtual, novaSenha })
      .pipe(switchMap(() => this.recarregarUsuario()));
  }

  recuperarSenha(cpf: string): Observable<MensagemResponse> {
    return this.http.post<MensagemResponse>(`${this.api}/senha/recuperar`, { cpf });
  }

  redefinirSenha(cpf: string, codigo: string, novaSenha: string): Observable<void> {
    return this.http.post<void>(`${this.api}/senha/redefinir`, { cpf, codigo, novaSenha });
  }

  limparSessao(): void {
    this.tokenAcesso = null;
    this.usuario.set(null);
    this.desafio.set(null);
    this.codigosRecuperacao.set(null);
  }

  private iniciarSessao(sessao: SessaoResponse): void {
    this.tokenAcesso = sessao.tokenAcesso;
    this.usuario.set(sessao.usuario);
    this.desafio.set(null);
  }

  private tokenDesafio(): string {
    return this.desafio()?.token ?? '';
  }
}
