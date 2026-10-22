import { DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, effect, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../api/api.service';
import { ThresholdEntry, TrainingZone, Zones } from '../api/models';
import { RiderContext } from '../rider/rider-context';
import { errorMessage, isoDate } from '../shared/format';

type Message = { kind: 'success' | 'error'; text: string } | null;

@Component({
  selector: 'app-profile-page',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule],
  templateUrl: './profile-page.html',
})
export class ProfilePage {
  protected readonly riderContext = inject(RiderContext);
  private readonly api = inject(ApiService);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly today = isoDate();

  protected readonly riderForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    weightKg: [70, [Validators.required, Validators.min(20), Validators.max(250)]],
    restingHeartRateBpm: this.fb.control<number | null>(null, [Validators.min(25), Validators.max(120)]),
  });

  protected readonly thresholdForm = this.fb.group({
    effectiveFrom: [this.today, Validators.required],
    ftpWatts: this.fb.control<number | null>(null, [Validators.min(30), Validators.max(2500)]),
    lthrBpm: this.fb.control<number | null>(null, [Validators.min(60), Validators.max(230)]),
    maxHeartRateBpm: this.fb.control<number | null>(null, [Validators.min(80), Validators.max(240)]),
  });

  protected readonly thresholds = httpResource<ThresholdEntry[]>(
    () => {
      const riderId = this.riderContext.riderId();
      return riderId ? `/api/riders/${riderId}/thresholds` : undefined;
    },
    { defaultValue: [] },
  );

  protected readonly zones = httpResource<Zones>(() => {
    const riderId = this.riderContext.riderId();
    return riderId ? `/api/riders/${riderId}/zones` : undefined;
  });

  protected readonly currentThresholdId = computed(
    () => this.riderContext.rider()?.currentThresholds?.id ?? null,
  );

  protected readonly savingRider = signal(false);
  protected readonly riderMessage = signal<Message>(null);
  protected readonly savingThreshold = signal(false);
  protected readonly thresholdMessage = signal<Message>(null);

  constructor() {
    // Fill the form whenever another rider is shown
    effect(() => {
      const rider = this.riderContext.rider();
      if (rider) {
        this.riderForm.reset({
          name: rider.name,
          weightKg: rider.weightKg,
          restingHeartRateBpm: rider.restingHeartRateBpm,
        });
      }
    });
  }

  protected saveRider(): void {
    if (this.riderForm.invalid) {
      this.riderForm.markAllAsTouched();
      return;
    }
    const request = this.riderForm.getRawValue();
    const riderId = this.riderContext.riderId();
    this.savingRider.set(true);
    this.riderMessage.set(null);
    const save = riderId ? this.api.updateRider(riderId, request) : this.api.createRider(request);
    save.subscribe({
      next: (rider) => {
        this.savingRider.set(false);
        this.riderMessage.set({ kind: 'success', text: riderId ? 'Profile saved.' : 'Profile created.' });
        this.riderContext.select(rider.id);
        this.riderContext.reload();
      },
      error: (error) => {
        this.savingRider.set(false);
        this.riderMessage.set({ kind: 'error', text: errorMessage(error) });
      },
    });
  }

  protected addThreshold(): void {
    const riderId = this.riderContext.riderId();
    if (!riderId || this.thresholdForm.invalid) {
      this.thresholdForm.markAllAsTouched();
      return;
    }
    const request = this.thresholdForm.getRawValue();
    this.savingThreshold.set(true);
    this.thresholdMessage.set(null);
    this.api.createThreshold(riderId, request).subscribe({
      next: (entry) => {
        this.savingThreshold.set(false);
        this.thresholdMessage.set({
          kind: 'success',
          text: `Thresholds from ${entry.effectiveFrom} saved. Activities from that day on were recalculated, earlier ones are unchanged.`,
        });
        this.thresholdForm.reset({ effectiveFrom: this.today });
        this.reloadThresholds();
      },
      error: (error) => {
        this.savingThreshold.set(false);
        this.thresholdMessage.set({ kind: 'error', text: errorMessage(error) });
      },
    });
  }

  protected deleteThreshold(entry: ThresholdEntry): void {
    const riderId = this.riderContext.riderId();
    if (!riderId || !confirm(`Delete thresholds valid from ${entry.effectiveFrom}?`)) {
      return;
    }
    this.api.deleteThreshold(riderId, entry.id).subscribe({
      next: () => {
        this.thresholdMessage.set({ kind: 'success', text: 'Entry deleted, affected activities were recalculated.' });
        this.reloadThresholds();
      },
      error: (error) => this.thresholdMessage.set({ kind: 'error', text: errorMessage(error) }),
    });
  }

  protected range(zone: TrainingZone, unit: string): string {
    return zone.max == null ? `≥ ${zone.min} ${unit}` : `${zone.min}–${zone.max} ${unit}`;
  }

  protected percentRange(zone: TrainingZone): string {
    return zone.maxPercent == null ? `> ${zone.minPercent - 1}%` : `${zone.minPercent}–${zone.maxPercent}%`;
  }

  private reloadThresholds(): void {
    this.thresholds.reload();
    this.zones.reload();
    this.riderContext.reload();
  }
}
