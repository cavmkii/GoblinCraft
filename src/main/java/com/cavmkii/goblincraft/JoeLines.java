package com.cavmkii.goblincraft;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Joe only talks about two things: Sean, and the Baby Wars. His lines live in
 * config/goblincraft/joe_lines.txt (written with defaults on first run) so they can be edited
 * without rebuilding. Reload with /goblincraft reload.
 */
public final class JoeLines {
    private static final Map<String, List<String>> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put("ambient", List.of(
                "Sean owes me $50 for fact-checking fees",
                "Where is President Seanald Trump?",
                "Sean I am pleased to inform you that I, Hi-Fido, have completed my awesome tiny keyboard and gotten the number function to work, thus making I, Hi-Fido 2,000,000,000x cooler than you @ImperfectPK",
                "I was walking behind Sean this morning and saw his phone. Mf listens to Mumford and Seans",
                "Sean be like \"No One Gets Me Im Moving to Svalbaard\"",
                "Me when I hop on the Sean type beat",
                "Woke up thinking about Sean I'm soooooo obsessed",
                "I know your past, present, and future and every variation of every Sean",
                "{player}! Have you seen Sean? Smug dwarf. Smells like rain. Owes me money.",
                "Sean thinks he can hide from me. Sean is WRONG.",
                "Everyone knows Sean started the Baby Wars. EVERYONE.",
                "Don't say it. Don't you DARE say 'baby wars', {player}.",
                "I was there, {player}. In the Baby Wars. You weren't. You don't get opinions.",
                "Sean sells weather. WEATHER. What kind of man sells weather?",
                "When I find Sean I'm biting his knees. Both of them.",
                "Sean sold me a sunny day. It rained for a week.",
                "{player}, if you see Sean, tell him Joe remembers the Baby Wars.",
                "Nobody talks about the Baby Wars. Nobody but me. Constantly.",
                "SEEEEAN! I can smell your stupid drizzle!",
                "The Baby Wars took everything from me. Also Sean did. Mostly Sean.",
                "You look like someone Sean would sell a raincloud to, {player}.",
                "Sean's charms don't work. I know. I stole one. Twice.",
                "There was a Baby Wars memorial once. Sean knocked it over."));
        DEFAULTS.put("steal", List.of(
                "This is going in the Sean revenge fund!",
                "Thanks, {player}! Sean's paying me back for this one way or another!",
                "Confiscated! In the name of the Baby Wars!",
                "Sean took everything from me. Now I take from you. That's economics."));
        DEFAULTS.put("provoked", List.of(
                "Did SEAN put you up to this, {player}?!",
                "You hit like Sean. That's not a compliment.",
                "That's a Baby Wars move, {player}! You're going DOWN!"));
        DEFAULTS.put("rage", List.of(
                "WHO SAID BABY WARS?! {player}! I'M COMING FOR YOU!",
                "BABY WARS?! You don't get to say that! Nobody gets to say that!",
                "{player} said it. {player} SAID IT. Everybody dies now.",
                "I've waited years for someone to say it again, {player}."));
        DEFAULTS.put("fight_start", List.of(
                "SEEEEEAN!!! FINALLY!",
                "There you are, you weather-peddling weasel!",
                "This is for the Baby Wars, Sean!",
                "Sean! Give me back my sunny day!"));
        DEFAULTS.put("fight_end", List.of(
                "This isn't over, Sean! The Baby Wars never end!",
                "Fine! FINE! I'm leaving! But only because I want to, Sean!",
                "Tactical retreat! Sean hasn't seen the last of me!"));
        DEFAULTS.put("death", List.of(
                "Tell... Sean... he still owes me...",
                "The Baby Wars... never... ended...",
                "Sean... did this... somehow..."));
        DEFAULTS.put("restrained", List.of(
                "A restraining order?! Sean wrote this. I KNOW Sean wrote this.",
                "Fine! Fifteen metres! I'm measuring, {player}!",
                "My lawyer will hear about this, {player}. My lawyer is also me.",
                "This order doesn't cover the Baby Wars and you KNOW it."));
        DEFAULTS.put("war_start", List.of(
                "YOU SAID IT. YOU SAID IT THREE TIMES. THE BABY WARS ARE BACK!",
                "Babies! To me! This is what we trained for!",
                "I told you not to say it, {player}. I TOLD you."));
        DEFAULTS.put("war_wave", List.of(
                "NEXT WAVE! Diapers ON!",
                "Send in the Grenadiers! Bottles UP!",
                "For every baby that falls, two more shall... also fall, probably. CHARGE!",
                "Binky Berserkers! Show them what teething really means!"));
        DEFAULTS.put("war", List.of(
                "Hold the line, babies! Hold it! What's a line? HOLD IT ANYWAY!",
                "{player}, put on a bonnet or get out of my war!",
                "Sean brought SECURITY? To a BABY WAR? Coward!",
                "I've seen things in the Baby Wars, {player}. Diapers. So many diapers.",
                "Nobody touches the cauldron! Especially not with Pukeberries!"));
        DEFAULTS.put("war_end", List.of(
                "This isn't over! The Baby Wars are never over!",
                "Fall back! Regroup! Nap time!",
                "Sean will pay for this. Sean will pay for ALL of this."));
        DEFAULTS.put("puke", List.of(
                "Sean's soup. It was Sean's soup. BLARGH!",
                "Thinking about the Baby Wars again... HURRK!",
                "Sean sold me 'bottled sunshine'. It was NOT sunshine. BLEUGH!",
                "Every time someone mentions Sean... BLAAARGH!"));
    }

    private static Map<String, List<String>> loaded = new HashMap<>();

    private JoeLines() {}

    public static Path file() {
        return Cfg.dir().resolve("joe_lines.txt");
    }

    public static List<String> get(String section) {
        List<String> lines = loaded.get(section);
        return lines != null && !lines.isEmpty() ? lines : DEFAULTS.get(section);
    }

    /** Reads the lines file, creating it with the defaults if it doesn't exist. */
    public static String load() {
        Path path = file();
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                Files.write(path, render(), StandardCharsets.UTF_8);
            }

            Map<String, List<String>> parsed = new HashMap<>();
            List<String> current = null;
            for (String raw : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    String name = line.substring(1, line.length() - 1).trim().toLowerCase(Locale.ROOT);
                    current = null;
                    if (DEFAULTS.containsKey(name)) {
                        current = new ArrayList<>();
                        parsed.put(name, current);
                    }
                    continue;
                }
                if (current != null) {
                    current.add(line);
                }
            }

            loaded = parsed;
            int count = parsed.values().stream().mapToInt(List::size).sum();
            return "Loaded " + count + " Joe lines from " + path;
        } catch (IOException e) {
            loaded = new HashMap<>();
            return "Couldn't read " + path + ", using built-in lines: " + e.getMessage();
        }
    }

    private static List<String> render() {
        List<String> out = new ArrayList<>();
        out.add("# Joe's dialogue. He only ever talks about Sean or the Baby Wars.");
        out.add("# One line per entry under each [section]. {player} becomes the nearest player's name.");
        out.add("# Delete a whole section to fall back to the built-in lines for it.");
        out.add("# Reload in-game with: /goblincraft reload");
        for (Map.Entry<String, List<String>> entry : DEFAULTS.entrySet()) {
            out.add("");
            out.add("[" + entry.getKey() + "]");
            out.addAll(entry.getValue());
        }
        return out;
    }
}
