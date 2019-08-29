package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentCaptor;
import org.mockito.hamcrest.MockitoHamcrest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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

        @Parameterized.Parameter(0)
        public EventDispatcher mEventDispatcher;
        @Parameterized.Parameter(1)
        public Class<EventDispatcher> mDispatcherType;

        @Parameterized.Parameters(name = "{1}")
        public static Collection<Object[]> data() {
            return Arrays.asList(new Object[][]{
                    {new SyncrounousDispatcher(), SyncrounousDispatcher.class},
                    {new ExecutorBasedDispatcher(new ImmediateExecutor()), ExecutorBasedDispatcher.class},
            });
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
}