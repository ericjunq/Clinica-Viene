import { Page, expect, test } from '@playwright/test';
import { codigoTotp } from './totp';
import { CPF_MASTER, SENHA_MASTER, SENHA_NOVA_MASTER } from './usuarios-e2e';

/**
 * Fluxo completo do cadastro (Fase 2): a MASTER publica a política de privacidade e uma ficha de saúde, faz o
 * pré-cadastro de um paciente sem e-mail e abre o quiosque; o paciente confere os dados, responde a ficha, aceita
 * a política e assina digitando o nome; a recepção gera o QR para o paciente baixar o PDF com o CPF.
 *
 * Depende do projeto "primeiro-acesso" (que cria a MASTER num banco novo).
 */
const CPF_PACIENTE = '39051724888';
const NOME_PACIENTE = 'Paciente Ficticio E2E';

/** Botão visível (o stepper vertical mantém os passos fechados na página). */
function botaoVisivel(page: Page, nome: string) {
  return page.getByRole('button', { name: nome, exact: true }).filter({ visible: true });
}

// Os testes deste arquivo usam a mesma sessão da MASTER, em sequência (o 2FA dela só é cadastrado uma vez).
test.describe.configure({ mode: 'serial' });

let page: Page;

test.beforeAll(async ({ browser }) => {
  page = await browser.newPage();
  page.on('dialog', (dialogo) => void dialogo.accept());

  // primeiro acesso da MASTER: 2FA + troca de senha
  await page.goto('/login');
  await page.locator('#cpf').pressSequentially(CPF_MASTER);
  await page.locator('#senha').fill(SENHA_MASTER);
  await page.getByRole('button', { name: 'Entrar' }).click();
  await expect(page).toHaveURL(/\/dois-fatores/);
  const segredo = (await page.getByTestId('segredo-2fa').textContent())?.trim() ?? '';
  await page.locator('#codigo-cadastro').fill(codigoTotp(segredo));
  await page.getByRole('button', { name: 'Confirmar' }).click();
  await page.getByRole('button', { name: 'Já guardei os códigos' }).click();
  await expect(page).toHaveURL(/\/trocar-senha/);
  await page.locator('#senha-atual').fill(SENHA_MASTER);
  await page.locator('#nova-senha').fill(SENHA_NOVA_MASTER);
  await page.locator('#confirmacao').fill(SENHA_NOVA_MASTER);
  await page.getByRole('button', { name: 'Salvar' }).click();
  await expect(page.getByRole('heading', { name: /Olá,/ })).toBeVisible();
});

test.afterAll(async () => {
  await page?.close();
});

test('MASTER configura, cadastra o paciente, ele assina no quiosque e baixa o PDF', async ({
  browser,
}) => {
  test.setTimeout(180_000);

  // configurações: publica a política de privacidade de exemplo
  await page.getByRole('link', { name: 'Configurações' }).click();
  await page.getByRole('button', { name: 'Carregar textos de exemplo' }).click();
  await expect(page.getByText('rascunho').first()).toBeVisible();
  await page.getByRole('button', { name: 'Publicar' }).first().click();
  await expect(page.getByText('vigente').first()).toBeVisible();

  // configurações: ficha de saúde com uma pergunta de alerta, publicada
  await page.getByRole('tab', { name: 'Fichas de saúde' }).click();
  await page.getByRole('button', { name: 'Nova ficha' }).click();
  await page.locator('#modelo-titulo').fill('Ficha E2E');
  const pergunta = page.getByTestId('pergunta-1');
  await pergunta
    .getByLabel('Pergunta (como o paciente lê)')
    .fill('Tem alergia a algum medicamento?');
  await pergunta.getByText('Alerta para o profissional').click();
  await page.getByRole('button', { name: 'Salvar rascunho' }).click();
  await expect(page.getByText(/Odontologia · v1 · Ficha E2E · 1 perguntas/)).toBeVisible();
  await page.getByRole('button', { name: 'Publicar' }).click();
  await expect(page.getByText('vigente')).toBeVisible();

  // pré-cadastro do paciente (sem e-mail)
  await page.getByRole('link', { name: 'Pacientes' }).click();
  await page.getByRole('button', { name: 'Novo paciente' }).click();
  // o diálogo leva o foco ao primeiro campo ao terminar de abrir: espera, senão o texto cai no campo errado
  await expect(page.locator('#paciente-nome')).toBeFocused();
  await page.locator('#paciente-nome').fill(NOME_PACIENTE);
  await page.locator('#paciente-nascimento').fill('1990-05-20');
  await page.locator('#paciente-cpf').pressSequentially(CPF_PACIENTE);
  await page.locator('#paciente-telefone').fill('77999990000');
  await page.getByRole('button', { name: 'Cadastrar' }).click();
  await expect(page.getByRole('heading', { name: NOME_PACIENTE })).toBeVisible();

  // convite no aparelho da clínica (quiosque)
  await page.getByRole('tab', { name: 'Convite' }).click();
  await page.getByText('Conferi o documento com foto do paciente').click();
  await page.getByRole('button', { name: 'Abrir quiosque' }).click();
  const linkConvite = (await page.getByTestId('link-convite').textContent())?.trim() ?? '';
  expect(linkConvite).toMatch(/\/cadastro#[A-Za-z0-9_-]+$/);

  // o paciente preenche num aparelho separado (outro navegador, sem a sessão da recepção)
  const tablet = await browser.newContext();
  const paciente = await tablet.newPage();
  await paciente.goto(linkConvite.slice(linkConvite.indexOf('/cadastro')));
  await paciente.getByRole('button', { name: 'Começar' }).click();
  await paciente.getByRole('textbox', { name: 'CEP', exact: true }).fill('45000-000');
  await paciente.getByRole('textbox', { name: 'UF', exact: true }).fill('BA');
  await paciente.getByRole('textbox', { name: 'Rua', exact: true }).fill('Rua Fictícia');
  await paciente.getByRole('textbox', { name: 'Número', exact: true }).fill('100');
  await paciente.getByRole('textbox', { name: 'Bairro', exact: true }).fill('Centro');
  await paciente.getByRole('textbox', { name: 'Cidade', exact: true }).fill('Vitória da Conquista');
  await botaoVisivel(paciente, 'Próximo').click();
  await paciente.getByRole('radio', { name: 'Não' }).check();
  await botaoVisivel(paciente, 'Próximo').click();
  await paciente.getByText('Li e estou ciente da política de privacidade').click();
  await botaoVisivel(paciente, 'Próximo').click();
  await paciente.getByRole('button', { name: 'Conferir ficha' }).click();
  await expect(paciente.getByText('Ficha conferida')).toBeVisible();
  await paciente.getByText('Digitar o nome').click();
  await paciente.locator('#nome-assinatura').fill(NOME_PACIENTE);
  await paciente.locator('#declaracao').getByRole('checkbox').check();
  await paciente.getByRole('button', { name: 'Assinar ficha' }).click();
  await expect(paciente.getByText('Ficha assinada com sucesso')).toBeVisible();
  await expect(paciente.getByText('Pode devolver o aparelho à recepção.')).toBeVisible();

  // a recepção acha o paciente na busca, vê a ficha assinada (sem abrir o PDF) e gera o QR de download
  await page.getByRole('link', { name: 'Pacientes' }).first().click();
  await page.locator('#busca-paciente').fill('Paciente Ficticio');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await page.getByRole('cell', { name: NOME_PACIENTE }).click();
  await expect(page.getByRole('heading', { name: NOME_PACIENTE })).toBeVisible();
  await page.getByRole('tab', { name: 'Documentos' }).click();
  await expect(page.getByText('Ficha de cadastro assinada')).toBeVisible();
  await page.getByRole('button', { name: 'Mostrar QR para baixar' }).click();
  await expect(page.getByAltText('QR code para baixar a ficha')).toBeVisible();
  const linkEntrega = (await page.getByTestId('link-entrega').textContent())?.trim() ?? '';
  expect(linkEntrega).toMatch(/\/documento#[A-Za-z0-9_-]+$/);

  // o paciente baixa no celular dele informando o CPF
  await paciente.goto(linkEntrega.slice(linkEntrega.indexOf('/documento')));
  await paciente.locator('#cpf').pressSequentially(CPF_PACIENTE);
  const [download] = await Promise.all([
    paciente.waitForEvent('download'),
    paciente.getByRole('button', { name: 'Baixar PDF' }).click(),
  ]);
  expect(download.suggestedFilename()).toMatch(/\.pdf$/);
  await tablet.close();

  // privacidade e histórico refletem a ficha assinada
  await page.getByRole('tab', { name: 'Privacidade' }).click();
  await expect(page.getByText('Ciente', { exact: true })).toBeVisible();
  await page.getByRole('tab', { name: 'Histórico' }).click();
  await expect(page.getByText('Paciente (aparelho da clínica)').first()).toBeVisible();
});

/** Dia da semana (rótulo da tela) de uma data aaaa-mm-dd. */
const DIAS = ['Domingo', 'Segunda', 'Terça', 'Quarta', 'Quinta', 'Sexta', 'Sábado'];

test('agenda: profissional, horários, consulta marcada no calendário, check-in e aviso em tempo real', async ({
  browser,
}) => {
  test.setTimeout(180_000);
  // mesmo dia da semana, daqui a 7 dias (sempre no futuro), na hora da clínica (UTC−3)
  const hojeClinica = new Date(Date.now() - 3 * 3600_000);
  const alvo = new Date(hojeClinica.getTime() + 7 * 86400_000).toISOString().slice(0, 10);
  const diaSemana = DIAS[new Date(`${alvo}T12:00:00Z`).getUTCDay()];

  // a MASTER cadastra a dentista
  await page.getByRole('link', { name: 'Usuários' }).click();
  await page.getByRole('button', { name: 'Novo usuário' }).click();
  await expect(page.locator('#nome')).toBeFocused();
  await page.locator('#nome').fill('Dentista Ficticia');
  await page.locator('#cpf-novo').pressSequentially('86241307940');
  await page.locator('#email').fill('dentista.e2e@exemplo.com');
  await page.locator('#perfil').click();
  await page.getByRole('option', { name: 'Profissional de saúde' }).click();
  await page.getByRole('combobox', { name: 'Área de atuação' }).click();
  await page.getByRole('option', { name: 'ODONTOLOGIA' }).click();
  await page.getByRole('combobox', { name: 'Conselho' }).click();
  await page.getByRole('option', { name: 'CRO', exact: true }).click();
  await page.locator('#numero').fill('12345');
  await page.getByRole('combobox', { name: 'UF' }).click();
  await page.getByRole('option', { name: 'BA', exact: true }).click();
  await page.locator('#senha-provisoria').fill('Provisoria#Dentista1');
  await page.getByRole('button', { name: 'Criar usuário' }).click();
  await expect(page.getByRole('cell', { name: 'Dentista Ficticia' })).toBeVisible();

  // configurações: consultório e categorias de exemplo
  await page.getByRole('link', { name: 'Configurações' }).click();
  await page.getByRole('tab', { name: 'Consultórios' }).click();
  await page.locator('#novo-consultorio').fill('Consultório 1');
  await page.getByRole('button', { name: 'Adicionar' }).click();
  await expect(page.getByText('Consultório 1')).toBeVisible();
  await page.getByRole('tab', { name: 'Categorias de procedimento' }).click();
  await page.getByRole('button', { name: 'Carregar categorias de exemplo' }).click();
  await expect(page.getByText('ORTODONTIA', { exact: true })).toBeVisible();

  // horário fixo da dentista nesse dia da semana, 8h–18h
  await page.getByRole('link', { name: 'Agenda' }).click();
  await page.getByRole('link', { name: 'Horários dos profissionais' }).click();
  await page.locator('#horarios-profissional').click();
  await page.getByRole('option', { name: 'Dentista Ficticia' }).click();
  await page.locator('#fixo-dia').click();
  await page.getByRole('option', { name: diaSemana }).click();
  await page.locator('#fixo-fim').fill('18:00');
  await page.getByRole('button', { name: 'Adicionar horário fixo' }).click();
  await expect(page.getByText(`${diaSemana} 08:00–18:00`)).toBeVisible();

  // agenda do dia: clica às 9h na coluna do consultório (faixa começa às 7h; 1,6 px por minuto)
  await page.getByRole('link', { name: '← Agenda' }).click();
  await page.locator('#agenda-data').fill(alvo);
  const coluna = page.locator('.coluna[data-coluna="Consultório 1"]');
  await expect(coluna.locator('.livre')).toHaveCount(1);
  await coluna.click({ position: { x: 40, y: (9 * 60 - 7 * 60) * 1.6 + 6 } });

  // marcação: paciente, dentista já escolhida (única atendendo), dois procedimentos
  await expect(page.getByRole('heading', { name: 'Marcar consulta' })).toBeVisible();
  await expect(page.locator('#agendamento-hora')).toHaveValue('09:00');
  await expect(page.locator('#agendamento-busca')).toBeFocused();
  await page.locator('#agendamento-busca').fill('Paciente Ficticio');
  await page.getByRole('button', { name: 'Buscar' }).click();
  await page.getByRole('button', { name: 'Escolher' }).click();
  await page.getByTestId('item-1').getByRole('combobox', { name: 'Categoria' }).click();
  await page.getByRole('option', { name: 'Ortodontia' }).click();
  await page
    .getByTestId('item-1')
    .getByLabel('Descrição (opcional)')
    .fill('Manutenção do aparelho');
  await page.getByRole('button', { name: 'Outro procedimento' }).click();
  await page.getByTestId('item-2').getByRole('combobox', { name: 'Categoria' }).click();
  await page.getByRole('option', { name: /Clínica geral/ }).click();
  await page.getByRole('button', { name: 'Marcar', exact: true }).click();

  const bloco = coluna.locator('.bloco', { hasText: NOME_PACIENTE });
  await expect(bloco).toBeVisible();
  await expect(bloco).toContainText('09:00–09:30');

  // semana da dentista (única profissional, já escolhida): 7 colunas, a consulta na coluna do dia
  await page.getByText('Semana', { exact: true }).click();
  await expect(page.getByTestId('periodo-semana')).toBeVisible();
  await expect(page.locator('.coluna')).toHaveCount(7);
  const colunaDoDia = page.locator(`.coluna[data-dia="${alvo}"]`);
  await expect(colunaDoDia.locator('.livre')).toHaveCount(1);
  await expect(colunaDoDia.locator('.bloco', { hasText: NOME_PACIENTE })).toContainText(
    '09:00–09:30',
  );
  // a semana seguinte não tem a consulta; voltar mostra de novo
  await page.getByRole('button', { name: 'Próxima semana' }).click();
  await expect(page.locator('.bloco', { hasText: NOME_PACIENTE })).toHaveCount(0);
  await page.getByRole('button', { name: 'Semana anterior' }).click();
  await expect(colunaDoDia.locator('.bloco', { hasText: NOME_PACIENTE })).toBeVisible();

  // a dentista entra no sistema (outro navegador) e fica com a agenda aberta
  const contextoDentista = await browser.newContext();
  const dentista = await contextoDentista.newPage();
  await dentista.goto('/login');
  await dentista.locator('#cpf').pressSequentially('86241307940');
  await dentista.locator('#senha').fill('Provisoria#Dentista1');
  await dentista.getByRole('button', { name: 'Entrar' }).click();
  await expect(dentista).toHaveURL(/\/dois-fatores/);
  const segredo = (await dentista.getByTestId('segredo-2fa').textContent())?.trim() ?? '';
  await dentista.locator('#codigo-cadastro').fill(codigoTotp(segredo));
  await dentista.getByRole('button', { name: 'Confirmar' }).click();
  await dentista.getByRole('button', { name: 'Já guardei os códigos' }).click();
  await dentista.locator('#senha-atual').fill('Provisoria#Dentista1');
  await dentista.locator('#nova-senha').fill('Nova#SenhaDentista26');
  await dentista.locator('#confirmacao').fill('Nova#SenhaDentista26');
  await dentista.getByRole('button', { name: 'Salvar' }).click();
  await dentista.getByRole('link', { name: 'Agenda' }).click();
  await dentista.locator('#agenda-data').fill(alvo);
  await expect(dentista.locator('.bloco', { hasText: NOME_PACIENTE })).toBeVisible();

  // a recepção faz o check-in pela lista do dia
  await page.getByText('Lista do dia', { exact: true }).click();
  const linha = page.getByTestId(`consulta-${NOME_PACIENTE}`);
  await expect(linha).toContainText('Ortodontia');
  await linha.getByRole('button', { name: 'Chegou' }).click();
  await expect(linha).toContainText('Chegou');

  // a dentista recebe o aviso na hora e a agenda dela se atualiza
  await expect(dentista.getByText(`Paciente chegou: ${NOME_PACIENTE}`)).toBeVisible({
    timeout: 10_000,
  });
  await dentista.getByRole('button', { name: 'Ciente', exact: true }).click();
  // um único aviso: depois do "Ciente" não sobra nenhum na tela
  await expect(dentista.locator('mat-snack-bar-container')).toHaveCount(0);
  await expect(dentista.locator('.bloco', { hasText: NOME_PACIENTE })).toContainText('Chegou');

  // ela inicia o atendimento pelos detalhes da consulta
  await dentista.locator('.bloco', { hasText: NOME_PACIENTE }).click();
  await dentista.getByRole('button', { name: 'Iniciar atendimento' }).click();
  await expect(dentista.getByRole('button', { name: 'Concluir' })).toBeVisible();
  await dentista.getByRole('button', { name: 'Fechar' }).click();
  await contextoDentista.close();
});
