package com.notifier;

import com.notifier.dispatchers.EventDispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

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
    public void registerListener(Listener listener, Predicate<Event> predicate) {
        mListeners.add(new PredicatedListener(listener, predicate));
    }

    @Override
    public void unregisterListener(Listener listener) {
        mListeners.removeIf((l)->l.equals(listener));
    }

    @Override
    public <E extends Event, L extends Listener> void fire(E event, Class<E> eventType, Class<L> listenerType,
                                                           BiConsumer<L, E> listenerCall) {
        mEventDispatcher.dispatch(
                Collections.unmodifiableCollection(mListeners),
                (l)->listenerType.isInstance(l) || l instanceof PredicatedListener,
                event,
                new TypeSafeCaller<>(listenerType, eventType, listenerCall));
    }

    static class PredicatedListener implements Listener {

        private final Listener mListener;
        private final Predicate<Event> mPredicate;

        PredicatedListener(Listener listener, Predicate<Event> predicate) {
            mListener = listener;
            mPredicate = predicate;
        }

        public <L extends Listener, E extends Event> void call(E event, Class<L> listenerType,
                                                               BiConsumer<L, E> listenerCall) {
            if (listenerType.isInstance(mListener) && mPredicate.test(event)) {
                listenerCall.accept(listenerType.cast(mListener), event);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return mListener.equals(obj);
        }
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
            if ((mListenerType.isInstance(listener) || listener instanceof PredicatedListener) &&
                    mEventType.isInstance(event))  {
                E eventOfType = mEventType.cast(event);

                if (listener instanceof PredicatedListener) {
                    ((PredicatedListener)listener).call(eventOfType, mListenerType, mListenerCall);
                } else {
                    L listenerOfType = mListenerType.cast(listener);
                    mListenerCall.accept(listenerOfType, eventOfType);
                }
            }
        }
    }
}
