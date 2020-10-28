package com.notifier;

import com.notifier.dispatchers.BlockingDispatcher;
import com.notifier.dispatchers.ExecutorBasedDispatcher;
import com.notifier.dispatchers.QueuedDispatcher;
import com.notifier.dispatchers.SyncrounousDispatcher;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
        return new DispatchingController(QueuedDispatcher.withBlockingHandler());
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches all events in a separate thread, by order.
     *     The thread is used in provided by <code>executorService</code> by submitting a task via
     *     {@link Executor#execute(Runnable)}, which should execute the runnable without stop, which is why this executor
     *     should not execute the runnable synchronously, since it will block the current thread.
     *     All the events will be dispatched one by one in the task provided to the executor.A lot of listeners or events might
     *     cause contention and delay in dispatching. It is recommended to use this implementation only for
     *     small situations.
     * </p>
     * <p>
     *     The exact nature of the thread used depends on the <code>executorService</code> implementation and state.
     *     This could prove problematic for <code>executorService</code>s which are heavily used as all the threads
     *     might be occupied and dispatching events will not be able to execute.
     * </p>
     *
     * @param executor {@link Executor} to use for running the dispatching task.
     *
     * @return event controller
     */
    public static EventController newSingleThreadController(Executor executor) {
        return new DispatchingController(QueuedDispatcher.withBlockingHandler(executor));
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches all events in a separate thread, by order.
     *     The thread is used in provided by <code>executorService</code> by submitting a task via
     *     {@link ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)}, which should execute the runnable periodically.
     *     All the events will be dispatched one by one in the task provided to the executor. A lot of listeners or events might
     *     cause contention and delay in dispatching. It is recommended to use this implementation only for small situations.
     * </p>
     * <p>
     *     Because of the periodically nature of the dispatching, a delay between the event and the listener handling is expected.
     * </p>
     * <p>
     *     Since {@link ScheduledExecutorService} decided on how to allocates threads, it is entirely possible that
     *     the dispatching task will not run during the expected time due to the all the threads being in use.
     * </p>
     * <p>
     *     During execution, the dispatching task will attempt to dispatching all new events until no more events
     *     are available or a given amount of time has elapsed. If there are more events to dispatch, but the max time
     *     as elapsed, those events will only be dispatched during the next run.
     *
     *     This max run time is the product of <code>min(periodMs / 2, 50)</code>.
     * </p>
     *
     * @param executorService {@link ScheduledExecutorService} for running the dispatching task
     * @param periodMs period of checking and dispatching new events.
     *
     * @return event controller
     */
    public static EventController newPeriodicDispatchingController(ScheduledExecutorService executorService, long periodMs) {
        return new DispatchingController(QueuedDispatcher.withPeriodicHandler(executorService, periodMs));
    }

    /**
     * <p>
     *     Creates a new {@link EventController} which dispatches all events by order. All the events will be
     *     dispatched one by one in the task provided to the executor. A lot of listeners or events might
     *     cause contention and delay in dispatching. It is recommended to use this implementation only for small situations.
     * </p>
     * <p>
     *     The period of execution and which thread is used depends entirely on the executor and thus
     *     remains in user hands.
     * </p>
     * <p>
     *     During execution, the dispatching task will attempt to dispatching all new events until no more events
     *     are available or a given amount of time has elapsed. If there are more events to dispatch, but the max time
     *     as elapsed, those events will only be dispatched during the next run.
     * </p>
     *
     * @param taskExecutor a {@link Consumer} which should cause a periodic execution of the given task.
     *                     The task will only be given once.
     * @param maxPeriodRunTimeMs max amount of time the dispatching task should continue dispatching new events if available.
     *
     * @return event controller
     */
    public static EventController newPeriodicDispatchingController(Consumer<Runnable> taskExecutor, long maxPeriodRunTimeMs) {
        return new DispatchingController(QueuedDispatcher.withPeriodicHandler(taskExecutor, maxPeriodRunTimeMs));
    }
}
