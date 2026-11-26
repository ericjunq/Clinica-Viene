import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/** Regras de senha (D-066) — repetidas aqui só por usabilidade; quem valida de verdade é o backend. */
export function regrasNaoAtendidas(senha: string | null | undefined): string[] {
  const valor = senha ?? '';
  const faltando: string[] = [];
  if (valor.length < 12) faltando.push('pelo menos 12 caracteres');
  if (!/\p{Lu}/u.test(valor)) faltando.push('1 letra maiúscula');
  if (!/\p{Ll}/u.test(valor)) faltando.push('1 letra minúscula');
  if (!/[^\p{L}\p{N}\s]/u.test(valor)) faltando.push('1 caractere especial');
  return faltando;
}

export const senhaForteValidator: ValidatorFn = (
  controle: AbstractControl,
): ValidationErrors | null => {
  const faltando = regrasNaoAtendidas(controle.value as string);
  return faltando.length ? { senhaFraca: faltando } : null;
};

/** Grupo com os campos `novaSenha` e `confirmacao` iguais. */
export const senhasIguaisValidator: ValidatorFn = (
  grupo: AbstractControl,
): ValidationErrors | null =>
  grupo.get('novaSenha')?.value === grupo.get('confirmacao')?.value
    ? null
    : { senhasDiferentes: true };
