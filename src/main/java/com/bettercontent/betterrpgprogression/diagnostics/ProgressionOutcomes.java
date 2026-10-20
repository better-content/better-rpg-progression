package com.bettercontent.betterrpgprogression.diagnostics;

/** Scalar projections of existing allocation/entitlement decisions, never a gameplay policy. */
public final class ProgressionOutcomes {
    private ProgressionOutcomes() {}
    public static String manual(boolean policyAccepted, int changedStats) {
        if (!policyAccepted) return "rejected";
        return changedStats > 0 ? "committed" : changedStats == 0 ? "unchanged" : "unknown";
    }
    public static String entitlement(boolean inserted) { return inserted ? "enqueued" : "unchanged"; }
}
