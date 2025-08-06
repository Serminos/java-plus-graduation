package ru.practicum;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stats.dto.EndpointHitDto;
import ru.practicum.stats.dto.ViewStatsDto;

import java.util.List;

public class StatsClient {
    private final DiscoveryClient discoveryClient;
    private final RetryTemplate retryTemplate;
    private final String statServiceId;

    private static final String HIT_ENDPOINT = "/hit";
    private static final String STATS_ENDPOINT = "/stats";

    public StatsClient(DiscoveryClient discoveryClient,
                      RetryTemplate retryTemplate,
                      String statServiceId) {
        this.discoveryClient = discoveryClient;
        this.retryTemplate = retryTemplate;
        this.statServiceId = statServiceId;
    }

    public void saveStatEvent(EndpointHitDto endpointHitDto) {
        executeWithDiscovery(client ->
                client.post()
                        .uri(HIT_ENDPOINT)
                        .body(endpointHitDto)
                        .retrieve()
                        .toBodilessEntity()
        );
    }

    public ResponseEntity<List<ViewStatsDto>> getStats(String start,
                                                       String end,
                                                       @Nullable List<String> uris,
                                                       boolean unique) {
        return executeWithDiscovery(client -> {
            String uri = buildStatsUri(start, end, uris, unique);
            return client.get()
                    .uri(uri)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<>() {});
        });
    }

    private String buildStatsUri(String start, String end, @Nullable List<String> uris, boolean unique) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(STATS_ENDPOINT)
                .queryParam("start", start)
                .queryParam("end", end)
                .queryParam("unique", unique);

        if (uris != null && !uris.isEmpty()) {
            builder.queryParam("uris", String.join(",", uris));
        }

        return builder.build().toUriString();
    }

    private <T> T executeWithDiscovery(StatClientOperation<T> operation) {
        try {
            return retryTemplate.execute((RetryCallback<T, Exception>) context -> {
                ServiceInstance instance = getAvailableInstance();
                RestClient client = createRestClient(instance.getUri().toString());
                return operation.execute(client);
            });
        } catch (Exception e) {
            throw new StatsServerUnavailableException(
                    "Сервер статистики недоступен после повторных попыток", e
            );
        }
    }

    private ServiceInstance getAvailableInstance() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statServiceId);
        if (instances == null || instances.isEmpty()) {
            throw new IllegalStateException("Сервер статистики не найден в реестре");
        }
        return instances.getFirst();
    }

    private RestClient createRestClient(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .defaultStatusHandler(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        (request, response) -> {
                            throw new RestClientException(
                                    "HTTP error " + response.getStatusCode() + ": " + response.getStatusText()
                            );
                        })
                .build();
    }

    @FunctionalInterface
    private interface StatClientOperation<T> {
        T execute(RestClient client);
    }

    public static class StatsServerUnavailableException extends RuntimeException {
        public StatsServerUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}