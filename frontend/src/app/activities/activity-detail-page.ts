import { DatePipe, DecimalPipe, PercentPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ApiService } from '../api/api.service';
import { Activity, ActivityAnalysisResponse } from '../api/models';
import { errorMessage, formatDuration, sportLabel } from '../shared/format';
import { ZoneBars } from '../shared/zone-bars';

@Component({
  selector: 'app-activity-detail-page',
  imports: [DatePipe, DecimalPipe, PercentPipe, RouterLink, ZoneBars],
  templateUrl: './activity-detail-page.html',
})
export class ActivityDetailPage {
  private readonly api = inject(ApiService);

  /** Route parameter {@code :id}. */
  readonly id = input.required<string>();

  protected readonly activity = httpResource<Activity>(() => `/api/activities/${this.id()}`);
  protected readonly analysis = httpResource<ActivityAnalysisResponse>(
    () => `/api/activities/${this.id()}/analysis`,
  );

  protected readonly reanalyzing = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly duration = formatDuration;
  protected readonly sportLabel = sportLabel;

  protected reanalyze(): void {
    this.reanalyzing.set(true);
    this.error.set(null);
    this.api.reanalyze(Number(this.id())).subscribe({
      next: (result) => {
        this.reanalyzing.set(false);
        this.analysis.set(result);
      },
      error: (error) => {
        this.reanalyzing.set(false);
        this.error.set(errorMessage(error));
      },
    });
  }
}
