package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;
import org.junit.AfterClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BlockingDispatcherTest {

    private static ExecutorService sExecutorService = Executors.newCachedThreadPool();

    @AfterClass
    public static void tearDown() {
        sExecutorService.shutdownNow();
    }

    @Test
    public void dispatch_noTimeout_waitsUntilAllHaveBeingCalled() throws Exception {
        final Listener[] LISTENERS = {
                mock(Listener.class),
                mock(Listener.class),
                mock(Listener.class)
        };
        final Event EVENT = mock(Event.class);

        Predicate<Listener> predicate = truePredicate();

        Collection<Boolean> conditions = new ArrayList<>();
        CountDownLatch callLatch = new CountDownLatch(1);
        BiConsumer<Listener, Event> caller = delayedCaller(conditions::add, ()-> callLatch.getCount() > 0);

        EventDispatcher eventDispatcher = new BlockingDispatcher(sExecutorService, -1, TimeUnit.MICROSECONDS);
        eventDispatcher.dispatch(Arrays.asList(LISTENERS), predicate, EVENT, caller);
        callLatch.countDown();

        assertThat(conditions, everyItem(equalTo(true)));
    }

    @Test
    public void dispatch_withTimeout_waitsUntilAllHaveBeingCalled() throws Exception {
        final Listener[] LISTENERS = {
                mock(Listener.class),
                mock(Listener.class),
                mock(Listener.class)
        };
        final Event EVENT = mock(Event.class);

        Predicate<Listener> predicate = truePredicate();

        Collection<Boolean> conditions = new ArrayList<>();
        CountDownLatch callLatch = new CountDownLatch(1);
        BiConsumer<Listener, Event> caller = delayedCaller(conditions::add, ()-> callLatch.getCount() > 0);

        EventDispatcher eventDispatcher = new BlockingDispatcher(sExecutorService, 500, TimeUnit.MILLISECONDS);
        eventDispatcher.dispatch(Arrays.asList(LISTENERS), predicate, EVENT, caller);
        callLatch.countDown();

        assertThat(conditions, everyItem(equalTo(true)));
    }

    @Test
    public void dispatch_withTimeout_timeoutWasReachedAndStopped() throws Exception {
        final Listener[] LISTENERS = {
                mock(Listener.class),
                mock(Listener.class),
                mock(Listener.class)
        };
        final Event EVENT = mock(Event.class);

        Predicate<Listener> predicate = truePredicate();

        Collection<Boolean> conditions = new ArrayList<>();
        CountDownLatch callLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(LISTENERS.length);
        BiConsumer<Listener, Event> caller = delayedCaller(conditions::add, ()-> callLatch.getCount() == 0, doneLatch::countDown);

        EventDispatcher eventDispatcher = new BlockingDispatcher(sExecutorService, 50, TimeUnit.MILLISECONDS);
        eventDispatcher.dispatch(Arrays.asList(LISTENERS), predicate, EVENT, caller);
        callLatch.countDown();

        doneLatch.await(1, TimeUnit.MINUTES);

        assertThat(conditions, everyItem(equalTo(true)));
    }

    private Predicate<Listener> truePredicate() {
        Predicate<Listener> predicate = mock(Predicate.class);
        when(predicate.test(any(Listener.class))).thenReturn(true);

        return predicate;
    }

    private BiConsumer<Listener, Event> delayedCaller(Consumer<Boolean> conditionChecker, BooleanSupplier condition) {
        return delayedCaller(conditionChecker, condition, ()->{});
    }

    private BiConsumer<Listener, Event> delayedCaller(Consumer<Boolean> conditionChecker, BooleanSupplier condition, Runnable onDone) {
        BiConsumer<Listener, Event> caller = mock(BiConsumer.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Thread.sleep(200);
                conditionChecker.accept(condition.getAsBoolean());
                onDone.run();
                return null;
            }
        }).when(caller).accept(any(Listener.class), any(Event.class));

        return caller;
    }
}