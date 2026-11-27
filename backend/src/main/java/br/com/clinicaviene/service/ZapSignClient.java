package br.com.clinicaviene.service;

import br.com.clinicaviene.config.ZapSignProperties;
import br.com.clinicaviene.dto.ZapSignDocumentoResponse;
import br.com.clinicaviene.dto.ZapSignNovoDocumentoRequest;
import br.com.clinicaviene.dto.ZapSignSignatarioRequest;
import br.com.clinicaviene.exceptions.RegraNegocioException;
import br.com.clinicaviene.exceptions.ZapSignException;
import java.net.http.HttpClient;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Cliente da API da ZapSign (D-090, D-103) — só a estrutura: ainda não está ligado ao fluxo da ficha, que usa o
 * provedor interno. Com {@code clinicaviene.zapsign.habilitado=false} (padrão) recusa toda chamada sem sair da
 * aplicação. Nunca registra em log o token, o PDF ou dados do signatário.
 */
@Component
public class ZapSignClient {

    private final ZapSignProperties propriedades;
    private final RestClient http;

    public ZapSignClient(ZapSignProperties propriedades) {
        this.propriedades = propriedades;
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(propriedades.tempoLimite()).build());
        fabrica.setReadTimeout(propriedades.tempoLimite());
        this.http = RestClient.builder()
                .baseUrl(propriedades.urlApi())
                .requestFactory(fabrica)
                .build();
    }

    /**
     * Cria o documento na ZapSign com o PDF e devolve os signatários com o {@code sign_url} de cada um.
     *
     * @param idExterno id do {@code Documento} no sistema (vai em {@code external_id})
     */
    public ZapSignDocumentoResponse criarDocumento(
            String nome, byte[] pdf, String idExterno, List<ZapSignSignatarioRequest> signatarios) {
        conferirHabilitado();
        if (pdf == null || pdf.length == 0) {
            throw new RegraNegocioException("PDF vazio.");
        }
        if (pdf.length > propriedades.tamanhoMaximoPdf().toBytes()) {
            throw new RegraNegocioException("O PDF passa do limite de "
                    + propriedades.tamanhoMaximoPdf().toMegabytes() + " MB aceito pela plataforma de assinatura.");
        }
        if (signatarios == null || signatarios.isEmpty()) {
            throw new RegraNegocioException("Informe ao menos um signatário.");
        }
        var corpo = new ZapSignNovoDocumentoRequest(
                nome, Base64.getEncoder().encodeToString(pdf), "pt-br", idExterno, true, signatarios);
        return chamar(() -> http.post()
                .uri("/docs/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .body(ZapSignDocumentoResponse.class));
    }

    /** Situação atual do documento (inclui o link temporário do PDF assinado quando concluído). */
    public ZapSignDocumentoResponse detalharDocumento(String tokenDocumento) {
        conferirHabilitado();
        if (!StringUtils.hasText(tokenDocumento)) {
            throw new RegraNegocioException("Documento da plataforma de assinatura não informado.");
        }
        return chamar(() -> http.get()
                .uri("/docs/{token}/", tokenDocumento)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + propriedades.token())
                .retrieve()
                .body(ZapSignDocumentoResponse.class));
    }

    private void conferirHabilitado() {
        if (!propriedades.habilitado()) {
            throw new ZapSignException("Integração com a ZapSign desligada.");
        }
        if (!StringUtils.hasText(propriedades.token())) {
            throw new ZapSignException("Token da ZapSign não configurado.");
        }
    }

    private static ZapSignDocumentoResponse chamar(Supplier<ZapSignDocumentoResponse> chamada) {
        ZapSignDocumentoResponse resposta;
        try {
            resposta = chamada.get();
        } catch (RestClientException e) {
            // Só o tipo: a resposta da ZapSign pode repetir dados do signatário.
            throw new ZapSignException("Falha na comunicação com a ZapSign (" + e.getClass().getSimpleName() + ").", e);
        }
        if (resposta == null || !StringUtils.hasText(resposta.token())) {
            throw new ZapSignException("Resposta inesperada da ZapSign.");
        }
        return resposta;
    }
}
