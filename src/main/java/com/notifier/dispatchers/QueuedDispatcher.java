package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class QueuedDispatcher implements EventDispatcher {

    private final BlockingQueue<Runnable> mEvents;

    public QueuedDispatcher() {
        mEvents = new LinkedBlockingQueue<>();

        Thread runThread = new Thread(new Task(mEvents), toString()+"-handling thread");
        runThread.setDaemon(true);
        runThread.start();
    }

    public QueuedDispatcher(ExecutorService executorService) {
        mEvents = new LinkedBlockingQueue<>();
        executorService.submit(new Task(mEvents));
    }

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
        for (Listener listener : listeners) {
            if (listenerFilter.test(listener)) {
                mEvents.add(new DispatchingTask(listener, event, listenerCall));
            }
        }
    }

    private static class Task implements Runnable {

        private final BlockingQueue<Runnable> mQueue;

        private Task(BlockingQueue<Runnable> queue) {
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
}
