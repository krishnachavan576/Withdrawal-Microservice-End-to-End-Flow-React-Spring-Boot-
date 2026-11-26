import { useEffect, useRef } from "react";
import { inr } from "../api/errors";

// Native <dialog>: focus is trapped and Esc closes it without extra code.
export default function ConfirmModal({ open, amount, account, submitting, onCancel, onConfirm }) {
  const ref = useRef(null);

  useEffect(() => {
    const dlg = ref.current;
    if (!dlg) return;
    if (open && !dlg.open) dlg.showModal();
    if (!open && dlg.open) dlg.close();
  }, [open]);

  if (!account) return null;
  return (
    <dialog ref={ref} className="modal" onCancel={(e) => { e.preventDefault(); if (!submitting) onCancel(); }}>
      <h2>Confirm withdrawal</h2>
      <p className="modal-amount">{inr(amount)}</p>
      <dl className="summary">
        <dt>Send to</dt>
        <dd>
          {account.beneficiaryName}, account ending {account.beneficiaryLast4}
        </dd>
        <dt>Bank code</dt>
        <dd>{account.beneficiaryIfsc}</dd>
        <dt>Balance after</dt>
        <dd>{inr(Number(account.balance) - Number(amount))}</dd>
      </dl>
      <div className="actions">
        <button type="button" className="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
        <button type="button" onClick={onConfirm} disabled={submitting}>
          {submitting ? "Sending…" : `Withdraw ${inr(amount)}`}
        </button>
      </div>
    </dialog>
  );
}
