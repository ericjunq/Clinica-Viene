import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { TipoTermo } from '../../core/models/cadastro.model';
import { Termo } from '../../core/models/configuracao.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { ROTULO_TERMO } from '../../core/models/paciente.model';
import { ConfiguracaoService } from '../../core/services/configuracao.service';

const TIPOS: TipoTermo[] = [
  'POLITICA_PRIVACIDADE',
  'CONSENTIMENTO_LEMBRETES',
  'CONSENTIMENTO_PROMOCOES',
  'CONSENTIMENTO_IMAGEM',
  'CONSENTIMENTO_PESQUISA',
];

interface GrupoTermo {
  tipo: TipoTermo;
  versoes: Termo[];
  vigente: Termo | null;
  rascunho: Termo | null;
}

/** Termos LGPD versionados (D-093, D-096): o MASTER edita rascunhos e publica; o paciente lê a vigente. */
@Component({
  selector: 'app-termos',
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './termos.component.html',
  styleUrl: './configuracoes.scss',
})
export class TermosComponent implements OnInit {
  private readonly configuracao = inject(ConfiguracaoService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly rotulo = ROTULO_TERMO;
  protected readonly termos = signal<Termo[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  /** Termo em edição: id do rascunho, ou o tipo de uma nova versão. */
  protected readonly editando = signal<{ id: string | null; tipo: TipoTermo } | null>(null);

  protected readonly form = inject(FormBuilder).nonNullable.group({
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    texto: ['', [Validators.required, Validators.maxLength(100000)]],
  });

  protected readonly grupos = computed<GrupoTermo[]>(() =>
    TIPOS.map((tipo) => {
      const versoes = this.termos()
        .filter((t) => t.tipo === tipo)
        .sort((a, b) => b.versao - a.versao);
      return {
        tipo,
        versoes,
        vigente: versoes.find((t) => t.status === 'PUBLICADO') ?? null,
        rascunho: versoes.find((t) => t.status === 'RASCUNHO') ?? null,
      };
    }),
  );

  ngOnInit(): void {
    this.carregar();
  }

  protected carregarExemplos(): void {
    this.executar(
      this.configuracao.carregarTermosExemplo(),
      'Textos de exemplo carregados como rascunho.',
    );
  }

  protected editar(termo: Termo): void {
    this.form.reset({ titulo: termo.titulo, texto: termo.texto });
    this.editando.set({ id: termo.id, tipo: termo.tipo });
  }

  /** Nova versão partindo do texto da vigente (ou em branco). */
  protected novaVersao(grupo: GrupoTermo): void {
    this.form.reset({
      titulo: grupo.vigente?.titulo ?? this.rotulo[grupo.tipo],
      texto: grupo.vigente?.texto ?? '',
    });
    this.editando.set({ id: null, tipo: grupo.tipo });
  }

  protected salvar(): void {
    const edicao = this.editando();
    if (!edicao || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { titulo, texto } = this.form.getRawValue();
    const pedido = edicao.id
      ? this.configuracao.editarTermo(edicao.id, titulo.trim(), texto)
      : this.configuracao.criarTermo(edicao.tipo, titulo.trim(), texto);
    this.executar(pedido, 'Rascunho salvo.', () => this.editando.set(null));
  }

  protected publicar(termo: Termo): void {
    const aviso =
      `Publicar a versão ${termo.versao} de "${this.rotulo[termo.tipo]}"? ` +
      'Depois de publicada ela não pode ser alterada e passa a valer para os novos cadastros.';
    if (!window.confirm(aviso)) return;
    this.executar(this.configuracao.publicarTermo(termo.id), 'Versão publicada.');
  }

  private executar(pedido: Observable<unknown>, sucesso: string, depois?: () => void): void {
    this.ocupado.set(true);
    this.erro.set(null);
    pedido.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.avisos.open(sucesso, 'OK', { duration: 3000 });
        depois?.();
        this.carregar();
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  private carregar(): void {
    this.configuracao.termos().subscribe({
      next: (lista) => this.termos.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
