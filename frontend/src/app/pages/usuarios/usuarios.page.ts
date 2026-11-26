import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { mensagemDeErro } from '../../core/models/erro.model';
import {
  AreaAtuacao,
  ConselhoProfissional,
  NovoUsuario,
  Perfil,
  Usuario,
} from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { senhaForteValidator } from '../../core/validators/senha.validator';

const UFS = [
  'AC',
  'AL',
  'AP',
  'AM',
  'BA',
  'CE',
  'DF',
  'ES',
  'GO',
  'MA',
  'MT',
  'MS',
  'MG',
  'PA',
  'PB',
  'PR',
  'PE',
  'PI',
  'RJ',
  'RN',
  'RS',
  'RO',
  'RR',
  'SC',
  'SP',
  'SE',
  'TO',
];

/** Gestão de usuários (DEV e MASTER). As regras de quem pode o quê são validadas no backend. */
@Component({
  selector: 'app-usuarios-page',
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    DialogModule,
    InputMaskModule,
    InputTextModule,
    MessageModule,
    PasswordModule,
    SelectModule,
    TableModule,
    TagModule,
  ],
  templateUrl: './usuarios.page.html',
})
export class UsuariosPage implements OnInit {
  private readonly usuarioService = inject(UsuarioService);
  private readonly mensagens = inject(MessageService);
  private readonly eu = inject(AuthService).usuario;

  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly carregando = signal(false);
  protected readonly dialogoAberto = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erroFormulario = signal<string | null>(null);

  protected readonly perfisDisponiveis = computed<{ label: string; value: Perfil }[]>(() => [
    { label: 'Dono(a) da clínica (MASTER)', value: 'MASTER' },
    { label: 'Recepção (ADMIN)', value: 'ADMIN' },
    { label: 'Profissional de saúde', value: 'PROFISSIONAL' },
  ]);
  protected readonly areas: AreaAtuacao[] = [
    'ODONTOLOGIA',
    'PSICOLOGIA',
    'MEDICINA',
    'NUTRICAO',
    'FISIOTERAPIA',
    'FONOAUDIOLOGIA',
    'ENFERMAGEM',
    'OUTRA',
  ];
  protected readonly conselhos: ConselhoProfissional[] = [
    'CRO',
    'CRP',
    'CRM',
    'CRN',
    'CREFITO',
    'CRFA',
    'COREN',
    'OUTRO',
  ];
  protected readonly ufs = UFS;

  protected readonly form = inject(FormBuilder).group({
    nomeCompleto: ['', [Validators.required, Validators.maxLength(150)]],
    cpf: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    telefone: [''],
    perfil: ['ADMIN' as Perfil, Validators.required],
    areaAtuacao: [null as AreaAtuacao | null],
    conselho: [null as ConselhoProfissional | null],
    numeroRegistro: [''],
    ufRegistro: [null as string | null],
    senhaProvisoria: ['', [Validators.required, senhaForteValidator]],
  });

  ngOnInit(): void {
    this.carregar();
  }

  protected ehProfissional(): boolean {
    return this.form.controls.perfil.value === 'PROFISSIONAL';
  }

  /** DEV não desativa MASTER; ninguém altera o próprio status (D-067, D-068). */
  protected podeAlterarStatus(usuario: Usuario): boolean {
    const eu = this.eu();
    if (!eu || eu.id === usuario.id || usuario.perfil === 'DEV') {
      return false;
    }
    return !(eu.perfil === 'DEV' && usuario.perfil === 'MASTER');
  }

  protected abrirCadastro(): void {
    this.form.reset({ perfil: 'ADMIN' });
    this.erroFormulario.set(null);
    this.dialogoAberto.set(true);
  }

  protected salvar(): void {
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const valor = this.form.getRawValue();
    const profissional = valor.perfil === 'PROFISSIONAL';
    const novo: NovoUsuario = {
      nomeCompleto: valor.nomeCompleto ?? '',
      cpf: valor.cpf ?? '',
      email: valor.email ?? '',
      telefone: valor.telefone || null,
      perfil: valor.perfil ?? 'ADMIN',
      areaAtuacao: profissional ? valor.areaAtuacao : null,
      conselho: profissional ? valor.conselho : null,
      numeroRegistro: profissional ? valor.numeroRegistro || null : null,
      ufRegistro: profissional ? valor.ufRegistro : null,
      senhaProvisoria: valor.senhaProvisoria ?? '',
    };
    this.salvando.set(true);
    this.erroFormulario.set(null);
    this.usuarioService.criar(novo).subscribe({
      next: (criado) => {
        this.salvando.set(false);
        this.dialogoAberto.set(false);
        this.mensagens.add({
          severity: 'success',
          summary: 'Usuário criado',
          detail: `${criado.nomeCompleto}: informe a senha provisória; a troca é obrigatória no primeiro acesso.`,
        });
        this.carregar();
      },
      error: (e: unknown) => {
        this.erroFormulario.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }

  protected alternarStatus(usuario: Usuario): void {
    const novoStatus = usuario.status === 'ATIVO' ? 'INATIVO' : 'ATIVO';
    this.usuarioService.alterarStatus(usuario.id, novoStatus).subscribe({
      next: () => {
        this.mensagens.add({
          severity: 'success',
          summary: novoStatus === 'ATIVO' ? 'Usuário ativado' : 'Usuário desativado',
          detail: usuario.nomeCompleto,
        });
        this.carregar();
      },
      error: (e: unknown) =>
        this.mensagens.add({
          severity: 'error',
          summary: 'Não foi possível',
          detail: mensagemDeErro(e),
        }),
    });
  }

  private carregar(): void {
    this.carregando.set(true);
    this.usuarioService.listar().subscribe({
      next: (lista) => {
        this.usuarios.set(lista);
        this.carregando.set(false);
      },
      error: (e: unknown) => {
        this.carregando.set(false);
        this.mensagens.add({
          severity: 'error',
          summary: 'Erro ao carregar',
          detail: mensagemDeErro(e),
        });
      },
    });
  }
}
