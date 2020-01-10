package com.notifier.dispatchers;

import com.notifier.BenchmarkEvent;
import com.notifier.BenchmarkListener;
import com.notifier.EmptyExecutor;
import com.notifier.Event;
import com.notifier.Listener;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@State(Scope.Thread)
public class DispatcherBenchmark {

    @Param({"SYNCHRONOUS", "EXECUTOR_BASED_EMPTY"})
    public DispatcherImpl mDispatcherImpl;

    private EventDispatcher mEventDispatcher;
    private Collection<Listener> mListeners;
    private Predicate<Listener> mListenerFilter;
    private Event mEvent;
    private BiConsumer<Listener, Event> mListenerCaller;

    @Setup(Level.Trial)
    public void setup() {
        mEventDispatcher = mDispatcherImpl.create();

        mListeners = IntStream.range(0, 20)
                .mapToObj((i)-> new BenchmarkListener.Empty())
                .collect(Collectors.toList());
        mListenerFilter = (l)->true;
        mEvent = new BenchmarkEvent.Empty();
    }

    @Benchmark
    @BenchmarkMode({Mode.Throughput})
    public void dispatch_withListeners_withEmptyCaller(Blackhole blackhole) {
        mEventDispatcher.dispatch(mListeners, mListenerFilter, mEvent, (l, e)-> {
            blackhole.consume(l);
            blackhole.consume(e);
        });
    }

    public enum DispatcherImpl {
        SYNCHRONOUS {
            @Override
            EventDispatcher create() {
                return new SyncrounousDispatcher();
            }
        },
        EXECUTOR_BASED_EMPTY {
            @Override
            EventDispatcher create() {
                return new ExecutorBasedDispatcher(new EmptyExecutor());
            }
        }
        ;

        abstract EventDispatcher create();
    }
}
