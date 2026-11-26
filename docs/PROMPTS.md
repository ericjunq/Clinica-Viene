# ClinicaViene — Prompts para o Claude Code

## 1. Prompt inicial (primeira sessão)

Cole no Claude Code, na raiz do repositório, depois de copiar `CLAUDE.md`, `.claude/settings.json` e a pasta `docs/` e de colocar o projeto do Spring Initializr em `backend/`.

```
Você vai desenvolver comigo o sistema ClinicaViene, um sistema de gestão para clínica odontológica.

ANTES DE QUALQUER COISA, leia na íntegra: CLAUDE.md, docs/CONTEXTO.md, docs/DECISOES.md e docs/CHECKLIST.md.
Esses arquivos são a fonte da verdade do projeto. Depois de ler, me dê um resumo de 5 linhas do que entendeu
e liste qualquer contradição ou lacuna que encontrar antes de começar.

Regras de trabalho (permanentes, valem para todas as sessões):

1. LGPD à risca e máxima integridade e segurança jurídica para a clínica em toda decisão.
   Se uma solução for mais simples mas mais fraca juridicamente, aponte o trade-off e pergunte.
2. Trabalhamos em fases e em partes pequenas (docs/CHECKLIST.md). Uma implementação por vez, com commits direto na main (D-058).
   (Texto original, substituído: "cada uma numa branch:
   backend-nome-da-implementacao, frontend-nome-da-implementacao, docker-nome-da-implementacao,
   ci-nome-da-implementacao, docs-nome-da-implementacao — sempre a partir da main atualizada.
3. Commits no formato "tipo: descrição" (feat, fix, refactor, test, docs, chore, ci, build, perf, style), em português.
   NUNCA adicione Co-Authored-By, "Generated with Claude Code" ou qualquer atribuição de IA em commits ou PRs.
4. A cada implementação, junto com o código, atualize obrigatoriamente:
   - docs/DECISOES.md: toda ideia que discutirmos, aceita, recusada ou a confirmar, com data e motivo
     (nunca apague; marque como substituída);
   - docs/CHECKLIST.md: marque o que foi feito e adicione o que surgiu;
   - docs/CONTEXTO.md: qualquer mudança de regra, entidade, fluxo ou arquitetura.
   O objetivo é que eu consiga continuar o projeto em outro computador ou outro chat só com o repositório.
5. Testes reais: toda funcionalidade vem com testes unitários, de integração (Testcontainers) e,
   quando houver tela, E2E no navegador (Playwright). Rode os testes. Nunca diga que passaram sem ver a saída.
   Se este computador não tiver JDK 25 ou Docker, diga isso claramente, liste os testes não executados
   e marque o item como "implementado — testes pendentes de execução". Nunca simule resultados.
6. Itens "A CONFIRMAR" em docs/DECISOES.md não são implementados de forma definitiva:
   implemente configurável ou me pergunte.
7. Antes de implementar algo grande, apresente um plano curto (arquivos, entidades, endpoints, testes)
   e espere meu ok. Para coisas pequenas e claras, siga direto.
8. Ao terminar cada implementação: rode os testes, faça o commit, faça o push na main, acompanhe o CI e me diga
   o que foi feito, o que foi testado (com resultado real) e o próximo passo sugerido do checklist.

Tarefa desta sessão — Fase 0 (fundação):
- Organize o monorepo: backend/ (projeto Spring já gerado), frontend/ (Angular), docker/, docs/, .github/workflows/.
- Complete o pom.xml com as dependências manuais listadas em docs/CHECKLIST.md (Fase 0), com versões compatíveis com Spring Boot 4.2.x.
- Configure JaCoCo, Spotless e Surefire/Failsafe (unitários x integração).
- Crie docker-compose.yml (PostgreSQL, MinIO, Mailpit, backend, frontend), Dockerfiles multi-stage e .env.example.
- Crie o workflow de CI do GitHub Actions (build, testes back, testes front, lint, build de imagem).
- Crie o teste de verificação de módulos do Spring Modulith.
- Um commit por item, direto na main (D-058).
- Atualize docs/CHECKLIST.md e docs/DECISOES.md ao final.
```

## 2. Prompt de retomada (qualquer sessão nova, em qualquer máquina)

```
Vamos continuar o projeto ClinicaViene.
Leia CLAUDE.md, docs/CONTEXTO.md, docs/DECISOES.md e docs/CHECKLIST.md na íntegra.
Rode git status e git log --oneline -15 e verifique o status do último CI da main (gh run list --branch main --limit 3).
Depois me diga: (1) onde paramos, (2) itens em andamento ou com testes pendentes de execução,
(3) itens A CONFIRMAR que bloqueiam o próximo passo, (4) o próximo passo que você sugere.
Siga todas as regras do CLAUDE.md, incluindo commits sem coautoria e atualização dos docs a cada implementação.
```

## 3. Prompt para registrar uma discussão

```
Registre em docs/DECISOES.md o que decidimos agora: [descreva].
Classifique como aceita, recusada ou a confirmar, com data e motivo,
e reflita a mudança em docs/CONTEXTO.md e docs/CHECKLIST.md se necessário. Commit: "docs: ...".
```
