import { useCallback, useEffect, useState } from 'react';
import { api } from '../api';
import { Avatar, ErrorText, Modal, PRIORITIES, Priority, STATUSES, Type } from '../ui';
import { TaskForm, toRequest } from './TaskModal';

function useTasks(projectId, query, version) {
  const [tasks, setTasks] = useState([]);
  const [error, setError] = useState('');
  const load = useCallback(() => {
    api(`/api/projects/${projectId}/tasks?${query}`).then(setTasks).catch((e) => setError(e.message));
  }, [projectId, query]);
  useEffect(() => { load(); }, [load, version]);
  return { tasks, error, setError, load };
}

function NewTask({ project, sprints, defaultSprintId, onDone }) {
  const [open, setOpen] = useState(false);
  const [error, setError] = useState('');
  if (project.archived) return null;
  const submit = async (body) => {
    try {
      await api(`/api/projects/${project.id}/tasks`, { method: 'POST', body });
      setOpen(false);
      onDone();
    } catch (e) {
      setError(e.message);
    }
  };
  return (
    <>
      <button className="btn btn-primary" onClick={() => setOpen(true)}>+ New task</button>
      {open && (
        <Modal onClose={() => setOpen(false)}>
          <h2 className="mb-3 text-lg font-semibold">New task</h2>
          <ErrorText error={error} />
          <TaskForm project={project} sprints={sprints} initial={{ sprintId: defaultSprintId }} onSubmit={submit} onCancel={() => setOpen(false)} />
        </Modal>
      )}
    </>
  );
}

function TaskCard({ task, openTask, onStatus }) {
  return (
    <div draggable onDragStart={(e) => e.dataTransfer.setData('text/plain', String(task.id))}
      onClick={() => openTask(task.id)}
      className="cursor-pointer rounded-md border border-slate-200 bg-white p-3 text-sm shadow-sm hover:border-indigo-300">
      <div className="mb-1 flex items-center gap-1">
        <Type t={task.type} /> <Priority p={task.priority} />
        <span className="ml-auto text-xs text-slate-400">#{task.id}</span>
      </div>
      <p className="font-medium">{task.title}</p>
      {task.labels && <p className="mt-1 text-xs text-slate-500">{task.labels}</p>}
      <div className="mt-2 flex items-center gap-2">
        <Avatar user={task.assignee} />
        <select className="ml-auto rounded border border-slate-200 text-xs" value={task.status} aria-label="Status"
          onClick={(e) => e.stopPropagation()} onChange={(e) => onStatus(task.id, e.target.value)}>
          {STATUSES.map(([k, l]) => <option key={k} value={k}>{l}</option>)}
        </select>
      </div>
    </div>
  );
}

export function Board({ project, sprints, version, openTask }) {
  const active = sprints.find((s) => s.status === 'ACTIVE');
  const [f, setF] = useState({ sprint: active ? String(active.id) : '', assigneeId: '', priority: '', label: '', q: '' });
  const [over, setOver] = useState(null);

  const p = new URLSearchParams();
  if (f.sprint === 'backlog') p.set('backlog', 'true');
  else if (f.sprint) p.set('sprintId', f.sprint);
  ['assigneeId', 'priority', 'label', 'q'].forEach((k) => f[k] && p.set(k, f[k]));
  const { tasks, error, setError, load } = useTasks(project.id, p.toString(), version);

  const move = async (id, status) => {
    try {
      await api(`/api/tasks/${id}/status`, { method: 'PATCH', body: { status } });
      load();
    } catch (e) {
      setError(e.message);
    }
  };
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value });

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-end gap-2">
        <select className="input w-auto" value={f.sprint} onChange={set('sprint')} aria-label="Sprint">
          <option value="">All sprints</option>
          <option value="backlog">Backlog</option>
          {sprints.map((s) => <option key={s.id} value={s.id}>{s.name} ({s.status.toLowerCase()})</option>)}
        </select>
        <select className="input w-auto" value={f.assigneeId} onChange={set('assigneeId')} aria-label="Assignee">
          <option value="">Any assignee</option>
          {project.members.map((m) => <option key={m.id} value={m.id}>{m.fullName}</option>)}
        </select>
        <select className="input w-auto" value={f.priority} onChange={set('priority')} aria-label="Priority">
          <option value="">Any priority</option>
          {PRIORITIES.map((x) => <option key={x}>{x}</option>)}
        </select>
        <input className="input w-36" placeholder="Label" value={f.label} onChange={set('label')} />
        <input className="input w-48" placeholder="Search title..." value={f.q} onChange={set('q')} />
        <div className="ml-auto">
          <NewTask project={project} sprints={sprints} defaultSprintId={f.sprint && f.sprint !== 'backlog' ? Number(f.sprint) : null} onDone={load} />
        </div>
      </div>
      <ErrorText error={error} />
      <div className="grid gap-3 md:grid-cols-4">
        {STATUSES.map(([status, label]) => {
          const col = tasks.filter((t) => t.status === status);
          return (
            <section key={status}
              onDragOver={(e) => { e.preventDefault(); setOver(status); }}
              onDragLeave={() => setOver(null)}
              onDrop={(e) => { e.preventDefault(); setOver(null); move(e.dataTransfer.getData('text/plain'), status); }}
              className={`min-h-64 space-y-2 rounded-lg p-2 ${over === status ? 'bg-indigo-50 ring-2 ring-indigo-300' : 'bg-slate-100'}`}>
              <h3 className="px-1 text-xs font-semibold uppercase text-slate-500">{label} ({col.length})</h3>
              {col.map((t) => <TaskCard key={t.id} task={t} openTask={openTask} onStatus={move} />)}
            </section>
          );
        })}
      </div>
    </div>
  );
}

export function Backlog({ project, sprints, version, refresh, openTask, canManage }) {
  const { tasks, error, setError, load } = useTasks(project.id, '', version);
  const [form, setForm] = useState({ name: '', goal: '', startDate: '', endDate: '' });
  const open = sprints.filter((s) => s.status !== 'COMPLETED');
  const ordered = [...sprints].sort((a, b) => ({ ACTIVE: 0, PLANNED: 1, COMPLETED: 2 })[a.status] - ({ ACTIVE: 0, PLANNED: 1, COMPLETED: 2 })[b.status]);

  const run = async (fn) => {
    setError('');
    try { await fn(); refresh(); } catch (e) { setError(e.message); }
  };
  const moveTo = (t, sprintId) => run(() => api(`/api/tasks/${t.id}`, { method: 'PUT', body: { ...toRequest(t), sprintId: sprintId ? Number(sprintId) : null } }));

  const rows = (list) => (
    <ul className="divide-y divide-slate-100">
      {list.map((t) => (
        <li key={t.id} className="flex items-center gap-2 py-1.5 text-sm">
          <Type t={t.type} /> <Priority p={t.priority} />
          <button className="truncate text-left hover:underline" onClick={() => openTask(t.id)}>#{t.id} {t.title}</button>
          <span className="badge bg-slate-100 text-slate-600">{t.status}</span>
          <Avatar user={t.assignee} />
          {!project.archived && (
            <select className="ml-auto rounded border border-slate-200 text-xs" value={t.sprintId || ''} aria-label="Move to sprint"
              onChange={(e) => moveTo(t, e.target.value)}>
              <option value="">Backlog</option>
              {open.map((s) => <option key={s.id} value={s.id}>{s.name}</option>)}
            </select>
          )}
        </li>
      ))}
      {list.length === 0 && <li className="py-1.5 text-sm text-slate-400">No tasks</li>}
    </ul>
  );

  return (
    <div className="space-y-4">
      <ErrorText error={error} />
      {canManage && !project.archived && (
        <form className="card flex flex-wrap items-end gap-2"
          onSubmit={(e) => { e.preventDefault(); run(async () => { await api(`/api/projects/${project.id}/sprints`, { method: 'POST', body: { ...form, startDate: form.startDate || null, endDate: form.endDate || null } }); setForm({ name: '', goal: '', startDate: '', endDate: '' }); }); }}>
          <div className="flex-1"><label className="label">Sprint name</label><input className="input" required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></div>
          <div className="flex-[2]"><label className="label">Goal</label><input className="input" value={form.goal} onChange={(e) => setForm({ ...form, goal: e.target.value })} /></div>
          <div><label className="label">Start</label><input className="input" type="date" value={form.startDate} onChange={(e) => setForm({ ...form, startDate: e.target.value })} /></div>
          <div><label className="label">End</label><input className="input" type="date" value={form.endDate} onChange={(e) => setForm({ ...form, endDate: e.target.value })} /></div>
          <button className="btn btn-primary">Create sprint</button>
        </form>
      )}
      {ordered.map((s) => (
        <div key={s.id} className="card">
          <div className="mb-2 flex items-center gap-2">
            <h3 className="font-semibold">{s.name}</h3>
            <span className={`badge ${s.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-slate-100 text-slate-600'}`}>{s.status}</span>
            <span className="text-xs text-slate-500">{s.startDate || '?'} → {s.endDate || '?'}</span>
            {s.goal && <span className="text-xs italic text-slate-500">{s.goal}</span>}
            {canManage && !project.archived && s.status === 'PLANNED' && (
              <button className="btn ml-auto" onClick={() => run(() => api(`/api/sprints/${s.id}/start`, { method: 'POST' }))}>Start sprint</button>
            )}
            {canManage && !project.archived && s.status === 'ACTIVE' && (
              <button className="btn ml-auto" onClick={() => run(() => api(`/api/sprints/${s.id}/complete`, { method: 'POST' }))}>Complete sprint</button>
            )}
          </div>
          {rows(tasks.filter((t) => t.sprintId === s.id))}
        </div>
      ))}
      <div className="card">
        <div className="mb-2 flex items-center">
          <h3 className="font-semibold">Backlog</h3>
          <div className="ml-auto"><NewTask project={project} sprints={sprints} defaultSprintId={null} onDone={load} /></div>
        </div>
        {rows(tasks.filter((t) => !t.sprintId))}
      </div>
    </div>
  );
}
