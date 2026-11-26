import { useState } from "react";
import api from "../api/client";
import { friendlyError } from "../api/errors";

export default function BeneficiaryForm({ accountId, onLinked }) {
  const [form, setForm] = useState({ name: "", ifsc: "", accountNumber: "" });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

  const set = (k) => (e) => {
    const v = e.target.value;
    setForm((f) => ({ ...f, [k]: k === "ifsc" ? v.toUpperCase() : v }));
  };

  const submit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const { data } = await api.post(`/accounts/${accountId}/beneficiary`, form);
      onLinked(data);
    } catch (err) {
      setError(friendlyError(err));
    } finally {
      setBusy(false);
    }
  };

  return (
    <form onSubmit={submit} className="stack">
      <h2>Add the bank account to receive money</h2>
      <div className="field">
        <label htmlFor="b-name">Name on the bank account</label>
        <input id="b-name" value={form.name} onChange={set("name")} required maxLength={100} autoComplete="name" />
      </div>
      <div className="field">
        <label htmlFor="b-ifsc">IFSC code</label>
        <input id="b-ifsc" value={form.ifsc} onChange={set("ifsc")} required maxLength={11} placeholder="HDFC0001234" />
      </div>
      <div className="field">
        <label htmlFor="b-acc">Account number</label>
        <input id="b-acc" value={form.accountNumber} onChange={set("accountNumber")} required inputMode="numeric"
               pattern="[0-9]{9,18}" title="9 to 18 digits" autoComplete="off" />
      </div>
      {error && <p className="field-error" role="alert">{error}</p>}
      <button type="submit" disabled={busy}>{busy ? "Verifying…" : "Save bank account"}</button>
    </form>
  );
}
