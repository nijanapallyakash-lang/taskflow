// Same-origin by default (Vite proxy in dev, nginx proxy in Docker). Set VITE_API_URL to call a remote API.
export const BASE = import.meta.env.VITE_API_URL || '';

export async function api(path, { method = 'GET', body, raw } = {}) {
  const headers = {};
  const token = localStorage.getItem('token');
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json';
    body = JSON.stringify(body);
  }
  const res = await fetch(BASE + path, { method, headers, body });
  if (res.status === 401 && token) {
    localStorage.clear();
    window.location.href = '/login';
  }
  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: res.statusText }));
    const fields = err.fieldErrors && Object.entries(err.fieldErrors).map(([k, v]) => `${k}: ${v}`).join(', ');
    throw new Error(fields || err.message || 'Request failed');
  }
  if (raw) return res.blob();
  return res.status === 204 ? null : res.json();
}

export async function download(path, filename) {
  const blob = await api(path, { raw: true });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

export const avatarUrl = (u) => (u?.hasAvatar ? `${BASE}/api/users/${u.id}/avatar` : null);
