import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom } from 'rxjs';
import { AgendaService } from './agenda.service';

describe('AgendaService', () => {
  let servico: AgendaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    servico = TestBed.inject(AgendaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lista a agenda do período, com filtro opcional de profissional', async () => {
    const dia = firstValueFrom(servico.agendamentos('2026-12-02', '2026-12-02'));
    const req = http.expectOne((r) => r.url === '/api/v1/agendamentos');
    expect(req.request.params.get('de')).toBe('2026-12-02');
    expect(req.request.params.has('profissionalId')).toBe(false);
    req.flush([]);
    await dia;

    const dele = firstValueFrom(servico.agendamentos('2026-12-02', '2026-12-08', 'p1'));
    http
      .expectOne((r) => r.url === '/api/v1/agendamentos' && r.params.get('profissionalId') === 'p1')
      .flush([]);
    await dele;
  });

  it('marca consulta com itens e duração', async () => {
    const marcar = firstValueFrom(
      servico.marcar({
        pacienteId: 'pa',
        profissionalId: 'pr',
        consultorioId: 'c1',
        inicio: '2026-12-02T12:00:00.000Z',
        duracaoMinutos: 45,
        itens: [
          { categoriaId: 'orto', descricao: 'Manutenção' },
          { categoriaId: 'clinica', descricao: null },
        ],
        observacao: null,
      }),
    );
    const req = http.expectOne('/api/v1/agendamentos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body.itens).toHaveLength(2);
    expect(req.request.body.duracaoMinutos).toBe(45);
    req.flush({ id: 'a1' });
    expect((await marcar).id).toBe('a1');
  });

  it('muda situação, cancela e remarca com motivo', async () => {
    const status = firstValueFrom(servico.mudarStatus('a1', 'PACIENTE_CHEGOU'));
    const r1 = http.expectOne('/api/v1/agendamentos/a1/status');
    expect(r1.request.body).toEqual({ status: 'PACIENTE_CHEGOU' });
    r1.flush({});
    await status;

    const cancelar = firstValueFrom(servico.cancelar('a1', 'Desistiu'));
    const r2 = http.expectOne('/api/v1/agendamentos/a1/cancelar');
    expect(r2.request.body).toEqual({ motivo: 'Desistiu' });
    r2.flush({ status: 'CANCELADO' });
    expect((await cancelar).status).toBe('CANCELADO');

    const remarcar = firstValueFrom(
      servico.remarcar('a1', {
        inicio: '2026-12-03T12:00:00.000Z',
        duracaoMinutos: null,
        motivo: 'Pediu',
      }),
    );
    const r3 = http.expectOne('/api/v1/agendamentos/a1/remarcar');
    expect(r3.request.body.motivo).toBe('Pediu');
    r3.flush({ id: 'a2' });
    expect((await remarcar).id).toBe('a2');
  });

  it('busca a disponibilidade e cadastra horário extra', async () => {
    const disp = firstValueFrom(servico.disponibilidade('p1', '2026-12-02', '2026-12-02'));
    const req = http.expectOne((r) => r.url === '/api/v1/profissionais/p1/disponibilidade');
    expect(req.request.params.get('ate')).toBe('2026-12-02');
    req.flush([]);
    await disp;

    const extra = firstValueFrom(
      servico.criarExcecao('p1', {
        tipo: 'HORARIO_EXTRA',
        consultorioId: 'c1',
        inicio: '2026-12-03T11:00:00.000Z',
        fim: '2026-12-03T14:00:00.000Z',
        motivo: null,
      }),
    );
    const r2 = http.expectOne('/api/v1/profissionais/p1/excecoes');
    expect(r2.request.body.tipo).toBe('HORARIO_EXTRA');
    r2.flush({ id: 'e1' });
    await extra;
  });
});
