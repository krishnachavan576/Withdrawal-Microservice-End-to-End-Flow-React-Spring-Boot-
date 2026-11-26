import { useCallback, useEffect, useRef, useState } from "react";
import api from "../api/client";
import { friendlyError } from "../api/errors";

const POLL_MS = 3000;
const POLL_MAX = 40; // about 2 minutes, then we tell the user it is still processing

/**
 * phase: idle | submitting | pending | success | failed | error
 *
 * Idempotency: one UUID per (account, amount) attempt. If the network drops and the user taps again,
 * the SAME key is sent, so the backend returns the original result instead of debiting twice.
 */
export default function useWithdraw(onSettled) {
  const [phase, setPhase] = useState("idle");
  const [txn, setTxn] = useState(null);
  const [error, setError] = useState(null);
  const [slow, setSlow] = useState(false);
  const keyRef = useRef({ sig: null, key: null });
  const pollRef = useRef(null);

  const stopPolling = () => {
    clearInterval(pollRef.current);
    pollRef.current = null;
  };
  useEffect(() => stopPolling, []);

  const resetKey = () => {
    keyRef.current = { sig: null, key: null };
  };

  const settle = useCallback(
    (t) => {
      setTxn(t);
      if (t.status === "PENDING") {
        setPhase("pending");
        return false;
      }
      setPhase(t.status === "SUCCESS" ? "success" : "failed");
      resetKey();
      onSettled?.(t);
      return true;
    },
    [onSettled]
  );

  const startPolling = useCallback(
    (id) => {
      stopPolling();
      setSlow(false);
      let attempts = 0;
      pollRef.current = setInterval(async () => {
        attempts += 1;
        try {
          const { data } = await api.get(`/withdrawals/${id}`);
          if (settle(data)) stopPolling();
        } catch {
          /* transient network error: keep polling */
        }
        if (attempts >= POLL_MAX) {
          stopPolling();
          setSlow(true);
        }
      }, POLL_MS);
    },
    [settle]
  );

  const submit = useCallback(
    async ({ accountId, amount }) => {
      const [whole, frac = ""] = amount.trim().split(".");
      const normalized = `${parseInt(whole, 10)}.${frac.padEnd(2, "0")}`;

      const sig = `${accountId}:${normalized}`;
      if (keyRef.current.sig !== sig) keyRef.current = { sig, key: crypto.randomUUID() };

      setPhase("submitting");
      setError(null);
      try {
        const { data } = await api.post(
          "/withdrawals",
          { accountId, amount: normalized },
          { headers: { "Idempotency-Key": keyRef.current.key } }
        );
        const done = settle(data);
        if (!done) {
          onSettled?.(data); // balance was already debited: refresh it right away
          startPolling(data.transactionId);
        }
      } catch (e) {
        setError(friendlyError(e));
        setPhase("error");
        // A real answer from the server means the request is finished: next attempt gets a fresh key.
        // No response (timeout / offline) means the result is unknown: keep the key so a retry is safe.
        if (e.response) resetKey();
      }
    },
    [settle, startPolling, onSettled]
  );

  const reset = useCallback(() => {
    stopPolling();
    setPhase("idle");
    setTxn(null);
    setError(null);
    setSlow(false);
  }, []);

  return { phase, txn, error, slow, submit, reset };
}
