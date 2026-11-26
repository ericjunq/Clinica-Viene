# ClinicaViene — Dúvidas abertas

> Dúvidas que surgiram durante o trabalho, para discutir com o autor.
> Depois de respondida, a dúvida vira decisão em `DECISOES.md` (aceita/recusada/a confirmar) e sai daqui.
> Formato: `Q-xxx — título` · data · contexto · opções · sugestão.

---

**Q-001 — Substituto do MinIO para storage S3 local** · 2026-11-26 · (D-056)
A imagem `minio/minio` não existe mais no Docker Hub nem no Quay (verificado hoje). O `compose.yaml` ficou sem storage; só é necessário na Fase 2 (PDFs).
Opções: **SeaweedFS** (leve, S3 compatível, ativo), **Garage** (leve, focado em self-host), **RustFS**, **LocalStack** (emula AWS, bom para testes), ou imagem MinIO antiga de terceiros (sem atualizações de segurança).
Sugestão: decidir junto com a hospedagem (D-041). Se produção for AWS/Cloudflare R2/Backblaze, usar LocalStack ou SeaweedFS só em dev/teste. O código usa o AWS SDK S3, que funciona com qualquer opção.

**Q-002 — CPF do usuário interno também cifrado + HMAC?** · 2026-11-26
D-052 cobre paciente e responsável legal. O `Usuario` faz login por CPF; para manter o mesmo nível de proteção, o CPF dele também pode ser cifrado (AES-GCM) e o login buscaria pelo `cpf_hash`.
Sugestão: sim, mesmo tratamento (custo baixo, padrão único).

**Q-003 — Renomear o pacote Java `com.github.ericjunq.Clinica_Viene`** · 2026-11-26
O Initializr gerou o pacote com maiúscula e underscore (fora da convenção Java: tudo minúsculo). Os módulos do Modulith serão subpacotes dele (`...Clinica_Viene.pacientes` etc.), então renomear depois custa mais.
Sugestão: renomear agora para `com.github.ericjunq.clinicaviene` (precisa de aprovação, mexe em todos os arquivos Java).

**Q-004 — Formatador do Spotless** · 2026-11-26
Configurei o Spotless com **palantir-java-format** (estilo do Google com 4 espaços e linha de 120), mas **não liguei o `spotless:check` no build**: os arquivos atuais usam tab (padrão do Initializr) e nesta máquina não há Java para rodar `./mvnw spotless:apply`. Se ligar o check agora, o CI quebra.
Opções: (a) palantir-java-format; (b) google-java-format (2 espaços); (c) outro.
Sugestão: (a). Em uma máquina com Java, rodar `./mvnw spotless:apply`, commitar (`style: aplica formatação do spotless`) e então ligar o check no CI.

**Q-005 — Pasta `docker/` ainda é necessária?** · 2026-11-26
O checklist previa `docker/`, mas o `compose.yaml` ficou na raiz (onde o Docker Compose e o `spring-boot-docker-compose` o encontram) e cada Dockerfile fica junto do seu projeto (`backend/Dockerfile`, futuramente `frontend/Dockerfile`).
Sugestão: só criar `docker/` se surgir algo específico (ex.: config do Nginx, scripts). Por ora, não criar.

**Q-006 — Modo de conclusão dos eventos do Spring Modulith** · 2026-11-26
Por padrão, eventos processados ficam na tabela `event_publication` marcados como concluídos (modo `UPDATE`). Há os modos `DELETE` (apaga) e `ARCHIVE` (move para `event_publication_archive`).
Sugestão: manter `UPDATE` por ora (não apaga nada — combina com auditoria) e definir política de limpeza depois. Atenção: eventos serializados podem conter dados pessoais — os eventos devem carregar só ids, nunca dados de paciente.

**Q-007 — Proteção da branch `main`** · 2026-11-26
Precisa ser configurada por você no GitHub (Settings → Branches → regra para `main`: exigir PR, exigir o check "Backend — testes" verde, bloquear push direto). Depois do primeiro run do CI, o nome do check aparece para seleção.

**Q-008 — Push não funciona nesta máquina: os testes ainda não rodaram em lugar nenhum** · 2026-11-26
Aqui não há `gh` nem credencial do GitHub configurada (`git push` falha pedindo usuário). Por isso **nenhuma branch foi enviada e o CI nunca rodou**: todo o código da Fase 0 está como "implementado — testes pendentes de execução".
Para enviar: configure a autenticação (`gh auth login`, ou um token em gerenciador de credenciais) e rode `git push -u origin <branch>` para as 4 branches, na ordem do CHECKLIST. A `ci-pipeline-inicial` contém todas as anteriores; o CI roda nela.
Testes escritos e **não executados**: `EntidadesLombokArchTest` (7 testes), `ModulosTest`, `ClinicaVieneApplicationIT`, `MigracoesIT` (2 testes); build das imagens Docker e validação do compose.
Executados de verdade nesta máquina: testes unitários do frontend (2 passaram) e `npm run build`.
A branch `frontend-lint` (topo da pilha) é a mais completa: contém todos os commits. Risco extra no build da imagem do backend: o `mvnw` baixa o Maven dentro do Alpine (depende de wget com HTTPS na imagem).
Pontos com mais risco de falhar na primeira execução (tudo escrito sem compilar): API do ArchUnit no `EntidadesLombokArchTest`; validação do Hibernate na tabela `event_publication`; versões novas (REST Assured 6.3.0, springdoc 3.2.1) com o Boot 4.2.0.

**Q-009 — Branches locais antigas** · 2026-11-26
As branches locais `backend-estrutura-pasta-backend`, `ci-testes-backend` e `docs-politica-migrations` (da primeira tentativa, nunca enviadas) foram substituídas pelas 4 da D-054. Posso apagá-las?

**Q-010 — Ordem dos PRs incluindo o frontend** · 2026-11-26
Além das 4 branches que você definiu, criei `frontend-projeto-angular` (gera o Angular, Dockerfile do frontend, job do frontend no CI). Também criei `frontend-lint` (angular-eslint + Prettier no CI). Ordem de merge: `docs-fundacao-projeto` → `backend-estrutura-monorepo` → `docker-compose-base` → `ci-pipeline-inicial` → `frontend-projeto-angular` → `frontend-lint`.
Alternativa: se preferir menos PRs, dá para abrir um PR só da última branch para a `main` (contém todos os commits, um por implementação).
