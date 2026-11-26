package br.com.clinicaviene.compartilhado.cripto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * Cifra/decifra um campo de texto da entidade de forma transparente (AES-256-GCM — D-052). Usado no CPF
 * (paciente, responsável, usuário — D-061) e em segredos como o do TOTP.
 *
 * <p>Uso: {@code @Convert(converter = CampoCifradoConverter.class)}. É um bean do Spring (o Hibernate do
 * Spring Boot obtém conversores pelo contêiner do Spring), por isso recebe o {@link Cifrador} injetado.
 */
@Component
@Converter
public class CampoCifradoConverter implements AttributeConverter<String, String> {

    private final Cifrador cifrador;

    public CampoCifradoConverter(Cifrador cifrador) {
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
