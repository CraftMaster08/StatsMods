package net.craftmaster08.cm08statscore.ranking;

import java.util.UUID;

public record RankEntry(
        UUID uuid,
        String username,
        long value,
        int rank
) {}
