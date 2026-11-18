import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../App';
import { ErrorText } from '../ui';

function AuthCard({ title, children }) {
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <div className="card w-full max-w-sm space-y-4">
        <h1 className="text-2xl font-bold text-indigo-700">TaskFlow</h1>
        <h2 className="text-lg font-semibold">{title}</h2>
        {children}
      </div>
    </div>
  );
}

export function Login() {
  const { setUser } = useAuth();
  const nav = useNavigate();
  const [form, setForm] = useState({ email: '', password: '' });
  const [error, setError] = useState('');

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const r = await api('/api/auth/login', { method: 'POST', body: form });
      setUser(r.user, r.token);
      nav('/');
    } catch (x) {
      setError(x.message);
    }
  };

  return (
    <AuthCard title="Sign in">
      <form onSubmit={submit} className="space-y-3">
        <ErrorText error={error} />
        <input className="input" type="email" placeholder="Email" required autoComplete="email"
          value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input className="input" type="password" placeholder="Password" required autoComplete="current-password"
          value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button className="btn btn-primary w-full justify-center">Sign in</button>
      </form>
      <p className="text-sm">No account? <Link className="text-indigo-700 underline" to="/register">Register</Link></p>
    </AuthCard>
  );
}

export function Register() {
  const [form, setForm] = useState({ fullName: '', email: '', password: '' });
  const [error, setError] = useState('');
  const [done, setDone] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await api('/api/auth/register', { method: 'POST', body: form });
      setDone(true);
    } catch (x) {
      setError(x.message);
    }
  };

  if (done)
    return (
      <AuthCard title="Check your inbox">
        <p className="text-sm">We sent a verification link to <b>{form.email}</b>. Click it to activate your account.</p>
        <Link className="btn" to="/login">Back to sign in</Link>
      </AuthCard>
    );

  return (
    <AuthCard title="Create an account">
      <form onSubmit={submit} className="space-y-3">
        <ErrorText error={error} />
        <input className="input" placeholder="Full name" required value={form.fullName}
          onChange={(e) => setForm({ ...form, fullName: e.target.value })} />
        <input className="input" type="email" placeholder="Email" required value={form.email}
          onChange={(e) => setForm({ ...form, email: e.target.value })} />
        <input className="input" type="password" placeholder="Password (min 8 chars)" required minLength={8}
          autoComplete="new-password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} />
        <button className="btn btn-primary w-full justify-center">Register</button>
      </form>
      <p className="text-sm">Have an account? <Link className="text-indigo-700 underline" to="/login">Sign in</Link></p>
    </AuthCard>
  );
}

export function Verify() {
  const [params] = useSearchParams();
  const [msg, setMsg] = useState('Verifying...');
  useEffect(() => {
    api(`/api/auth/verify?token=${encodeURIComponent(params.get('token') || '')}`)
      .then((r) => setMsg(r.message))
      .catch((e) => setMsg(e.message));
  }, [params]);
  return (
    <AuthCard title="Email verification">
      <p className="text-sm">{msg}</p>
      <Link className="btn" to="/login">Go to sign in</Link>
    </AuthCard>
  );
}
