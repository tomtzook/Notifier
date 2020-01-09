package com.notifier;

import com.notifier.dispatchers.EventDispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

public class DispatchingController implements EventController {

    private final EventDispatcher mEventDispatcher;
    private final Collection<Listener> mListeners;

    DispatchingController(EventDispatcher eventDispatcher, Collection<Listener> listeners) {
        mEventDispatcher = eventDispatcher;
        mListeners = listeners;
    }

    public DispatchingController(EventDispatcher eventDispatcher) {
        this(eventDispatcher, new CopyOnWriteArrayList<>());
    }

    @Override
    public void registerListener(Listener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterListener(Listener listener) {
        mListeners.remove(listener);
    }

    @Override
    public <E extends Event, L extends Listener> void fire(E event, Class<E> eventType, Class<L> listenerType,
                                                           BiConsumer<L, E> listenerCall) {
        mEventDispatcher.dispatch(
                Collections.unmodifiableCollection(mListeners),
                listenerType::isInstance,
                event,
                new TypeSafeCaller<>(listenerType, eventType, listenerCall));
    }

    private static class TypeSafeCaller<L extends Listener, E extends Event> implements BiConsumer<Listener, Event> {

        private final Class<L> mListenerType;
        private final Class<E> mEventType;

        private final BiConsumer<L, E> mListenerCall;

        private TypeSafeCaller(Class<L> listenerType, Class<E> eventType, BiConsumer<L, E> listenerCall) {
            mListenerType = listenerType;
            mEventType = eventType;
            mListenerCall = listenerCall;
        }

        @Override
        public void accept(Listener listener, Event event) {
            if (mListenerType.isInstance(listener) && mEventType.isInstance(event))  {
                L listenerOfType = mListenerType.cast(listener);
                E eventOfType = mEventType.cast(event);

                mListenerCall.accept(listenerOfType, eventOfType);
            }
        }
    }
}
