import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { CategoriasComponent } from './categorias.component';
import { ConsultoriosComponent } from './consultorios.component';
import { ModelosAnamneseComponent } from './modelos-anamnese.component';
import { TermosComponent } from './termos.component';

/** Configurações do MASTER: termos LGPD e fichas de saúde (D-089), consultórios e categorias de procedimento (D-106). */
@Component({
  selector: 'app-configuracoes-page',
  imports: [
    MatTabsModule,
    TermosComponent,
    ModelosAnamneseComponent,
    ConsultoriosComponent,
    CategoriasComponent,
  ],
  template: `
    <h1>Configurações</h1>
    <mat-tab-group animationDuration="0ms" mat-stretch-tabs="false">
      <mat-tab label="Termos e consentimentos"><app-termos /></mat-tab>
      <mat-tab label="Fichas de saúde"><app-modelos-anamnese /></mat-tab>
      <mat-tab label="Consultórios"><app-consultorios /></mat-tab>
      <mat-tab label="Categorias de procedimento"><app-categorias /></mat-tab>
    </mat-tab-group>
  `,
})
export class ConfiguracoesPage {}
