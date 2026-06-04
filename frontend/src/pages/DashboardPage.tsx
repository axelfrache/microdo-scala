import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { PlusCircle, RefreshCw } from 'lucide-react'
import { disableJob, listJobs, type BackupJob } from '../lib/api'

export function DashboardPage() {
  const [jobs, setJobs] = useState<BackupJob[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const fetchJobs = async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await listJobs()
      setJobs(data)
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to load jobs')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void fetchJobs()
  }, [])

  const handleDisable = async (id: string) => {
    try {
      const updated = await disableJob(id)
      setJobs(previous => previous.map(job => (job.id === id ? updated : job)))
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Unable to disable job')
    }
  }

  const enabledCount = jobs.filter(job => job.enabled).length

  return (
    <section className="space-y-6">
      <div className="grid gap-4 md:grid-cols-[1.4fr_0.8fr]">
        <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl shadow-cyan-950/20 backdrop-blur">
          <h1 className="mt-4 text-3xl font-semibold text-white md:text-4xl">Bienvenue sur SquirrelVault</h1>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-300">
            Cette application sert à créer, consulter et suivre tes jobs de sauvegarde ainsi que leur historique d’exécution.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <Link to="/create" className="inline-flex items-center gap-2 rounded-full bg-cyan-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200">
              <PlusCircle className="h-4 w-4" />
              Créer un nouveau backup
            </Link>
            <button onClick={fetchJobs} className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm font-medium text-slate-100 transition hover:bg-white/10">
              <RefreshCw className="h-4 w-4" />
              Rafraîchir
            </button>
          </div>
        </div>
        <aside className="rounded-3xl border border-white/10 bg-slate-900/70 p-6">
          <p className="text-sm uppercase tracking-[0.28em] text-slate-400">Statut</p>
          <div className="mt-4 space-y-3 text-sm text-slate-300">
            <div className="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
              <span>Jobs affichés</span>
              <strong className="text-white">{jobs.length}</strong>
            </div>
            <div className="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
              <span>Jobs actifs</span>
              <strong className="text-cyan-200">{enabledCount}</strong>
            </div>
            <div className="flex items-center justify-between rounded-2xl bg-white/5 px-4 py-3">
              <span>Jobs désactivés</span>
              <strong className="text-slate-200">{jobs.length - enabledCount}</strong>
            </div>
          </div>
        </aside>
      </div>

      {error && <div className="rounded-2xl border border-red-500/20 bg-red-500/10 px-4 py-3 text-sm text-red-200">{error}</div>}
      {loading && <div className="text-sm text-slate-400">Chargement des backups…</div>}

      <div className="grid gap-4">
        {jobs.length === 0 && !loading ? (
          <div className="rounded-3xl border border-dashed border-white/15 bg-white/5 p-8 text-center text-slate-300">
            Aucun backup actif pour l’instant.
          </div>
        ) : null}

        {jobs.map(job => (
          <article key={job.id} className="rounded-3xl border border-white/10 bg-slate-900/80 p-5 shadow-lg shadow-black/20">
            <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
              <div>
                <div className="flex items-center gap-3">
                  <h2 className="text-lg font-semibold text-white">{job.name}</h2>
                  <span className={`rounded-full px-3 py-1 text-xs font-semibold ${job.enabled ? 'bg-emerald-400/15 text-emerald-200' : 'bg-slate-500/20 text-slate-200'}`}>
                    {job.enabled ? 'Actif' : 'Désactivé'}
                  </span>
                </div>
                <p className="mt-2 text-sm text-slate-400">Source: {job.source}</p>
                <div className="mt-3 flex flex-wrap gap-2 text-xs text-slate-300">
                  <span className="rounded-full bg-white/5 px-3 py-1">{job.sourceType}</span>
                  <span className="rounded-full bg-white/5 px-3 py-1">Bucket: {job.targetBucket}</span>
                  <span className="rounded-full bg-white/5 px-3 py-1">Cron: {job.schedule}</span>
                </div>
              </div>
              {job.enabled && (
                <button onClick={() => handleDisable(job.id)} className="rounded-full bg-rose-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-rose-400">
                  Désactiver
                </button>
              )}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}