import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { finalize } from "rxjs";
import { errorMessage } from "../../shared/feedback";

interface OrganizationSettings {
  establishmentName: string;
  notificationEmail: string;
  updatedAt: string;
}

@Component({
  selector: "app-organization-settings",
  imports: [ReactiveFormsModule],
  templateUrl: "./organization-settings.component.html",
  styleUrl: "./organization-settings.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrganizationSettingsComponent {
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly message = signal("");
  readonly success = signal("");
  readonly form = new FormGroup({
    establishmentName: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    notificationEmail: new FormControl("liryelaguiargit@gmail.com", {
      nonNullable: true,
      validators: [Validators.required, Validators.email, Validators.maxLength(254)],
    }),
  });

  constructor(private readonly http: HttpClient) {
    this.http
      .get<OrganizationSettings>("/api/v1/admin/settings/organization")
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (settings) => this.form.patchValue(settings),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    this.message.set("");
    this.success.set("");
    this.http
      .patch<OrganizationSettings>(
        "/api/v1/admin/settings/organization",
        this.form.getRawValue(),
      )
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (settings) => {
          this.form.patchValue(settings);
          this.success.set("Dados do estabelecimento atualizados.");
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
