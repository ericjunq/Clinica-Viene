import { AreaAtuacao } from './usuario.model';
import { Endereco, ModoConvite, Parentesco, TipoTermo } from './cadastro.model';

/** Cadastro de pacientes nas telas internas (2G). CPFs sempre mascarados nas respostas. */

export interface PacienteResumo {
  id: string;
  nomeCompleto: string;
  cpfMascarado: string | null;
  dataNascimento: string;
  telefone: string | null;
}

export interface Paciente {
  id: string;
  nomeCompleto: string;
  cpfMascarado: string | null;
  dataNascimento: string;
  idade: number;
  menorDeIdade: boolean;
  email: string | null;
  telefone: string | null;
  endereco: Endereco | null;
  responsavel: {
    nomeCompleto: string;
    cpfMascarado: string | null;
    parentesco: Parentesco;
    telefone: string | null;
    email: string | null;
  } | null;
  semCpf: boolean;
  criadoEm: string;
  atualizadoEm: string;
}

export interface NovoPaciente {
  nomeCompleto: string;
  cpf: string | null;
  dataNascimento: string;
  email: string | null;
  telefone: string | null;
  endereco: Endereco | null;
  responsavel: {
    nomeCompleto: string;
    cpf: string;
    parentesco: Parentesco;
    telefone: string | null;
    email: string | null;
  } | null;
  semCpfJustificativa: string | null;
  confirmarDuplicidade: boolean;
}

export interface ContatoPaciente {
  email: string | null;
  telefone: string | null;
  endereco: Endereco | null;
}

export type OrigemAlteracao = 'RECEPCAO' | 'PACIENTE_LINK' | 'PACIENTE_QUIOSQUE';

export interface PacienteVersao {
  id: string;
  camposAlterados: string;
  origem: OrigemAlteracao;
  alteradoPor: string | null;
  criadoEm: string;
}

export type SituacaoConvite = 'ATIVO' | 'USADO' | 'EXPIRADO' | 'CANCELADO';

export interface Convite {
  id: string;
  modo: ModoConvite;
  areaAtuacao: AreaAtuacao;
  situacao: SituacaoConvite;
  criadoEm: string;
  expiraEm: string;
  enviadoPorEmailEm: string | null;
  usadoEm: string | null;
}

export interface NovoConvite {
  modo: ModoConvite;
  areaAtuacao: AreaAtuacao;
  enviarEmail: boolean;
  documentoConferido: boolean;
}

export interface ConviteCriado {
  id: string;
  modo: ModoConvite;
  link: string;
  qrCode: string;
  expiraEm: string;
  emailEnviadoPara: string | null;
  podeReenviarEm: string | null;
}

export type DecisaoTermo = 'ACEITO' | 'RECUSADO' | 'REVOGADO';

export interface Consentimento {
  tipo: TipoTermo;
  obrigatorio: boolean;
  situacao: DecisaoTermo | null;
  versao: number | null;
  registradoEm: string | null;
  origem: OrigemAlteracao | null;
  peloResponsavel: boolean;
}

export interface DocumentoPaciente {
  id: string;
  tipo: 'FICHA_CADASTRO';
  criadoEm: string;
  assinadoPor: 'PACIENTE' | 'RESPONSAVEL_LEGAL' | null;
  nomeSignatario: string | null;
  metodo: 'DESENHO' | 'NOME_DIGITADO' | null;
}

export interface EntregaCriada {
  link: string;
  qrCode: string;
  expiraEm: string;
  emailEnviadoPara: string | null;
}

export const ROTULO_TERMO: Record<TipoTermo, string> = {
  POLITICA_PRIVACIDADE: 'Política de privacidade',
  CONSENTIMENTO_LEMBRETES: 'Lembretes por WhatsApp/SMS',
  CONSENTIMENTO_PROMOCOES: 'Promoções e novidades',
  CONSENTIMENTO_IMAGEM: 'Uso de imagem (antes e depois)',
  CONSENTIMENTO_PESQUISA: 'Pesquisas de satisfação',
};

export const ROTULO_AREA: Record<AreaAtuacao, string> = {
  ODONTOLOGIA: 'Odontologia',
  PSICOLOGIA: 'Psicologia',
  MEDICINA: 'Medicina',
  NUTRICAO: 'Nutrição',
  FISIOTERAPIA: 'Fisioterapia',
  FONOAUDIOLOGIA: 'Fonoaudiologia',
  ENFERMAGEM: 'Enfermagem',
  OUTRA: 'Outra',
};

export const ROTULO_ORIGEM: Record<OrigemAlteracao, string> = {
  RECEPCAO: 'Recepção',
  PACIENTE_LINK: 'Paciente (link no celular)',
  PACIENTE_QUIOSQUE: 'Paciente (aparelho da clínica)',
};

/** Idade completa em anos numa data (para decidir se o pré-cadastro precisa de responsável). */
export function idadeEm(nascimento: string, hoje: Date = new Date()): number {
  const [ano, mes, dia] = nascimento.split('-').map(Number);
  let idade = hoje.getFullYear() - ano;
  const mesHoje = hoje.getMonth() + 1;
  if (mesHoje < mes || (mesHoje === mes && hoje.getDate() < dia)) {
    idade--;
  }
  return idade;
}
