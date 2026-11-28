# ClinicaViene — Dúvidas abertas

> Dúvidas que surgiram durante o trabalho, para discutir com o autor.
> Depois de respondida, a dúvida vira decisão em `DECISOES.md` (aceita/recusada/a confirmar) e sai daqui.
> Formato: `Q-xxx — título` · data · contexto · opções · sugestão.

---



Respondidas em 2026-11-26: Q-001 → D-056/D-060 · Q-002 → D-061 · Q-003 → D-062 · Q-004 → D-063 · Q-005 → D-064 · Q-006 → D-059 · Q-007 → D-065 · Q-008 (push) e Q-009 (branches) resolvidas · Q-010 → D-058.

**Q-011 — resolvida em 2026-11-26**: trocado para Angular Material (D-084).

**Q-012 — Arquivo oficial do logo** · 2026-11-26
O monograma "V" do sistema é um desenho em SVG que reproduz o logo das imagens. Se houver o arquivo original (de preferência SVG, ou PNG com fundo transparente), ele substitui o desenho.

**Q-013 — resolvida em 2026-11-27**: área escolhida no convite, com a coluna incluída direto no `CREATE TABLE` da V14 (D-101).

**Q-014 — Quem pode criar (e promover a) MASTER** · 2026-11-27
Hoje DEV **e** MASTER criam MASTER (CONTEXTO §2, `PerfilPermissoes.podeCriar`), e a mesma regra vale para promover um usuário a MASTER (`PATCH /usuarios/{id}/perfil`). O autor levantou a possibilidade de só o DEV criar MASTER. A decidir:
1. Criar MASTER: só DEV, ou DEV e MASTER (como hoje)?
2. Promover a MASTER: segue a mesma regra?
3. Ativar/desativar MASTER: continua com o MASTER (o DEV não desativa MASTER — D-067)?
Impacto da mudança: `PerfilPermissoes` + `PerfilPermissoesTest`, matriz do `UsuarioControllerIT`, opções de perfil na tela de usuários, CONTEXTO §2 e nova decisão (ajusta D-067).
