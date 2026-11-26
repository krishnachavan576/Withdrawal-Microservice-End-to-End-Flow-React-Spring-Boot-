import { useCallback, useEffect, useState } from "react";
import api from "../api/client";
import { friendlyError, inr } from "../api/errors";
import useWithdraw from "../hooks/useWithdraw";
import AmountInput, { AMOUNT_RE } from "../components/AmountInput";
import ConfirmModal from "../components/ConfirmModal";
import ResultCard from "../components/ResultCard";
import BeneficiaryForm from "../components/BeneficiaryForm";
import TransactionList from "../components/TransactionList";

export default function WithdrawPage() {
  const [account, setAccount] = useState(null);
  const [history, setHistory] = useState([]);
  const [loadError, setLoadError] = useState(null);
  const [amount, setAmount] = useState("");
  const [touched, setTouched] = useState(false);
  const [confirming, setConfirming] = useState(false);

  const refresh = useCallback(async () => {
    try {
      const { data: accounts } = await api.get("/accounts");
      const acc = accounts[0] ?? null;
      setAccount(acc);
      if (acc) {
        const { data } = await api.get("/withdrawals", { params: { accountId: acc.id } });
        setHistory(data);
      }
    } catch (e) {
      setLoadError(friendlyError(e));
    }
  }, []);

  useEffect(() => { refresh(); }, [refresh]);

  const { phase, txn, error, slow, submit, reset } = useWithdraw(refresh);

  if (loadError) return <p className="field-error" role="alert">{loadError}</p>;
  if (!account) return <p className="muted">Loading your account…</p>;
  if (!account.hasBeneficiary) {
    return <BeneficiaryForm accountId={account.id} onLinked={setAccount} />;
  }

  const valid = AMOUNT_RE.test(amount) && Number(amount) >= 1;
  const tooMuch = valid && Number(amount) > Number(account.balance);
  const fieldError = !touched || !amount ? null
    : !valid ? "Enter an amount of ₹1 or more, with up to 2 decimals."
    : tooMuch ? `You can withdraw up to ${inr(account.balance)}.`
    : null;

  const busy = phase === "submitting" || phase === "pending";
  const canSubmit = valid && !tooMuch && !busy;

  const onConfirm = async () => {
    await submit({ accountId: account.id, amount });
    setConfirming(false);
  };

  const onDone = () => { reset(); setAmount(""); setTouched(false); };

  return (
    <div className="layout">
      <section className="balance" aria-label="Account balance">
        <p className="label">Available balance</p>
        <p className="money">{inr(account.balance)}</p>
        <p className="muted">Account {account.maskedAccountNumber}</p>
        <p className="muted">Daily limit {inr(account.dailyLimit)}</p>
      </section>

      <section aria-label="Withdraw">
        {(phase === "idle" || phase === "error" || phase === "submitting") && (
          <form
            className="stack"
            onSubmit={(e) => { e.preventDefault(); setTouched(true); if (canSubmit) setConfirming(true); }}
          >
            <AmountInput value={amount} onChange={(v) => { setAmount(v); setTouched(true); if (phase === "error") reset(); }} error={fieldError} disabled={busy} />
            <p className="muted">
              Money goes to {account.beneficiaryName}, account ending {account.beneficiaryLast4}.
            </p>
            <button type="submit" disabled={busy}>Review withdrawal</button>
          </form>
        )}
        <ResultCard phase={phase} txn={txn} error={error} slow={slow} onDone={onDone} />
      </section>

      <section className="history" aria-label="Recent withdrawals">
        <h2>Recent withdrawals</h2>
        <TransactionList items={history} />
      </section>

      <ConfirmModal
        open={confirming}
        amount={amount}
        account={account}
        submitting={phase === "submitting"}
        onCancel={() => setConfirming(false)}
        onConfirm={onConfirm}
      />
    </div>
  );
}
