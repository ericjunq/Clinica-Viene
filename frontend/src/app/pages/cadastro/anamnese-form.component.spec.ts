import { PerguntaAnamnese } from '../../core/models/cadastro.model';
import { pendentes, respostasPreenchidas, respostasVazias } from './anamnese-form.component';

function pergunta(parcial: Partial<PerguntaAnamnese>): PerguntaAnamnese {
  return {
    codigo: 'p',
    secao: null,
    texto: 'Pergunta',
    tipo: 'SIM_NAO',
    obrigatoria: true,
    opcoes: null,
    alertaClinico: null,
    detalheObrigatorioQuandoSim: null,
    ajuda: null,
    ...parcial,
  };
}

describe('ficha de saúde', () => {
  const perguntas = [
    pergunta({ codigo: 'alergia', tipo: 'SIM_NAO_DETALHE', detalheObrigatorioQuandoSim: true }),
    pergunta({ codigo: 'doencas', tipo: 'MULTIPLA_ESCOLHA', opcoes: ['Nenhuma', 'Diabetes'] }),
    pergunta({ codigo: 'obs', tipo: 'TEXTO_LONGO', obrigatoria: false }),
  ];

  it('começa sem nada marcado', () => {
    const respostas = respostasVazias(perguntas);
    expect(respostas['alergia']).toEqual({ valor: null, valores: null, detalhe: null });
    expect(respostas['doencas'].valores).toEqual([]);
  });

  it('aponta obrigatórias sem resposta e Sim sem o "qual?"', () => {
    const respostas = respostasVazias(perguntas);
    expect(pendentes(perguntas, respostas).map((p) => p.codigo)).toEqual(['alergia', 'doencas']);

    respostas['alergia'].valor = 'SIM';
    respostas['doencas'].valores = ['Nenhuma'];
    expect(pendentes(perguntas, respostas).map((p) => p.codigo)).toEqual(['alergia']);

    respostas['alergia'].detalhe = 'Dipirona';
    expect(pendentes(perguntas, respostas)).toEqual([]);
  });

  it('envia só as perguntas respondidas', () => {
    const respostas = respostasVazias(perguntas);
    respostas['alergia'].valor = 'NAO';
    respostas['obs'].valor = '   ';
    expect(Object.keys(respostasPreenchidas(respostas))).toEqual(['alergia']);
  });
});
