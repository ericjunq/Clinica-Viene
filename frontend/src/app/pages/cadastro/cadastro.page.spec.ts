import { tokenDoFragmento } from './cadastro.page';

describe('tokenDoFragmento', () => {
  it('lê o token que vem depois do # do link', () => {
    expect(tokenDoFragmento('abc_DEF-123')).toBe('abc_DEF-123');
  });

  it('sem token, não há convite', () => {
    expect(tokenDoFragmento(null)).toBeNull();
    expect(tokenDoFragmento('  ')).toBeNull();
  });
});
