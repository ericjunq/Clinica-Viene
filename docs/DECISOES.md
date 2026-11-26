# ClinicaViene — Registro de Decisões

> Todas as ideias discutidas: aceitas, recusadas e a confirmar, com o motivo.
> Atualizar a cada implementação/discussão. Nunca apagar uma decisão: se mudar, marcar como **SUBSTITUÍDA por D-xxx**.
> Princípios permanentes: **LGPD à risca** e **máxima integridade e segurança jurídica para a clínica**.

Formato: `D-xxx — Título` · Status · Data · Decisão · Motivo.

---

## ✅ Aceitas

**D-001 — Stack Java 25 + Spring Boot 4.2.x + Angular + PostgreSQL** · 2026-11-26
Motivo: preferência do autor; Java 25 é LTS e compatível com Boot 4.2. Se a 4.2.0 final não estiver disponível no Initializr, usar a 4.1.x mais recente e subir depois.
Atualização 2026-11-26: mantido Spring Boot **4.2.0** (GA). Não voltar para 4.1 por causa de dependências (ver D-049).

**D-002 — Monólito modular com Spring Modulith** · 2026-11-26
Motivo: um time pequeno não precisa de microsserviços; módulos com limites verificados em teste facilitam crescer e integrar chatbot/gateway depois.

**D-003 — Perfis DEV, MASTER, ADMIN, PROFISSIONAL com menor privilégio** · 2026-11-26
ADMIN não vê conteúdo clínico; DEV não acessa dados de pacientes em produção. Motivo: minimização (LGPD art. 6º, III) e redução de risco de vazamento.

**D-004 — Prontuário pertence ao paciente; acesso por vínculo profissional↔paciente** · 2026-11-26
Substitui a ideia original de prontuário vinculado a um único profissional. Motivo: paciente pode ser atendido por mais de um profissional (especialidades diferentes); transferência vira encerrar/abrir vínculo, preservando histórico.

**D-005 — Prontuário, anamnese e documentos assinados são imutáveis (correção por adendo)** · 2026-11-26
Motivo: valor probatório; prontuário é documento legal.

**D-006 — Auditoria de leitura e alteração de dados de saúde** · 2026-11-26
Motivo: responsabilização e prova em caso de incidente ou disputa judicial.

**D-007 — Acesso de emergência ("break-glass") com justificativa, prazo e aviso ao MASTER** · 2026-11-26
Motivo: atender paciente na ausência do profissional vinculado sem abrir mão do controle.

**D-008 — Cadastro com origem registrada: PACIENTE_LINK, PACIENTE_QUIOSQUE, RECEPCAO** · 2026-11-26
Motivo: provar quem forneceu as informações (risco de "eu disse que tinha alergia, mas marcaram que não").

**D-009 — Link de cadastro completo (dados + anamnese + termos + assinatura) vinculado a um pré-cadastro** · 2026-11-26
Recepção cria pré-cadastro mínimo → token de uso único, expira em 72h. Motivo: liga a identidade ao link, evita link genérico preenchido por terceiros e elimina erros de digitação da recepção.

**D-010 — Taxa calculada no modo "garantir o líquido" (gross-up)** · 2026-11-26
`valorCobrado = base / (1 − taxa)`. Motivo: a operadora cobra a taxa sobre o valor cobrado; somar a taxa por cima faz a clínica receber menos que o pretendido (ex.: R$ 1.000 em 3x → recebe R$ 996,25). Modo `ACRESCIMO_SIMPLES` mantido como opção configurável.

**D-011 — Desconto definido pela recepção (ADMIN/MASTER), por percentual ou valor, por item** · 2026-11-26
Motivo obrigatório; acima de limite configurável exige aprovação do MASTER. Desconto entra no cálculo (a observação antiga "desconto não entra em nenhum cálculo" era um bug).

**D-013 — Taxas e repasses configuráveis com vigência + snapshot no pagamento** · 2026-11-26
Motivo: mudar configuração não pode alterar pagamentos passados.

**D-014 — Taxa por forma de pagamento E por número de parcelas (1x–12x), com bandeira opcional e componente fixo** · 2026-11-26
Substitui a taxa única "crédito 2x ou mais = 5,99%". Motivo: operadoras cobram taxa diferente por quantidade de parcelas; boletos costumam ter tarifa fixa.

**D-017 — Orçamento com itens; N pagamentos por orçamento** · 2026-11-26
Substitui "1 pagamento por procedimento". Motivo: pacote de procedimentos pago junto e pagamento misto (PIX + cartão).

**D-018 — Status de pagamento PENDENTE/PARCIAL/QUITADO/CANCELADO/ESTORNADO, com parcelas pagas/restantes, valor por parcela e próximo vencimento sempre visíveis** · 2026-11-26
Cartão parcelado = QUITADO na aprovação (operadora assume); boleto/carnê = acompanhamento parcela a parcela.

**D-019 — Enums persistidos como STRING** · 2026-11-26
Substitui o uso de `ORDINAL` em `StatusPagamento`/`StatusSessao`. Motivo: reordenar enum corromperia dados silenciosamente.

**D-020 — `Sessao` renomeada para `Agendamento`, com consultório, início/fim e status completo** · 2026-11-26
Bloqueios de horário viram `BloqueioAgenda` (em vez de agendamento com paciente nulo).

**D-021 — Conflitos de agenda impedidos no banco (`EXCLUDE USING gist` + `tstzrange`)** · 2026-11-26
Motivo: garante que duas recepcionistas não marquem o mesmo horário simultaneamente.

**D-022 — Agenda com consultórios (N), disponibilidade recorrente e exceções** · 2026-11-26

**D-023 — Notificação de chegada em tempo real via WebSocket** · 2026-11-26

**D-024 — Menores de idade exigem responsável legal** · 2026-11-26 · Motivo: LGPD art. 14; ortodontia atende muitos menores.

**D-025 — Anamnese versionada com validade e revisão registrada pelo profissional** · 2026-11-26
Motivo: ficha antiga protege pouco; registrar a revisão mostra diligência do profissional.

**D-026 — E-mail não leva PDF com dado de saúde em anexo aberto (link autenticado ou PDF com senha)** · 2026-11-26
Motivo: dado sensível (LGPD art. 11 e 46); e-mail pode ir ao destinatário errado.

**D-027 — Base legal de saúde/obrigação legal para o atendimento; consentimento só para usos opcionais** · 2026-11-26
Motivo: se tudo dependesse de consentimento, a revogação inviabilizaria guardar o prontuário.

**D-028 — Confirmação por e-mail/SMS não é obrigatória para todos** · 2026-11-26
Obrigatória só para cadastro via link. Motivo: pacientes da zona rural sem celular.

**D-029 — Assinatura: interface `ProvedorAssinatura`; preferência por plataforma existente; tablet na recepção se possível** · 2026-11-26
Profissional preferencialmente com ICP-Brasil (Resolução CFO 91/2009 – NGS2).

**D-030 — Pontos de extensão: `GatewayPagamento`, `CanalNotificacao`, chatbot via casos de uso com mesmas permissões** · 2026-11-26

**D-031 — Testes reais: unitário, integração (Testcontainers), E2E (Playwright), CI no GitHub Actions** · 2026-11-26

**D-032 — Git: branch por implementação (`backend-…`, `frontend-…`, `docker-…`, `ci-…`, `docs-…`), commits `tipo: descrição`, SEM coautoria de IA** · 2026-11-26
Reforçado por `.claude/settings.json` (`attribution` vazio) e `CLAUDE.md`.

**D-033 — Contexto entre máquinas via `CLAUDE.md` + `docs/CONTEXTO.md` + `docs/DECISOES.md` + `docs/CHECKLIST.md` versionados no repositório** · 2026-11-26
Motivo: chats não são compartilhados entre máquinas; o repositório é.

**D-034 — Construção em fases (não tudo de uma vez)** · 2026-11-26

**D-044 — Monorepo: raiz `Clinica-Viene/`, backend em `backend/`** · 2026-11-26
Projeto Maven (`pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/`, `src/`) movido para `backend/`; raiz fica com `CLAUDE.md`, `docs/`, `.claude/`, `compose.yaml`, `.env.example`, `.github/` e `.gitignore` geral (segredos, dumps, IDE). Caminhos de build ajustados no compose e no CI. Motivo: separar backend, frontend, docker e CI no mesmo repositório.

**D-045 — Sessões declaram o ambiente (completo ou só construção de código)** · 2026-11-26
No início de cada sessão o Claude pergunta se a máquina tem JDK 25/Docker/Node. Sem ambiente, não executa build/testes e marca itens como "testes pendentes de execução". Motivo: o autor trabalha em máquinas sem Java; evita tentativas de execução e garante honestidade sobre testes.

**D-046 — Começar só com CI (testes a cada push); CD adiado** · 2026-11-26
Workflow `.github/workflows/ci.yml` roda `./mvnw verify` (unitários + integração com Testcontainers) em todo push, de qualquer branch, e em PRs para a `main`. O CI entra na Fase 0 e é o **lugar oficial dos testes**: só se considera teste aprovado com CI verde. Após cada push, o Claude acompanha a execução (`gh run watch`, `gh run view --log-failed`, ou a API pública do GitHub quando não houver `gh`), corrige falhas e reenvia. Frontend, E2E, lint, cobertura, scan e build de imagem entram no CI conforme forem criados. **CD (publicar imagem no GHCR / deploy) fica para quando o projeto estiver avançado** e a hospedagem estiver definida (D-041). Motivo: ainda não há deploy, então CD agora não teria função; e, sem JDK em algumas máquinas do autor, o CI é onde os testes efetivamente rodam.

**D-047 — Política de migrations Flyway** · 2026-11-26
- Migrations em SQL puro (`backend/src/main/resources/db/migration/`).
- Em ambiente de dev: **apenas `CREATE`** (tabelas, índices, constraints, extensões).
- `INSERT` de **dados fictícios** permitido **só para testes**, em `backend/src/test/resources/db/testdata/`, incluído na `spring.flyway.locations` apenas no perfil de teste. Nunca em `src/main`.
- `ALTER`, `UPDATE`, `DELETE`, `DROP` e qualquer outra alteração: **só com aprovação explícita do autor**.
Motivo: controle total do autor sobre o schema; dados fictícios nunca chegam a produção (CLAUDE.md: nenhum dado real fora de produção, nenhum dado de teste dentro dela); evita alterações silenciosas de estrutura ou de dados.

---

**D-043 — UI: PrimeNG + FullCalendar** · 2026-11-26 · (saiu de A CONFIRMAR)
PrimeNG (versão compatível com o Angular instalado) como biblioteca de componentes e FullCalendar (`@fullcalendar/angular`) para a agenda. Não bloqueia o `ng new`; as bibliotecas entram quando começarem as telas. Motivo: PrimeNG tem tabelas, formulários e componentes de calendário prontos para sistema administrativo; FullCalendar resolve visões de agenda por dia/semana/recurso (consultório/profissional).

**D-049 — Spring Modulith 2.2.0 (GA) via `spring-modulith-bom`** · 2026-11-26
O projeto foi gerado com `2.2.0-M1`; a 2.2.0 GA existe no Maven Central e foi adotada. Tabela `event_publication` (registro de eventos do Modulith, schema v2 para PostgreSQL) criada por migration Flyway, porque `ddl-auto=validate`. Motivo: não usar versão milestone; schema sempre via Flyway.

**D-050 — Docker Compose de dev: imagens fixadas, credenciais no `.env`, extensões via Flyway** · 2026-11-26
- `compose.yaml` na raiz. Postgres fixado em `postgres:18.7-alpine` (nada de `latest`); o mesmo vale para os testes (Testcontainers).
- Credenciais via variáveis do `.env` (no `.gitignore`); `.env.example` versionado com valores fictícios.
- Extensão `btree_gist` criada por migration Flyway (`CREATE EXTENSION IF NOT EXISTS btree_gist;`), não por script de init do Docker, para valer em todos os ambientes (dev, teste, produção).
- O serviço `backend` fica no profile `app` do compose: `docker compose up` sobe só a infraestrutura (usada pelo `spring-boot-docker-compose` ao rodar pela IDE); `docker compose --profile app up` sobe tudo.
Motivo: builds reprodutíveis, nenhum segredo no Git, e a extensão necessária para a exclusão de conflitos de agenda (D-021) existir em qualquer banco.

**D-051 — Dependências e regras do Lombok** · 2026-11-26
- Removido `thymeleaf-extras-springsecurity6` (Thymeleaf só para PDF/e-mail). Mantido `spring-boot-devtools`.
- Lombok mantido (versão gerenciada pelo Boot 4.2.0: 1.18.48, compatível com Java 25), com regras:
  - Permitido: `@Getter`, `@Setter` (só onde necessário), `@NoArgsConstructor(access = PROTECTED)` em entidades, `@RequiredArgsConstructor` em services, `@Slf4j`, `@Builder` pontual.
  - Proibido: `@Data`, `@EqualsAndHashCode`, `@ToString` e `@AllArgsConstructor` em entidades JPA; `@Value` (usar `record`).
  - `equals`/`hashCode` das entidades escritos à mão, baseados apenas no id. DTOs sempre `record`.
  - `annotationProcessorPaths` na ordem: `lombok`, `lombok-mapstruct-binding`, `mapstruct-processor`.
  - `lombok.config` com `config.stopBubbling = true` e `lombok.addLombokGeneratedAnnotation = true` (JaCoCo ignora código gerado).
  - Teste ArchUnit falha se `@Data`, `@EqualsAndHashCode`, `@ToString` ou `@AllArgsConstructor` forem usados em `@Entity`, ou `@Value` em qualquer classe.
Motivo: `@Data`/`@EqualsAndHashCode`/`@ToString` em entidades causam bugs clássicos de JPA (lazy loading disparado, recursão infinita, hashCode instável em coleções) e podem vazar dado pessoal em log via `toString`.

**D-052 — CPF criptografado (AES-GCM) + `cpf_hash` (HMAC-SHA256) para unicidade e busca** · 2026-11-26
- CPF guardado criptografado com AES-256-GCM via `AttributeConverter` (IV aleatório por valor), com **chave versionada** vinda do ambiente (o valor cifrado carrega o id da versão da chave → permite rotação).
- Coluna `cpf_hash` = HMAC-SHA256 do CPF normalizado (só dígitos) com chave secreta própria (diferente da de criptografia). **Não usar SHA puro**: CPF tem ~1 bilhão de combinações e seria revertido por força bruta.
- Índice único parcial em `cpf_hash` (`WHERE cpf_hash IS NOT NULL`). Busca por CPF = calcular o HMAC e buscar pelo hash.
- Mesmo tratamento para o CPF do responsável legal (e, por coerência, do `Usuario` — ver DUVIDAS).
- **Chave perdida = dado irrecuperável**: backup das chaves separado do backup do banco, procedimento de rotação documentado (CONTEXTO §11).
Motivo: CPF é identificador forte; criptografia em repouso limita o dano de vazamento do banco (LGPD art. 46), e o HMAC mantém unicidade e busca sem expor o valor.

**D-053 — Paciente sem CPF** · 2026-11-26
- CPF **opcional para menores de 18 anos**; nesse caso o CPF do responsável legal é obrigatório.
- Adulto continua com CPF obrigatório; exceção só aprovada pelo MASTER, com justificativa e registro em auditoria.
- Sem CPF: alerta de possível duplicidade quando nome + data de nascimento + responsável coincidirem.
Motivo: bebês e crianças frequentemente não têm CPF; o alerta reduz cadastros duplicados sem bloquear o atendimento.

**D-054 — Branches da Fase 0 empilhadas, na ordem definida pelo autor** · 2026-11-26
Ordem: 1) `docs-fundacao-projeto` → 2) `backend-estrutura-monorepo` → 3) `docker-compose-base` → 4) `ci-pipeline-inicial`. Cada uma criada a partir da anterior (a `main` ainda não tem nada disso); PRs devem ser mergeados nessa ordem. Substitui as branches locais provisórias `backend-estrutura-pasta-backend`, `ci-testes-backend` e `docs-politica-migrations` (nunca publicadas).
Motivo: o autor não está presente para mergear entre uma e outra; empilhar mantém uma branch por implementação sem bloquear o trabalho.

**D-057 — Frontend: Angular 22.3, zoneless, sem SSR, servido por nginx com `/api/` no mesmo domínio** · 2026-11-26
- Gerado com `ng new` (Angular CLI 22.3.0): standalone, **zoneless** (padrão atual do Angular, detecção de mudanças por signals), SCSS, Vitest, roteamento, **sem SSR** (sistema interno autenticado; SEO irrelevante e SSR traria um servidor Node a mais para proteger).
- Produção/compose: imagem `nginxinc/nginx-unprivileged` (sem root); o nginx encaminha `/api/` para o backend → mesmo domínio, sem CORS, cookies/tokens mais simples de proteger. Em dev, `ng serve` usa `proxy.conf.json` com o mesmo efeito.
- Branch `frontend-projeto-angular`, empilhada sobre `ci-pipeline-inicial` (D-054).
Motivo: stack atual e enxuta; menos superfície de ataque.

**D-055 — `docs/DUVIDAS.md` para dúvidas abertas** · 2026-11-26
Dúvidas que surgirem com o autor ausente vão para `docs/DUVIDAS.md`; depois de discutidas, viram decisão em `DECISOES.md` e saem de lá.

## ❌ Recusadas

**R-001 — "Simular" execução de testes** · 2026-11-26
Motivo: um teste "simulado" é um resultado inventado. Testes só contam quando executados (localmente com JDK/Docker ou no CI).

**R-002 — Prontuário vinculado a um único profissional** · 2026-11-26 · Substituída por D-004.

**R-003 — Enums com `EnumType.ORDINAL`** · 2026-11-26 · Substituída por D-019.

**R-004 — 1 pagamento por procedimento (FK única)** · 2026-11-26 · Substituída por D-017.

**R-005 — Somar a taxa "por cima" como padrão** · 2026-11-26 · Substituída por D-010 (continua disponível como modo opcional).

**R-006 — Taxa única para "crédito 2x ou mais"** · 2026-11-26 · Substituída por D-014.

**R-007 — Enviar documentos clínicos como anexo comum por e-mail** · 2026-11-26 · Substituída por D-026.

**R-008 — Perfil DEV com acesso total a dados de pacientes em produção** · 2026-11-26 · Substituída por D-003.

**R-009 — Exigir confirmação por e-mail/SMS de todos os pacientes para concluir cadastro** · 2026-11-26 · Substituída por D-028.

**R-010 — Link de cadastro genérico (não vinculado a pré-cadastro)** · 2026-11-26 · Substituída por D-009.

**R-011 — Guardar refresh token em texto puro e URL pública de documento** · 2026-11-26
Motivo: vazamento do banco exporia sessões e documentos. Guardar hash do token e chave de storage privado.

**R-012 — Hash SHA-256 puro do CPF** · 2026-11-26 · Substituída por D-052 (HMAC com chave secreta).
Motivo: espaço de ~10⁹ CPFs é quebrável por força bruta em pouco tempo.

**R-013 — Criar extensões do Postgres por script de init do Docker** · 2026-11-26 · Substituída por D-050.
Motivo: só valeria no container de dev; produção e testes ficariam sem a extensão.

**R-014 — Imagens Docker com tag `latest`** · 2026-11-26 · Substituída por D-050.

---

## ⏳ A confirmar

**D-012 — Base do repasse ao profissional** · A CONFIRMAR (com a dona da clínica) · 2026-11-26
Regra provisória: `valorProfissional = valorProcedimento (cheio) × %profissional`, independente de desconto e taxa. Consequência: **a clínica arca integralmente com o desconto**. Implementar configurável (`BASE_REPASSE = VALOR_PROCEDIMENTO | VALOR_COM_DESCONTO`).

**D-035 — Momento do repasse em pagamentos parcelados próprios (boleto/carnê)** · A CONFIRMAR
Repasse integral na contratação ou proporcional às parcelas recebidas? O que acontece em inadimplência?

**D-015 — Provedor da maquininha e tabela real de taxas** · A CONFIRMAR
Seed atual usa tabela pública da InfinitePay (até R$ 20 mil/mês, Visa/Master, 26/11/2026) apenas como referência.

**D-036 — Tarifa do boleto e juros do carnê próprio** · A CONFIRMAR
Seed: boleto 5% (valor legado), juros 0% a.m.

**D-016 — Plataforma de assinatura (Clicksign, ZapSign, D4Sign, outra)** · A CONFIRMAR
Critérios: custo por documento, API, assinatura presencial em tablet, pacote de evidências, aceitação de ICP-Brasil.

**D-037 — Aquisição de tablet para a recepção** · A CONFIRMAR

**D-038 — Limite de desconto sem aprovação do MASTER** · A CONFIRMAR (sugestão: 10%)

**D-039 — Prazo de validade da anamnese** · A CONFIRMAR (sugestão: 12 meses)

**D-040 — Prazo de retenção do prontuário e política de retenção dos demais dados** · A CONFIRMAR com advogado

**D-041 — Hospedagem (provedor, região) e backups** · A CONFIRMAR

**D-042 — Ações permitidas ao chatbot da recepção** · A CONFIRMAR (discutir quando chegar a fase)

**D-043 — Biblioteca de UI** · ~~A CONFIRMAR~~ → decidida em 2026-11-26 (ver Aceitas: PrimeNG + FullCalendar).

**D-048 — Como carregar dados de configuração reais (seeds) em produção** · A CONFIRMAR · 2026-11-26
A D-047 só permite `INSERT` de dados fictícios em testes, mas o sistema precisa de configurações iniciais reais: taxas (§8.5), repasses (§8.6), modelo de anamnese, termos LGPD e o primeiro usuário MASTER. Opções: (a) migration de `INSERT` específica, aprovada caso a caso; (b) cadastro pela tela de configurações/endpoint administrativo, sem seed no banco; (c) inicializador na aplicação (ex.: `ApplicationRunner`) idempotente. Decidir antes da Fase 1 (primeiro usuário) e da Fase 5 (taxas/repasses).

**D-056 — Storage S3 local (substituto do MinIO)** · A CONFIRMAR · 2026-11-26
A imagem oficial `minio/minio` não está mais disponível no Docker Hub nem no Quay (verificado em 26/11/2026). O compose de dev ficou **sem** storage por enquanto (só será necessário na Fase 2 — PDFs). Opções: SeaweedFS, Garage, RustFS, LocalStack (S3), ou fixar uma imagem MinIO antiga de terceiros. O código usará o AWS SDK S3 (compatível com qualquer uma). Ver `docs/DUVIDAS.md`.
