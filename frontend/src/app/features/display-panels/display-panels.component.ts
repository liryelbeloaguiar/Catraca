import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { finalize, forkJoin } from "rxjs";
import { ApiRecord } from "../../core/models";
import { errorMessage } from "../../shared/feedback";

interface DisplayPanel {
  id: string;
  code: string;
  name: string;
  floor?: string;
  publicToken: string;
  audioEnabled: boolean;
  voiceEnabled: boolean;
  lastCallsLimit: number;
  active: boolean;
  unitId: string;
  unitName: string;
  queueNames: string;
}

@Component({
  selector: "app-display-panels",
  imports: [ReactiveFormsModule],
  templateUrl: "./display-panels.component.html",
  styleUrl: "./display-panels.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DisplayPanelsComponent {
  readonly panels = signal<DisplayPanel[]>([]);
  readonly units = signal<ApiRecord[]>([]);
  readonly queues = signal<ApiRecord[]>([]);
  readonly selectedQueues = signal<Set<string>>(new Set());
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly message = signal("");
  readonly success = signal("");
  readonly form = new FormGroup({
    unitId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    code: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    name: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    floor: new FormControl("", { nonNullable: true }),
    lastCallsLimit: new FormControl(5, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1), Validators.max(20)],
    }),
    audioEnabled: new FormControl(true, { nonNullable: true }),
    voiceEnabled: new FormControl(true, { nonNullable: true }),
  });

  constructor(private readonly http: HttpClient) {
    this.load();
    this.form.controls.unitId.valueChanges.subscribe(() =>
      this.selectedQueues.set(new Set()),
    );
  }

  queuesForUnit(): ApiRecord[] {
    return this.queues().filter(
      (queue) =>
        queue["unit_id"] === this.form.controls.unitId.value &&
        queue["active"] === true,
    );
  }

  toggleQueue(queueId: string): void {
    const selected = new Set(this.selectedQueues());
    selected.has(queueId) ? selected.delete(queueId) : selected.add(queueId);
    this.selectedQueues.set(selected);
  }

  create(): void {
    if (this.form.invalid || !this.selectedQueues().size) {
      this.message.set("Preencha os dados e selecione ao menos uma fila.");
      return;
    }
    this.saving.set(true);
    this.message.set("");
    this.success.set("");
    this.http
      .post<DisplayPanel>("/api/v1/display-panels", {
        ...this.form.getRawValue(),
        floor: this.form.controls.floor.value || null,
        queueIds: [...this.selectedQueues()],
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.success.set(
            "Painel criado. Ele já pode ser aberto em outra aba.",
          );
          this.showForm.set(false);
          this.selectedQueues.set(new Set());
          this.form.reset({
            unitId: "",
            code: "",
            name: "",
            floor: "",
            lastCallsLimit: 5,
            audioEnabled: true,
            voiceEnabled: true,
          });
          this.load();
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  toggle(panel: DisplayPanel): void {
    this.http
      .patch<void>(`/api/v1/display-panels/${panel.id}/active`, {
        active: !panel.active,
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.message.set(errorMessage(error)),
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
    this.loading.set(true);
    forkJoin({
      panels: this.http.get<DisplayPanel[]>("/api/v1/display-panels"),
      units: this.http.get<ApiRecord[]>("/api/v1/units", {
        params: { size: 100 },
      }),
      queues: this.http.get<ApiRecord[]>("/api/v1/queues", {
        params: { size: 100 },
      }),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.panels.set(result.panels);
          this.units.set(
            result.units.filter((unit) => unit["active"] === true),
          );
          this.queues.set(result.queues);
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
