import { HttpClient } from "@angular/common/http";
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  signal,
} from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { AuthService } from "../core/auth.service";

@Component({
  selector: "app-shell",
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: "./shell.component.html",
  styleUrl: "./shell.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent implements OnDestroy {
  readonly menuOpen = signal(false);
  readonly avatarUrl = signal<string | null>(null);
  constructor(
    readonly auth: AuthService,
    private readonly http: HttpClient,
  ) {
    this.http
      .get<{ id: string; hasAvatar: boolean }>("/api/v1/profile")
      .subscribe((profile) => {
        if (profile.hasAvatar)
          this.http
            .get(`/api/v1/profile/avatar/${profile.id}`, {
              responseType: "blob",
            })
            .subscribe((blob) => this.avatarUrl.set(URL.createObjectURL(blob)));
      });
  }
  initials(): string {
    return (this.auth.session()?.fullName ?? "QF")
      .split(" ")
      .slice(0, 2)
      .map((name) => name[0])
      .join("")
      .toUpperCase();
  }
  roleLabel(): string {
    const role =
      this.auth
        .session()
        ?.authorities.find((authority) => authority.startsWith("ROLE_"))
        ?.replace("ROLE_", "") ?? "USUÁRIO";
    const names: Record<string, string> = {
      DEV_ADMIN: "Administrador técnico",
      ADMIN_USER: "Administrador de usuários",
      ADMIN: "Administrador",
      PATIENT: "Paciente",
      DOCTOR: "Médico",
      PROFESSIONAL: "Profissional",
      RECEPTIONIST: "Recepcionista",
      COUNTER_ATTENDANT: "Atendente de guichê",
    };
    return names[role] ?? role;
  }
  ngOnDestroy(): void {
    if (this.avatarUrl()) URL.revokeObjectURL(this.avatarUrl()!);
  }
}
