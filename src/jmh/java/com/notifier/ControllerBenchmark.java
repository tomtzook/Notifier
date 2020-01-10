package com.notifier;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class ControllerBenchmark {

    @Param({"DISPATCHING"})
    public ControllerImpl mControllerImpl;

    private EventController mEventController;
    private BenchmarkEvent mEvent;
    private Class<BenchmarkEvent> mEventType;
    private Class<BenchmarkListener> mListenerType;
    private BiConsumer<BenchmarkListener, BenchmarkEvent> mListenerCaller;

    @Setup(Level.Trial)
    public void setup() {
        Collection<Listener> listeners = IntStream.range(0, 20)
                .mapToObj((i)-> new BenchmarkListener.Empty())
                .collect(Collectors.toList());

        mEventController = mControllerImpl.createWithListeners(listeners);
        mEvent = new BenchmarkEvent.Empty();
        mEventType = BenchmarkEvent.class;
        mListenerType = BenchmarkListener.class;
        mListenerCaller = BenchmarkListener::onEvent;

    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void fire_withEmptyListeners() {
        mEventController.fire(mEvent, mEventType, mListenerType, mListenerCaller);
    }

    public enum ControllerImpl {
        DISPATCHING {
            @Override
            EventController createWithListeners(Collection<Listener> listeners) {
                return new DispatchingController(new EmptyEventDispatcher(), listeners);
            }
        }
        ;

        abstract EventController createWithListeners(Collection<Listener> listeners);
    }
}
