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

**D-002 — Monólito modular com Spring Modulith** · 2026-11-26 · **SUBSTITUÍDA por D-078** (arquitetura em camadas; o Spring Modulith segue só para o registro de eventos — D-059)
Motivo: um time pequeno não precisa de microsserviços; módulos com limites verificados em teste facilitam crescer e integrar chatbot/gateway depois.

**D-003 — Perfis DEV, MASTER, ADMIN, PROFISSIONAL com menor privilégio** · 2026-11-26
ADMIN não vê conteúdo clínico; DEV não acessa dados de pacientes em produção. Motivo: minimização (LGPD art. 6º, III) e redução de risco de vazamento.

**D-004 — Prontuário pertence ao paciente; acesso por vínculo profissional↔paciente** · 2026-11-26
Substitui a ideia original de prontuário vinculado a um único profissional. Motivo: paciente pode ser atendido por mais de um profissional (especialidades diferentes); transferência vira encerrar/abrir vínculo, preservando histórico.

**D-005 — Prontuário, anamnese e documentos assinados são imutáveis (correção por adendo)** · 2026-11-26
Motivo: valor probatório; prontuário é documento legal.

**D-006 — Auditoria de leitura e alteração de dados de saúde** · 2026-11-26
Motivo: responsabilização e prova em caso de incidente ou disputa judicial.

**D-007 — Acesso de emergência ("break-glass") com justificativa, prazo e aviso ao MASTER** · 2026-11-26 · **Ajustada pela D-088**: só profissionais de saúde usam o acesso de emergência (MASTER/ADMIN/DEV nunca leem conteúdo clínico)
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

**D-032 — Git: branch por implementação (`backend-…`, `frontend-…`, `docker-…`, `ci-…`, `docs-…`), commits `tipo: descrição`, SEM coautoria de IA** · 2026-11-26 · Parte das branches **SUBSTITUÍDA por D-058** (commits e sem coautoria continuam valendo)
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

**D-043 — UI: PrimeNG + FullCalendar** · 2026-11-26 · (saiu de A CONFIRMAR) · **Parte do PrimeNG SUBSTITUÍDA por D-084** (Angular Material); **parte do FullCalendar SUBSTITUÍDA por D-085** (calendário próprio)
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

**D-054 — Branches da Fase 0 empilhadas, na ordem definida pelo autor** · 2026-11-26 · **SUBSTITUÍDA por D-058** (as 7 branches foram integradas na `main` por fast-forward, com CI verde)
Ordem: 1) `docs-fundacao-projeto` → 2) `backend-estrutura-monorepo` → 3) `docker-compose-base` → 4) `ci-pipeline-inicial`. Cada uma criada a partir da anterior (a `main` ainda não tem nada disso); PRs devem ser mergeados nessa ordem. Substitui as branches locais provisórias `backend-estrutura-pasta-backend`, `ci-testes-backend` e `docs-politica-migrations` (nunca publicadas).
Motivo: o autor não está presente para mergear entre uma e outra; empilhar mantém uma branch por implementação sem bloquear o trabalho.

**D-057 — Frontend: Angular 22.3, zoneless, sem SSR, servido por nginx com `/api/` no mesmo domínio** · 2026-11-26
- Gerado com `ng new` (Angular CLI 22.3.0): standalone, **zoneless** (padrão atual do Angular, detecção de mudanças por signals), SCSS, Vitest, roteamento, **sem SSR** (sistema interno autenticado; SEO irrelevante e SSR traria um servidor Node a mais para proteger).
- Produção/compose: imagem `nginxinc/nginx-unprivileged` (sem root); o nginx encaminha `/api/` para o backend → mesmo domínio, sem CORS, cookies/tokens mais simples de proteger. Em dev, `ng serve` usa `proxy.conf.json` com o mesmo efeito.
- Branch `frontend-projeto-angular`, empilhada sobre `ci-pipeline-inicial` (D-054).
Motivo: stack atual e enxuta; menos superfície de ataque.

**D-058 — Desenvolvimento direto na `main`, sem branches e sem PRs** · 2026-11-26
Substitui a parte de branches da D-032 e a D-054. Commits continuam no formato `tipo: descrição`, um por implementação/alteração, sem coautoria; push direto na `main`. O CI roda a cada push e continua sendo o lugar oficial dos testes; CI vermelho na `main` é corrigido antes de qualquer outra implementação. A proteção da `main` (Q-007) deixa de exigir PR.
Motivo: decisão do autor — projeto de uma pessoa só; branches empilhadas e PRs davam trabalho sem revisor para justificá-los.

**D-056 — SeaweedFS como storage S3 de desenvolvimento e teste** · 2026-11-26 · (saiu de A CONFIRMAR)
- Substitui o MinIO previsto no planejamento. Motivo da troca: as imagens oficiais do MinIO foram removidas do Docker Hub e do Quay em 2026 (verificado em 26/11/2026: `minio/minio` inexistente nos dois registros).
- SeaweedFS (Apache-2.0), imagem com versão fixada (`chrislusf/seaweedfs:4.55`), modo S3 (`server -s3`), **apenas em dev e testes**.
- O código usa **apenas o AWS SDK S3 com endpoint configurável**, para o storage ser intercambiável (dev, teste e produção podem ser diferentes).
- Testes de integração do storage com `GenericContainer` do Testcontainers (não há módulo Testcontainers dedicado ao SeaweedFS).
- Em dev, S3 sem autenticação (porta só em `127.0.0.1`); credenciais entram com o módulo `documentos` (Fase 2).
Motivo: alternativa livre, leve e ativa; o endpoint configurável evita prender o projeto a um fornecedor.

**D-059 — Eventos do Spring Modulith arquivados, reprocessados ao reiniciar, schema só via Flyway** · 2026-11-26
- `spring.modulith.events.completion-mode=archive`: eventos concluídos vão para `event_publication_archive` (nada é apagado).
- `spring.modulith.events.republish-outstanding-events-on-restart=true`: eventos pendentes são reprocessados ao reiniciar.
- Tabelas `event_publication` (V2) e `event_publication_archive` (V3) criadas pelo Flyway; inicialização automática de schema do Modulith desligada (`spring.modulith.events.jdbc.schema-initialization.enabled=false`).
- Eventos carregam **apenas ids**, nunca dados pessoais (o evento fica serializado no banco).
- Limpeza periódica do arquivo de eventos: pendente (CHECKLIST).
Motivo: rastreabilidade (combina com a auditoria) sem perder eventos em queda da aplicação.

**D-061 — CPF do usuário interno com o mesmo tratamento do paciente** · 2026-11-26
CPF do `Usuario` cifrado com AES-GCM + `cpf_hash` (HMAC-SHA256), reaproveitando o mesmo `AttributeConverter` e o mesmo cálculo de hash da D-052. O login busca o usuário pelo `cpf_hash` do CPF informado (normalizado).
Motivo: padrão único de proteção do CPF em todo o sistema.

**D-062 — Pacote Java e groupId `br.com.clinicaviene`** · 2026-11-26
Pacote renomeado de `com.github.ericjunq.Clinica_Viene` para `br.com.clinicaviene` (minúsculas, sem underscore); `groupId` `br.com.clinicaviene`, `artifactId` `clinica-viene`. Classe principal continua `ClinicaVieneApplication`. Fixtures do teste ArchUnit ficam em `fixtures.lombok` (fora do pacote da aplicação, para o Hibernate e o Modulith não as enxergarem).
Motivo: convenção Java; os módulos do Modulith são subpacotes, então renomear cedo é mais barato.

**D-063 — Spotless com palantir-java-format, `spotless:check` desligado por enquanto** · 2026-11-26
Formatador palantir-java-format. O `spotless:check` só será ligado no CI depois de rodar `./mvnw spotless:apply` numa máquina com Java e commitar `style: aplica formatação palantir` (no mesmo commit/push em que o check é ligado).
Motivo: ligar o check antes de formatar quebraria o CI; nesta máquina não há Java.

**D-064 — Sem pasta `docker/`: Dockerfiles em `backend/` e `frontend/`, compose na raiz** · 2026-11-26
A pasta `docker/` prevista no planejamento não foi criada (não há arquivos de configuração que precisem dela; o `nginx.conf` fica junto do frontend). Criar só se surgir configuração específica de infraestrutura.

**D-065 — Ruleset da `main` configurado pelo autor** · 2026-11-26
Tarefa manual do autor no GitHub: ruleset na `main` bloqueando **force push** e **exclusão**. Como o desenvolvimento é direto na `main` (D-058, confirmada pelo autor em 2026-11-26), o ruleset **não** exige PR nem checks antes do push (isso bloquearia o próprio push); o CI roda após cada push e falhas são corrigidas antes de seguir.

**D-048 — Primeiro acesso: inicializador cria o DEV; DEV cria o MASTER** · 2026-11-26 · (saiu de A CONFIRMAR)
Um inicializador na aplicação, ativo só com o banco **sem nenhum usuário**, cria o usuário DEV a partir de variáveis de ambiente (CPF, nome, e-mail, senha provisória) e registra em auditoria; com o banco populado, não faz nada. O DEV cria o MASTER pelo sistema. Senha provisória obriga troca de senha e cadastro de 2FA no primeiro login. Seeds de configuração (taxas, repasses, termos, anamnese) continuam A CONFIRMAR (ver D-048 em A confirmar). Recusada a migration de `INSERT` para usuários (poria dado real no repositório).

**D-066 — Política de senha** · 2026-11-26
Mínimo 12 caracteres, com pelo menos 1 letra maiúscula, 1 minúscula e 1 caractere especial. Validada no backend (e repetida no frontend só por usabilidade).

**D-067 — DEV independente do MASTER** · 2026-11-26
O DEV é o autor do sistema. Criado apenas pelo inicializador (nenhum endpoint cria DEV). O MASTER não vê, não edita e não desativa DEV; o DEV não desativa MASTER (se necessário, o DEV faz manualmente no banco, registrando o motivo fora do sistema). DEV continua sem acesso a dados de pacientes (D-003).
Motivo: o desenvolvedor precisa de acesso técnico que não dependa da clínica, e a clínica de controle que não dependa do desenvolvedor.

**D-068 — Status do usuário: só ATIVO e INATIVO, com efeito imediato** · 2026-11-26
Sem status `BLOQUEADO` (bloqueio por tentativas é temporário, via rate limit). MASTER ativa/desativa qualquer usuário exceto DEV; DEV ativa/desativa ADMIN e PROFISSIONAL. Desativar revoga todas as sessões; o status é checado a cada requisição.
Motivo: simplicidade; inativo não pode acessar nem pelos 15 min restantes do JWT.

**D-069 — Clínica multiprofissional: área de atuação e registro profissional** · 2026-11-26
A dona planeja atender outras áreas (psicologia, medicina etc.). O `Usuario` PROFISSIONAL tem **área de atuação** (`ODONTOLOGIA`, `PSICOLOGIA`, `MEDICINA`, `NUTRICAO`, `FISIOTERAPIA`, `FONOAUDIOLOGIA`, `ENFERMAGEM`, `OUTRA`) e **registro profissional** (conselho — CRO, CRP, CRM, CRN, CREFITO, CRFa, COREN, OUTRO —, número e UF). Substitui o campo `cro`. Impactos nas fases seguintes em D-070.

**D-071 — Autenticação: JWT de acesso + token de renovação em cookie HttpOnly** · 2026-11-26
Login em 2 passos (senha → desafio de 5 min/5 tentativas → 2FA). JWT de acesso 15 min, só em memória no frontend, header `Authorization`; assinado com RSA (chave do ambiente, validado pelo OAuth2 Resource Server). Token de renovação opaco de 12 h em cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, guardado como hash, rotacionado a cada uso; reuso revoga a cadeia.
Motivo: XSS não consegue roubar o token de longa duração; sem estado de sessão no servidor; CSRF neutralizado por `SameSite=Strict`, path restrito e resposta no corpo. Alternativa considerada: sessão no servidor com cookie (mais simples, revogação imediata) — não adotada para manter o desenho já documentado; a revogação imediata é obtida checando o status a cada requisição.

**D-072 — 2FA: TOTP + códigos de recuperação, com alternativa por e-mail** · 2026-11-26
TOTP (RFC 6238, implementado com `javax.crypto`) obrigatório no primeiro acesso, com 10 códigos de recuperação de uso único. O usuário também pode pedir um código de 6 dígitos por e-mail na tela do 2FA (sujeito à D-073). Observação registrada: e-mail é um segundo fator mais fraco que o app (quem invade o e-mail recebe o código); aceito pelo autor por praticidade.

**D-073 — Reenvio de e-mails e códigos com espera e limite** · 2026-11-26
Para todo envio de código/link (2FA por e-mail, recuperação de senha, convite de cadastro, códigos de assinatura): espera crescente entre envios (1, 2, 5, 10 min); máximo **5 envios** por destinatário e finalidade, depois só após **1 hora**; 5 tentativas erradas invalidam o código. Configurável no `application.yaml`, auditado, respostas genéricas.
Motivo: evitar abuso (spam, custo de envio, força bruta) sem travar o usuário legítimo.

**D-074 — Hash de senha com Argon2id** · 2026-11-26 · Adiciona a dependência BouncyCastle (`bcprov`), exigida pelo `Argon2PasswordEncoder` do Spring Security.

**D-075 — Termos LGPD (`TermoVersao`/`AceiteTermo`) movidos para a Fase 2** · 2026-11-26 · O aceite pertence ao paciente, que só existe na Fase 2.

**D-076 — `audit_log` append-only garantido por trigger no banco** · 2026-11-26
Função + trigger (`CREATE FUNCTION`/`CREATE TRIGGER`, compatível com a D-047) que rejeitam `UPDATE` e `DELETE`. Motivo: a garantia não depende só do código da aplicação.

**D-077 — Camadas dentro de cada módulo e nomes com sufixo do papel** · 2026-11-26 · **SUBSTITUÍDA por D-078** no mesmo dia (a parte dos nomes continua valendo)
Pedido do autor: organizar em `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`, `exceptions/`, `auth/` etc., com nomes claros (`UsuarioService`, `LoginService`, `UsuarioController`, `ProntuarioRepository`). Discutidas duas opções:
- (A) camadas no topo do projeto (um `service/` para tudo) — **recusada** (R-015);
- (B) camadas **dentro de cada módulo** do Spring Modulith — **aceita**.
Motivo: B mantém a D-002 e a verificação automática de limites entre módulos (`ModulosTest`); com A, qualquer service poderia ler, por exemplo, o repositório da anamnese sem passar pela auditoria — a regra "toda leitura de dado de saúde é auditada" passaria a depender só de disciplina. B também mantém cada assunto junto e as pastas pequenas. Módulos se comunicam por chamada de método no mesmo processo (não é microsserviço; sem HTTP entre módulos).
O que um módulo expõe aos outros fica marcado com `@NamedInterface` no `package-info.java` do subpacote. Classes da Fase 0/1 renomeadas (ex.: `ServicoUsuarios` → `UsuarioService`, `RepositorioUsuarios` → `UsuarioRepository`, `ControladorUsuarios` → `UsuarioController`, `EntradaAuditoria` → `AuditLog`, `TratadorDeErros` → `GlobalExceptionHandler`, `Cifrador` → `CifradorAesGcm`, `HashCpf` → `CpfHasher`).

**D-078 — Arquitetura em camadas na raiz da aplicação** · 2026-11-26 · Substitui D-002 e D-077
Decisão do autor, depois de discutir as opções (ver D-077/R-015): pastas `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/` (não `enum`: palavra reservada do Java), `exceptions/`, `auth/`, `config/`, `util/`, `validation/` diretamente em `br.com.clinicaviene`, com cada arquivo na pasta da sua camada e nomes com o papel no sufixo (`UsuarioService`, `UsuarioController`, `UsuarioRepository`…).
Consequências registradas:
- O teste `ModulosTest` (verificação de limites entre módulos do Spring Modulith) foi removido; o Spring Modulith continua no projeto apenas para o registro de eventos (D-059).
- A regra "dado de saúde só é acessado pelo service que audita" passa a ser convenção (CLAUDE.md), não verificação automática. Pode ser reforçada depois com regras ArchUnit específicas (ex.: só `ProntuarioService` usa `ProntuarioRepository`) — a avaliar quando surgir o primeiro dado de saúde (Fase 2).
Motivo: preferência do autor por uma estrutura mais conhecida e direta de navegar, também como material de estudo.

**D-079 — Detalhes do login em 2 passos** · 2026-11-26
- **Rate limit** (Bucket4j, em memória): 20 tentativas de login por IP e 5 senhas erradas por CPF a cada 15 min (configurável). Bloqueio → HTTP 429 com `Retry-After` e auditoria `LOGIN_BLOQUEADO_TENTATIVAS`. O limite por CPF conta só falhas e vale de qualquer IP.
- **Resposta genérica** para CPF inexistente, senha errada ou usuário inativo ("CPF ou senha inválidos."), com o hash Argon2 verificado mesmo sem usuário (mesmo tempo de resposta).
- **Desafio**: token opaco de uso único, 5 min, 5 códigos errados (configurável).
- **TOTP**: janela de ±1 passo (30 s) para tolerar relógio do celular; **anti-replay** — cada código vale uma vez (último passo aceito guardado em `totp_uso`).
- **Códigos de recuperação**: 10, formato `XXXX-XXXX-XXXX-XXXX` sem caracteres ambíguos (~79 bits), guardados como SHA-256, uso único; mostrados uma única vez, ao confirmar o cadastro do 2FA. Digitação tolerante (hífen, espaço, minúsculas).
- **QR code** gerado no backend (ZXing) e devolvido como `data:image/png;base64` — o segredo não passa por serviço externo de QR.
- Cookie de renovação: `cv_renovacao`.
Motivo: segurança contra força bruta e enumeração de CPFs sem travar o usuário legítimo.

**D-080 — Códigos por e-mail (2FA e recuperação de senha)** · 2026-11-26
- Um serviço único (`EnvioCodigoService`) aplica a D-073 a toda finalidade (`DOIS_FATORES_EMAIL`, `RECUPERACAO_SENHA`), com limites independentes por finalidade. Código de 6 dígitos, hash SHA-256 no banco, 10 min de validade, uso único; envio novo invalida o anterior.
- 2FA por e-mail só depois do app cadastrado (primeiro acesso exige o app — D-072).
- Recuperação de senha por **código** (não link): o usuário informa CPF, recebe o código no e-mail cadastrado e define a senha nova. A solicitação responde sempre igual (não revela se o CPF existe); em espera de reenvio, ignora em silêncio em vez de responder 429. Redefinir derruba todas as sessões; o 2FA continua exigido no próximo login (a recuperação de senha não contorna o segundo fator).
- E-mails sem dado clínico nem pessoal além do necessário (sem CPF/nome); falha de envio vai para o log sem destinatário nem conteúdo.
- Testes com servidor SMTP de teste (GreenMail).

**D-081 — Frontend da Fase 1 (telas de acesso e usuários)** · 2026-11-26
- Estrutura: `core/` (`models`, `services`, `interceptors`, `guards`, `validators`), `layout/` (`ShellComponent`), `pages/` (uma pasta por tela: `login`, `dois-fatores`, `trocar-senha`, `recuperar-senha`, `inicio`, `usuarios`). Nomes claros (`AuthService`, `UsuarioService`, `LoginPage`…), em linha com a D-078.
- Token de acesso **só em memória** no `AuthService` (nunca `localStorage`); ao abrir/recarregar a página, a sessão é restaurada chamando `/auth/renovar` (cookie HttpOnly). O `authInterceptor` põe o token nas chamadas e, com 401, renova uma vez e repete; renovações simultâneas são compartilhadas e o 409 (outra aba renovou) é tentado de novo.
- Guards só escondem telas (a regra de verdade está no backend): sessão, troca de senha pendente → `/trocar-senha`, perfil (`Usuários` só para DEV/MASTER).
- Componentes: inicialmente PrimeNG, trocado por Angular Material (D-084); telas carregadas sob demanda (lazy loading); orçamento padrão do Angular (500 kB / 1 MB).
- Regra de senha repetida no frontend só por usabilidade.
- **E2E (Playwright) no CI**: job `e2e` sobe o ambiente completo (`docker compose --profile app`) com `.env` descartável (chaves geradas na hora, DEV inicial fictício) e percorre o primeiro acesso no navegador, calculando o código TOTP no teste.

**D-082 — Identidade visual Viene no frontend** · 2026-11-26
Seguindo as peças enviadas pelo autor (Viene Odontologia & Saúde): fundo creme (`#F6E9E0`) com formas pêssego e linhas brancas finas; caramelo do monograma (`#C69A72`); marrom nos textos (`#6B4A2E`); ação (botões) em caramelo escuro `#8C6240` (contraste AA com texto branco); títulos em serifa clássica (**Cormorant Garamond**) e texto em **Montserrat** — ambas OFL, **hospedadas no próprio sistema** (pacotes `@fontsource`), sem Google Fonts (o navegador não envia o IP do usuário a terceiros — LGPD). Divisor com ✦ e marca "VIENE / ODONTOLOGIA & SAÚDE". O monograma é um SVG que reproduz o logo; substituir pelo arquivo oficial quando houver (ver DUVIDAS).

**D-084 — Angular Material no lugar do PrimeNG** · 2026-11-26 · Substitui a parte de PrimeNG da D-043
Ao renderizar as telas apareceu o aviso "Invalid PrimeUI License": o PrimeNG compatível com Angular 22 (e desde a v18) está sob a **PrimeUI License**, comercial — a licença Community gratuita exige chave, renovação anual e limites (menos de US$ 1 mi/ano, menos de 10 funcionários, menos de 5 devs). Decisão do autor: trocar para **Angular Material 22** (MIT, Google), sem chave nem renovação e sem risco de mudar de faixa se a clínica crescer. Ícones: `material-symbols` (Apache-2.0), hospedado localmente. A identidade visual da D-082 foi mantida (tema M3 com os tons da Viene). Efeito colateral: pacote inicial caiu de ~660 kB para ~327 kB.
Agenda: ver D-085 (calendário próprio no lugar do FullCalendar).

**D-085 — Agenda com calendário próprio (sem FullCalendar)** · 2026-11-27 · Substitui a parte de FullCalendar da D-043
O FullCalendar tem núcleo gratuito (MIT), mas as visões por recurso — agenda do dia com uma coluna por consultório ou por profissional, e a linha do tempo — são da versão Premium (licença comercial por desenvolvedor). Decisão do autor: construir o calendário da agenda no próprio frontend.
- Grade de horários com posicionamento por CSS (início → distância do topo, duração → altura); colunas configuráveis (dias, consultórios ou profissionais); sobreposições dividem a largura; arrastar para remarcar com o Angular CDK Drag & Drop (MIT, já no projeto).
- Regras da clínica desenhadas no próprio calendário: horários fora da disponibilidade esmaecidos, exceções (folgas/feriados), bloqueios hachurados, cor por status do agendamento, check-in direto no bloco.
- Ordem de construção (Fase 3): (1) visão do dia por consultório/profissional; (2) semana de um profissional; (3) lista do dia para o check-in; (4) arrastar para remarcar (com confirmação e motivo); (5) mês só se fizer falta.
Motivo: nenhuma licença (nem se a clínica crescer), encaixe exato nas regras de agenda e no visual da Viene, pacote mais leve. Custo: mais trabalho de construção e testes (posições, sobreposição, E2E de agendamento), e a manutenção é nossa.

**D-086 — Edição de usuário e mudança de perfil** · 2026-11-27
- **Editar dados** (`PUT /api/v1/usuarios/{id}`): nome, e-mail, telefone e registro profissional. Pode quem pode alterar o status do usuário (MASTER: todos menos DEV; DEV: ADMIN e PROFISSIONAL) e o próprio gestor nos próprios dados. E-mail e registro continuam únicos. Auditoria `USUARIO_ALTERADO`.
- **CPF não se edita**: identifica a pessoa e é a base do login (`cpf_hash`). CPF errado → inativar e cadastrar de novo.
- **Mudar perfil** (`PATCH /api/v1/usuarios/{id}/perfil`): nunca para DEV nem de DEV; ninguém muda o próprio perfil; o ator precisa poder gerenciar o usuário **e** poder criar o perfil novo; para PROFISSIONAL o registro é obrigatório, saindo de PROFISSIONAL o registro é apagado. As sessões do usuário caem (ele entra de novo). Auditoria `USUARIO_PERFIL_ALTERADO`.
- **Correção de segurança encontrada no caminho**: o perfil usado nas regras vinha do claim do JWT (válido por até 15 min) — um MASTER rebaixado continuaria agindo como MASTER até o token expirar. Agora o perfil vem do banco a cada requisição (autoridade `ROLE_<perfil>` montada pelo `UsuarioJwtConverter`); o claim do token é só informativo.
Regras propostas pelo Claude com base nas já existentes; o autor pode ajustar.

**D-087 — Itens finais da Fase 1 (infra)** · 2026-11-27
- Cobertura mínima verificada no build (JaCoCo, unitários + integração): **85% das linhas no total** e **80% em `service` e `auth`** (meta da Fase 1). Cobertura medida antes de ligar: 90,9% no total, 95,5% em `service`, 93,1% em `auth`.
- Rate limit: baldes ociosos (já recarregados) removidos a cada 15 min (`@Scheduled`); o Bucket4j usa o `Clock` da aplicação (testável).
- Imagem do backend: estágio de build com a imagem oficial `maven:3.10.2-eclipse-temurin-25-alpine` (antes o `mvnw` baixava o Maven a cada build e falhou uma vez com 429 do repositório central).

**D-088 — Anamnese: preenchida só pelo paciente e lida só pelos profissionais do atendimento** · 2026-11-27
- O paciente preenche a anamnese **diretamente no sistema** (no próprio celular ou num aparelho da clínica — D-091), cifrada no banco (D-052/§11), com controle de acesso por função.
- **Leitura exclusiva dos profissionais de saúde envolvidos no atendimento** (vínculo profissional↔paciente — Fase 4). **ADMIN, MASTER e DEV nunca leem** o conteúdo da anamnese — nem por acesso de emergência, que fica restrito a profissionais (ajusta a D-007). A dona da clínica não atende como profissional (confirmado pelo autor).
- **A recepção nunca preenche a anamnese** pelo paciente. Se o paciente precisar de ajuda (não lê, sem celular), um acompanhante preenche com ele, no aparelho dele ou da clínica, e isso é registrado.
- Toda leitura e gravação registrada no `audit_log` (imutável). Até a Fase 4 (vínculo), ninguém dentro da clínica lê a anamnese pelo sistema; o paciente recebe o PDF assinado.

**D-089 — Modelos de anamnese e termos configuráveis, com exemplo completo** · 2026-11-27 · Resolve a parte "anamnese e termos" da D-048
O MASTER cria e publica versões dos modelos de anamnese (por área de atuação) e dos termos pela tela de configuração; versão publicada não muda. O sistema traz um **exemplo completo** de ficha odontológica e de termos, marcado como rascunho para revisão da dentista e do advogado antes de publicar. Nenhum conteúdo real é carregado por migration.

**D-090 — Plataforma de assinatura: ZapSign** · 2026-11-27 · Resolve a D-016
Escolhida pelo custo (o autor conhece a D4Sign; ZapSign é equivalente e mais barata). Requisito do autor: assinatura **no próprio documento**, desenhada (tablet, celular, mouse) ou **digitada**, dentro do sistema. Implementação: interface `ProvedorAssinatura` com o adaptador **ZapSign** (assinatura embutida no sistema) quando houver conta/sandbox; até lá, e nos testes, um **provedor interno** (desenho com `signature_pad` ou nome digitado + código de confirmação + trilha de evidências: IP, aparelho, data/hora, hash do PDF). Na Fase 2 o paciente assina a **ficha** (cadastro + anamnese + termos); assinatura de procedimento (paciente + profissional) é da Fase 6.

**D-091 — Fluxo de cadastro: pré-cadastro na recepção + paciente completa e assina uma vez** · 2026-11-27
- A recepção faz o **pré-cadastro** (nome, CPF, nascimento, e-mail, telefone) e gera o convite.
- O paciente abre o convite com esses dados já preenchidos, **confere/corrige**, completa o resto (endereço, responsável se menor), responde a anamnese, aceita os termos e faz **uma única assinatura** do documento completo — atestando inclusive os dados digitados pela recepção.
- **Onde preencheu** muda a prova de identidade e fica registrado: no **próprio celular** (link/QR) → código no e-mail do paciente, origem `PACIENTE_LINK`; num **aparelho da clínica** (tablet ou PC da recepção virado para o paciente), aberto só a partir da tela logada da recepção ("abrir para o paciente neste aparelho") → a recepcionista confere o documento com foto e registra quem conferiu, origem `PACIENTE_QUIOSQUE`. Substitui o preenchimento pela recepção (origem `RECEPCAO` deixa de existir para a anamnese).

**D-092 — Alterações cadastrais** · 2026-11-27
- **Nova anamnese** → sempre nova ficha assinada (as anteriores permanecem).
- **Endereço, telefone e e-mail** podem ser alterados pela recepção, sem nova assinatura, gerando **versão auditada** (quem, quando, o que mudou); a próxima ficha assinada já sai com os dados novos.
- **Nome, CPF e data de nascimento** não são editáveis: correção só por nova ficha conferida e assinada pelo paciente.
- Validade da anamnese: 12 meses, configurável (resolve a D-039).

**D-093 — Consentimentos opcionais** · 2026-11-27
Separados da ciência da política de privacidade (obrigatória, base legal de saúde/obrigação legal — o paciente não "desliga" o prontuário). Opcionais, **desmarcados por padrão** e revogáveis a qualquer momento: lembretes de consulta por WhatsApp/SMS; promoções e novidades (campanhas só para quem marcou); uso de fotos de antes/depois em divulgação; pesquisas de satisfação. Cada marcação/revogação registrada (versão do texto, data/hora, IP, aparelho); o sistema impede usar o dado para um fim sem consentimento ativo. Não consentir não impede o atendimento.

**D-094 — Entrega do PDF ao paciente: link + CPF** · 2026-11-27
E-mail só com o link (uso limitado, expira em alguns dias); a página pede o **CPF completo** do paciente (para menor sem CPF, o do responsável) antes de liberar o download. O "segredo" não vai no mesmo e-mail que o link. PDF nunca anexado (D-026).

**D-095 — Ids UUID v7** · 2026-11-27
Ids continuam UUID (não enumeráveis em URLs, gerados no construtor, sem colisão entre sistemas), agora na versão **7** (ordenados pelo tempo: inserção eficiente no índice, como uma sequence). Gerador único `IdUtils.novoId()`. Tabelas existentes não mudam (a coluna continua `UUID`). Exceção mantida: `audit_log` com identity (interno, só inclusão).

**D-096 — Termos, consentimentos e modelos de anamnese: como funcionam** · 2026-11-27
- **Rascunho → publicado**: termos e modelos de anamnese nascem como rascunho editável; publicados ficam imutáveis (o banco rejeita `UPDATE`/`DELETE` de linha publicada). Para mudar, cria-se nova versão (no modelo, "duplicar"). A vigente é a publicada mais recente de cada tipo/área.
- **Decisões do paciente** (`aceite_termo`, somente inclusão): ao concluir o cadastro, cada termo vigente recebe `ACEITO` (marcado) ou `RECUSADO` (oferecido desmarcado — prova de que não houve consentimento). Revogação = novo registro `REVOGADO`. O estado atual é a decisão mais recente. A **política de privacidade não se revoga** (é ciência; atendimento e prontuário seguem base legal de saúde/obrigação legal).
- Antes de qualquer uso opcional (lembretes, promoções, imagem, pesquisa), o sistema consulta `temConsentimentoAtivo`.
- **Alerta clínico**: Sim nas perguntas Sim/Não; nas de escolha, **a primeira opção é a resposta "normal"** e as demais acionam o alerta; nas demais, qualquer resposta.
- **Exemplos** (D-089) ficam em `backend/src/main/resources/exemplos/` e viram rascunhos por ação do MASTER (`POST /api/v1/termos/exemplos`, `POST /api/v1/modelos-anamnese/exemplos/odontologia`) — sem migration de `INSERT`. Ficha odontológica de exemplo: 46 perguntas, 19 de alerta clínico (alergias, anticoagulantes, bisfosfonatos, cardiopatias, coagulação, gravidez etc.). Termos de exemplo com campos `[ENTRE COLCHETES]` e aviso de revisão jurídica.
- Só o MASTER configura termos e modelos; ele cuida das perguntas, mas nunca vê respostas (D-088).

**D-097 — Cache de dependências na montagem das imagens (CI)** · 2026-11-27
O job Docker falhou de forma intermitente com **429 (Too Many Requests)** do Maven Central: cada execução montava a imagem do backend do zero e baixava todas as dependências. Correção: o Dockerfile do backend baixa as dependências numa **camada própria** (`mvn dependency:go-offline`, refeita só quando o `pom.xml` muda) e os jobs `docker` e `e2e` montam as imagens com `docker/build-push-action` e **cache do GitHub Actions** (`type=gha`); o E2E reaproveita as imagens (`docker compose up --no-build`, imagens `clinica-viene-backend:local`/`clinica-viene-frontend:local`). Complementa a D-087 (imagem oficial do Maven).

**D-098 — Convite de cadastro: regras** · 2026-11-27 · Detalha a D-091
- Dois modos: **LINK** (celular do paciente; vale **72 h**; mostrado como link/QR e, se pedido, enviado por e-mail) e **QUIOSQUE** (aparelho da clínica; vale **2 h**; só é criado com a recepcionista confirmando que conferiu o documento com foto — fica gravado quem conferiu; nunca vai por e-mail).
- **Um convite ativo por paciente**: gerar outro cancela o anterior. A recepção também pode cancelar.
- O token (≥ 256 bits) aparece **só na resposta da criação** (link + QR code); no banco fica o hash SHA-256. Link inválido, expirado, cancelado ou usado dá sempre a mesma resposta.
- E-mail vai para o e-mail do paciente ou, se ele não tiver (menor), para o do responsável; não leva dado de saúde; a resposta mostra o e-mail mascarado. Reenvios seguem a D-073 por paciente (espera 1/2/5/10 min, 5 envios, depois 1 h); gerar só o QR, sem e-mail, não tem espera.
- Só ADMIN e MASTER geram, listam e cancelam convites; tudo auditado (`CONVITE_CRIADO`, `CONVITE_ENVIADO_EMAIL`, `CONVITE_CANCELADO`). Validades e endereço público (`APP_URL_PUBLICA`) configuráveis.

**D-099 — README profissional mantido junto com o código** · 2026-11-27
`README.md` na raiz no padrão de projetos profissionais (badges, sumário, sobre, funcionalidades com situação, tecnologias, arquitetura, pré-requisitos, clonar, `.env` com geração das chaves, rodar em containers ou em modo dev, primeiro acesso, portas, testes, estrutura, segurança/LGPD, documentação, contribuição, roadmap, licença). O `frontend/README.md` gerado pelo Angular CLI foi trocado por um específico. O README é atualizado a cada implementação que mude como rodar/testar, variáveis, portas ou a situação dos módulos (regra no `CLAUDE.md`). Licença declarada como **proprietária, todos os direitos reservados** (o repositório não concede licença de uso).

**D-100 — Fluxo público do cadastro: identidade, sessão de preenchimento e revisão** · 2026-11-27 · Detalha a D-091
- **Identidade**: no modo LINK o paciente só entra com o **código de 6 dígitos** enviado ao e-mail do convite (do paciente ou, se menor sem e-mail, do responsável) — 10 min, 5 tentativas, esperas da D-073. Por isso **convite LINK exige e-mail cadastrado**; sem e-mail, só quiosque. No QUIOSQUE a sessão abre direto (o documento foi conferido ao gerar o convite).
- **Sessão de preenchimento** (`sessao_cadastro`): token próprio, válido por **2 h** (configurável), ligado ao convite; perde a validade se o convite for usado ou cancelado. Só hashes do código e do token no banco.
- **Tokens fora das URLs**: na API, convite no cabeçalho `X-Convite-Cadastro` e sessão em `X-Sessao-Cadastro`; o link para o paciente é **`/cadastro#<token>`** — o que vem depois do `#` não é enviado ao servidor (não aparece em log do Nginx nem em `Referer`), e a tela apaga o token da barra de endereço ao abrir. Rotas públicas com limite por IP, separado do login.
- **Frontend**: tela `/cadastro` (pública) com confirmação do código ou botão "Começar" (quiosque) e passo a passo: seus dados → ficha de saúde → privacidade (consentimentos opcionais desmarcados) → conferir. Tokens e respostas só em memória (o aparelho pode ser o quiosque da clínica); recarregar a página exige abrir o link de novo. O interceptor não manda o token da equipe nas rotas `/api/v1/publico/`.
- **Formulário**: pré-cadastro para conferir, modelo de anamnese vigente da área do convite (D-101) e termos vigentes. Leitura auditada sem usuário ("pelo próprio paciente").
- **Revisão**: confere a ficha inteira — dados (endereço completo exigido; CPF válido e não usado por outro paciente; adulto sem CPF só se já aprovado pelo MASTER; menor com responsável), respostas contra o modelo vigente e **uma decisão para cada termo vigente** com a política aceita. Se a clínica publicar nova versão durante o preenchimento, pede para recarregar.
- **Nada da ficha é gravado antes da assinatura** (2F): as respostas da anamnese só chegam ao banco junto com a ficha assinada e cifrada — menos dado de saúde guardado sem necessidade.
- Acompanhante (quem ajudou a preencher) é campo opcional da ficha (D-088).

**D-101 — Área da ficha de saúde no convite; coluna incluída no CREATE da V14** · 2026-11-27 · Resolve a Q-013
- A recepção escolhe a **área** (odontologia, psicologia…) ao gerar o convite; o paciente responde o modelo de anamnese vigente **daquela área**. Sem escolha, vale a área padrão (`clinicaviene.convite.area-padrao`, ODONTOLOGIA). Área sem modelo publicado: o paciente vê o aviso para falar com a recepção.
- **Aprovado pelo autor**: como nenhum banco tem dado real (só testes; o CI cria banco novo a cada execução), a coluna `area_atuacao` entrou **editando o `CREATE TABLE` da V14**, em vez de um `ALTER`. Efeito colateral: banco local que já rodou a V14 acusa checksum diferente no Flyway — recriar com `docker compose down -v`. A aprovação vale para este caso; a regra geral da D-047 continua (mudanças de schema só com aprovação).

**D-102 — Assinatura da ficha, PDF, storage e entrega** · 2026-11-27 · Detalha D-088, D-090, D-094
- **Provedor interno** atrás de `ProvedorAssinatura`: assinatura **desenhada** (PNG conferido: formato, tamanho, não pode estar em branco) ou **nome digitado** (tem de ser o nome completo de quem assina, sem diferença de acento/maiúsculas). Quem assina: o paciente, ou o responsável legal se menor. Obrigatória a declaração "as informações são verdadeiras". A confirmação de identidade é a do início da sessão (código no e-mail ou documento conferido no quiosque — D-100); não se pede um segundo código para assinar.
- **Ao assinar**, numa única transação: confere a ficha de novo, grava os dados conferidos (inclusive correção de nome/CPF/nascimento — D-092) com versão de origem `PACIENTE_LINK`/`PACIENTE_QUIOSQUE`, registra as decisões sobre os termos, gera o PDF, guarda e grava `Documento` + `Assinatura` + `FichaAnamnese` e encerra o convite.
- **PDF** (Thymeleaf → openhtmltopdf): dados, responsável, ficha de saúde, termos com o texto da versão aceita, declaração, assinatura e **trilha de evidências** (como a identidade foi confirmada, data/hora, IP, aparelho, id do documento, hash SHA-256 do conteúdo assinado). O **SHA-256 do PDF** fica no banco e é conferido a cada download.
- **Storage**: S3 compatível com endpoint configurável; o PDF sai do servidor **já cifrado** (AES-256-GCM, mesma chave versionada dos campos) — o provedor de storage nunca vê o conteúdo. SeaweedFS em dev e em contêiner nos testes; bucket criado sob demanda só em dev/teste.
- **Imutável no banco**: `documento`, `assinatura` e `ficha_anamnese` com trigger que rejeita UPDATE/DELETE. Respostas, alertas, acompanhante e evidências cifrados.
- **Entrega (D-094)**: e-mail com link `/documento#<token>` para o paciente (ou o responsável, quando ele assinou), sem dado de saúde; vale **7 dias**, até **5 downloads**, **5 CPFs errados** bloqueiam o link. A página pede o CPF do paciente (ou do responsável, se o paciente não tiver CPF). Sem e-mail (quiosque), a recepção entrega o PDF depois (2G).
- **Frontend**: depois de "Conferir ficha", a tela de assinatura oferece **Desenhar** (quadro próprio em canvas — dedo, caneta ou mouse; sem biblioteca externa) ou **Digitar o nome**, com a declaração obrigatória; ao concluir, a tela esquece tokens e respostas e, no quiosque, pede para devolver o aparelho. Página pública `/documento#<token>` pede o CPF e baixa o PDF.

**D-103 — Adaptador ZapSign: estrutura pronta, envio desligado** · 2026-11-27 · Detalha a D-090
- O autor criou conta na ZapSign, mas o plano gratuito tem **3 documentos/mês**: o teste com documento real fica para quando o projeto estiver avançado. Por ora, só a estrutura, **sem nenhum envio**.
- `ZapSignClient` (`criarDocumento` com o PDF em base64 e `detalharDocumento`) + `ZapSignProperties` (`clinicaviene.zapsign.*`, variáveis `ZAPSIGN_*`). Com `habilitado=false` (padrão) toda chamada é recusada **antes** de sair da aplicação; os testes usam um servidor HTTP local que imita a API.
- Conferido na documentação da ZapSign: **1 PDF de até 10 MB** por documento (o cliente recusa acima disso antes de enviar), PDF em base64 sem o prefixo `data:`, autenticação `Bearer`. Signatário com `auth_mode=assinaturaTela`, nome travado (`lock_name`) e **sem e-mail/WhatsApp automático** da ZapSign — o sistema mostra o `sign_url`. `original_file`/`signed_file` são links temporários (60 min): o PDF assinado será baixado e guardado no storage próprio, nunca o link.
- Existe **sandbox** (`https://sandbox.api.zapsign.com.br/api/v1`, conta própria em sandbox.app.zapsign.com.br) sem validade jurídica e que não consome a cota — é o padrão da `url-api`. Produção (`api.zapsign.com.br`) exige plano com API.
- Ainda **não está ligado ao fluxo da ficha** (que segue com o provedor interno): ligar exige definir o momento do envio, a tela embutida do `sign_url` e o webhook de documento assinado — fica para quando houver o teste real.

**D-104 — Telas internas da Fase 2 (2G)** · 2026-11-27 · Detalha D-088, D-091, D-094, D-098
- **Pacientes** (ADMIN e MASTER): busca por nome, CPF ou nascimento; pré-cadastro (menor exige responsável; adulto sem CPF só o MASTER, com justificativa; alerta de duplicidade pede confirmação); ficha com abas **Cadastro** (edição só de contato/endereço — D-092), **Convite**, **Documentos**, **Privacidade** (revogar consentimento a pedido do paciente; a recepção nunca marca consentimento por ele) e **Histórico**.
- **Convite**: escolha da área da ficha (D-101); **link** (só com e-mail cadastrado, opção de enviar por e-mail) ou **quiosque** (exige marcar "Conferi o documento com foto"). O QR e o link aparecem só na criação. "Abrir neste aparelho" **encerra a sessão da recepção** antes de abrir o cadastro, para o paciente não ficar com acesso às telas internas.
- **Entrega do PDF sem e-mail** (pendência da D-102): a recepção **não abre o PDF** (tem dado de saúde — D-088). Ela gera um **novo link de download** e mostra o **QR na tela** para o paciente ler no próprio celular (ou envia ao e-mail, se houver). Mesmas regras da D-094 (7 dias, 5 downloads, CPF). Endpoints: `GET /api/v1/pacientes/{id}/documentos` (só metadados) e `POST …/documentos/{documentoId}/entregas`; auditoria `DOCUMENTO_LINK_GERADO`.
- **Configurações** (só MASTER): termos por tipo (carregar exemplos, editar rascunho em Markdown, publicar com confirmação, nova versão a partir da vigente) e **fichas de saúde** por área com **editor de perguntas** (seção, texto, tipo, opções, obrigatória, alerta, "qual?" obrigatório, ajuda; subir/descer/remover). Código da pergunta gerado do texto (sem acento, único); "nova versão" duplica um modelo publicado.
- **E2E**: projetos do Playwright `primeiro-acesso` (DEV cria também uma MASTER fictícia) → `fluxos` (MASTER publica política e ficha, pré-cadastra, abre o quiosque; o paciente assina num navegador separado; a recepção gera o QR e o paciente baixa o PDF com o CPF). Sem nova tentativa automática no projeto `fluxos` (a senha provisória já terá sido trocada).

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

**R-015 — Camadas no topo do projeto (um `controller/`, `service/`, `repository/` para o sistema todo)** · 2026-11-26 · Recusada inicialmente em favor da D-077; **revertida pelo autor: passou a ser a D-078**.
Motivo: perderia a verificação de limites entre módulos (D-002) e a garantia estrutural de que dados de saúde só são acessados pelos serviços que auditam.

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

**D-016 — Plataforma de assinatura (Clicksign, ZapSign, D4Sign, outra)** · ~~A CONFIRMAR~~ → decidida em 2026-11-27: **ZapSign** (D-090)
Critérios: custo por documento, API, assinatura presencial em tablet, pacote de evidências, aceitação de ICP-Brasil.

**D-037 — Aquisição de tablet para a recepção** · A CONFIRMAR

**D-038 — Limite de desconto sem aprovação do MASTER** · A CONFIRMAR (sugestão: 10%)

**D-039 — Prazo de validade da anamnese** · ~~A CONFIRMAR~~ → 12 meses, configurável (D-092)

**D-040 — Prazo de retenção do prontuário e política de retenção dos demais dados** · A CONFIRMAR com advogado

**D-041 — Hospedagem (provedor, região) e backups** · A CONFIRMAR

**D-060 — Storage de produção (documentos)** · A CONFIRMAR · 2026-11-26 · decidir junto da D-041
Definido mais para o fim do projeto, após levantamento de custos: volume estimado de documentos, preço de armazenamento e de transferência, backup, e hospedagem no Brasil (LGPD). Qualquer opção precisa ser S3 compatível (o código usa o AWS SDK S3 com endpoint configurável — D-056).

**D-042 — Ações permitidas ao chatbot da recepção** · A CONFIRMAR (discutir quando chegar a fase)

**D-043 — Biblioteca de UI** · ~~A CONFIRMAR~~ → decidida em 2026-11-26 (ver Aceitas: hoje Angular Material — D-084 — e calendário próprio — D-085).

**D-048 — Como carregar dados de configuração reais (seeds) em produção** · A CONFIRMAR · 2026-11-26 · **Parte do primeiro usuário decidida em 2026-11-26** (ver Aceitas: inicializador cria o DEV). Continua A CONFIRMAR para taxas, repasses, anamnese e termos.
A D-047 só permite `INSERT` de dados fictícios em testes, mas o sistema precisa de configurações iniciais reais: taxas (§8.5), repasses (§8.6), modelo de anamnese, termos LGPD e o primeiro usuário MASTER. Opções: (a) migration de `INSERT` específica, aprovada caso a caso; (b) cadastro pela tela de configurações/endpoint administrativo, sem seed no banco; (c) inicializador na aplicação (ex.: `ApplicationRunner`) idempotente. Decidir antes da Fase 1 (primeiro usuário) e da Fase 5 (taxas/repasses).

**D-056 — Storage S3 local (substituto do MinIO)** · ~~A CONFIRMAR~~ → decidida em 2026-11-26 (ver Aceitas: SeaweedFS em dev/teste; produção em D-060).
A imagem oficial `minio/minio` não está mais disponível no Docker Hub nem no Quay (verificado em 26/11/2026). O compose de dev ficou **sem** storage por enquanto (só será necessário na Fase 2 — PDFs). Opções: SeaweedFS, Garage, RustFS, LocalStack (S3), ou fixar uma imagem MinIO antiga de terceiros. O código usará o AWS SDK S3 (compatível com qualquer uma). Ver `docs/DUVIDAS.md`.

**D-070 — Impactos da clínica multiprofissional nas próximas fases** · A CONFIRMAR · 2026-11-26
- Categorias de procedimento e percentuais de repasse por área (a lista atual da §8.6 é só de odontologia).
- Modelos de anamnese por área.
- Campos do prontuário por área (ex.: "dente/região" só em odontologia).
- Prazos de retenção e regras de assinatura que variam por conselho (CFO, CFP, CFM…).
- **Psicologia**: regras próprias de sigilo e registro documental (Resoluções CFP 01/2009 e 06/2019); avaliar se o acesso de emergência (D-007) pode abrir prontuário psicológico. Validar com advogado.

**D-083 — Licença do PrimeNG** · ~~A CONFIRMAR~~ → decidida em 2026-11-26 (ver Aceitas: D-084).