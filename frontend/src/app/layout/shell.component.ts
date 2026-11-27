import { Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { LogoComponent } from '../shared/logo/logo.component';

interface ItemMenu {
  rotulo: string;
  icone: string;
  rota: string;
}

/** Moldura das telas autenticadas: menu (conforme o perfil) e saída. */
@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatButtonModule,
    MatIconModule,
    MatToolbarModule,
    LogoComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly itens = computed<ItemMenu[]>(() => {
    const perfil = this.usuario()?.perfil;
    const itens: ItemMenu[] = [{ rotulo: 'Início', icone: 'home', rota: '/' }];
    if (perfil === 'ADMIN' || perfil === 'MASTER' || perfil === 'PROFISSIONAL') {
      itens.push({ rotulo: 'Agenda', icone: 'calendar_month', rota: '/agenda' });
    }
    if (perfil === 'ADMIN' || perfil === 'MASTER') {
      itens.push({ rotulo: 'Pacientes', icone: 'patient_list', rota: '/pacientes' });
    }
    if (perfil === 'DEV' || perfil === 'MASTER') {
      itens.push({ rotulo: 'Usuários', icone: 'group', rota: '/usuarios' });
    }
    if (perfil === 'MASTER') {
      itens.push({ rotulo: 'Configurações', icone: 'settings', rota: '/configuracoes' });
    }
    itens.push({ rotulo: 'Trocar senha', icone: 'key', rota: '/trocar-senha' });
    return itens;
  });

  protected sair(): void {
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login']),
      error: () => void this.router.navigate(['/login']),
    });
  }
}
