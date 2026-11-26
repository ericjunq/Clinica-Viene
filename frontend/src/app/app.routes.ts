import { Routes } from '@angular/router';
import {
  autenticadoGuard,
  desafioGuard,
  perfilGuard,
  sessaoCompletaGuard,
} from './core/guards/auth.guards';

/** Cada tela é carregada só quando aberta (lazy loading), para o carregamento inicial ficar leve. */
export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.page').then((m) => m.LoginPage),
    title: 'Entrar — ClinicaViene',
  },
  {
    path: 'dois-fatores',
    loadComponent: () =>
      import('./pages/dois-fatores/dois-fatores.page').then((m) => m.DoisFatoresPage),
    canActivate: [desafioGuard],
    title: 'Verificação — ClinicaViene',
  },
  {
    path: 'recuperar-senha',
    loadComponent: () =>
      import('./pages/recuperar-senha/recuperar-senha.page').then((m) => m.RecuperarSenhaPage),
    title: 'Recuperar senha — ClinicaViene',
  },
  {
    path: 'trocar-senha',
    loadComponent: () =>
      import('./pages/trocar-senha/trocar-senha.page').then((m) => m.TrocarSenhaPage),
    canActivate: [autenticadoGuard],
    title: 'Trocar senha — ClinicaViene',
  },
  {
    path: '',
    loadComponent: () => import('./layout/shell.component').then((m) => m.ShellComponent),
    canActivate: [autenticadoGuard, sessaoCompletaGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./pages/inicio/inicio.page').then((m) => m.InicioPage),
        title: 'ClinicaViene',
      },
      {
        path: 'usuarios',
        loadComponent: () => import('./pages/usuarios/usuarios.page').then((m) => m.UsuariosPage),
        canActivate: [perfilGuard('DEV', 'MASTER')],
        title: 'Usuários — ClinicaViene',
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
