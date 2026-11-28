import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject } from 'rxjs';
import { AvisoChegada, AvisosService, enderecoWebSocket, textoDoAviso } from './avisos.service';

const AVISO: AvisoChegada = {
  tipo: 'PACIENTE_CHEGOU',
  agendamentoId: 'a1',
  pacienteNome: 'Ana Teste',
  consultorioNome: 'Consultório 1',
  inicio: '2026-12-02T12:00:00Z',
  chegadaEm: '2026-12-02T11:55:00Z',
};

describe('aviso de chegada — Ciente', () => {
  it('mostra o aviso até "Ciente" e, ao clicar, registra no sistema', () => {
    const acao = new Subject<void>();
    const abrir = vi.fn(() => ({ onAction: () => acao }));
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: MatSnackBar, useValue: { open: abrir } },
      ],
    });
    const servico = TestBed.inject(AvisosService);
    const http = TestBed.inject(HttpTestingController);

    servico['mostrar'](AVISO);
    expect(abrir).toHaveBeenCalledWith(textoDoAviso(AVISO), 'Ciente', expect.anything());
    // nada é registrado só por mostrar
    http.expectNone('/api/v1/agendamentos/a1/aviso-chegada/ciente');

    acao.next();
    const req = http.expectOne('/api/v1/agendamentos/a1/aviso-chegada/ciente');
    expect(req.request.method).toBe('POST');
    req.flush({ id: 'a1' });
    http.verify();
  });
});

describe('avisos em tempo real', () => {
  it('usa ws/wss na mesma origem do app', () => {
    expect(enderecoWebSocket({ protocol: 'http:', host: 'localhost:4200' })).toBe(
      'ws://localhost:4200/api/v1/ws',
    );
    expect(enderecoWebSocket({ protocol: 'https:', host: 'clinica.exemplo' })).toBe(
      'wss://clinica.exemplo/api/v1/ws',
    );
  });

  it('monta o texto do aviso de chegada com a hora da clínica', () => {
    expect(textoDoAviso(AVISO)).toBe(
      'Paciente chegou: Ana Teste — Consultório 1 (consulta das 09:00)',
    );
  });
});
