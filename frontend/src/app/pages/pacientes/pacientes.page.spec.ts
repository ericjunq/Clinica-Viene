import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { NovoPaciente } from '../../core/models/paciente.model';
import { AuthService } from '../../core/services/auth.service';
import { PacientesPage } from './pacientes.page';

function nascimentoComIdade(anos: number): string {
  const d = new Date();
  return `${d.getFullYear() - anos}-01-01`;
}

describe('PacientesPage — pré-cadastro', () => {
  function criar(perfil: 'ADMIN' | 'MASTER') {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthService).usuario.set({ perfil } as never);
    const fixture = TestBed.createComponent(PacientesPage);
    return fixture.componentInstance;
  }

  function montar(pagina: PacientesPage, valores: Record<string, unknown>): NovoPaciente | string {
    pagina['form'].patchValue({
      nomeCompleto: 'Ana Teste',
      dataNascimento: nascimentoComIdade(30),
      telefone: '77999990000',
      cpf: '529.982.247-25',
      ...valores,
    });
    return pagina['montarPedido'](false);
  }

  it('monta o pedido de adulto com CPF, sem responsável nem endereço', () => {
    const pedido = montar(criar('ADMIN'), {});
    expect(pedido).toMatchObject({
      nomeCompleto: 'Ana Teste',
      cpf: '529.982.247-25',
      responsavel: null,
      endereco: null,
      semCpfJustificativa: null,
      confirmarDuplicidade: false,
    });
  });

  it('exige ao menos um contato', () => {
    expect(montar(criar('ADMIN'), { telefone: '', email: '' })).toBe(
      'Informe ao menos um contato: e-mail ou telefone.',
    );
  });

  it('menor de idade precisa de responsável com CPF e parentesco', () => {
    const pagina = criar('ADMIN');
    expect(
      montar(pagina, { dataNascimento: nascimentoComIdade(8), semCpf: true, cpf: '' }),
    ).toContain('informe nome, CPF e parentesco do responsável');
    const pedido = montar(pagina, {
      dataNascimento: nascimentoComIdade(8),
      semCpf: true,
      cpf: '',
      responsavelNome: 'Mãe Teste',
      responsavelCpf: '111.444.777-35',
      responsavelParentesco: 'MAE',
    });
    expect(pedido).toMatchObject({
      cpf: null,
      responsavel: { nomeCompleto: 'Mãe Teste', cpf: '111.444.777-35', parentesco: 'MAE' },
    });
  });

  it('adulto sem CPF: recepção não cadastra; MASTER cadastra com justificativa', () => {
    expect(montar(criar('ADMIN'), { semCpf: true, cpf: '' })).toBe(
      'Paciente adulto sem CPF só pode ser cadastrado pelo MASTER.',
    );
    TestBed.resetTestingModule();
    const master = criar('MASTER');
    expect(montar(master, { semCpf: true, cpf: '' })).toBe(
      'Informe a justificativa para cadastrar adulto sem CPF.',
    );
    expect(
      montar(master, { semCpf: true, cpf: '', semCpfJustificativa: 'Estrangeiro sem CPF' }),
    ).toMatchObject({ cpf: null, semCpfJustificativa: 'Estrangeiro sem CPF' });
  });

  it('enviar o formulário de busca chama a API sem recarregar a página', () => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
    TestBed.inject(AuthService).usuario.set({ perfil: 'ADMIN' } as never);
    const fixture = TestBed.createComponent(PacientesPage);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const elemento = fixture.nativeElement as HTMLElement;
    const campo = elemento.querySelector<HTMLInputElement>('#busca-paciente')!;
    campo.value = 'Maria';
    campo.dispatchEvent(new Event('input'));
    const envio = new Event('submit', { cancelable: true });
    elemento.querySelector('form')!.dispatchEvent(envio);

    expect(envio.defaultPrevented).toBe(true);
    http
      .expectOne((r) => r.url === '/api/v1/pacientes' && r.params.get('busca') === 'Maria')
      .flush([]);
    http.verify();
  });
});
