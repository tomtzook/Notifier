package com.notifier;

import com.notifier.dispatchers.BlockingDispatcher;
import com.notifier.dispatchers.ExecutorBasedDispatcher;
import com.notifier.dispatchers.SyncrounousDispatcher;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Controllers {

    private static final long NO_MAX_WAIT_TIME = -1;

    private Controllers() {}

    public static EventController newSyncExecutionController() {
        return new DispatchingController(new SyncrounousDispatcher());
    }

    public static EventController newExecutorBasedController(Executor executor) {
        return new DispatchingController(new ExecutorBasedDispatcher(executor));
    }

    public static EventController newBlockingController(ExecutorService executorService) {
        return newBlockingController(executorService, emptyHandler());
    }

    public static EventController newBlockingController(ExecutorService executorService,
                                                        long maxWaitTime, TimeUnit maxWaitTimeUnit) {
        return newBlockingController(executorService, maxWaitTime, maxWaitTimeUnit, emptyHandler());
    }

    public static EventController newBlockingController(ExecutorService executorService,
                                                        Consumer<Throwable> errorHandler) {
        return newBlockingController(executorService, NO_MAX_WAIT_TIME, TimeUnit.MILLISECONDS, errorHandler);
    }

    public static EventController newBlockingController(ExecutorService executorService,
                                                        long maxWaitTime, TimeUnit maxWaitTimeUnit,
                                                        Consumer<Throwable> errorHandler) {
        return new DispatchingController(new BlockingDispatcher(executorService,
                maxWaitTime, maxWaitTimeUnit,
                errorHandler));
    }

    private static Consumer<Throwable> emptyHandler() {
        return (throwable) -> {};
    }
}
