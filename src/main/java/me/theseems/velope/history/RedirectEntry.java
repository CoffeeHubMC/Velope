package me.theseems.velope.history;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;
import me.theseems.velope.server.VelopedServer;

import java.util.UUID;

@Data
@AllArgsConstructor
@ToString
public class RedirectEntry {
    private UUID playerUUID;
    private VelopedServer from;
    private String to;
}
