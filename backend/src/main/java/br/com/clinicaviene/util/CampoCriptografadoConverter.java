package br.com.clinicaviene.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Cifra/decifra um campo de texto da entidade de forma transparente (AES-256-GCM — D-052). Usado no CPF
 * (paciente, responsável, usuário — D-061) e em segredos como o do TOTP.
 *
 * <p>Uso: {@code @Convert(converter = CampoCriptografadoConverter.class)}. É um bean do Spring (o Hibernate do
 * Spring Boot obtém conversores pelo contêiner do Spring), por isso recebe o {@link CifradorAesGcm} injetado.
 */
@Component
@Converter
public class CampoCriptografadoConverter implements AttributeConverter<String, String> {

    private final CifradorAesGcm cifrador;

    public CampoCriptografadoConverter(CifradorAesGcm cifrador) {
        this.cifrador = cifrador;
    }

    @Override
    public String convertToDatabaseColumn(String valor) {
        return cifrador.cifrar(valor);
    }

    @Override
    public String convertToEntityAttribute(String valorNoBanco) {
        return cifrador.decifrar(valorNoBanco);
    }
}
