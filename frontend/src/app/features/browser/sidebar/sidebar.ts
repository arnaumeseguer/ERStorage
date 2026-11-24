import { Component, computed, inject } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { LucideFolder, LucideLogOut } from '@lucide/angular';
import { AuthService } from '../../../core/auth.service';
import { FileSizePipe } from '../../../shared/file-size-pipe';

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, FileSizePipe, LucideFolder, LucideLogOut],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
})
export class Sidebar {
  protected readonly auth = inject(AuthService);
  protected readonly user = this.auth.currentUser;

  protected readonly usedPercent = computed(() => {
    const user = this.user();
    if (!user || user.storageQuotaBytes === 0) {
      return 0;
    }
    return Math.min(100, Math.round((user.storageUsedBytes / user.storageQuotaBytes) * 100));
  });

  protected readonly nearlyFull = computed(() => this.usedPercent() >= 90);

  constructor() {
    if (!this.user()) {
      this.auth.loadCurrentUser().subscribe();
    }
  }
}