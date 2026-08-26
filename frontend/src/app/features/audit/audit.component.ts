import { DatePipe } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { finalize } from 'rxjs';
import { ApiRecord } from '../../core/models';
import { errorMessage } from '../../shared/feedback';
import { AppIconComponent } from '../../shared/app-icon.component';

@Component({ selector: 'app-audit', imports: [DatePipe, AppIconComponent], templateUrl: './audit.component.html', styleUrl: './audit.component.scss', changeDetection: ChangeDetectionStrategy.OnPush })
export class AuditComponent {
  readonly rows = signal<ApiRecord[]>([]); readonly loading = signal(true); readonly message = signal('');
  constructor(http: HttpClient) { http.get<ApiRecord[]>('/api/v1/audit-logs').pipe(finalize(() => this.loading.set(false))).subscribe({ next: rows => this.rows.set(rows), error: error => this.message.set(errorMessage(error)) }); }
  asDate(value: ApiRecord[string]): string | null { return typeof value === 'string' ? value : null; }
}
