import { DatePipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { PerguntaAnamnese, TipoPergunta } from '../../core/models/cadastro.model';
import {
  ModeloAnamnese,
  TIPOS_PERGUNTA,
  codigoDaPergunta,
  normalizarPergunta,
  problemasDoModelo,
  temOpcoes,
} from '../../core/models/configuracao.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { ROTULO_AREA } from '../../core/models/paciente.model';
import { AreaAtuacao } from '../../core/models/usuario.model';
import { ConfiguracaoService } from '../../core/services/configuracao.service';

/** Pergunta em edição: opções como texto (uma por linha). */
interface PerguntaEditavel extends PerguntaAnamnese {
  opcoesTexto: string;
}

interface Edicao {
  id: string | null;
  areaAtuacao: AreaAtuacao;
  titulo: string;
  perguntas: PerguntaEditavel[];
}

/**
 * Modelos de ficha de saúde por área (D-089, D-096). O MASTER cuida das perguntas, mas nunca vê respostas
 * (D-088). Modelo publicado não muda: "Nova versão" duplica em rascunho.
 */
@Component({
  selector: 'app-modelos-anamnese',
  imports: [
    DatePipe,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  templateUrl: './modelos-anamnese.component.html',
  styleUrls: ['./configuracoes.scss', './modelos-anamnese.component.scss'],
})
export class ModelosAnamneseComponent implements OnInit {
  private readonly configuracao = inject(ConfiguracaoService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly rotuloArea = ROTULO_AREA;
  protected readonly areas = Object.keys(ROTULO_AREA) as AreaAtuacao[];
  protected readonly tipos = TIPOS_PERGUNTA;
  protected readonly temOpcoes = temOpcoes;

  protected readonly modelos = signal<ModeloAnamnese[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly edicao = signal<Edicao | null>(null);
  protected readonly problemas = signal<string[]>([]);
  protected novaArea: AreaAtuacao = 'ODONTOLOGIA';

  ngOnInit(): void {
    this.carregar();
  }

  protected temOdontologia(): boolean {
    return this.modelos().some((m) => m.areaAtuacao === 'ODONTOLOGIA');
  }

  protected vigente(m: ModeloAnamnese): boolean {
    const publicados = this.modelos().filter(
      (x) => x.areaAtuacao === m.areaAtuacao && x.status === 'PUBLICADO',
    );
    return m.status === 'PUBLICADO' && Math.max(...publicados.map((x) => x.versao)) === m.versao;
  }

  protected carregarExemplo(): void {
    this.executar(
      this.configuracao.carregarModeloExemploOdontologia(),
      'Ficha odontológica de exemplo criada como rascunho.',
    );
  }

  protected novoModelo(): void {
    this.problemas.set([]);
    this.edicao.set({
      id: null,
      areaAtuacao: this.novaArea,
      titulo: `Ficha de saúde — ${this.rotuloArea[this.novaArea]}`,
      perguntas: [],
    });
    this.adicionarPergunta();
  }

  protected editar(m: ModeloAnamnese): void {
    this.problemas.set([]);
    this.edicao.set({
      id: m.id,
      areaAtuacao: m.areaAtuacao,
      titulo: m.titulo,
      perguntas: m.perguntas.map((p) => ({ ...p, opcoesTexto: (p.opcoes ?? []).join('\n') })),
    });
  }

  protected novaVersao(m: ModeloAnamnese): void {
    this.executar(
      this.configuracao.duplicarModelo(m.id),
      `Versão ${m.versao + 1} criada como rascunho.`,
    );
  }

  protected publicar(m: ModeloAnamnese): void {
    const aviso =
      `Publicar a versão ${m.versao} da ficha de ${this.rotuloArea[m.areaAtuacao]}? ` +
      'Depois de publicada ela não pode ser alterada e passa a valer para os novos convites.';
    if (!window.confirm(aviso)) return;
    this.executar(this.configuracao.publicarModelo(m.id), 'Ficha publicada.');
  }

  // --- editor ---

  protected adicionarPergunta(): void {
    const e = this.edicao();
    if (!e) return;
    const ultima = e.perguntas.at(-1);
    e.perguntas = [
      ...e.perguntas,
      {
        codigo: '',
        secao: ultima?.secao ?? null,
        texto: '',
        tipo: 'SIM_NAO',
        obrigatoria: true,
        opcoes: null,
        alertaClinico: false,
        detalheObrigatorioQuandoSim: null,
        ajuda: null,
        opcoesTexto: '',
      },
    ];
    this.edicao.set({ ...e });
  }

  protected remover(i: number): void {
    const e = this.edicao();
    if (!e) return;
    e.perguntas = e.perguntas.filter((_, j) => j !== i);
    this.edicao.set({ ...e });
  }

  protected mover(i: number, direcao: -1 | 1): void {
    const e = this.edicao();
    const j = i + direcao;
    if (!e || j < 0 || j >= e.perguntas.length) return;
    const lista = [...e.perguntas];
    [lista[i], lista[j]] = [lista[j], lista[i]];
    e.perguntas = lista;
    this.edicao.set({ ...e });
  }

  protected mudouTipo(p: PerguntaEditavel, tipo: TipoPergunta): void {
    p.tipo = tipo;
    if (tipo !== 'SIM_NAO_DETALHE') {
      p.detalheObrigatorioQuandoSim = null;
    }
  }

  /** Converte o editor no formato da API, gerando código para perguntas novas. */
  protected perguntasParaSalvar(e: Edicao): PerguntaAnamnese[] {
    const usados = new Set(e.perguntas.map((p) => p.codigo).filter((c) => !!c));
    return e.perguntas.map((p) => {
      let codigo = p.codigo;
      if (!codigo) {
        codigo = codigoDaPergunta(p.texto, usados);
        usados.add(codigo);
      }
      const { opcoesTexto, ...pergunta } = p;
      return normalizarPergunta({ ...pergunta, codigo, opcoes: opcoesTexto.split('\n') });
    });
  }

  protected salvar(): void {
    const e = this.edicao();
    if (!e) return;
    const perguntas = this.perguntasParaSalvar(e);
    const problemas = problemasDoModelo(e.titulo, perguntas);
    this.problemas.set(problemas);
    if (problemas.length) return;
    const pedido = e.id
      ? this.configuracao.editarModelo(e.id, e.titulo.trim(), perguntas)
      : this.configuracao.criarModelo(e.areaAtuacao, e.titulo.trim(), perguntas);
    this.executar(pedido, 'Rascunho salvo.', () => this.edicao.set(null));
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
    this.configuracao.modelos().subscribe({
      next: (lista) => this.modelos.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
