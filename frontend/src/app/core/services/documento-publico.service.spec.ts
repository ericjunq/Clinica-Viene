import { HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { mensagemDeErro } from '../models/erro.model';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { DocumentoPublicoService } from './documento-publico.service';

describe('DocumentoPublicoService', () => {
  let servico: DocumentoPublicoService;
  let backend: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    servico = TestBed.inject(DocumentoPublicoService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('manda o token do link em cabeçalho e o CPF no corpo, e recebe o PDF', async () => {
    const download = firstValueFrom(servico.baixar('token-link', '123.456.789-09'));
    const req = backend.expectOne('/api/v1/publico/documentos/baixar');
    expect(req.request.headers.get('X-Entrega-Documento')).toBe('token-link');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.body).toEqual({ cpf: '123.456.789-09' });
    req.flush(new Blob(['%PDF-1.7'], { type: 'application/pdf' }));
    expect((await download).size).toBeGreaterThan(0);
  });

  it('transforma o erro em Blob na mensagem da API', async () => {
    const download = firstValueFrom(servico.baixar('token-link', '1')).catch((e: unknown) => e);
    backend
      .expectOne('/api/v1/publico/documentos/baixar')
      .flush(
        new Blob([
          JSON.stringify({ status: 422, detail: 'O CPF informado não confere com o cadastro.' }),
        ]),
        { status: 422, statusText: 'Unprocessable' },
      );
    const erro = await download;
    expect(erro).toBeInstanceOf(HttpErrorResponse);
    expect(mensagemDeErro(erro)).toBe('O CPF informado não confere com o cadastro.');
  });
});
