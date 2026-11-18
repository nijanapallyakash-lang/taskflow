import { avatarUrl } from './api';

export const STATUSES = [
  ['TODO', 'To Do'],
  ['IN_PROGRESS', 'In Progress'],
  ['CODE_REVIEW', 'Code Review'],
  ['DONE', 'Done'],
];
export const STATUS_LABEL = Object.fromEntries(STATUSES);
export const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
export const TYPES = ['TASK', 'BUG', 'STORY'];
export const ROLES = ['ADMIN', 'PROJECT_MANAGER', 'TEAM_MEMBER'];

const PRIORITY_CLASS = {
  LOW: 'bg-slate-100 text-slate-700',
  MEDIUM: 'bg-blue-100 text-blue-800',
  HIGH: 'bg-amber-100 text-amber-800',
  CRITICAL: 'bg-red-100 text-red-800',
};
const TYPE_CLASS = { BUG: 'bg-red-50 text-red-700', TASK: 'bg-sky-50 text-sky-700', STORY: 'bg-green-50 text-green-700' };

export const Priority = ({ p }) => <span className={`badge ${PRIORITY_CLASS[p]}`}>{p}</span>;
export const Type = ({ t }) => <span className={`badge ${TYPE_CLASS[t]}`}>{t}</span>;

export function Avatar({ user, large }) {
  if (!user) return null;
  const url = avatarUrl(user);
  const cls = `inline-flex ${large ? 'h-20 w-20 text-xl' : 'h-7 w-7'} shrink-0 items-center justify-center overflow-hidden rounded-full bg-indigo-100 text-xs font-semibold text-indigo-700`;
  return (
    <span className={cls} title={user.fullName}>
      {url ? <img src={url} alt={user.fullName} className="h-full w-full object-cover" /> : initials(user.fullName)}
    </span>
  );
}

const initials = (n = '') => n.split(' ').map((w) => w[0]).slice(0, 2).join('').toUpperCase();

export const ErrorText = ({ error }) => (error ? <p className="rounded bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p> : null);

export function Modal({ onClose, children, wide }) {
  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-black/40 p-4" onClick={onClose}>
      <div className={`card my-8 w-full ${wide ? 'max-w-4xl' : 'max-w-lg'}`} onClick={(e) => e.stopPropagation()} role="dialog">
        {children}
      </div>
    </div>
  );
}

export const fmtDate = (d) => (d ? new Date(d).toLocaleString() : '');
