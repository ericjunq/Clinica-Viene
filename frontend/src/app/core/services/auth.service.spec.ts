import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { SessaoResponse } from '../models/auth.model';
import { Usuario } from '../models/usuario.model';
import { AuthService } from './auth.service';

export function usuarioFicticio(parcial: Partial<Usuario> = {}): Usuario {
  return {
    id: 'u1',
    nomeCompleto: 'Ana Teste',
    cpfMascarado: '***.018.159-**',
    email: 'ana@exemplo.com',
    telefone: null,
    perfil: 'MASTER',
    status: 'ATIVO',
    areaAtuacao: null,
    conselho: null,
    numeroRegistro: null,
    ufRegistro: null,
    trocarSenha: false,
    doisFatoresAtivo: true,
    criadoEm: '2026-11-26T00:00:00Z',
    ...parcial,
  };
}

export function sessaoFicticia(token = 'token-1', codigos: string[] | null = null): SessaoResponse {
  return {
    tokenAcesso: token,
    acessoExpiraEm: '2026-11-26T00:15:00Z',
    sessaoExpiraEm: '2026-11-26T12:00:00Z',
    usuario: usuarioFicticio(),
    codigosRecuperacao: codigos,
  };
}

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('guarda o desafio do login e o usa no 2FA', async () => {
    const login = firstValueFrom(auth.login('526.018.159-06', 'Senha#Teste123'));
    http
      .expectOne('/api/v1/auth/login')
      .flush({ desafio: 'd1', etapa: 'VERIFICAR_2FA', expiraEm: 'x' });
    await login;
    expect(auth.desafio()?.etapa).toBe('VERIFICAR_2FA');

    const verificacao = firstValueFrom(auth.verificarDoisFatores('TOTP', '123456'));
    const req = http.expectOne('/api/v1/auth/2fa/verificar');
    expect(req.request.body).toEqual({ desafio: 'd1', metodo: 'TOTP', codigo: '123456' });
    req.flush(sessaoFicticia());
    await verificacao;

    expect(auth.token()).toBe('token-1');
    expect(auth.autenticado()).toBe(true);
    expect(auth.desafio()).toBeNull();
  });

  it('guarda os códigos de recuperação ao confirmar o cadastro do 2FA', async () => {
    const confirmacao = firstValueFrom(auth.confirmarCadastroDoisFatores('123456'));
    http
      .expectOne('/api/v1/auth/2fa/confirmar')
      .flush(sessaoFicticia('t', ['AAAA-BBBB-CCCC-DDDD']));
    await confirmacao;

    expect(auth.codigosRecuperacao()).toEqual(['AAAA-BBBB-CCCC-DDDD']);
  });

  it('renovar compartilha chamadas simultâneas e tenta de novo após 409 (outra aba renovou)', async () => {
    const primeira = firstValueFrom(auth.renovar());
    const segunda = firstValueFrom(auth.renovar());
    http
      .expectOne('/api/v1/auth/renovar')
      .flush({ detail: 'concorrente' }, { status: 409, statusText: 'Conflict' });
    await new Promise((r) => setTimeout(r, 350));
    http.expectOne('/api/v1/auth/renovar').flush(sessaoFicticia('token-2'));

    expect((await primeira).tokenAcesso).toBe('token-2');
    expect((await segunda).tokenAcesso).toBe('token-2');
    expect(auth.token()).toBe('token-2');
  });

  it('restaurar sessão sem cookie segue deslogado', async () => {
    const restaurar = auth.restaurarSessao();
    http
      .expectOne('/api/v1/auth/renovar')
      .flush({ detail: 'x' }, { status: 401, statusText: 'Unauthorized' });
    await restaurar;

    expect(auth.autenticado()).toBe(false);
  });

  it('logout limpa a sessão mesmo se a chamada falhar', async () => {
    auth['iniciarSessao'](sessaoFicticia());
    const saida = firstValueFrom(auth.logout()).catch(() => undefined);
    http.expectOne('/api/v1/auth/logout').flush(null, { status: 500, statusText: 'Erro' });
    await saida;

    expect(auth.token()).toBeNull();
    expect(auth.usuario()).toBeNull();
  });
});
