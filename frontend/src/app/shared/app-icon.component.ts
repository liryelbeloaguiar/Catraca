import { ChangeDetectionStrategy, Component, Input } from "@angular/core";
import { NgIcon } from "@ng-icons/core";
import {
  lucideArrowRight,
  lucideBell,
  lucideBriefcaseMedical,
  lucideBuilding2,
  lucideCalendarDays,
  lucideCheck,
  lucideCircleAlert,
  lucideClipboardList,
  lucideClock3,
  lucideExternalLink,
  lucideGauge,
  lucideHospital,
  lucideInbox,
  lucideInfo,
  lucideLayers3,
  lucideListTodo,
  lucideLogOut,
  lucideMapPin,
  lucideMenu,
  lucideMonitor,
  lucidePanelTop,
  lucidePlus,
  lucideScrollText,
  lucideSettings2,
  lucideShieldCheck,
  lucideStethoscope,
  lucideTicketCheck,
  lucideUserRound,
  lucideUserRoundCog,
  lucideUsersRound,
  lucideX,
} from "@ng-icons/lucide";

const icons = {
  appointments: lucideCalendarDays,
  arrowRight: lucideArrowRight,
  audit: lucideScrollText,
  bell: lucideBell,
  building: lucideBuilding2,
  check: lucideCheck,
  clock: lucideClock3,
  dashboard: lucideGauge,
  department: lucideLayers3,
  employee: lucideBriefcaseMedical,
  externalLink: lucideExternalLink,
  hospital: lucideHospital,
  inbox: lucideInbox,
  info: lucideInfo,
  list: lucideListTodo,
  logout: lucideLogOut,
  mapPin: lucideMapPin,
  menu: lucideMenu,
  monitor: lucideMonitor,
  panel: lucidePanelTop,
  plus: lucidePlus,
  queue: lucideClipboardList,
  settings: lucideSettings2,
  shield: lucideShieldCheck,
  specialty: lucideStethoscope,
  ticket: lucideTicketCheck,
  user: lucideUserRound,
  users: lucideUsersRound,
  userSettings: lucideUserRoundCog,
  warning: lucideCircleAlert,
  x: lucideX,
} as const;

export type AppIconName = keyof typeof icons;

@Component({
  selector: "app-icon",
  imports: [NgIcon],
  template: '<ng-icon [svg]="icons[name]" aria-hidden="true" />',
  styles: `:host { display: inline-flex; align-items: center; justify-content: center; line-height: 0; }`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppIconComponent {
  @Input({ required: true }) name!: AppIconName;
  protected readonly icons = icons;
}
