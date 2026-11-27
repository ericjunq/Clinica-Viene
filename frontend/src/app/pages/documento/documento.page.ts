import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ActivatedRoute } from '@angular/router';
import { mensagemDeErro } from '../../core/models/erro.model';
import { DocumentoPublicoService } from '../../core/services/documento-publico.service';
import { CpfMaskDirective } from '../../shared/cpf-mask/cpf-mask.directive';
import { LogoComponent } from '../../shared/logo/logo.component';
import { tokenDoFragmento } from '../../shared/token-do-link';

/**
 * Download da ficha assinada pelo link do e-mail (D-094): o token vem depois do # e a página pede o CPF do
 * paciente (ou do responsável, se o paciente não tiver CPF) antes de liberar o PDF.
 */
@Component({
  selector: 'app-documento-page',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    CpfMaskDirective,
    LogoComponent,
  ],
  templateUrl: './documento.page.html',
  styleUrl: '../pagina-publica.scss',
})
export class DocumentoPage implements OnInit {
  private readonly servico = inject(DocumentoPublicoService);
  private readonly rota = inject(ActivatedRoute);

  protected token: string | null = null;
  protected readonly enviando = signal(false);
  protected readonly erro = signal<string | null>(null);
  protected readonly baixado = signal(false);
  protected readonly cpf = inject(FormBuilder).nonNullable.control('', [
    Validators.required,
    Validators.pattern(/^\d{3}\.?\d{3}\.?\d{3}-?\d{2}$/),
  ]);

  ngOnInit(): void {
    this.token = tokenDoFragmento(this.rota.snapshot.fragment);
    history.replaceState(history.state, '', '/documento');
    if (!this.token) {
      this.erro.set('Link incompleto. Abra o link exatamente como recebeu no e-mail.');
    }
  }

  protected baixar(): void {
    if (!this.token || this.cpf.invalid || this.enviando()) {
      this.cpf.markAsTouched();
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    this.servico.baixar(this.token, this.cpf.value).subscribe({
      next: (pdf) => {
        salvarArquivo(pdf, 'ficha-cadastro-viene.pdf');
        this.baixado.set(true);
        this.enviando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }
}

function salvarArquivo(conteudo: Blob, nome: string): void {
  const url = URL.createObjectURL(conteudo);
  const link = document.createElement('a');
  link.href = url;
  link.download = nome;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
