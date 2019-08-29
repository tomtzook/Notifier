package com.notifier;

import java.util.function.BiConsumer;

public interface EventController {

    void registerListener(Listener listener);
    <E extends Event, L extends Listener> void fire(E event, Class<E> eventType,
                                                    Class<L> listenerType,
                                                    BiConsumer<L, E> listenerCall);
}
