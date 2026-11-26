import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { NgControl } from '@angular/forms';

/** Formata o campo como CPF (000.000.000-00) enquanto o usuário digita. */
@Directive({ selector: 'input[appCpfMask]' })
export class CpfMaskDirective {
  private readonly elemento = inject<ElementRef<HTMLInputElement>>(ElementRef);
  private readonly controle = inject(NgControl, { optional: true });

  @HostListener('input')
  aoDigitar(): void {
    const formatado = formatarCpf(this.elemento.nativeElement.value);
    this.elemento.nativeElement.value = formatado;
    this.controle?.control?.setValue(formatado, { emitEvent: false });
  }
}

export function formatarCpf(valor: string): string {
  const d = valor.replace(/\D/g, '').slice(0, 11);
  if (d.length <= 3) return d;
  if (d.length <= 6) return `${d.slice(0, 3)}.${d.slice(3)}`;
  if (d.length <= 9) return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6)}`;
  return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
}
