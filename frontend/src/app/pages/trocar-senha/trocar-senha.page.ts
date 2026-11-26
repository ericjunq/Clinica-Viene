import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';
import { LogoComponent } from '../../shared/logo/logo.component';
import { senhaForteValidator, senhasIguaisValidator } from '../../core/validators/senha.validator';

/** Troca da própria senha (obrigatória no primeiro acesso, com senha provisória). */
@Component({
  selector: 'app-trocar-senha-page',
  imports: [
    LogoComponent,
    ReactiveFormsModule,
    ButtonModule,
    CardModule,
    MessageModule,
    PasswordModule,
  ],
  templateUrl: './trocar-senha.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class TrocarSenhaPage {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  protected readonly obrigatoria = this.auth.usuario()?.trocarSenha ?? false;
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly form = inject(FormBuilder).nonNullable.group(
    {
      senhaAtual: ['', Validators.required],
      novaSenha: ['', [Validators.required, senhaForteValidator]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguaisValidator },
  );

  protected regrasFaltando(): string[] {
    return (this.form.controls.novaSenha.errors?.['senhaFraca'] as string[] | undefined) ?? [];
  }

  protected salvar(): void {
    if (this.form.invalid || this.enviando()) {
      this.form.markAllAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    const { senhaAtual, novaSenha } = this.form.getRawValue();
    this.auth.trocarSenha(senhaAtual, novaSenha).subscribe({
      next: () => void this.router.navigate(['/']),
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}
