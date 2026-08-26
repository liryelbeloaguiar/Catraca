import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, catchError, finalize, of, shareReplay, tap } from 'rxjs';
import { AuthResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionState = signal<AuthResponse | null>(null);
  private refreshInFlight: Observable<AuthResponse> | null = null;

  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.sessionState() !== null);

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/login', { email, password }, { withCredentials: true })
      .pipe(tap((session) => this.store(session)));
  }

  register(request: { fullName: string; email: string; password: string; document: string; birthDate: string; phone: string }): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>('/api/v1/auth/register', request, { withCredentials: true })
      .pipe(tap((session) => this.store(session)));
  }

  refresh(): Observable<AuthResponse> {
    if (!this.refreshInFlight) {
      this.refreshInFlight = this.http
        .post<AuthResponse>('/api/v1/auth/refresh', {}, { withCredentials: true })
        .pipe(
          tap((session) => this.store(session)),
          finalize(() => {
            this.refreshInFlight = null;
          }),
          shareReplay({ bufferSize: 1, refCount: false }),
        );
    }
    return this.refreshInFlight;
  }

  ensureSession(): Observable<AuthResponse | null> {
    const current = this.sessionState();
    return current ? of(current) : this.refresh().pipe(catchError(() => of(null)));
  }

  logout(): void {
    this.http.post<void>('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({ error: () => undefined });
    this.clearSession();
    void this.router.navigateByUrl('/login');
  }

  clearSession(): void {
    this.sessionState.set(null);
  }

  token(): string | null {
    return this.sessionState()?.accessToken ?? null;
  }

  has(authority: string): boolean {
    return this.sessionState()?.authorities.includes(authority) ?? false;
  }

  updateFullName(fullName: string): void {
    const session = this.sessionState();
    if (session) this.store({ ...session, fullName });
  }

  private store(session: AuthResponse): void {
    this.sessionState.set(session);
  }
}
