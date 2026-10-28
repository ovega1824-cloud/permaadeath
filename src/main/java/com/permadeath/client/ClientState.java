package com.permadeath.client;

import com.permadeath.Change;

import java.util.ArrayList;
import java.util.List;

/** Cambios activos tal como los conoce este cliente (se sincronizan desde el servidor). */
public final class ClientState {
    public static volatile List<Change> active = new ArrayList<>();

    private ClientState() {}
}
