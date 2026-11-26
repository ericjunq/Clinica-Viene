import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideBrowserGlobalErrorListeners,
} from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { MessageService } from 'primeng/api';
import { providePrimeNG } from 'primeng/config';
import { routes } from './app.routes';
import { VienePreset } from './core/theme/viene.preset';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { AuthService } from './core/services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withInterceptors([authInterceptor])),
    // Ao abrir/recarregar: recupera a sessão pelo cookie HttpOnly antes das rotas decidirem para onde ir.
    provideAppInitializer(() => inject(AuthService).restaurarSessao()),
    providePrimeNG({ theme: { preset: VienePreset, options: { darkModeSelector: false } } }),
    MessageService,
  ],
};
