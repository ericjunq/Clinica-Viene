import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { ConfiguracaoService } from './configuracao.service';

describe('ConfiguracaoService', () => {
  let servico: ConfiguracaoService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(ConfiguracaoService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('cria rascunho de termo e publica', async () => {
    const criar = firstValueFrom(servico.criarTermo('POLITICA_PRIVACIDADE', 'Política', 'Texto'));
    const req = http.expectOne('/api/v1/termos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      tipo: 'POLITICA_PRIVACIDADE',
      titulo: 'Política',
      texto: 'Texto',
    });
    req.flush({ id: 't1' });
    await criar;

    const publicar = firstValueFrom(servico.publicarTermo('t1'));
    const pub = http.expectOne('/api/v1/termos/t1/publicar');
    expect(pub.request.method).toBe('POST');
    pub.flush({ id: 't1', status: 'PUBLICADO' });
    expect((await publicar).status).toBe('PUBLICADO');
  });

  it('edita modelo com PUT e cria nova versão duplicando', async () => {
    const editar = firstValueFrom(servico.editarModelo('m1', 'Ficha', []));
    const req = http.expectOne('/api/v1/modelos-anamnese/m1');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ titulo: 'Ficha', perguntas: [] });
    req.flush({});
    await editar;

    const duplicar = firstValueFrom(servico.duplicarModelo('m1'));
    http.expectOne('/api/v1/modelos-anamnese/m1/duplicar').flush({ id: 'm2' });
    expect((await duplicar).id).toBe('m2');
  });

  it('carrega exemplos de termos e da ficha odontológica', async () => {
    const termos = firstValueFrom(servico.carregarTermosExemplo());
    http.expectOne('/api/v1/termos/exemplos').flush([]);
    await termos;
    const modelo = firstValueFrom(servico.carregarModeloExemploOdontologia());
    http.expectOne('/api/v1/modelos-anamnese/exemplos/odontologia').flush({});
    await modelo;
  });
});
