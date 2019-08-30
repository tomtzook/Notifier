package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.hamcrest.MockitoHamcrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EventDispatcherTest {

    @RunWith(Parameterized.class)
    public static class ImplTest {

        @Parameterized.Parameter(0)
        public DispatcherContainer mDispatcherContainer;
        @Parameterized.Parameter(1)
        public Class<EventDispatcher> mDispatcherType;
        @Parameterized.Parameter(2)
        public String mTestName;

        private EventDispatcher mEventDispatcher;

        @Parameterized.Parameters(name = "{1}-{2}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {new DispatcherContainer(new SyncrounousDispatcher()),
                            SyncrounousDispatcher.class, "base"},
                    {new DispatcherContainer(new ExecutorBasedDispatcher(new ImmediateExecutor())),
                            ExecutorBasedDispatcher.class, "immediate"},
                    {DispatcherContainer.withExecutor(ExecutorBasedDispatcher::new, Executors.newCachedThreadPool()),
                            ExecutorBasedDispatcher.class, "executorService"},
                    {DispatcherContainer.withExecutor((executor)->{
                        return new BlockingDispatcher(executor, -1, TimeUnit.MICROSECONDS, (t)->{});
                    }, Executors.newCachedThreadPool()),
                            ExecutorBasedDispatcher.class, "executorService-noTimeout"},
            });
        }

        @Before
        public void setUp() throws Exception {
            mEventDispatcher = mDispatcherContainer.mEventDispatcher;
        }

        @After
        public void tearDown() throws Exception {
            for (AutoCloseable closeable : mDispatcherContainer.mResources) {
                try {
                    closeable.close();
                } catch (Throwable t) {}
            }
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
            BiConsumer<Listener, Event> caller = mock(BiConsumer.class);

            mEventDispatcher.dispatch(Arrays.asList(LISTENERS),
                    predicate, EVENT, caller);

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
            BiConsumer<Listener, Event> caller = mock(BiConsumer.class);

            Collection<Listener> listeners = new ArrayList<>();
            listeners.addAll(Arrays.asList(CALL));
            listeners.addAll(Arrays.asList(DONT_CALL));

            mEventDispatcher.dispatch(listeners, predicate, EVENT, caller);

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
    }

    private static class DispatcherContainer {
        EventDispatcher mEventDispatcher;
        Collection<AutoCloseable> mResources;

        DispatcherContainer(EventDispatcher eventDispatcher, Collection<AutoCloseable> closeables) {
            mEventDispatcher = eventDispatcher;
            mResources = closeables;
        }

        DispatcherContainer(EventDispatcher eventDispatcher) {
            this(eventDispatcher, new ArrayList<>());
        }

        static DispatcherContainer withExecutor(Function<ExecutorService, EventDispatcher> eventDispatcher, ExecutorService executor) {
            return new DispatcherContainer(eventDispatcher.apply(executor), Collections.singleton(executor::shutdownNow));
        }
    }
}