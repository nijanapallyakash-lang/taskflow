import { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../App';
import { Avatar, ErrorText, fmtDate } from '../ui';
import { Backlog, Board } from './Board';
import Dashboard from './Dashboard';
import TaskModal from './TaskModal';

const TABS = [['board', 'Board'], ['backlog', 'Backlog & Sprints'], ['dashboard', 'Dashboard'], ['activity', 'Activity'], ['settings', 'Settings']];

export default function Project() {
  const { id } = useParams();
  const { user } = useAuth();
  const [params, setParams] = useSearchParams();
  const [project, setProject] = useState(null);
  const [sprints, setSprints] = useState([]);
  const [version, setVersion] = useState(0); // bump to make child views refetch
  const [error, setError] = useState('');
  const tab = params.get('tab') || 'board';
  const taskId = params.get('task');

  const load = useCallback(async () => {
    try {
      const [p, s] = await Promise.all([api(`/api/projects/${id}`), api(`/api/projects/${id}/sprints`)]);
      setProject(p);
      setSprints(s);
    } catch (e) {
      setError(e.message);
    }
  }, [id]);
  useEffect(() => { load(); }, [load]);

  const setQuery = (patch) => {
    const next = Object.fromEntries(params);
    Object.entries(patch).forEach(([k, v]) => (v == null ? delete next[k] : (next[k] = v)));
    setParams(next);
  };
  const refresh = () => { setVersion((v) => v + 1); load(); };

  if (error) return <ErrorText error={error} />;
  if (!project) return <p>Loading...</p>;
  const ctx = { project, sprints, version, refresh, openTask: (t) => setQuery({ task: t }), canManage: user.role !== 'TEAM_MEMBER' };

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold">
          {project.name} {project.archived && <span className="badge bg-amber-100 text-amber-800 align-middle">ARCHIVED</span>}
        </h1>
        <p className="text-sm text-slate-600">{project.description}</p>
      </div>
      <nav className="flex gap-1 border-b border-slate-200">
        {TABS.map(([k, label]) => (
          <button key={k} onClick={() => setQuery({ tab: k })}
            className={`-mb-px border-b-2 px-3 py-2 text-sm ${tab === k ? 'border-indigo-600 font-semibold text-indigo-700' : 'border-transparent text-slate-600'}`}>
            {label}
          </button>
        ))}
      </nav>
      {tab === 'board' && <Board {...ctx} />}
      {tab === 'backlog' && <Backlog {...ctx} />}
      {tab === 'dashboard' && <Dashboard {...ctx} />}
      {tab === 'activity' && <Activity {...ctx} />}
      {tab === 'settings' && <Settings {...ctx} />}
      {taskId && <TaskModal id={taskId} {...ctx} onClose={() => setQuery({ task: null })} />}
    </div>
  );
}

function Activity({ project, version, openTask }) {
  const [items, setItems] = useState([]);
  useEffect(() => { api(`/api/projects/${project.id}/activity`).then(setItems); }, [project.id, version]);
  return (
    <ul className="card divide-y divide-slate-100">
      {items.map((a) => (
        <li key={a.id} className="flex items-center gap-3 py-2 text-sm">
          <Avatar user={a.user} />
          <span><b>{a.user.fullName}</b> <span className="badge bg-slate-100">{a.action}</span> {a.details}</span>
          {a.taskId && <button className="text-indigo-700 underline" onClick={() => openTask(a.taskId)}>#{a.taskId}</button>}
          <span className="ml-auto text-xs text-slate-500">{fmtDate(a.createdAt)}</span>
        </li>
      ))}
      {items.length === 0 && <li className="py-2 text-sm text-slate-500">No activity yet.</li>}
    </ul>
  );
}

function Settings({ project, canManage, refresh }) {
  const nav = useNavigate();
  const [form, setForm] = useState({ name: project.name, description: project.description || '' });
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const run = async (fn) => {
    setError('');
    try { await fn(); refresh(); } catch (e) { setError(e.message); }
  };

  return (
    <div className="grid gap-4 md:grid-cols-2">
      <ErrorText error={error} />
      <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); run(() => api(`/api/projects/${project.id}`, { method: 'PUT', body: form })); }}>
        <h2 className="font-semibold">Details</h2>
        <input className="input" required disabled={!canManage} value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        <textarea className="input" rows={4} disabled={!canManage} value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        {canManage && (
          <div className="flex gap-2">
            <button className="btn btn-primary">Save</button>
            <button type="button" className="btn btn-danger"
              onClick={() => run(async () => { await api(`/api/projects/${project.id}/${project.archived ? 'unarchive' : 'archive'}`, { method: 'POST' }); if (!project.archived) nav('/'); })}>
              {project.archived ? 'Unarchive' : 'Archive'} project
            </button>
          </div>
        )}
      </form>
      <div className="card space-y-3">
        <h2 className="font-semibold">Members</h2>
        <ul className="space-y-2">
          {project.members.map((m) => (
            <li key={m.id} className="flex items-center gap-2 text-sm">
              <Avatar user={m} /> {m.fullName} <span className="text-slate-500">{m.email}</span>
              {m.id === project.owner.id && <span className="badge bg-indigo-50 text-indigo-700">owner</span>}
              {canManage && m.id !== project.owner.id && (
                <button className="ml-auto text-xs text-red-600" onClick={() => run(() => api(`/api/projects/${project.id}/members/${m.id}`, { method: 'DELETE' }))}>remove</button>
              )}
            </li>
          ))}
        </ul>
        {canManage && (
          <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); run(async () => { await api(`/api/projects/${project.id}/members`, { method: 'POST', body: { email } }); setEmail(''); }); }}>
            <input className="input" type="email" required placeholder="user@company.com" value={email} onChange={(e) => setEmail(e.target.value)} />
            <button className="btn">Add</button>
          </form>
        )}
      </div>
    </div>
  );
}
