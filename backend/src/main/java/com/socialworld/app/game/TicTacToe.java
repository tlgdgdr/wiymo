package com.socialworld.app.game;

/**
 * Pure tic-tac-toe rules over a 9-character board string, indices 0-8 read
 * left-to-right, top-to-bottom. No Spring, no database — every rule is
 * decided here so the service only has to care about whose turn it is.
 */
public final class TicTacToe {

    public static final char EMPTY = '.';
    public static final char CHALLENGER = 'X';
    public static final char OPPONENT = 'O';
    public static final int CELLS = 9;

    private static final int[][] LINES = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},   // rows
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},   // columns
            {0, 4, 8}, {2, 4, 6}               // diagonals
    };

    private TicTacToe() {
    }

    public static String emptyBoard() {
        return String.valueOf(EMPTY).repeat(CELLS);
    }

    public static boolean isValidBoard(String board) {
        if (board == null || board.length() != CELLS) {
            return false;
        }
        for (char c : board.toCharArray()) {
            if (c != EMPTY && c != CHALLENGER && c != OPPONENT) {
                return false;
            }
        }
        return true;
    }

    public static boolean canPlay(String board, int cell) {
        return isValidBoard(board) && cell >= 0 && cell < CELLS && board.charAt(cell) == EMPTY;
    }

    public static String play(String board, int cell, char mark) {
        char[] cells = board.toCharArray();
        cells[cell] = mark;
        return new String(cells);
    }

    /** The mark occupying a completed line, or {@link #EMPTY} if there is none. */
    public static char winner(String board) {
        for (int[] line : LINES) {
            char first = board.charAt(line[0]);
            if (first != EMPTY && first == board.charAt(line[1]) && first == board.charAt(line[2])) {
                return first;
            }
        }
        return EMPTY;
    }

    public static boolean isFull(String board) {
        return board.indexOf(EMPTY) < 0;
    }
}
