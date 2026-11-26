import { HttpErrorResponse } from '@angular/common/http';

/** Erro da API no formato Problem Details (RFC 9457). */
export interface ProblemDetail {
  status: number;
  detail?: string;
  erros?: { campo: string; mensagem: string }[];
}

/** Mensagem amigável para mostrar ao usuário a partir de um erro HTTP. */
export function mensagemDeErro(erro: unknown): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) {
      return 'Sem conexão com o servidor. Tente novamente.';
    }
    const problema = erro.error as ProblemDetail | null;
    if (erro.status === 429) {
      const segundos = Number(erro.headers.get('Retry-After'));
      return segundos > 0
        ? `Muitas tentativas. Tente novamente em ${formatarEspera(segundos)}.`
        : 'Muitas tentativas. Tente novamente mais tarde.';
    }
    if (problema?.erros?.length) {
      return problema.erros.map((e) => `${e.campo}: ${e.mensagem}`).join('; ');
    }
    if (problema?.detail) {
      return problema.detail;
    }
  }
  return 'Erro inesperado. Tente novamente.';
}

export function formatarEspera(segundos: number): string {
  if (segundos < 60) {
    return `${segundos} s`;
  }
  const minutos = Math.ceil(segundos / 60);
  return `${minutos} min`;
}
