import { Component, computed, input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { PerguntaAnamnese, RespostaAnamnese } from '../../core/models/cadastro.model';

/** Resposta vazia para cada pergunta do modelo (nada vem marcado: o paciente responde tudo). */
export function respostasVazias(perguntas: PerguntaAnamnese[]): Record<string, RespostaAnamnese> {
  return Object.fromEntries(
    perguntas.map((p) => [
      p.codigo,
      { valor: null, valores: p.tipo === 'MULTIPLA_ESCOLHA' ? [] : null, detalhe: null },
    ]),
  );
}

/** Perguntas obrigatórias ainda sem resposta (ou Sim sem o "qual?"), para avisar antes de enviar. */
export function pendentes(
  perguntas: PerguntaAnamnese[],
  respostas: Record<string, RespostaAnamnese>,
): PerguntaAnamnese[] {
  return perguntas.filter((p) => {
    const r = respostas[p.codigo];
    const vazia = !r || (!r.valor?.trim() && !(r.valores && r.valores.length));
    if (vazia) {
      return !!p.obrigatoria;
    }
    return p.detalheObrigatorioQuandoSim === true && r.valor === 'SIM' && !r.detalhe?.trim();
  });
}

/** Só envia o que foi respondido (o backend confere tudo de novo contra o modelo). */
export function respostasPreenchidas(
  respostas: Record<string, RespostaAnamnese>,
): Record<string, RespostaAnamnese> {
  return Object.fromEntries(
    Object.entries(respostas).filter(
      ([, r]) => !!r.valor?.trim() || !!(r.valores && r.valores.length),
    ),
  );
}

/** Ficha de saúde montada a partir do modelo vigente (D-089), agrupada por seção. */
@Component({
  selector: 'app-anamnese-form',
  imports: [FormsModule, MatCheckboxModule, MatFormFieldModule, MatInputModule, MatRadioModule],
  templateUrl: './anamnese-form.component.html',
  styleUrl: './cadastro.page.scss',
})
export class AnamneseFormComponent {
  readonly perguntas = input.required<PerguntaAnamnese[]>();
  readonly respostas = input.required<Record<string, RespostaAnamnese>>();
  readonly mostrarPendentes = input(false);

  protected readonly secoes = computed(() => {
    const grupos: { nome: string; perguntas: PerguntaAnamnese[] }[] = [];
    for (const p of this.perguntas()) {
      const nome = p.secao ?? '';
      const grupo = grupos.find((g) => g.nome === nome);
      if (grupo) {
        grupo.perguntas.push(p);
      } else {
        grupos.push({ nome, perguntas: [p] });
      }
    }
    return grupos;
  });

  protected pendente(p: PerguntaAnamnese): boolean {
    return this.mostrarPendentes() && pendentes([p], this.respostas()).length > 0;
  }

  protected marcada(p: PerguntaAnamnese, opcao: string): boolean {
    return this.respostas()[p.codigo].valores?.includes(opcao) ?? false;
  }

  protected alternar(p: PerguntaAnamnese, opcao: string, marcar: boolean): void {
    const r = this.respostas()[p.codigo];
    const atuais = r.valores ?? [];
    r.valores = marcar ? [...atuais, opcao] : atuais.filter((v) => v !== opcao);
  }
}
