package me.theseems.velope.listener.history;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import me.theseems.velope.history.RedirectHistoryRepository;

public class HistoryDisconnectListener {
    @Inject
    private RedirectHistoryRepository historyRepository;

    @Subscribe
    public void onInitialPick(DisconnectEvent event) {
        historyRepository.removeLastRedirect(event.getPlayer().getUniqueId());
    }
}
