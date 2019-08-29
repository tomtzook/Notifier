package com.notifier.dispatchers;

import java.util.concurrent.Executor;

public class ImmediateExecutor implements Executor {

    @Override
    public void execute(Runnable command) {
        command.run();
    }
}
