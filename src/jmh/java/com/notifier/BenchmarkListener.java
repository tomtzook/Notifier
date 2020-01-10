package com.notifier;

public interface BenchmarkListener extends Listener {

    void onEvent(BenchmarkEvent event);

    class Empty implements BenchmarkListener {

        @Override
        public void onEvent(BenchmarkEvent event) {
        }
    }
}
