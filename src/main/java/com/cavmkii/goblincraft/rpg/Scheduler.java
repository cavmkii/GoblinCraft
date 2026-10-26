package com.cavmkii.goblincraft.rpg;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.MinecraftServer;

/** Delayed and repeating server work for multi-tick skills (Whirlwind spins, Consecrate pulses, arrow rain). */
public final class Scheduler {
    private record Task(int dueTick, Runnable action) {}

    private static final List<Task> TASKS = new ArrayList<>();
    private static int tick;

    private Scheduler() {}

    public static void later(int delayTicks, Runnable action) {
        TASKS.add(new Task(tick + Math.max(1, delayTicks), action));
    }

    static void tick(MinecraftServer server) {
        tick++;
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (task.dueTick <= tick) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            task.action.run();
        }
    }
}
