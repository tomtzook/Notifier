package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class SyncrounousDispatcher implements EventDispatcher {

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {
        for (Listener listener : listeners) {
            if (listenerFilter.test(listener)) {
                listenerCall.accept(listener, event);
            }
        }
    }
}
