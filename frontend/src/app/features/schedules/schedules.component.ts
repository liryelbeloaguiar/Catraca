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
import { AppIconComponent } from "../../shared/app-icon.component";

@Component({
  selector: "app-schedules",
  imports: [ReactiveFormsModule, AppIconComponent],
  templateUrl: "./schedules.component.html",
  styleUrl: "./schedules.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SchedulesComponent {
  readonly schedules = signal<ApiRecord[]>([]);
  readonly professionals = signal<ApiRecord[]>([]);
  readonly units = signal<ApiRecord[]>([]);
  readonly rooms = signal<ApiRecord[]>([]);
  readonly selectedDays = signal<Set<number>>(new Set());
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly editingId = signal<string | null>(null);
  readonly message = signal("");
  readonly success = signal("");
  readonly days = [
    { value: 1, label: "Seg", full: "Segunda" },
    { value: 2, label: "Ter", full: "Terça" },
    { value: 3, label: "Qua", full: "Quarta" },
    { value: 4, label: "Qui", full: "Quinta" },
    { value: 5, label: "Sex", full: "Sexta" },
    { value: 6, label: "Sáb", full: "Sábado" },
    { value: 7, label: "Dom", full: "Domingo" },
  ];
  readonly form = new FormGroup({
    professionalId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    unitId: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    roomId: new FormControl("", { nonNullable: true }),
    validFrom: new FormControl(this.inputDate(0), {
      nonNullable: true,
      validators: Validators.required,
    }),
    validUntil: new FormControl(this.inputDate(30), {
      nonNullable: true,
      validators: Validators.required,
    }),
    startTime: new FormControl("08:00", {
      nonNullable: true,
      validators: Validators.required,
    }),
    endTime: new FormControl("17:00", {
      nonNullable: true,
      validators: Validators.required,
    }),
    breakStart: new FormControl("12:00", { nonNullable: true }),
    breakEnd: new FormControl("13:00", { nonNullable: true }),
    slotDurationMinutes: new FormControl(30, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(5)],
    }),
    capacity: new FormControl(1, {
      nonNullable: true,
      validators: [Validators.required, Validators.min(1)],
    }),
  });
  constructor(private readonly http: HttpClient) {
    this.load();
  }
  toggleDay(day: number): void {
    const next = new Set(this.selectedDays());
    next.has(day) ? next.delete(day) : next.add(day);
    this.selectedDays.set(next);
  }
  roomsForUnit(): ApiRecord[] {
    return this.rooms().filter(
      (room) =>
        room["unit_id"] === this.form.controls.unitId.value &&
        room["active"] === true,
    );
  }
  selectedProfessional(): string {
    return String(
      this.professionals().find(
        (item) => item["id"] === this.form.controls.professionalId.value,
      )?.["full_name"] ?? "Não selecionado",
    );
  }
  selectedUnit(): string {
    return String(
      this.units().find(
        (item) => item["id"] === this.form.controls.unitId.value,
      )?.["name"] ?? "Não selecionada",
    );
  }
  selectedDaysLabel(): string {
    const selected = this.days.filter((day) =>
      this.selectedDays().has(day.value),
    );
    return selected.length
      ? selected.map((day) => day.label).join(", ")
      : "Nenhum dia";
  }
  estimatedSlots(): number {
    const values = this.form.getRawValue();
    if (!values.validFrom || !values.validUntil || !this.selectedDays().size)
      return 0;
    const from = new Date(`${values.validFrom}T12:00:00`);
    const until = new Date(`${values.validUntil}T12:00:00`);
    if (until < from) return 0;
    const minutes = (time: string) => {
      const [hour, minute] = time.split(":").map(Number);
      return hour * 60 + minute;
    };
    let dailyMinutes = Math.max(
      0,
      minutes(values.endTime) - minutes(values.startTime),
    );
    if (values.breakStart && values.breakEnd)
      dailyMinutes -= Math.max(
        0,
        minutes(values.breakEnd) - minutes(values.breakStart),
      );
    const slotsPerDay = Math.max(
      0,
      Math.floor(dailyMinutes / values.slotDurationMinutes),
    );
    let workingDays = 0;
    for (
      const day = new Date(from);
      day <= until;
      day.setDate(day.getDate() + 1)
    ) {
      const isoDay = day.getDay() || 7;
      if (this.selectedDays().has(isoDay)) workingDays++;
    }
    return workingDays * slotsPerDay;
  }
  openCreate(): void {
    if (this.showForm() && !this.editingId()) {
      this.cancelEdit();
      return;
    }
    this.resetForm();
    this.showForm.set(true);
  }
  edit(schedule: ApiRecord): void {
    this.editingId.set(String(schedule["id"]));
    this.selectedDays.set(
      new Set(
        String(schedule["days_of_week"] ?? "")
          .split(",")
          .filter(Boolean)
          .map(Number),
      ),
    );
    this.form.reset({
      professionalId: String(schedule["professional_id"] ?? ""),
      unitId: String(schedule["unit_id"] ?? ""),
      roomId: String(schedule["room_id"] ?? ""),
      validFrom: String(schedule["valid_from"] ?? ""),
      validUntil: String(schedule["valid_until"] ?? ""),
      startTime: String(schedule["start_time"] ?? "08:00").slice(0, 5),
      endTime: String(schedule["end_time"] ?? "17:00").slice(0, 5),
      breakStart: String(schedule["break_start"] ?? "").slice(0, 5),
      breakEnd: String(schedule["break_end"] ?? "").slice(0, 5),
      slotDurationMinutes: Number(schedule["slot_duration_minutes"] ?? 30),
      capacity: Number(schedule["capacity"] ?? 1),
    });
    this.form.controls.professionalId.disable();
    this.form.controls.unitId.disable();
    this.message.set("");
    this.success.set("");
    this.showForm.set(true);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }
  cancelEdit(): void {
    this.resetForm();
    this.showForm.set(false);
  }
  save(): void {
    if (this.form.invalid || !this.selectedDays().size) {
      this.message.set(
        "Preencha os campos e selecione ao menos um dia da semana.",
      );
      return;
    }
    this.saving.set(true);
    this.message.set("");
    this.success.set("");
    const values = this.form.getRawValue();
    const id = this.editingId();
    const request = id
      ? this.http.put<{ id: string; slotsCreated: number; patientsNotified: number }>(
          `/api/v1/schedules/${id}`,
          {
            ...values,
            roomId: values.roomId || null,
            breakStart: values.breakStart || null,
            breakEnd: values.breakEnd || null,
            daysOfWeek: [...this.selectedDays()],
          },
        )
      : this.http.post<{ id: string; slotsCreated: number }>(
          "/api/v1/schedules",
          {
        ...values,
        roomId: values.roomId || null,
        breakStart: values.breakStart || null,
        breakEnd: values.breakEnd || null,
        daysOfWeek: [...this.selectedDays()],
          },
        );
    request
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (result) => {
          this.success.set(id
            ? `Escala atualizada, ${result.slotsCreated} horário(s) futuro(s) gerado(s) e ${"patientsNotified" in result ? result.patientsNotified : 0} paciente(s) avisado(s).`
            : `Escala criada com ${result.slotsCreated} horário(s).`);
          this.cancelEdit();
          this.load();
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  private resetForm(): void {
    this.editingId.set(null);
    this.selectedDays.set(new Set());
    this.form.controls.professionalId.enable();
    this.form.controls.unitId.enable();
    this.form.reset({
      professionalId: "",
      unitId: "",
      roomId: "",
      validFrom: this.inputDate(0),
      validUntil: this.inputDate(30),
      startTime: "08:00",
      endTime: "17:00",
      breakStart: "12:00",
      breakEnd: "13:00",
      slotDurationMinutes: 30,
      capacity: 1,
    });
  }
  toggle(schedule: ApiRecord): void {
    this.http
      .patch(`/api/v1/schedules/${schedule["id"]}/active`, {
        active: !schedule["active"],
      })
      .subscribe({
        next: () => this.load(),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  private load(): void {
    forkJoin({
      schedules: this.http.get<ApiRecord[]>("/api/v1/schedules"),
      professionals: this.http.get<ApiRecord[]>("/api/v1/professionals"),
      units: this.http.get<ApiRecord[]>("/api/v1/units", {
        params: { size: 100 },
      }),
      rooms: this.http.get<ApiRecord[]>("/api/v1/rooms", {
        params: { size: 100 },
      }),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (r) => {
          this.schedules.set(r.schedules);
          this.professionals.set(
            r.professionals.filter((p) => p["active"] === true),
          );
          this.units.set(r.units.filter((u) => u["active"] === true));
          this.rooms.set(r.rooms);
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  private inputDate(offset: number): string {
    const date = new Date();
    date.setDate(date.getDate() + offset);
    return date.toISOString().slice(0, 10);
  }
}
