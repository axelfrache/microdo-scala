export type BackupJobSourceType =
  | 'PostgreSQL'
  | 'MySQL'
  | 'Filesystem'
  | 'KubernetesPVC'
  | 'S3Bucket'
  | 'ConfigFiles'

export type BackupJob = {
  id: string
  name: string
  sourceType: BackupJobSourceType
  source: string
  targetBucket: string
  targetPrefix: string
  schedule: string
  retentionDays: number
  critical: boolean
  expectedFrequencyHours: number
  enabled: boolean
  createdAt: string
}

export type BackupRun = {
  id: string
  jobId: string
  status: string
  startedAt: string | null
  finishedAt: string | null
  duration: number | null
  sizeMb: number | null
  location: string | null
  error: string | null
}

export type BackupHistoryEntry = {
  job: BackupJob
  run: BackupRun
}

export type CreateBackupJobInput = {
  name: string
  sourceType: BackupJobSourceType
  source: string
  targetBucket: string
  targetPrefix: string
  schedule: string
  retentionDays: number
  critical: boolean
  expectedFrequencyHours: number
  enabled: boolean
}

const base = '/api'
const TIMEOUT_MS = 10_000

async function fetchT(url: string, options: RequestInit = {}): Promise<Response> {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS)
  try {
    return await fetch(url, { ...options, signal: controller.signal })
  } finally {
    clearTimeout(timer)
  }
}

export async function listJobs(enabled?: boolean): Promise<BackupJob[]> {
  const url = new URL(`${base}/backup-jobs`, window.location.href)
  if (enabled !== undefined) url.searchParams.set('enabled', String(enabled))
  const res = await fetchT(url.toString())
  if (!res.ok) throw new Error(`listJobs failed: ${res.status}`)
  return res.json()
}

export async function createJob(body: CreateBackupJobInput): Promise<BackupJob> {
  const res = await fetchT(`${base}/backup-jobs`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(`createJob failed: ${res.status} ${text}`)
  }
  return res.json()
}

export async function getJob(id: string): Promise<BackupJob> {
  const res = await fetchT(`${base}/backup-jobs/${id}`)
  if (!res.ok) throw new Error(`getJob failed: ${res.status}`)
  return res.json()
}

export async function disableJob(id: string): Promise<BackupJob> {
  const res = await fetchT(`${base}/backup-jobs/${id}/disable`, { method: 'PATCH' })
  if (!res.ok) throw new Error(`disableJob failed: ${res.status}`)
  return res.json()
}

export async function listRunsForJob(jobId: string): Promise<BackupRun[]> {
  const res = await fetchT(`${base}/backup-jobs/${jobId}/runs`)
  if (!res.ok) throw new Error(`listRunsForJob failed: ${res.status}`)
  return res.json()
}

export async function listHistory(): Promise<BackupHistoryEntry[]> {
  const res = await fetchT(`${base}/backup-history`)
  if (!res.ok) throw new Error(`listHistory failed: ${res.status}`)
  return res.json()
}
