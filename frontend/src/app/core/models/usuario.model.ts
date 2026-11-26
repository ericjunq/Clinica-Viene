export type Perfil = 'DEV' | 'MASTER' | 'ADMIN' | 'PROFISSIONAL';
export type StatusUsuario = 'ATIVO' | 'INATIVO';
export type AreaAtuacao =
  | 'ODONTOLOGIA'
  | 'PSICOLOGIA'
  | 'MEDICINA'
  | 'NUTRICAO'
  | 'FISIOTERAPIA'
  | 'FONOAUDIOLOGIA'
  | 'ENFERMAGEM'
  | 'OUTRA';
export type ConselhoProfissional =
  'CRO' | 'CRP' | 'CRM' | 'CRN' | 'CREFITO' | 'CRFA' | 'COREN' | 'OUTRO';

/** Usuário como a API devolve (CPF sempre mascarado). */
export interface Usuario {
  id: string;
  nomeCompleto: string;
  cpfMascarado: string | null;
  email: string;
  telefone: string | null;
  perfil: Perfil;
  status: StatusUsuario;
  areaAtuacao: AreaAtuacao | null;
  conselho: ConselhoProfissional | null;
  numeroRegistro: string | null;
  ufRegistro: string | null;
  trocarSenha: boolean;
  doisFatoresAtivo: boolean;
  criadoEm: string;
}

export interface NovoUsuario {
  nomeCompleto: string;
  cpf: string;
  email: string;
  telefone: string | null;
  perfil: Perfil;
  areaAtuacao: AreaAtuacao | null;
  conselho: ConselhoProfissional | null;
  numeroRegistro: string | null;
  ufRegistro: string | null;
  senhaProvisoria: string;
}
