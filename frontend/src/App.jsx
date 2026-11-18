import { createContext, useContext, useState } from 'react';
import { Link, Navigate, NavLink, Route, Routes, useNavigate } from 'react-router-dom';
import { Login, Register, Verify } from './pages/Auth';
import Projects from './pages/Projects';
import Project from './pages/Project';
import Profile from './pages/Profile';
import Admin from './pages/Admin';
import { Avatar } from './ui';

const AuthCtx = createContext(null);
export const useAuth = () => useContext(AuthCtx);

function readUser() {
  try {
    return JSON.parse(localStorage.getItem('user'));
  } catch {
    return null;
  }
}

export default function App() {
  const [user, setUserState] = useState(readUser);
  const setUser = (u, token) => {
    if (token) localStorage.setItem('token', token);
    if (u) localStorage.setItem('user', JSON.stringify(u));
    else localStorage.clear();
    setUserState(u);
  };

  return (
    <AuthCtx.Provider value={{ user, setUser }}>
      {user ? (
        <>
          <Nav />
          <main className="mx-auto max-w-7xl p-4">
            <Routes>
              <Route path="/" element={<Projects />} />
              <Route path="/projects/:id" element={<Project />} />
              <Route path="/profile" element={<Profile />} />
              <Route path="/verify" element={<Verify />} />
              {user.role === 'ADMIN' && <Route path="/admin" element={<Admin />} />}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </>
      ) : (
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/verify" element={<Verify />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      )}
    </AuthCtx.Provider>
  );
}

function Nav() {
  const { user, setUser } = useAuth();
  const nav = useNavigate();
  const link = ({ isActive }) => `text-sm ${isActive ? 'font-semibold text-indigo-700' : 'text-slate-600 hover:text-slate-900'}`;
  return (
    <header className="border-b border-slate-200 bg-white">
      <div className="mx-auto flex max-w-7xl items-center gap-6 px-4 py-3">
        <Link to="/" className="text-lg font-bold text-indigo-700">TaskFlow</Link>
        <NavLink to="/" end className={link}>Projects</NavLink>
        {user.role === 'ADMIN' && <NavLink to="/admin" className={link}>Admin</NavLink>}
        <div className="ml-auto flex items-center gap-3">
          <NavLink to="/profile" className="flex items-center gap-2 text-sm">
            <Avatar user={user} /> {user.fullName}
            <span className="badge bg-slate-100 text-slate-600">{user.role.replace('_', ' ')}</span>
          </NavLink>
          <button className="btn" onClick={() => { setUser(null); nav('/login'); }}>Log out</button>
        </div>
      </div>
    </header>
  );
}
