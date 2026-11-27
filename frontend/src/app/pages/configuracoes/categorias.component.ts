import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { CategoriaProcedimento } from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { ROTULO_AREA } from '../../core/models/paciente.model';
import { AreaAtuacao } from '../../core/models/usuario.model';
import { AgendaService } from '../../core/services/agenda.service';

/**
 * Categorias de procedimento por área (D-106). O código não muda (o financeiro usa); o nome pode mudar. Desativar
 * tira das opções de novos agendamentos.
 */
@Component({
  selector: 'app-categorias',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `
    <section class="cartao">
      <p class="nota">
        Cada item de um agendamento tem uma categoria da área do profissional, mais uma descrição
        (ex.: "Ortodontia" — "manutenção do aparelho").
      </p>
      <div class="barra">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Área</mat-label>
          <mat-select [(ngModel)]="area" (ngModelChange)="areaSelecionada.set($event)">
            @for (a of areas; track a) {
              <mat-option [value]="a">{{ rotuloArea[a] }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        @if (area === 'ODONTOLOGIA' && !daArea().length) {
          <button mat-stroked-button (click)="carregarExemplo()" [disabled]="ocupado()">
            Carregar categorias de exemplo
          </button>
        }
      </div>
      <form class="barra" (ngSubmit)="criar()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Nova categoria</mat-label>
          <input matInput id="nova-categoria" name="nome" [(ngModel)]="novoNome" maxlength="80" />
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="ocupado() || !novoNome.trim()">
          Adicionar
        </button>
      </form>
      @if (erro(); as e) {
        <p class="alerta erro" role="alert">{{ e }}</p>
      }
      <ul class="lista">
        @for (c of daArea(); track c.id) {
          <li>
            @if (editando() === c.id) {
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Nome</mat-label>
                <input matInput name="edicao" [(ngModel)]="nomeEdicao" maxlength="80" />
              </mat-form-field>
              <span class="acoes">
                <button mat-button (click)="editando.set(null)">Cancelar</button>
                <button mat-flat-button (click)="salvar(c, c.ativa)">Salvar</button>
              </span>
            } @else {
              <span>
                {{ c.nome }} <span class="nota">{{ c.codigo }}</span>
                <span class="etiqueta" [class.ativo]="c.ativa" [class.inativo]="!c.ativa">{{
                  c.ativa ? 'ativa' : 'desativada'
                }}</span>
              </span>
              <span class="acoes">
                <button mat-button (click)="editar(c)">Renomear</button>
                <button mat-button (click)="salvar(c, !c.ativa, c.nome)">
                  {{ c.ativa ? 'Desativar' : 'Ativar' }}
                </button>
              </span>
            }
          </li>
        } @empty {
          <li class="nota">Nenhuma categoria nesta área.</li>
        }
      </ul>
    </section>
  `,
  styleUrl: './configuracoes.scss',
})
export class CategoriasComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly rotuloArea = ROTULO_AREA;
  protected readonly areas = Object.keys(ROTULO_AREA) as AreaAtuacao[];
  protected readonly categorias = signal<CategoriaProcedimento[]>([]);
  protected readonly areaSelecionada = signal<AreaAtuacao>('ODONTOLOGIA');
  protected readonly daArea = computed(() =>
    this.categorias().filter((c) => c.areaAtuacao === this.areaSelecionada()),
  );
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly editando = signal<string | null>(null);
  protected area: AreaAtuacao = 'ODONTOLOGIA';
  protected novoNome = '';
  protected nomeEdicao = '';

  ngOnInit(): void {
    this.carregar();
  }

  protected carregarExemplo(): void {
    this.executar(
      this.agenda.carregarCategoriasOdontologia(),
      'Categorias de exemplo carregadas.',
      () => undefined,
    );
  }

  protected criar(): void {
    this.executar(
      this.agenda.criarCategoria(this.area, this.novoNome.trim(), null),
      'Categoria adicionada.',
      () => {
        this.novoNome = '';
      },
    );
  }

  protected editar(c: CategoriaProcedimento): void {
    this.nomeEdicao = c.nome;
    this.editando.set(c.id);
  }

  protected salvar(c: CategoriaProcedimento, ativa: boolean, nome = this.nomeEdicao): void {
    this.executar(
      this.agenda.atualizarCategoria(c.id, nome.trim(), ativa),
      'Categoria atualizada.',
      () => this.editando.set(null),
    );
  }

  private executar(pedido: Observable<unknown>, sucesso: string, depois: () => void): void {
    this.ocupado.set(true);
    this.erro.set(null);
    pedido.subscribe({
      next: () => {
        this.ocupado.set(false);
        this.avisos.open(sucesso, 'OK', { duration: 3000 });
        depois();
        this.carregar();
      },
      error: (e: unknown) => {
        this.ocupado.set(false);
        this.erro.set(mensagemDeErro(e));
      },
    });
  }

  private carregar(): void {
    this.agenda.categorias().subscribe({
      next: (lista) => this.categorias.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
