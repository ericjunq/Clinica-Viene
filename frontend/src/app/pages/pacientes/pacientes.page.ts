import { DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, TemplateRef, computed, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { PARENTESCOS, Parentesco } from '../../core/models/cadastro.model';
import { mensagemDeErro } from '../../core/models/erro.model';
import { NovoPaciente, PacienteResumo, idadeEm } from '../../core/models/paciente.model';
import { AuthService } from '../../core/services/auth.service';
import { PacienteService } from '../../core/services/paciente.service';
import { CpfMaskDirective } from '../../shared/cpf-mask/cpf-mask.directive';

/** Lista/busca de pacientes e pré-cadastro pela recepção (D-091). O paciente completa e assina pelo convite. */
@Component({
  selector: 'app-pacientes-page',
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    CpfMaskDirective,
  ],
  templateUrl: './pacientes.page.html',
  styleUrl: './pacientes.page.scss',
})
export class PacientesPage {
  private readonly pacienteService = inject(PacienteService);
  private readonly router = inject(Router);
  private readonly dialogo = inject(MatDialog);
  private readonly formularioTpl = viewChild.required<TemplateRef<unknown>>('formulario');
  private dialogoAberto: MatDialogRef<unknown> | null = null;
  private readonly fb = inject(FormBuilder);

  private readonly usuario = inject(AuthService).usuario;
  protected readonly ehMaster = computed(() => this.usuario()?.perfil === 'MASTER');
  protected readonly colunas = ['nome', 'cpf', 'nascimento', 'telefone'];
  protected readonly parentescos = PARENTESCOS;
  protected readonly pacientes = signal<PacienteResumo[]>([]);
  protected readonly buscou = signal(false);
  protected readonly carregando = signal(false);
  protected readonly erroBusca = signal<string | null>(null);
  protected readonly salvando = signal(false);
  protected readonly erroFormulario = signal<string | null>(null);
  /** Alerta de possível duplicidade (409): pede confirmação antes de criar. */
  protected readonly duplicidade = signal(false);

  protected readonly busca = this.fb.nonNullable.control('');

  protected readonly form = this.fb.group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    semCpf: [false],
    cpf: [''],
    dataNascimento: ['', Validators.required],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    telefone: ['', Validators.maxLength(20)],
    semCpfJustificativa: ['', Validators.maxLength(500)],
    responsavelNome: ['', Validators.maxLength(150)],
    responsavelCpf: [''],
    responsavelParentesco: [null as Parentesco | null],
    responsavelTelefone: ['', Validators.maxLength(20)],
    responsavelEmail: ['', [Validators.email, Validators.maxLength(254)]],
  });

  private readonly valores = toSignal(this.form.valueChanges, { initialValue: this.form.value });

  /** Menor de idade precisa de responsável (LGPD art. 14). */
  protected readonly menor = computed(() => {
    const nascimento = this.valores().dataNascimento;
    return !!nascimento && idadeEm(nascimento) < 18;
  });
  protected readonly adultoSemCpf = computed(() => !!this.valores().semCpf && !this.menor());
  /** Menor pode não ter CPF (o responsável tem); adulto sem CPF só com aprovação do MASTER (D-053). */
  protected readonly bloqueadoSemCpf = computed(() => this.adultoSemCpf() && !this.ehMaster());

  protected pesquisar(): void {
    const texto = this.busca.value.trim();
    if (texto.length < 2) {
      this.erroBusca.set('Digite ao menos 2 caracteres do nome, o CPF ou a data de nascimento.');
      return;
    }
    this.erroBusca.set(null);
    this.carregando.set(true);
    this.pacienteService.pesquisar(texto).subscribe({
      next: (lista) => {
        this.pacientes.set(lista);
        this.buscou.set(true);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.erroBusca.set(mensagemDeErro(e));
        this.carregando.set(false);
      },
    });
  }

  protected abrir(paciente: PacienteResumo): void {
    void this.router.navigate(['/pacientes', paciente.id]);
  }

  protected abrirCadastro(): void {
    this.form.reset({ semCpf: false });
    this.erroFormulario.set(null);
    this.duplicidade.set(false);
    this.dialogoAberto = this.dialogo.open(this.formularioTpl(), {
      width: '40rem',
      maxWidth: '95vw',
    });
  }

  protected fecharCadastro(): void {
    this.dialogoAberto?.close();
  }

  protected salvar(confirmarDuplicidade = false): void {
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const pedido = this.montarPedido(confirmarDuplicidade);
    if (typeof pedido === 'string') {
      this.erroFormulario.set(pedido);
      return;
    }
    this.salvando.set(true);
    this.erroFormulario.set(null);
    this.pacienteService.preCadastrar(pedido).subscribe({
      next: (criado) => {
        this.salvando.set(false);
        this.dialogoAberto?.close();
        void this.router.navigate(['/pacientes', criado.id]);
      },
      error: (e: unknown) => {
        this.salvando.set(false);
        if (e instanceof HttpErrorResponse && e.status === 409) {
          this.duplicidade.set(true);
          return;
        }
        this.erroFormulario.set(mensagemDeErro(e));
      },
    });
  }

  /** Monta o pedido ou devolve a mensagem do que falta (o backend confere tudo de novo). */
  protected montarPedido(confirmarDuplicidade: boolean): NovoPaciente | string {
    const v = this.form.getRawValue();
    const texto = (s: string | null | undefined) => (s ?? '').trim() || null;
    const semCpf = !!v.semCpf;
    const cpf = semCpf ? null : texto(v.cpf);
    if (!semCpf && !cpf) {
      return 'Informe o CPF ou marque "Paciente sem CPF".';
    }
    if (!texto(v.email) && !texto(v.telefone)) {
      return 'Informe ao menos um contato: e-mail ou telefone.';
    }
    const menor = !!v.dataNascimento && idadeEm(v.dataNascimento) < 18;
    if (semCpf && !menor && !this.ehMaster()) {
      return 'Paciente adulto sem CPF só pode ser cadastrado pelo MASTER.';
    }
    if (semCpf && !menor && !texto(v.semCpfJustificativa)) {
      return 'Informe a justificativa para cadastrar adulto sem CPF.';
    }
    let responsavel: NovoPaciente['responsavel'] = null;
    if (menor) {
      if (!texto(v.responsavelNome) || !texto(v.responsavelCpf) || !v.responsavelParentesco) {
        return 'Paciente menor de idade: informe nome, CPF e parentesco do responsável.';
      }
      responsavel = {
        nomeCompleto: texto(v.responsavelNome)!,
        cpf: texto(v.responsavelCpf)!,
        parentesco: v.responsavelParentesco,
        telefone: texto(v.responsavelTelefone),
        email: texto(v.responsavelEmail),
      };
    }
    return {
      nomeCompleto: v.nomeCompleto!.trim(),
      cpf,
      dataNascimento: v.dataNascimento!,
      email: texto(v.email),
      telefone: texto(v.telefone),
      endereco: null,
      responsavel,
      semCpfJustificativa: semCpf && !menor ? texto(v.semCpfJustificativa) : null,
      confirmarDuplicidade,
    };
  }
}
