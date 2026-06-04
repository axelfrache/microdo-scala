import { useNavigate } from 'react-router-dom'
import { Sparkles } from 'lucide-react'
import CreateBackupForm from '../components/CreateBackupForm'

export function CreateBackupPage() {
  const navigate = useNavigate()

  return (
    <section className="grid gap-6">
      <div className="rounded-3xl border border-white/10 bg-white/5 p-6 shadow-2xl shadow-cyan-950/20 backdrop-blur">
        <div className="flex items-center gap-2 text-sm uppercase tracking-[0.24em] text-cyan-200">
          <Sparkles className="h-4 w-4" />
          Nouveau backup
        </div>

        <div className="mt-6 rounded-3xl border border-white/10 bg-slate-950/50 p-4">
          <CreateBackupForm onCreated={() => navigate('/')} />
        </div>
        </div>
    </section>
  )
}