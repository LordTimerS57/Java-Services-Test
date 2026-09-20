package com.exa.util;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Demandes de changement de mot de passe en attente de confirmation par email. */
public final class PasswordResetStore {
    public enum Status { OK, NONE, EXPIRED, INVALID, LOCKED }

    public static final class Outcome {
        public final Status status;
        public final String passwordHash;
        Outcome(Status status, String passwordHash) { this.status = status; this.passwordHash = passwordHash; }
    }

    private static final class Pending {
        String codeHash; String passwordHash; long createdAt; long expiresAt; int attempts;
    }

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final long RESEND_MS = 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final Map<String, Pending> PENDING = new ConcurrentHashMap<>();
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordResetStore() { }

    /** Retourne le code à envoyer, ou null si une demande a été faite il y a moins d'une minute. */
    public static synchronized String create(String matricule, String newPassword) {
        long now = System.currentTimeMillis();
        Pending old = PENDING.get(matricule);
        if (old != null && now - old.createdAt < RESEND_MS) return null;
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        Pending p = new Pending();
        p.codeHash = PasswordUtil.hash(code);
        p.passwordHash = PasswordUtil.hash(newPassword);
        p.createdAt = now;
        p.expiresAt = now + TTL_MS;
        PENDING.put(matricule, p);
        return code;
    }

    public static void cancel(String matricule) { PENDING.remove(matricule); }

    public static synchronized Outcome verify(String matricule, String code) {
        Pending p = PENDING.get(matricule);
        if (p == null) return new Outcome(Status.NONE, null);
        if (System.currentTimeMillis() > p.expiresAt) { PENDING.remove(matricule); return new Outcome(Status.EXPIRED, null); }
        if (++p.attempts > MAX_ATTEMPTS) { PENDING.remove(matricule); return new Outcome(Status.LOCKED, null); }
        if (!PasswordUtil.matches(code, p.codeHash)) return new Outcome(Status.INVALID, null);
        PENDING.remove(matricule);
        return new Outcome(Status.OK, p.passwordHash);
    }
}