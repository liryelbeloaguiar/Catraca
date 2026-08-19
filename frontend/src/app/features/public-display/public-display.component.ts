import { DatePipe } from "@angular/common";
import { HttpClient } from "@angular/common/http";
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  signal,
} from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { finalize } from "rxjs";

interface PublicCall {
  displayCode: string;
  status: string;
  calledAt: string;
  queueName: string;
  roomName?: string;
  floor?: string;
  counterName?: string;
}

interface PublicPanel {
  name: string;
  unitName: string;
  floor?: string;
  audioEnabled: boolean;
  voiceEnabled: boolean;
  serverTime: string;
  calls: PublicCall[];
}

@Component({
  selector: "app-public-display",
  imports: [DatePipe],
  templateUrl: "./public-display.component.html",
  styleUrl: "./public-display.component.scss",
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PublicDisplayComponent implements OnDestroy {
  readonly panel = signal<PublicPanel | null>(null);
  readonly now = signal(new Date());
  readonly loading = signal(true);
  readonly offline = signal(false);
  readonly soundUnlocked = signal(false);
  private readonly token: string;
  private readonly clockId: number;
  private readonly events: EventSource;
  private lastCallKey = "";

  constructor(
    route: ActivatedRoute,
    private readonly http: HttpClient,
  ) {
    sessionStorage.removeItem("catraca.session");
    document.body.classList.add("public-display-mode");
    this.token = route.snapshot.paramMap.get("token") ?? "";
    this.load();
    this.events = new EventSource(
      `/api/v1/public/display-panels/${this.token}/events`,
    );
    this.events.addEventListener("refresh", () => this.load());
    this.events.onopen = () => this.offline.set(false);
    this.events.onerror = () => this.offline.set(true);
    this.clockId = window.setInterval(() => this.now.set(new Date()), 1000);
  }

  currentCall(): PublicCall | null {
    return this.panel()?.calls[0] ?? null;
  }

  recentCalls(): PublicCall[] {
    return this.panel()?.calls.slice(1) ?? [];
  }

  destination(call: PublicCall): string {
    return call.counterName || call.roomName || "Aguarde orientação";
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      CALLED: "Dirija-se ao local indicado",
      IN_SERVICE: "Em atendimento",
      COMPLETED: "Atendimento concluído",
    };
    return labels[status] ?? "Chamada";
  }

  enableSound(): void {
    this.soundUnlocked.set(true);
    this.beep();
  }

  fullscreen(): void {
    if (!document.fullscreenElement) {
      void document.documentElement.requestFullscreen();
    } else {
      void document.exitFullscreen();
    }
  }

  ngOnDestroy(): void {
    this.events.close();
    window.clearInterval(this.clockId);
    speechSynthesis.cancel();
    document.body.classList.remove("public-display-mode");
  }

  private load(): void {
    this.http
      .get<PublicPanel>(`/api/v1/public/display-panels/${this.token}`)
      .pipe(finalize(() => this.loading.set(false)))
      .subscribe({
        next: (panel) => {
          this.offline.set(false);
          this.panel.set(panel);
          const current = panel.calls[0];
          const key = current
            ? `${current.displayCode}:${current.calledAt}`
            : "";
          if (this.lastCallKey && key && key !== this.lastCallKey) {
            this.announce(current, panel);
          }
          this.lastCallKey = key;
        },
        error: () => this.offline.set(true),
      });
  }

  private announce(call: PublicCall, panel: PublicPanel): void {
    if (!this.soundUnlocked() || !panel.audioEnabled) return;
    this.beep();
    if (panel.voiceEnabled && "speechSynthesis" in window) {
      const destination = this.destination(call);
      const message = new SpeechSynthesisUtterance(
        `Ficha ${call.displayCode.replace("-", " ")}. Dirija-se a ${destination}.`,
      );
      message.lang = "pt-BR";
      message.rate = 0.82;
      window.setTimeout(() => speechSynthesis.speak(message), 650);
    }
  }

  private beep(): void {
    try {
      const context = new AudioContext();
      const oscillator = context.createOscillator();
      const gain = context.createGain();
      oscillator.frequency.value = 720;
      gain.gain.setValueAtTime(0.001, context.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.18, context.currentTime + 0.03);
      gain.gain.exponentialRampToValueAtTime(0.001, context.currentTime + 0.35);
      oscillator.connect(gain).connect(context.destination);
      oscillator.start();
      oscillator.stop(context.currentTime + 0.36);
    } catch {}
  }
}
