package com.flamingo.tictactoe.session.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameSessionControllerIntegrationTests {

	private static final StubEngineServer ENGINE = new StubEngineServer();

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@BeforeAll
	static void startEngine() {
		ENGINE.start();
	}

	@AfterAll
	static void stopEngine() {
		ENGINE.stop();
	}

	@DynamicPropertySource
	static void engineProperties(DynamicPropertyRegistry registry) {
		registry.add("game-engine.base-url", ENGINE::baseUrl);
	}

	@Test
	void createsSession() throws Exception {
		HttpResponse<String> response = post("/sessions");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.sessionId")).isNotNull();
		assertThat(json(response, "$.gameId")).isEqualTo(json(response, "$.sessionId"));
		assertThat(json(response, "$.status")).isEqualTo("CREATED");
		assertThat(json(response, "$.game")).isNull();
		assertThat((List<?>) json(response, "$.moves")).isEmpty();
	}

	@Test
	void returnsNotFoundForUnknownSession() throws Exception {
		HttpResponse<String> response = get("/sessions/" + UUID.randomUUID());

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(json(response, "$.status")).isEqualTo(404);
		assertThat((String) json(response, "$.message")).contains("was not found");
	}

	@Test
	void simulatesFullGameAndStoresMoveHistory() throws Exception {
		String sessionId = (String) json(post("/sessions"), "$.sessionId");

		HttpResponse<String> response = post("/sessions/" + sessionId + "/simulate");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.sessionId")).isEqualTo(sessionId);
		assertThat(json(response, "$.status")).isEqualTo("COMPLETED");
		assertThat(json(response, "$.game.status")).isIn("X_WON", "O_WON", "DRAW");
		assertThat((List<?>) json(response, "$.moves")).isNotEmpty();
		assertThat(json(response, "$.moves[0].turn")).isEqualTo(1);
		assertThat(json(response, "$.moves[0].player")).isEqualTo("X");

		HttpResponse<String> lookup = get("/sessions/" + sessionId);
		assertThat(lookup.statusCode()).isEqualTo(200);
		assertThat(json(lookup, "$.status")).isEqualTo("COMPLETED");
		assertThat((List<?>) json(lookup, "$.moves")).hasSameSizeAs((List<?>) json(response, "$.moves"));
	}

	@Test
	void rejectsRepeatedSimulationForCompletedSession() throws Exception {
		String sessionId = (String) json(post("/sessions"), "$.sessionId");
		assertThat(post("/sessions/" + sessionId + "/simulate").statusCode()).isEqualTo(200);

		HttpResponse<String> response = post("/sessions/" + sessionId + "/simulate");

		assertThat(response.statusCode()).isEqualTo(409);
		assertThat(json(response, "$.message")).isEqualTo("Session is already completed");
	}

	@Test
	void rejectsGetForSimulationEndpoint() throws Exception {
		String sessionId = (String) json(post("/sessions"), "$.sessionId");

		HttpResponse<String> response = get("/sessions/" + sessionId + "/simulate");

		assertThat(response.statusCode()).isEqualTo(405);
		assertThat(json(response, "$.status")).isEqualTo(405);
		assertThat(json(response, "$.message")).isEqualTo("Method is not supported for this endpoint");
	}

	@Test
	void returnsNotFoundForUnknownEndpoint() throws Exception {
		String sessionId = (String) json(post("/sessions"), "$.sessionId");

		HttpResponse<String> response = get("/sessions/" + sessionId + "/simulates");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(json(response, "$.status")).isEqualTo(404);
		assertThat(json(response, "$.message")).isEqualTo("No endpoint found for GET /sessions/" + sessionId + "/simulates");
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri(path))
				.GET()
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> post(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri(path))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:%d%s".formatted(port, path));
	}

	private Object json(HttpResponse<String> response, String path) {
		return JsonPath.read(response.body(), path);
	}

	private static class StubEngineServer {

		private static final Pattern PLAYER_PATTERN = Pattern.compile("\"player\"\\s*:\\s*\"([XO])\"");
		private static final Pattern ROW_PATTERN = Pattern.compile("\"row\"\\s*:\\s*(\\d)");
		private static final Pattern COL_PATTERN = Pattern.compile("\"col\"\\s*:\\s*(\\d)");
		private static final int[][] WINNING_LINES = {
				{ 0, 1, 2 },
				{ 3, 4, 5 },
				{ 6, 7, 8 },
				{ 0, 3, 6 },
				{ 1, 4, 7 },
				{ 2, 5, 8 },
				{ 0, 4, 8 },
				{ 2, 4, 6 }
		};

		private HttpServer server;
		private final Map<String, GameSnapshot> games = new HashMap<>();

		void start() {
			try {
				server = HttpServer.create(new InetSocketAddress(0), 0);
				server.createContext("/games", this::handle);
				server.start();
			} catch (IOException exception) {
				throw new UncheckedIOException(exception);
			}
		}

		void stop() {
			server.stop(0);
		}

		String baseUrl() {
			return "http://localhost:%d".formatted(server.getAddress().getPort());
		}

		private void handle(HttpExchange exchange) throws IOException {
			String path = exchange.getRequestURI().getPath();
			String gameId = path.split("/")[2];

			if ("GET".equals(exchange.getRequestMethod())) {
				GameSnapshot game = games.get(gameId);
				if (game == null) {
					write(exchange, 404, "{}");
					return;
				}
				write(exchange, 200, response(gameId, game.board(), game.player(), game.row(), game.col()));
				return;
			}

			if (!"POST".equals(exchange.getRequestMethod()) || !path.endsWith("/move")) {
				write(exchange, 404, "{}");
				return;
			}

			String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
			char[] board = boardFor(gameId);
			String player = requiredMatch(PLAYER_PATTERN, body);
			int row = Integer.parseInt(requiredMatch(ROW_PATTERN, body));
			int col = Integer.parseInt(requiredMatch(COL_PATTERN, body));
			board[row * 3 + col] = player.charAt(0);
			games.put(gameId, new GameSnapshot(board, player, row, col));

			write(exchange, 200, response(gameId, board, player, row, col));
		}

		private char[] boardFor(String gameId) {
			GameSnapshot existing = games.get(gameId);
			if (existing != null) {
				return existing.board();
			}
			int seed = Math.floorMod(gameId.hashCode(), 2);
			List<Integer> opening = seed == 0 ? List.of(0, 4, 8, 1, 2, 6) : List.of(4, 0, 8, 2, 6, 3);
			char[] board = "---------".toCharArray();
			int turn = 0;
			for (Integer index : opening) {
				board[index] = turn % 2 == 0 ? 'X' : 'O';
				turn++;
			}
			return board;
		}

		private String response(String gameId, char[] board, String player, int row, int col) {
			String status = status(board);
			String winner = switch (status) {
				case "X_WON" -> "\"X\"";
				case "O_WON" -> "\"O\"";
				default -> "null";
			};
			return """
					{
					"gameId": "%s",
					"board": %s,
					"status": "%s",
					"winner": %s,
					"lastMove": {
						"player": "%s",
						"row": %d,
						"col": %d
					}
					}
					""".formatted(gameId, boardJson(board), status, winner, player, row, col);
		}

		private String status(char[] board) {
			for (int[] line : WINNING_LINES) {
				char first = board[line[0]];
				if (first != '-' && first == board[line[1]] && first == board[line[2]]) {
					return first == 'X' ? "X_WON" : "O_WON";
				}
			}
			return "DRAW";
		}

		private String boardJson(char[] board) {
			List<String> rows = new ArrayList<>();
			for (int row = 0; row < 3; row++) {
				List<String> cells = new ArrayList<>();
				for (int col = 0; col < 3; col++) {
					char value = board[row * 3 + col];
					cells.add(value == '-' ? "null" : "\"%s\"".formatted(value));
				}
				rows.add("[%s]".formatted(String.join(",", cells)));
			}
			return "[%s]".formatted(String.join(",", rows));
		}

		private String requiredMatch(Pattern pattern, String body) {
			Matcher matcher = pattern.matcher(body);
			if (!matcher.find()) {
				throw new IllegalArgumentException("Request body did not match " + pattern);
			}
			return matcher.group(1);
		}

		private void write(HttpExchange exchange, int status, String body) throws IOException {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		}

		private record GameSnapshot(char[] board, String player, int row, int col) {
		}
	}
}
