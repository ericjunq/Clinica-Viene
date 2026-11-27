import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';

/**
 * Quadro para desenhar a assinatura com o dedo, a caneta ou o mouse. Emite o PNG (data URI) a cada traço,
 * ou null quando limpo.
 */
@Component({
  selector: 'app-quadro-assinatura',
  imports: [MatButtonModule],
  template: `
    <div class="quadro">
      <canvas
        #tela
        aria-label="Área para desenhar a assinatura"
        role="img"
        (pointerdown)="iniciar($event)"
        (pointermove)="mover($event)"
        (pointerup)="terminar()"
        (pointerleave)="terminar()"
        (pointercancel)="terminar()"
      ></canvas>
      @if (vazio()) {
        <span class="dica">Assine aqui</span>
      }
    </div>
    <button mat-button type="button" (click)="limpar()" [disabled]="vazio()">Limpar</button>
  `,
  styles: `
    .quadro {
      position: relative;
      border: 1px dashed var(--viene-caramelo);
      border-radius: 0.75rem;
      background: #fff;
    }
    canvas {
      display: block;
      width: 100%;
      height: 11rem;
      touch-action: none;
      cursor: crosshair;
    }
    .dica {
      position: absolute;
      inset: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      color: var(--mat-sys-outline);
      pointer-events: none;
      font-family: var(--viene-fonte-titulo);
      font-size: 1.4rem;
    }
  `,
})
export class QuadroAssinaturaComponent implements AfterViewInit, OnDestroy {
  readonly assinatura = output<string | null>();
  protected readonly vazio = signal(true);

  private readonly tela = viewChild.required<ElementRef<HTMLCanvasElement>>('tela');
  private contexto: CanvasRenderingContext2D | null = null;
  private desenhando = false;
  private readonly aoRedimensionar = () => this.ajustarTamanho();

  ngAfterViewInit(): void {
    this.ajustarTamanho();
    window.addEventListener('resize', this.aoRedimensionar);
  }

  ngOnDestroy(): void {
    window.removeEventListener('resize', this.aoRedimensionar);
  }

  protected iniciar(evento: PointerEvent): void {
    if (!this.contexto) {
      return;
    }
    this.desenhando = true;
    this.tela().nativeElement.setPointerCapture?.(evento.pointerId);
    const { x, y } = this.ponto(evento);
    this.contexto.beginPath();
    this.contexto.moveTo(x, y);
    this.contexto.lineTo(x + 0.1, y + 0.1);
    this.contexto.stroke();
  }

  protected mover(evento: PointerEvent): void {
    if (!this.desenhando || !this.contexto) {
      return;
    }
    const { x, y } = this.ponto(evento);
    this.contexto.lineTo(x, y);
    this.contexto.stroke();
  }

  protected terminar(): void {
    if (!this.desenhando) {
      return;
    }
    this.desenhando = false;
    this.vazio.set(false);
    this.assinatura.emit(this.tela().nativeElement.toDataURL('image/png'));
  }

  limpar(): void {
    const canvas = this.tela().nativeElement;
    this.contexto?.clearRect(0, 0, canvas.width, canvas.height);
    this.vazio.set(true);
    this.assinatura.emit(null);
  }

  /** Resolução real = tamanho na tela (evita traço borrado). Redimensionar apaga o desenho. */
  private ajustarTamanho(): void {
    const canvas = this.tela().nativeElement;
    const largura = Math.max(1, Math.round(canvas.clientWidth));
    const altura = Math.max(1, Math.round(canvas.clientHeight));
    if (canvas.width === largura && canvas.height === altura) {
      return;
    }
    canvas.width = largura;
    canvas.height = altura;
    this.contexto = canvas.getContext('2d');
    if (this.contexto) {
      this.contexto.lineWidth = 2.5;
      this.contexto.lineCap = 'round';
      this.contexto.lineJoin = 'round';
      this.contexto.strokeStyle = '#2b1d12';
    }
    if (!this.vazio()) {
      this.limpar();
    }
  }

  private ponto(evento: PointerEvent): { x: number; y: number } {
    const caixa = this.tela().nativeElement.getBoundingClientRect();
    return { x: evento.clientX - caixa.left, y: evento.clientY - caixa.top };
  }
}
