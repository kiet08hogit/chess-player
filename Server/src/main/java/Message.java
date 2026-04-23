import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

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
        OPPONENT_LEFT,
        UPDATE_PLAYER_LIST,
        FIND_MATCH,
        CREATE_ROOM,
        JOIN_ROOM,
        WATCH_MATCH,
        ERROR,
        SURRENDER,
        REMATCH,
        GLOBAL_CHAT,
        CANCEL_WAITING,
        REMATCH_REQUEST,
        REMATCH_ACCEPT,
        REMATCH_DECLINE,
        PLAY_BOT,
        SIGNUP,
        SIGNUP_SUCCESS,
        SIGNUP_FAIL
    }

    public Action action;
    public String username;
    public String content;
    public String opponentName;
    public String player1Name;
    public String player2Name;
    public ArrayList<String> playerList;
    public HashMap<String, String> playerStatusMap;
    public String roomId;
    public boolean isBotMatch;
    public int botLevel;

    // Movement details
    public int startX = -1;
    public int startY = -1;
    public int endX = -1;
    public int endY = -1;

    // Game state representation sent by server
    public int[][] board; 

    // Info for client
    public boolean isPlayer1; 
    public boolean isMyTurn;
    public String gameStatus;
    public boolean isSpectator;
    
    // Auth & Info
    public String password;
    
    // Scores
    public int p1Score;
    public int p2Score;

    public Message() {}

    public Message(Action action) {
        this.action = action;
    }
}
