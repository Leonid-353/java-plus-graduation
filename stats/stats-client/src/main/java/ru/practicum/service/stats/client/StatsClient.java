package ru.practicum.service.stats.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import ru.practicum.service.stats.GetStatsRequest;
import ru.practicum.service.stats.StatsDtoRequest;
import ru.practicum.service.stats.StatsDtoResponse;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class StatsClient {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final RestClient restClient;

    @Autowired
    public StatsClient(@Value("${stats-server.url}") String serverUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(serverUrl)
                .build();
    }

    public void saveHit(StatsDtoRequest request) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public List<StatsDtoResponse> getStats(GetStatsRequest request) {
        StringBuilder uriBuilder = new StringBuilder("/stats?start={start}&end={end}");

        if (request.getUnique() != null) {
            uriBuilder.append("&unique={unique}");
        }

        if (request.hasUris()) {
            for (int i = 0; i < request.getUris().size(); i++) {
                uriBuilder.append("&uris={uri").append(i).append("}");
            }
        }

        return restClient.get()
                .uri(uriBuilder.toString(), buildUriVariables(request))
                .retrieve()
                .body(new ParameterizedTypeReference<List<StatsDtoResponse>>() {
                });
    }

    private Object[] buildUriVariables(GetStatsRequest request) {
        Object[] baseVars = new Object[]{
                request.getStart().toLocalDateTime().format(FORMATTER),
                request.getEnd().toLocalDateTime().format(FORMATTER)
        };

        if (request.getUnique() != null && !request.hasUris()) {
            return new Object[]{
                    baseVars[0], baseVars[1], request.getUnique()
            };
        }

        if (request.hasUris() && request.getUnique() == null) {
            Object[] uriVars = new Object[2 + request.getUris().size()];
            System.arraycopy(baseVars, 0, uriVars, 0, 2);

            for (int i = 0; i < request.getUris().size(); i++) {
                uriVars[2 + i] = request.getUris().get(i);
            }

            return uriVars;
        }

        if (request.hasUris() && request.getUnique() != null) {
            Object[] uriVars = new Object[3 + request.getUris().size()];
            System.arraycopy(baseVars, 0, uriVars, 0, 2);
            uriVars[2] = request.getUnique();

            for (int i = 0; i < request.getUris().size(); i++) {
                uriVars[3 + i] = request.getUris().get(i);
            }

            return uriVars;
        }

        return baseVars;
    }
}