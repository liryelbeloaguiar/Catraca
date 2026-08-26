import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { AppIconComponent, AppIconName } from "../app-icon.component";

@Component({
  selector: "app-empty-state",
  imports: [AppIconComponent],
  template: `<div class="empty-state"><app-icon [name]="icon" /><h3>{{ title }}</h3>@if (description) { <p>{{ description }}</p> }<ng-content /></div>`,
  styles: `.empty-state{display:grid;justify-items:center;gap:8px;padding:34px;text-align:center;color:var(--color-muted)} app-icon{font-size:30px;color:var(--color-primary-600)} h3,p{margin:0} h3{color:var(--color-text)}`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmptyStateComponent {
  @Input() icon: AppIconName = "inbox";
  @Input({ required: true }) title = "";
  @Input() description = "";
}
