import java.util.ArrayList;
import java.util.List;

public class Game {
    private int[][] board;
    private boolean p1Turn; // true = P1, false = P2
    private String player1Name;
    private String player2Name;
    private boolean isGameOver;
    private String winner;

    public Game(String player1, String player2) {
        this.player1Name = player1;
        this.player2Name = player2;
        this.board = new int[8][8];
        initializeBoard();
        this.p1Turn = true; // P1 starts
        this.isGameOver = false;
        this.winner = null;
    }

    private void initializeBoard() {
        // 0 = empty, 1 = P1 (bottom), 2 = P2 (top), 3 = P1 King, 4 = P2 King
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                if ((x + y) % 2 != 0) {
                    if (y < 3) board[x][y] = 2; // P2 starts top
                    else if (y > 4) board[x][y] = 1; // P1 starts bottom
                    else board[x][y] = 0;
                } else {
                    board[x][y] = 0;
                }
            }
        }
    }

    public int[][] getBoard() {
        return board;
    }

    public boolean isP1Turn() {
        return p1Turn;
    }

    public boolean isGameOver() {
        return isGameOver;
    }

    public String getWinner() {
        return winner;
    }
    
    public String getPlayer1Name() { return player1Name; }
    public String getPlayer2Name() { return player2Name; }

    // Returns null if valid, or an error reason if invalid
    public synchronized String attemptMove(boolean isP1, int startX, int startY, int endX, int endY) {
        if (isGameOver) return "Game is over.";
        if (isP1 != p1Turn) return "Not your turn.";

        if (!isValidBoardPos(startX, startY) || !isValidBoardPos(endX, endY)) {
            return "Out of bounds.";
        }

        int piece = board[startX][startY];
        if (piece == 0) return "No piece at start position.";
        if (isP1 && (piece == 2 || piece == 4)) return "Not your piece.";
        if (!isP1 && (piece == 1 || piece == 3)) return "Not your piece.";
        if (board[endX][endY] != 0) return "End position is not empty.";
        if ((endX + endY) % 2 == 0) return "Invalid square color.";

        boolean isKing = (piece == 3 || piece == 4);
        int dx = endX - startX;
        int dy = endY - startY;

        // Check direction
        if (!isKing) {
            if (isP1 && dy >= 0) return "Must move forward (up).";
            if (!isP1 && dy <= 0) return "Must move forward (down).";
        }

        // Check if there are mandatory jumps available somewhere on the board
        boolean jumpAvailable = false;
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = board[x][y];
                if (p != 0 && ((isP1 && (p==1||p==3)) || (!isP1 && (p==2||p==4)))) {
                    if (canJump(x, y)) {
                        jumpAvailable = true;
                        break;
                    }
                }
            }
            if (jumpAvailable) break;
        }

        boolean isJump = Math.abs(dx) == 2 && Math.abs(dy) == 2;
        boolean isSimple = Math.abs(dx) == 1 && Math.abs(dy) == 1;

        if (isSimple) {
            if (jumpAvailable) return "Jump is mandatory.";
        } else if (isJump) {
            int midX = startX + dx / 2;
            int midY = startY + dy / 2;
            int midPiece = board[midX][midY];
            if (midPiece == 0) return "Invalid jump.";
            if (isP1 && (midPiece == 1 || midPiece == 3)) return "Cannot jump own piece.";
            if (!isP1 && (midPiece == 2 || midPiece == 4)) return "Cannot jump own piece.";
            
            // Perform jump
            board[midX][midY] = 0; 
        } else {
            return "Invalid move length.";
        }

        // Perform move
        board[startX][startY] = 0;
        board[endX][endY] = piece;

        // Promote to king
        boolean promoted = false;
        if (isP1 && piece == 1 && endY == 0) {
            board[endX][endY] = 3;
            promoted = true;
        } else if (!isP1 && piece == 2 && endY == 7) {
            board[endX][endY] = 4;
            promoted = true;
        }

        // Handle double jumps
        boolean doubleJumpAvail = false;
        if (isJump && !promoted) {
            doubleJumpAvail = canJumpFromPosition(endX, endY);
        }

        if (!doubleJumpAvail) {
            p1Turn = !p1Turn; // Change turn
        }
        
        checkWinCondition();
        return null; // success
    }

    private boolean isValidBoardPos(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    private boolean canJump(int x, int y) {
        return canJumpFromPosition(x, y);
    }

    private boolean canJumpFromPosition(int x, int y) {
        int piece = board[x][y];
        if (piece == 0) return false;
        boolean isP1 = (piece == 1 || piece == 3);
        boolean isKing = (piece == 3 || piece == 4);

        int[][] dirs;
        if (isKing) {
            dirs = new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        } else if (isP1) {
            dirs = new int[][]{{-1, -1}, {1, -1}};
        } else {
            dirs = new int[][]{{-1, 1}, {1, 1}};
        }

        for (int[] dir : dirs) {
            int midX = x + dir[0];
            int midY = y + dir[1];
            int destX = x + 2 * dir[0];
            int destY = y + 2 * dir[1];

            if (isValidBoardPos(destX, destY)) {
                int midP = board[midX][midY];
                int destP = board[destX][destY];
                if (destP == 0 && midP != 0) {
                    boolean midIsOpponent = isP1 ? (midP == 2 || midP == 4) : (midP == 1 || midP == 3);
                    if (midIsOpponent) return true;
                }
            }
        }
        return false;
    }

    private void checkWinCondition() {
        int p1Count = 0;
        int p2Count = 0;
        boolean p1CanMove = false;
        boolean p2CanMove = false;

        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                int p = board[x][y];
                if (p == 1 || p == 3) {
                    p1Count++;
                    if (!p1CanMove && hasValidMove(x, y)) p1CanMove = true;
                } else if (p == 2 || p == 4) {
                    p2Count++;
                    if (!p2CanMove && hasValidMove(x, y)) p2CanMove = true;
                }
            }
        }

        if (p1Count == 0 || (!p1CanMove && p1Turn)) {
            isGameOver = true;
            winner = player2Name;
        } else if (p2Count == 0 || (!p2CanMove && !p1Turn)) {
            isGameOver = true;
            winner = player1Name;
        } else if (!p1CanMove && !p2CanMove) {
            isGameOver = true;
            winner = "Draw";
        }
    }

    private boolean hasValidMove(int x, int y) {
        int piece = board[x][y];
        boolean isP1 = (piece == 1 || piece == 3);
        boolean isKing = (piece == 3 || piece == 4);

        int[][] dirs;
        if (isKing) {
            dirs = new int[][]{{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        } else if (isP1) {
            dirs = new int[][]{{-1, -1}, {1, -1}};
        } else {
            dirs = new int[][]{{-1, 1}, {1, 1}};
        }

        for (int[] dir : dirs) {
            int destX = x + dir[0];
            int destY = y + dir[1];
            if (isValidBoardPos(destX, destY) && board[destX][destY] == 0) return true;
        }
        return canJumpFromPosition(x, y);
    }
}
