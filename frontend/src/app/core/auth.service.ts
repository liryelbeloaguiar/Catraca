import { HttpClient } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { AuthResponse } from './models';

const SESSION_KEY = 'catraca.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly sessionState = signal<AuthResponse | null>(this.restore());
  readonly session = this.sessionState.asReadonly();
  readonly authenticated = computed(() => this.sessionState() !== null);

  constructor(private readonly http: HttpClient, private readonly router: Router) {}

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/login', { email, password }, { withCredentials: true })
      .pipe(tap(session => this.store(session)));
  }

  register(request: { fullName: string; email: string; password: string; document: string; birthDate: string; phone: string }): Observable<AuthResponse> {
    return this.http.post<AuthResponse>('/api/v1/auth/register', request, { withCredentials: true })
      .pipe(tap(session => this.store(session)));
  }

  logout(): void {
    this.http.post<void>('/api/v1/auth/logout', {}, { withCredentials: true }).subscribe({ error: () => undefined });
    sessionStorage.removeItem(SESSION_KEY);
    this.sessionState.set(null);
    void this.router.navigateByUrl('/login');
  }

  token(): string | null { return this.sessionState()?.accessToken ?? null; }
  has(authority: string): boolean { return this.sessionState()?.authorities.includes(authority) ?? false; }
  updateFullName(fullName: string): void {
    const session = this.sessionState();
    if (session) this.store({ ...session, fullName });
  }

  private store(session: AuthResponse): void {
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    this.sessionState.set(session);
  }

  private restore(): AuthResponse | null {
    const serialized = sessionStorage.getItem(SESSION_KEY);
    if (!serialized) return null;
    try { return JSON.parse(serialized) as AuthResponse; }
    catch { sessionStorage.removeItem(SESSION_KEY); return null; }
  }
}
