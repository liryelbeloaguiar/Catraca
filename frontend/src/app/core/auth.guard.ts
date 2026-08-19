import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "./auth.service";

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.authenticated() || inject(Router).createUrlTree(["/login"]);
};

export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return !auth.authenticated() || inject(Router).createUrlTree(["/painel"]);
};

export const devAdminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return (
    auth.has("ROLE_DEV_ADMIN") || inject(Router).createUrlTree(["/painel"])
  );
};
