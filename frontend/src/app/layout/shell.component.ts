import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { MenubarModule } from 'primeng/menubar';
import { AuthService } from '../core/services/auth.service';

/** Moldura das telas autenticadas: menu (conforme o perfil) e saída. */
@Component({
  selector: 'app-shell',
  imports: [RouterOutlet, ButtonModule, MenubarModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly usuario = this.auth.usuario;
  protected readonly itens = computed<MenuItem[]>(() => {
    const perfil = this.usuario()?.perfil;
    const itens: MenuItem[] = [{ label: 'Início', icon: 'pi pi-home', routerLink: '/' }];
    if (perfil === 'DEV' || perfil === 'MASTER') {
      itens.push({ label: 'Usuários', icon: 'pi pi-users', routerLink: '/usuarios' });
    }
    itens.push({ label: 'Trocar senha', icon: 'pi pi-key', routerLink: '/trocar-senha' });
    return itens;
  });

  protected sair(): void {
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login']),
      error: () => void this.router.navigate(['/login']),
    });
  }
}
