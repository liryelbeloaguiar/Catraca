import { ChangeDetectionStrategy, Component } from "@angular/core";
import { FeedbackBannerComponent } from "./feedback-banner.component";
import { ToastService } from "./toast.service";

@Component({
  selector: "app-toast-container",
  imports: [FeedbackBannerComponent],
  template: `<aside class="toasts" aria-label="Notificações do sistema">@for (toast of toasts.messages(); track toast.id) { <app-feedback-banner [kind]="toast.kind" [message]="toast.text" [dismissible]="true" (dismiss)="toasts.dismiss(toast.id)" /> }</aside>`,
  styles: `.toasts{position:fixed;z-index:2000;top:20px;right:20px;display:grid;gap:10px;width:min(390px,calc(100vw - 40px));pointer-events:none}.toasts app-feedback-banner{pointer-events:auto;box-shadow:var(--shadow-sm)}`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ToastContainerComponent { constructor(readonly toasts: ToastService) {} }
