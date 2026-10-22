import { DecimalPipe } from '@angular/common';
import { Component, computed, input } from '@angular/core';

import { ZoneTime } from '../api/models';
import { formatDuration } from './format';

/** Horizontal bars showing time spent in each training zone. */
@Component({
  selector: 'app-zone-bars',
  imports: [DecimalPipe],
  template: `
    @for (zone of zones(); track zone.number) {
      <div class="row">
        <span class="name">Z{{ zone.number }} {{ zone.name }}</span>
        <span class="track">
          <span class="bar" [style.width.%]="(zone.seconds / maxSeconds()) * 100" [style.background]="color()"></span>
        </span>
        <span class="time num">{{ duration(zone.seconds) }}</span>
        <span class="percent num muted">{{ zone.percent | number: '1.0-0' }}%</span>
      </div>
    } @empty {
      <p class="muted">No data.</p>
    }
  `,
  styles: `
    .row {
      display: grid;
      grid-template-columns: minmax(120px, 11rem) 1fr 4.5rem 3rem;
      align-items: center;
      gap: 10px;
      padding: 3px 0;
      font-size: 0.9rem;
    }
    .name {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
    .track {
      height: 12px;
      background: var(--surface-muted);
      border-radius: 6px;
      overflow: hidden;
    }
    .bar {
      display: block;
      height: 100%;
      border-radius: 6px;
      min-width: 2px;
    }
    .time,
    .percent {
      text-align: right;
    }
  `,
})
export class ZoneBars {
  readonly zones = input.required<ZoneTime[]>();
  readonly color = input('var(--power)');

  protected readonly maxSeconds = computed(() =>
    Math.max(1, ...this.zones().map((zone) => zone.seconds)),
  );

  protected readonly duration = formatDuration;
}
