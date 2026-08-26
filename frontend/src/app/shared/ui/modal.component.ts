import { ChangeDetectionStrategy, Component, EventEmitter, HostListener, Input, Output } from "@angular/core";
import { AppIconComponent } from "../app-icon.component";

@Component({
  selector: "app-modal",
  imports: [AppIconComponent],
  template: `<div class="backdrop" role="presentation" (click)="close.emit()"><section role="dialog" aria-modal="true" [attr.aria-label]="title" (click)="$event.stopPropagation()"><header><h2>{{ title }}</h2><button type="button" aria-label="Fechar" (click)="close.emit()"><app-icon name="x" /></button></header><div class="body"><ng-content /></div><footer><ng-content select="[modal-actions]" /></footer></section></div>`,
  styles: `.backdrop{position:fixed;inset:0;z-index:1000;display:grid;place-items:center;padding:20px;background:rgba(10,34,39,.6)}section{width:min(620px,100%);max-height:calc(100vh - 40px);overflow:auto;border-radius:var(--radius-lg);background:var(--color-surface);box-shadow:var(--shadow-md)}header{display:flex;align-items:center;justify-content:space-between;padding:20px 22px;border-bottom:1px solid var(--color-border-soft)}h2{margin:0}button{border:0;background:transparent;cursor:pointer}.body{padding:22px}footer:has(*){display:flex;justify-content:flex-end;gap:10px;padding:16px 22px;border-top:1px solid var(--color-border-soft)}`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ModalComponent {
  @Input({ required: true }) title = "";
  @Output() readonly close = new EventEmitter<void>();
  @HostListener("document:keydown.escape") onEscape(): void { this.close.emit(); }
}
