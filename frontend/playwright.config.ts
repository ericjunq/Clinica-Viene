import { defineConfig, devices } from '@playwright/test';

/**
 * Testes E2E no navegador contra o ambiente completo (docker compose --profile app): frontend em nginx,
 * backend, Postgres e Mailpit. No CI, o job "e2e" sobe o ambiente e define as variáveis E2E_*.
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env['E2E_URL'] ?? 'http://localhost:4200',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
