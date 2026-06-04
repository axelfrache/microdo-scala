import { Monitor, PlusCircle, History } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'

const linkBase = 'inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition'

export function AppShell() {
  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      <div className="absolute inset-x-0 top-0 -z-10 h-80 bg-gradient-to-b from-cyan-500/20 via-sky-500/10 to-transparent" />
      <header className="border-b border-white/10 bg-slate-950/80 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-col gap-4 px-6 py-5 md:flex-row md:items-center md:justify-between">
          <div>
            <div className="flex items-center gap-2 text-3xl font-semibold">
              SquirrelVault
            </div>
          </div>
          <nav className="flex flex-wrap gap-2">
            <NavLink to="/" className={({ isActive }) => `${linkBase} ${isActive ? 'bg-white text-slate-950' : 'bg-white/5 text-slate-200 hover:bg-white/10'}`}>
              <Monitor className="h-4 w-4" />
              Backups
            </NavLink>
            <NavLink to="/create" className={({ isActive }) => `${linkBase} ${isActive ? 'bg-cyan-300 text-slate-950' : 'bg-cyan-400/10 text-cyan-200 hover:bg-cyan-400/20'}`}>
              <PlusCircle className="h-4 w-4" />
              Nouveau backup
            </NavLink>
            <NavLink to="/history" className={({ isActive }) => `${linkBase} ${isActive ? 'bg-white text-slate-950' : 'bg-white/5 text-slate-200 hover:bg-white/10'}`}>
              <History className="h-4 w-4" />
              Historique
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-6 py-8">
        <Outlet />
      </main>
    </div>
  )
}