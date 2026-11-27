import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { ModelosAnamneseComponent } from './modelos-anamnese.component';
import { TermosComponent } from './termos.component';

/** Configurações do MASTER (D-089): termos LGPD e fichas de saúde. Rascunho edita; publicado não muda. */
@Component({
  selector: 'app-configuracoes-page',
  imports: [MatTabsModule, TermosComponent, ModelosAnamneseComponent],
  template: `
    <h1>Configurações</h1>
    <mat-tab-group animationDuration="0ms" mat-stretch-tabs="false">
      <mat-tab label="Termos e consentimentos"><app-termos /></mat-tab>
      <mat-tab label="Fichas de saúde"><app-modelos-anamnese /></mat-tab>
    </mat-tab-group>
  `,
})
export class ConfiguracoesPage {}
