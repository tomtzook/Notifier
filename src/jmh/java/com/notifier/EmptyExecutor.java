package com.notifier;

import java.util.concurrent.Executor;

public class EmptyExecutor implements Executor {

    @Override
    public void execute(Runnable runnable) {

    }
}
