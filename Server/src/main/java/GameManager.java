import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameManager {
    private HashMap<String, Server.ClientThread> onlineUsers = new HashMap<>();
    private HashMap<String, GameSession> roomMap = new HashMap<>();
    private HashMap<String, Server.ClientThread> waitingRooms = new HashMap<>();
    private List<GameSession> activeGames = new ArrayList<>();
    
    private Server server;
    private Random random = new Random();
    private DatabaseManager db = new DatabaseManager();

    // constructor
    public GameManager(Server server) {
        this.server = server;
    }

    // generate room id for friends
    private String generateRoomId() {
        String id;
        do {
            id = String.format("%04d", random.nextInt(10000));
        } while (roomMap.containsKey(id));
        return id;
    }

    // login user 
    public synchronized boolean login(String username, String password, Server.ClientThread client) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }
        
        if (!db.authenticateUser(username, password)) {
            return false;
        }
        
        onlineUsers.put(username, client);
        client.username = username;
        
        server.log("User logged in: " + username);
        
        // Broadcast user list to everyone
        broadcastOnlinePlayers();
        
        // Send LOGIN_SUCCESS immediately
        client.send(new Message(Message.Action.LOGIN_SUCCESS));
        
        return true;
    }

    // sign up user
    public synchronized boolean signup(String username, String password, Server.ClientThread client) {
        if (onlineUsers.containsKey(username)) {
            return false;
        }
        
        if (!db.registerUser(username, password)) {
            return false;
        }
        
        server.log("New user registered: " + username);
        client.send(new Message(Message.Action.SIGNUP_SUCCESS));
        
        return true;
    }

    // disconnect user
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

    // broadcast online players
    private synchronized void broadcastOnlinePlayers() {
        Message msg = new Message(Message.Action.UPDATE_PLAYER_LIST);
        HashMap<String, String> statusMap = new HashMap<>();
        
        // though we are in a synchronized method.
        for (String uname : onlineUsers.keySet()) {
            Server.ClientThread c = onlineUsers.get(uname);
            String displayStatus = c.status;
            
            if ("In a match".equals(c.status) && c.gameSession != null) {
                if (c.gameSession.isBotMatch) {
                    displayStatus = "(training)";
                } else {
                    displayStatus = "in a game #" + c.gameSession.roomId;
                }
            }
            
            int[] stats = db.getStats(uname);
            displayStatus += " [W:" + stats[0] + " L:" + stats[1] + "]";
            
            statusMap.put(uname, displayStatus);
        }
        msg.playerStatusMap = statusMap;
        
        for (Server.ClientThread client : onlineUsers.values()) {
            client.send(msg);
        }
    }

    // find match
    public synchronized void findMatch(Server.ClientThread client) {
        client.status = "Waiting for opponent";
        server.log(client.username + " is waiting for an opponent");
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

    // start match for two players
    private void startMatch(Server.ClientThread p1, Server.ClientThread p2) {
        startMatch(p1, p2, generateRoomId());
    }

    // start match for two players with room id
    private void startMatch(Server.ClientThread p1, Server.ClientThread p2, String roomId) {
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
    
    // send match start
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

    // connect a client to an active match as a spectator
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
            
            // Send chat history to new spectator
            for (String chatMsg : session.chatHistory) {
                Message cm = new Message(Message.Action.CHAT_MESSAGE);
                cm.content = chatMsg;
                cm.username = "History"; 
                client.send(cm);
            }
            
            broadcastOnlinePlayers();
        } else {
            Message err = new Message(Message.Action.ERROR);
            err.content = "Room not found: " + roomId;
            client.send(err);
        }
    }

    // handle client requesting to leave a match and return to the main lobby
    public synchronized void playAgain(Server.ClientThread client) {
        if (client.gameSession != null) {
            GameSession session = client.gameSession;
            if (client == session.p1 || client == session.p2) {
                 Server.ClientThread opponent = session.getOpponent(client);
                 if (opponent != null) {
                     Message m = new Message(Message.Action.OPPONENT_LEFT);
                     m.content = client.username;
                     m.gameStatus = "MATCH_LEFT";
                     opponent.send(m);
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

    // The central router for processing all in-game and lobby messages from clients
    public synchronized void processMessage(Server.ClientThread client, Message msg) {
        if (msg.action == Message.Action.FIND_MATCH) {
            findMatch(client);
        } else if (msg.action == Message.Action.CREATE_ROOM) {
            String roomId = generateRoomId();
            client.status = "Waiting in a room";
            waitingRooms.put(roomId, client);
            broadcastOnlinePlayers();
            
            Message m = new Message(Message.Action.CREATE_ROOM);
            m.roomId = roomId;
            client.send(m);
            server.log(client.username + " created room #" + roomId);
            server.log(client.username + " is waiting for friends in room #" + roomId);
        } else if (msg.action == Message.Action.JOIN_ROOM) {
            if (waitingRooms.containsKey(msg.roomId)) {
                Server.ClientThread host = waitingRooms.remove(msg.roomId);
                startMatch(host, client, msg.roomId);
            } else {
                Message err = new Message(Message.Action.ERROR);
                err.content = "Room #" + msg.roomId + " does not exist or is already full.";
                client.send(err);
            }
        } else if (msg.action == Message.Action.PLAY_BOT) {
            playBot(client, msg.botLevel);
        } else if (msg.action == Message.Action.CANCEL_WAITING) {
            server.log(client.username + " cancelled waiting");
            client.status = "Online";
            waitingRooms.values().remove(client);
            broadcastOnlinePlayers();
        } else if (msg.action == Message.Action.WATCH_MATCH) {
            watchMatch(client, msg.roomId);
        } else if (msg.action == Message.Action.CHAT_MESSAGE) {
            GameSession session = client.gameSession;
            if (session != null) {
                msg.username = client.username;
                msg.content = filterProfanity(msg.content);
                
                String historyEntry = msg.username + ": " + msg.content;
                session.chatHistory.add(historyEntry);
                
                // Broadcast to everyone in the room
                session.p1.send(msg);
                session.p2.send(msg);
                for (Server.ClientThread s : session.spectators) {
                    s.send(msg);
                }
                server.log(client.username + " sent a room message in #" + session.roomId);
            } else {
                server.log(client.username + " send a message to server: " + msg.content);
            }
        } else if (msg.action == Message.Action.GLOBAL_CHAT) {
            msg.username = client.username;
            msg.content = filterProfanity(msg.content);
            for (Server.ClientThread s : onlineUsers.values()) {
                s.send(msg);
            }
            server.log("Global chat from " + client.username + ": " + msg.content);
        } else if (msg.action == Message.Action.SURRENDER) {
            GameSession session = client.gameSession;
            if (session != null) {
                if (client != session.p1 && client != session.p2) return; 
                String winner = (client == session.p1) ? session.p2.username : session.p1.username;
                session.game.setGameOver(winner);
                
                if (winner.equals(session.p1.username)) {
                    session.p1Score++;
                } else {
                    session.p2Score++;
                }
                
                if (!session.isBotMatch) {
                    db.addWin(winner);
                    db.addLoss(client.username);
                }
                
                server.log(client.username + " surrendered, " + winner + " won. Room ID: #" + session.roomId + " [Score: " + session.p1Score + "-" + session.p2Score + "]");
                
                broadcastGameState(session);
            }
        } else if (msg.action == Message.Action.REMATCH) {
            GameSession session = client.gameSession;
            if (session != null) {
                if (client != session.p1 && client != session.p2) return; 
                Server.ClientThread opponent = session.getOpponent(client);
                if (opponent != null) {
                    opponent.send(new Message(Message.Action.REMATCH_REQUEST));
                    server.log(client.username + " requested a rematch against " + opponent.username);
                }
            }
        } else if (msg.action == Message.Action.REMATCH_ACCEPT) {
            GameSession session = client.gameSession;
            if (session != null) {
                if (client != session.p1 && client != session.p2) return;
                
                server.log(client.username + " accepted rematch in room #" + session.roomId);
                
                Server.ClientThread tempP = session.p1;
                session.p1 = session.p2;
                session.p2 = tempP;
                
                int tempS = session.p1Score;
                session.p1Score = session.p2Score;
                session.p2Score = tempS;

                session.game = new Game(session.p1.username, session.p2.username);
                sendMatchStart(session);
            }
        } else if (msg.action == Message.Action.REMATCH_DECLINE) {
            GameSession session = client.gameSession;
            if (session != null) {
                Server.ClientThread opponent = session.getOpponent(client);
                if (opponent != null) {
                    Message m = new Message(Message.Action.ERROR);
                    m.content = "Opponent declined rematch.";
                    opponent.send(m);
                }
                playAgain(client);
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
                
                if (session.game.isGameOver()) {
                    String winner = session.game.getWinner();
                    String loser = "";
                    if (winner.equals(session.p1.username)) {
                        session.p1Score++;
                        loser = session.isBotMatch ? "BOT" : (session.p2 != null ? session.p2.username : "unknown");
                    } else {
                        session.p2Score++;
                        loser = session.p1.username;
                    }
                    
                    if (!session.isBotMatch) {
                        db.addWin(winner);
                        db.addLoss(loser);
                    }
                    
                    server.log(winner + " won against " + loser + ". Room ID: #" + session.roomId + " [Score: " + session.p1Score + "-" + session.p2Score + "]");
                }
                
                broadcastGameState(session);

                if (session.isBotMatch && !session.game.isGameOver() && !session.game.isP1Turn()) {
                    new Thread(() -> {
                        try { Thread.sleep(800); } catch (Exception e) {}
                        makeBotMove(session);
                    }).start();
                }
            } else if (msg.action == Message.Action.PLAY_AGAIN) {
                 playAgain(client);
            }
        }
    }

    // broadcast game state
    private void broadcastGameState(GameSession session) {
        Game game = session.game;
        Message update = new Message(Message.Action.GAME_STATE_UPDATE);
        update.board = game.getBoard();
        update.roomId = session.roomId;
        update.player1Name = session.p1.username;
        update.player2Name = (session.p2 != null) ? session.p2.username : "BOT";
        update.p1Score = session.p1Score;
        update.p2Score = session.p2Score;
        update.isBotMatch = session.isBotMatch;
        
        if (game.isGameOver()) {
            update.action = Message.Action.GAME_OVER;
            update.content = game.getWinner();
            update.p1Score = session.p1Score;
            update.p2Score = session.p2Score;
        }

        Message m1 = cloneUpdate(update);
        m1.isMyTurn = game.isP1Turn();
        m1.gameStatus = game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
        if (game.isGameOver()) m1.gameStatus = "Winner: " + game.getWinner();
        session.p1.send(m1);

        if (session.p2 != null) {
            Message m2 = cloneUpdate(update);
            m2.isMyTurn = !game.isP1Turn();
            m2.gameStatus = !game.isP1Turn() ? "Your Turn" : "Opponent's Turn";
            if (game.isGameOver()) m2.gameStatus = "Winner: " + game.getWinner();
            session.p2.send(m2);
        }
        
        for (Server.ClientThread s : session.spectators) {
            Message ms = cloneUpdate(update);
            ms.isSpectator = true;
            ms.gameStatus = "Spectating: " + session.p1.username + " vs " + session.p2.username;
            if (game.isGameOver()) ms.gameStatus = "Winner: " + game.getWinner();
            s.send(ms);
        }
    }
    
    // handle when a player disconnects or leaves mid-match, awarding a forfeit win to the opponent
    private void handleForfeit(GameSession session, String leaver) {
        Message m = new Message(Message.Action.OPPONENT_LEFT);
        m.roomId = session.roomId;
        m.content = leaver;
        m.gameStatus = "SERVER_DISCONNECT";
        
        if (session.p1 != null && session.p1.username.equals(leaver)) {
            if (session.p2 != null) {
                session.p2.send(m);
                session.p2.gameSession = null;
                session.p2.status = "Online";
            }
        } else if (session.p2 != null && session.p2.username.equals(leaver)) {
            if (session.p1 != null) {
                session.p1.send(m);
                session.p1.gameSession = null;
                session.p1.status = "Online";
            }
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

    // helper method to deep clone a game state message so it can be customized for individual players
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

    // start solo training match against server bot
    private void playBot(Server.ClientThread client, int level) {
        client.status = "In a match";
        Game botGame = new Game(client.username, "BOT");
        GameSession session = new GameSession(client, null, botGame, "BOT_ROOM");
        session.isBotMatch = true;
        session.botLevel = level;
        client.gameSession = session;
        
        server.log(client.username + " started training with bot (Level " + level + ")");
        
        Message m = new Message(Message.Action.MATCH_FOUND);
        m.roomId = "TRAINING";
        m.opponentName = "BOT (Lv" + level + ")";
        m.isPlayer1 = true;
        m.isBotMatch = true;
        m.botLevel = level;
        m.board = botGame.getBoard();
        client.send(m);
        
        broadcastGameState(session);
        broadcastOnlinePlayers();
    }

    // Executes the bot's turn in a training match based on its difficulty level.
    // Level 1: Random moves. Level 2: Prioritizes jump captures.
    private void makeBotMove(GameSession session) {
        synchronized(this) {
            if (session.game.isGameOver() || session.game.isP1Turn()) return;
            
            List<int[]> moves = session.game.getAllValidMoves(false);
            if (moves.isEmpty()) return;
            
            int[] choice;
            if (session.botLevel == 2) {
                // Prioritize jumps (index 4 is jump flag)
                List<int[]> jumps = new ArrayList<>();
                for (int[] m : moves) if (m[4] == 1) jumps.add(m);
                
                if (!jumps.isEmpty()) {
                    choice = jumps.get(random.nextInt(jumps.size()));
                } else {
                    choice = moves.get(random.nextInt(moves.size()));
                }
            } else {
                // Random move
                choice = moves.get(random.nextInt(moves.size()));
            }
            
            session.game.attemptMove(false, choice[0], choice[1], choice[2], choice[3]);
            broadcastGameState(session);
            
            // Check if bot can jump again (double jump)
            if (!session.game.isGameOver() && !session.game.isP1Turn()) {
                new Thread(() -> {
                    try { Thread.sleep(600); } catch (Exception e) {}
                    makeBotMove(session);
                }).start();
            }
        }
    }

    // filters profanity from chat messages using asterisks
    private String filterProfanity(String input) {
        if (input == null) return null;
        String[] badWords = { 
            "fuck", "shit", "bitch", "asshole", "dick", "pussy", 
            "porn", "sex", "nude", "slut", "whore", "crap", "cunt",
            "faggot", "nigger", "nigga", "cock", "penis", "vagina", "boob"
        };
        String filtered = input;
        for (String word : badWords) {
            String regex = "(?i)" + java.util.regex.Pattern.quote(word);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < word.length(); i++) sb.append("*");
            filtered = filtered.replaceAll(regex, sb.toString());
        }
        return filtered;
    }

    public class GameSession {
        Server.ClientThread p1;
        Server.ClientThread p2;
        Game game;
        String roomId;
        List<Server.ClientThread> spectators = new ArrayList<>();
        List<String> chatHistory = new ArrayList<>();
        int p1Score = 0;
        int p2Score = 0;
        boolean isBotMatch = false;
        int botLevel = 1;

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
