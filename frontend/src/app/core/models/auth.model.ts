import { Usuario } from './usuario.model';

export type EtapaLogin = 'CADASTRAR_2FA' | 'VERIFICAR_2FA';
export type MetodoDoisFatores = 'TOTP' | 'RECUPERACAO' | 'EMAIL';

export interface LoginResponse {
  desafio: string;
  etapa: EtapaLogin;
  expiraEm: string;
}

export interface CadastroDoisFatoresResponse {
  segredo: string;
  uriOtpAuth: string;
  /** PNG em data URI, pronto para um <img>. */
  qrCode: string;
}

export interface SessaoResponse {
  tokenAcesso: string;
  acessoExpiraEm: string;
  sessaoExpiraEm: string;
  usuario: Usuario;
  /** Só no cadastro do 2FA (mostrados uma única vez). */
  codigosRecuperacao: string[] | null;
}

export interface EnvioCodigoResponse {
  podeReenviarEm: string;
}

export interface MensagemResponse {
  mensagem: string;
}

/** Desafio em andamento entre a senha e o 2FA (só em memória). */
export interface DesafioAtual {
  token: string;
  etapa: EtapaLogin;
  expiraEm: string;
}
