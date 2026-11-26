import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputOtpModule } from 'primeng/inputotp';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { SelectButtonModule } from 'primeng/selectbutton';
import { interval } from 'rxjs';
import { CadastroDoisFatoresResponse, MetodoDoisFatores } from '../../core/models/auth.model';
import { formatarEspera, mensagemDeErro } from '../../core/models/erro.model';
import { AuthService } from '../../core/services/auth.service';

/**
 * Passo 2 do login (D-072):
 * - primeiro acesso: cadastrar o app autenticador (QR code), confirmar com o primeiro código e guardar os
 *   códigos de recuperação;
 * - acessos seguintes: código do app, código por e-mail ou código de recuperação.
 */
@Component({
  selector: 'app-dois-fatores-page',
  imports: [
    FormsModule,
    RouterLink,
    ButtonModule,
    CardModule,
    InputOtpModule,
    InputTextModule,
    MessageModule,
    SelectButtonModule,
  ],
  templateUrl: './dois-fatores.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class DoisFatoresPage implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly etapa = computed(() => this.auth.desafio()?.etapa ?? null);
  protected readonly cadastro = signal<CadastroDoisFatoresResponse | null>(null);
  protected readonly codigosRecuperacao = this.auth.codigosRecuperacao;
  protected readonly erro = signal<string | null>(null);
  protected readonly info = signal<string | null>(null);
  protected readonly enviando = signal(false);

  protected readonly metodos: { label: string; value: MetodoDoisFatores }[] = [
    { label: 'App autenticador', value: 'TOTP' },
    { label: 'E-mail', value: 'EMAIL' },
    { label: 'Código de recuperação', value: 'RECUPERACAO' },
  ];
  protected metodo: MetodoDoisFatores = 'TOTP';
  protected codigo = '';

  private readonly agora = signal(Date.now());
  private readonly podeReenviarEm = signal<number | null>(null);
  protected readonly esperaReenvio = computed(() => {
    const limite = this.podeReenviarEm();
    return limite ? Math.max(0, Math.ceil((limite - this.agora()) / 1000)) : 0;
  });
  protected readonly formatarEspera = formatarEspera;

  ngOnInit(): void {
    interval(1000)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.agora.set(Date.now()));
    if (this.etapa() === 'CADASTRAR_2FA') {
      this.auth.iniciarCadastroDoisFatores().subscribe({
        next: (c) => this.cadastro.set(c),
        error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
      });
    }
  }

  protected confirmarCadastro(): void {
    this.executar(() => this.auth.confirmarCadastroDoisFatores(this.codigo.trim()), false);
  }

  protected verificar(): void {
    this.executar(() => this.auth.verificarDoisFatores(this.metodo, this.codigo.trim()), true);
  }

  protected enviarEmail(): void {
    this.erro.set(null);
    this.auth.enviarCodigoEmail().subscribe({
      next: (r) => {
        this.podeReenviarEm.set(new Date(r.podeReenviarEm).getTime());
        this.info.set('Enviamos um código para o seu e-mail. Ele vale por 10 minutos.');
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }

  protected trocarMetodo(): void {
    this.codigo = '';
    this.erro.set(null);
    this.info.set(null);
  }

  /** Depois de guardar os códigos de recuperação, entra no sistema. */
  protected concluir(): void {
    this.auth.codigosRecuperacao.set(null);
    void this.router.navigate(['/']);
  }

  private executar(
    acao: () => ReturnType<AuthService['verificarDoisFatores']>,
    entrarDireto: boolean,
  ): void {
    if (!this.codigo.trim() || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    acao().subscribe({
      next: () => {
        this.enviando.set(false);
        if (entrarDireto) {
          void this.router.navigate(['/']);
        }
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.codigo = '';
        this.enviando.set(false);
      },
    });
  }
}
