import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import {
  AssinaturaFicha,
  CodigoEnviado,
  ConviteAberto,
  FichaAssinada,
  FichaCadastro,
  FormularioCadastro,
  RevisaoCadastro,
  SessaoCadastro,
} from '../models/cadastro.model';

/**
 * Cadastro do paciente pelo convite (/api/v1/publico/cadastro — D-100). Os tokens do convite e da sessão
 * ficam só em memória (nada no navegador: o aparelho pode ser o quiosque da clínica) e vão em cabeçalhos.
 */
@Injectable({ providedIn: 'root' })
export class CadastroPublicoService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/v1/publico/cadastro';
  private convite: string | null = null;
  private readonly sessao = signal<string | null>(null);

  readonly sessaoAberta = this.sessao.asReadonly();

  usarConvite(token: string): void {
    this.convite = token;
    this.sessao.set(null);
  }

  /** Esquece os tokens (fim do preenchimento, ou o quiosque volta para a recepção). */
  encerrar(): void {
    this.convite = null;
    this.sessao.set(null);
  }

  abrir(): Observable<ConviteAberto> {
    return this.http.get<ConviteAberto>(this.api, { headers: this.cabecalhos() });
  }

  enviarCodigo(): Observable<CodigoEnviado> {
    return this.http.post<CodigoEnviado>(`${this.api}/codigo`, null, {
      headers: this.cabecalhos(),
    });
  }

  confirmarCodigo(codigo: string): Observable<SessaoCadastro> {
    return this.http
      .post<SessaoCadastro>(
        `${this.api}/codigo/confirmar`,
        { codigo },
        { headers: this.cabecalhos() },
      )
      .pipe(tap((s) => this.sessao.set(s.token)));
  }

  iniciarQuiosque(): Observable<SessaoCadastro> {
    return this.http
      .post<SessaoCadastro>(`${this.api}/quiosque`, null, { headers: this.cabecalhos() })
      .pipe(tap((s) => this.sessao.set(s.token)));
  }

  formulario(): Observable<FormularioCadastro> {
    return this.http.get<FormularioCadastro>(`${this.api}/formulario`, {
      headers: this.cabecalhos(),
    });
  }

  revisar(ficha: FichaCadastro): Observable<RevisaoCadastro> {
    return this.http.post<RevisaoCadastro>(`${this.api}/revisao`, ficha, {
      headers: this.cabecalhos(),
    });
  }

  assinar(ficha: FichaCadastro, assinatura: AssinaturaFicha): Observable<FichaAssinada> {
    return this.http.post<FichaAssinada>(
      `${this.api}/assinatura`,
      { ficha, assinatura, declaracao: true },
      { headers: this.cabecalhos() },
    );
  }

  private cabecalhos(): HttpHeaders {
    let cabecalhos = new HttpHeaders();
    if (this.convite) {
      cabecalhos = cabecalhos.set('X-Convite-Cadastro', this.convite);
    }
    const sessao = this.sessao();
    if (sessao) {
      cabecalhos = cabecalhos.set('X-Sessao-Cadastro', sessao);
    }
    return cabecalhos;
  }
}
