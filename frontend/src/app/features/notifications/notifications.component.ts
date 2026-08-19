import { DatePipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { finalize } from "rxjs";
import { ApiRecord } from "../../core/models";
import { errorMessage } from "../../shared/feedback";

@Component({
  selector: "app-notifications",
  imports: [DatePipe],
  templateUrl: "./notifications.component.html",
  styleUrl: "./notifications.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationsComponent {
  readonly rows = signal<ApiRecord[]>([]);
  readonly loading = signal(true);
  readonly message = signal("");

  constructor(private readonly http: HttpClient) {
    this.load();
  }

  markRead(row: ApiRecord): void {
    if (row["read_at"]) return;
    this.http
      .patch<void>(`/api/v1/notifications/${row["id"]}/read`, {})
      .subscribe({
        next: () =>
          this.rows.update((items) =>
            items.map((item) =>
              item["id"] === row["id"]
                ? { ...item, read_at: new Date().toISOString() }
                : item,
            ),
          ),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  emailLabel(status: unknown): string {
    const labels: Record<string, string> = {
      SENT: "E-mail enviado",
      QUEUED: "Envio em andamento",
      FAILED: "Falha no e-mail",
      PENDING_CONFIGURATION: "Aguardando configuração SMTP",
    };
    return labels[String(status)] ?? String(status);
  }

  asDate(value: unknown): string | number | Date | null {
    return typeof value === "string" || typeof value === "number" || value instanceof Date
      ? value
      : null;
  }

  private load(): void {
    this.http
      .get<ApiRecord[]>("/api/v1/notifications", { params: { size: 100 } })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.rows.set(rows),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
