import { useEffect, useState } from 'react';
import { api, download } from '../api';
import { ErrorText, PRIORITIES, STATUSES } from '../ui';

export default function Dashboard({ project, version }) {
  const [d, setD] = useState(null);
  const [error, setError] = useState('');
  useEffect(() => { api(`/api/projects/${project.id}/dashboard`).then(setD).catch((e) => setError(e.message)); }, [project.id, version]);

  const exportAs = (fmt) =>
    download(`/api/projects/${project.id}/export?format=${fmt}`, `${project.name}-report.${fmt}`).catch((e) => setError(e.message));

  if (error) return <ErrorText error={error} />;
  if (!d) return <p>Loading...</p>;

  return (
    <div className="space-y-4">
      <div className="flex gap-2">
        <button className="btn" onClick={() => exportAs('csv')}>Export CSV</button>
        <button className="btn" onClick={() => exportAs('pdf')}>Export PDF</button>
      </div>
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
        <Tile label="Total tasks" value={d.totalTasks} />
        <Tile label="Completed" value={`${d.doneTasks} (${d.completionPercent}%)`} />
        <Tile label="Estimated hours" value={d.estimatedHours.toFixed(1)} />
        <Tile label="Logged hours" value={d.loggedHours.toFixed(1)} />
      </div>
      <div className="grid gap-4 lg:grid-cols-2">
        <div className="card">
          <h3 className="mb-2 font-semibold">Burndown {d.burndown && <span className="text-sm font-normal text-slate-500">- {d.burndown.sprintName}</span>}</h3>
          <Burndown data={d.burndown} />
        </div>
        <div className="card space-y-4">
          <Bars title="By status" entries={STATUSES.map(([k, l]) => [l, d.byStatus[k]])} total={d.totalTasks} />
          <Bars title="By priority" entries={PRIORITIES.map((k) => [k, d.byPriority[k]])} total={d.totalTasks} />
        </div>
      </div>
    </div>
  );
}

const Tile = ({ label, value }) => (
  <div className="card">
    <p className="label">{label}</p>
    <p className="text-2xl font-bold">{value}</p>
  </div>
);

function Bars({ title, entries, total }) {
  return (
    <div>
      <h3 className="mb-2 font-semibold">{title}</h3>
      {entries.map(([label, n]) => (
        <div key={label} className="mb-1 flex items-center gap-2 text-sm">
          <span className="w-28 shrink-0">{label}</span>
          <div className="h-3 flex-1 rounded bg-slate-100">
            <div className="h-3 rounded bg-indigo-500" style={{ width: `${total ? (n / total) * 100 : 0}%` }} />
          </div>
          <span className="w-8 text-right tabular-nums">{n}</span>
        </div>
      ))}
    </div>
  );
}

function Burndown({ data }) {
  if (!data) return <p className="text-sm text-slate-500">Start a sprint to see its burndown.</p>;
  const W = 600, H = 240, P = 32;
  const pts = data.points;
  const max = Math.max(1, data.totalTasks);
  const x = (i) => P + (pts.length === 1 ? 0 : (i * (W - 2 * P)) / (pts.length - 1));
  const y = (v) => H - P - (v * (H - 2 * P)) / max;
  const line = (key) => pts.map((p, i) => (p[key] == null ? null : `${x(i)},${y(p[key])}`)).filter(Boolean).join(' ');
  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="w-full" role="img" aria-label="Sprint burndown chart">
      <line x1={P} y1={H - P} x2={W - P} y2={H - P} stroke="#cbd5e1" />
      <line x1={P} y1={P} x2={P} y2={H - P} stroke="#cbd5e1" />
      <text x={P - 6} y={P + 4} fontSize="11" textAnchor="end" fill="#64748b">{max}</text>
      <text x={P - 6} y={H - P} fontSize="11" textAnchor="end" fill="#64748b">0</text>
      <text x={P} y={H - 10} fontSize="11" fill="#64748b">{pts[0].date}</text>
      <text x={W - P} y={H - 10} fontSize="11" textAnchor="end" fill="#64748b">{pts[pts.length - 1].date}</text>
      <polyline points={line('ideal')} fill="none" stroke="#94a3b8" strokeDasharray="5 4" strokeWidth="1.5" />
      <polyline points={line('remaining')} fill="none" stroke="#4f46e5" strokeWidth="2.5" />
      <g fontSize="11">
        <line x1={W - 170} y1={P} x2={W - 150} y2={P} stroke="#4f46e5" strokeWidth="2.5" />
        <text x={W - 145} y={P + 4} fill="#334155">Remaining</text>
        <line x1={W - 85} y1={P} x2={W - 65} y2={P} stroke="#94a3b8" strokeDasharray="5 4" />
        <text x={W - 60} y={P + 4} fill="#334155">Ideal</text>
      </g>
    </svg>
  );
}
