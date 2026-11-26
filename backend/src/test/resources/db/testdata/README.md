# Dados fictícios para testes (D-047)

Migrations de `INSERT` com **dados fictícios** usados apenas nos testes de integração.
São carregadas só no perfil `test` (`application-test.yaml`) e nunca entram no jar da aplicação.

- Nunca colocar dados reais de pacientes aqui.
- Usar versões a partir de `V1000__` para não colidir com as migrations de `src/main/resources/db/migration`
  (ex.: `V1000__insere_usuarios_ficticios.sql`).
- `ALTER`, `UPDATE`, `DELETE`, `DROP`: só com aprovação explícita do autor.
