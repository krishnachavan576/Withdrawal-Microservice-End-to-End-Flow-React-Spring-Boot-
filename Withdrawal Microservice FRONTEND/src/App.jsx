import { useEffect, useState } from "react";
import { session } from "./api/client";
import LoginPage from "./pages/LoginPage";
import WithdrawPage from "./pages/WithdrawPage";

export default function App() {
  const [authed, setAuthed] = useState(!!session.token());

  useEffect(() => {
    const onExpired = () => setAuthed(false);
    window.addEventListener("auth:expired", onExpired);
    return () => window.removeEventListener("auth:expired", onExpired);
  }, []);

  const logout = () => { session.clear(); setAuthed(false); };

  return (
    <>
      <header className="topbar">
        <span className="brand">Withdraw</span>
        {authed && (
          <span>
            {session.name()} <button type="button" className="link" onClick={logout}>Log out</button>
          </span>
        )}
      </header>
      <div className="page">
        {authed ? <WithdrawPage /> : <LoginPage onLogin={() => setAuthed(true)} />}
      </div>
    </>
  );
}
