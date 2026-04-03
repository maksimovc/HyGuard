package dev.thenexusgates.hyguard.visual;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class VisualScheduler {

    private final ScheduledExecutorService scheduler;
    private final List<ScheduledFuture<?>> tasks = new CopyOnWriteArrayList<>();

    public VisualScheduler(ScheduledExecutorService scheduler) {
        this.scheduler = scheduler;
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable runnable,
                                                  long initialDelay,
                                                  long period,
                                                  TimeUnit unit) {
        if (scheduler == null) {
            return null;
        }
        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(runnable, initialDelay, period, unit);
        tasks.add(task);
        return task;
    }

    public void cancel(ScheduledFuture<?> task) {
        if (task == null) {
            return;
        }
        task.cancel(false);
        tasks.remove(task);
    }

    public void cancelAll() {
        for (ScheduledFuture<?> task : tasks) {
            if (task != null) {
                task.cancel(false);
            }
        }
        tasks.clear();
    }
}