package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.hamcrest.MockitoHamcrest;
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
import java.util.function.Predicate;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventDispatcherTest {

    @RunWith(Parameterized.class)
    public static class ImplTest {

        private static ExecutorService sExecutorService = Executors.newCachedThreadPool();

        @Parameterized.Parameter(0)
        public EventDispatcher mEventDispatcher;
        @Parameterized.Parameter(1)
        public Class<EventDispatcher> mDispatcherType;
        @Parameterized.Parameter(2)
        public String mTestName;

        @Parameterized.Parameters(name = "{1}-{2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {new SyncrounousDispatcher(),
                            SyncrounousDispatcher.class, "base"},
                    {new ExecutorBasedDispatcher(new ImmediateExecutor()),
                            ExecutorBasedDispatcher.class, "immediate"},
                    {new ExecutorBasedDispatcher(sExecutorService),
                            ExecutorBasedDispatcher.class, "executorService"},
                    {new BlockingDispatcher(sExecutorService, -1, TimeUnit.MICROSECONDS, (t)->{}),
                            ExecutorBasedDispatcher.class, "executorService-noTimeout"},
            });
        }

        @AfterClass
        public static void tearDown() throws Exception {
            sExecutorService.shutdownNow();
        }

        @Test
        public void dispatch_forListenersWhichMatchesAll_dispatchesAll() throws Exception {
            final Listener[] LISTENERS = {
                    mock(Listener.class),
                    mock(Listener.class),
                    mock(Listener.class)
            };
            final Event EVENT = mock(Event.class);

            Predicate<Listener> predicate = truePredicate();

            CountDownLatch callLatch = new CountDownLatch(LISTENERS.length);
            BiConsumer<Listener, Event> caller = mockCallerMarkingLatch(callLatch);

            mEventDispatcher.dispatch(Arrays.asList(LISTENERS),
                    predicate, EVENT, caller);

            callLatch.await(1, TimeUnit.MINUTES);

            ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
            verify(caller, atLeastOnce()).accept(listenerCaptor.capture(), eq(EVENT));

            Collection<Listener> invokedListeners = listenerCaptor.getAllValues();
            assertThat(invokedListeners, hasSize(LISTENERS.length));
            assertThat(invokedListeners, containsInAnyOrder(LISTENERS));
        }

        @Test
        public void dispatch_forListenersLimitedByPredicate_dispatchesThoseWhoPassPredicate() throws Exception {
            final Listener[] CALL = {
                    mock(Listener.class),
                    mock(Listener.class),
                    mock(Listener.class)
            };
            final Listener[] DONT_CALL = {
                    mock(Listener.class),
                    mock(Listener.class),
                    mock(Listener.class)
            };
            final Event EVENT = mock(Event.class);

            Predicate<Listener> predicate = predicateFor(Arrays.asList(CALL));

            CountDownLatch callLatch = new CountDownLatch(CALL.length);
            BiConsumer<Listener, Event> caller = mockCallerMarkingLatch(callLatch);

            Collection<Listener> listeners = new ArrayList<>();
            listeners.addAll(Arrays.asList(CALL));
            listeners.addAll(Arrays.asList(DONT_CALL));

            mEventDispatcher.dispatch(listeners, predicate, EVENT, caller);

            callLatch.await(1, TimeUnit.MINUTES);

            ArgumentCaptor<Listener> listenerCaptor = ArgumentCaptor.forClass(Listener.class);
            verify(caller, atLeastOnce()).accept(listenerCaptor.capture(), eq(EVENT));

            Collection<Listener> invokedListeners = listenerCaptor.getAllValues();
            assertThat(invokedListeners, hasSize(CALL.length));
            assertThat(invokedListeners, containsInAnyOrder(CALL));
        }

        private Predicate<Listener> truePredicate() {
            Predicate<Listener> predicate = mock(Predicate.class);
            when(predicate.test(any(Listener.class))).thenReturn(true);

            return predicate;

        }

        private Predicate<Listener> predicateFor(Collection<Listener> listeners) {
            Predicate<Listener> predicate = mock(Predicate.class);
            when(predicate.test(MockitoHamcrest.argThat(is(in(listeners))))).thenReturn(true);

            return predicate;
        }

        private BiConsumer<Listener, Event> mockCallerMarkingLatch(CountDownLatch latch) {
            BiConsumer<Listener, Event> caller = mock(BiConsumer.class);
            doAnswer(new Answer<Void>() {
                @Override
                public Void answer(InvocationOnMock invocation) throws Throwable {
                    latch.countDown();
                    return null;
                }
            }).when(caller).accept(any(Listener.class), any(Event.class));

            return caller;
        }
    }
}