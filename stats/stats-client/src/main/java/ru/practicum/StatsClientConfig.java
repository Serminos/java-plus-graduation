package ru.practicum;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.MaxAttemptsRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

@Configuration
public class StatsClientConfig {

    @Value("${discovery.services.stats-server-id:stats-server}")
    private String statServiceId;



    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(3000L);
        retryTemplate.setBackOffPolicy(backOffPolicy);

        MaxAttemptsRetryPolicy retryPolicy = new MaxAttemptsRetryPolicy(3);
        retryTemplate.setRetryPolicy(retryPolicy);

        return retryTemplate;
    }

    @Bean
    public StatsClient statClient(DiscoveryClient discoveryClient,
                                 RetryTemplate retryTemplate) {
        return new StatsClient(discoveryClient, retryTemplate, statServiceId);
    }


}
