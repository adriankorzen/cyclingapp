import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  ActivityAnalysisResponse,
  GarminSyncResult,
  Rider,
  RiderRequest,
  ThresholdEntry,
  ThresholdRequest,
} from './models';

/** Write operations of the backend API. Reads are done with httpResource in the components. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  createRider(request: RiderRequest): Observable<Rider> {
    return this.http.post<Rider>('/api/riders', request);
  }

  updateRider(riderId: number, request: RiderRequest): Observable<Rider> {
    return this.http.put<Rider>(`/api/riders/${riderId}`, request);
  }

  createThreshold(riderId: number, request: ThresholdRequest): Observable<ThresholdEntry> {
    return this.http.post<ThresholdEntry>(`/api/riders/${riderId}/thresholds`, request);
  }

  deleteThreshold(riderId: number, thresholdId: number): Observable<void> {
    return this.http.delete<void>(`/api/riders/${riderId}/thresholds/${thresholdId}`);
  }

  syncGarmin(riderId: number): Observable<GarminSyncResult> {
    return this.http.post<GarminSyncResult>(`/api/riders/${riderId}/garmin/sync`, null);
  }

  reanalyze(activityId: number): Observable<ActivityAnalysisResponse> {
    return this.http.post<ActivityAnalysisResponse>(`/api/activities/${activityId}/analysis`, null);
  }
}
