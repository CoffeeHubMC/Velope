package me.theseems.velope.handler;

import com.google.inject.Inject;
import me.theseems.velope.history.RedirectHistoryRepository;

public class FailureHistoryWipeHandler implements Runnable {
    @Inject
    private RedirectHistoryRepository historyRepository;

    @Override
    public void run() {
        historyRepository.cleanFailures();
    }
}
