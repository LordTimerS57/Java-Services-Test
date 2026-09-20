package com.exa.ws;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

/** Prévient les clients connectés qu'un message a changé (le client recharge ensuite via REST). */
@ServerEndpoint("/ws/messages")
public class MessageSocket {
    private static final Set<Session> SESSIONS = new CopyOnWriteArraySet<>();

    @OnOpen public void onOpen(Session session) { SESSIONS.add(session); }
    @OnClose public void onClose(Session session) { SESSIONS.remove(session); }
    @OnError public void onError(Session session, Throwable error) { SESSIONS.remove(session); }

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
}