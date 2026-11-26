import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { sessaoFicticia } from '../services/auth.service.spec';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let auth: AuthService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    auth['iniciarSessao'](sessaoFicticia('token-velho'));
  });

  afterEach(() => backend.verify());

  it('envia o token de acesso nas chamadas da API', async () => {
    const chamada = firstValueFrom(http.get('/api/v1/usuarios'));
    const req = backend.expectOne('/api/v1/usuarios');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-velho');
    req.flush([]);
    await chamada;
  });

  it('não envia token no login nem na renovação', async () => {
    const login = firstValueFrom(http.post('/api/v1/auth/login', {}));
    const req = backend.expectOne('/api/v1/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush({});
    await login;
  });

  it('com 401, renova a sessão e repete a chamada com o token novo', async () => {
    const chamada = firstValueFrom(http.get('/api/v1/usuarios'));
    backend.expectOne('/api/v1/usuarios').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/renovar').flush(sessaoFicticia('token-novo'));
    const repetida = backend.expectOne('/api/v1/usuarios');
    expect(repetida.request.headers.get('Authorization')).toBe('Bearer token-novo');
    repetida.flush([{ id: 'x' }]);

    expect(await chamada).toEqual([{ id: 'x' }]);
  });

  it('se não conseguir renovar, limpa a sessão e volta para o login', async () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const chamada = firstValueFrom(http.get('/api/v1/usuarios')).catch(() => 'falhou');
    backend.expectOne('/api/v1/usuarios').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend
      .expectOne('/api/v1/auth/renovar')
      .flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(await chamada).toBe('falhou');
    expect(auth.autenticado()).toBe(false);
    expect(navegar).toHaveBeenCalledWith(['/login']);
  });
});
