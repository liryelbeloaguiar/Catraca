import { HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, effect, input, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { finalize } from 'rxjs';
import { ApiRecord } from '../../core/models';
import { errorMessage } from '../../shared/feedback';
import { AppIconComponent } from '../../shared/app-icon.component';

const TITLES: Record<string, string> = {
  units: 'Unidades', services: 'Serviços', specialties: 'Especialidades', priorities: 'Prioridades', rooms: 'Salas', counters: 'Guichês', queues: 'Filas', departments: 'Setores', 'professional-types': 'Tipos profissionais'
};

@Component({
  selector: 'app-catalog', imports: [ReactiveFormsModule, AppIconComponent], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="page-head"><div><span class="eyebrow">ADMINISTRAÇÃO</span><h1>{{ title() }}</h1><p>Cadastre e mantenha os recursos usados na operação.</p></div><button class="button primary" (click)="openCreate()">{{ showForm() ? 'Fechar' : '+ Novo cadastro' }}</button></section>
    @if (showForm()) {
      <form class="panel form-panel" [formGroup]="form" (ngSubmit)="save()">
        <div class="panel-head"><div><h2>{{ editingId() ? 'Editar cadastro' : 'Novo cadastro' }}</h2><p>Revise os dados antes de salvar.</p></div></div>
        @if (message()) { <div class="alert error">{{ message() }}</div> }
        <div class="form-grid three">
          <label>Código<input formControlName="code" placeholder="Ex.: UNI-01"></label>
          <label class="span-two">Nome<input formControlName="name" placeholder="Nome do cadastro"></label>
          <label class="full">Descrição / endereço / andar<input formControlName="description"></label>
          @if (needsUnit()) { <label class="span-two">ID da unidade<input formControlName="unitId" placeholder="UUID da unidade"></label> }
          @if (resource() === 'services') { <label>Duração (minutos)<input type="number" formControlName="durationMinutes"></label> }
          @if (resource() === 'priorities') { <label>Peso<input type="number" formControlName="weight"></label><label>Ordem de exibição<input type="number" formControlName="displayOrder"></label> }
          @if (resource() === 'queues') { <label>Tolerância (minutos)<input type="number" formControlName="gracePeriodMinutes"></label><label>Ausência após (minutos)<input type="number" formControlName="noShowAfterMinutes"></label> }
        </div>
        <div class="form-actions"><button type="button" class="button ghost" (click)="cancel()">Cancelar</button><button class="button primary" type="submit" [disabled]="form.invalid || saving()">{{ saving() ? 'Salvando…' : editingId() ? 'Salvar alterações' : 'Salvar cadastro' }}</button></div>
      </form>
    }
    <section class="panel">
      <div class="panel-head"><div><h2>Cadastros existentes</h2><p>{{ rows().length }} registro(s) encontrado(s)</p></div><input class="search" [formControl]="search" placeholder="Buscar por nome"></div>
      @if (loading()) { <div class="empty">Carregando…</div> }
      @else if (message() && !showForm()) { <div class="alert error space">{{ message() }}</div> }
      @else if (!rows().length) { <div class="empty"><app-icon name="building" /><h3>Nenhum cadastro encontrado</h3><p>Use “Novo cadastro” para inserir o primeiro registro.</p></div> }
      @else {
        <div class="table-wrap"><table><thead><tr><th>Código</th><th>Nome</th><th>Descrição</th><th>Status</th><th></th></tr></thead><tbody>
          @for (row of rows(); track row['id']) { <tr><td><code>{{ row['code'] || row['ticket_prefix'] || '—' }}</code></td><td><strong>{{ row['name'] }}</strong></td><td>{{ row['description'] || row['address'] || row['floor'] || '—' }}</td><td><span class="badge" [class.inactive]="!row['active']">{{ row['active'] ? 'Ativo' : 'Inativo' }}</span></td><td><button class="link-button" (click)="edit(row)">Editar</button> <button class="link-button" (click)="toggle(row)">{{ row['active'] ? 'Desativar' : 'Ativar' }}</button></td></tr> }
        </tbody></table></div>
      }
    </section>`
})
export class CatalogComponent {
  readonly resource = input.required<string>(); readonly rows = signal<ApiRecord[]>([]); readonly loading = signal(false); readonly saving = signal(false); readonly showForm = signal(false); readonly editingId = signal<string | null>(null); readonly message = signal('');
  readonly search = new FormControl('', { nonNullable: true });
  readonly form = new FormGroup({
    code: new FormControl('', { nonNullable: true }), name: new FormControl('', { nonNullable: true, validators: Validators.required }), description: new FormControl('', { nonNullable: true }), unitId: new FormControl('', { nonNullable: true }), durationMinutes: new FormControl(30, { nonNullable: true }), weight: new FormControl(0, { nonNullable: true }), displayOrder: new FormControl(0, { nonNullable: true }), gracePeriodMinutes: new FormControl(0, { nonNullable: true }), noShowAfterMinutes: new FormControl(0, { nonNullable: true })
  });
  constructor(private readonly http: HttpClient) {
    effect(() => { this.resource(); this.cancel(); this.load(); });
    this.search.valueChanges.subscribe(() => this.load());
  }
  title(): string { return TITLES[this.resource()] ?? 'Cadastros'; }
  needsUnit(): boolean { return ['rooms', 'counters', 'queues', 'departments'].includes(this.resource()); }
  load(): void { this.loading.set(true); this.message.set(''); this.http.get<ApiRecord[]>(`/api/v1/${this.resource()}`, { params: { search: this.search.value } }).pipe(finalize(() => this.loading.set(false))).subscribe({ next: rows => this.rows.set(rows), error: error => this.message.set(errorMessage(error)) }); }
  openCreate(): void { if (this.showForm() && !this.editingId()) { this.cancel(); return; } this.resetForm(); this.showForm.set(true); }
  edit(row: ApiRecord): void {
    this.editingId.set(String(row['id']));
    this.form.reset({ code: String(row['code'] || row['ticket_prefix'] || ''), name: String(row['name'] || ''), description: String(row['description'] || row['address'] || row['floor'] || ''), unitId: String(row['unit_id'] || ''), durationMinutes: Number(row['default_duration_minutes'] ?? 30), weight: Number(row['weight'] ?? 0), displayOrder: Number(row['display_order'] ?? 0), gracePeriodMinutes: Number(row['grace_period_minutes'] ?? 0), noShowAfterMinutes: Number(row['no_show_after_minutes'] ?? 0) });
    this.message.set(''); this.showForm.set(true); window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  cancel(): void { this.resetForm(); this.showForm.set(false); }
  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true); this.message.set('');
    const id = this.editingId(); const request = id ? this.http.put(`/api/v1/${this.resource()}/${id}`, this.form.getRawValue()) : this.http.post(`/api/v1/${this.resource()}`, this.form.getRawValue());
    request.pipe(finalize(() => this.saving.set(false))).subscribe({ next: () => { this.cancel(); this.load(); }, error: error => this.message.set(errorMessage(error)) });
  }
  private resetForm(): void { this.editingId.set(null); this.form.reset({ code: '', name: '', description: '', unitId: '', durationMinutes: 30, weight: 0, displayOrder: 0, gracePeriodMinutes: 0, noShowAfterMinutes: 0 }); }
  toggle(row: ApiRecord): void { this.http.patch(`/api/v1/${this.resource()}/${row['id']}/active`, { active: !row['active'] }).subscribe({ next: () => this.load(), error: error => this.message.set(errorMessage(error)) }); }
}
