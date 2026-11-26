import { provideHttpClient } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { sessaoFicticia, usuarioFicticio } from '../services/auth.service.spec';
import { autenticadoGuard, perfilGuard, sessaoCompletaGuard } from './auth.guards';

function executar(guarda: typeof autenticadoGuard): boolean | UrlTree {
  return TestBed.runInInjectionContext(() =>
    guarda({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot),
  ) as boolean | UrlTree;
}

function destino(resultado: boolean | UrlTree): string {
  return resultado instanceof UrlTree ? resultado.toString() : String(resultado);
}

describe('guards de rota', () => {
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient()] });
    auth = TestBed.inject(AuthService);
  });

  it('sem sessão manda para o login', () => {
    expect(destino(executar(autenticadoGuard))).toBe('/login');
  });

  it('com troca de senha pendente manda para a troca de senha', () => {
    auth['iniciarSessao']({ ...sessaoFicticia(), usuario: usuarioFicticio({ trocarSenha: true }) });
    expect(executar(autenticadoGuard)).toBe(true);
    expect(destino(executar(sessaoCompletaGuard))).toBe('/trocar-senha');
  });

  it('libera a tela só para os perfis informados', () => {
    auth['iniciarSessao']({ ...sessaoFicticia(), usuario: usuarioFicticio({ perfil: 'ADMIN' }) });
    expect(destino(executar(perfilGuard('DEV', 'MASTER')))).toBe('/');

    auth['iniciarSessao']({ ...sessaoFicticia(), usuario: usuarioFicticio({ perfil: 'MASTER' }) });
    expect(executar(perfilGuard('DEV', 'MASTER'))).toBe(true);
  });
});
