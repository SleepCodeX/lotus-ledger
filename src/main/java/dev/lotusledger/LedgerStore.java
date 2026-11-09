package dev.lotusledger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Atomic replacement plus a last-good backup. Unreadable data is never silently overwritten. */
public final class LedgerStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path path;
    private boolean recovered;
    public LedgerStore(Path path) { this.path = path; }
    public boolean recoveredBackup() { return recovered; }
    private Path backup() { return path.resolveSibling(path.getFileName() + ".bak"); }

    private LedgerData read(Path source) throws IOException {
        try {
            LedgerData data = GSON.fromJson(Files.readString(source, StandardCharsets.UTF_8), LedgerData.class);
            if (data == null) throw new IllegalArgumentException("Empty ledger");
            data.validate();
            return data;
        } catch (RuntimeException error) {
            throw new IOException("Invalid collection data in " + source.getFileName(), error);
        }
    }

    public LedgerData load() throws IOException {
        if (!Files.exists(path) && !Files.exists(backup())) return new LedgerData();
        try { return read(path); }
        catch (IOException primaryError) {
            if (!Files.exists(backup())) throw primaryError;
            LedgerData data = read(backup());
            // Keep the damaged primary file for manual recovery before replacing it.
            if (Files.exists(path)) Files.copy(path, path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis()));
            recovered = true;
            return data;
        }
    }

    public void save(LedgerData data) throws IOException {
        data.validate();
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary = Files.createTempFile(path.toAbsolutePath().getParent(), "lotus-ledger-", ".tmp");
        try {
            Files.writeString(temporary, GSON.toJson(data), StandardCharsets.UTF_8);
            if (Files.exists(path) && !recovered) {
                read(path); // Never replace a good backup with damaged input.
                Files.copy(path, backup(), StandardCopyOption.REPLACE_EXISTING);
            }
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            recovered = false;
        } finally { Files.deleteIfExists(temporary); }
    }
}
