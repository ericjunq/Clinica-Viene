/**
 * Cálculos do calendário da agenda (D-085): tempo da clínica e posição dos blocos. Funções puras, testadas.
 *
 * A clínica fica em America/Bahia, que não tem horário de verão desde 2012: o fuso é fixo em UTC−3. Assim o
 * calendário mostra sempre a hora da clínica, qualquer que seja o fuso do computador.
 */
export const OFFSET_CLINICA_MINUTOS = -180;
const MS_MINUTO = 60_000;

/** Data (aaaa-mm-dd) na clínica de um instante. */
export function dataNaClinica(instante: string | Date): string {
  const d = new Date(new Date(instante).getTime() + OFFSET_CLINICA_MINUTOS * MS_MINUTO);
  return d.toISOString().slice(0, 10);
}

/** Minutos desde a meia-noite da clínica, no dia {@code data}. Pode passar de 1440 se o instante for no dia seguinte. */
export function minutosNoDia(instante: string, data: string): number {
  return Math.round(
    (new Date(instante).getTime() - instanteDaClinica(data, 0).getTime()) / MS_MINUTO,
  );
}

/** Instante de {@code data} às {@code minutos} (hora da clínica). */
export function instanteDaClinica(data: string, minutos: number): Date {
  const [ano, mes, dia] = data.split('-').map(Number);
  return new Date(Date.UTC(ano, mes - 1, dia) + (minutos - OFFSET_CLINICA_MINUTOS) * MS_MINUTO);
}

export function hojeNaClinica(agora: Date = new Date()): string {
  return dataNaClinica(agora);
}

export function somarDias(data: string, dias: number): string {
  const [ano, mes, dia] = data.split('-').map(Number);
  return new Date(Date.UTC(ano, mes - 1, dia + dias)).toISOString().slice(0, 10);
}

/** "08:30" a partir de minutos no dia. */
export function formatarHora(minutos: number): string {
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
}

/** Minutos de "08:30". */
export function minutosDaHora(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
}

/** Arredonda para baixo na grade (ex.: 8:40 → 8:30 com grade de 15). */
export function naGrade(minutos: number, grade: number): number {
  return Math.floor(minutos / grade) * grade;
}

/**
 * Faixa de horas mostrada no dia: padrão 7h–19h, ampliada para caber disponibilidade e consultas (sempre em horas
 * cheias).
 */
export function faixaDoDia(
  trechos: { inicio: number; fim: number }[],
  padrao: { inicio: number; fim: number } = { inicio: 7 * 60, fim: 19 * 60 },
): { inicio: number; fim: number } {
  let inicio = padrao.inicio;
  let fim = padrao.fim;
  for (const t of trechos) {
    inicio = Math.min(inicio, Math.max(0, t.inicio));
    fim = Math.max(fim, Math.min(24 * 60, t.fim));
  }
  return { inicio: Math.floor(inicio / 60) * 60, fim: Math.ceil(fim / 60) * 60 };
}

/** Posição vertical (em px) de um trecho dentro da faixa do dia. */
export function posicao(
  inicio: number,
  fim: number,
  faixa: { inicio: number; fim: number },
  pxPorMinuto: number,
): { topo: number; altura: number } {
  const ini = Math.max(inicio, faixa.inicio);
  const f = Math.min(fim, faixa.fim);
  return { topo: (ini - faixa.inicio) * pxPorMinuto, altura: Math.max(0, f - ini) * pxPorMinuto };
}

/**
 * Divide a largura entre blocos que se sobrepõem (ex.: uma consulta cancelada e a que ocupou o horário): cada bloco
 * recebe uma raia e o total de raias do seu grupo.
 */
export function distribuirRaias<T extends { inicio: number; fim: number }>(
  blocos: T[],
): (T & { raia: number; raias: number })[] {
  const ordenados = [...blocos].sort((a, b) => a.inicio - b.inicio || b.fim - a.fim);
  const resultado: (T & { raia: number; raias: number })[] = [];
  let grupo: (T & { raia: number; raias: number })[] = [];
  let fimDoGrupo = -Infinity;
  const fecharGrupo = () => {
    const raias = grupo.reduce((max, b) => Math.max(max, b.raia + 1), 0);
    grupo.forEach((b) => (b.raias = raias));
    resultado.push(...grupo);
    grupo = [];
  };
  for (const bloco of ordenados) {
    if (bloco.inicio >= fimDoGrupo && grupo.length) {
      fecharGrupo();
    }
    const ocupadas = grupo.filter((b) => b.fim > bloco.inicio).map((b) => b.raia);
    let raia = 0;
    while (ocupadas.includes(raia)) {
      raia++;
    }
    grupo.push({ ...bloco, raia, raias: 1 });
    fimDoGrupo = grupo.length === 1 ? bloco.fim : Math.max(fimDoGrupo, bloco.fim);
  }
  if (grupo.length) {
    fecharGrupo();
  }
  return resultado;
}

/** O trecho [inicio, fim) cabe inteiro em algum trecho disponível? */
export function cabeEm(
  disponiveis: { inicio: number; fim: number }[],
  inicio: number,
  fim: number,
): boolean {
  return disponiveis.some((d) => inicio >= d.inicio && fim <= d.fim);
}
