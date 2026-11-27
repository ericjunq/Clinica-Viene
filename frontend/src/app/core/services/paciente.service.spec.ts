import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { PacienteService } from './paciente.service';

describe('PacienteService', () => {
  let servico: PacienteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(PacienteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pesquisa pelo parâmetro "busca"', async () => {
    const chamada = firstValueFrom(servico.pesquisar('Maria Souza'));
    const req = http.expectOne((r) => r.url === '/api/v1/pacientes');
    expect(req.request.params.get('busca')).toBe('Maria Souza');
    req.flush([]);
    expect(await chamada).toEqual([]);
  });

  it('atualiza só contato e endereço com PATCH', async () => {
    const chamada = firstValueFrom(
      servico.atualizarContato('p1', { email: 'a@exemplo.com', telefone: null, endereco: null }),
    );
    const req = http.expectOne('/api/v1/pacientes/p1/contato');
    expect(req.request.method).toBe('PATCH');
    expect(Object.keys(req.request.body as object).sort()).toEqual([
      'email',
      'endereco',
      'telefone',
    ]);
    req.flush({});
    await chamada;
  });

  it('cria convite com modo, área e conferência do documento', async () => {
    const chamada = firstValueFrom(
      servico.criarConvite('p1', {
        modo: 'QUIOSQUE',
        areaAtuacao: 'ODONTOLOGIA',
        enviarEmail: false,
        documentoConferido: true,
      }),
    );
    const req = http.expectOne('/api/v1/pacientes/p1/convites');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      modo: 'QUIOSQUE',
      areaAtuacao: 'ODONTOLOGIA',
      enviarEmail: false,
      documentoConferido: true,
    });
    req.flush({ id: 'c1', link: 'http://x/cadastro#t' });
    expect((await chamada).id).toBe('c1');
  });

  it('cancela convite e revoga consentimento com POST', async () => {
    const cancelar = firstValueFrom(servico.cancelarConvite('p1', 'c1'));
    http.expectOne('/api/v1/pacientes/p1/convites/c1/cancelar').flush(null);
    await cancelar;

    const revogar = firstValueFrom(
      servico.revogarConsentimento('p1', 'CONSENTIMENTO_IMAGEM', null),
    );
    const req = http.expectOne('/api/v1/pacientes/p1/consentimentos/CONSENTIMENTO_IMAGEM/revogar');
    expect(req.request.body).toEqual({ justificativa: null });
    req.flush(null);
    await revogar;
  });

  it('gera link de entrega do documento sem baixar o PDF', async () => {
    const chamada = firstValueFrom(servico.gerarEntrega('p1', 'd1', true));
    const req = http.expectOne('/api/v1/pacientes/p1/documentos/d1/entregas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ enviarEmail: true });
    expect(req.request.responseType).toBe('json');
    req.flush({
      link: 'http://x/documento#t',
      qrCode: 'data:',
      expiraEm: '',
      emailEnviadoPara: 'a***@x',
    });
    expect((await chamada).emailEnviadoPara).toBe('a***@x');
  });
});
