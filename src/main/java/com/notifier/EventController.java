package com.notifier;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface EventController {

    void registerListener(Listener listener);
    void registerListener(Listener listener, Predicate<Event> predicate);

    void unregisterListener(Listener listener);

    <E extends Event, L extends Listener> void fire(E event, Class<E> eventType,
                                                    Class<L> listenerType,
                                                    BiConsumer<L, E> listenerCall);
}
