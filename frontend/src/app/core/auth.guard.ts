import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "./auth.service";
import { map } from "rxjs";

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.ensureSession().pipe(
    map((session) => session !== null || router.createUrlTree(["/login"])),
  );
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.ensureSession().pipe(
    map((session) => session === null || router.createUrlTree(["/painel"])),
  );
};

export const devAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return (
    auth.has("ROLE_DEV_ADMIN") || inject(Router).createUrlTree(["/painel"])
  );
};
