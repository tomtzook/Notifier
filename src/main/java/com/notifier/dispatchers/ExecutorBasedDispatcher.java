package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class ExecutorBasedDispatcher implements EventDispatcher {

    private final Executor mExecutor;

    public ExecutorBasedDispatcher(Executor executor) {
        mExecutor = executor;
    }

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
        for (Listener listener : listeners) {
            if (listenerFilter.test(listener)) {
                mExecutor.execute(new DispatchingTask(listener, event, listenerCall));
            }
        }
    }
}
