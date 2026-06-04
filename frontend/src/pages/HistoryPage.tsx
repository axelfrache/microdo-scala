import { useEffect, useState } from 'react'
import { CalendarClock, Clock3, Archive, Filter, MoveRight, RefreshCw } from 'lucide-react'
import { listHistory, type BackupHistoryEntry } from '../lib/api'

type HistoryEntry = {
  id: string
  name: string
  source: string
  target: string
  sortAt: number
  executedAt: string
  status: string
  duration: string
  sizeMb: string
  location: string
  error: string | null
}

const statusStyles: Record<string, string> = {
  SUCCESS: 'bg-emerald-400/15 text-emerald-200',
  FAILED: 'bg-rose-400/15 text-rose-200',
  RUNNING: 'bg-cyan-400/15 text-cyan-200',
  PENDING: 'bg-amber-400/15 text-amber-200',
}

const statusLabels: Record<string, string> = {
  SUCCESS: 'Succès',
  FAILED: 'Échec',
  RUNNING: 'En cours',
  PENDING: 'En attente',
}

const formatDateTime = (value: string | null) => {
  if (!value) return 'En attente'
  const date = new Date(value)
  return Number.isNaN(date.getTime())
    ? value
    : new Intl.DateTimeFormat('fr-FR', {
        dateStyle: 'medium',
        timeStyle: 'short',
      }).format(date)
}

const formatDuration = (run: BackupHistoryEntry['run']) => {
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

const toHistoryEntry = ({ job, run }: BackupHistoryEntry): HistoryEntry => ({
  id: run.id,
  name: job.name,
  source: job.source,
  target: job.targetBucket + (job.targetPrefix ? `/${job.targetPrefix}` : ''),
  sortAt: new Date(run.finishedAt ?? run.startedAt ?? 0).getTime(),
  executedAt: formatDateTime(run.finishedAt ?? run.startedAt),
  status: statusLabels[run.status] ?? run.status,
  duration: formatDuration(run),
  sizeMb: run.sizeMb !== null ? `${run.sizeMb} MB` : 'N/A',
  location: run.location ?? 'N/A',
  error: run.error,
})

export function HistoryPage() {
  const [entries, setEntries] = useState<HistoryEntry[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchHistory = async () => {
    setLoading(true)
    setError(null)
    try {
      const history = await listHistory()
      const nextEntries = history
        .map(toHistoryEntry)
        .sort((a, b) => b.sortAt - a.sortAt)

      setEntries(nextEntries)
    } catch (err: any) {
      setError(err.message ?? 'Unable to load history')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchHistory()
  }, [])

  const successCount = entries.filter(entry => entry.status === 'Succès').length

  return (
    <section className="space-y-6">
      <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl shadow-cyan-950/20 backdrop-blur">
        <div className="flex items-center gap-2 text-sm uppercase tracking-[0.24em] text-cyan-200">
          <Archive className="h-4 w-4" />
          Historique
        </div>
        <h1 className="mt-4 text-3xl font-semibold text-white">Historique des backups</h1>
        <p className="mt-3 max-w-3xl text-sm leading-6 text-slate-300">
          Cette page lit les exécutions depuis `backup_runs` via le backend et les assemble avec les jobs pour afficher l’historique réel.
        </p>
        <div className="mt-6 grid gap-3 md:grid-cols-3">
          <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
            <div className="flex items-center gap-2 text-slate-400"><CalendarClock className="h-4 w-4" /> Période</div>
            <div className="mt-2 text-sm text-white">Tous les runs disponibles</div>
          </div>
          <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
            <div className="flex items-center gap-2 text-slate-400"><Clock3 className="h-4 w-4" /> Vue</div>
            <div className="mt-2 text-sm text-white">{entries.length} exécutions</div>
          </div>
          <div className="rounded-2xl border border-white/10 bg-slate-950/50 p-4">
            <div className="flex items-center gap-2 text-slate-400"><Filter className="h-4 w-4" /> Filtre</div>
            <div className="mt-2 text-sm text-white">{successCount} succès</div>
          </div>
        </div>
      </div>

      <div className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/80 shadow-lg shadow-black/20">
        <div className="flex items-center justify-between border-b border-white/10 px-6 py-4 text-sm text-slate-400">
          <button onClick={fetchHistory} className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-100 transition hover:bg-white/10">
            <RefreshCw className="h-3.5 w-3.5" />
            Rafraîchir
          </button>
        </div>
        {error && <div className="border-b border-red-500/20 bg-red-500/10 px-6 py-3 text-sm text-red-200">{error}</div>}
        {loading && <div className="px-6 py-4 text-sm text-slate-400">Chargement de l’historique…</div>}
        <div className="divide-y divide-white/10">
          {!loading && entries.length === 0 ? (
            <div className="px-6 py-8 text-center text-sm text-slate-300">Aucune exécution trouvée pour le moment.</div>
          ) : null}

          {entries.map(entry => (
            <article key={entry.id} className="grid gap-4 px-6 py-5 md:grid-cols-[1.2fr_1fr_1fr_auto] md:items-center">
              <div>
                <div className="text-lg font-semibold text-white">{entry.name}</div>
                <div className="mt-1 text-sm text-slate-400">{entry.source}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-[0.24em] text-slate-500">Destination</div>
                <div className="mt-1 text-sm text-slate-200">{entry.target}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-[0.24em] text-slate-500">Exécution</div>
                <div className="mt-1 text-sm text-slate-200">{entry.executedAt}</div>
                <div className="mt-1 text-xs text-slate-400">Durée {entry.duration}</div>
                <div className="mt-1 text-xs text-slate-400">Taille {entry.sizeMb}</div>
              </div>
              <div className="flex items-center gap-3 md:justify-end">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyles[entry.status] ?? 'bg-slate-500/20 text-slate-200'}`}>{entry.status}</span>
                <MoveRight className="h-4 w-4 text-slate-500" />
              </div>
              {entry.location !== 'N/A' || entry.error ? (
                <div className="md:col-span-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm text-slate-300">
                  {entry.location !== 'N/A' ? <div>Emplacement: {entry.location}</div> : null}
                  {entry.error ? <div className="mt-1 text-rose-200">Erreur: {entry.error}</div> : null}
                </div>
              ) : null}
            </article>
          ))}
        </div>
      </div>
    </section>
  )
}