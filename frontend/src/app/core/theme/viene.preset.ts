import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Identidade visual Viene Odontologia & Saúde: creme, caramelo (cor do monograma) e marrom.
 * O tom de ação (botões) é o caramelo escuro (#8C6240), que tem contraste suficiente com texto branco.
 */
export const VienePreset = definePreset(Aura, {
  semantic: {
    primary: {
      50: '#fbf6f1',
      100: '#f4e6d9',
      200: '#e9cdb4',
      300: '#ddb48f',
      400: '#d0a27c',
      500: '#c69a72',
      600: '#a87a52',
      700: '#8c6240',
      800: '#6b4a2e',
      900: '#4f361f',
      950: '#33220f',
    },
    colorScheme: {
      light: {
        primary: {
          color: '{primary.700}',
          contrastColor: '#ffffff',
          hoverColor: '{primary.800}',
          activeColor: '{primary.900}',
        },
        highlight: {
          background: '{primary.100}',
          focusBackground: '{primary.200}',
          color: '{primary.900}',
          focusColor: '{primary.950}',
        },
        surface: {
          0: '#ffffff',
          50: '#fbf6f1',
          100: '#f6ece3',
          200: '#eedfd2',
          300: '#e2cdbb',
          400: '#c9b19c',
          500: '#a8907b',
          600: '#85705d',
          700: '#655444',
          800: '#47392d',
          900: '#2e241c',
          950: '#1c1611',
        },
      },
    },
  },
});
