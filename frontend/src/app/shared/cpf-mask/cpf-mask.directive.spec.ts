import { formatarCpf } from './cpf-mask.directive';

describe('formatarCpf', () => {
  it('formata enquanto digita e ignora o que não é número', () => {
    expect(formatarCpf('526')).toBe('526');
    expect(formatarCpf('52601')).toBe('526.01');
    expect(formatarCpf('5260181')).toBe('526.018.1');
    expect(formatarCpf('52601815906')).toBe('526.018.159-06');
    expect(formatarCpf('526.018.159-06999')).toBe('526.018.159-06');
    expect(formatarCpf('abc')).toBe('');
  });
});
