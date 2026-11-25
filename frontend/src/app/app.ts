import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ServerStatusService } from './core/server-status.service';
import { DemoBanner } from './shared/demo-banner/demo-banner';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, DemoBanner],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  constructor() {
    inject(ServerStatusService).check();
  }
}