import {
  cabeEm,
  dataNaClinica,
  distribuirRaias,
  faixaDoDia,
  formatarHora,
  hojeNaClinica,
  instanteDaClinica,
  minutosDaHora,
  minutosNoDia,
  naGrade,
  posicao,
  somarDias,
} from './calendario';

describe('calendário da agenda', () => {
  it('usa a hora da clínica (UTC−3), qualquer que seja o fuso do computador', () => {
    expect(instanteDaClinica('2026-12-02', 9 * 60).toISOString()).toBe('2026-12-02T12:00:00.000Z');
    expect(minutosNoDia('2026-12-02T12:00:00Z', '2026-12-02')).toBe(9 * 60);
    // 01:30 UTC do dia 3 ainda é dia 2 (22h30) na clínica
    expect(dataNaClinica('2026-12-03T01:30:00Z')).toBe('2026-12-02');
    expect(minutosNoDia('2026-12-03T01:30:00Z', '2026-12-02')).toBe(22 * 60 + 30);
    expect(hojeNaClinica(new Date('2026-12-03T02:00:00Z'))).toBe('2026-12-02');
  });

  it('soma dias atravessando meses e anos', () => {
    expect(somarDias('2026-12-31', 1)).toBe('2027-01-01');
    expect(somarDias('2026-03-01', -1)).toBe('2026-02-28');
  });

  it('formata e lê horas e encaixa na grade', () => {
    expect(formatarHora(8 * 60 + 5)).toBe('08:05');
    expect(minutosDaHora('17:45')).toBe(17 * 60 + 45);
    expect(naGrade(8 * 60 + 40, 15)).toBe(8 * 60 + 30);
  });

  it('amplia a faixa do dia para caber horários fora do padrão, em horas cheias', () => {
    expect(faixaDoDia([])).toEqual({ inicio: 420, fim: 1140 });
    expect(faixaDoDia([{ inicio: 6 * 60 + 30, fim: 20 * 60 + 15 }])).toEqual({
      inicio: 360,
      fim: 1260,
    });
  });

  it('posiciona e corta o bloco na faixa', () => {
    const faixa = { inicio: 7 * 60, fim: 19 * 60 };
    expect(posicao(8 * 60, 8 * 60 + 30, faixa, 2)).toEqual({ topo: 120, altura: 60 });
    expect(posicao(6 * 60, 7 * 60 + 30, faixa, 2)).toEqual({ topo: 0, altura: 60 });
  });

  it('divide a largura só entre blocos que se sobrepõem', () => {
    const r = distribuirRaias([
      { id: 'a', inicio: 480, fim: 540 },
      { id: 'b', inicio: 510, fim: 570 },
      { id: 'c', inicio: 570, fim: 600 },
      { id: 'd', inicio: 480, fim: 510 },
    ]);
    const por = Object.fromEntries(r.map((b) => [b.id, b]));
    // a ocupa 8h–9h; d (8h–8h30) e b (8h30–9h30) não se tocam e dividem a segunda raia
    expect(por['a']).toMatchObject({ raia: 0, raias: 2 });
    expect(por['d']).toMatchObject({ raia: 1, raias: 2 });
    expect(por['b']).toMatchObject({ raia: 1, raias: 2 });
    expect(por['c']).toMatchObject({ raia: 0, raias: 1 });
  });

  it('confere se o horário cabe na disponibilidade', () => {
    const livres = [{ inicio: 600, fim: 720 }];
    expect(cabeEm(livres, 600, 630)).toBe(true);
    expect(cabeEm(livres, 705, 735)).toBe(false);
  });
});
