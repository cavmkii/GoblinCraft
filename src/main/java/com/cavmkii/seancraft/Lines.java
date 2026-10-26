package com.cavmkii.seancraft;

import java.util.List;
import net.minecraft.util.RandomSource;

/**
 * Dialogue. {player} is replaced with the nearest player's name.
 * Joe's lines come from {@link JoeLines} (an editable file); Sean's are below.
 */
public final class Lines {
    private static final RandomSource RANDOM = RandomSource.create();

    public static List<String> joeAmbient() { return JoeLines.get("ambient"); }
    public static List<String> joeSteal() { return JoeLines.get("steal"); }
    public static List<String> joeProvoked() { return JoeLines.get("provoked"); }
    public static List<String> joeRage() { return JoeLines.get("rage"); }
    public static List<String> joeFightStart() { return JoeLines.get("fight_start"); }
    public static List<String> joeFightEnd() { return JoeLines.get("fight_end"); }
    public static List<String> joeDeath() { return JoeLines.get("death"); }
    public static List<String> joePuke() { return JoeLines.get("puke"); }
    public static List<String> joeRestrained() { return JoeLines.get("restrained"); }
    public static List<String> joeWarStart() { return JoeLines.get("war_start"); }
    public static List<String> joeWarWave() { return JoeLines.get("war_wave"); }
    public static List<String> joeWar() { return JoeLines.get("war"); }
    public static List<String> joeWarEnd() { return JoeLines.get("war_end"); }

    public static final List<String> SEAN_GREET = List.of(
            "Ah, a customer! Fair winds to you, friend.",
            "Storm's coming. Good thing I sell umbrellas. Well. Soda. Close enough.",
            "Sean's Weatherworks, open for business! Mind the puddles.",
            "Welcome! Have you seen a small, rude goblin? No? Good.");

    public static final List<String> SEAN_PITCH = List.of(
            "Soda! Tastes like a thunderstorm!",
            "Restraining orders! Keep the goblin at fifteen blocks, guaranteed!",
            "Merch! It's black! It does nothing!",
            "Everything must go before the next thunderstorm!");

    public static final List<String> SEAN_FIGHT_START = List.of(
            "Oh no. Not you again, Joe.",
            "Joe! I told you, NO REFUNDS!",
            "Stand back, customers. This one bites.");

    public static final List<String> SEAN_FIGHT_END = List.of(
            "And STAY gone! Now, where was I? Ah yes, discounts!",
            "Sorry about the scorch marks. Those are free, by the way.");

    public static final List<String> SEAN_WAR = List.of(
            "Security! Earn your pay!",
            "Merch wearers, you're with me! Everyone else, good luck!",
            "War prices are in effect. Sorry. Not sorry.",
            "Joe, this is a BABY WAR. Have you no shame?",
            "Keep them away from the stock!");

    private Lines() {}

    public static String pick(List<String> lines) {
        return pick(lines, null);
    }

    public static String pick(List<String> lines, String playerName) {
        String line = lines.get(RANDOM.nextInt(lines.size()));
        return line.replace("{player}", playerName == null || playerName.isEmpty() ? "you" : playerName);
    }
}
