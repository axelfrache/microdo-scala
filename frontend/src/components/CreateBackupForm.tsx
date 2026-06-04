import { useState, type FormEvent } from 'react'
import { createJob, type BackupJob, type BackupJobSourceType } from '../lib/api'

type Props = {
  onCreated: (job: BackupJob) => void
}

export default function CreateBackupForm({ onCreated }: Props) {
  const [name, setName] = useState('')
  const [sourceType, setSourceType] = useState<BackupJobSourceType>('S3Bucket')
  const [source, setSource] = useState('')
  const [targetBucket, setTargetBucket] = useState('')
  const [targetPrefix, setTargetPrefix] = useState('')
  const [schedule, setSchedule] = useState('0 2 * * *')
  const [retentionDays, setRetentionDays] = useState(7)
  const [critical, setCritical] = useState(false)
  const [expectedFrequencyHours, setExpectedFrequencyHours] = useState(24)
  const [enabled, setEnabled] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const reset = () => {
    setName('')
    setSource('')
    setTargetBucket('')
    setTargetPrefix('')
    setSchedule('0 2 * * *')
    setRetentionDays(7)
    setCritical(false)
    setExpectedFrequencyHours(24)
    setEnabled(true)
  }

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError(null)
    if (!name.trim()) return setError('name is required')
    if (!source.trim()) return setError('source is required')
    if (!targetBucket.trim()) return setError('targetBucket is required')

    setLoading(true)
    try {
      const job = await createJob({
        name,
        sourceType,
        source,
        targetBucket,
        targetPrefix,
        schedule,
        retentionDays,
        critical,
        expectedFrequencyHours,
        enabled,
      })
      onCreated(job)
      reset()
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : String(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4 mb-4">
      <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Nom</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" placeholder="Name" value={name} onChange={e => setName(e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Type de source</span>
          <select className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 focus:border-cyan-300" value={sourceType} onChange={e => setSourceType(e.target.value as BackupJobSourceType)}>
            <option>S3Bucket</option>
            <option>PostgreSQL</option>
            <option>MySQL</option>
            <option>Filesystem</option>
            <option>KubernetesPVC</option>
            <option>ConfigFiles</option>
          </select>
        </label>
        <label className="grid gap-1 text-sm text-slate-300 md:col-span-2">
          <span>Source</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" placeholder="Source" value={source} onChange={e => setSource(e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Bucket cible</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" placeholder="Target bucket" value={targetBucket} onChange={e => setTargetBucket(e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Préfixe cible</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" placeholder="Target prefix" value={targetPrefix} onChange={e => setTargetPrefix(e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Planification</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" placeholder="Schedule (cron)" value={schedule} onChange={e => setSchedule(e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Rétention (jours)</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" type="number" placeholder="Retention days" value={retentionDays} onChange={e => setRetentionDays(Number(e.target.value))} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Fréquence attendue (heures)</span>
          <input className="rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300" type="number" placeholder="Expected frequency hours" value={expectedFrequencyHours} onChange={e => setExpectedFrequencyHours(Number(e.target.value))} />
        </label>
      </div>
      <div className="flex flex-wrap items-center gap-4 text-sm text-slate-300">
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={critical} onChange={e => setCritical(e.target.checked)} />
          Critique
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={enabled} onChange={e => setEnabled(e.target.checked)} />
          Activé
        </label>
      </div>
      <div>
        <button type="submit" disabled={loading} className="rounded-full bg-cyan-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60">{loading ? 'Creating…' : 'Create'}</button>
      </div>
      {error && <div className="text-red-600">{error}</div>}
    </form>
  )
}
