import { Component, OnInit, TemplateRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { Observable, concatMap, of } from 'rxjs';
import { mensagemDeErro } from '../../core/models/erro.model';
import {
  AreaAtuacao,
  ConselhoProfissional,
  NovoUsuario,
  Perfil,
  RegistroProfissional,
  Usuario,
} from '../../core/models/usuario.model';
import { AuthService } from '../../core/services/auth.service';
import { UsuarioService } from '../../core/services/usuario.service';
import { senhaForteValidator } from '../../core/validators/senha.validator';
import { CpfMaskDirective } from '../../shared/cpf-mask/cpf-mask.directive';

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
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    CpfMaskDirective,
  ],
  templateUrl: './usuarios.page.html',
  styleUrl: './usuarios.page.scss',
})
export class UsuariosPage implements OnInit {
  private readonly usuarioService = inject(UsuarioService);
  private readonly avisos = inject(MatSnackBar);
  private readonly dialogo = inject(MatDialog);
  private readonly eu = inject(AuthService).usuario;
  private readonly formularioTpl = viewChild.required<TemplateRef<unknown>>('formulario');
  private dialogoAberto: MatDialogRef<unknown> | null = null;

  protected readonly colunas = ['nome', 'cpf', 'email', 'perfil', 'registro', 'status', 'acoes'];
  protected readonly usuarios = signal<Usuario[]>([]);
  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly erroFormulario = signal<string | null>(null);
  /** Usuário em edição (null = cadastro de novo usuário). */
  protected readonly emEdicao = signal<Usuario | null>(null);

  protected readonly perfis: { rotulo: string; valor: Perfil }[] = [
    { rotulo: 'Dono(a) da clínica (MASTER)', valor: 'MASTER' },
    { rotulo: 'Recepção (ADMIN)', valor: 'ADMIN' },
    { rotulo: 'Profissional de saúde', valor: 'PROFISSIONAL' },
  ];
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

  /** Pode editar: quem pode alterar o status, ou a própria pessoa (D-086). */
  protected podeEditar(usuario: Usuario): boolean {
    return this.eu()?.id === usuario.id || this.podeAlterarStatus(usuario);
  }

  protected ehEuMesmo(): boolean {
    return this.emEdicao()?.id === this.eu()?.id;
  }

  protected abrirCadastro(): void {
    this.emEdicao.set(null);
    this.form.reset({ perfil: 'ADMIN' });
    this.form.controls.cpf.enable();
    this.form.controls.senhaProvisoria.enable();
    this.form.controls.perfil.enable();
    this.abrirDialogo();
  }

  protected abrirEdicao(usuario: Usuario): void {
    this.emEdicao.set(usuario);
    this.form.reset({
      nomeCompleto: usuario.nomeCompleto,
      cpf: usuario.cpfMascarado ?? '',
      email: usuario.email,
      telefone: usuario.telefone ?? '',
      perfil: usuario.perfil,
      areaAtuacao: usuario.areaAtuacao,
      conselho: usuario.conselho,
      numeroRegistro: usuario.numeroRegistro ?? '',
      ufRegistro: usuario.ufRegistro,
      senhaProvisoria: '',
    });
    // CPF não muda; senha não se define aqui; ninguém muda o próprio perfil (D-086)
    this.form.controls.cpf.disable();
    this.form.controls.senhaProvisoria.disable();
    if (this.ehEuMesmo() || usuario.perfil === 'DEV') {
      this.form.controls.perfil.disable();
    } else {
      this.form.controls.perfil.enable();
    }
    this.abrirDialogo();
  }

  protected salvar(): void {
    if (this.form.invalid || this.salvando()) {
      this.form.markAllAsTouched();
      return;
    }
    const emEdicao = this.emEdicao();
    if (emEdicao) {
      this.salvarEdicao(emEdicao);
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
        this.dialogoAberto?.close();
        this.avisar(
          `${criado.nomeCompleto} criado(a). Informe a senha provisória: a troca é obrigatória no primeiro acesso.`,
        );
        this.carregar();
      },
      error: (e: unknown) => {
        this.erroFormulario.set(mensagemDeErro(e));
        this.salvando.set(false);
      },
    });
  }

  private salvarEdicao(usuario: Usuario): void {
    const valor = this.form.getRawValue();
    const novoPerfil = valor.perfil ?? usuario.perfil;
    const registro: RegistroProfissional =
      novoPerfil === 'PROFISSIONAL'
        ? {
            areaAtuacao: valor.areaAtuacao,
            conselho: valor.conselho,
            numeroRegistro: valor.numeroRegistro || null,
            ufRegistro: valor.ufRegistro,
          }
        : { areaAtuacao: null, conselho: null, numeroRegistro: null, ufRegistro: null };
    const mudarPerfil: Observable<unknown> =
      novoPerfil !== usuario.perfil
        ? this.usuarioService.alterarPerfil(usuario.id, novoPerfil, registro)
        : of(null);
    this.salvando.set(true);
    this.erroFormulario.set(null);
    mudarPerfil
      .pipe(
        concatMap(() =>
          this.usuarioService.atualizar(usuario.id, {
            nomeCompleto: valor.nomeCompleto ?? '',
            email: valor.email ?? '',
            telefone: valor.telefone || null,
            ...registro,
          }),
        ),
      )
      .subscribe({
        next: (salvo) => {
          this.salvando.set(false);
          this.dialogoAberto?.close();
          this.avisar(
            novoPerfil !== usuario.perfil
              ? `${salvo.nomeCompleto}: dados salvos. Perfil alterado — a pessoa precisa entrar de novo.`
              : `${salvo.nomeCompleto}: dados salvos.`,
          );
          this.carregar();
        },
        error: (e: unknown) => {
          this.erroFormulario.set(mensagemDeErro(e));
          this.salvando.set(false);
          this.carregar();
        },
      });
  }

  private abrirDialogo(): void {
    this.erroFormulario.set(null);
    this.dialogoAberto = this.dialogo.open(this.formularioTpl(), {
      width: '34rem',
      maxWidth: '95vw',
    });
  }

  protected alternarStatus(usuario: Usuario): void {
    const novoStatus = usuario.status === 'ATIVO' ? 'INATIVO' : 'ATIVO';
    this.usuarioService.alterarStatus(usuario.id, novoStatus).subscribe({
      next: () => {
        this.avisar(
          `${usuario.nomeCompleto}: ${novoStatus === 'ATIVO' ? 'ativado(a)' : 'desativado(a)'}.`,
        );
        this.carregar();
      },
      error: (e: unknown) => this.avisar(mensagemDeErro(e)),
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
        this.avisar(mensagemDeErro(e));
      },
    });
  }

  private avisar(texto: string): void {
    this.avisos.open(texto, 'OK', { duration: 6000 });
  }
}
