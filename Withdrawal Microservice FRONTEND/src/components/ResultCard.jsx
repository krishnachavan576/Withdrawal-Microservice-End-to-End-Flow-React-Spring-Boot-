import { inr } from "../api/errors";

export default function ResultCard({ phase, txn, error, slow, onDone }) {
  if (phase === "pending") {
    return (
      <div className="result" role="status">
        <h3>Withdrawal in progress</h3>
        <p>
          {inr(txn.amount)} has left your balance and is on its way to your bank. This usually takes a few seconds.
          {slow && " It is taking longer than usual. You can leave this page: we will keep trying and return the money if the transfer fails."}
        </p>
      </div>
    );
  }
  if (phase === "success") {
    return (
      <div className="result ok" role="status">
        <h3>{inr(txn.amount)} sent to your bank</h3>
        <p>Reference {txn.transactionId.slice(0, 8)}. Your new balance is {inr(txn.balanceAfter)}.</p>
        <button type="button" onClick={onDone}>Make another withdrawal</button>
      </div>
    );
  }
  if (phase === "failed") {
    return (
      <div className="result bad" role="alert">
        <h3>Withdrawal did not go through</h3>
        <p>{inr(txn.amount)} was returned to your balance. Nothing was sent to your bank.</p>
        <button type="button" onClick={onDone}>Try again</button>
      </div>
    );
  }
  if (phase === "error") {
    return (
      <div className="result bad" role="alert">
        <h3>Could not start the withdrawal</h3>
        <p>{error}</p>
      </div>
    );
  }
  return null;
}
