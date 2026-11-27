import { enderecoWebSocket, textoDoAviso } from './avisos.service';

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
    expect(
      textoDoAviso({
        tipo: 'PACIENTE_CHEGOU',
        agendamentoId: 'a1',
        pacienteNome: 'Ana Teste',
        consultorioNome: 'Consultório 1',
        inicio: '2026-12-02T12:00:00Z',
        chegadaEm: '2026-12-02T11:55:00Z',
      }),
    ).toBe('Paciente chegou: Ana Teste — Consultório 1 (consulta das 09:00)');
  });
});
