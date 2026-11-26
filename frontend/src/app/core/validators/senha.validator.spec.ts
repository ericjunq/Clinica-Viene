import { FormControl, FormGroup } from '@angular/forms';
import { regrasNaoAtendidas, senhaForteValidator, senhasIguaisValidator } from './senha.validator';

describe('validação de senha (D-066)', () => {
  it('aceita senha com 12+ caracteres, maiúscula, minúscula e especial', () => {
    expect(regrasNaoAtendidas('Clinica#Viene')).toEqual([]);
    expect(regrasNaoAtendidas('Árvore.secreta')).toEqual([]);
  });

  it('lista o que falta', () => {
    expect(regrasNaoAtendidas('curta')).toEqual([
      'pelo menos 12 caracteres',
      '1 letra maiúscula',
      '1 caractere especial',
    ]);
    expect(regrasNaoAtendidas('Sem Especial 123')).toEqual(['1 caractere especial']);
    expect(regrasNaoAtendidas(null)).toHaveLength(4);
  });

  it('funciona como validador de formulário', () => {
    expect(senhaForteValidator(new FormControl('fraca'))).toEqual({
      senhaFraca: expect.any(Array),
    });
    expect(senhaForteValidator(new FormControl('Clinica#Viene'))).toBeNull();
  });

  it('confere a confirmação', () => {
    const grupo = new FormGroup({
      novaSenha: new FormControl('Clinica#Viene'),
      confirmacao: new FormControl('Outra#Senha12'),
    });
    expect(senhasIguaisValidator(grupo)).toEqual({ senhasDiferentes: true });
    grupo.controls.confirmacao.setValue('Clinica#Viene');
    expect(senhasIguaisValidator(grupo)).toBeNull();
  });
});
