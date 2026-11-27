import { AreaAtuacao } from './usuario.model';
import { PerguntaAnamnese, TipoPergunta, TipoTermo } from './cadastro.model';

/** Configuração do MASTER: termos e modelos de anamnese (D-089, D-096). */

export type StatusVersao = 'RASCUNHO' | 'PUBLICADO';

export interface Termo {
  id: string;
  tipo: TipoTermo;
  obrigatorio: boolean;
  versao: number;
  titulo: string;
  texto: string;
  status: StatusVersao;
  criadoEm: string;
  publicadoEm: string | null;
}

export interface ModeloAnamnese {
  id: string;
  areaAtuacao: AreaAtuacao;
  versao: number;
  titulo: string;
  status: StatusVersao;
  perguntas: PerguntaAnamnese[];
  criadoEm: string;
  publicadoEm: string | null;
}

export const TIPOS_PERGUNTA: { valor: TipoPergunta; rotulo: string }[] = [
  { valor: 'SIM_NAO', rotulo: 'Sim / Não' },
  { valor: 'SIM_NAO_DETALHE', rotulo: 'Sim / Não + "qual?"' },
  { valor: 'TEXTO_CURTO', rotulo: 'Texto curto' },
  { valor: 'TEXTO_LONGO', rotulo: 'Texto longo' },
  { valor: 'ESCOLHA_UNICA', rotulo: 'Escolher uma opção' },
  { valor: 'MULTIPLA_ESCOLHA', rotulo: 'Marcar várias opções' },
  { valor: 'DATA', rotulo: 'Data' },
  { valor: 'NUMERO', rotulo: 'Número' },
];

/**
 * Código estável da pergunta a partir do texto (sem acento, minúsculo, com _), único no modelo. Segue a regra do
 * backend: começa com letra, 2 a 60 caracteres.
 */
export function codigoDaPergunta(texto: string, existentes: Iterable<string>): string {
  let base =
    texto
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
      .slice(0, 50)
      .replace(/_+$/g, '') || 'pergunta';
  if (!/^[a-z]/.test(base)) {
    base = `p_${base}`;
  }
  if (base.length < 2) {
    base = 'pergunta';
  }
  const usados = new Set(existentes);
  let codigo = base;
  for (let i = 2; usados.has(codigo); i++) {
    codigo = `${base}_${i}`;
  }
  return codigo;
}

/** Problemas que impedem salvar o modelo (o backend confere de novo). */
export function problemasDoModelo(titulo: string, perguntas: PerguntaAnamnese[]): string[] {
  const problemas: string[] = [];
  if (!titulo.trim()) {
    problemas.push('Informe o título do modelo.');
  }
  if (perguntas.length === 0) {
    problemas.push('Inclua ao menos uma pergunta.');
  }
  const codigos = new Set<string>();
  perguntas.forEach((p, i) => {
    const n = i + 1;
    if (!p.texto.trim()) {
      problemas.push(`Pergunta ${n}: escreva o texto.`);
    }
    if (!/^[a-z][a-z0-9_]{1,59}$/.test(p.codigo)) {
      problemas.push(`Pergunta ${n}: código inválido (use letras minúsculas, números e _).`);
    } else if (codigos.has(p.codigo)) {
      problemas.push(`Pergunta ${n}: código repetido (${p.codigo}).`);
    }
    codigos.add(p.codigo);
    if (temOpcoes(p.tipo)) {
      const opcoes = p.opcoes ?? [];
      if (opcoes.length < 2) {
        problemas.push(`Pergunta ${n}: informe ao menos duas opções.`);
      } else if (new Set(opcoes).size !== opcoes.length) {
        problemas.push(`Pergunta ${n}: há opções repetidas.`);
      }
    }
  });
  return problemas;
}

export function temOpcoes(tipo: TipoPergunta): boolean {
  return tipo === 'ESCOLHA_UNICA' || tipo === 'MULTIPLA_ESCOLHA';
}

/** Ajusta a pergunta ao tipo antes de salvar: opções só em escolha, "qual?" obrigatório só em Sim/Não + detalhe. */
export function normalizarPergunta(p: PerguntaAnamnese): PerguntaAnamnese {
  const opcoes = temOpcoes(p.tipo)
    ? (p.opcoes ?? []).map((o) => o.trim()).filter((o) => o.length > 0)
    : null;
  return {
    ...p,
    texto: p.texto.trim(),
    secao: p.secao?.trim() || null,
    ajuda: p.ajuda?.trim() || null,
    opcoes,
    obrigatoria: !!p.obrigatoria,
    alertaClinico: !!p.alertaClinico,
    detalheObrigatorioQuandoSim:
      p.tipo === 'SIM_NAO_DETALHE' ? !!p.detalheObrigatorioQuandoSim : null,
  };
}
