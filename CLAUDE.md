# CLAUDE.md — ClinicaViene

Sistema de gestão para clínica de saúde multiprofissional — hoje odontologia, preparada para psicologia, medicina etc. (agenda, pacientes, prontuário, financeiro, documentos assinados).
Stack: **Java 25 + Spring Boot 4.2.x** (backend) · **Angular** (frontend) · **PostgreSQL** · **Docker** · **GitHub Actions**.

## Leitura obrigatória ao iniciar QUALQUER sessão

Antes de escrever código, leia nesta ordem:

1. `docs/CONTEXTO.md` — objetivos, regras de negócio, LGPD, modelo de dados, arquitetura (fonte da verdade).
2. `docs/DECISOES.md` — ideias aceitas, recusadas e a confirmar, com o porquê de cada uma.
3. `docs/CHECKLIST.md` — o que está feito, em andamento e pendente.
4. `docs/DUVIDAS.md` — dúvidas abertas para discutir com o usuário (anote ali o que surgir quando ele não estiver presente).

Depois de ler, **pergunte ao usuário qual é o ambiente desta sessão** antes de rodar qualquer comando de build/teste:

- **Ambiente completo** — a máquina tem JDK 25, Docker, Node/npm (e o que mais for preciso) para compilar e rodar os testes; ou
- **Só construção de código** — sem rodar build/testes nesta máquina (ex.: sem Java). Nesse caso, não tente executar `mvnw`, `java`, Docker etc.; escreva o código e os testes, liste os testes **não executados** e marque os itens do checklist como `implementado — testes pendentes de execução` (a execução fica para o CI ou outra máquina).

Se algo pedido na sessão contradizer esses documentos, **pare e pergunte** antes de implementar.
Itens marcados **A CONFIRMAR** não devem ser implementados de forma definitiva: implemente de forma configurável ou pergunte.

## Atualização obrigatória a cada implementação

Toda implementação ou alteração deve, **no mesmo commit ou nos commits da mesma implementação**:

- atualizar `docs/CONTEXTO.md` se mudou regra, entidade, fluxo ou arquitetura;
- registrar em `docs/DECISOES.md` toda ideia nova discutida (aceita, recusada ou a confirmar) com data e motivo;
- marcar/adicionar itens em `docs/CHECKLIST.md`.
- atualizar o `README.md` (raiz e/ou `frontend/README.md`) se mudou como instalar/rodar/testar, variável de ambiente, porta, serviço ou a situação de um módulo (tabela de funcionalidades e roadmap) — D-099.

Documentação desatualizada é considerada bug.

## Regras inegociáveis

- **LGPD à risca** e máxima segurança jurídica para a clínica (ver `docs/CONTEXTO.md` §11).
- **Prontuário, anamnese e documentos assinados são imutáveis.** Nada de UPDATE/DELETE: correções são feitas por adendo/nova versão.
- **Toda leitura e alteração de dado de saúde gera registro em `audit_log`.**
- Enums persistidos sempre com `@Enumerated(EnumType.STRING)`. Nunca `ORDINAL`.
- Dinheiro sempre `BigDecimal` / `NUMERIC(12,2)`; percentuais `NUMERIC(7,4)`. Nunca `double`/`float`.
- Pagamentos guardam **snapshot** das taxas e percentuais usados (nunca recalcular o passado).
- Datas em UTC (`Instant`/`OffsetDateTime`) no banco; exibição em `America/Bahia`.
- Schema só via **Flyway** (`ddl-auto=validate`), com migrations em **SQL puro**. Política de migrations (D-047):
  - Em ambiente de dev, migrations contêm **apenas `CREATE`** (tabelas, índices, constraints, extensões).
  - `INSERT` com **dados fictícios** só para testes, em `backend/src/test/resources/db/testdata/` (nunca em `src/main`, nunca rodam fora dos testes).
  - **`ALTER`, `UPDATE`, `DELETE`, `DROP` ou qualquer outra mudança: só com aprovação explícita do usuário.** Pergunte antes de escrever.
- Nenhum segredo no repositório (usar variáveis de ambiente / `.env` fora do Git; `.env.example` só com valores fictícios).
- Nenhum dado real de paciente fora de produção.
- CPF (paciente e responsável) criptografado com AES-GCM + coluna `cpf_hash` (HMAC-SHA256) para unicidade e busca (D-052).

## Estrutura e nomes do backend (D-078)

Arquitetura em camadas, com as pastas na raiz do pacote da aplicação (`br.com.clinicaviene`), ao lado de `ClinicaVieneApplication`:

```
br.com.clinicaviene/
  controller/   XxxController            (REST, fino: só converte e delega)
  service/      XxxService               (regras de negócio e autorização)
  repository/   XxxRepository            (Spring Data)
  entity/       Xxx                      (entidades JPA)
  dto/          XxxRequest, XxxResponse, XxxDto (records)
  enums/        enums persistidos como STRING   ("enum" é palavra reservada do Java)
  exceptions/   XxxException + GlobalExceptionHandler
  auth/         segurança, JWT, permissões
  config/       configurações e propriedades
  util/         utilitários (CPF, criptografia)
  validation/   validadores
```

- **Nomes claros**, com o papel no sufixo em inglês e o domínio em português: `UsuarioService`, `LoginService`, `UsuarioController`, `ProntuarioRepository`, `NovoUsuarioRequest`, `UsuarioResponse`, `SessaoInvalidaException`.
- Cada arquivo vai na pasta da sua camada. Testes ficam no mesmo pacote da classe testada.
- Dados de saúde só são lidos/alterados pelo service do assunto, que registra a auditoria (controllers e outros services não acessam esses repositories diretamente).

## Lombok (D-051)

- **Permitido**: `@Getter`, `@Setter` (só onde necessário), `@NoArgsConstructor(access = AccessLevel.PROTECTED)` em entidades, `@RequiredArgsConstructor` em services, `@Slf4j`, `@Builder` pontual.
- **Proibido**: `@Data`, `@EqualsAndHashCode`, `@ToString` e `@AllArgsConstructor` em entidades JPA; `@Value` (usar `record`).
- `equals`/`hashCode` de entidades escritos à mão, baseados apenas no `id`.
- DTOs sempre como `record`.
- Em DTOs de **requisição**, campos opcionais usam tipos wrapper (`Boolean`, `Integer`), nunca primitivos: o Jackson 3 (Boot 4) rejeita com 400 um primitivo ausente no JSON.
- Regras verificadas por teste ArchUnit (`EntidadesLombokArchTest`).

## Testes — honestidade obrigatória

- Nunca declare um teste como aprovado sem ter visto a saída real da execução.
- Se o ambiente não tiver JDK 25 / Docker para rodar os testes, diga isso explicitamente, liste quais testes **não foram executados** e marque o item do checklist como `implementado — testes pendentes de execução`.
- Nunca "simule" resultado de teste.
- Toda funcionalidade nova vem com testes: unitários, integração (Testcontainers) e, quando houver tela, E2E (Playwright).
- **O CI (GitHub Actions) é o lugar oficial dos testes** (D-046). Testes só contam como aprovados com o CI verde.
- Após cada push, acompanhe a execução (`gh run watch` / `gh run view --log-failed`; sem `gh`, pela API pública:
  `curl -s https://api.github.com/repos/ericjunq/Clinica-Viene/actions/runs?branch=main`), leia as falhas, corrija e reenvie.
- CI vermelho na `main` é prioridade: corrija antes de seguir para a próxima implementação.
- Nomes: testes unitários terminam em `Test` (Surefire, `./mvnw test`); de integração em `IT` (Failsafe, `./mvnw verify`).

Comandos:

```bash
# backend (projeto Maven em backend/)
cd backend
./mvnw verify                      # unitários + integração (requer Docker para Testcontainers)
./mvnw test                        # apenas unitários
# sem JDK local, com Docker (a partir de backend/):
docker run --rm -v "$PWD":/app -v /var/run/docker.sock:/var/run/docker.sock -w /app maven:3-eclipse-temurin-25 ./mvnw verify
# frontend (em frontend/)
npm test                           # unitários (Vitest)
npx playwright test                # E2E (ambiente completo no ar; variáveis E2E_DEV_CPF/E2E_DEV_SENHA)
# infraestrutura de dev (Postgres, Mailpit) — na raiz, com .env criado a partir de .env.example
docker compose up -d
# ambiente completo (infra + backend em container)
docker compose --profile app up -d --build
```

## Git

- **Trabalho direto na `main`** (D-058): sem branches de implementação e sem PRs. Commits pequenos, um por implementação/alteração, com push na `main`.
- **Commits**: `tipo: descrição` em português, no imperativo, minúsculo.
  Tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `ci`, `build`, `perf`, `style`.
  Ex.: `feat: adiciona cadastro de consultórios`
- **SEM COAUTORIA**: nunca adicionar `Co-Authored-By`, `Generated with Claude Code` ou qualquer atribuição a IA em commits. (Reforçado em `.claude/settings.json`.)
- Antes do push, rode o que der para rodar localmente (conforme o ambiente da sessão); depois do push, acompanhe o CI da `main`.
- Nunca commitar `.env`, credenciais, dumps de banco ou documentos de pacientes.
