import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  it('renders the navigation and the current rider', async () => {
    const fixture = TestBed.createComponent(App);
    // Let the riders resource send its request (whenStable would wait for the pending request)
    TestBed.tick();

    TestBed.inject(HttpTestingController)
      .expectOne('/api/riders')
      .flush([{ id: 1, name: 'Anna', weightKg: 60, currentThresholds: null }]);
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('nav')?.textContent).toContain('Activities');
    expect(element.querySelector('.rider-name')?.textContent).toContain('Anna');
  });
});
