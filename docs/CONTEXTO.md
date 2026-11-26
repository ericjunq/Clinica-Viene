# ClinicaViene — Contexto do Projeto

> Fonte da verdade do projeto. Deve ser atualizada a cada implementação ou alteração.
> Última atualização: 2026-11-26 — plano da Fase 1: clínica multiprofissional, DEV independente, status ATIVO/INATIVO, política de senha, autenticação (JWT + refresh em cookie), 2FA (TOTP + e-mail), regras de reenvio.

---

## 1. Visão geral e objetivos

Sistema web para uma clínica de saúde de cidade pequena (hoje com **1 consultório** e atendimento odontológico, preparada para N consultórios e **outras áreas da saúde** — psicologia, medicina, nutrição etc. — D-069).

Objetivos, em ordem de prioridade:

1. **Segurança jurídica da clínica** — toda informação clínica e todo aceite/assinatura devem ter prova de autoria, data, origem e integridade.
2. **Conformidade total com a LGPD** (dados de saúde são dados pessoais sensíveis).
3. Organizar recepção: agenda, cadastro, check-in, avisar o profissional.
4. Organizar financeiro: cobrança com taxas, descontos, parcelas, status e divisão profissional/clínica.
5. Prontuário digital com histórico completo e imutável.
6. Preparar integrações futuras: API de pagamentos, WhatsApp/SMS, chatbot para a recepção.

O sistema será construído **em partes** (ver `docs/CHECKLIST.md`), não de uma vez.

---

## 2. Perfis e permissões

| Perfil | Quem é | Pode |
|---|---|---|
| **DEV** | Desenvolvedor (o autor) | Administração técnica; criar o MASTER e usuários ADMIN/PROFISSIONAL; ativar/desativar ADMIN e PROFISSIONAL. **Independente do MASTER** (D-067): criado só pelo inicializador, o MASTER não vê/edita/desativa DEV, e o DEV não desativa MASTER. **Não acessa dados de pacientes em produção** (acesso técnico excepcional só com justificativa e auditoria). |
| **MASTER** | Dono(a) da clínica | Tudo, exceto gerenciar DEV: criar usuários (ADMIN, PROFISSIONAL, MASTER), ativar/desativar usuários, configurações, relatórios, financeiro, aprovar descontos acima do limite, acesso de emergência a prontuários (com justificativa). |
| **ADMIN** | Recepção | Cadastro de pacientes, gerar link/QR de cadastro, agenda (marcar/remarcar/cancelar), check-in e aviso ao profissional, orçamentos, pagamentos, configuração de preços/taxas/repasses, transferir vínculo paciente↔profissional. **Não vê conteúdo clínico** (anamnese, procedimentos descritos, documentos clínicos) — só dados cadastrais, agenda e financeiro. |
| **PROFISSIONAL** | Profissional de saúde (dentista, psicólogo, médico…), com área de atuação e registro no conselho | Ver e registrar no prontuário **apenas** de pacientes com vínculo ativo com ele; ver a própria agenda; receber aviso de chegada; ver os próprios repasses. |

Regras:

- Usuários internos fazem login com **CPF + senha**, com **2FA** (TOTP ou código por e-mail). O usuário é localizado pelo `cpf_hash` do CPF informado (D-061).
- Somente DEV e MASTER criam usuários ADMIN/PROFISSIONAL. O primeiro MASTER é criado pelo DEV; depois, MASTER também cria MASTER. **Nenhum endpoint cria DEV** (D-067).
- **Primeiro acesso (D-048)**: o inicializador cria o usuário DEV a partir de variáveis de ambiente, uma única vez, só com o banco sem usuários (auditado); o DEV cria o MASTER pelo sistema. Senha provisória → troca e cadastro de 2FA obrigatórios no primeiro login.
- **Status (D-068)**: apenas `ATIVO` e `INATIVO`; usuário nunca é apagado. MASTER ativa/desativa qualquer usuário exceto DEV; DEV ativa/desativa ADMIN e PROFISSIONAL. Desativar revoga todas as sessões na hora, e o status é checado a cada requisição — inativo não acessa nada. Desativar um MASTER: só manualmente no banco, pelo DEV (fora do sistema, registrar o motivo).
- **Novo usuário**: quem cria define uma senha provisória e a repassa ao usuário; troca obrigatória (e cadastro de 2FA) no primeiro login. Respostas da API mostram o CPF mascarado (`***.018.159-**`).
- **Senha (D-066)**: mínimo 12 caracteres, com pelo menos 1 letra maiúscula, 1 minúscula e 1 caractere especial. Hash Argon2id (D-074).
- **Bloqueio por tentativas**: não é status; é bloqueio temporário por rate limit (IP e CPF).
- Pacientes **não têm login** no sistema interno; interagem apenas por links seguros de uso único (cadastro, anamnese, assinatura, visualização de documento).
- Autorização na camada de serviço (não só no controller), testada por perfil.

---

## 3. Módulos (monólito modular — Spring Modulith)

| Módulo | Responsabilidade |
|---|---|
| `identidade` | Usuários, perfis, autenticação JWT + refresh token, 2FA, recuperação de senha |
| `pacientes` | Cadastro, responsável legal, convites de cadastro (link/QR), quiosque |
| `anamnese` | Modelos de ficha versionados, respostas, revisão pelo profissional |
| `prontuario` | Prontuário, vínculos profissional↔paciente, procedimentos, adendos, acesso de emergência |
| `agenda` | Consultórios, disponibilidade, exceções, bloqueios, agendamentos, check-in |
| `financeiro` | Orçamentos, descontos, configuração de taxas e repasses, pagamentos, parcelas, relatórios |
| `documentos` | Geração de PDF, armazenamento (S3 compatível — SeaweedFS em dev/teste, produção a definir), hash, integração com plataforma de assinatura |
| `notificacoes` | Tempo real (WebSocket), e-mail, futuramente SMS/WhatsApp |
| `auditoria` | Log imutável de acessos e alterações |
| `lgpd` | Termos versionados, aceites, consentimentos opcionais, solicitações do titular |

Módulos só se comunicam por API pública do módulo ou eventos de domínio, no mesmo processo (chamada de método; não é microsserviço). Limites verificados em teste (`ApplicationModules.verify()`).

**Organização interna (D-077)**: dentro de cada módulo, pastas por camada — `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `enums/`, `exceptions/` (e `auth/`, `config/`, `validation/` quando necessário) — com nomes `XxxController`, `XxxService`, `XxxRepository`, `XxxRequest`/`XxxResponse`/`XxxDto`, `XxxException`. A API pública de um módulo é marcada com `@NamedInterface` (ex.: `auditoria/service`, `auditoria/dto`, `auditoria/enums`).

---

## 4. Pacientes e cadastro

### 4.1 Origem do cadastro (sempre registrada)

| Origem | Como acontece |
|---|---|
| `PACIENTE_LINK` | Paciente preenche no próprio celular via link (e-mail/WhatsApp) ou QR code |
| `PACIENTE_QUIOSQUE` | Paciente (ou acompanhante) preenche e assina **ele mesmo** num tablet da recepção em modo quiosque |
| `RECEPCAO` | Recepção preenche pelo paciente (ex.: sem celular, não alfabetizado). Ficha lida em voz alta, paciente assina; registra o usuário da recepção e, se houver, testemunha |

### 4.2 Fluxo recomendado (link/QR)

1. Recepção cria **pré-cadastro mínimo**: nome, CPF, telefone e/ou e-mail.
2. Sistema gera `ConviteCadastro`: token aleatório (≥ 256 bits, guardado como hash), **uso único**, expira em 72h, vinculado àquele paciente. Exibe QR code e/ou envia por e-mail.
3. Paciente abre o link → confirma identidade (data de nascimento ou últimos dígitos do CPF) → completa dados pessoais → responde anamnese → lê termos → assina (OTP + desenho opcional).
4. Registro selado: PDF gerado, hash SHA-256 guardado, origem `PACIENTE_LINK`.

### 4.3 Regras

- Confirmação por e-mail/SMS **não é obrigatória** para concluir cadastro (pacientes sem celular). É obrigatória apenas quando a origem é `PACIENTE_LINK`.
- Menor de 18 anos: exige `ResponsavelLegal` (nome, CPF, parentesco), e é o responsável quem aceita termos e assina (LGPD art. 14).
- **CPF (D-053)**:
  - Adulto: CPF obrigatório. Exceção (adulto sem CPF) só com aprovação do MASTER, justificativa escrita e registro em auditoria.
  - Menor de 18 anos: CPF do paciente **opcional**; se ausente, o CPF do responsável legal é **obrigatório**.
  - Sem CPF: o sistema alerta possível duplicidade quando nome + data de nascimento + responsável coincidirem com outro cadastro.
  - CPF único por paciente (via `cpf_hash`, índice único parcial); validação de dígitos; normalizado (só dígitos) antes de cifrar e calcular o hash.
- Alteração de dados cadastrais gera nova versão (histórico), com quem alterou e origem.

---

## 5. Anamnese (ficha de triagem)

- `ModeloAnamnese` versionado (perguntas, tipos, obrigatoriedade). Respostas sempre ligadas à versão do modelo usada.
- Cada `FichaAnamnese` tem: origem (§4.1), data, hash do PDF, assinatura do paciente/responsável.
- **Imutável.** Paciente atualiza → nova ficha; as anteriores permanecem.
- **Validade**: nova ficha exigida a cada novo tratamento, antes de qualquer `CIRURGIA`/`IMPLANTE`, e quando a última tiver mais de N meses (configurável, padrão 12).
- O profissional registra que **revisou** a ficha com o paciente antes do procedimento (data/hora + usuário).
- Alerta visual de alergias/condições relevantes no topo do prontuário.

---

## 6. Prontuário

- **1 prontuário por paciente** (pertence ao paciente, não ao profissional).
- Acesso do profissional via `VinculoProfissionalPaciente` (início, fim, criado por, motivo). Um paciente pode ter **vários vínculos ativos** (ex.: ortodontia com A, implante com B).
- Transferência pela recepção/master = encerrar vínculo + abrir novo, com motivo. Histórico preservado.
- `Procedimento`: categoria, descrição clínica, dente/região, profissional executor, data, status de assinatura. **Imutável** após assinatura; correção via `AdendoProcedimento`.
- Cada procedimento gera documento assinado pelo **profissional e pelo paciente** (§9).
- **Acesso de emergência** (profissional sem vínculo, ex.: colega de férias): exige justificativa escrita, concede acesso temporário (ex.: 24h), registra em auditoria e notifica o MASTER.
- Toda leitura de prontuário é auditada (quem, quando, o quê).
- Retenção: prontuário **não é apagado** a pedido do titular — guarda obrigatória por lei (Lei 13.787/2018 menciona 20 anos a partir do último registro; validar com advogado).

---

## 7. Agenda

- `Consultorio` (nome, ativo). Hoje 1, sistema suporta N.
- `DisponibilidadeProfissional`: regras recorrentes por profissional **e** consultório (dia da semana, hora início/fim, vigência) — horários fixos diários/semanais.
- `ExcecaoAgenda`: folgas, férias, feriados, horários extras pontuais (dinâmicos).
- `BloqueioAgenda`: bloqueio de horário sem paciente.
- `Agendamento`: paciente, profissional, consultório, início, fim, procedimento previsto, observação, origem.
- Status: `AGENDADO → CONFIRMADO → PACIENTE_CHEGOU → EM_ATENDIMENTO → CONCLUIDO`, ou `FALTOU` / `CANCELADO` (com motivo e quem cancelou). Remarcar = cancelar com motivo "remarcado" + novo agendamento ligado ao anterior.
- **Conflitos impedidos no banco**: `EXCLUDE USING gist` com `tstzrange` para (profissional, período) e (consultório, período), ignorando cancelados.
- **Check-in**: recepção marca `PACIENTE_CHEGOU` → notificação em tempo real para o profissional (WebSocket/STOMP), com som e confirmação de leitura.

---

## 8. Financeiro

### 8.1 Entidades

- `TabelaPreco` (opcional): preço sugerido por categoria/procedimento.
- `Orcamento` → `ItemOrcamento` (categoria, descrição, profissional, **valorProcedimento**).
- `Desconto` por item: tipo `PERCENTUAL` ou `VALOR`, motivo obrigatório, usuário. Definido pela recepção (ADMIN/MASTER). Acima de um limite configurável → exige aprovação do MASTER.
- `Pagamento` (N por orçamento — permite pagamento misto, ex. parte PIX + parte cartão).
- `Parcela` (para formas parceladas).
- `ConfiguracaoTaxa` (forma, nº parcelas, bandeira opcional, % e valor fixo, provedor, **vigência**).
- `ConfiguracaoRepasse` (categoria, % profissional, **vigência**). % clínica = 100 − % profissional.
- `Repasse` (valor devido ao profissional por item, status).

### 8.2 Formas de pagamento

`PIX`, `DEBITO`, `CREDITO` (1x a 12x), `BOLETO` (à vista ou carnê parcelado), `DINHEIRO`.
Preparado para `GATEWAY` (API de pagamentos) no futuro, com taxa do gateway como componente separado.

### 8.3 Fórmulas (decisão D-010: modo "garantir o líquido")

```
valorProcedimento   = valor do item
desconto            = PERCENTUAL: valorProcedimento × % | VALOR: valor fixo
base                = valorProcedimento − desconto                  (o que a clínica quer receber líquido)
taxa                = taxa da forma/parcelas vigente (ConfiguracaoTaxa)
valorCobrado        = arredonda( (base + taxaFixa) / (1 − taxa%) )  (o que o paciente paga)
valorTaxa           = valorCobrado − base                           (fica com a operadora)
valorParcela        = valorCobrado / nParcelas  (centavos restantes distribuídos nas primeiras parcelas)

valorProfissional   = valorProcedimento × %profissional            (D-012: base = valor CHEIO do procedimento — A CONFIRMAR)
valorClinica        = base − valorProfissional
```

- Modo de cálculo configurável: `GARANTIR_LIQUIDO` (padrão) ou `ACRESCIMO_SIMPLES` (`valorCobrado = base × (1 + taxa%)`), guardado no snapshot.
- Arredondamento: `HALF_EVEN`, 2 casas. A soma das parcelas é **exatamente** igual ao `valorCobrado`.
- Se `valorClinica` ficar negativo ou abaixo de um limite configurável (desconto alto) → alerta e exige aprovação do MASTER.
- O valor total, a taxa, o valor por parcela e a quantidade de parcelas **são exibidos ao paciente antes** de confirmar.

**Exemplos (taxas de referência da §8.5):**

| Caso | Base | Cobrado | Parcelas | Taxa operadora | Clínica recebe | Profissional (60%) | Clínica fica |
|---|---|---|---|---|---|---|---|
| Implante R$ 1.000, crédito 3x (6,12%) | 1.000,00 | 1.065,19 | 355,07 + 2×355,06 | 65,19 | 1.000,00 | 600,00 | 400,00 |
| Mesmo, com 10% de desconto | 900,00 | 958,67 | 319,57 + 2×319,55 | 58,67 | 900,00 | 600,00 | 300,00 |
| Implante R$ 1.000, crédito 12x (12,40%) | 1.000,00 | 1.141,55 | 11×95,13 + 95,12 | 141,55 | 1.000,00 | 600,00 | 400,00 |

(Comparação: no modo antigo "somar por cima", 3x daria R$ 1.061,20 cobrados e a operadora levaria R$ 64,95 → clínica receberia R$ 996,25, não R$ 1.000.)

> No exemplo com desconto o profissional recebe sobre o valor cheio (R$ 1.000), ou seja, **a clínica arca com o desconto**. Isso segue a regra provisória D-012 — A CONFIRMAR.

### 8.4 Status e acompanhamento

`StatusPagamento`: `PENDENTE`, `PARCIAL`, `QUITADO`, `CANCELADO`, `ESTORNADO`.
`StatusParcela`: `PENDENTE`, `PAGA`, `ATRASADA`, `CANCELADA`.

- **Cartão parcelado**: a operadora assume o parcelamento → para a clínica o pagamento fica `QUITADO` na aprovação; parcelas exibidas apenas como informação (quantidade e valor). Recebíveis da operadora podem ser controlados à parte.
- **Boleto/carnê parcelado** (parcelamento próprio): cada parcela tem vencimento e status; o pagamento é `PARCIAL` enquanto houver parcela em aberto.
- Sempre exibir: total, pago, em aberto, parcelas pagas / restantes (ex.: "3 de 10 pagas — 7 restantes"), valor por parcela, próximo vencimento, atrasos.
- Histórico de mudanças de status (quem, quando).
- Cancelamento e estorno com motivo; estorno não apaga, gera lançamento inverso.

### 8.5 Tabela de taxas de referência (seed inicial)

> **Valores de referência apenas para desenvolver e testar as fórmulas.** Fonte: tabela pública da InfinitePay (faixa até R$ 20 mil/mês, Visa/Mastercard, com antecipação inclusa), consultada em 26/11/2026. Devem ser substituídos pela tabela real do provedor contratado (A CONFIRMAR — D-015). Tudo configurável pela tela de configurações, com vigência.

| Forma | Taxa |
|---|---|
| PIX | 0,00% |
| DÉBITO | 1,37% |
| CRÉDITO 1x | 3,15% |
| CRÉDITO 2x | 5,39% |
| CRÉDITO 3x | 6,12% |
| CRÉDITO 4x | 6,85% |
| CRÉDITO 5x | 7,57% |
| CRÉDITO 6x | 8,28% |
| CRÉDITO 7x | 8,99% |
| CRÉDITO 8x | 9,69% |
| CRÉDITO 9x | 10,38% |
| CRÉDITO 10x | 11,06% |
| CRÉDITO 11x | 11,74% |
| CRÉDITO 12x | 12,40% |
| BOLETO | 5,00% (valor legado — A CONFIRMAR; boletos costumam ter tarifa fixa por boleto, o modelo suporta % e valor fixo) |
| DINHEIRO | 0,00% |

Bandeiras Elo/Amex/Hipercard costumam ter taxas maiores; o modelo aceita taxa por bandeira (opcional).
Carnê/boleto parcelado próprio: juros ao mês configurável (padrão 0% — A CONFIRMAR).

### 8.6 Divisão profissional/clínica (seed inicial, configurável com vigência)

| Categoria | Profissional | Clínica |
|---|---|---|
| HARMONIZACAO_FACIAL | 70% | 30% |
| FACETA | 60% | 40% |
| IMPLANTE | 60% | 40% |
| ORTODONTIA | 50% | 50% |
| ENDODONTIA | 50% | 50% |
| PROTESE | 40% | 60% |
| CIRURGIA | 40% | 60% |
| CLINICA | 30% | 70% |

### 8.7 Snapshot

Cada `Pagamento` guarda: modo de cálculo, % e valor fixo da taxa, provedor, % profissional, valores calculados, usuário e data. Mudar configurações **nunca** altera pagamentos já registrados.

---

## 9. Documentos e assinaturas

- Documentos gerados em PDF (HTML Thymeleaf → openhtmltopdf), guardados em storage privado S3 compatível (criptografado; AWS SDK S3 com endpoint configurável — SeaweedFS em dev/teste, produção A CONFIRMAR — D-056/D-060), referenciados por **chave**, nunca URL pública. Hash SHA-256 guardado no banco.
- Tipos: ficha de cadastro + anamnese, termo de consentimento do procedimento, registro de procedimento, termos LGPD aceitos.
- **Paciente**: assinatura eletrônica avançada (Lei 14.063/2020) — OTP por e-mail/SMS ou presencial no tablet, desenho da assinatura, IP, dispositivo, data/hora, hash, trilha de evidências.
- **Profissional**: preferencialmente certificado digital ICP-Brasil (Resolução CFO 91/2009 – NGS2).
- **Plataforma de assinatura**: preferência por integrar uma existente (Clicksign, ZapSign, D4Sign…) via interface `ProvedorAssinatura` — escolha A CONFIRMAR (D-016). Até lá, implementação interna de assinatura avançada atrás da mesma interface.
- **Tablet na recepção** (se conseguido): assinatura presencial do próprio documento.
- **Envio**: e-mail **não leva o PDF com dado de saúde em anexo aberto**. Envia notificação com link autenticado de uso limitado ou PDF protegido por senha. Destinatários: procedimento → profissional, clínica, paciente; ficha de cadastro → clínica, paciente.

---

## 10. Notificações

- Tempo real (WebSocket/STOMP): chegada do paciente, mudanças de agenda.
- E-mail (Spring Mail): convites, confirmações, documentos (via link).
- Canais plugáveis (`CanalNotificacao`): SMS e WhatsApp Business no futuro.
- Mensagens nunca contêm dado clínico no corpo.

---

## 11. LGPD e segurança jurídica

**Bases legais**
- Atendimento e prontuário: tutela da saúde e cumprimento de obrigação legal (LGPD art. 11, II "a" e "f") — **não dependem de consentimento**.
- Financeiro: execução de contrato e obrigação legal.
- Consentimento específico, opcional e revogável apenas para usos extras (ex.: lembretes por WhatsApp, marketing).

**Termos**
- `TermoVersao` (tipo, versão, texto, vigência). `AceiteTermo` registra versão, data/hora, IP, dispositivo, origem.
- Aceite da política de privacidade/termos de uso obrigatório para concluir cadastro; consentimentos opcionais em separado e desmarcados por padrão.

**Direitos do titular**
- Fluxo para pedidos (acesso, correção, portabilidade, informação). Pedido de exclusão responde com a retenção obrigatória do prontuário (art. 16, I).

**Segurança técnica**
- TLS em tudo; senhas com Argon2id (D-074); 2FA para internos (D-072); rate limit em login e links.
- **Autenticação (D-071)**:
  - Login em 2 passos: CPF + senha → **desafio** (token interno de uso único, 5 min, máx. 5 códigos errados) → código 2FA → sessão.
  - **Token de acesso**: JWT assinado (RSA, chave do ambiente), 15 min, só na memória do frontend, enviado no header `Authorization`.
  - **Token de renovação**: valor aleatório opaco (hash no banco), 12 h, cookie `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`, rotação a cada uso; reuso de token já trocado revoga toda a cadeia (sessão).
  - Status do usuário checado a cada requisição (inativo perde acesso na hora).
- **2FA (D-072)**: TOTP (app autenticador, RFC 6238) + 10 códigos de recuperação de uso único (guardados como hash); alternativa: código de 6 dígitos por e-mail. Segredo TOTP guardado cifrado.
- **Reenvio de e-mails e códigos (D-073)**: vale para código 2FA por e-mail, recuperação de senha, convites de cadastro e códigos de assinatura. Espera crescente entre envios (1, 2, 5, 10 min); máximo **5 envios** por destinatário e finalidade, depois só após **1 hora**; código/link errado 5 vezes é invalidado. Tudo configurável e auditado; respostas sempre genéricas (não revelam se CPF/e-mail existe).
- Criptografia de campos sensíveis (anamnese, CPF) e de backups; documentos em storage privado.
- **CPF (D-052)**: cifrado com **AES-256-GCM** via `AttributeConverter` (IV aleatório por valor; o texto cifrado carrega o id da versão da chave). Coluna `cpf_hash` = **HMAC-SHA256** do CPF normalizado com chave secreta separada, para unicidade (índice único parcial `WHERE cpf_hash IS NOT NULL`) e busca. Nunca SHA puro (quebrável por força bruta). Vale para paciente, responsável legal e usuário interno (D-061), com o mesmo converter.
- **Gestão de chaves**:
  - Chaves vêm do ambiente (`CRIPTO_CHAVES` — mapa versão→chave, `CRIPTO_CHAVE_ATIVA`, `CPF_HMAC_CHAVE`), nunca do repositório.
  - **Chave de criptografia perdida = dado irrecuperável.** Backup das chaves em local separado do backup do banco (ex.: cofre de senhas + cópia offline), com acesso restrito ao MASTER/responsável técnico.
  - **Rotação**: adicionar nova versão de chave, torná-la ativa (novos valores usam a nova), re-cifrar os antigos em job controlado; a versão antiga só sai depois que nenhum dado a usa.
  - A chave do HMAC não é rotacionável sem recalcular todos os `cpf_hash` (exige job dedicado e aprovação).
  - Procedimento detalhado: documento operacional a escrever antes de produção (CHECKLIST).
- Princípio do menor privilégio (§2); DEV sem acesso a dados de pacientes em produção.
- `AuditLog` append-only: usuário, ação, entidade, id, data, IP, justificativa (quando houver). Garantido no banco por trigger que rejeita `UPDATE`/`DELETE` (D-076).
- Ambientes de dev/teste apenas com dados fictícios.
- Logs sem dados pessoais/sensíveis.

**Governança (documentos fora do código, mas rastreados no checklist)**
- Encarregado (DPO) nomeado; RIPD; registro das operações de tratamento; política de retenção; plano de resposta a incidentes (comunicação à ANPD e aos titulares no prazo regulamentar); contratos com operadores (hospedagem, e-mail, assinatura, gateway).
- Validar tudo com advogado especializado em saúde/LGPD.

---

## 12. Modelo de dados (resumo)

```
Usuario(id, nomeCompleto, cpf (cifrado), cpfHash UK, email UK, telefone, perfil, status ATIVO|INATIVO,
        areaAtuacao?, conselho?, numeroRegistro?, ufRegistro?   (obrigatórios para PROFISSIONAL — D-069),
        senhaHash, trocarSenha, totpSegredo (cifrado)?, totpAtivo, createdAt, updatedAt)
RefreshToken(id, tokenHash UK, usuario_id, cadeia_id, substituidoPor?, revogadoEm?, createdAt, expiresAt)
DesafioLogin(id, tokenHash UK, usuario_id, tentativas, usadoEm?, expiraEm)
CodigoRecuperacao2fa(id, usuario_id, codigoHash, usadoEm?)
EnvioCodigo(id, destinatarioHash, finalidade, codigoHash, tentativas, enviadoEm, expiraEm, usadoEm?)   (reenvio/cooldown — D-073)
Paciente(id, nomeCompleto, cpf? (cifrado), cpfHash? UK parcial, dataNascimento, telefone, email, endereco, origemCadastro,
         semCpfJustificativa?, semCpfAprovadoPor?, semCpfAprovadoEm?, createdAt, updatedAt)
ResponsavelLegal(id, paciente_id, nome, cpf (cifrado, obrigatório), cpfHash, parentesco, telefone, email)
ConviteCadastro(id, paciente_id, tokenHash, expiraEm, usadoEm, criadoPor)
ModeloAnamnese(id, versao, perguntas jsonb, ativo)
FichaAnamnese(id, paciente_id, modelo_id, respostas jsonb, origem, preenchidoPor, documento_id, createdAt)
RevisaoAnamnese(id, ficha_id, profissional_id, revisadoEm)
Prontuario(id, paciente_id UK, createdAt)
VinculoProfissionalPaciente(id, paciente_id, profissional_id, inicio, fim, criadoPor, motivo)
AcessoEmergencia(id, profissional_id, paciente_id, justificativa, inicio, fim)
Procedimento(id, prontuario_id, profissional_id, itemOrcamento_id?, categoria, descricao, denteRegiao, realizadoEm, documento_id, statusAssinatura)
AdendoProcedimento(id, procedimento_id, autor_id, texto, documento_id, createdAt)
Consultorio(id, nome, ativo)
DisponibilidadeProfissional(id, profissional_id, consultorio_id, diaSemana, horaInicio, horaFim, vigenciaInicio, vigenciaFim)
ExcecaoAgenda(id, profissional_id, consultorio_id?, inicio, fim, tipo, motivo)
BloqueioAgenda(id, consultorio_id, profissional_id?, periodo tstzrange, motivo)
Agendamento(id, paciente_id, profissional_id, consultorio_id, periodo tstzrange, categoriaPrevista, status, motivoCancelamento, agendamentoOrigem_id?, createdAt)
Orcamento(id, paciente_id, status, criadoPor, createdAt)
ItemOrcamento(id, orcamento_id, profissional_id, categoria, descricao, valorProcedimento, descontoTipo, descontoValor, descontoMotivo, aprovadoPor?)
ConfiguracaoTaxa(id, forma, parcelas, bandeira?, percentual, valorFixo, provedor, vigenciaInicio, vigenciaFim)
ConfiguracaoRepasse(id, categoria, percentualProfissional, vigenciaInicio, vigenciaFim)
Pagamento(id, orcamento_id, forma, parcelas, modoCalculo, base, valorCobrado, valorTaxa, snapshot jsonb, status, createdAt)
Parcela(id, pagamento_id, numero, valor, vencimento, pagoEm, status)
Repasse(id, itemOrcamento_id, profissional_id, valor, status, pagoEm)
Documento(id, tipo, storageKey, sha256, criadoEm)
Assinatura(id, documento_id, signatario tipo/id, metodo, evidencias jsonb, assinadoEm)
TermoVersao(id, tipo, versao, texto, vigenciaInicio)
AceiteTermo(id, termo_id, paciente_id, aceitoEm, ip, dispositivo, origem)
AuditLog(id, usuario_id, acao, entidade, entidadeId, ip, justificativa, ocorridoEm)
```

Todos os enums persistidos como STRING.

---

## 13. Arquitetura e stack

**Backend**: Java 25, Spring Boot 4.2.x, Maven, Spring Modulith, Spring Web MVC, Spring Security + OAuth2 Resource Server (JWT), Spring Data JPA, PostgreSQL, Flyway, Validation, WebSocket, Mail, Thymeleaf, Actuator, springdoc-openapi, MapStruct, openhtmltopdf, ZXing, SDK S3, Bucket4j.

Versões: Spring Boot **4.2.0**, Spring Modulith **2.2.0** (GA, via `spring-modulith-bom`), Lombok (gerenciado pelo Boot) + MapStruct.
Pacote base e `groupId`: **`br.com.clinicaviene`** (D-062); módulos do Modulith são subpacotes (`br.com.clinicaviene.pacientes`, …).

**Eventos do Modulith (D-059)**: `completion-mode=archive` (concluídos vão para `event_publication_archive`), `republish-outstanding-events-on-restart=true`, tabelas criadas pelo Flyway (inicialização automática desligada). Eventos carregam só ids, nunca dados pessoais.

**Frontend**: Angular **22.3** (standalone components, zoneless — sem zone.js, SCSS, sem SSR — D-057), **PrimeNG** (componentes) + **FullCalendar** (`@fullcalendar/angular`, agenda) — D-043; `signature_pad`, Vitest, Playwright.

**Infra**: Docker (imagens multi-stage, processos sem root), `compose.yaml` na raiz (Postgres `18.7-alpine`, Mailpit, SeaweedFS `4.55` como S3 de dev — D-056; backend e frontend no profile `app` — frontend em nginx com `/api/` encaminhado ao backend no mesmo domínio). Storage de produção: A CONFIRMAR após análise de custos (D-060), credenciais no `.env` (fora do Git) a partir de `.env.example`, GitHub Actions.

**Estrutura do repositório (D-044, D-064)**: `backend/` (Maven + `Dockerfile`), `frontend/` (Angular + `Dockerfile` + `nginx.conf`), `docs/`, `.github/workflows/`, `compose.yaml` e `.env.example` na raiz. Não há pasta `docker/` (criar só se surgir configuração de infraestrutura que não pertença a um dos projetos).

**Lombok (D-051)**: permitido `@Getter`, `@Setter` (só onde necessário), `@NoArgsConstructor(access = PROTECTED)` em entidades, `@RequiredArgsConstructor` em services, `@Slf4j`, `@Builder` pontual. Proibido `@Data`, `@EqualsAndHashCode`, `@ToString`, `@AllArgsConstructor` em entidades JPA e `@Value` em geral (usar `record`). `equals`/`hashCode` de entidade à mão, só pelo id. DTOs sempre `record`. `lombok.config` com `config.stopBubbling = true` e `lombok.addLombokGeneratedAnnotation = true`. Verificado por teste ArchUnit.

**Banco e migrations (D-047)**: Flyway com SQL puro em `backend/src/main/resources/db/migration/`; `ddl-auto=validate`. Extensões (`btree_gist`) e as tabelas `event_publication`/`event_publication_archive` do Modulith também vêm de migrations (D-049/D-050/D-059). Em dev, migrations só com `CREATE`. `INSERT` de dados fictícios apenas para testes, em `backend/src/test/resources/db/testdata/` (carregado só no perfil de teste). `ALTER`/`UPDATE`/`DELETE`/`DROP` somente com aprovação do autor. Seeds de configuração real (taxas, repasses, termos, primeiro MASTER): forma A CONFIRMAR (D-048).

**Padrões**: DTOs com records; controllers finos; regras no domínio/serviço; erros com Problem Details (RFC 9457); API versionada `/api/v1`; OpenAPI documentado.

**Pontos de extensão futuros**
- `GatewayPagamento` (adaptadores por provedor; webhooks idempotentes; taxa do gateway separada).
- `ProvedorAssinatura`.
- `CanalNotificacao` (e-mail, SMS, WhatsApp).
- **Chatbot da recepção**: chama os mesmos casos de uso com as mesmas permissões do usuário logado; confirmação explícita antes de qualquer ação que altere dados; nunca envia dado clínico ao provedor de LLM sem contrato e base legal (transferência internacional). Ações permitidas a definir.

---

## 14. Testes e CI/CD

- **Backend unitário**: JUnit 5 + Mockito + AssertJ. Foco total nas fórmulas financeiras (tabela de casos, arredondamento, soma das parcelas).
- **Backend integração**: `@SpringBootTest` + Testcontainers (PostgreSQL; SeaweedFS via `GenericContainer` para storage) + REST Assured + GreenMail. Testes de autorização por perfil.
- **Arquitetura**: Spring Modulith `verify()` + ArchUnit.
- **Frontend unitário**: Vitest.
- **E2E navegador**: Playwright contra o ambiente docker-compose.
- **Cobertura**: JaCoCo (meta inicial 80% em `financeiro`, `prontuario`, `identidade`).
- **CI (GitHub Actions)** — `.github/workflows/ci.yml`, roda em **todo push** (desenvolvimento direto na `main` — D-058). Hoje: job `backend` (build + testes unitários e de integração com `./mvnw verify` + relatórios de teste e JaCoCo como artefatos), job `frontend` (npm ci, lint com angular-eslint, Prettier, Vitest, build de produção) e job `docker` (valida `compose.yaml` e constrói as imagens do backend e do frontend, sem publicar). Meta: testes front, E2E, cobertura, Spotless/lint, scan de dependências (Dependabot/Trivy), build da imagem Docker, adicionados conforme existirem. CI vermelho na `main` é corrigido antes de qualquer outra implementação. Formatação do backend (Spotless/palantir) com check desligado até a primeira formatação (D-063).
- **CD**: **adiado** (D-046) — sem deploy por enquanto. Quando houver hospedagem: publicação da imagem no GHCR em merge na `main` + deploy.

---

## 15. Glossário

- **Base**: valor que a clínica deve receber líquido (procedimento − desconto).
- **Valor cobrado**: o que o paciente paga (base + taxa da forma de pagamento).
- **Repasse**: parte do profissional.
- **Snapshot**: cópia das configurações usadas num pagamento.
- **Vínculo**: permissão de um profissional para atuar no prontuário de um paciente.
