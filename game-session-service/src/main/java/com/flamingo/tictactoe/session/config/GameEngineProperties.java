package com.flamingo.tictactoe.session.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "game-engine")
public record GameEngineProperties(
		String baseUrl
) {
}
