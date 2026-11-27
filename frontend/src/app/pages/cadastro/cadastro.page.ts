import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatStepperModule } from '@angular/material/stepper';
import { ActivatedRoute } from '@angular/router';
import { Observable } from 'rxjs';
import {
  ConviteAberto,
  DadosCadastro,
  FichaCadastro,
  FormularioCadastro,
  PARENTESCOS,
  Parentesco,
  RespostaAnamnese,
  RevisaoCadastro,
} from '../../core/models/cadastro.model';
import { formatarEspera, mensagemDeErro } from '../../core/models/erro.model';
import { CadastroPublicoService } from '../../core/services/cadastro-publico.service';
import { CpfMaskDirective, formatarCpf } from '../../shared/cpf-mask/cpf-mask.directive';
import { LogoComponent } from '../../shared/logo/logo.component';
import {
  AnamneseFormComponent,
  pendentes,
  respostasPreenchidas,
  respostasVazias,
} from './anamnese-form.component';

type Etapa = 'carregando' | 'invalido' | 'codigo' | 'quiosque' | 'preenchendo' | 'revisado';

/** Extrai o token do convite: o link é /cadastro#token (o que vem depois do # não sai do navegador). */
export function tokenDoFragmento(fragmento: string | null): string | null {
  const token = fragmento?.trim();
  return token ? token : null;
}

/**
 * Cadastro do paciente pelo convite (CONTEXTO §4.2, D-091, D-100): confirma a identidade (código no e-mail
 * ou quiosque), confere os dados, responde a ficha de saúde e decide sobre os termos. Nada fica guardado no
 * navegador; a ficha só é gravada com a assinatura.
 */
@Component({
  selector: 'app-cadastro-page',
  imports: [
    FormsModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatStepperModule,
    CpfMaskDirective,
    LogoComponent,
    AnamneseFormComponent,
  ],
  templateUrl: './cadastro.page.html',
  styleUrls: ['../pagina-publica.scss', './cadastro.page.scss'],
})
export class CadastroPage implements OnInit, OnDestroy {
  private readonly servico = inject(CadastroPublicoService);
  private readonly rota = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder).nonNullable;

  protected readonly parentescos = PARENTESCOS;
  protected readonly etapa = signal<Etapa>('carregando');
  protected readonly erro = signal<string | null>(null);
  protected readonly enviando = signal(false);
  protected readonly convite = signal<ConviteAberto | null>(null);
  protected readonly codigoEnviadoPara = signal<string | null>(null);
  protected readonly avisoReenvio = signal<string | null>(null);
  protected readonly formulario = signal<FormularioCadastro | null>(null);
  protected readonly revisao = signal<RevisaoCadastro | null>(null);
  protected readonly mostrarPendentes = signal(false);
  protected respostas: Record<string, RespostaAnamnese> = {};
  protected decisoes: Record<string, boolean> = {};

  protected readonly codigo = this.fb.control('', [
    Validators.required,
    Validators.pattern(/^\d{6}$/),
  ]);

  protected readonly dados = this.fb.group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    cpf: [''],
    dataNascimento: ['', Validators.required],
    email: ['', [Validators.email, Validators.maxLength(254)]],
    telefone: ['', Validators.maxLength(20)],
    endereco: this.fb.group({
      cep: ['', [Validators.required, Validators.pattern(/^\d{5}-?\d{3}$/)]],
      logradouro: ['', [Validators.required, Validators.maxLength(150)]],
      numero: ['', [Validators.required, Validators.maxLength(20)]],
      complemento: ['', Validators.maxLength(80)],
      bairro: ['', [Validators.required, Validators.maxLength(80)]],
      cidade: ['', [Validators.required, Validators.maxLength(80)]],
      uf: ['', [Validators.required, Validators.pattern(/^[A-Za-z]{2}$/)]],
    }),
    responsavel: this.fb.group({
      nomeCompleto: ['', Validators.maxLength(150)],
      cpf: [''],
      parentesco: ['' as Parentesco | ''],
      telefone: ['', Validators.maxLength(20)],
      email: ['', [Validators.email, Validators.maxLength(254)]],
    }),
    comAcompanhante: [false],
    acompanhanteNome: ['', Validators.maxLength(150)],
    acompanhanteMotivo: ['', Validators.maxLength(300)],
  });

  protected readonly temResponsavel = computed(
    () => this.formulario()?.menorDeIdade === true || !!this.formulario()?.dados.responsavel,
  );

  ngOnInit(): void {
    const token = tokenDoFragmento(this.rota.snapshot.fragment);
    // tira o token da barra de endereço e do histórico
    history.replaceState(history.state, '', '/cadastro');
    if (!token) {
      this.invalido('Link de cadastro incompleto. Abra o link exatamente como recebeu.');
      return;
    }
    this.servico.usarConvite(token);
    this.servico.abrir().subscribe({
      next: (convite) => {
        this.convite.set(convite);
        this.etapa.set(convite.precisaCodigo ? 'codigo' : 'quiosque');
      },
      error: (e: unknown) => this.invalido(mensagemDeErro(e)),
    });
  }

  ngOnDestroy(): void {
    this.servico.encerrar();
  }

  protected enviarCodigo(): void {
    this.executar(this.servico.enviarCodigo(), (enviado) => {
      this.codigoEnviadoPara.set(enviado.emailMascarado);
      const espera = Math.max(0, (Date.parse(enviado.podeReenviarEm) - Date.now()) / 1000);
      this.erro.set(null);
      this.avisoReenvio.set(
        `Se não chegar, você pode pedir outro em ${formatarEspera(Math.ceil(espera))}.`,
      );
    });
  }

  protected confirmarCodigo(): void {
    if (this.codigo.invalid) {
      this.codigo.markAsTouched();
      return;
    }
    this.executar(this.servico.confirmarCodigo(this.codigo.value), () => this.carregarFormulario());
  }

  protected comecarNoQuiosque(): void {
    this.executar(this.servico.iniciarQuiosque(), () => this.carregarFormulario());
  }

  /** Confere a ficha inteira no servidor; tudo certo, segue para a assinatura. */
  protected revisar(): void {
    const f = this.formulario();
    if (!f) {
      return;
    }
    if (this.dados.invalid) {
      this.dados.markAllAsTouched();
      this.erro.set('Confira os campos destacados em "Seus dados".');
      return;
    }
    if (pendentes(f.anamnese.perguntas, this.respostas).length) {
      this.mostrarPendentes.set(true);
      this.erro.set('Ainda há perguntas da ficha de saúde sem resposta.');
      return;
    }
    if (!this.politicaAceita()) {
      this.erro.set('Para continuar, marque que leu a política de privacidade.');
      return;
    }
    this.executar(this.servico.revisar(this.ficha(f)), (revisao) => {
      this.revisao.set(revisao);
      this.etapa.set('revisado');
    });
  }

  protected politicaAceita(): boolean {
    return (this.formulario()?.termos ?? [])
      .filter((t) => t.obrigatorio)
      .every((t) => this.decisoes[t.id]);
  }

  protected campoInvalido(caminho: string): boolean {
    const controle = this.dados.get(caminho);
    return !!controle && controle.invalid && controle.touched;
  }

  private carregarFormulario(): void {
    this.enviando.set(true);
    this.servico.formulario().subscribe({
      next: (f) => {
        this.preencher(f);
        this.formulario.set(f);
        this.etapa.set('preenchendo');
        this.enviando.set(false);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }

  private preencher(f: FormularioCadastro): void {
    const d = f.dados;
    this.dados.reset({
      nomeCompleto: d.nomeCompleto,
      cpf: d.cpf ? formatarCpf(d.cpf) : '',
      dataNascimento: d.dataNascimento,
      email: d.email ?? '',
      telefone: d.telefone ?? '',
      endereco: {
        cep: d.endereco?.cep ?? '',
        logradouro: d.endereco?.logradouro ?? '',
        numero: d.endereco?.numero ?? '',
        complemento: d.endereco?.complemento ?? '',
        bairro: d.endereco?.bairro ?? '',
        cidade: d.endereco?.cidade ?? '',
        uf: d.endereco?.uf ?? '',
      },
      responsavel: {
        nomeCompleto: d.responsavel?.nomeCompleto ?? '',
        cpf: d.responsavel?.cpf ? formatarCpf(d.responsavel.cpf) : '',
        parentesco: d.responsavel?.parentesco ?? '',
        telefone: d.responsavel?.telefone ?? '',
        email: d.responsavel?.email ?? '',
      },
      comAcompanhante: false,
      acompanhanteNome: '',
      acompanhanteMotivo: '',
    });
    const responsavel = this.dados.controls.responsavel.controls;
    if (f.menorDeIdade || d.responsavel) {
      responsavel.nomeCompleto.addValidators(Validators.required);
      responsavel.cpf.addValidators(Validators.required);
      responsavel.parentesco.addValidators(Validators.required);
    }
    this.respostas = respostasVazias(f.anamnese.perguntas);
    // consentimentos opcionais vêm desmarcados (D-093)
    this.decisoes = Object.fromEntries(f.termos.map((t) => [t.id, false]));
  }

  private ficha(f: FormularioCadastro): FichaCadastro {
    const v = this.dados.getRawValue();
    const vazio = (s: string) => (s.trim() ? s.trim() : null);
    const r = v.responsavel;
    const dados: DadosCadastro = {
      nomeCompleto: v.nomeCompleto.trim(),
      cpf: vazio(v.cpf),
      dataNascimento: v.dataNascimento,
      email: vazio(v.email),
      telefone: vazio(v.telefone),
      endereco: {
        cep: vazio(v.endereco.cep),
        logradouro: vazio(v.endereco.logradouro),
        numero: vazio(v.endereco.numero),
        complemento: vazio(v.endereco.complemento),
        bairro: vazio(v.endereco.bairro),
        cidade: vazio(v.endereco.cidade),
        uf: vazio(v.endereco.uf)?.toUpperCase() ?? null,
      },
      responsavel:
        this.temResponsavel() && r.parentesco
          ? {
              nomeCompleto: r.nomeCompleto.trim(),
              cpf: r.cpf,
              parentesco: r.parentesco,
              telefone: vazio(r.telefone),
              email: vazio(r.email),
            }
          : null,
    };
    return {
      dados,
      modeloAnamneseId: f.anamnese.id,
      respostas: respostasPreenchidas(this.respostas),
      termos: f.termos.map((t) => ({ termoVersaoId: t.id, aceito: !!this.decisoes[t.id] })),
      acompanhante:
        v.comAcompanhante && v.acompanhanteNome.trim()
          ? { nome: v.acompanhanteNome.trim(), motivo: vazio(v.acompanhanteMotivo) }
          : null,
    };
  }

  private executar<T>(chamada: Observable<T>, sucesso: (resultado: T) => void): void {
    if (this.enviando()) {
      return;
    }
    this.enviando.set(true);
    this.erro.set(null);
    chamada.subscribe({
      next: (resultado) => {
        this.enviando.set(false);
        sucesso(resultado);
      },
      error: (e: unknown) => {
        this.erro.set(mensagemDeErro(e));
        this.enviando.set(false);
      },
    });
  }

  private invalido(mensagem: string): void {
    this.erro.set(mensagem);
    this.etapa.set('invalido');
  }
}
