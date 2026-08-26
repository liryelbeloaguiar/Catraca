import { HttpClient } from "@angular/common/http";
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  signal,
} from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { finalize, forkJoin, Subscription } from "rxjs";
import { ApiRecord } from "../../core/models";
import { errorMessage } from "../../shared/feedback";
import { roleLabel } from "../../shared/role-labels";
import { AppIconComponent } from "../../shared/app-icon.component";

interface Employee {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  active: boolean;
  employeeNumber: string;
  badgeCode: string;
  jobTitle: string;
  hiredOn?: string;
  unitName?: string;
  roles: string[];
}
interface RoleOption {
  code: string;
  name: string;
}

@Component({
  selector: "app-employees",
  imports: [ReactiveFormsModule, AppIconComponent],
  templateUrl: "./employees.component.html",
  styleUrl: "./employees.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmployeesComponent implements OnDestroy {
  private avatarRequest?: Subscription;
  readonly employees = signal<Employee[]>([]);
  readonly roles = signal<RoleOption[]>([]);
  readonly units = signal<ApiRecord[]>([]);
  readonly professionalTypes = signal<ApiRecord[]>([]);
  readonly specialties = signal<ApiRecord[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly showForm = signal(false);
  readonly badge = signal<Employee | null>(null);
  readonly badgeAvatarUrl = signal<string | null>(null);
  readonly message = signal("");
  readonly success = signal("");
  readonly establishmentName = signal("Catraca");
  readonly form = new FormGroup({
    fullName: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    email: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    temporaryPassword: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8)],
    }),
    roleCode: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    jobTitle: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    phone: new FormControl("", { nonNullable: true }),
    unitId: new FormControl("", { nonNullable: true }),
    hiredOn: new FormControl("", { nonNullable: true }),
    professionalTypeId: new FormControl("", { nonNullable: true }),
    specialtyId: new FormControl("", { nonNullable: true }),
    registrationNumber: new FormControl("", { nonNullable: true }),
    defaultDurationMinutes: new FormControl(30, { nonNullable: true }),
  });

  constructor(private readonly http: HttpClient) {
    this.load();
  }

  create(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.message.set("");
    this.success.set("");
    const values = this.form.getRawValue();
    this.http
      .post<Employee>("/api/v1/admin/employees", {
        ...values,
        unitId: values.unitId || null,
        hiredOn: values.hiredOn || null,
        professionalTypeId: values.professionalTypeId || null,
        specialtyId: values.specialtyId || null,
        registrationNumber: values.registrationNumber || null,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (employee) => {
          this.employees.update((rows) =>
            [...rows, employee].sort((a, b) =>
              a.fullName.localeCompare(b.fullName),
            ),
          );
          this.form.reset();
          this.showForm.set(false);
          this.openBadge(employee);
          this.success.set("Funcionário cadastrado e crachá gerado.");
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  toggle(employee: Employee): void {
    this.http
      .patch<void>(`/api/v1/admin/employees/${employee.id}/active`, {
        active: !employee.active,
      })
      .subscribe({
        next: () =>
          this.employees.update((rows) =>
            rows.map((row) =>
              row.id === employee.id ? { ...row, active: !row.active } : row,
            ),
          ),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  initials(employee: Employee): string {
    return employee.fullName
      .split(" ")
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();
  }
  isProfessional(): boolean {
    return ["DOCTOR", "PROFESSIONAL"].includes(
      this.form.controls.roleCode.value,
    );
  }
  roleName(role: string): string {
    return roleLabel(role);
  }
  openBadge(employee: Employee): void {
    this.releaseBadgeAvatar();
    this.badge.set(employee);
    this.avatarRequest = this.http
      .get(`/api/v1/profile/avatar/${employee.id}`, { responseType: "blob" })
      .subscribe({
        next: (blob) =>
          this.badgeAvatarUrl.set(URL.createObjectURL(blob)),
        error: () => this.badgeAvatarUrl.set(null),
      });
  }
  closeBadge(): void {
    this.badge.set(null);
    this.releaseBadgeAvatar();
  }
  printBadge(): void {
    document.body.classList.add("badge-printing");
    window.addEventListener(
      "afterprint",
      () => document.body.classList.remove("badge-printing"),
      { once: true },
    );
    window.print();
  }
  ngOnDestroy(): void {
    this.releaseBadgeAvatar();
  }

  private releaseBadgeAvatar(): void {
    this.avatarRequest?.unsubscribe();
    this.avatarRequest = undefined;
    const current = this.badgeAvatarUrl();
    if (current) URL.revokeObjectURL(current);
    this.badgeAvatarUrl.set(null);
  }

  private load(): void {
    forkJoin({
      employees: this.http.get<Employee[]>("/api/v1/admin/employees"),
      roles: this.http.get<RoleOption[]>("/api/v1/admin/employees/roles"),
      units: this.http.get<ApiRecord[]>("/api/v1/units", {
        params: { size: 100 },
      }),
      professionalTypes: this.http.get<ApiRecord[]>(
        "/api/v1/professional-types",
        { params: { size: 100 } },
      ),
      specialties: this.http.get<ApiRecord[]>("/api/v1/specialties", {
        params: { size: 100 },
      }),
      settings: this.http.get<{ establishmentName: string }>(
        "/api/v1/admin/settings/organization",
      ),
    })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (result) => {
          this.employees.set(result.employees);
          this.roles.set(result.roles);
          this.units.set(
            result.units.filter((unit) => unit["active"] === true),
          );
          this.professionalTypes.set(
            result.professionalTypes.filter((item) => item["active"] === true),
          );
          this.specialties.set(
            result.specialties.filter((item) => item["active"] === true),
          );
          this.establishmentName.set(result.settings.establishmentName);
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
