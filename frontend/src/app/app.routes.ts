import { Routes } from "@angular/router";
import { authGuard, devAdminGuard, guestGuard } from "./core/auth.guard";

export const routes: Routes = [
  {
    path: "login",
    canActivate: [guestGuard],
    loadComponent: () =>
      import("./features/auth/login.component").then((m) => m.LoginComponent),
  },
  {
    path: "cadastro",
    canActivate: [guestGuard],
    loadComponent: () =>
      import("./features/auth/register.component").then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: "painel-publico/:token",
    loadComponent: () =>
      import("./features/public-display/public-display.component").then(
        (m) => m.PublicDisplayComponent,
      ),
  },
  {
    path: "",
    canActivate: [authGuard],
    loadComponent: () =>
      import("./layout/shell.component").then((m) => m.ShellComponent),
    children: [
      { path: "", pathMatch: "full", redirectTo: "painel" },
      {
        path: "painel",
        loadComponent: () =>
          import("./features/dashboard/dashboard.component").then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        path: "agendamentos",
        loadComponent: () =>
          import("./features/appointments/appointments.component").then(
            (m) => m.AppointmentsComponent,
          ),
      },
      {
        path: "meu-perfil",
        loadComponent: () =>
          import("./features/profile/profile.component").then(
            (m) => m.ProfileComponent,
          ),
      },
      {
        path: "notificacoes",
        loadComponent: () =>
          import("./features/notifications/notifications.component").then(
            (m) => m.NotificationsComponent,
          ),
      },
      {
        path: "funcionarios",
        loadComponent: () =>
          import("./features/employees/employees.component").then(
            (m) => m.EmployeesComponent,
          ),
      },
      {
        path: "usuarios",
        canActivate: [devAdminGuard],
        loadComponent: () =>
          import("./features/users/users.component").then(
            (m) => m.UsersComponent,
          ),
      },
      {
        path: "estabelecimento",
        loadComponent: () =>
          import("./features/settings/organization-settings.component").then(
            (m) => m.OrganizationSettingsComponent,
          ),
      },
      {
        path: "escalas",
        loadComponent: () =>
          import("./features/schedules/schedules.component").then(
            (m) => m.SchedulesComponent,
          ),
      },
      {
        path: "paineis",
        loadComponent: () =>
          import("./features/display-panels/display-panels.component").then(
            (m) => m.DisplayPanelsComponent,
          ),
      },
      {
        path: "filas",
        loadComponent: () =>
          import("./features/queue/queue.component").then(
            (m) => m.QueueComponent,
          ),
      },
      {
        path: "administracao/:resource",
        loadComponent: () =>
          import("./features/administration/catalog.component").then(
            (m) => m.CatalogComponent,
          ),
      },
      {
        path: "auditoria",
        canActivate: [devAdminGuard],
        loadComponent: () =>
          import("./features/audit/audit.component").then(
            (m) => m.AuditComponent,
          ),
      },
    ],
  },
  { path: "**", redirectTo: "" },
];
