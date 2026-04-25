package com.flamingo.tictactoe.session.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GameEngineProperties.class)
class RestClientConfig {

	@Bean
	RestClient gameEngineRestClient(GameEngineProperties properties) {
		return RestClient.builder()
				.baseUrl(properties.baseUrl())
				.build();
	}
}
