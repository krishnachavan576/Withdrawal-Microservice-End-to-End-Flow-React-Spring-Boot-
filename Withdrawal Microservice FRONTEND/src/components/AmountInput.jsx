import { useId } from "react";

export const AMOUNT_RE = /^\d{1,9}(\.\d{1,2})?$/;

export default function AmountInput({ value, onChange, error, disabled }) {
  const id = useId();
  return (
    <div className="field">
      <label htmlFor={id}>Amount to withdraw</label>
      <div className={`amount ${error ? "has-error" : ""}`}>
        <span aria-hidden="true">₹</span>
        <input
          id={id}
          inputMode="decimal"
          autoComplete="off"
          placeholder="0.00"
          value={value}
          disabled={disabled}
          aria-invalid={!!error}
          aria-describedby={error ? `${id}-err` : undefined}
          onChange={(e) => onChange(e.target.value.replace(/[^\d.]/g, ""))}
        />
      </div>
      {error && (
        <p id={`${id}-err`} className="field-error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
