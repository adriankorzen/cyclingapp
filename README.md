# CyclingApp

Cycling training application.

## Repository layout

| Directory   | Description                                   |
|-------------|-----------------------------------------------|
| `backend/`  | REST API — Java 25, Spring Boot 4, Maven      |
| `frontend/` | Web client — Angular 22                       |

## Frontend

### Requirements

- Node.js 24 LTS

### Running locally

```bash
cd frontend
npm install
npm start
```

Open <http://localhost:4200>. API calls to `/api` are proxied to the backend on port 8080 (`proxy.conf.json`),
so start the backend first.

| Page | Content |
|------|---------|
| Activities | training log for a date range with totals, NP, IF, TSS and intervals per ride; Garmin sync |
| Activity detail | metrics, time in power and heart rate zones, detected intervals, re-analysis, FIT download |
| Profile | rider data, threshold history (e.g. new FTP test) and current Coggan zones |

```bash
npm test        # unit tests (Vitest)
npm run build   # production build into dist/
```

## Backend

### Requirements

- JDK 25
- Docker (local PostgreSQL and integration tests)

### Running locally

```bash
cd backend
./mvnw spring-boot:run
```

PostgreSQL is started automatically from `backend/compose.yaml` via Spring Boot Docker Compose support.
Health check: <http://localhost:8080/actuator/health>

### Tests

```bash
cd backend
./mvnw test
```

Integration tests start a throwaway PostgreSQL container with Testcontainers.

### Database migrations

Schema changes are Flyway migrations in `backend/src/main/resources/db/migration`
(`V<version>__<description>.sql`). Hibernate only validates the schema.

### Garmin Connect integration

Activities are imported through the [Garmin Connect Personal API](https://developer.garmin.com/gc-developer-program/personal/personal-api/)
(personal, non-commercial use of your own data).

1. Create a personal API key in Garmin Connect Web: Settings → Personal API Key.
2. Expose it to the backend as the `GARMIN_API_KEY` environment variable.
3. Sync cycling activities of a rider:

```bash
curl -X POST "http://localhost:8080/api/riders/1/garmin/sync"                  # since the latest imported activity
curl -X POST "http://localhost:8080/api/riders/1/garmin/sync?since=2026-01-01" # from a given date
```

The first sync reaches back `garmin.personal-api.initial-sync-days` (30 by default). Re-running a sync is safe,
already imported activities are skipped. The original FIT file of each activity is stored and available at
`GET /api/activities/{id}/file`.

### Threshold history

FTP, LTHR and max HR are kept as a history (`/api/riders/{id}/thresholds`): every entry is valid from its
`effectiveFrom` date until the next one. Values omitted when adding an entry are carried over, so a new FTP
test only needs `{"effectiveFrom": "2026-10-22", "ftpWatts": 290}`.

Activities are always analysed with the thresholds valid on the day of the ride (rides before the first entry
use the oldest one). Adding, changing or deleting an entry recalculates only the activities whose thresholds
actually changed — a new FTP never changes TSS of earlier rides.

### Activity analysis

Every imported activity is analysed right away from its FIT file, relative to the rider's thresholds valid on
the day of the ride, and the result is stored:

- Coggan power metrics: average power, NP, VI, IF, TSS, work (kJ)
- time in Coggan power and heart rate zones
- automatic detection of structured work blocks (interval sets), per-block power and HR,
  VO2max stimulus time (>= 92% of max HR) and cardiac drift

| Endpoint | Description |
|----------|-------------|
| `GET /api/activities/{id}/analysis` | stored analysis with the thresholds it was calculated with |
| `GET /api/activities/{id}/analysis?targetWatts=…&relativeThreshold=…&minDuration=…&maxGap=…` | ad hoc analysis with custom block detection settings, not stored |
| `POST /api/activities/{id}/analysis` | recalculate and store |
| `POST /api/riders/{id}/analyses/refresh[?all=true]` | analyse activities without an up to date analysis (missing, older analyzer version or other thresholds); `all=true` recalculates everything |
| `GET /api/riders/{id}/analyses?from=YYYY-MM-DD&to=YYYY-MM-DD` | key metrics (TSS, IF, NP, ...) of analysed activities, last 90 days by default |

Stored analyses carry the analyzer version (`ActivityAnalyzer.VERSION`); bump it when the algorithm changes
and run a refresh to recalculate outdated results.

### Production profile

Run with `SPRING_PROFILES_ACTIVE=prod` and provide `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
