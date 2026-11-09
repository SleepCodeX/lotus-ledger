package dev.lotusledger;

import java.util.EnumMap;
import java.util.Map;

/** Stable, per-entry snapshots. Reopening the menu always starts a fresh authoritative reconciliation. */
public final class MenuSynchronizer {
    public enum State { READING, UPDATED, CURRENT, CATCH_DURING_READ }
    public record Result(int changedSpecies, int parsedSpecies, State state) {}
    private record Candidate(FrogMenu.Entry entry, long catchRevision) {}
    private Object menu;
    private LedgerData.Progress profile;
    private final Map<Frog, Candidate> candidates = new EnumMap<>(Frog.class);
    private final Map<Frog, FrogMenu.Entry> applied = new EnumMap<>(Frog.class);
    private final Map<Frog, FrogMenu.Entry> blocked = new EnumMap<>(Frog.class);

    public void close() { menu = null; profile = null; candidates.clear(); applied.clear(); blocked.clear(); }
    public Result observe(Object menuIdentity, LedgerData.Progress activeProfile, long catchRevision,
                          Map<Frog, FrogMenu.Entry> snapshot) {
        if (menu != menuIdentity || profile != activeProfile) {
            close(); menu = menuIdentity; profile = activeProfile;
        }
        int changes = 0;
        State state = State.CURRENT;
        for (var item : snapshot.entrySet()) {
            Frog frog = item.getKey();
            FrogMenu.Entry entry = item.getValue();
            if (entry.equals(applied.get(frog))) continue;
            if (entry.equals(blocked.get(frog))) { state = State.CATCH_DURING_READ; continue; }
            Candidate candidate = candidates.get(frog);
            if (candidate == null || !entry.equals(candidate.entry)) {
                candidates.put(frog, new Candidate(entry, catchRevision));
                state = State.READING;
                continue;
            }
            if (candidate.catchRevision != catchRevision) {
                blocked.put(frog, entry);
                state = State.CATCH_DURING_READ;
                continue;
            }
            if (profile.synchronize(frog, entry)) changes++;
            applied.put(frog, entry);
        }
        if (changes > 0) state = snapshot.size() == Frog.values().length ? State.UPDATED : State.READING;
        else if (state == State.CURRENT && snapshot.size() < Frog.values().length) state = State.READING;
        return new Result(changes, snapshot.size(), state);
    }
}
