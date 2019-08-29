package com.notifier.dispatchers;

import com.notifier.Event;
import com.notifier.Listener;

import java.util.function.BiConsumer;

class DispatchingTask implements Runnable {

    private final Listener mListener;
    private final Event mEvent;
    private final BiConsumer<Listener, Event> mListenerCall;

    DispatchingTask(Listener listener, Event event, BiConsumer<Listener, Event> listenerCall) {
        mListener = listener;
        mEvent = event;
        mListenerCall = listenerCall;
    }

    @Override
    public void run() {
        mListenerCall.accept(mListener, mEvent);
    }

}
