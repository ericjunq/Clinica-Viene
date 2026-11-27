import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { Consultorio } from '../../core/models/agenda.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { AgendaService } from '../../core/services/agenda.service';

/** Consultórios da clínica (MASTER). Nunca são apagados: desativar tira da agenda. */
@Component({
  selector: 'app-consultorios',
  imports: [FormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <section class="cartao">
      <p class="nota">Cada consultório vira uma coluna na agenda do dia.</p>
      <form class="barra" (ngSubmit)="criar()">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Novo consultório</mat-label>
          <input matInput id="novo-consultorio" name="nome" [(ngModel)]="novoNome" maxlength="80" />
        </mat-form-field>
        <button mat-flat-button type="submit" [disabled]="ocupado() || !novoNome.trim()">
          Adicionar
        </button>
      </form>
      @if (erro(); as e) {
        <p class="alerta erro" role="alert">{{ e }}</p>
      }
      <ul class="lista">
        @for (c of consultorios(); track c.id) {
          <li>
            @if (editando() === c.id) {
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Nome</mat-label>
                <input matInput name="edicao" [(ngModel)]="nomeEdicao" maxlength="80" />
              </mat-form-field>
              <span class="acoes">
                <button mat-button (click)="editando.set(null)">Cancelar</button>
                <button mat-flat-button (click)="salvar(c, c.ativo)">Salvar</button>
              </span>
            } @else {
              <span>
                {{ c.nome }}
                <span class="etiqueta" [class.ativo]="c.ativo" [class.inativo]="!c.ativo">{{
                  c.ativo ? 'ativo' : 'desativado'
                }}</span>
              </span>
              <span class="acoes">
                <button mat-button (click)="editar(c)">Renomear</button>
                <button mat-button (click)="salvar(c, !c.ativo, c.nome)">
                  {{ c.ativo ? 'Desativar' : 'Ativar' }}
                </button>
              </span>
            }
          </li>
        } @empty {
          <li class="nota">Nenhum consultório cadastrado.</li>
        }
      </ul>
    </section>
  `,
  styleUrl: './configuracoes.scss',
})
export class ConsultoriosComponent implements OnInit {
  private readonly agenda = inject(AgendaService);
  private readonly avisos = inject(MatSnackBar);

  protected readonly consultorios = signal<Consultorio[]>([]);
  protected readonly erro = signal<string | null>(null);
  protected readonly ocupado = signal(false);
  protected readonly editando = signal<string | null>(null);
  protected novoNome = '';
  protected nomeEdicao = '';

  ngOnInit(): void {
    this.carregar();
  }

  protected criar(): void {
    this.executar(
      this.agenda.criarConsultorio(this.novoNome.trim()),
      'Consultório adicionado.',
      () => {
        this.novoNome = '';
      },
    );
  }

  protected editar(c: Consultorio): void {
    this.nomeEdicao = c.nome;
    this.editando.set(c.id);
  }

  protected salvar(c: Consultorio, ativo: boolean, nome = this.nomeEdicao): void {
    this.executar(
      this.agenda.atualizarConsultorio(c.id, nome.trim(), ativo),
      'Consultório atualizado.',
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
    this.agenda.consultorios().subscribe({
      next: (lista) => this.consultorios.set(lista),
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
  }
}
