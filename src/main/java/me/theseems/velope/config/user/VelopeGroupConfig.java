package me.theseems.velope.config.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import me.theseems.velope.algo.BalanceStrategy;

import java.util.List;

@Data
@AllArgsConstructor
public class VelopeGroupConfig {
    private final String name;
    private final List<String> servers;
    private final BalanceStrategy balanceStrategy;
    private final String parent;
    private final VelopeCommandConfig command;
}
