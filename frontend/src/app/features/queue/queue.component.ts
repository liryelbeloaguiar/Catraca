import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { FormControl, ReactiveFormsModule, Validators } from "@angular/forms";
import { RouterLink } from "@angular/router";
import { finalize, forkJoin } from "rxjs";
import { AuthService } from "../../core/auth.service";
import { ApiRecord } from "../../core/models";
import { errorMessage } from "../../shared/feedback";
import { AppIconComponent } from "../../shared/app-icon.component";

interface DisplayPanel {
  id: string;
  name: string;
  floor?: string;
  publicToken: string;
  active: boolean;
  unitName: string;
}

@Component({
  selector: "app-queue",
  imports: [ReactiveFormsModule, RouterLink, AppIconComponent],
  templateUrl: "./queue.component.html",
  styleUrl: "./queue.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class QueueComponent {
  readonly queues = signal<ApiRecord[]>([]);
  readonly counters = signal<ApiRecord[]>([]);
  readonly rooms = signal<ApiRecord[]>([]);
  readonly panels = signal<DisplayPanel[]>([]);
  readonly loading = signal(true);
  readonly calling = signal(false);
  readonly message = signal("");
  readonly calledTicket = signal("");
  readonly failed = signal(false);
  readonly queueId = new FormControl("", {
    nonNullable: true,
    validators: Validators.required,
  });
  readonly counterId = new FormControl("", { nonNullable: true });
  readonly roomId = new FormControl("", { nonNullable: true });

  constructor(
    private readonly http: HttpClient,
    readonly auth: AuthService,
  ) {
    this.load();
    this.queueId.valueChanges.subscribe(() => {
      this.counterId.setValue("", { emitEvent: false });
      this.roomId.setValue("", { emitEvent: false });
    });
    this.counterId.valueChanges.subscribe((value) => {
      if (value) this.roomId.setValue("", { emitEvent: false });
    });
    this.roomId.valueChanges.subscribe((value) => {
      if (value) this.counterId.setValue("", { emitEvent: false });
    });
  }

  selectedQueue(): ApiRecord | undefined {
    return this.queues().find((queue) => queue["id"] === this.queueId.value);
  }
  countersForQueue(): ApiRecord[] {
    const unitId = this.selectedQueue()?.["unit_id"];
    return this.counters().filter(
      (item) => item["unit_id"] === unitId && item["active"] === true,
    );
  }
  roomsForQueue(): ApiRecord[] {
    const unitId = this.selectedQueue()?.["unit_id"];
    return this.rooms().filter(
      (item) => item["unit_id"] === unitId && item["active"] === true,
    );
  }
  selectQueue(queue: ApiRecord): void {
    this.queueId.setValue(String(queue["id"]));
    this.message.set("");
    this.calledTicket.set("");
  }
  callNext(): void {
    if (this.queueId.invalid) return;
    this.calling.set(true);
    this.message.set("");
    this.calledTicket.set("");
    this.http
      .post<ApiRecord>(`/api/v1/queues/${this.queueId.value}/call-next`, {
        counterId: this.counterId.value || null,
        roomId: this.roomId.value || null,
      })
      .pipe(finalize(() => this.calling.set(false)))
      .subscribe({
        next: (ticket) => {
          this.failed.set(false);
          this.calledTicket.set(String(ticket["displayCode"] ?? ticket["id"]));
          this.message.set("Chamada enviada aos painéis vinculados.");
        },
        error: (error) => {
          this.failed.set(true);
          this.message.set(errorMessage(error));
        },
      });
  }
  openPanel(panel: DisplayPanel): void {
    const opened = window.open(
      `/painel-publico/${panel.publicToken}`,
      "_blank",
      "noopener,noreferrer",
    );
    if (opened) opened.opener = null;
  }
  private load(): void {
    forkJoin({
      queues: this.http.get<ApiRecord[]>("/api/v1/queues", {
        params: { size: 100 },
      }),
      counters: this.http.get<ApiRecord[]>("/api/v1/counters", {
        params: { size: 100 },
      }),
      rooms: this.http.get<ApiRecord[]>("/api/v1/rooms", {
        params: { size: 100 },
      }),
      panels: this.http.get<DisplayPanel[]>("/api/v1/display-panels"),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.queues.set(
            result.queues.filter((item) => item["active"] === true),
          );
          this.counters.set(result.counters);
          this.rooms.set(result.rooms);
          this.panels.set(result.panels.filter((item) => item.active));
        },
        error: (error) => {
          this.failed.set(true);
          this.message.set(errorMessage(error));
        },
      });
  }
}
