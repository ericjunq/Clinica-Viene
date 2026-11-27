package br.com.clinicaviene.service;

import br.com.clinicaviene.config.ArmazenamentoProperties;
import br.com.clinicaviene.util.CifradorAesGcm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Documentos no storage privado (CONTEXTO §9): cifrados pela própria aplicação (AES-256-GCM, chave versionada)
 * antes de sair do servidor — o storage só vê bytes cifrados, seja qual for o provedor. Referência sempre pela
 * chave, nunca por URL pública.
 */
@Service
@RequiredArgsConstructor
public class ArmazenamentoService {

    private static final String TIPO = "application/octet-stream";

    private final S3Client s3Client;
    private final ArmazenamentoProperties propriedades;
    private final CifradorAesGcm cifrador;
    private volatile boolean bucketConferido;

    public void guardar(String chave, byte[] conteudo) {
        garantirBucket();
        s3Client.putObject(
                r -> r.bucket(propriedades.bucket()).key(chave).contentType(TIPO),
                RequestBody.fromBytes(cifrador.cifrarBytes(conteudo)));
    }

    public byte[] ler(String chave) {
        return cifrador.decifrarBytes(
                s3Client.getObjectAsBytes(r -> r.bucket(propriedades.bucket()).key(chave)).asByteArray());
    }

    /** Em dev/teste cria o bucket na primeira gravação (o storage pode subir depois do backend). */
    private void garantirBucket() {
        if (bucketConferido || !propriedades.criarBucket()) {
            return;
        }
        synchronized (this) {
            if (bucketConferido) {
                return;
            }
            try {
                s3Client.headBucket(r -> r.bucket(propriedades.bucket()));
            } catch (NoSuchBucketException e) {
                s3Client.createBucket(r -> r.bucket(propriedades.bucket()));
            }
            bucketConferido = true;
        }
    }
}
