import { useEffect, useState } from 'react';
import { api } from '../api';
import { useAuth } from '../App';
import { Avatar, ErrorText, ROLES } from '../ui';

export default function Admin() {
  const { user } = useAuth();
  const [users, setUsers] = useState([]);
  const [q, setQ] = useState('');
  const [error, setError] = useState('');

  const load = () => api('/api/admin/users').then(setUsers).catch((e) => setError(e.message));
  useEffect(() => { load(); }, []);

  const update = async (id, body) => {
    setError('');
    try { await api(`/api/admin/users/${id}`, { method: 'PATCH', body }); load(); } catch (e) { setError(e.message); }
  };

  const shown = users.filter((u) => `${u.fullName} ${u.email}`.toLowerCase().includes(q.toLowerCase()));

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <h1 className="text-2xl font-bold">User management</h1>
        <input className="input ml-auto w-64" placeholder="Search users..." value={q} onChange={(e) => setQ(e.target.value)} />
      </div>
      <ErrorText error={error} />
      <div className="card overflow-x-auto p-0">
        <table className="w-full text-sm">
          <thead className="bg-slate-50 text-left text-xs uppercase text-slate-500">
            <tr><th className="p-3">User</th><th className="p-3">Verified</th><th className="p-3">Role</th><th className="p-3">Status</th></tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {shown.map((u) => (
              <tr key={u.id}>
                <td className="flex items-center gap-2 p-3"><Avatar user={u} /><div><p className="font-medium">{u.fullName}</p><p className="text-xs text-slate-500">{u.email}</p></div></td>
                <td className="p-3">{u.verified ? 'Yes' : 'Pending'}</td>
                <td className="p-3">
                  <select className="input w-auto" value={u.role} disabled={u.id === user.id} onChange={(e) => update(u.id, { role: e.target.value })}>
                    {ROLES.map((r) => <option key={r} value={r}>{r.replace('_', ' ')}</option>)}
                  </select>
                </td>
                <td className="p-3">
                  <button className={`btn ${u.active ? '' : 'btn-primary'}`} disabled={u.id === user.id} onClick={() => update(u.id, { active: !u.active })}>
                    {u.active ? 'Deactivate' : 'Activate'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
