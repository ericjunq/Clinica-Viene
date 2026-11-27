/** Fluxo público do cadastro do paciente (D-091, D-100). */

export type ModoConvite = 'LINK' | 'QUIOSQUE';
export type TipoTermo =
  | 'POLITICA_PRIVACIDADE'
  | 'CONSENTIMENTO_LEMBRETES'
  | 'CONSENTIMENTO_PROMOCOES'
  | 'CONSENTIMENTO_IMAGEM'
  | 'CONSENTIMENTO_PESQUISA';
export type TipoPergunta =
  | 'SIM_NAO'
  | 'SIM_NAO_DETALHE'
  | 'TEXTO_CURTO'
  | 'TEXTO_LONGO'
  | 'ESCOLHA_UNICA'
  | 'MULTIPLA_ESCOLHA'
  | 'DATA'
  | 'NUMERO';
export type Parentesco = 'MAE' | 'PAI' | 'AVO' | 'TIO' | 'IRMAO' | 'TUTOR' | 'OUTRO';

export interface ConviteAberto {
  modo: ModoConvite;
  primeiroNome: string;
  precisaCodigo: boolean;
  emailMascarado: string | null;
}

export interface CodigoEnviado {
  emailMascarado: string;
  podeReenviarEm: string;
  expiraEm: string;
}

export interface SessaoCadastro {
  token: string;
  expiraEm: string;
}

export interface Endereco {
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
}

export interface Responsavel {
  nomeCompleto: string;
  cpf: string;
  parentesco: Parentesco;
  telefone: string | null;
  email: string | null;
}

export interface DadosCadastro {
  nomeCompleto: string;
  cpf: string | null;
  dataNascimento: string;
  email: string | null;
  telefone: string | null;
  endereco: Endereco | null;
  responsavel: Responsavel | null;
}

export interface PerguntaAnamnese {
  codigo: string;
  secao: string | null;
  texto: string;
  tipo: TipoPergunta;
  obrigatoria: boolean | null;
  opcoes: string[] | null;
  alertaClinico: boolean | null;
  detalheObrigatorioQuandoSim: boolean | null;
  ajuda: string | null;
}

export interface RespostaAnamnese {
  valor: string | null;
  valores: string[] | null;
  detalhe: string | null;
}

export interface TermoFormulario {
  id: string;
  tipo: TipoTermo;
  obrigatorio: boolean;
  versao: number;
  titulo: string;
  texto: string;
}

export interface FormularioCadastro {
  modo: ModoConvite;
  menorDeIdade: boolean;
  dados: DadosCadastro;
  anamnese: { id: string; titulo: string; versao: number; perguntas: PerguntaAnamnese[] };
  termos: TermoFormulario[];
}

export interface FichaCadastro {
  dados: DadosCadastro;
  modeloAnamneseId: string;
  respostas: Record<string, RespostaAnamnese>;
  termos: { termoVersaoId: string; aceito: boolean }[];
  acompanhante: { nome: string; motivo: string | null } | null;
}

export interface RevisaoCadastro {
  prontoParaAssinar: boolean;
  peloResponsavel: boolean;
}

export const PARENTESCOS: { valor: Parentesco; rotulo: string }[] = [
  { valor: 'MAE', rotulo: 'Mãe' },
  { valor: 'PAI', rotulo: 'Pai' },
  { valor: 'AVO', rotulo: 'Avô/Avó' },
  { valor: 'TIO', rotulo: 'Tio(a)' },
  { valor: 'IRMAO', rotulo: 'Irmão(ã)' },
  { valor: 'TUTOR', rotulo: 'Tutor(a)' },
  { valor: 'OUTRO', rotulo: 'Outro' },
];

export type MetodoAssinatura = 'DESENHO' | 'NOME_DIGITADO';

export interface AssinaturaFicha {
  metodo: MetodoAssinatura;
  imagem: string | null;
  nomeDigitado: string | null;
}

export interface FichaAssinada {
  fichaId: string;
  documentoId: string;
  sha256: string;
  assinadoEm: string;
  entregaEnviadaPara: string | null;
}
