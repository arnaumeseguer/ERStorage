import { Component, inject } from '@angular/core';
import { LucideWifiOff } from '@lucide/angular';
import { ServerStatusService } from '../../core/server-status.service';

@Component({
  selector: 'app-server-status-banner',
  imports: [LucideWifiOff],
  templateUrl: './server-status-banner.html',
  styleUrl: './server-status-banner.scss',
})
export class ServerStatusBanner {
  protected readonly server = inject(ServerStatusService);
}