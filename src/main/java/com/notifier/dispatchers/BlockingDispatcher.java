package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class BlockingDispatcher implements EventDispatcher {

    private final ExecutorService mExecutorService;
    private final long mMaxWaitTime;
    private final TimeUnit mWaitTimeUnit;
    private final Consumer<Throwable> mErrorHandler;

    public BlockingDispatcher(ExecutorService executorService, long maxWaitTime, TimeUnit waitTimeUnit, Consumer<Throwable> errorHandler) {
        mExecutorService = executorService;
        mMaxWaitTime = maxWaitTime;
        mWaitTimeUnit = waitTimeUnit;
        mErrorHandler = errorHandler;
    }

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
        Collection<Future> futures = new ArrayList<>();

        for (Listener listener : listeners) {
            if (listenerFilter.test(listener)) {
                Future future = mExecutorService.submit(new DispatchingTask(listener, event, listenerCall));
                futures.add(future);
            }
        }

        for (Future future : futures) {
            try {
                if (mMaxWaitTime <= 0) {
                    future.get();
                } else {
                    future.get(mMaxWaitTime, mWaitTimeUnit);
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                mErrorHandler.accept(e);
            }
        }
    }
}
