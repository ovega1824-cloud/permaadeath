package com.permadeath.client;

import com.permadeath.Change;

import java.util.ArrayList;
import java.util.List;

/** Puntos de entrada del lado cliente, llamados desde los paquetes de red. */
public final class ClientHooks {

    public static void animation(byte type, String text) {
        AnimationOverlay.start(type == 1, text);
    }

    public static void sync(List<Integer> ids) {
        List<Change> list = new ArrayList<>();
        for (int id : ids) {
            if (id >= 0 && id < Change.VALUES.length) list.add(Change.VALUES[id]);
        }
        ClientState.active = list;
    }

    private ClientHooks() {}
}
