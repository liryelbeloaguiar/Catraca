import { ChangeDetectionStrategy, Component, signal } from "@angular/core";
import {
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from "@angular/forms";
import { Router, RouterLink } from "@angular/router";
import { finalize } from "rxjs";
import { AuthService } from "../../core/auth.service";
import { errorMessage } from "../../shared/feedback";

@Component({
  selector: "app-login",
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: "./login.component.html",
  styleUrl: "./login.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginComponent {
  readonly loading = signal(false);
  readonly message = signal("");
  readonly form = new FormGroup({
    email: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required, Validators.email],
    }),
    password: new FormControl("", {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });
  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
  ) {}
  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.message.set("");
    const { email, password } = this.form.getRawValue();
    this.auth
      .login(email, password)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: () => void this.router.navigateByUrl("/painel"),
        error: (error) => this.message.set(errorMessage(error)),
      });
  }
}
