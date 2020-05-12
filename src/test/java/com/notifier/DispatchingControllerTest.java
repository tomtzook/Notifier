package com.notifier;

import com.notifier.dispatchers.EventDispatcher;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DispatchingControllerTest {

    @Test
    public void register_forListener_storesListener() throws Exception {
        final Listener LISTENER = mock(Listener.class);

        Collection<Listener> listeners = new ArrayList<>();
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.registerListener(LISTENER);

        assertThat(listeners, contains(LISTENER));
    }

    @Test
    public void unregister_forRegisterListener_removesListener() throws Exception {
        final Listener LISTENER = mock(Listener.class);

        Collection<Listener> listeners = new ArrayList<>();
        listeners.add(LISTENER);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.unregisterListener(LISTENER);

        assertThat(listeners, not(contains(LISTENER)));
    }

    @Test
    public void unregister_forPredicatedListener_removesListener() throws Exception {
        final DispatchingController.PredicatedListener LISTENER = mock(DispatchingController.PredicatedListener.class);

        Collection<Listener> listeners = new ArrayList<>();
        listeners.add(LISTENER);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.unregisterListener(LISTENER);

        assertThat(listeners, not(contains(LISTENER)));
    }

    @Test
    public void fire_eventForRegisteredListeners_passesAllListenersToDispatcher() throws Exception {
        final Listener[] LISTENERS = {
                mock(Listener.class),
                mock(Listener.class),
                mock(Listener.class)
        };

        Collection<Listener> listeners = new ArrayList<>(Arrays.asList(LISTENERS));

        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);

        dispatchingController.fire(mock(Event.class), Event.class, Listener.class, mock(BiConsumer.class));

        ArgumentCaptor<Collection<Listener>> listenerCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(eventDispatcher, times(1)).dispatch(
                listenerCaptor.capture(),
                any(Predicate.class), any(Event.class), any(BiConsumer.class));

        Collection<Listener> passedListeners = listenerCaptor.getValue();
        assertThat(passedListeners, hasSize(LISTENERS.length));
        assertThat(passedListeners, contains(LISTENERS));
    }

    @Test
    public void fire_forListener_callsListener() throws Exception {
        FakeListener listener = mock(FakeListener.class);
        Event event = mock(Event.class);

        Collection<Listener> listeners = new ArrayList<>(Collections.singleton(listener));
        EventDispatcher eventDispatcher = new FakeDispatching();

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.fire(event, Event.class, FakeListener.class, FakeListener::call);

        verify(listener, times(1)).call(eq(event));
    }

    @Test
    public void fire_forPredicatedListenerWithCorrectEvent_callsListener() throws Exception {
        FakeListener listener = mock(FakeListener.class);
        Event event = mock(Event.class);
        Predicate<Event> predicate = (e)->true;

        Collection<Listener> listeners = new ArrayList<>(Collections.singleton(
                new DispatchingController.PredicatedListener(listener, predicate)));
        EventDispatcher eventDispatcher = new FakeDispatching();

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.fire(event, Event.class, FakeListener.class, FakeListener::call);

        verify(listener, times(1)).call(eq(event));
    }

    @Test
    public void fire_forPredicatedListenerWithWrongEvent_callsListener() throws Exception {
        FakeListener listener = mock(FakeListener.class);
        Event event = mock(Event.class);
        Predicate<Event> predicate = (e)->false;

        Collection<Listener> listeners = new ArrayList<>(Collections.singleton(
                new DispatchingController.PredicatedListener(listener, predicate)));
        EventDispatcher eventDispatcher = new FakeDispatching();

        DispatchingController dispatchingController = new DispatchingController(eventDispatcher, listeners);
        dispatchingController.fire(event, Event.class, FakeListener.class, FakeListener::call);

        verify(listener, never()).call(eq(event));
    }

    private interface FakeListener extends Listener {
        void call(Event event);
    }

    private static class FakeDispatching implements EventDispatcher {

        @Override
        public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
            for (Listener listener : listeners) {
                if (listenerFilter.test(listener)) {
                    listenerCall.accept(listener, event);
                }
            }
        }
    }
}