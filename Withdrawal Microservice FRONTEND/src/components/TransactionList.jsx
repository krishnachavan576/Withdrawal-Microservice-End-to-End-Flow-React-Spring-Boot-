import { inr } from "../api/errors";

const LABEL = { PENDING: "In progress", SUCCESS: "Sent", FAILED: "Returned" };

export default function TransactionList({ items }) {
  if (!items.length) {
    return <p className="muted">No withdrawals yet. Your first one will show up here.</p>;
  }
  return (
    <table className="ledger">
      <thead>
        <tr><th>Date</th><th>Status</th><th className="num">Amount</th></tr>
      </thead>
      <tbody>
        {items.map((t) => (
          <tr key={t.transactionId}>
            <td>{new Date(t.createdAt).toLocaleString("en-IN", { dateStyle: "medium", timeStyle: "short" })}</td>
            <td><span className={`status ${t.status.toLowerCase()}`}>{LABEL[t.status]}</span></td>
            <td className="num">{inr(t.amount)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
