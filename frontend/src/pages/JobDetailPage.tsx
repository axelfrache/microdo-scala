import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, RefreshCw } from 'lucide-react'
import { getJob, listRunsForJob, type BackupJob, type BackupRun } from '../lib/api'
import { statusStyles, statusLabels, formatDateTime, formatDuration } from '../lib/history'

export function JobDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [job, setJob] = useState<BackupJob | null>(null)
  const [runs, setRuns] = useState<BackupRun[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchData = async (jobId: string) => {
    setLoading(true)
    setError(null)
    try {
      const [fetchedJob, fetchedRuns] = await Promise.all([getJob(jobId), listRunsForJob(jobId)])
      setJob(fetchedJob)
      setRuns(fetchedRuns)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load job')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (id) void fetchData(id)
  }, [id])

  return (
    <section className="space-y-6">
      <div className="flex items-center gap-4">
        <Link to="/" className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-sm text-slate-300 transition hover:bg-white/10">
          <ArrowLeft className="h-4 w-4" />
          Retour
        </Link>
      </div>

      {error && <div className="rounded-2xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">{error}</div>}
      {loading && <div className="text-sm text-slate-400">Chargement…</div>}

      {job && (
        <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl shadow-cyan-950/20 backdrop-blur">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-2xl font-semibold text-white">{job.name}</h1>
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${job.enabled ? 'bg-emerald-400/15 text-emerald-200' : 'bg-slate-500/20 text-slate-200'}`}>
                  {job.enabled ? 'Actif' : 'Désactivé'}
                </span>
                {job.critical && (
                  <span className="rounded-full bg-amber-400/15 px-3 py-1 text-xs font-semibold text-amber-200">Critique</span>
                )}
              </div>
              <p className="mt-2 text-sm text-slate-400">{job.source}</p>
            </div>
          </div>
          <div className="mt-5 grid gap-3 sm:grid-cols-2 lg:grid-cols-4 text-sm">
            {[
              ['Type', job.sourceType],
              ['Bucket', job.targetBucket + (job.targetPrefix ? `/${job.targetPrefix}` : '')],
              ['Cron', job.schedule],
              ['Rétention', `${job.retentionDays} jours`],
            ].map(([label, value]) => (
              <div key={label} className="rounded-2xl border border-white/10 bg-slate-950/50 px-4 py-3">
                <div className="text-xs uppercase tracking-widest text-slate-500">{label}</div>
                <div className="mt-1 text-slate-200">{value}</div>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="overflow-hidden rounded-3xl border border-white/10 bg-slate-900/80 shadow-lg shadow-black/20">
        <div className="flex items-center justify-between border-b border-white/10 px-6 py-4">
          <span className="text-sm text-slate-400">{runs.length} exécution{runs.length !== 1 ? 's' : ''}</span>
          <button onClick={() => id && void fetchData(id)} className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-100 transition hover:bg-white/10">
            <RefreshCw className="h-3.5 w-3.5" />
            Rafraîchir
          </button>
        </div>

        <div className="divide-y divide-white/10">
          {!loading && runs.length === 0 ? (
            <div className="px-6 py-8 text-center text-sm text-slate-300">Aucune exécution pour ce job.</div>
          ) : null}

          {runs.map(run => (
            <div key={run.id} className="grid gap-3 px-6 py-4 sm:grid-cols-[1fr_1fr_1fr_auto] sm:items-center">
              <div>
                <div className="text-xs uppercase tracking-widest text-slate-500">Début</div>
                <div className="mt-1 text-sm text-slate-200">{formatDateTime(run.startedAt)}</div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-widest text-slate-500">Durée · Taille</div>
                <div className="mt-1 text-sm text-slate-200">
                  {formatDuration(run)} · {run.sizeMb !== null ? `${run.sizeMb} MB` : 'N/A'}
                </div>
              </div>
              <div>
                <div className="text-xs uppercase tracking-widest text-slate-500">Emplacement</div>
                <div className="mt-1 truncate text-sm text-slate-400">{run.location ?? '—'}</div>
              </div>
              <div className="flex items-center gap-2 sm:justify-end">
                <span className={`rounded-full px-3 py-1 text-xs font-semibold ${statusStyles[run.status] ?? 'bg-slate-500/20 text-slate-200'}`}>
                  {statusLabels[run.status] ?? run.status}
                </span>
              </div>
              {run.error && (
                <div className="sm:col-span-4 rounded-2xl border border-rose-500/20 bg-rose-500/10 px-4 py-2 text-sm text-rose-200">
                  {run.error}
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
