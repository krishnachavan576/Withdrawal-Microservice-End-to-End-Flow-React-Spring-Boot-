import { useState } from "react";
import api, { session } from "../api/client";
import { friendlyError } from "../api/errors";

export default function LoginPage({ onLogin }) {
  const [username, setUsername] = useState(import.meta.env.DEV ? "demo" : "");
  const [password, setPassword] = useState(import.meta.env.DEV ? "demo123" : "");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const { data } = await api.post("/auth/login", { username, password });
      session.save(data.token, data.fullName);
      onLogin();
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="login">
      <h1>Log in to withdraw</h1>
      <form onSubmit={submit} className="stack">
        <div className="field">
          <label htmlFor="u">Username</label>
          <input id="u" value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" required />
        </div>
        <div className="field">
          <label htmlFor="p">Password</label>
          <input id="p" type="password" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="current-password" required />
        </div>
        {error && <p className="field-error" role="alert">{error}</p>}
        <button type="submit" disabled={busy}>{busy ? "Logging in…" : "Log in"}</button>
      </form>
    </main>
  );
}
