package ru.yandex.practicum.dto.stats;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Data
@NoArgsConstructor
public class GetStatsRequest {
    private OffsetDateTime start;
    private OffsetDateTime end;
    private List<String> uris;
    private Boolean unique;

    public static GetStatsRequest of(LocalDateTime start,
                                     LocalDateTime end,
                                     List<String> uris,
                                     Boolean unique) {
        GetStatsRequest request = new GetStatsRequest();
        request.setStart(OffsetDateTime.of(start, ZoneOffset.UTC));
        request.setEnd(OffsetDateTime.of(end, ZoneOffset.UTC));
        request.setUnique(unique);
        if (uris != null) {
            request.setUris(uris);
        }

        return request;
    }

    public boolean hasUris() {
        return uris != null && !uris.isEmpty();
    }
}
