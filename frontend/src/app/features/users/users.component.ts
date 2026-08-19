import { DatePipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { finalize } from "rxjs";
import { errorMessage } from "../../shared/feedback";
import { roleLabel } from "../../shared/role-labels";

interface SystemUser {
  id: string;
  fullName: string;
  email: string;
  phone?: string;
  active: boolean;
  hasAvatar: boolean;
  employeeNumber?: string;
  createdAt: string;
  roles: string[];
}

@Component({
  selector: "app-users",
  imports: [DatePipe, FormsModule],
  templateUrl: "./users.component.html",
  styleUrl: "./users.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersComponent {
  readonly users = signal<SystemUser[]>([]);
  readonly loading = signal(true);
  readonly message = signal("");
  search = "";

  constructor(private readonly http: HttpClient) {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.message.set("");
    this.http
      .get<SystemUser[]>("/api/v1/dev/users", {
        params: { size: 100, search: this.search.trim() },
      })
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (users) => this.users.set(users),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  toggle(user: SystemUser): void {
    this.http
      .patch<void>(`/api/v1/dev/users/${user.id}/active`, {
        active: !user.active,
      })
      .subscribe({
        next: () =>
          this.users.update((users) =>
            users.map((item) =>
              item.id === user.id ? { ...item, active: !item.active } : item,
            ),
          ),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }

  roleName(role: string): string {
    return roleLabel(role);
  }
}
