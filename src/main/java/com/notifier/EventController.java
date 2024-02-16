package com.notifier;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface EventController {

    RegisteredListener registerListener(Listener listener);
    RegisteredListener registerListener(Listener listener, Predicate<Event> predicate);
    <E extends Event> RegisteredListener registerListenerForEvent(Listener listener, Class<E> eventType);
    <E extends Event> RegisteredListener registerListenerForEvent(Listener listener, Class<E> eventType, Predicate<? super E> predicate);

    <E extends Event, L extends Listener> void fire(E event, Class<E> eventType,
                                                    Class<L> listenerType,
                                                    BiConsumer<L, E> listenerCall);
}
