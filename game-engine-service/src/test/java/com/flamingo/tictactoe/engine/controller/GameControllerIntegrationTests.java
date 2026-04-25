package com.flamingo.tictactoe.engine.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTests {

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Test
	void acceptsValidMoveAndReturnsUpdatedGame() throws Exception {
		String gameId = newGameId();

		HttpResponse<String> response = move(gameId, "X", 0, 2);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.gameId")).isEqualTo(gameId);
		assertThat(json(response, "$.board[0][2]")).isEqualTo("X");
		assertThat(json(response, "$.status")).isEqualTo("IN_PROGRESS");
		assertThat(json(response, "$.winner")).isNull();
		assertThat(json(response, "$.lastMove.player")).isEqualTo("X");
		assertThat(json(response, "$.lastMove.row")).isEqualTo(0);
		assertThat(json(response, "$.lastMove.col")).isEqualTo(2);
	}

	@Test
	void returnsExistingGameState() throws Exception {
		String gameId = newGameId();
		assertThat(move(gameId, "O", 1, 1).statusCode()).isEqualTo(200);

		HttpResponse<String> response = get("/games/" + gameId);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.gameId")).isEqualTo(gameId);
		assertThat(json(response, "$.board[1][1]")).isEqualTo("O");
		assertThat(json(response, "$.status")).isEqualTo("IN_PROGRESS");
	}

	@Test
	void returnsNotFoundForUnknownGameLookup() throws Exception {
		HttpResponse<String> response = get("/games/" + newGameId());

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(json(response, "$.status")).isEqualTo(404);
		assertThat((String) json(response, "$.message")).contains("was not found");
	}

	@Test
	void returnsNotFoundForUnknownEndpoint() throws Exception {
		HttpResponse<String> response = get("/games");

		assertThat(response.statusCode()).isEqualTo(404);
		assertThat(json(response, "$.status")).isEqualTo(404);
		assertThat(json(response, "$.message")).isEqualTo("No endpoint found for GET /games");
	}

	@Test
	void rejectsMoveOutsideBoard() throws Exception {
		String gameId = newGameId();

		HttpResponse<String> response = postJson("/games/" + gameId + "/move", """
				{
				"player": "X",
				"row": 3,
				"col": 0
				}
				""");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(json(response, "$.status")).isEqualTo(400);
		assertThat((String) json(response, "$.message")).contains("row must be less than or equal to 2");
	}

	@Test
	void rejectsUnknownPlayerSymbol() throws Exception {
		HttpResponse<String> response = postJson("/games/" + newGameId() + "/move", """
				{
				"player": "Z",
				"row": 0,
				"col": 0
				}
				""");

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(json(response, "$.message")).isEqualTo("Request body is invalid");
	}

	@Test
	void rejectsOccupiedCell() throws Exception {
		String gameId = newGameId();
		assertThat(move(gameId, "X", 0, 0).statusCode()).isEqualTo(200);

		HttpResponse<String> response = move(gameId, "O", 0, 0);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(json(response, "$.message")).isEqualTo("Cell is already occupied");
	}

	@Test
	void detectsRowWin() throws Exception {
		String gameId = newGameId();

		move(gameId, "X", 0, 0);
		move(gameId, "O", 1, 0);
		move(gameId, "X", 0, 1);
		move(gameId, "O", 1, 1);

		HttpResponse<String> response = move(gameId, "X", 0, 2);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.status")).isEqualTo("X_WON");
		assertThat(json(response, "$.winner")).isEqualTo("X");
	}

	@Test
	void detectsColumnWin() throws Exception {
		String gameId = newGameId();

		move(gameId, "X", 0, 0);
		move(gameId, "O", 0, 1);
		move(gameId, "X", 1, 0);
		move(gameId, "O", 0, 2);

		HttpResponse<String> response = move(gameId, "X", 2, 0);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.status")).isEqualTo("X_WON");
		assertThat(json(response, "$.winner")).isEqualTo("X");
	}

	@Test
	void detectsDiagonalWin() throws Exception {
		String gameId = newGameId();

		move(gameId, "O", 0, 0);
		move(gameId, "X", 0, 1);
		move(gameId, "O", 1, 1);
		move(gameId, "X", 0, 2);

		HttpResponse<String> response = move(gameId, "O", 2, 2);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.status")).isEqualTo("O_WON");
		assertThat(json(response, "$.winner")).isEqualTo("O");
	}

	@Test
	void detectsDraw() throws Exception {
		String gameId = newGameId();

		move(gameId, "X", 0, 0);
		move(gameId, "O", 0, 1);
		move(gameId, "X", 0, 2);
		move(gameId, "O", 1, 1);
		move(gameId, "X", 1, 0);
		move(gameId, "O", 1, 2);
		move(gameId, "X", 2, 1);
		move(gameId, "O", 2, 0);

		HttpResponse<String> response = move(gameId, "X", 2, 2);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(json(response, "$.status")).isEqualTo("DRAW");
		assertThat(json(response, "$.winner")).isNull();
	}

	@Test
	void rejectsMoveAfterGameCompletion() throws Exception {
		String gameId = newGameId();

		move(gameId, "X", 0, 0);
		move(gameId, "O", 1, 0);
		move(gameId, "X", 0, 1);
		move(gameId, "O", 1, 1);
		move(gameId, "X", 0, 2);

		HttpResponse<String> response = move(gameId, "O", 2, 2);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(json(response, "$.message")).isEqualTo("Game is already completed");
	}

	private HttpResponse<String> move(String gameId, String player, int row, int col) throws IOException, InterruptedException {
		return postJson("/games/" + gameId + "/move", """
				{
				"player": "%s",
				"row": %d,
				"col": %d
				}
				""".formatted(player, row, col));
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri(path))
				.GET()
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> postJson(String path, String body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri(path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:%d%s".formatted(port, path));
	}

	private Object json(HttpResponse<String> response, String path) {
		return JsonPath.read(response.body(), path);
	}

	private String newGameId() {
		return "game-" + UUID.randomUUID();
	}
}
