import { expect, test } from '@playwright/test';
import { codigoTotp } from './totp';

/**
 * Primeiro acesso do usuário DEV criado pelo inicializador (D-048): login → cadastro do 2FA → códigos de
 * recuperação → troca de senha obrigatória → gestão de usuários → sair.
 *
 * Usa um banco novo (o job E2E do CI sobe o ambiente do zero). Rodar de novo no mesmo banco falha de
 * propósito: a senha provisória já terá sido trocada.
 */
const CPF_DEV = process.env['E2E_DEV_CPF'] ?? '';
const SENHA_PROVISORIA = process.env['E2E_DEV_SENHA'] ?? '';
const SENHA_NOVA = 'Nova#SenhaDoDev2026';

test('primeiro acesso do DEV: 2FA, troca de senha e criação de usuário', async ({ page }) => {
  test.skip(!CPF_DEV || !SENHA_PROVISORIA, 'Defina E2E_DEV_CPF e E2E_DEV_SENHA');

  await page.goto('/login');
  await page.locator('#cpf').pressSequentially(CPF_DEV.replace(/\D/g, ''));
  await page.locator('#senha').fill(SENHA_PROVISORIA);
  await page.getByRole('button', { name: 'Entrar' }).click();

  // cadastro do app autenticador: lê a chave mostrada na tela e calcula o código como o app faria
  await expect(page).toHaveURL(/\/dois-fatores/);
  await expect(page.getByAltText('QR code para o app autenticador')).toBeVisible();
  const segredo = (await page.getByTestId('segredo-2fa').textContent())?.trim() ?? '';
  expect(segredo).toMatch(/^[A-Z2-7]{32}$/);
  await page.locator('#codigo-cadastro').fill(codigoTotp(segredo));
  await page.getByRole('button', { name: 'Confirmar' }).click();

  // códigos de recuperação: mostrados uma única vez
  await expect(page.getByTestId('codigos-recuperacao').locator('li')).toHaveCount(10);
  await page.getByRole('button', { name: 'Já guardei os códigos' }).click();

  // senha provisória: troca obrigatória antes de usar o sistema
  await expect(page).toHaveURL(/\/trocar-senha/);
  await page.locator('#senha-atual').fill(SENHA_PROVISORIA);
  await page.locator('#nova-senha').fill(SENHA_NOVA);
  await page.locator('#confirmacao').fill(SENHA_NOVA);
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('heading', { name: /Olá,/ })).toBeVisible();

  // gestão de usuários: cria uma recepcionista (perfil padrão ADMIN)
  await page.getByRole('link', { name: 'Usuários' }).click();
  await expect(page.getByRole('heading', { name: 'Usuários' })).toBeVisible();
  await page.getByRole('button', { name: 'Novo usuário' }).click();
  await page.locator('#nome').fill('Recepcionista Fictícia');
  await page.locator('#cpf-novo').pressSequentially('08301661305');
  await page.locator('#email').fill('recepcao.e2e@exemplo.com');
  await page.locator('#senha-provisoria').fill('Provisoria#Recepcao1');
  await page.getByRole('button', { name: 'Criar usuário' }).click();
  await expect(page.getByRole('cell', { name: 'Recepcionista Fictícia' })).toBeVisible();
  await expect(page.getByRole('cell', { name: '***.016.613-**' })).toBeVisible();

  // edição: muda o nome (CPF fica bloqueado)
  await page
    .getByRole('row', { name: /Recepcionista Fictícia/ })
    .getByRole('button', { name: 'Editar' })
    .click();
  await expect(page.locator('#cpf-novo')).toBeDisabled();
  await page.locator('#nome').fill('Recepcionista Editada');
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('cell', { name: 'Recepcionista Editada' })).toBeVisible();

  // sair: volta ao login e a sessão não é restaurada ao recarregar
  await page.getByRole('button', { name: 'Sair' }).click();
  await expect(page).toHaveURL(/\/login/);
  await page.reload();
  await expect(page).toHaveURL(/\/login/);
});

test('senha errada mostra mensagem genérica', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#cpf').pressSequentially('18609139034');
  await page.locator('#senha').fill('Senha#Errada12345');
  await page.getByRole('button', { name: 'Entrar' }).click();

  await expect(page.getByText('CPF ou senha inválidos.')).toBeVisible();
});

test('rotas internas sem sessão voltam para o login', async ({ page }) => {
  await page.goto('/usuarios');
  await expect(page).toHaveURL(/\/login/);
});
