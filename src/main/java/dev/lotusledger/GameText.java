package dev.lotusledger;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** Parses game text only. Player chat, island discoveries and inventory pickups are never catches. */
public final class GameText {
    private static final Pattern COLOR = Pattern.compile("§[0-9A-FK-ORa-fk-or]");
    private static final Pattern CATCH = Pattern.compile(
        "^\\S TROPHY FROG! You caught (?:an? )?([A-Za-z -]+) (BRONZE|SILVER|GOLD|DIAMOND)!$");
    private static final Pattern PROFILE = Pattern.compile("^Profile: ([A-Za-z]+)(?: [♲Ⓑ☀]+)?$");
    private static final Pattern JOIN = Pattern.compile(
        "^(?:You are playing on profile:|Your profile was changed to:) ([A-Za-z]+)(?: \\(Co-op\\))?$",
        Pattern.CASE_INSENSITIVE);

    private GameText() {}
    public record Catch(Frog frog, Tier tier) {}
    public static String clean(String text) { return COLOR.matcher(text).replaceAll("").strip(); }
    public static Optional<Catch> parseCatch(String message) {
        var match = CATCH.matcher(clean(message));
        if (!match.matches()) return Optional.empty();
        return Frog.fromName(match.group(1)).map(frog -> new Catch(frog, Tier.valueOf(match.group(2))));
    }
    public static Optional<String> profile(String line) {
        String clean = clean(line);
        for (Pattern pattern : new Pattern[]{PROFILE, JOIN}) {
            var match = pattern.matcher(clean);
            if (match.matches()) return Optional.of(match.group(1).toLowerCase(Locale.ROOT));
        }
        return Optional.empty();
    }
    public static boolean isHypixel(String address) {
        String host = address.toLowerCase(Locale.ROOT).split(":", 2)[0];
        return host.equals("hypixel.net") || host.endsWith(".hypixel.net");
    }
    public static String profileKey(String playerUuid, String server, String profile) {
        String realm = server.toLowerCase(Locale.ROOT).startsWith("alpha.") ? "alpha" : "main";
        return playerUuid + "/" + realm + "/" + profile.toLowerCase(Locale.ROOT);
    }
}
