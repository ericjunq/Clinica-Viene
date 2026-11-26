import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Perfil } from '../models/usuario.model';
import { AuthService } from '../services/auth.service';

/** Só com sessão aberta. */
export const autenticadoGuard: CanActivateFn = () =>
  inject(AuthService).autenticado() ? true : inject(Router).createUrlTree(['/login']);

/** Só com sessão completa: com troca de senha pendente, vai para a troca de senha. */
export const sessaoCompletaGuard: CanActivateFn = () => {
  const usuario = inject(AuthService).usuario();
  return usuario && !usuario.trocarSenha ? true : inject(Router).createUrlTree(['/trocar-senha']);
};

/** Só durante o login em 2 passos (depois da senha aceita). */
export const desafioGuard: CanActivateFn = () =>
  inject(AuthService).desafio() ? true : inject(Router).createUrlTree(['/login']);

/** Só para os perfis informados (a regra de verdade está no backend; isto só esconde a tela). */
export function perfilGuard(...perfis: Perfil[]): CanActivateFn {
  return () => {
    const usuario = inject(AuthService).usuario();
    return usuario && perfis.includes(usuario.perfil) ? true : inject(Router).createUrlTree(['/']);
  };
}
