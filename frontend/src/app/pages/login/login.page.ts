import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';
import { CpfMaskDirective } from '../../shared/cpf-mask/cpf-mask.directive';
import { LogoComponent } from '../../shared/logo/logo.component';

/** Passo 1 do login: CPF + senha. */
@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    CpfMaskDirective,
    LogoComponent,
  ],
  templateUrl: './login.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly mostrarSenha = signal(false);
  protected readonly form = inject(FormBuilder).nonNullable.group({
    cpf: ['', Validators.required],
    senha: ['', Validators.required],
  });

  protected entrar(): void {
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    const { cpf, senha } = this.form.getRawValue();
    this.auth.login(cpf, senha).subscribe({
      next: () => void this.router.navigate(['/dois-fatores']),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
