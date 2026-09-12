package com.socialworld.app.game.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Cell index 0-8 for tic-tac-toe; other games will widen this. */
public record MoveRequest(@Min(0) @Max(8) int cell) {
}
