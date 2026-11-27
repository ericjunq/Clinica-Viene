import { PerguntaAnamnese } from './cadastro.model';
import { codigoDaPergunta, normalizarPergunta, problemasDoModelo } from './configuracao.model';

function pergunta(parcial: Partial<PerguntaAnamnese>): PerguntaAnamnese {
  return {
    codigo: 'p1',
    secao: null,
    texto: 'Pergunta',
    tipo: 'SIM_NAO',
    obrigatoria: true,
    opcoes: null,
    alertaClinico: false,
    detalheObrigatorioQuandoSim: null,
    ajuda: null,
    ...parcial,
  };
}

describe('editor de ficha de saúde', () => {
  it('gera código sem acento, começando com letra e sem repetir', () => {
    expect(codigoDaPergunta('Tem alergia a medicamento?', [])).toBe('tem_alergia_a_medicamento');
    expect(codigoDaPergunta('Está grávida?', ['esta_gravida'])).toBe('esta_gravida_2');
    expect(codigoDaPergunta('3 ou mais cirurgias', [])).toBe('p_3_ou_mais_cirurgias');
    expect(codigoDaPergunta('?', [])).toBe('pergunta');
    expect(codigoDaPergunta('x'.repeat(80), []).length).toBeLessThanOrEqual(52);
  });

  it('aponta título vazio, texto vazio, código repetido e escolha com menos de 2 opções', () => {
    const problemas = problemasDoModelo('', [
      pergunta({ codigo: 'a1', texto: '' }),
      pergunta({ codigo: 'a1' }),
      pergunta({ codigo: 'b1', tipo: 'ESCOLHA_UNICA', opcoes: ['Só uma'] }),
      pergunta({ codigo: 'c1', tipo: 'MULTIPLA_ESCOLHA', opcoes: ['A', 'A'] }),
      pergunta({ codigo: 'Inválido' }),
    ]);
    expect(problemas).toEqual([
      'Informe o título do modelo.',
      'Pergunta 1: escreva o texto.',
      'Pergunta 2: código repetido (a1).',
      'Pergunta 3: informe ao menos duas opções.',
      'Pergunta 4: há opções repetidas.',
      'Pergunta 5: código inválido (use letras minúsculas, números e _).',
    ]);
    expect(problemasDoModelo('Ficha', [])).toEqual(['Inclua ao menos uma pergunta.']);
    expect(problemasDoModelo('Ficha', [pergunta({})])).toEqual([]);
  });

  it('ajusta a pergunta ao tipo antes de salvar', () => {
    expect(
      normalizarPergunta(
        pergunta({
          tipo: 'SIM_NAO',
          opcoes: ['x'],
          detalheObrigatorioQuandoSim: true,
          secao: '  ',
        }),
      ),
    ).toMatchObject({ opcoes: null, detalheObrigatorioQuandoSim: null, secao: null });
    expect(
      normalizarPergunta(pergunta({ tipo: 'ESCOLHA_UNICA', opcoes: [' Não ', '', 'Sim'] })).opcoes,
    ).toEqual(['Não', 'Sim']);
    expect(
      normalizarPergunta(pergunta({ tipo: 'SIM_NAO_DETALHE', detalheObrigatorioQuandoSim: true }))
        .detalheObrigatorioQuandoSim,
    ).toBe(true);
  });
});
