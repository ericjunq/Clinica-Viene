/**
 * Infraestrutura compartilhada por todos os módulos: criptografia de campos (D-052), tratamento de erros
 * (Problem Details) e configuração de segurança.
 *
 * <p>Módulo aberto: os subpacotes podem ser usados diretamente pelos outros módulos.
 */
@ApplicationModule(displayName = "Compartilhado", type = ApplicationModule.Type.OPEN)
package br.com.clinicaviene.compartilhado;

import org.springframework.modulith.ApplicationModule;
