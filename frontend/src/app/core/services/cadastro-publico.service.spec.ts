import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { AuthService } from './auth.service';
import { sessaoFicticia } from './auth.service.spec';
import { CadastroPublicoService } from './cadastro-publico.service';

describe('CadastroPublicoService', () => {
  let servico: CadastroPublicoService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    servico = TestBed.inject(CadastroPublicoService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('envia o token do convite em cabeçalho, nunca na URL', async () => {
    servico.usarConvite('token-convite');
    const aberto = firstValueFrom(servico.abrir());
    const req = backend.expectOne('/api/v1/publico/cadastro');
    expect(req.request.headers.get('X-Convite-Cadastro')).toBe('token-convite');
    expect(req.request.headers.has('X-Sessao-Cadastro')).toBe(false);
    req.flush({
      modo: 'LINK',
      primeiroNome: 'Ana',
      precisaCodigo: true,
      emailMascarado: 'a***@x.com',
    });
    expect((await aberto).primeiroNome).toBe('Ana');
  });

  it('guarda a sessão confirmada só em memória e a usa nas chamadas seguintes', async () => {
    servico.usarConvite('token-convite');
    const confirmado = firstValueFrom(servico.confirmarCodigo('123456'));
    const req = backend.expectOne('/api/v1/publico/cadastro/codigo/confirmar');
    expect(req.request.body).toEqual({ codigo: '123456' });
    req.flush({ token: 'token-sessao', expiraEm: '2026-01-01T00:00:00Z' });
    await confirmado;
    expect(servico.sessaoAberta()).toBe('token-sessao');

    const formulario = firstValueFrom(servico.formulario());
    const reqFormulario = backend.expectOne('/api/v1/publico/cadastro/formulario');
    expect(reqFormulario.request.headers.get('X-Sessao-Cadastro')).toBe('token-sessao');
    reqFormulario.flush({});
    await formulario;

    servico.encerrar();
    expect(servico.sessaoAberta()).toBeNull();
  });

  it('não manda o token da equipe nem tenta renovar a sessão no fluxo público', async () => {
    TestBed.inject(AuthService)['iniciarSessao'](sessaoFicticia('token-equipe'));
    servico.usarConvite('token-convite');
    const chamada = firstValueFrom(servico.iniciarQuiosque()).catch(() => 'falhou');
    const req = backend.expectOne('/api/v1/publico/cadastro/quiosque');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(await chamada).toBe('falhou');
    backend.expectNone('/api/v1/auth/renovar');
  });
});
