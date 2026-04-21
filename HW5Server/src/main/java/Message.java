import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Action {
        LOGIN,
        LOGIN_SUCCESS,
        LOGIN_FAIL,
        MATCH_FOUND,
        GAME_STATE_UPDATE,
        MOVE,
        CHAT_MESSAGE,
        GAME_OVER,
        PLAY_AGAIN,
        OPPONENT_LEFT
    }

    public Action action;
    public String username;
    public String content;
    public String opponentName;

    // Movement details
    public int startX = -1;
    public int startY = -1;
    public int endX = -1;
    public int endY = -1;

    // Game state representation sent by server
    // 0 = empty, 1 = P1, 2 = P2, 3 = P1 King, 4 = P2 King
    public int[][] board; 

    // Info for client
    public boolean isPlayer1; // true if this client is player 1
    public boolean isMyTurn;
    public String gameStatus;

    public Message() {}

    public Message(Action action) {
        this.action = action;
    }
}
