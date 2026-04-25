package com.flamingo.tictactoe.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class GameSessionServiceApplication {

	static void main(String[] args) {
		SpringApplication.run(GameSessionServiceApplication.class, args);
	}
}
