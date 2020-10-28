package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class QueuedDispatcher implements EventDispatcher {

    private final Queue<Runnable> mEvents;

    public QueuedDispatcher(Queue<Runnable> events) {
        mEvents = events;
    }

    public static QueuedDispatcher withBlockingHandler() {
        BlockingQueue<Runnable> events = new LinkedBlockingQueue<>();
        QueuedDispatcher dispatcher = new QueuedDispatcher(events);

        Thread runThread = new Thread(new BlockingTask(events), dispatcher.toString()+"-handling thread");
        runThread.setDaemon(true);
        runThread.start();

        return dispatcher;
    }

    public static QueuedDispatcher withBlockingHandler(Executor executor) {
        BlockingQueue<Runnable> events = new LinkedBlockingQueue<>();
        executor.execute(new BlockingTask(events));

        return new QueuedDispatcher(events);
    }

    public static QueuedDispatcher withPeriodicHandler(Consumer<Runnable> taskExecutor, long maxPeriodRunTimeMs) {
        Queue<Runnable> events = new LinkedList<>();
        taskExecutor.accept(new PeriodicTask(events, maxPeriodRunTimeMs));

        return new QueuedDispatcher(events);
    }

    public static QueuedDispatcher withPeriodicHandler(ScheduledExecutorService executorService, long periodMs, long maxPeriodRunTimeMs) {
        Queue<Runnable> events = new LinkedList<>();
        executorService.scheduleAtFixedRate(new PeriodicTask(events, maxPeriodRunTimeMs), periodMs, periodMs, TimeUnit.MILLISECONDS);

        return new QueuedDispatcher(events);
    }

    public static QueuedDispatcher withPeriodicHandler(ScheduledExecutorService executorService, long periodMs) {
        return withPeriodicHandler(executorService, periodMs, Math.min(periodMs / 2, 50));
    }

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
        for (Listener listener : listeners) {
            if (listenerFilter.test(listener)) {
                mEvents.add(new DispatchingTask(listener, event, listenerCall));
            }
        }
    }

    private static class BlockingTask implements Runnable {

        private final BlockingQueue<Runnable> mQueue;

        private BlockingTask(BlockingQueue<Runnable> queue) {
            mQueue = queue;
        }

        @Override
        public void run() {
            try {
                while (!Thread.interrupted()) {
                    Runnable runnable = mQueue.poll(1, TimeUnit.SECONDS);
                    if (runnable != null) {
                        runnable.run();
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private static class PeriodicTask implements Runnable {

        private final Queue<Runnable> mQueue;
        private final long mMaxPeriodRunTimeMs;

        private PeriodicTask(Queue<Runnable> queue, long maxPeriodRunTimeMs) {
            mQueue = queue;
            mMaxPeriodRunTimeMs = maxPeriodRunTimeMs;
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start < mMaxPeriodRunTimeMs) {
                Runnable runnable = mQueue.poll();
                if (runnable == null) {
                    break;
                }

                runnable.run();
            }
        }
    }
}
