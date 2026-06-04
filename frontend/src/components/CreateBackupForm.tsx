import { useState, type FormEvent } from 'react'
import { createJob, type BackupJob, type BackupJobSourceType } from '../lib/api'

type Props = {
  onCreated: (job: BackupJob) => void
}

type FormState = {
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

const INITIAL_STATE: FormState = {
  name: '',
  sourceType: 'PostgreSQL',
  source: '',
  targetBucket: '',
  targetPrefix: '',
  schedule: '0 2 * * *',
  retentionDays: 7,
  critical: false,
  expectedFrequencyHours: 24,
  enabled: true,
}

const INPUT_CLASS =
  'rounded border border-white/10 bg-slate-950/60 p-2 text-white outline-none ring-0 placeholder:text-slate-500 focus:border-cyan-300'

export default function CreateBackupForm({ onCreated }: Props) {
  const [form, setForm] = useState<FormState>(INITIAL_STATE)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) =>
    setForm(prev => ({ ...prev, [key]: value }))

  const submit = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    setError(null)
    if (!form.name.trim()) return setError('name is required')
    if (!form.source.trim()) return setError('source is required')
    if (!form.targetBucket.trim()) return setError('targetBucket is required')

    setLoading(true)
    try {
      const job = await createJob(form)
      onCreated(job)
      setForm(INITIAL_STATE)
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
          <input className={INPUT_CLASS} placeholder="Name" value={form.name} onChange={e => set('name', e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Type de source</span>
          <select className={INPUT_CLASS} value={form.sourceType} onChange={e => set('sourceType', e.target.value as BackupJobSourceType)}>
            <option value="PostgreSQL">PostgreSQL</option>
            <option value="MySQL" disabled>MySQL (bientôt)</option>
            <option value="Filesystem" disabled>Filesystem (bientôt)</option>
            <option value="KubernetesPVC" disabled>KubernetesPVC (bientôt)</option>
            <option value="S3Bucket" disabled>S3Bucket (bientôt)</option>
            <option value="ConfigFiles" disabled>ConfigFiles (bientôt)</option>
          </select>
        </label>
        <label className="grid gap-1 text-sm text-slate-300 md:col-span-2">
          <span>Source</span>
          <input className={INPUT_CLASS} placeholder="Source" value={form.source} onChange={e => set('source', e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Bucket cible</span>
          <input className={INPUT_CLASS} placeholder="Target bucket" value={form.targetBucket} onChange={e => set('targetBucket', e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Préfixe cible</span>
          <input className={INPUT_CLASS} placeholder="Target prefix" value={form.targetPrefix} onChange={e => set('targetPrefix', e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Planification</span>
          <input className={INPUT_CLASS} placeholder="Schedule (cron)" value={form.schedule} onChange={e => set('schedule', e.target.value)} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Rétention (jours)</span>
          <input className={INPUT_CLASS} type="number" placeholder="Retention days" value={form.retentionDays} onChange={e => set('retentionDays', Number(e.target.value))} />
        </label>
        <label className="grid gap-1 text-sm text-slate-300">
          <span>Fréquence attendue (heures)</span>
          <input className={INPUT_CLASS} type="number" placeholder="Expected frequency hours" value={form.expectedFrequencyHours} onChange={e => set('expectedFrequencyHours', Number(e.target.value))} />
        </label>
      </div>
      <div className="flex flex-wrap items-center gap-4 text-sm text-slate-300">
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={form.critical} onChange={e => set('critical', e.target.checked)} />
          Critique
        </label>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={form.enabled} onChange={e => set('enabled', e.target.checked)} />
          Activé
        </label>
      </div>
      <div>
        <button type="submit" disabled={loading} className="rounded-full bg-cyan-300 px-4 py-2 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-60">
          {loading ? 'Creating…' : 'Create'}
        </button>
      </div>
      {error && <div className="text-red-600">{error}</div>}
    </form>
  )
}
