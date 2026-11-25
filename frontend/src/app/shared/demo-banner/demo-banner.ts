import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { LucideClock, LucideLogOut } from '@lucide/angular';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-demo-banner',
  imports: [LucideClock, LucideLogOut],
  templateUrl: './demo-banner.html',
  styleUrl: './demo-banner.scss',
})
export class DemoBanner {
  protected readonly auth = inject(AuthService);
  private readonly now = signal(Date.now());

  protected readonly minutesLeft = computed(() => {
    const user = this.auth.currentUser();
    if (!user?.guest || !user.expiresAt) {
      return null;
    }
    const millisLeft = Date.parse(user.expiresAt) - this.now();
    return Math.max(0, Math.ceil(millisLeft / 60_000));
  });

  constructor() {
    const timer = setInterval(() => {
      this.now.set(Date.now());
      if (this.minutesLeft() === 0) {
        this.auth.logout();
      }
    }, 15_000);
    inject(DestroyRef).onDestroy(() => clearInterval(timer));
  }
}