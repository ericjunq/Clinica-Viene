package br.com.clinicaviene.util;

import br.com.clinicaviene.dto.IntervaloDisponivelDto;
import br.com.clinicaviene.entity.DisponibilidadeProfissional;
import br.com.clinicaviene.entity.ExcecaoAgenda;
import br.com.clinicaviene.enums.TipoExcecaoAgenda;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Disponibilidade do profissional num período (D-106): horários fixos semanais (hora local da clínica) + horários
 * extras − ausências, unidos por consultório. Cálculo puro, sem banco.
 */
public final class DisponibilidadeCalculo {

    private DisponibilidadeCalculo() {}

    /**
     * @param de primeiro dia (inclusive), no fuso da clínica
     * @param ate último dia (inclusive)
     */
    public static List<IntervaloDisponivelDto> calcular(
            LocalDate de,
            LocalDate ate,
            ZoneId fuso,
            List<DisponibilidadeProfissional> fixos,
            List<ExcecaoAgenda> excecoes) {
        Map<UUID, List<Instant[]>> porConsultorio = new LinkedHashMap<>();
        for (LocalDate dia = de; !dia.isAfter(ate); dia = dia.plusDays(1)) {
            for (DisponibilidadeProfissional f : fixos) {
                if (f.dia() == dia.getDayOfWeek() && f.vigenteEm(dia)) {
                    adicionar(porConsultorio, f.getConsultorioId(),
                            dia.atTime(f.getHoraInicio()).atZone(fuso).toInstant(),
                            dia.atTime(f.getHoraFim()).atZone(fuso).toInstant());
                }
            }
        }
        Instant limiteInicio = de.atStartOfDay(fuso).toInstant();
        Instant limiteFim = ate.plusDays(1).atStartOfDay(fuso).toInstant();
        for (ExcecaoAgenda e : excecoes) {
            if (e.ativa() && e.getTipo() == TipoExcecaoAgenda.HORARIO_EXTRA) {
                Instant ini = max(e.getInicio(), limiteInicio);
                Instant fim = min(e.getFim(), limiteFim);
                if (ini.isBefore(fim)) {
                    adicionar(porConsultorio, e.getConsultorioId(), ini, fim);
                }
            }
        }
        List<IntervaloDisponivelDto> resultado = new ArrayList<>();
        porConsultorio.forEach((consultorio, intervalos) -> {
            List<Instant[]> livres = unir(intervalos);
            for (ExcecaoAgenda e : excecoes) {
                if (e.ativa()
                        && e.getTipo() == TipoExcecaoAgenda.AUSENCIA
                        && (e.getConsultorioId() == null || e.getConsultorioId().equals(consultorio))) {
                    livres = subtrair(livres, e.getInicio(), e.getFim());
                }
            }
            livres.forEach(i -> resultado.add(new IntervaloDisponivelDto(consultorio, i[0], i[1])));
        });
        resultado.sort(Comparator.comparing(IntervaloDisponivelDto::inicio)
                .thenComparing(i -> i.consultorioId().toString()));
        return resultado;
    }

    /** O período [inicio, fim) cabe inteiro num trecho disponível do consultório? */
    public static boolean cabe(List<IntervaloDisponivelDto> disponiveis, UUID consultorioId, Instant inicio, Instant fim) {
        return disponiveis.stream().anyMatch(i -> i.contem(consultorioId, inicio, fim));
    }

    private static void adicionar(Map<UUID, List<Instant[]>> mapa, UUID consultorio, Instant inicio, Instant fim) {
        mapa.computeIfAbsent(consultorio, k -> new ArrayList<>()).add(new Instant[] {inicio, fim});
    }

    /** Ordena e junta trechos que se tocam ou se sobrepõem. */
    static List<Instant[]> unir(List<Instant[]> intervalos) {
        List<Instant[]> ordenados = new ArrayList<>(intervalos);
        ordenados.sort(Comparator.comparing(i -> i[0]));
        List<Instant[]> unidos = new ArrayList<>();
        for (Instant[] i : ordenados) {
            if (!unidos.isEmpty() && !i[0].isAfter(unidos.getLast()[1])) {
                Instant[] ultimo = unidos.getLast();
                ultimo[1] = max(ultimo[1], i[1]);
            } else {
                unidos.add(new Instant[] {i[0], i[1]});
            }
        }
        return unidos;
    }

    static List<Instant[]> subtrair(List<Instant[]> intervalos, Instant inicio, Instant fim) {
        List<Instant[]> resultado = new ArrayList<>();
        for (Instant[] i : intervalos) {
            if (!fim.isAfter(i[0]) || !inicio.isBefore(i[1])) {
                resultado.add(i);
                continue;
            }
            if (i[0].isBefore(inicio)) {
                resultado.add(new Instant[] {i[0], inicio});
            }
            if (fim.isBefore(i[1])) {
                resultado.add(new Instant[] {fim, i[1]});
            }
        }
        return resultado;
    }

    private static Instant max(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant min(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }
}
