package com.notifier;

import com.notifier.dispatchers.EventDispatcher;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class EmptyEventDispatcher implements EventDispatcher {

    @Override
    public void dispatch(Collection<Listener> listeners, Predicate<Listener> listenerFilter, Event event, BiConsumer<Listener, Event> listenerCall) {

    }
}
