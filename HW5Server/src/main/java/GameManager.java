import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameManager {
    private HashMap<String, Server.ClientThread> onlineUsers = new HashMap<>();
    private Server.ClientThread waitingClient = null;
    private List<GameSession> activeGames = new ArrayList<>();
    
    private Server server;

    public GameManager(Server server) {
        this.server = server;
    }

    public synchronized boolean login(String username, Server.ClientThread client) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }
        onlineUsers.put(username, client);
        client.username = username;
        
        server.log("User logged in: " + username);

        // Try matchmaking
        if (waitingClient == null) {
            waitingClient = client;
            server.log(username + " is waiting for a match.");
        } else {
            // Match found!
            Server.ClientThread p1 = waitingClient;
            Server.ClientThread p2 = client;
            waitingClient = null;

            GameSession session = new GameSession(p1, p2, new Game(p1.username, p2.username));
            activeGames.add(session);
            p1.gameSession = session;
            p2.gameSession = session;

            server.log("Match started: " + p1.username + " vs " + p2.username);

            // Notify P1
            Message m1 = new Message(Message.Action.MATCH_FOUND);
            m1.opponentName = p2.username;
            m1.isPlayer1 = true;
            p1.send(m1);

            // Notify P2
            Message m2 = new Message(Message.Action.MATCH_FOUND);
            m2.opponentName = p1.username;
            m2.isPlayer1 = false;
            p2.send(m2);

            broadcastGameState(session);
        }

        return true;
    }

    public synchronized void disconnect(Server.ClientThread client) {
        if (client.username != null) {
            onlineUsers.remove(client.username);
            if (waitingClient == client) {
                waitingClient = null;
            }
            if (client.gameSession != null) {
                handleForfeit(client.gameSession, client.username);
            }
            server.log("User disconnected: " + client.username);
        }
    }

    public synchronized void playAgain(Server.ClientThread client) {
        if (client.gameSession != null) {
            activeGames.remove(client.gameSession);
            client.gameSession = null;
        }
        // treat as new matchmaking
        if (waitingClient == null) {
            waitingClient = client;
        } else {
            Server.ClientThread p1 = waitingClient;
            Server.ClientThread p2 = client;
            waitingClient = null;

            GameSession session = new GameSession(p1, p2, new Game(p1.username, p2.username));
            activeGames.add(session);
            p1.gameSession = session;
            p2.gameSession = session;

            // Notify P1
            Message m1 = new Message(Message.Action.MATCH_FOUND);
            m1.opponentName = p2.username;
            m1.isPlayer1 = true;
            p1.send(m1);

            // Notify P2
            Message m2 = new Message(Message.Action.MATCH_FOUND);
            m2.opponentName = p1.username;
            m2.isPlayer1 = false;
            p2.send(m2);

            broadcastGameState(session);
        }
    }

    public synchronized void processMessage(Server.ClientThread client, Message msg) {
        GameSession session = client.gameSession;
        if (session == null) return;

        if (msg.action == Message.Action.CHAT_MESSAGE) {
            Server.ClientThread opponent = session.getOpponent(client);
            if (opponent != null) {
                opponent.send(msg);
            }
        } else if (msg.action == Message.Action.MOVE) {
            boolean isP1 = client == session.p1;
            String error = session.game.attemptMove(isP1, msg.startX, msg.startY, msg.endX, msg.endY);
            if (error != null) {
                server.log("Invalid move by " + client.username + ": " + error);
                // Can send error back to client if needed
            }
            broadcastGameState(session);
        } else if (msg.action == Message.Action.PLAY_AGAIN) {
             playAgain(client);
        }
    }

    private void broadcastGameState(GameSession session) {
        Game game = session.game;
        
        Message update = new Message(Message.Action.GAME_STATE_UPDATE);
        update.board = game.getBoard();
        
        if (game.isGameOver()) {
            update.action = Message.Action.GAME_OVER;
            update.content = game.getWinner();
        }

        // Send to P1
        Message m1 = cloneUpdate(update);
        m1.isMyTurn = game.isP1Turn();
        m1.gameStatus = game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
        if (game.isGameOver()) {
            m1.gameStatus = "Winner: " + game.getWinner();
        }
        session.p1.send(m1);

        // Send to P2
        Message m2 = cloneUpdate(update);
        m2.isMyTurn = !game.isP1Turn();
        m2.gameStatus = !game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
        if (game.isGameOver()) {
            m2.gameStatus = "Winner: " + game.getWinner();
        }
        session.p2.send(m2);
    }
    
    private void handleForfeit(GameSession session, String leaver) {
        Server.ClientThread p1 = session.p1;
        Server.ClientThread p2 = session.p2;
        
        Message m = new Message(Message.Action.OPPONENT_LEFT);
        if (p1.username.equals(leaver)) {
            p2.send(m);
            p2.gameSession = null;
        } else {
            p1.send(m);
            p1.gameSession = null;
        }
        activeGames.remove(session);
    }

    private Message cloneUpdate(Message m) {
        Message n = new Message(m.action);
        n.board = m.board;
        n.content = m.content;
        return n;
    }

    class GameSession {
        Server.ClientThread p1;
        Server.ClientThread p2;
        Game game;

        GameSession(Server.ClientThread p1, Server.ClientThread p2, Game game) {
            this.p1 = p1;
            this.p2 = p2;
            this.game = game;
        }

        Server.ClientThread getOpponent(Server.ClientThread c) {
            if (c == p1) return p2;
            if (c == p2) return p1;
            return null;
        }
    }
}
