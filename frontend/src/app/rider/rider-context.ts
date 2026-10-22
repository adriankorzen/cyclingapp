import { httpResource } from '@angular/common/http';
import { Injectable, computed, signal } from '@angular/core';

import { Rider } from '../api/models';

const STORAGE_KEY = 'cyclingapp.riderId';

/** The rider the app currently shows. Remembers the selection in the browser. */
@Injectable({ providedIn: 'root' })
export class RiderContext {
  readonly riders = httpResource<Rider[]>(() => '/api/riders', { defaultValue: [] });

  private readonly selectedId = signal<number | null>(readStoredId());

  readonly rider = computed<Rider | null>(() => {
    const riders = this.riders.value();
    return riders.find((rider) => rider.id === this.selectedId()) ?? riders[0] ?? null;
  });

  readonly riderId = computed(() => this.rider()?.id ?? null);

  select(riderId: number): void {
    this.selectedId.set(riderId);
    try {
      localStorage.setItem(STORAGE_KEY, String(riderId));
    } catch {
      // Storage may be unavailable (private mode); the selection then lasts for the session only
    }
  }

  reload(): void {
    this.riders.reload();
  }
}

function readStoredId(): number | null {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? Number(stored) : null;
  } catch {
    return null;
  }
}
