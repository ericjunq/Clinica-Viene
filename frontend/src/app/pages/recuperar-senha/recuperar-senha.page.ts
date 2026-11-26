import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputMaskModule } from 'primeng/inputmask';
import { InputOtpModule } from 'primeng/inputotp';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';
import { senhaForteValidator, senhasIguaisValidator } from '../../core/validators/senha.validator';

/** Senha esquecida (1G): CPF → código no e-mail → senha nova. O 2FA continua no próximo login. */
@Component({
  selector: 'app-recuperar-senha-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CardModule,
    InputMaskModule,
    InputOtpModule,
    MessageModule,
    PasswordModule,
  ],
  templateUrl: './recuperar-senha.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class RecuperarSenhaPage {
  private readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly etapa = signal<'cpf' | 'codigo' | 'concluido'>('cpf');
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly info = signal<string | null>(null);

  protected readonly formCpf = this.fb.group({ cpf: ['', Validators.required] });
  protected readonly formSenha = this.fb.group(
    {
      codigo: ['', [Validators.required, Validators.minLength(6)]],
      novaSenha: ['', [Validators.required, senhaForteValidator]],
      confirmacao: ['', Validators.required],
    },
    { validators: senhasIguaisValidator },
  );

  protected regrasFaltando(): string[] {
    return (this.formSenha.controls.novaSenha.errors?.['senhaFraca'] as string[] | undefined) ?? [];
  }

  protected pedirCodigo(): void {
    if (this.formCpf.invalid || this.enviando()) {
      this.formCpf.markAllAsTouched();
      return;
    }
    this.iniciar();
    this.auth.recuperarSenha(this.formCpf.getRawValue().cpf).subscribe({
      next: (r) => {
        this.info.set(r.mensagem);
        this.etapa.set('codigo');
        this.enviando.set(false);
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  protected redefinir(): void {
    if (this.formSenha.invalid || this.enviando()) {
      this.formSenha.markAllAsTouched();
      return;
    }
    this.iniciar();
    const { codigo, novaSenha } = this.formSenha.getRawValue();
    this.auth.redefinirSenha(this.formCpf.getRawValue().cpf, codigo, novaSenha).subscribe({
      next: () => {
        this.etapa.set('concluido');
        this.enviando.set(false);
      },
      error: (e: unknown) => this.falhar(e),
    });
  }

  private iniciar(): void {
    this.enviando.set(true);
    this.erro.set(null);
  }

  private falhar(e: unknown): void {
    this.erro.set(mensagemDeErro(e));
    this.enviando.set(false);
  }
}
