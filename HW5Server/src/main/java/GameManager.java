import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameManager {
    private HashMap<String, Server.ClientThread> onlineUsers = new HashMap<>();
    private HashMap<String, GameSession> roomMap = new HashMap<>();
    private List<GameSession> activeGames = new ArrayList<>();
    
    private Server server;
    private Random random = new Random();

    public GameManager(Server server) {
        this.server = server;
    }

    private String generateRoomId() {
        String id;
        do {
            id = String.format("%04d", random.nextInt(10000));
        } while (roomMap.containsKey(id));
        return id;
    }

    public synchronized boolean login(String username, Server.ClientThread client) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }
        onlineUsers.put(username, client);
        client.username = username;
        client.status = "Online";
        
        server.log(username + " has join the server");

        broadcastOnlinePlayers();
        return true;
    }

    public synchronized void disconnect(Server.ClientThread client) {
        if (client.username != null) {
            onlineUsers.remove(client.username);
            if (client.gameSession != null) {
                if (client == client.gameSession.p1 || client == client.gameSession.p2) {
                    handleForfeit(client.gameSession, client.username);
                } else {
                    client.gameSession.spectators.remove(client);
                }
            }
            server.log(client.username + " left the server");
            broadcastOnlinePlayers();
        }
    }

    private void broadcastOnlinePlayers() {
        Message msg = new Message(Message.Action.UPDATE_PLAYER_LIST);
        HashMap<String, String> statusMap = new HashMap<>();
        
        for (Server.ClientThread client : onlineUsers.values()) {
            String displayStatus = client.status;
            if ("In a match".equals(client.status) && client.gameSession != null) {
                displayStatus = "in a game #" + client.gameSession.roomId;
            }
            statusMap.put(client.username, displayStatus);
        }
        msg.playerStatusMap = statusMap;
        
        for (Server.ClientThread client : onlineUsers.values()) {
            client.send(msg);
        }
    }

    public synchronized void findMatch(Server.ClientThread client) {
        client.status = "Waiting for opponent";
        broadcastOnlinePlayers();

        Server.ClientThread opponent = null;
        for (Server.ClientThread other : onlineUsers.values()) {
            if (other != client && "Waiting for opponent".equals(other.status)) {
                opponent = other;
                break;
            }
        }

        if (opponent != null) {
            startMatch(client, opponent);
        }
    }

    private void startMatch(Server.ClientThread p1, Server.ClientThread p2) {
        String roomId = generateRoomId();
        GameSession session = new GameSession(p1, p2, new Game(p1.username, p2.username), roomId);
        
        p1.status = "In a match";
        p2.status = "In a match";
        p1.gameSession = session;
        p2.gameSession = session;
        
        roomMap.put(roomId, session);
        activeGames.add(session);

        server.log(p1.username + " and " + p2.username + " has join a match, room id: #" + roomId);

        sendMatchStart(session);
        broadcastOnlinePlayers();
    }
    
    private void sendMatchStart(GameSession session) {
        Message m1 = new Message(Message.Action.MATCH_FOUND);
        m1.opponentName = session.p2.username;
        m1.player1Name = session.p1.username;
        m1.player2Name = session.p2.username;
        m1.isPlayer1 = true;
        m1.roomId = session.roomId;
        m1.p1Score = session.p1Score;
        m1.p2Score = session.p2Score;
        session.p1.send(m1);

        Message m2 = new Message(Message.Action.MATCH_FOUND);
        m2.opponentName = session.p1.username;
        m2.player1Name = session.p1.username;
        m2.player2Name = session.p2.username;
        m2.isPlayer1 = false;
        m2.roomId = session.roomId;
        m2.p1Score = session.p1Score;
        m2.p2Score = session.p2Score;
        session.p2.send(m2);

        broadcastGameState(session);
    }

    public synchronized void watchMatch(Server.ClientThread client, String roomId) {
        GameSession session = roomMap.get(roomId);
        if (session != null) {
            client.gameSession = session;
            session.spectators.add(client);
            client.status = "Watching #" + roomId;
            
            server.log(client.username + " is watching " + session.p1.username + " and " + session.p2.username + " 's match, room id: " + roomId);
            
            Message update = new Message(Message.Action.GAME_STATE_UPDATE);
            update.board = session.game.getBoard();
            update.isSpectator = true;
            update.player1Name = session.p1.username;
            update.player2Name = session.p2.username;
            update.p1Score = session.p1Score;
            update.p2Score = session.p2Score;
            update.roomId = session.roomId;
            update.gameStatus = "Spectating: " + session.p1.username + " vs " + session.p2.username;
            client.send(update);
            
            broadcastOnlinePlayers();
        } else {
            Message err = new Message(Message.Action.ERROR);
            err.content = "Room not found: " + roomId;
            client.send(err);
        }
    }

    public synchronized void playAgain(Server.ClientThread client) {
        if (client.gameSession != null) {
            GameSession session = client.gameSession;
            if (client == session.p1 || client == session.p2) {
                 Server.ClientThread opponent = session.getOpponent(client);
                 if (opponent != null) {
                     opponent.send(new Message(Message.Action.OPPONENT_LEFT));
                     opponent.status = "Online";
                     opponent.gameSession = null;
                 }
                 
                 roomMap.remove(session.roomId);
                 activeGames.remove(session);
                 
                 Message m = new Message(Message.Action.ERROR);
                 m.content = "Match ended.";
                 for (Server.ClientThread s : session.spectators) {
                     s.send(m);
                     s.status = "Online";
                     s.gameSession = null;
                 }
            } else {
                // Spectator leaving
                session.spectators.remove(client);
            }
            client.gameSession = null;
        }
        client.status = "Online";
        broadcastOnlinePlayers();
    }

    public synchronized void processMessage(Server.ClientThread client, Message msg) {
        if (msg.action == Message.Action.FIND_MATCH) {
            findMatch(client);
        } else if (msg.action == Message.Action.WATCH_MATCH) {
            watchMatch(client, msg.roomId);
        } else if (msg.action == Message.Action.CHAT_MESSAGE) {
            GameSession session = client.gameSession;
            if (session != null) {
                msg.username = client.username;
                // Broadcast to everyone in the room
                session.p1.send(msg);
                session.p2.send(msg);
                for (Server.ClientThread s : session.spectators) {
                    if (s != client) s.send(msg);
                }
                server.log(client.username + " sent a room message in #" + session.roomId);
            } else {
                server.log(client.username + " send a message to server: " + msg.content);
            }
        } else if (msg.action == Message.Action.SURRENDER) {
            GameSession session = client.gameSession;
            if (session != null) {
                if (client != session.p1 && client != session.p2) return; // Spectators can't surrender
                String winner = (client == session.p1) ? session.p2.username : session.p1.username;
                session.game.setGameOver(winner);
                broadcastGameState(session);
            }
        } else if (msg.action == Message.Action.REMATCH) {
            GameSession session = client.gameSession;
            if (session != null) {
                if (client != session.p1 && client != session.p2) return; // Spectators can't rematch
                Server.ClientThread tempP = session.p1;
                session.p1 = session.p2;
                session.p2 = tempP;
                
                int tempS = session.p1Score;
                session.p1Score = session.p2Score;
                session.p2Score = tempS;

                session.game = new Game(session.p1.username, session.p2.username);
                sendMatchStart(session);
            }
        } else {
            GameSession session = client.gameSession;
            if (session == null) return;

            if (msg.action == Message.Action.MOVE) {
                if (client != session.p1 && client != session.p2) return; 

                boolean isP1 = client == session.p1;
                String error = session.game.attemptMove(isP1, msg.startX, msg.startY, msg.endX, msg.endY);
                if (error != null) {
                    Message errMsg = new Message(Message.Action.ERROR);
                    errMsg.content = error;
                    client.send(errMsg);
                }
                broadcastGameState(session);
            } else if (msg.action == Message.Action.PLAY_AGAIN) {
                 playAgain(client);
            }
        }
    }

    private void broadcastGameState(GameSession session) {
        Game game = session.game;
        Message update = new Message(Message.Action.GAME_STATE_UPDATE);
        update.board = game.getBoard();
        update.roomId = session.roomId;
        update.player1Name = session.p1.username;
        update.player2Name = session.p2.username;
        update.p1Score = session.p1Score;
        update.p2Score = session.p2Score;
        
        if (game.isGameOver()) {
            update.action = Message.Action.GAME_OVER;
            update.content = game.getWinner();
            
            if (game.getWinner().equals(session.p1.username)) {
                session.p1Score++;
            } else if (game.getWinner().equals(session.p2.username)) {
                session.p2Score++;
            }
            update.p1Score = session.p1Score;
            update.p2Score = session.p2Score;
            
            server.log(game.getWinner() + " won " + (game.getWinner().equals(session.p1.username) ? session.p2.username : session.p1.username) + ", room id: " + session.roomId + " [Score: " + session.p1Score + "-" + session.p2Score + "]");
        }

        Message m1 = cloneUpdate(update);
        m1.isMyTurn = game.isP1Turn();
        m1.gameStatus = game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
        if (game.isGameOver()) m1.gameStatus = "Winner: " + game.getWinner();
        session.p1.send(m1);

        Message m2 = cloneUpdate(update);
        m2.isMyTurn = !game.isP1Turn();
        m2.gameStatus = !game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
        if (game.isGameOver()) m2.gameStatus = "Winner: " + game.getWinner();
        session.p2.send(m2);
        
        for (Server.ClientThread s : session.spectators) {
            Message ms = cloneUpdate(update);
            ms.isSpectator = true;
            ms.gameStatus = "Spectating: " + session.p1.username + " vs " + session.p2.username;
            if (game.isGameOver()) ms.gameStatus = "Winner: " + game.getWinner();
            s.send(ms);
        }
    }
    
    private void handleForfeit(GameSession session, String leaver) {
        Message m = new Message(Message.Action.OPPONENT_LEFT);
        m.roomId = session.roomId;
        
        if (session.p1.username.equals(leaver)) {
            session.p2.send(m);
            session.p2.gameSession = null;
            session.p2.status = "Online";
        } else {
            session.p1.send(m);
            session.p1.gameSession = null;
            session.p1.status = "Online";
        }
        
        Message ms = new Message(Message.Action.ERROR);
        ms.content = leaver + " left the match. Game ended.";
        for (Server.ClientThread s : session.spectators) {
            s.send(ms);
            s.status = "Online";
            s.gameSession = null;
        }
        
        roomMap.remove(session.roomId);
        activeGames.remove(session);
        broadcastOnlinePlayers();
    }

    private Message cloneUpdate(Message m) {
        Message n = new Message(m.action);
        n.board = m.board;
        n.content = m.content;
        n.roomId = m.roomId;
        n.p1Score = m.p1Score;
        n.p2Score = m.p2Score;
        n.player1Name = m.player1Name;
        n.player2Name = m.player2Name;
        return n;
    }

    public class GameSession {
        Server.ClientThread p1;
        Server.ClientThread p2;
        Game game;
        String roomId;
        List<Server.ClientThread> spectators = new ArrayList<>();
        int p1Score = 0;
        int p2Score = 0;

        GameSession(Server.ClientThread p1, Server.ClientThread p2, Game game, String roomId) {
            this.p1 = p1;
            this.p2 = p2;
            this.game = game;
            this.roomId = roomId;
        }

        Server.ClientThread getOpponent(Server.ClientThread c) {
            if (c == p1) return p2;
            if (c == p2) return p1;
            return null;
        }
    }
}
