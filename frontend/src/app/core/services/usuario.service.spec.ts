import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { UsuarioService } from './usuario.service';

describe('UsuarioService', () => {
  let servico: UsuarioService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(UsuarioService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('edita os dados com PUT (sem CPF nem perfil)', async () => {
    const chamada = firstValueFrom(
      servico.atualizar('u1', {
        nomeCompleto: 'Nome',
        email: 'a@exemplo.com',
        telefone: null,
        areaAtuacao: null,
        conselho: null,
        numeroRegistro: null,
        ufRegistro: null,
      }),
    );
    const req = http.expectOne('/api/v1/usuarios/u1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).not.toHaveProperty('cpf');
    expect(req.request.body).not.toHaveProperty('perfil');
    req.flush({});
    await chamada;
  });

  it('muda o perfil com PATCH levando o registro profissional', async () => {
    const chamada = firstValueFrom(
      servico.alterarPerfil('u1', 'PROFISSIONAL', {
        areaAtuacao: 'PSICOLOGIA',
        conselho: 'CRP',
        numeroRegistro: '123',
        ufRegistro: 'BA',
      }),
    );
    const req = http.expectOne('/api/v1/usuarios/u1/perfil');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({
      perfil: 'PROFISSIONAL',
      areaAtuacao: 'PSICOLOGIA',
      conselho: 'CRP',
      numeroRegistro: '123',
      ufRegistro: 'BA',
    });
    req.flush({});
    await chamada;
  });
});
