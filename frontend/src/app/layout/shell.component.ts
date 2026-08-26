import { HttpClient } from "@angular/common/http";
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  signal,
} from "@angular/core";
import { RouterLink, RouterLinkActive, RouterOutlet } from "@angular/router";
import { AuthService } from "../core/auth.service";
import { AppIconComponent } from "../shared/app-icon.component";
import { AvatarService } from "../shared/avatar.service";
import { roleLabel } from "../shared/role-labels";

type SystemStatus = "checking" | "online" | "offline";

@Component({
  selector: "app-shell",
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AppIconComponent],
  templateUrl: "./shell.component.html",
  styleUrl: "./shell.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShellComponent implements OnDestroy {
  readonly menuOpen = signal(false);
  readonly avatarUrl = signal<string | null>(null);
  readonly systemStatus = signal<SystemStatus>("checking");
  constructor(
    readonly auth: AuthService,
    private readonly http: HttpClient,
    private readonly avatars: AvatarService,
  ) {
    this.http.get<{ status?: string }>("/healthz").subscribe({
      next: (health) => this.systemStatus.set(health.status === "UP" ? "online" : "offline"),
      error: () => this.systemStatus.set("offline"),
    });

    this.http
      .get<{ id: string; hasAvatar: boolean }>("/api/v1/profile")
      .subscribe((profile) => {
        if (profile.hasAvatar)
          this.avatars
            .load(profile.id)
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
    return roleLabel(role);
  }
  systemStatusLabel(): string {
    const labels: Record<SystemStatus, string> = {
      checking: "Verificando sistema",
      online: "Sistema online",
      offline: "Sistema indisponível",
    };
    return labels[this.systemStatus()];
  }

  ngOnDestroy(): void {
    if (this.avatarUrl()) URL.revokeObjectURL(this.avatarUrl()!);
  }
}
