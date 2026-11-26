// Backend error code -> message a customer can act on.
const MESSAGES = {
  INSUFFICIENT_FUNDS: "Your balance is too low for this withdrawal.",
  DAILY_LIMIT_EXCEEDED: "This would pass your daily withdrawal limit. Try a smaller amount, or try again tomorrow.",
  INVALID_AMOUNT: "That amount is above the per-withdrawal maximum. Enter a smaller amount.",
  NO_BENEFICIARY: "Add the bank account you want the money sent to first.",
  ACCOUNT_INACTIVE: "This account is not active, so withdrawals are turned off.",
  FORBIDDEN: "This account is not linked to your login.",
  IDEMPOTENCY_CONFLICT: "That request was already sent with a different amount. Start a new withdrawal.",
  PAYOUT_FAILED: "The bank transfer did not go through. Your money was returned to your balance.",
  BENEFICIARY_FAILED: "We could not verify that bank account. Check the account number and IFSC, then try again.",
  INVALID_CREDENTIALS: "Wrong username or password.",
  UNAUTHORIZED: "Your session ended. Log in again.",
};

export function friendlyError(err) {
  if (!err?.response) {
    return "Can't reach the server. Check your connection and try again. Retrying is safe: you won't be charged twice.";
  }
  const { code, message } = err.response.data || {};
  if (code === "VALIDATION_ERROR" && message) return message;
  return MESSAGES[code] || message || "Something went wrong. Please try again.";
}

export const inr = (value) =>
  new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(Number(value));
