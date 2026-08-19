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
import { finalize } from "rxjs";
import { AuthService } from "../../core/auth.service";
import { errorMessage } from "../../shared/feedback";
import { roleLabel } from "../../shared/role-labels";

interface UserProfile {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  active: boolean;
  hasAvatar: boolean;
  employeeNumber?: string;
  badgeCode?: string;
  jobTitle?: string;
  hiredOn?: string;
  roles: string[];
}

@Component({
  selector: "app-profile",
  imports: [ReactiveFormsModule],
  templateUrl: "./profile.component.html",
  styleUrl: "./profile.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnDestroy {
  readonly profile = signal<UserProfile | null>(null);
  readonly avatarUrl = signal<string | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly uploading = signal(false);
  readonly message = signal("");
  readonly success = signal("");
  readonly profileForm = new FormGroup({
    fullName: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.maxLength(160)],
    }),
    phone: new FormControl("", {
      nonNullable: true,
      validators: Validators.maxLength(30),
    }),
  });
  readonly passwordForm = new FormGroup({
    currentPassword: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
    newPassword: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(8)],
    }),
    confirmPassword: new FormControl("", {
      nonNullable: true,
      validators: Validators.required,
    }),
  });

  constructor(
    private readonly http: HttpClient,
    private readonly auth: AuthService,
  ) {
    this.load();
  }

  saveProfile(): void {
    if (this.profileForm.invalid) return;
    this.saving.set(true);
    this.clearFeedback();
    this.http
      .patch<UserProfile>("/api/v1/profile", this.profileForm.getRawValue())
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.auth.updateFullName(profile.fullName);
          this.success.set("Perfil atualizado com sucesso.");
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  changePassword(): void {
    if (this.passwordForm.invalid) return;
    const values = this.passwordForm.getRawValue();
    if (values.newPassword !== values.confirmPassword) {
      this.message.set("A confirmação da nova senha não confere.");
      return;
    }
    this.saving.set(true);
    this.clearFeedback();
    this.http
      .post<void>("/api/v1/profile/password", {
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
      })
      .pipe(finalize(() => this.saving.set(false)))
      .subscribe({
        next: () => {
          this.passwordForm.reset();
          this.success.set(
            "Senha alterada. Entre novamente nos outros dispositivos.",
          );
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  uploadAvatar(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;
    const form = new FormData();
    form.append("file", file);
    this.uploading.set(true);
    this.clearFeedback();
    this.http
      .post<void>("/api/v1/profile/avatar", form)
      .pipe(finalize(() => this.uploading.set(false)))
      .subscribe({
        next: () => {
          this.success.set("Foto atualizada com sucesso.");
          this.loadAvatar();
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  initials(): string {
    return (this.profile()?.fullName ?? "QF")
      .split(" ")
      .slice(0, 2)
      .map((part) => part[0])
      .join("")
      .toUpperCase();
  }
  roleName(role: string): string {
    return roleLabel(role);
  }
  ngOnDestroy(): void {
    if (this.avatarUrl()) URL.revokeObjectURL(this.avatarUrl()!);
  }

  private load(): void {
    this.http
      .get<UserProfile>("/api/v1/profile")
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (profile) => {
          this.profile.set(profile);
          this.profileForm.patchValue({
            fullName: profile.fullName,
            phone: profile.phone ?? "",
          });
          if (profile.hasAvatar) this.loadAvatar();
        },
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
  private loadAvatar(): void {
    const profile = this.profile();
    if (!profile) return;
    this.http
      .get(`/api/v1/profile/avatar/${profile.id}`, { responseType: "blob" })
      .subscribe((blob) => {
        if (this.avatarUrl()) URL.revokeObjectURL(this.avatarUrl()!);
        this.avatarUrl.set(URL.createObjectURL(blob));
      });
  }
  private clearFeedback(): void {
    this.message.set("");
    this.success.set("");
  }
}
