/**
 * Token dos links enviados ao paciente (/cadastro#token, /documento#token). O que vem depois do # não é
 * enviado ao servidor; a página lê daqui e depois apaga da barra de endereço.
 */
export function tokenDoFragmento(fragmento: string | null): string | null {
  const token = fragmento?.trim();
  return token ? token : null;
}
