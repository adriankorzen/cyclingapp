import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { RiderContext } from './rider/rider-context';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
})
export class App {
  protected readonly riderContext = inject(RiderContext);

  protected onRiderChange(event: Event): void {
    this.riderContext.select(Number((event.target as HTMLSelectElement).value));
  }
}
