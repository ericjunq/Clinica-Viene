import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Agendamento } from '../../core/models/agenda.model';
import { DadosRemarcarArraste, RemarcarArrasteDialog } from './remarcar-arraste.dialog';

const CONSULTA = {
  id: 'a1',
  paciente: { id: 'pa', nomeCompleto: 'Ana Teste', fichaPendente: false },
  profissional: { id: 'p1', nome: 'Dentista Um' },
  consultorio: { id: 'c1', nome: 'Consultório 1' },
  // 02/12/2026 (quarta), 9h–9h30 na clínica
  inicio: '2026-12-02T12:00:00Z',
  fim: '2026-12-02T12:30:00Z',
  status: 'AGENDADO',
} as Agendamento;

describe('RemarcarArrasteDialog', () => {
  function criar(destino: Partial<DadosRemarcarArraste['destino']> = {}) {
    const fechar = vi.fn();
    const dados: DadosRemarcarArraste = {
      agendamento: CONSULTA,
      destino: {
        dia: '2026-12-03',
        minutos: 10 * 60,
        duracaoMinutos: 30,
        profissional: CONSULTA.profissional,
        consultorio: CONSULTA.consultorio,
        ...destino,
      },
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MAT_DIALOG_DATA, useValue: dados },
        { provide: MatDialogRef, useValue: { close: fechar } },
      ],
    });
    const fixture = TestBed.createComponent(RemarcarArrasteDialog);
    fixture.detectChanges();
    return {
      fixture,
      dialogo: fixture.componentInstance,
      http: TestBed.inject(HttpTestingController),
      fechar,
    };
  }

  it('mostra de onde e para onde a consulta vai, na hora da clínica', () => {
    const { fixture } = criar();
    const texto = (id: string) =>
      (fixture.nativeElement as HTMLElement).querySelector(`[data-testid="${id}"]`)?.textContent;
    expect(texto('remarcar-de')).toContain('Qua 02/12 09:00–09:30');
    expect(texto('remarcar-para')).toContain('Qui 03/12 10:00–10:30');
  });

  it('exige o motivo antes de remarcar', () => {
    const { dialogo, http } = criar();
    dialogo['confirmar']();
    expect(dialogo['erro']()).toBe('Informe o motivo da remarcação.');
    http.expectNone('/api/v1/agendamentos/a1/remarcar');
  });

  it('remarca mandando só o que mudou e fecha com a consulta nova', () => {
    const { dialogo, http, fechar } = criar({ consultorio: { id: 'c2', nome: 'Consultório 2' } });
    dialogo['motivo'] = 'Pediu outro dia';
    dialogo['confirmar']();
    const req = http.expectOne('/api/v1/agendamentos/a1/remarcar');
    expect(req.request.body).toEqual({
      inicio: '2026-12-03T13:00:00.000Z',
      duracaoMinutos: null,
      motivo: 'Pediu outro dia',
      profissionalId: null,
      consultorioId: 'c2',
    });
    req.flush({ id: 'a2' });
    expect(fechar).toHaveBeenCalledWith({ id: 'a2' });
  });

  it('mostra a recusa do backend (ex.: conflito) e deixa tentar de novo', () => {
    const { dialogo, http, fechar } = criar();
    dialogo['motivo'] = 'Pediu';
    dialogo['confirmar']();
    http
      .expectOne('/api/v1/agendamentos/a1/remarcar')
      .flush(
        { detail: 'O profissional já tem consulta nesse horário.' },
        { status: 409, statusText: 'Conflict' },
      );
    expect(dialogo['erro']()).toContain('já tem consulta');
    expect(dialogo['ocupado']()).toBe(false);
    expect(fechar).not.toHaveBeenCalled();
  });
});
