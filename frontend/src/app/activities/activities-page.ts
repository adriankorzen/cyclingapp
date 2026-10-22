import { DatePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';

import { ApiService } from '../api/api.service';
import { Activity, AnalysisSummary } from '../api/models';
import { RiderContext } from '../rider/rider-context';
import { errorMessage, formatDuration, isoDate, sportLabel } from '../shared/format';

interface ActivityRow {
  activity: Activity;
  summary: AnalysisSummary | null;
}

@Component({
  selector: 'app-activities-page',
  imports: [DatePipe, DecimalPipe, RouterLink],
  templateUrl: './activities-page.html',
})
export class ActivitiesPage {
  protected readonly riderContext = inject(RiderContext);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  protected readonly from = signal(isoDate(-90));
  protected readonly to = signal(isoDate());

  protected readonly activities = httpResource<Activity[]>(
    () => {
      const riderId = this.riderContext.riderId();
      return riderId ? `/api/riders/${riderId}/activities` : undefined;
    },
    { defaultValue: [] },
  );

  protected readonly summaries = httpResource<AnalysisSummary[]>(
    () => {
      const riderId = this.riderContext.riderId();
      return riderId
        ? { url: `/api/riders/${riderId}/analyses`, params: { from: this.from(), to: this.to() } }
        : undefined;
    },
    { defaultValue: [] },
  );

  /** Activities within the selected period joined with their analysis (if any), newest first. */
  protected readonly rows = computed<ActivityRow[]>(() => {
    const summaries = new Map(this.summaries.value().map((summary) => [summary.activityId, summary]));
    const from = this.from();
    const to = this.to();
    return this.activities
      .value()
      .filter((activity) => {
        const day = (activity.startTimeLocal ?? activity.startTime).slice(0, 10);
        return day >= from && day <= to;
      })
      .map((activity) => ({ activity, summary: summaries.get(activity.id) ?? null }));
  });

  protected readonly totals = computed(() =>
    this.rows().reduce(
      (totals, { activity, summary }) => ({
        rides: totals.rides + 1,
        seconds: totals.seconds + (summary?.durationSeconds ?? activity.durationSeconds ?? 0),
        meters: totals.meters + (activity.distanceMeters ?? 0),
        tss: totals.tss + (summary?.trainingStressScore ?? 0),
      }),
      { rides: 0, seconds: 0, meters: 0, tss: 0 },
    ),
  );

  protected readonly syncing = signal(false);
  protected readonly message = signal<{ kind: 'success' | 'error'; text: string } | null>(null);

  protected readonly duration = formatDuration;
  protected readonly sportLabel = sportLabel;

  protected setFrom(event: Event): void {
    this.from.set((event.target as HTMLInputElement).value);
  }

  protected setTo(event: Event): void {
    this.to.set((event.target as HTMLInputElement).value);
  }

  protected sync(): void {
    const riderId = this.riderContext.riderId();
    if (!riderId) {
      return;
    }
    this.syncing.set(true);
    this.message.set(null);
    this.api.syncGarmin(riderId).subscribe({
      next: (result) => {
        this.syncing.set(false);
        this.message.set({
          kind: 'success',
          text:
            result.imported > 0
              ? `Imported ${result.imported} new activities (since ${result.since}).`
              : `No new activities since ${result.since}.`,
        });
        this.activities.reload();
        this.summaries.reload();
      },
      error: (error) => {
        this.syncing.set(false);
        this.message.set({ kind: 'error', text: `Garmin sync failed: ${errorMessage(error)}` });
      },
    });
  }

  protected open(activityId: number): void {
    this.router.navigate(['/activities', activityId]);
  }
}
