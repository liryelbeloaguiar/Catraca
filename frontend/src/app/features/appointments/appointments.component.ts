import { DatePipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { finalize, forkJoin, Observable } from "rxjs";
import { AuthService } from "../../core/auth.service";
import { ApiRecord } from "../../core/models";
import { errorMessage } from "../../shared/feedback";
import { AppIconComponent } from "../../shared/app-icon.component";

@Component({
  selector: "app-appointments",
  imports: [DatePipe, ReactiveFormsModule, AppIconComponent],
  templateUrl: "./appointments.component.html",
  styleUrl: "./appointments.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppointmentsComponent {
  readonly rows = signal<ApiRecord[]>([]);
  readonly units = signal<ApiRecord[]>([]);
  readonly services = signal<ApiRecord[]>([]);
  readonly slots = signal<ApiRecord[]>([]);
  readonly counters = signal<ApiRecord[]>([]);
  readonly loading = signal(true);
  readonly loadingSlots = signal(false);
  readonly saving = signal(false);
  readonly message = signal("");
  readonly success = signal("");
  readonly showForm = signal(false);
  readonly patientId = signal("");
  readonly isPatient: boolean;
  readonly canOverbook: boolean;
  readonly form = new FormGroup({
    unitId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    serviceId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    timeSlotId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    desiredDate: new FormControl(new Date().toISOString().slice(0, 10), {
      nonNullable: true,
      validators: Validators.required,
    }),
    guestName: new FormControl("", { nonNullable: true }),
    counterId: new FormControl("", { nonNullable: true }),
    allowOverbook: new FormControl(false, { nonNullable: true }),
  });
  constructor(
    private readonly http: HttpClient,
    auth: AuthService,
  ) {
    this.isPatient = auth.has("ROLE_PATIENT");
    this.canOverbook = auth.has("APPOINTMENT_OVERBOOK");
    this.loadAppointments();
    this.loadSchedulingOptions();
    this.form.controls.unitId.valueChanges.subscribe((id) =>
      this.loadSlots(id),
    );
    this.form.controls.desiredDate.valueChanges.subscribe(() =>
      this.loadSlots(this.form.controls.unitId.value),
    );
  }
  schedule(): void {
    const values = this.form.getRawValue();
    if (
      this.form.invalid ||
      (this.isPatient && !this.patientId()) ||
      (!this.isPatient && values.guestName.trim().length < 2)
    ) {
      this.message.set("Preencha os dados obrigatórios do agendamento.");
      return;
    }
    if (this.requiresCounter() && !values.counterId) {
      this.message.set("Selecione o guichê de destino.");
      return;
    }
    this.saving.set(true);
    this.message.set("");
    this.success.set("");
    this.http
      .post<{ id: string }>("/api/v1/appointments", {
        patientId: this.isPatient ? this.patientId() : null,
        guestName: this.isPatient ? null : values.guestName.trim(),
        unitId: values.unitId,
        serviceId: values.serviceId,
        timeSlotId: values.timeSlotId,
        counterId: values.counterId || null,
        allowOverbook: this.canOverbook && values.allowOverbook,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.success.set(
            values.allowOverbook
              ? "Encaixe realizado com sucesso."
              : "Agendamento realizado com sucesso.",
          );
          this.showForm.set(false);
          this.form.reset({
            unitId: "",
            serviceId: "",
            timeSlotId: "",
            desiredDate: new Date().toISOString().slice(0, 10),
            guestName: "",
            counterId: "",
            allowOverbook: false,
          });
          this.loadAppointments();
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  slotLabel(slot: ApiRecord): string {
    const value =
      typeof slot["startsAt"] === "string" ? new Date(slot["startsAt"]) : null;
    if (!value) return "Horário inválido";
    const date = new Intl.DateTimeFormat("pt-BR", {
      timeStyle: "short",
    }).format(value);
    const available = Number(slot["availableCapacity"]);
    return `${date} — ${slot["professionalName"]} — ${available > 0 ? available + " vaga(s)" : "lotado / encaixe"}`;
  }
  requiresCounter(): boolean {
    return (
      this.services().find(
        (item) => item["id"] === this.form.controls.serviceId.value,
      )?.["requires_counter"] === true
    );
  }
  countersForUnit(): ApiRecord[] {
    return this.counters().filter(
      (item) =>
        item["unit_id"] === this.form.controls.unitId.value &&
        item["active"] === true,
    );
  }
  asDate(value: ApiRecord[string]): string | null {
    return typeof value === "string" ? value : null;
  }
  private loadAppointments(): void {
    this.loading.set(true);
    this.http
      .get<ApiRecord[]>("/api/v1/appointments")
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (rows) => this.rows.set(rows),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  private loadSchedulingOptions(): void {
    const requests: Record<string, Observable<unknown>> = {
      units: this.http.get<ApiRecord[]>("/api/v1/units", {
        params: { size: 100 },
      }),
      services: this.http.get<ApiRecord[]>("/api/v1/services", {
        params: { size: 100 },
      }),
      counters: this.http.get<ApiRecord[]>("/api/v1/counters", {
        params: { size: 100 },
      }),
    };
    if (this.isPatient)
      requests["patient"] = this.http.get<{ id: string }>(
        "/api/v1/patients/me",
      );
    forkJoin(requests).subscribe({
      next: (result) => {
        const typed = result as {
          units: ApiRecord[];
          services: ApiRecord[];
          counters: ApiRecord[];
          patient?: { id: string };
        };
        if (typed.patient) this.patientId.set(typed.patient.id);
        this.units.set(typed.units.filter((item) => item["active"] === true));
        this.services.set(
          typed.services.filter((item) => item["active"] === true),
        );
        this.counters.set(typed.counters);
      },
      error: (error) => this.message.set(errorMessage(error)),
    });
  }
  private loadSlots(unitId: string): void {
    this.slots.set([]);
    this.form.controls.timeSlotId.setValue("", { emitEvent: false });
    if (!unitId) return;
    const from = new Date(`${this.form.controls.desiredDate.value}T00:00:00`);
    const to = new Date(from);
    to.setDate(to.getDate() + 1);
    this.loadingSlots.set(true);
    this.http
      .get<ApiRecord[]>("/api/v1/time-slots", {
        params: { unitId, from: from.toISOString(), to: to.toISOString() },
      })
      .pipe(finalize(() => this.loadingSlots.set(false)))
      .subscribe({
        next: (slots) =>
          this.slots.set(
            slots.filter(
              (slot) =>
                slot["blocked"] !== true &&
                (Number(slot["availableCapacity"]) > 0 || this.canOverbook),
            ),
          ),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
