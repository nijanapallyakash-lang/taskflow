import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../App';
import { Avatar, ErrorText } from '../ui';

export default function Profile() {
  const { user, setUser } = useAuth();
  const [form, setForm] = useState({ fullName: user.fullName, skills: user.skills || '' });
  const [pw, setPw] = useState({ currentPassword: '', newPassword: '' });
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  const run = async (fn, ok) => {
    setError('');
    setMsg('');
    try { await fn(); setMsg(ok); } catch (e) { setError(e.message); }
  };

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <h1 className="text-2xl font-bold">Profile</h1>
      <ErrorText error={error} />
      {msg && <p className="rounded bg-green-50 px-3 py-2 text-sm text-green-800">{msg}</p>}

      <div className="card flex items-center gap-4">
        {/* key forces <img> refetch after upload */}
        <Avatar key={msg} user={user} large />
        <div>
          <label className="label" htmlFor="avatar">Avatar</label>
          <input id="avatar" type="file" accept="image/*" className="text-sm" onChange={(e) => {
            const file = e.target.files[0];
            if (!file) return;
            const fd = new FormData();
            fd.append('file', file);
            run(async () => setUser(await api('/api/users/me/avatar', { method: 'POST', body: fd })), 'Avatar updated');
          }} />
        </div>
      </div>

      <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); run(async () => setUser(await api('/api/users/me', { method: 'PUT', body: form })), 'Profile saved'); }}>
        <h2 className="font-semibold">Details</h2>
        <p className="text-sm text-slate-500">{user.email}</p>
        <div><label className="label">Full name</label><input className="input" required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></div>
        <div><label className="label">Skills (comma separated)</label><input className="input" placeholder="Java, React, SQL" value={form.skills} onChange={(e) => setForm({ ...form, skills: e.target.value })} /></div>
        {user.skills && <div className="flex flex-wrap gap-1">{user.skills.split(',').map((s) => s.trim()).filter(Boolean).map((s) => <span key={s} className="badge bg-indigo-50 text-indigo-700">{s}</span>)}</div>}
        <button className="btn btn-primary">Save</button>
      </form>

      <form className="card space-y-3" onSubmit={(e) => { e.preventDefault(); run(async () => { await api('/api/users/me/password', { method: 'PUT', body: pw }); setPw({ currentPassword: '', newPassword: '' }); }, 'Password changed'); }}>
        <h2 className="font-semibold">Change password</h2>
        <input className="input" type="password" placeholder="Current password" required autoComplete="current-password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} />
        <input className="input" type="password" placeholder="New password (min 8 chars)" required minLength={8} autoComplete="new-password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} />
        <button className="btn">Update password</button>
      </form>
    </div>
  );
}
