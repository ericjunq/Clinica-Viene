# ClinicaViene — Dúvidas abertas

> Dúvidas que surgiram durante o trabalho, para discutir com o autor.
> Depois de respondida, a dúvida vira decisão em `DECISOES.md` (aceita/recusada/a confirmar) e sai daqui.
> Formato: `Q-xxx — título` · data · contexto · opções · sugestão.

---



Respondidas em 2026-11-26: Q-001 → D-056/D-060 · Q-002 → D-061 · Q-003 → D-062 · Q-004 → D-063 · Q-005 → D-064 · Q-006 → D-059 · Q-007 → D-065 · Q-008 (push) e Q-009 (branches) resolvidas · Q-010 → D-058.

**Q-011 — resolvida em 2026-11-26**: trocado para Angular Material (D-084).

**Q-012 — Arquivo oficial do logo** · 2026-11-26
O monograma "V" do sistema é um desenho em SVG que reproduz o logo das imagens. Se houver o arquivo original (de preferência SVG, ou PNG com fundo transparente), ele substitui o desenho.

**Q-013 — Área da anamnese escolhida no convite** · 2026-11-27
Hoje o cadastro usa o modelo de anamnese vigente de uma área configurada (`clinicaviene.cadastro-publico.area-anamnese`, padrão ODONTOLOGIA). Quando a clínica tiver outras áreas (psicologia etc.), a recepção precisará escolher a área ao gerar o convite. Isso exige uma coluna nova em `convite_cadastro` (**`ALTER TABLE`**, que pela D-047 só com sua aprovação) — ou uma tabela nova ligada ao convite. Aprova o `ALTER` quando chegar a hora, ou prefere a tabela nova?
