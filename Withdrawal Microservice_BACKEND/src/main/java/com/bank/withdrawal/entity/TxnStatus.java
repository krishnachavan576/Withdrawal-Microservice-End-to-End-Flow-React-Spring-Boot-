package com.bank.withdrawal.entity;

/** PENDING = balance debited, payout not finished. SUCCESS = money reached bank. FAILED = refunded. */
public enum TxnStatus { PENDING, SUCCESS, FAILED }
