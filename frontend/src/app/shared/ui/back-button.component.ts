import { Location } from "@angular/common";
import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { Router } from "@angular/router";
import { AppIconComponent } from "../app-icon.component";

@Component({
  selector: "app-back-button",
  imports: [AppIconComponent],
  template: `<button type="button" class="back-button" (click)="goBack()"><app-icon name="arrowLeft" />{{ label }}</button>`,
  styles: `.back-button{display:inline-flex;align-items:center;gap:7px;margin:0 0 12px;padding:7px 10px;border:1px solid var(--color-border);border-radius:var(--radius-sm);color:var(--color-text-soft);background:var(--color-surface);font-size:12px;font-weight:700;cursor:pointer;transition:var(--transition-fast)}.back-button:hover{border-color:var(--color-primary-500);color:var(--color-primary-700);background:var(--color-primary-50)}`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BackButtonComponent {
  @Input() label = "Voltar";
  @Input() fallback = "/painel";

  constructor(private readonly location: Location, private readonly router: Router) {}

  goBack(): void {
    if (window.history.length > 1) this.location.back();
    else void this.router.navigateByUrl(this.fallback);
  }
}
