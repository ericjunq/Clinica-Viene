import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

/** Tela inicial (os módulos de agenda, pacientes etc. entram nas próximas fases). */
@Component({
  selector: 'app-inicio-page',
  template: `
    <h1>Olá, {{ usuario()?.nomeCompleto }}</h1>
    <p>Bem-vindo(a) ao ClinicaViene.</p>
  `,
})
export class InicioPage {
  protected readonly usuario = inject(AuthService).usuario;
}
