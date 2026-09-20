package com.exa.ws;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/**
 * Prévient les clients connectés qu'un message a changé (le client recharge ensuite via REST).
 * Chaque session est associée au matricule de l'utilisateur connecté (passé en query param
 * ?matricule=XXX à l'ouverture), ce qui permet de fermer sa session dès qu'il se déconnecte
 * (voir AuthResource.logout -> MessageSocket.disconnectUser).
 */
@ServerEndpoint("/ws/messages")
public class MessageSocket {
    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();
    // Un même utilisateur peut avoir plusieurs onglets/sessions ouverts simultanément
    private static final Map<String, Set<Session>> SESSIONS_BY_USER = new ConcurrentHashMap<>();

    @OnOpen
    public void onOpen(Session session) {
        SESSIONS.add(session);
        String matricule = matriculeOf(session);
        if (matricule != null) {
            SESSIONS_BY_USER.computeIfAbsent(matricule, key -> new CopyOnWriteArraySet<>()).add(session);
        }
    }

    @OnClose
    public void onClose(Session session) {
        removeSession(session);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        removeSession(session);
    }

    private void removeSession(Session session) {
        SESSIONS.remove(session);
        String matricule = matriculeOf(session);
        if (matricule != null) {
            SESSIONS_BY_USER.computeIfPresent(matricule, (key, sessions) -> {
                sessions.remove(session);
                return sessions.isEmpty() ? null : sessions;
            });
        }
    }

    private String matriculeOf(Session session) {
        List<String> values = session.getRequestParameterMap().get("matricule");
        return (values == null || values.isEmpty()) ? null : values.get(0);
    }

    /** type : CREATED, UPDATED, DELETED, REPORTED, HIDDEN. */
    public static void broadcast(String type, int id) {
        String json = "{\"type\":\"" + type + "\",\"id\":" + id + "}";
        for (Session session : SESSIONS) {
            synchronized (session) {
                try {
                    if (session.isOpen()) session.getBasicRemote().sendText(json);
                } catch (IOException | RuntimeException exception) {
                    SESSIONS.remove(session);
                }
            }
        }
    }

    /** Ferme immédiatement toutes les sessions WS ouvertes par cet utilisateur (appelé au logout). */
    public static void disconnectUser(String matricule) {
        if (matricule == null) return;
        Set<Session> sessions = SESSIONS_BY_USER.remove(matricule);
        if (sessions == null) return;
        for (Session session : sessions) {
            SESSIONS.remove(session);
            try {
                if (session.isOpen()) {
                    session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "Déconnexion"));
                }
            } catch (IOException ignored) {
                // session déjà fermée côté client, rien à faire
            }
        }
    }
}