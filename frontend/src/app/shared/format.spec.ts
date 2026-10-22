import { HttpErrorResponse } from '@angular/common/http';

import { errorMessage, formatDuration, sportLabel } from './format';

describe('formatDuration', () => {
  it('formats hours, minutes and seconds', () => {
    expect(formatDuration(7105.9)).toBe('1:58:26');
    expect(formatDuration(3600)).toBe('1:00:00');
  });

  it('omits hours below one hour', () => {
    expect(formatDuration(65)).toBe('1:05');
    expect(formatDuration(0)).toBe('0:00');
  });

  it('shows a dash for missing values', () => {
    expect(formatDuration(null)).toBe('–');
  });
});

describe('errorMessage', () => {
  it('prefers field validation errors', () => {
    const error = new HttpErrorResponse({
      status: 400,
      error: { detail: 'Request validation failed', errors: { ftpWatts: 'must be greater than or equal to 30' } },
    });
    expect(errorMessage(error)).toBe('ftpWatts: must be greater than or equal to 30');
  });

  it('uses the problem detail', () => {
    const error = new HttpErrorResponse({ status: 502, error: { detail: 'Garmin API request failed' } });
    expect(errorMessage(error)).toBe('Garmin API request failed');
  });

  it('reports an unreachable backend', () => {
    expect(errorMessage(new HttpErrorResponse({ status: 0 }))).toBe('Backend is not reachable');
  });
});

describe('sportLabel', () => {
  it('turns sport keys into labels', () => {
    expect(sportLabel('road_biking')).toBe('Road biking');
    expect(sportLabel(null)).toBe('–');
  });
});
