import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { AuthService } from '../../core/auth.service';
import { errorMessage } from '../../shared/feedback';

@Component({
  selector: 'app-register', imports: [ReactiveFormsModule, RouterLink], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main class="auth-page register-page">
      <section class="auth-brand">
        <a class="brand brand-light" href="/"><span class="brand-mark">C</span><span>Catraca</span></a>
        <div class="brand-message"><span class="eyebrow light">NOVO PACIENTE</span><h1>Seu atendimento<br><em>começa por aqui.</em></h1><p>Crie sua conta para acompanhar agendamentos e sua jornada de atendimento.</p></div>
      </section>
      <section class="auth-card-wrap">
        <form class="auth-card register-card" [formGroup]="form" (ngSubmit)="submit()">
          <div><span class="eyebrow">CADASTRO</span><h2>Crie sua conta</h2><p>Preencha seus dados pessoais.</p></div>
          @if (message()) { <div class="alert error">{{ message() }}</div> }
          <div class="form-grid">
            <label class="full">Nome completo<input formControlName="fullName" autocomplete="name"></label>
            <label>E-mail<input type="email" formControlName="email" autocomplete="email"></label>
            <label>CPF / documento<input formControlName="document"></label>
            <label>Data de nascimento<input type="date" formControlName="birthDate"></label>
            <label>Telefone<input formControlName="phone" autocomplete="tel"></label>
            <label class="full">Senha<input type="password" formControlName="password" autocomplete="new-password"><small>Mínimo de 8 caracteres.</small></label>
          </div>
          <button class="button primary wide" type="submit" [disabled]="form.invalid || loading()">{{ loading() ? 'Cadastrando…' : 'Criar conta' }}</button>
          <p class="auth-link">Já possui uma conta? <a routerLink="/login">Entrar</a></p>
        </form>
      </section>
    </main>`
})
export class RegisterComponent {
  readonly loading = signal(false); readonly message = signal('');
  readonly form = new FormGroup({
    fullName: new FormControl('', { nonNullable: true, validators: Validators.required }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    document: new FormControl('', { nonNullable: true, validators: Validators.required }),
    birthDate: new FormControl('', { nonNullable: true, validators: Validators.required }),
    phone: new FormControl('', { nonNullable: true }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8)] })
  });
  constructor(private readonly auth: AuthService, private readonly router: Router) {}
  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true); this.message.set('');
    this.auth.register(this.form.getRawValue()).pipe(finalize(() => this.loading.set(false))).subscribe({
      next: () => void this.router.navigateByUrl('/painel'), error: error => this.message.set(errorMessage(error))
    });
  }
}
