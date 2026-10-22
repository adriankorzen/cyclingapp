// Shapes of the backend REST API (see backend DTOs). Dates are ISO strings.

export interface ThresholdEntry {
  id: number;
  effectiveFrom: string;
  ftpWatts: number | null;
  lthrBpm: number | null;
  maxHeartRateBpm: number | null;
  createdAt: string;
}

export interface ThresholdRequest {
  effectiveFrom: string | null;
  ftpWatts: number | null;
  lthrBpm: number | null;
  maxHeartRateBpm: number | null;
}

export interface Rider {
  id: number;
  name: string;
  weightKg: number;
  restingHeartRateBpm: number | null;
  currentThresholds: ThresholdEntry | null;
  ftpWattsPerKg: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface RiderRequest {
  name: string;
  weightKg: number;
  restingHeartRateBpm: number | null;
}

export interface TrainingZone {
  number: number;
  name: string;
  minPercent: number;
  maxPercent: number | null;
  min: number;
  max: number | null;
}

export interface Zones {
  thresholdsEffectiveFrom: string | null;
  ftpWatts: number | null;
  lthrBpm: number | null;
  powerZones: TrainingZone[];
  heartRateZones: TrainingZone[];
}

export interface Activity {
  id: number;
  source: string;
  externalId: string;
  name: string | null;
  sportType: string | null;
  startTime: string;
  startTimeLocal: string | null;
  durationSeconds: number | null;
  movingDurationSeconds: number | null;
  distanceMeters: number | null;
  elevationGainMeters: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  averagePower: number | null;
  maxPower: number | null;
  normalizedPower: number | null;
  averageCadence: number | null;
  calories: number | null;
}

export interface AnalysisSummary {
  activityId: number;
  name: string | null;
  sportType: string | null;
  startTime: string;
  startTimeLocal: string | null;
  analyzedAt: string;
  ftpWatts: number | null;
  durationSeconds: number;
  averagePower: number | null;
  normalizedPower: number | null;
  variabilityIndex: number | null;
  intensityFactor: number | null;
  trainingStressScore: number | null;
  workKilojoules: number | null;
  averageHeartRate: number | null;
  maxHeartRate: number | null;
  intervalCount: number;
}

export interface GarminSyncResult {
  since: string;
  fetched: number;
  imported: number;
}

export interface ZoneTime {
  number: number;
  name: string;
  seconds: number;
  percent: number;
}

export interface PowerMetrics {
  averagePower: number;
  maxPower: number;
  normalizedPower: number;
  variabilityIndex: number | null;
  intensityFactor: number | null;
  trainingStressScore: number | null;
  averagePowerPercentFtp: number | null;
  workKilojoules: number;
}

export interface HeartRateMetrics {
  average: number;
  max: number;
  percentOfMax: number | null;
}

export interface IntervalBlock {
  number: number;
  startSecond: number;
  endSecond: number;
  durationSeconds: number;
  dropoutSeconds: number;
  averagePower: number;
  averagePowerExcludingZeros: number | null;
  maxPower: number;
  percentOfTarget: number | null;
  heartRateStart: number | null;
  heartRateEnd: number | null;
  maxHeartRate: number | null;
  efficiency: number | null;
  vo2StimulusSeconds: number | null;
}

export interface Intervals {
  detected: boolean;
  mode: 'AUTO' | 'MANUAL';
  relativeThreshold: number | null;
  minDurationSeconds: number | null;
  maxGapSeconds: number | null;
  meanDurationSeconds: number | null;
  durationConsistency: number | null;
  powerConsistency: number | null;
  targetPower: number | null;
  targetSource: 'AUTO' | 'MANUAL' | null;
  blocks: IntervalBlock[];
  vo2ThresholdBpm: number | null;
  totalVo2StimulusSeconds: number | null;
  cardiacDriftPercent: number | null;
}

export interface ActivityAnalysis {
  sport: string | null;
  subSport: string | null;
  durationSeconds: number;
  distanceMeters: number | null;
  power: PowerMetrics | null;
  heartRate: HeartRateMetrics | null;
  averageCadence: number | null;
  powerZones: ZoneTime[];
  heartRateZones: ZoneTime[];
  intervals: Intervals | null;
}

export interface ActivityAnalysisResponse {
  activityId: number;
  stored: boolean;
  analyzedAt: string;
  analyzerVersion: number;
  thresholds: {
    ftpWatts: number | null;
    lthrBpm: number | null;
    maxHeartRateBpm: number | null;
    targetWatts: number | null;
  };
  analysis: ActivityAnalysis;
}

/** RFC 9457 error body returned by the backend. */
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
