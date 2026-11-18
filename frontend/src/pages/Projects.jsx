import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../App';
import { Avatar, ErrorText } from '../ui';

export default function Projects() {
  const { user } = useAuth();
  const [archived, setArchived] = useState(false);
  const [projects, setProjects] = useState([]);
  const [form, setForm] = useState({ name: '', description: '' });
  const [error, setError] = useState('');
  const canCreate = user.role !== 'TEAM_MEMBER';

  const load = () => api(`/api/projects?archived=${archived}`).then(setProjects).catch((e) => setError(e.message));
  useEffect(() => { load(); }, [archived]); // eslint-disable-line react-hooks/exhaustive-deps

  const create = async (e) => {
    e.preventDefault();
    try {
      await api('/api/projects', { method: 'POST', body: form });
      setForm({ name: '', description: '' });
      load();
    } catch (x) {
      setError(x.message);
    }
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-bold">Projects</h1>
        <div className="ml-auto flex gap-1">
          <button className={`btn ${!archived ? 'btn-primary' : ''}`} onClick={() => setArchived(false)}>Active</button>
          <button className={`btn ${archived ? 'btn-primary' : ''}`} onClick={() => setArchived(true)}>Archived</button>
        </div>
      </div>
      <ErrorText error={error} />
      {canCreate && !archived && (
        <form onSubmit={create} className="card flex flex-wrap gap-2">
          <input className="input flex-1" placeholder="New project name" required value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })} />
          <input className="input flex-[2]" placeholder="Description" value={form.description}
            onChange={(e) => setForm({ ...form, description: e.target.value })} />
          <button className="btn btn-primary">Create project</button>
        </form>
      )}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {projects.map((p) => (
          <Link key={p.id} to={`/projects/${p.id}`} className="card block hover:border-indigo-300">
            <h2 className="font-semibold">{p.name}</h2>
            <p className="mt-1 line-clamp-2 text-sm text-slate-600">{p.description || 'No description'}</p>
            <div className="mt-3 flex -space-x-2">{p.members.slice(0, 8).map((m) => <Avatar key={m.id} user={m} />)}</div>
          </Link>
        ))}
        {projects.length === 0 && <p className="text-sm text-slate-500">No {archived ? 'archived ' : ''}projects yet.</p>}
      </div>
    </div>
  );
}
