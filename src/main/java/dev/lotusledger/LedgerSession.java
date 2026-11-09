package dev.lotusledger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Keeps catches attached to a verified player/profile and to the world in which they arrived. */
public final class LedgerSession {
    private final LedgerData data;
    private final String playerUuid, server;
    private final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<GameText.Catch> pending = new ArrayList<>();
    private Object world;
    private String profile;
    private long catchRevision;

    public LedgerSession(LedgerData data, String playerUuid, String server) {
        this.data = data;
        this.playerUuid = playerUuid;
        this.server = server;
    }
    public void bindWorld(Object nextWorld) {
        if (world == nextWorld) return;
        world = nextWorld;
        profile = null;
        pending.clear();
        seen.clear();
    }
    public void nextTick() { seen.clear(); }
    public String profile() { return profile; }
    public int buffered() { return pending.size(); }
    public long catchRevision() { return catchRevision; }
    public LedgerData.Progress progress() {
        return profile == null ? null : data.profiles.computeIfAbsent(
            GameText.profileKey(playerUuid, server, profile), ignored -> new LedgerData.Progress());
    }
    public boolean chooseProfile(String name) {
        boolean changed = !name.equals(profile);
        profile = name;
        for (GameText.Catch caught : pending) progress().record(caught);
        pending.clear();
        return changed;
    }
    public boolean accept(Object messageIdentity, GameText.Catch caught) {
        // Fabric's allow/cancel callbacks may expose the same packet twice when another mod hides it.
        // Distinct packets with identical text must still count as distinct catches.
        if (!seen.add(messageIdentity)) return false;
        catchRevision++;
        if (profile == null) pending.add(caught);
        else progress().record(caught);
        return true;
    }
}
