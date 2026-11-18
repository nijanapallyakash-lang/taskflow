import { useCallback, useEffect, useState } from 'react';
import { api, download } from '../api';
import { useAuth } from '../App';
import { Avatar, ErrorText, fmtDate, Modal, PRIORITIES, Priority, STATUSES, Type, TYPES } from '../ui';

export const toRequest = (t) => ({
  title: t.title,
  description: t.description,
  type: t.type,
  priority: t.priority,
  assigneeId: t.assignee?.id ?? null,
  sprintId: t.sprintId ?? null,
  labels: t.labels,
  estimatedHours: t.estimatedHours,
});

export function TaskForm({ project, sprints, initial = {}, onSubmit, onCancel, hideSprint }) {
  const [f, setF] = useState({
    title: '', description: '', type: 'TASK', priority: 'MEDIUM', assigneeId: '', sprintId: '', labels: '', estimatedHours: '',
    ...Object.fromEntries(Object.entries(initial).map(([k, v]) => [k, v ?? ''])),
  });
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });
  const submit = (e) => {
    e.preventDefault();
    onSubmit({
      ...f,
      assigneeId: f.assigneeId ? Number(f.assigneeId) : null,
      sprintId: f.sprintId ? Number(f.sprintId) : null,
      estimatedHours: f.estimatedHours === '' ? null : Number(f.estimatedHours),
    });
  };
  return (
    <form onSubmit={submit} className="space-y-3">
      <input className="input" placeholder="Title" required maxLength={255} value={f.title} onChange={set('title')} />
      <textarea className="input" rows={4} placeholder="Description" value={f.description} onChange={set('description')} />
      <div className="grid grid-cols-2 gap-2">
        <div><label className="label">Type</label><select className="input" value={f.type} onChange={set('type')}>{TYPES.map((t) => <option key={t}>{t}</option>)}</select></div>
        <div><label className="label">Priority</label><select className="input" value={f.priority} onChange={set('priority')}>{PRIORITIES.map((p) => <option key={p}>{p}</option>)}</select></div>
        <div>
          <label className="label">Assignee</label>
          <select className="input" value={f.assigneeId} onChange={set('assigneeId')}>
            <option value="">Unassigned</option>
            {project.members.map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}
          </select>
        </div>
        {!hideSprint && (
          <div>
            <label className="label">Sprint</label>
            <select className="input" value={f.sprintId} onChange={set('sprintId')}>
              <option value="">Backlog</option>
              {sprints.filter((s) => s.status !== 'COMPLETED').map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          </div>
        )}
        <div><label className="label">Labels (comma separated)</label><input className="input" value={f.labels} onChange={set('labels')} /></div>
        <div><label className="label">Estimate (hours)</label><input className="input" type="number" min="0" step="0.5" value={f.estimatedHours} onChange={set('estimatedHours')} /></div>
      </div>
      <div className="flex justify-end gap-2">
        {onCancel && <button type="button" className="btn" onClick={onCancel}>Cancel</button>}
        <button className="btn btn-primary">Save</button>
      </div>
    </form>
  );
}

export default function TaskModal({ id, project, sprints, refresh, openTask, onClose }) {
  const { user } = useAuth();
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  const [editing, setEditing] = useState(false);
  const [addingSub, setAddingSub] = useState(false);
  const [comment, setComment] = useState('');
  const [check, setCheck] = useState('');
  const [work, setWork] = useState({ hours: '', note: '', logDate: '' });
  const readOnly = project.archived;

  const load = useCallback(() => api(`/api/tasks/${id}`).then(setD).catch((e) => setError(e.message)), [id]);
  useEffect(() => {
    load();
    // ponytail: 5s polling keeps the discussion live; switch to WebSocket/STOMP if teams need instant updates.
    const timer = setInterval(load, 5000);
    return () => clearInterval(timer);
  }, [load]);

  const run = async (fn) => {
    setError('');
    try { await fn(); await load(); refresh(); } catch (e) { setError(e.message); }
  };

  if (!d) return <Modal onClose={onClose} wide><ErrorText error={error} />{!error && 'Loading...'}</Modal>;
  const t = d.task;
  const doneItems = d.checklist.filter((c) => c.done).length;

  return (
    <Modal onClose={onClose} wide>
      <div className="mb-3 flex items-start gap-2">
        <div className="flex-1">
          <div className="flex items-center gap-1 text-xs text-slate-500">
            <Type t={t.type} /> <Priority p={t.priority} /> #{t.id}
            {t.parentId && <button className="underline" onClick={() => openTask(t.parentId)}>parent #{t.parentId}</button>}
          </div>
          <h2 className="text-xl font-semibold">{t.title}</h2>
        </div>
        <button className="btn" onClick={onClose} aria-label="Close">✕</button>
      </div>
      <ErrorText error={error} />

      {editing ? (
        <TaskForm project={project} sprints={sprints} initial={toRequest(t)} hideSprint={!!t.parentId}
          onCancel={() => setEditing(false)}
          onSubmit={(body) => run(async () => { await api(`/api/tasks/${t.id}`, { method: 'PUT', body }); setEditing(false); })} />
      ) : (
        <div className="grid gap-4 md:grid-cols-3">
          <div className="space-y-5 md:col-span-2">
            <p className="whitespace-pre-wrap text-sm">{t.description || <span className="text-slate-400">No description</span>}</p>

            {!t.parentId && (
              <section>
                <h3 className="label">Sub-tasks</h3>
                <ul className="space-y-1">
                  {d.subtasks.map((s) => (
                    <li key={s.id} className="flex items-center gap-2 text-sm">
                      <span className={`badge ${s.status === 'DONE' ? 'bg-green-100 text-green-800' : 'bg-slate-100'}`}>{s.status}</span>
                      <button className="hover:underline" onClick={() => openTask(s.id)}>#{s.id} {s.title}</button>
                      <Avatar user={s.assignee} />
                    </li>
                  ))}
                </ul>
                {!readOnly && (addingSub ? (
                  <div className="mt-2 rounded border border-slate-200 p-3">
                    <TaskForm project={project} sprints={sprints} hideSprint onCancel={() => setAddingSub(false)}
                      onSubmit={(body) => run(async () => { await api(`/api/tasks/${t.id}/subtasks`, { method: 'POST', body }); setAddingSub(false); })} />
                  </div>
                ) : <button className="mt-1 text-sm text-indigo-700" onClick={() => setAddingSub(true)}>+ Add sub-task</button>)}
              </section>
            )}

            <section>
              <h3 className="label">Checklist {d.checklist.length > 0 && `(${doneItems}/${d.checklist.length})`}</h3>
              <ul className="space-y-1">
                {d.checklist.map((c) => (
                  <li key={c.id} className="group flex items-center gap-2 text-sm">
                    <input type="checkbox" checked={c.done} disabled={readOnly}
                      onChange={() => run(() => api(`/api/checklist/${c.id}/toggle`, { method: 'PATCH' }))} />
                    <span className={c.done ? 'text-slate-400 line-through' : ''}>{c.text}</span>
                    {!readOnly && <button className="invisible text-xs text-red-600 group-hover:visible" onClick={() => run(() => api(`/api/checklist/${c.id}`, { method: 'DELETE' }))}>delete</button>}
                  </li>
                ))}
              </ul>
              {!readOnly && (
                <form className="mt-1 flex gap-2" onSubmit={(e) => { e.preventDefault(); run(async () => { await api(`/api/tasks/${t.id}/checklist`, { method: 'POST', body: { text: check } }); setCheck(''); }); }}>
                  <input className="input" placeholder="Add checklist item" required value={check} onChange={(e) => setCheck(e.target.value)} />
                  <button className="btn">Add</button>
                </form>
              )}
            </section>

            <section>
              <h3 className="label">Attachments</h3>
              <ul className="space-y-1">
                {d.attachments.map((a) => (
                  <li key={a.id} className="flex items-center gap-2 text-sm">
                    <button className="text-indigo-700 hover:underline" onClick={() => download(`/api/attachments/${a.id}`, a.fileName).catch((e) => setError(e.message))}>{a.fileName}</button>
                    <span className="text-xs text-slate-500">{(a.size / 1024).toFixed(1)} KB · {a.uploadedBy.fullName}</span>
                    {!readOnly && <button className="text-xs text-red-600" onClick={() => run(() => api(`/api/attachments/${a.id}`, { method: 'DELETE' }))}>delete</button>}
                  </li>
                ))}
              </ul>
              {!readOnly && (
                <input type="file" className="mt-1 text-sm" aria-label="Upload attachment" onChange={(e) => {
                  const file = e.target.files[0];
                  if (!file) return;
                  const fd = new FormData();
                  fd.append('file', file);
                  e.target.value = '';
                  run(() => api(`/api/tasks/${t.id}/attachments`, { method: 'POST', body: fd }));
                }} />
              )}
            </section>

            <section>
              <h3 className="label">Discussion</h3>
              <ul className="space-y-3">
                {d.comments.map((c) => (
                  <li key={c.id} className="flex gap-2 text-sm">
                    <Avatar user={c.author} />
                    <div className="flex-1">
                      <p><b>{c.author.fullName}</b> <span className="text-xs text-slate-500">{fmtDate(c.createdAt)}</span>
                        {(c.author.id === user.id || user.role === 'ADMIN') && <button className="ml-2 text-xs text-red-600" onClick={() => run(() => api(`/api/comments/${c.id}`, { method: 'DELETE' }))}>delete</button>}
                      </p>
                      <p className="whitespace-pre-wrap">{c.body}</p>
                    </div>
                  </li>
                ))}
              </ul>
              {!readOnly && (
                <form className="mt-2 space-y-2" onSubmit={(e) => { e.preventDefault(); run(async () => { await api(`/api/tasks/${t.id}/comments`, { method: 'POST', body: { body: comment } }); setComment(''); }); }}>
                  <textarea className="input" rows={2} placeholder="Write a comment..." required value={comment} onChange={(e) => setComment(e.target.value)} />
                  <button className="btn">Comment</button>
                </form>
              )}
            </section>
          </div>

          <aside className="space-y-4 text-sm">
            <div>
              <label className="label">Status</label>
              <select className="input" value={t.status} disabled={readOnly}
                onChange={(e) => run(() => api(`/api/tasks/${t.id}/status`, { method: 'PATCH', body: { status: e.target.value } }))}>
                {STATUSES.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
              </select>
            </div>
            <dl className="space-y-1">
              <div className="flex justify-between"><dt className="text-slate-500">Assignee</dt><dd>{t.assignee?.fullName || '-'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Reporter</dt><dd>{t.reporter.fullName}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Sprint</dt><dd>{sprints.find((s) => s.id === t.sprintId)?.name || 'Backlog'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Labels</dt><dd>{t.labels || '-'}</dd></div>
              <div className="flex justify-between"><dt className="text-slate-500">Created</dt><dd>{fmtDate(t.createdAt)}</dd></div>
            </dl>

            <div className="rounded border border-slate-200 p-3">
              <h3 className="label">Time tracking</h3>
              <p>Estimated: <b>{t.estimatedHours ?? 0}h</b> · Logged: <b>{d.loggedHours}h</b></p>
              {t.estimatedHours > 0 && (
                <div className="my-2 h-2 rounded bg-slate-100">
                  <div className={`h-2 rounded ${d.loggedHours > t.estimatedHours ? 'bg-red-500' : 'bg-green-500'}`}
                    style={{ width: `${Math.min(100, (d.loggedHours / t.estimatedHours) * 100)}%` }} />
                </div>
              )}
              {!readOnly && (
                <form className="mt-2 space-y-1" onSubmit={(e) => { e.preventDefault(); run(async () => { await api(`/api/tasks/${t.id}/worklogs`, { method: 'POST', body: { hours: Number(work.hours), note: work.note, logDate: work.logDate || null } }); setWork({ hours: '', note: '', logDate: '' }); }); }}>
                  <div className="flex gap-1">
                    <input className="input" type="number" min="0.25" max="24" step="0.25" placeholder="Hours" required value={work.hours} onChange={(e) => setWork({ ...work, hours: e.target.value })} />
                    <input className="input" type="date" value={work.logDate} onChange={(e) => setWork({ ...work, logDate: e.target.value })} aria-label="Date" />
                  </div>
                  <input className="input" placeholder="Note" value={work.note} onChange={(e) => setWork({ ...work, note: e.target.value })} />
                  <button className="btn w-full justify-center">Log work</button>
                </form>
              )}
              <ul className="mt-2 space-y-1 text-xs">
                {d.workLogs.map((w) => <li key={w.id}>{w.logDate} · <b>{w.hours}h</b> · {w.user.fullName} {w.note && `- ${w.note}`}</li>)}
              </ul>
            </div>

            <div>
              <h3 className="label">History</h3>
              <ul className="max-h-48 space-y-1 overflow-y-auto text-xs">
                {d.activity.map((a) => <li key={a.id}><b>{a.user.fullName}</b> {a.action.toLowerCase().replace('_', ' ')} {a.details} <span className="text-slate-400">{fmtDate(a.createdAt)}</span></li>)}
              </ul>
            </div>

            {!readOnly && (
              <div className="flex gap-2">
                <button className="btn flex-1 justify-center" onClick={() => setEditing(true)}>Edit</button>
                <button className="btn btn-danger" onClick={() => run(async () => { await api(`/api/tasks/${t.id}`, { method: 'DELETE' }); onClose(); })}>Delete</button>
              </div>
            )}
          </aside>
        </div>
      )}
    </Modal>
  );
}
