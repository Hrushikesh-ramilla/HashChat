package com.hashchat.messenger.realtime.dto;

import java.util.List;

public record RealtimeSyncResponse(
        List<RealtimeSyncEvent> events,
        long nextCursor,
        boolean hasMore
) {
}
