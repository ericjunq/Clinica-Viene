import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputMaskModule } from 'primeng/inputmask';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';

/** Passo 1 do login: CPF + senha. */
@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CardModule,
    InputMaskModule,
    MessageModule,
    PasswordModule,
  ],
  templateUrl: './login.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class LoginPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
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
