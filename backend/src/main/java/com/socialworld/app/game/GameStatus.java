package com.socialworld.app.game;

public enum GameStatus {
    /** Invited, waiting for the opponent to accept. */
    PENDING,
    ACTIVE,
    /** Played to a win or a draw. */
    FINISHED,
    /** The opponent said no. */
    DECLINED,
    /** Someone quit or dropped mid-game; the other player takes the win. */
    ABANDONED;

    public boolean isLive() {
        return this == PENDING || this == ACTIVE;
    }
}
