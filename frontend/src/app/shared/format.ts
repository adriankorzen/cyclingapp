import { HttpErrorResponse } from '@angular/common/http';

import { ProblemDetail } from '../api/models';

/** Formats seconds as h:mm:ss (or m:ss below one hour). */
export function formatDuration(totalSeconds: number | null | undefined): string {
  if (totalSeconds == null || Number.isNaN(totalSeconds)) {
    return '–';
  }
  const seconds = Math.round(totalSeconds);
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = seconds % 60;
  const mm = h > 0 ? String(m).padStart(2, '0') : String(m);
  return `${h > 0 ? `${h}:` : ''}${mm}:${String(s).padStart(2, '0')}`;
}

/** Today (or a shifted day) in the browser's time zone as YYYY-MM-DD. */
export function isoDate(offsetDays = 0): string {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

/** Human readable message of a failed API call. */
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | null;
    if (problem?.errors) {
      return Object.entries(problem.errors)
        .map(([field, message]) => `${field}: ${message}`)
        .join(', ');
    }
    if (problem?.detail) {
      return problem.detail;
    }
    if (error.status === 0) {
      return 'Backend is not reachable';
    }
    return `Request failed (${error.status})`;
  }
  return 'Unexpected error';
}

/** Turns a sport type key like "road_biking" into "Road biking". */
export function sportLabel(sportType: string | null | undefined): string {
  if (!sportType) {
    return '–';
  }
  const words = sportType.replaceAll('_', ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}
