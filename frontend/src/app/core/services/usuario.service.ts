import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AtualizacaoUsuario,
  NovoUsuario,
  Perfil,
  RegistroProfissional,
  StatusUsuario,
  Usuario,
} from '../models/usuario.model';

/** Gestão de usuários: /api/v1/usuarios. As regras de quem pode o quê ficam no backend. */
@Injectable({ providedIn: 'root' })
export class UsuarioService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/v1/usuarios';

  listar(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.api);
  }

  criar(usuario: NovoUsuario): Observable<Usuario> {
    return this.http.post<Usuario>(this.api, usuario);
  }

  /** Nome, e-mail, telefone e registro profissional (CPF não muda — D-086). */
  atualizar(id: string, dados: AtualizacaoUsuario): Observable<Usuario> {
    return this.http.put<Usuario>(`${this.api}/${id}`, dados);
  }

  /** Muda o perfil; o usuário precisa entrar de novo (D-086). */
  alterarPerfil(id: string, perfil: Perfil, registro: RegistroProfissional): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.api}/${id}/perfil`, { perfil, ...registro });
  }

  alterarStatus(id: string, status: StatusUsuario): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.api}/${id}/status`, { status });
  }
}
