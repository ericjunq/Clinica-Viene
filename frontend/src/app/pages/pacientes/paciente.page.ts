import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PARENTESCOS, TipoTermo } from '../../core/models/cadastro.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import {
  Consentimento,
  Convite,
  ConviteCriado,
  DocumentoPaciente,
  EntregaCriada,
  Paciente,
  PacienteVersao,
  ROTULO_AREA,
  ROTULO_ORIGEM,
  ROTULO_TERMO,
} from '../../core/models/paciente.model';
import { AreaAtuacao } from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { PacienteService } from '../../core/services/paciente.service';

const AREAS: AreaAtuacao[] = [
  'ODONTOLOGIA',
  'PSICOLOGIA',
  'MEDICINA',
  'NUTRICAO',
  'FISIOTERAPIA',
  'FONOAUDIOLOGIA',
  'ENFERMAGEM',
  'OUTRA',
];

/**
 * Ficha cadastral do paciente na recepção (2G): dados e contato (D-092), convite por link/QR ou quiosque (D-098),
 * documentos assinados (só metadados + link de entrega — D-088, D-094), privacidade (D-093) e histórico.
 */
@Component({
  selector: 'app-paciente-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTabsModule,
  ],
  templateUrl: './paciente.page.html',
  styleUrl: './paciente.page.scss',
})
export class PacientePage implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  private readonly auth = inject(AuthService);
  private readonly avisos = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';

  protected readonly areas = AREAS;
  protected readonly rotuloArea = ROTULO_AREA;
  protected readonly rotuloOrigem = ROTULO_ORIGEM;
  protected readonly rotuloTermo = ROTULO_TERMO;

  protected readonly paciente = signal<Paciente | null>(null);
  protected readonly erro = signal<string | null>(null);
  protected readonly convites = signal<Convite[]>([]);
  protected readonly documentos = signal<DocumentoPaciente[]>([]);
  protected readonly consentimentos = signal<Consentimento[]>([]);
  protected readonly historico = signal<PacienteVersao[]>([]);

  protected readonly temEmail = computed(() => {
    const p = this.paciente();
    return !!(p?.email || p?.responsavel?.email);
  });
  protected readonly parentesco = computed(() => {
    const valor = this.paciente()?.responsavel?.parentesco;
    return PARENTESCOS.find((p) => p.valor === valor)?.rotulo ?? '';
  });

  // --- contato ---
  protected readonly editandoContato = signal(false);
  protected readonly salvandoContato = signal(false);
  protected readonly erroContato = signal<string | null>(null);
  protected readonly contato = this.fb.group({
    email: ['', [Validators.email, Validators.maxLength(254)]],
    telefone: ['', Validators.maxLength(20)],
    cep: ['', Validators.maxLength(9)],
    logradouro: ['', Validators.maxLength(150)],
    numero: ['', Validators.maxLength(20)],
    complemento: ['', Validators.maxLength(80)],
    bairro: ['', Validators.maxLength(80)],
    cidade: ['', Validators.maxLength(80)],
    uf: ['', Validators.maxLength(2)],
  });

  // --- convite ---
  protected readonly convite = this.fb.nonNullable.group({
    areaAtuacao: ['ODONTOLOGIA' as AreaAtuacao],
    enviarEmail: [true],
    documentoConferido: [false],
  });
  protected readonly gerandoConvite = signal(false);
  protected readonly erroConvite = signal<string | null>(null);
  protected readonly conviteCriado = signal<ConviteCriado | null>(null);

  // --- documentos ---
  protected readonly entrega = signal<{ documentoId: string; criada: EntregaCriada } | null>(null);
  protected readonly gerandoEntrega = signal(false);
  protected readonly erroDocumentos = signal<string | null>(null);

  // --- privacidade ---
  protected readonly revogando = signal<TipoTermo | null>(null);
  protected readonly justificativa = this.fb.nonNullable.control('', Validators.maxLength(500));
  protected readonly erroPrivacidade = signal<string | null>(null);

  ngOnInit(): void {
    this.pacienteService.buscar(this.id).subscribe({
      next: (p) => {
        this.paciente.set(p);
        this.convite.controls.enviarEmail.setValue(!!(p.email || p.responsavel?.email));
      },
      error: (e: unknown) => this.erro.set(mensagemDeErro(e)),
    });
    this.carregarConvites();
    this.carregarDocumentos();
    this.carregarConsentimentos();
    this.carregarHistorico();
  }

  // --- contato ---

  protected editarContato(): void {
    const p = this.paciente();
    if (!p) return;
    const e = p.endereco;
    this.contato.reset({
      email: p.email ?? '',
      telefone: p.telefone ?? '',
      cep: e?.cep ?? '',
      logradouro: e?.logradouro ?? '',
      numero: e?.numero ?? '',
      complemento: e?.complemento ?? '',
      bairro: e?.bairro ?? '',
      cidade: e?.cidade ?? '',
      uf: e?.uf ?? '',
    });
    this.erroContato.set(null);
    this.editandoContato.set(true);
  }

  protected salvarContato(): void {
    if (this.contato.invalid || this.salvandoContato()) {
      this.contato.markAllAsTouched();
      return;
    }
    const v = this.contato.getRawValue();
    const t = (s: string | null) => (s ?? '').trim() || null;
    this.salvandoContato.set(true);
    this.pacienteService
      .atualizarContato(this.id, {
        email: t(v.email),
        telefone: t(v.telefone),
        endereco: {
          cep: t(v.cep),
          logradouro: t(v.logradouro),
          numero: t(v.numero),
          complemento: t(v.complemento),
          bairro: t(v.bairro),
          cidade: t(v.cidade),
          uf: t(v.uf)?.toUpperCase() ?? null,
        },
      })
      .subscribe({
        next: (p) => {
          this.paciente.set(p);
          this.salvandoContato.set(false);
          this.editandoContato.set(false);
          this.avisos.open('Contato atualizado.', 'OK', { duration: 3000 });
          this.carregarHistorico();
        },
        error: (e: unknown) => {
          this.salvandoContato.set(false);
          this.erroContato.set(mensagemDeErro(e));
        },
      });
  }

  // --- convite ---

  protected gerarLink(): void {
    this.criarConvite('LINK');
  }

  protected abrirQuiosque(): void {
    if (!this.convite.controls.documentoConferido.value) {
      this.erroConvite.set('Confira o documento com foto do paciente antes de abrir o quiosque.');
      return;
    }
    this.criarConvite('QUIOSQUE');
  }

  private criarConvite(modo: 'LINK' | 'QUIOSQUE'): void {
    if (this.gerandoConvite()) return;
    const v = this.convite.getRawValue();
    this.gerandoConvite.set(true);
    this.erroConvite.set(null);
    this.conviteCriado.set(null);
    this.pacienteService
      .criarConvite(this.id, {
        modo,
        areaAtuacao: v.areaAtuacao,
        enviarEmail: modo === 'LINK' && v.enviarEmail,
        documentoConferido: modo === 'QUIOSQUE' && v.documentoConferido,
      })
      .subscribe({
        next: (criado) => {
          this.gerandoConvite.set(false);
          this.conviteCriado.set(criado);
          this.convite.controls.documentoConferido.setValue(false);
          this.carregarConvites();
        },
        error: (e: unknown) => {
          this.gerandoConvite.set(false);
          this.erroConvite.set(mensagemDeErro(e));
        },
      });
  }

  /**
   * Abre o cadastro neste aparelho para o paciente preencher. Antes, encerra a sessão da recepção: o paciente não
   * pode ficar com acesso às telas internas.
   */
  protected abrirNesteAparelho(link: string): void {
    const ir = () => window.location.assign(link);
    this.auth.logout().subscribe({ complete: ir, error: ir });
  }

  protected copiar(texto: string): void {
    void navigator.clipboard
      ?.writeText(texto)
      .then(() => this.avisos.open('Link copiado.', 'OK', { duration: 2500 }));
  }

  protected cancelarConvite(c: Convite): void {
    this.pacienteService.cancelarConvite(this.id, c.id).subscribe({
      next: () => {
        if (this.conviteCriado()?.id === c.id) {
          this.conviteCriado.set(null);
        }
        this.carregarConvites();
      },
      error: (e: unknown) => this.erroConvite.set(mensagemDeErro(e)),
    });
  }

  // --- documentos ---

  protected gerarEntrega(documento: DocumentoPaciente, enviarEmail: boolean): void {
    if (this.gerandoEntrega()) return;
    this.gerandoEntrega.set(true);
    this.erroDocumentos.set(null);
    this.pacienteService.gerarEntrega(this.id, documento.id, enviarEmail).subscribe({
      next: (criada) => {
        this.gerandoEntrega.set(false);
        this.entrega.set({ documentoId: documento.id, criada });
        if (criada.emailEnviadoPara) {
          this.avisos.open(`Link enviado para ${criada.emailEnviadoPara}.`, 'OK', {
            duration: 4000,
          });
        }
      },
      error: (e: unknown) => {
        this.gerandoEntrega.set(false);
        this.erroDocumentos.set(mensagemDeErro(e));
      },
    });
  }

  // --- privacidade ---

  protected podeRevogar(c: Consentimento): boolean {
    return !c.obrigatorio && c.situacao === 'ACEITO';
  }

  protected iniciarRevogacao(tipo: TipoTermo): void {
    this.justificativa.reset('');
    this.erroPrivacidade.set(null);
    this.revogando.set(tipo);
  }

  protected confirmarRevogacao(): void {
    const tipo = this.revogando();
    if (!tipo) return;
    this.pacienteService
      .revogarConsentimento(this.id, tipo, this.justificativa.value.trim() || null)
      .subscribe({
        next: () => {
          this.revogando.set(null);
          this.avisos.open('Consentimento revogado.', 'OK', { duration: 3000 });
          this.carregarConsentimentos();
        },
        error: (e: unknown) => this.erroPrivacidade.set(mensagemDeErro(e)),
      });
  }

  protected rotuloSituacao(c: Consentimento): string {
    if (c.obrigatorio) {
      return c.situacao === 'ACEITO' ? 'Ciente' : 'Pendente';
    }
    switch (c.situacao) {
      case 'ACEITO':
        return 'Autorizado';
      case 'RECUSADO':
        return 'Não autorizado';
      case 'REVOGADO':
        return 'Revogado';
      default:
        return 'Ainda não respondido';
    }
  }

  // --- carregamentos ---

  private carregarConvites(): void {
    this.pacienteService.listarConvites(this.id).subscribe({
      next: (lista) => this.convites.set(lista),
      error: () => this.convites.set([]),
    });
  }

  private carregarDocumentos(): void {
    this.pacienteService.documentos(this.id).subscribe({
      next: (lista) => this.documentos.set(lista),
      error: () => this.documentos.set([]),
    });
  }

  private carregarConsentimentos(): void {
    this.pacienteService.consentimentos(this.id).subscribe({
      next: (lista) => this.consentimentos.set(lista),
      error: () => this.consentimentos.set([]),
    });
  }

  private carregarHistorico(): void {
    this.pacienteService.historico(this.id).subscribe({
      next: (lista) => this.historico.set(lista),
      error: () => this.historico.set([]),
    });
  }
}
