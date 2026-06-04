import type { BackupHistoryEntry } from './api'

export type HistoryEntry = {
  id: string
  name: string
  source: string
  target: string
  sortAt: number
  executedAt: string
  rawStatus: string
  status: string
  duration: string
  sizeMb: string
  location: string
  error: string | null
}

export const statusStyles: Record<string, string> = {
  SUCCESS: 'bg-emerald-400/15 text-emerald-200',
  FAILED: 'bg-rose-400/15 text-rose-200',
  RUNNING: 'bg-cyan-400/15 text-cyan-200',
  PENDING: 'bg-amber-400/15 text-amber-200',
}

export const statusLabels: Record<string, string> = {
  SUCCESS: 'Succès',
  FAILED: 'Échec',
  RUNNING: 'En cours',
  PENDING: 'En attente',
}

export function formatDateTime(value: string | null): string {
  if (!value) return 'En attente'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' }).format(date)
}

export function formatDuration(run: BackupHistoryEntry['run']): string {
  if (run.duration !== null) {
    const totalSeconds = Math.max(0, Math.round(run.duration))
    const minutes = Math.floor(totalSeconds / 60)
    const seconds = totalSeconds % 60
    return minutes > 0 ? `${minutes}m ${String(seconds).padStart(2, '0')}s` : `${seconds}s`
  }
  if (run.startedAt && run.finishedAt) {
    const started = new Date(run.startedAt).getTime()
    const finished = new Date(run.finishedAt).getTime()
    if (!Number.isNaN(started) && !Number.isNaN(finished) && finished >= started) {
      const totalSeconds = Math.round((finished - started) / 1000)
      const minutes = Math.floor(totalSeconds / 60)
      const seconds = totalSeconds % 60
      return minutes > 0 ? `${minutes}m ${String(seconds).padStart(2, '0')}s` : `${seconds}s`
    }
  }
  return 'En attente'
}

export function toHistoryEntry({ job, run }: BackupHistoryEntry): HistoryEntry {
  return {
    id: run.id,
    name: job.name,
    source: job.source,
    target: job.targetBucket + (job.targetPrefix ? `/${job.targetPrefix}` : ''),
    sortAt: new Date(run.finishedAt ?? run.startedAt ?? 0).getTime(),
    executedAt: formatDateTime(run.finishedAt ?? run.startedAt),
    rawStatus: run.status,
    status: statusLabels[run.status] ?? run.status,
    duration: formatDuration(run),
    sizeMb: run.sizeMb !== null ? `${run.sizeMb} MB` : 'N/A',
    location: run.location ?? 'N/A',
    error: run.error,
  }
}
