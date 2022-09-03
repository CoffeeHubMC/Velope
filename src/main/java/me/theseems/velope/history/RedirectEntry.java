package me.theseems.velope.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.theseems.velope.server.VelopedServer;

import java.util.UUID;

@Data
@AllArgsConstructor
public class RedirectEntry {
    private UUID playerUUID;
    private VelopedServer from;
    private String to;
}
