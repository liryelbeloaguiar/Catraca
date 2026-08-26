import { Injectable, signal } from "@angular/core";
import { FeedbackKind } from "./feedback-banner.component";

export interface ToastMessage { id: number; kind: FeedbackKind; text: string; }

@Injectable({ providedIn: "root" })
export class ToastService {
  private nextId = 0;
  readonly messages = signal<ToastMessage[]>([]);
  show(text: string, kind: FeedbackKind = "info", duration = 4000): void {
    const id = ++this.nextId;
    this.messages.update(items => [...items, { id, kind, text }]);
    window.setTimeout(() => this.dismiss(id), duration);
  }
  success(text: string): void { this.show(text, "success"); }
  error(text: string): void { this.show(text, "error", 6000); }
  dismiss(id: number): void { this.messages.update(items => items.filter(item => item.id !== id)); }
}
