import { createHmac } from 'node:crypto';

const ALFABETO = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ234567';

function deBase32(texto: string): Buffer {
  const limpo = texto.replace(/[=\s]/g, '').toUpperCase();
  const bytes: number[] = [];
  let buffer = 0;
  let bits = 0;
  for (const c of limpo) {
    buffer = (buffer << 5) | ALFABETO.indexOf(c);
    bits += 5;
    if (bits >= 8) {
      bytes.push((buffer >> (bits - 8)) & 0xff);
      bits -= 8;
    }
  }
  return Buffer.from(bytes);
}

/** Código TOTP (RFC 6238, SHA-1, 30 s, 6 dígitos) — o mesmo que o app autenticador mostraria. */
export function codigoTotp(segredoBase32: string, instanteMs = Date.now()): string {
  const passo = Buffer.alloc(8);
  passo.writeBigUInt64BE(BigInt(Math.floor(instanteMs / 1000 / 30)));
  const hash = createHmac('sha1', deBase32(segredoBase32)).update(passo).digest();
  const deslocamento = hash[hash.length - 1] & 0x0f;
  const binario =
    ((hash[deslocamento] & 0x7f) << 24) |
    (hash[deslocamento + 1] << 16) |
    (hash[deslocamento + 2] << 8) |
    hash[deslocamento + 3];
  return String(binario % 1_000_000).padStart(6, '0');
}
