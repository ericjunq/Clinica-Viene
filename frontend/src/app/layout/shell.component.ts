import { Component, OnDestroy, OnInit, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/services/auth.service';
import { AvisosService } from '../core/services/avisos.service';
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
export class ShellComponent implements OnInit, OnDestroy {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly avisos = inject(AvisosService);

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

  ngOnInit(): void {
    this.avisos.conectar();
  }

  ngOnDestroy(): void {
    this.avisos.desconectar();
  }

  protected sair(): void {
    this.avisos.desconectar();
    this.auth.logout().subscribe({
      complete: () => void this.router.navigate(['/login']),
      error: () => void this.router.navigate(['/login']),
    });
  }
}
