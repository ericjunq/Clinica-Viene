import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { mensagemDeErro } from './erro.model';

describe('mensagemDeErro', () => {
  it('usa o detail do Problem Details', () => {
    const erro = new HttpErrorResponse({
      status: 401,
      error: { status: 401, detail: 'CPF ou senha inválidos.' },
    });
    expect(mensagemDeErro(erro)).toBe('CPF ou senha inválidos.');
  });

  it('mostra quanto esperar no 429', () => {
    const erro = new HttpErrorResponse({
      status: 429,
      headers: new HttpHeaders({ 'Retry-After': '90' }),
    });
    expect(mensagemDeErro(erro)).toBe('Muitas tentativas. Tente novamente em 2 min.');
  });

  it('lista os campos inválidos', () => {
    const erro = new HttpErrorResponse({
      status: 400,
      error: { status: 400, erros: [{ campo: 'email', mensagem: 'inválido' }] },
    });
    expect(mensagemDeErro(erro)).toBe('email: inválido');
  });

  it('sem conexão', () => {
    expect(mensagemDeErro(new HttpErrorResponse({ status: 0 }))).toContain('Sem conexão');
  });
});
