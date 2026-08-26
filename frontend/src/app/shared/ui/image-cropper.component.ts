import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  Output,
  ViewChild,
  signal,
} from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ModalComponent } from "./modal.component";

@Component({
  selector: "app-image-cropper",
  imports: [FormsModule, ModalComponent],
  template: `
    <app-modal title="Ajustar foto" (close)="cancel.emit()">
      <div class="crop-preview">
        <img
          #image
          [src]="sourceUrl"
          alt="Prévia do recorte da foto"
          [style.object-position]="positionX() + '% ' + positionY() + '%'"
          [style.transform]="'scale(' + zoom() + ')'"
        />
      </div>
      <div class="crop-controls">
        <label>Posição horizontal<input type="range" min="0" max="100" [ngModel]="positionX()" (ngModelChange)="positionX.set(+$event)" /></label>
        <label>Posição vertical<input type="range" min="0" max="100" [ngModel]="positionY()" (ngModelChange)="positionY.set(+$event)" /></label>
        <label>Zoom<input type="range" min="1" max="2.5" step="0.05" [ngModel]="zoom()" (ngModelChange)="zoom.set(+$event)" /></label>
      </div>
      @if (processing()) { <p class="processing" role="status">Preparando imagem…</p> }
      <div modal-actions>
        <button type="button" class="button ghost" (click)="cancel.emit()">Cancelar</button>
        <button type="button" class="button primary" [disabled]="processing()" (click)="crop()">Usar esta foto</button>
      </div>
    </app-modal>
  `,
  styles: `
    .crop-preview{width:min(320px,75vw);aspect-ratio:1;margin:0 auto 22px;overflow:hidden;border-radius:50%;background:var(--color-primary-50);box-shadow:inset 0 0 0 4px var(--color-surface),0 0 0 1px var(--color-border)}
    .crop-preview img{width:100%;height:100%;display:block;object-fit:cover;transition:transform var(--transition-fast);transform-origin:center}
    .crop-controls{display:grid;gap:14px}.crop-controls label{display:grid;gap:6px;color:var(--color-text-soft);font-size:13px;font-weight:700}.crop-controls input{width:100%;accent-color:var(--color-primary-600)}
    .processing{margin:14px 0 0;text-align:center;font-size:13px}
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageCropperComponent {
  @Input({ required: true }) sourceUrl = "";
  @Output() readonly cancel = new EventEmitter<void>();
  @Output() readonly confirm = new EventEmitter<Blob>();
  @ViewChild("image", { static: true }) private image!: ElementRef<HTMLImageElement>;

  readonly positionX = signal(50);
  readonly positionY = signal(50);
  readonly zoom = signal(1);
  readonly processing = signal(false);

  crop(): void {
    const image = this.image.nativeElement;
    if (!image.naturalWidth || !image.naturalHeight) return;
    this.processing.set(true);
    const canvas = document.createElement("canvas");
    canvas.width = 640;
    canvas.height = 640;
    const context = canvas.getContext("2d");
    if (!context) {
      this.processing.set(false);
      return;
    }

    const baseCrop = Math.min(image.naturalWidth, image.naturalHeight);
    const cropSize = baseCrop / this.zoom();
    const maxX = image.naturalWidth - cropSize;
    const maxY = image.naturalHeight - cropSize;
    const sourceX = maxX * (this.positionX() / 100);
    const sourceY = maxY * (this.positionY() / 100);
    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(image, sourceX, sourceY, cropSize, cropSize, 0, 0, canvas.width, canvas.height);
    canvas.toBlob((blob) => {
      this.processing.set(false);
      if (blob) this.confirm.emit(blob);
    }, "image/jpeg", 0.9);
  }
}
