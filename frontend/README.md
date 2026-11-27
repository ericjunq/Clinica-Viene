# ClinicaViene — Frontend

Aplicação Angular 22 (standalone, zoneless) com Angular Material. Visão geral do projeto, configuração do `.env` e formas de rodar tudo: veja o [README da raiz](../README.md).

## Rodando em desenvolvimento

Com o backend no ar em `http://localhost:8080` (veja o README da raiz):

```bash
npm ci
npm start          # http://localhost:4200 — /api é encaminhado ao backend (proxy.conf.json)
```

## Scripts

| Comando | O que faz |
| --- | --- |
| `npm start` | Servidor de desenvolvimento com recarga automática |
| `npm run build` | Build de produção em `dist/` |
| `npm test` | Testes unitários (Vitest) |
| `npm run lint` | ESLint (angular-eslint) |
| `npm run format` / `format:check` | Prettier |
| `npm run e2e` | Testes E2E (Playwright), com o ambiente completo no ar, banco novo e `E2E_DEV_CPF`/`E2E_DEV_SENHA` definidos |

## Organização

```
src/app/
├── core/       modelos, serviços de API, interceptors, guards e validadores
├── layout/     estrutura da área logada (barra superior, navegação)
├── pages/      telas (login, 2FA, usuários, pacientes, configurações, cadastro público…), carregadas sob demanda
└── shared/     componentes reutilizáveis (logo, máscara de CPF)
e2e/            testes Playwright
```

O token de acesso fica só em memória; a renovação usa um cookie `HttpOnly` definido pelo backend. Em container, o frontend é servido pelo Nginx (`nginx.conf`), que também encaminha `/api` ao backend. Os avisos em tempo real (chegada do paciente, agenda alterada) usam STOMP sobre WebSocket em `/api/v1/ws` (`@stomp/stompjs`); o Nginx e o proxy de desenvolvimento (`proxy.conf.json`, `ws: true`) repassam o `Upgrade`.
