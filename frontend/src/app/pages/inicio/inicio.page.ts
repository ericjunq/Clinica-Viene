import { Component, inject } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';

/** Tela inicial (os módulos de agenda, pacientes etc. entram nas próximas fases). */
@Component({
  selector: 'app-inicio-page',
  template: `
    <section class="boas-vindas">
      <h1>Olá, {{ usuario()?.nomeCompleto }}</h1>
      <div class="divisor-viene">✦</div>
      <p class="frase">Cuidando de cada sorriso com excelência e confiança.</p>
    </section>
  `,
  styles: `
    .boas-vindas {
      text-align: center;
      padding: 3rem 1rem;
    }
    h1 {
      font-size: 3rem;
    }
    .divisor-viene {
      max-width: 24rem;
      margin: 0 auto 1rem;
    }
    .frase {
      font-family: var(--viene-fonte-titulo);
      font-style: italic;
      font-size: 1.5rem;
      color: var(--viene-caramelo);
    }
  `,
})
export class InicioPage {
  protected readonly usuario = inject(AuthService).usuario;
}
