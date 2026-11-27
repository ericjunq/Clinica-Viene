import { AreaAtuacao } from './usuario.model';

/** Agenda (Fase 3, D-106). Horários e datas da clínica sempre em America/Bahia. */

export type DiaSemana =
  'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export const DIAS_SEMANA: { valor: DiaSemana; rotulo: string }[] = [
  { valor: 'MONDAY', rotulo: 'Segunda' },
  { valor: 'TUESDAY', rotulo: 'Terça' },
  { valor: 'WEDNESDAY', rotulo: 'Quarta' },
  { valor: 'THURSDAY', rotulo: 'Quinta' },
  { valor: 'FRIDAY', rotulo: 'Sexta' },
  { valor: 'SATURDAY', rotulo: 'Sábado' },
  { valor: 'SUNDAY', rotulo: 'Domingo' },
];

export interface Consultorio {
  id: string;
  nome: string;
  ativo: boolean;
}

export interface CategoriaProcedimento {
  id: string;
  areaAtuacao: AreaAtuacao;
  codigo: string;
  nome: string;
  ativa: boolean;
}

export interface Profissional {
  id: string;
  nomeCompleto: string;
  areaAtuacao: AreaAtuacao | null;
}

export interface HorarioFixo {
  id: string;
  consultorioId: string;
  diaSemana: DiaSemana;
  horaInicio: string;
  horaFim: string;
  vigenciaInicio: string;
  vigenciaFim: string | null;
}

export type TipoExcecao = 'HORARIO_EXTRA' | 'AUSENCIA';

export interface ExcecaoAgenda {
  id: string;
  consultorioId: string | null;
  tipo: TipoExcecao;
  inicio: string;
  fim: string;
  motivo: string | null;
}

export interface HorariosProfissional {
  profissional: Profissional;
  fixos: HorarioFixo[];
  excecoes: ExcecaoAgenda[];
}

export interface IntervaloDisponivel {
  consultorioId: string;
  inicio: string;
  fim: string;
}

export type StatusAgendamento =
  | 'AGENDADO'
  | 'CONFIRMADO'
  | 'PACIENTE_CHEGOU'
  | 'EM_ATENDIMENTO'
  | 'CONCLUIDO'
  | 'FALTOU'
  | 'CANCELADO';

export const ROTULO_STATUS: Record<StatusAgendamento, string> = {
  AGENDADO: 'Agendado',
  CONFIRMADO: 'Confirmado',
  PACIENTE_CHEGOU: 'Chegou',
  EM_ATENDIMENTO: 'Em atendimento',
  CONCLUIDO: 'Concluído',
  FALTOU: 'Faltou',
  CANCELADO: 'Cancelado',
};

export interface Agendamento {
  id: string;
  paciente: { id: string; nomeCompleto: string; fichaPendente: boolean };
  profissional: { id: string; nome: string };
  consultorio: { id: string; nome: string };
  inicio: string;
  fim: string;
  status: StatusAgendamento;
  observacao: string | null;
  itens: {
    categoriaId: string;
    categoriaCodigo: string;
    categoriaNome: string;
    descricao: string | null;
  }[];
  agendamentoOrigemId: string | null;
  motivoCancelamento: string | null;
  chegadaEm: string | null;
  criadoEm: string;
}

export interface ItemAgendamento {
  categoriaId: string;
  descricao: string | null;
}

export interface NovoAgendamento {
  pacienteId: string;
  profissionalId: string;
  consultorioId: string;
  inicio: string;
  duracaoMinutos: number;
  itens: ItemAgendamento[];
  observacao: string | null;
}

/** Grade da agenda (D-106): horários de 15 em 15 min; consulta padrão de 30 min. */
export const GRADE_MINUTOS = 15;
export const DURACAO_PADRAO_MINUTOS = 30;
