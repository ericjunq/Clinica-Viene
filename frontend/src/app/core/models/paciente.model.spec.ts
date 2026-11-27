import { idadeEm } from './paciente.model';

describe('idadeEm', () => {
  const hoje = new Date(2026, 10, 27); // 27/11/2026

  it('conta anos completos', () => {
    expect(idadeEm('2008-11-27', hoje)).toBe(18);
    expect(idadeEm('2008-11-28', hoje)).toBe(17);
    expect(idadeEm('2008-12-01', hoje)).toBe(17);
    expect(idadeEm('1990-01-15', hoje)).toBe(36);
  });
});
