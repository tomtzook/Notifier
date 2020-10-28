package com.notifier;

import com.notifier.dispatchers.BlockingDispatcher;
import com.notifier.dispatchers.ExecutorBasedDispatcher;
import com.notifier.dispatchers.QueuedDispatcher;
import com.notifier.dispatchers.SyncrounousDispatcher;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

public class Controllers {

    private static final long NO_MAX_WAIT_TIME = -1;

    private Controllers() {}

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches events in the same
     *     thread of the call, blocking the call to {@link EventController#fire(Event, Class, Class, BiConsumer) fire}
     *     until all listeners have finished.
     * </p>
     *
     * @return event controller
     */
    public static EventController newSyncExecutionController() {
        return new DispatchingController(new SyncrounousDispatcher());
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches events using the
     *     given {@link Executor}. Each listener will be called in a {@link Runnable task}
     *     which is passed to {@link Executor#execute(Runnable)}. Depending on the implementation
     *     of the executor, the call to {@link EventController#fire(Event, Class, Class, BiConsumer) fire}
     *     might be blocking, or not.
     * </p>
     *
     * @param executor executor for running listeners
     *
     * @return event controller
     */
    public static EventController newExecutorBasedController(Executor executor) {
        return new DispatchingController(new ExecutorBasedDispatcher(executor));
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches events using the
     *     given {@link ExecutorService}. Each listener will be called in a {@link Runnable task}
     *     which is passed to {@link ExecutorService#submit(Runnable)}. Depending on the implementation
     *     of the executor, the threads handling might be different.
     * </p>
     * <p>
     *     A call to {@link EventController#fire(Event, Class, Class, BiConsumer) fire} will be blocking
     *     until all the listeners have being called.
     * </p>
     *
     * @param executorService executor service for running listeners
     *
     * @return event controller
     */
    public static EventController newBlockingController(ExecutorService executorService) {
        return newBlockingController(executorService, NO_MAX_WAIT_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches events using the
     *     given {@link ExecutorService}. Each listener will be called in a {@link Runnable task}
     *     which is passed to {@link ExecutorService#submit(Runnable)}. Depending on the implementation
     *     of the executor, the threads handling might be different.
     * </p>
     * <p>
     *     A call to {@link EventController#fire(Event, Class, Class, BiConsumer) fire} will be blocking
     *     until all the listeners have being called, or a timeout has occurred.
     * </p>
     * <p>
     *     If a timeout has occurred, listeners will still be called, however, the call is simply no
     *     longer blocking.
     * </p>
     *
     * @param executorService executor service
     * @param maxWaitTime max wait timeout
     * @param maxWaitTimeUnit time unit for the max timeout
     *
     * @return event controller
     */
    public static EventController newBlockingController(ExecutorService executorService,
                                                        long maxWaitTime, TimeUnit maxWaitTimeUnit) {
        return new DispatchingController(new BlockingDispatcher(executorService,
                maxWaitTime, maxWaitTimeUnit));
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches all events in a separate thread, by order.
     *     The created thread is a daemon thread and runs all the calls one by one. A lot of listeners or events might
     *     cause contention and delay in dispatching. It is recommended to use this implementation only for
     *     small situations.
     * </p>
     *
     * @return event controller
     */
    public static EventController newSingleThreadController() {
        return new DispatchingController(new QueuedDispatcher());
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches all events in a separate thread, by order.
     *     The thread is used in provided by <code>executorService</code> by submitting a task via
     *     {@link ExecutorService#submit(Runnable)}. All the events will be dispatched one by one in the task
     *     provided to the executor.A lot of listeners or events might
     *     cause contention and delay in dispatching. It is recommended to use this implementation only for
     *     small situations.
     * </p>
     * <p>
     *     The exact nature of the thread used depends on the <code>executorService</code> implementation and state.
     *     This could prove problematic for <code>executorService</code>s which are heavily used as all the threads
     *     might be occupied and dispatching events will not be able to execute.
     * </p>
     *
     * @param executorService {@link ExecutorService} to use for running the dispatching task.
     *
     * @return event controller
     */
    public static EventController newSingleThreadController(ExecutorService executorService) {
        return new DispatchingController(new QueuedDispatcher(executorService));
    }
}
