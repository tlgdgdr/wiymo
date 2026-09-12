package com.socialworld.app.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicTacToeTest {

    @Test
    void emptyBoardHasNineEmptyCells() {
        assertThat(TicTacToe.emptyBoard()).isEqualTo(".........");
        assertThat(TicTacToe.winner(TicTacToe.emptyBoard())).isEqualTo(TicTacToe.EMPTY);
        assertThat(TicTacToe.isFull(TicTacToe.emptyBoard())).isFalse();
    }

    @Test
    void detectsEveryWinningLine() {
        String[] wins = {
                "XXX......", "...XXX...", "......XXX",   // rows
                "X..X..X..", ".X..X..X.", "..X..X..X",   // columns
                "X...X...X", "..X.X.X.."                 // diagonals
        };
        for (String board : wins) {
            assertThat(TicTacToe.winner(board)).as(board).isEqualTo(TicTacToe.CHALLENGER);
        }
    }

    @Test
    void aFullBoardWithNoLineIsADraw() {
        String board = "XXOOOXXXO";
        assertThat(TicTacToe.isFull(board)).isTrue();
        assertThat(TicTacToe.winner(board)).isEqualTo(TicTacToe.EMPTY);
    }

    @Test
    void rejectsOccupiedAndOutOfRangeCells() {
        String board = "X........";
        assertThat(TicTacToe.canPlay(board, 0)).isFalse();
        assertThat(TicTacToe.canPlay(board, 1)).isTrue();
        assertThat(TicTacToe.canPlay(board, -1)).isFalse();
        assertThat(TicTacToe.canPlay(board, 9)).isFalse();
    }

    @Test
    void rejectsBoardsThatAreNotNineLegalCharacters() {
        assertThat(TicTacToe.isValidBoard(null)).isFalse();
        assertThat(TicTacToe.isValidBoard("....")).isFalse();
        assertThat(TicTacToe.isValidBoard("XXXXXXXXXX")).isFalse();
        assertThat(TicTacToe.isValidBoard("ZZZZZZZZZ")).isFalse();
        assertThat(TicTacToe.isValidBoard("XO.XO.XO.")).isTrue();
    }

    @Test
    void playOnlyChangesTheChosenCell() {
        assertThat(TicTacToe.play(TicTacToe.emptyBoard(), 4, TicTacToe.OPPONENT)).isEqualTo("....O....");
    }
}
