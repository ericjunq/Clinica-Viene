import { Component, input } from '@angular/core';

/**
 * Marca Viene: monograma "V" + "VIENE / ODONTOLOGIA & SAÚDE".
 * O monograma é um desenho em SVG que reproduz o logo; quando houver o arquivo oficial (SVG), basta
 * substituir o conteúdo do <svg>.
 */
@Component({
  selector: 'app-logo',
  template: `
    <div class="logo" [class.compacto]="compacto()">
      <svg class="monograma" viewBox="0 0 100 100" aria-hidden="true">
        <path
          d="M39 39 L31 25 C27 18 17 19 16 27 C15.6 30 16.5 32 18 34.5 L35 63 C39 70 47 70 51 63 L73 25 C77 18 87 19 88 27 C88.4 30 87.5 32 86 34.5 L58 81 C55 86 48 86 45 81 L38 69 L45 69"
        />
      </svg>
      @if (!somenteMonograma()) {
        <div class="nome">
          <span class="viene">VIENE</span>
          <span class="subtitulo">ODONTOLOGIA &amp; SAÚDE</span>
        </div>
      }
    </div>
  `,
  styles: `
    .logo {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      color: var(--viene-caramelo);
    }
    .monograma {
      width: 3.25rem;
      height: 3.25rem;
      fill: none;
      stroke: currentColor;
      stroke-width: 8.5;
      stroke-linecap: round;
      stroke-linejoin: round;
    }
    .nome {
      display: flex;
      flex-direction: column;
      line-height: 1.1;
    }
    .viene {
      font-family: var(--viene-fonte-titulo);
      font-size: 2rem;
      font-weight: 500;
      letter-spacing: 0.45em;
      color: var(--viene-marrom);
    }
    .subtitulo {
      font-size: 0.6rem;
      font-weight: 600;
      letter-spacing: 0.3em;
      color: var(--viene-marrom);
    }
    .compacto .monograma {
      width: 2.25rem;
      height: 2.25rem;
    }
    .compacto .viene {
      font-size: 1.35rem;
    }
    .compacto .subtitulo {
      font-size: 0.45rem;
    }
  `,
})
export class LogoComponent {
  readonly compacto = input(false);
  readonly somenteMonograma = input(false);
}
