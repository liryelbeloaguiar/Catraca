import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from "@angular/core";
import { AppIconComponent } from "../app-icon.component";

export type FeedbackKind = "success" | "error" | "warning" | "info";

@Component({
  selector: "app-feedback-banner",
  imports: [AppIconComponent],
  template: `
    <div class="feedback" [class]="'feedback ' + kind" role="status" aria-live="polite">
      <app-icon [name]="kind === 'error' || kind === 'warning' ? 'warning' : kind === 'success' ? 'check' : 'info'" />
      <span><ng-content />{{ message }}</span>
      @if (dismissible) {
        <button type="button" aria-label="Fechar mensagem" (click)="dismiss.emit()"><app-icon name="x" /></button>
      }
    </div>
  `,
  styles: `
    .feedback { display:flex; align-items:flex-start; gap:10px; padding:12px 14px; border:1px solid var(--color-border); border-radius:var(--radius-md); color:var(--color-text-soft); background:var(--color-surface-muted); }
    .feedback.error { color:var(--color-danger); border-color:color-mix(in srgb, var(--color-danger) 25%, white); background:var(--color-danger-bg); }
    .feedback.success { color:var(--color-success); border-color:color-mix(in srgb, var(--color-success) 25%, white); background:var(--color-success-bg); }
    .feedback.warning { color:var(--color-warning); background:var(--color-warning-bg); }
    span { flex:1; line-height:1.45; } button { border:0; padding:0; color:inherit; background:transparent; cursor:pointer; }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackBannerComponent {
  @Input() kind: FeedbackKind = "info";
  @Input() message = "";
  @Input() dismissible = false;
  @Output() readonly dismiss = new EventEmitter<void>();
}
