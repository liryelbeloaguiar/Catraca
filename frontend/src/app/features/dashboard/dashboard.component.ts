import { ChangeDetectionStrategy, Component } from "@angular/core";
import { RouterLink } from "@angular/router";
import { AuthService } from "../../core/auth.service";
import { AppIconComponent } from "../../shared/app-icon.component";

@Component({
  selector: "app-dashboard",
  imports: [RouterLink, AppIconComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: ` <section class="page-head">
      <div>
        <span class="eyebrow">VISÃO GERAL</span>
        <h1>Olá, {{ firstName() }}.</h1>
        <p>Acompanhe sua operação e acesse as tarefas principais.</p>
      </div>
      <span class="date-chip">{{ today }}</span>
    </section>
    <section class="hero-card">
      <div>
        <span class="eyebrow light">CATRACA</span>
        <h2>
          Atendimentos mais fluidos começam com uma operação bem organizada.
        </h2>
        <p>
          Use o menu para gerenciar cadastros, agendamentos e filas em tempo
          real.
        </p>
      </div>
      <div class="hero-art"><span></span><span></span><span></span></div>
    </section>
    <h3 class="section-title">Acesso rápido</h3>
    <section class="quick-grid">
      @if (auth.has("APPOINTMENT_READ")) {
        <a class="quick-card" routerLink="/agendamentos"
          ><span class="quick-icon green"><app-icon name="appointments" /></span>
          <div>
            <strong>Agendamentos</strong>
            <p>Consulte e acompanhe os horários.</p>
          </div>
          <b><app-icon name="arrowRight" /></b></a
        >
      }
      @if (auth.has("QUEUE_READ")) {
        <a class="quick-card" routerLink="/filas"
          ><span class="quick-icon orange"><app-icon name="queue" /></span>
          <div>
            <strong>Filas e fichas</strong>
            <p>Opere chamadas e acompanhe a espera.</p>
          </div>
          <b><app-icon name="arrowRight" /></b></a
        >
      }
      @if (auth.has("ADMINISTRATION_MANAGE")) {
        <a class="quick-card" routerLink="/administracao/units"
          ><span class="quick-icon blue"><app-icon name="building" /></span>
          <div>
            <strong>Cadastros</strong>
            <p>Configure unidades e recursos.</p>
          </div>
          <b><app-icon name="arrowRight" /></b></a
        >
      }
      @if (auth.has("ROLE_DEV_ADMIN")) {
        <a class="quick-card" routerLink="/auditoria"
          ><span class="quick-icon purple"><app-icon name="audit" /></span>
          <div>
            <strong>Auditoria</strong>
            <p>Consulte as ações realizadas.</p>
          </div>
          <b><app-icon name="arrowRight" /></b></a
        >
      }
    </section>
})
export class DashboardComponent {
  readonly today = new Intl.DateTimeFormat("pt-BR", {
    weekday: "long",
    day: "2-digit",
    month: "long",
  }).format(new Date());
  constructor(readonly auth: AuthService) {}
  firstName(): string {
    return this.auth.session()?.fullName.split(" ")[0] ?? "";
  }
}
