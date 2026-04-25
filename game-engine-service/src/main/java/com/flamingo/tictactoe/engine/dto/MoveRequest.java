package com.flamingo.tictactoe.engine.dto;

import com.flamingo.tictactoe.engine.model.PlayerSymbol;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequest(
		@NotNull PlayerSymbol player,
		@NotNull @Min(0) @Max(2) Integer row,
		@NotNull @Min(0) @Max(2) Integer col
) {
}
