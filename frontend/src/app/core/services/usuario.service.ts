import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { NovoUsuario, StatusUsuario, Usuario } from '../models/usuario.model';

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

  alterarStatus(id: string, status: StatusUsuario): Observable<Usuario> {
    return this.http.patch<Usuario>(`${this.api}/${id}/status`, { status });
  }
}
