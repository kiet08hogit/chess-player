import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

public class GuiClient extends Application {

    Client clientConnection;
    Stage primaryStage;
    
    // View References
    LoginView loginView;
    HomeView homeView;
    GameView gameView;
    
    // Persistent Scenes
    Scene loginScene;
    Scene homeScene;
    Scene gameScene;
    Scene waitingScene;
    
    // Global State
    String loggedInUser = "";
    boolean isP1;
    boolean isSpectatorMode = false;
    int selectedX = -1;
    int selectedY = -1;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        clientConnection = new Client(data -> {
            Platform.runLater(() -> handleServerMessage((Message) data));
        });
        clientConnection.start();

        // Initialize views
        loginView = new LoginView(this);
        homeView = new HomeView(this);
        gameView = new GameView(this);

        // Create persistent scenes
        loginScene = new Scene(loginView.getRoot(), 500, 450);
        homeScene = new Scene(homeView.getRoot(), 1150, 750);
        gameScene = new Scene(gameView.getRoot(), 1150, 750);
        waitingScene = createWaitingGui();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(loginScene);
        primaryStage.setTitle("Checkers Login");
        primaryStage.show();
    }
    
    public void onLoginAttempt(String user) {
        this.loggedInUser = user;
        Message m = new Message(Message.Action.LOGIN);
        m.username = user;
        clientConnection.send(m);
    }

    public void onFindMatch() {
        clientConnection.send(new Message(Message.Action.FIND_MATCH));
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Searching for Match...");
    }

    public void onJoinRoom(String roomId) {
        Message m = new Message(Message.Action.JOIN_ROOM);
        m.roomId = roomId;
        clientConnection.send(m);
    }

    public void onWatchMatch(String roomId) {
        Message m = new Message(Message.Action.WATCH_MATCH);
        m.roomId = roomId;
        clientConnection.send(m);
    }

    private void handleServerMessage(Message data) {
        if (data.action == Message.Action.LOGIN_SUCCESS) {
            homeView.setUserLabel(loggedInUser);
            primaryStage.setScene(homeScene);
            primaryStage.setTitle("Checkers - Home");
        } 
        else if (data.action == Message.Action.LOGIN_FAIL) {
            new Alert(AlertType.ERROR, "Login Failed: " + data.content).show();
        } 
        else if (data.action == Message.Action.MATCH_FOUND || data.action == Message.Action.GAME_STATE_UPDATE) {
            if (primaryStage.getScene() != gameScene) {
                primaryStage.setScene(gameScene);
                gameView.clearChat();
                gameView.resetUI();
            }
            
            if (data.action == Message.Action.MATCH_FOUND) {
                this.isP1 = data.isPlayer1;
                this.isSpectatorMode = false;
                primaryStage.setTitle("Checkers Room #" + data.roomId);
                gameView.addChatMessage("System: Joined match " + data.roomId);
            }
            
            if (data.isSpectator) {
                this.isSpectatorMode = true;
                this.isP1 = true; 
                primaryStage.setTitle("Spectating Room #" + data.roomId);
            }
            
            gameView.updateState(data);
        } 
        else if (data.action == Message.Action.CHAT_MESSAGE) {
            gameView.addChatMessage(data.username + ": " + data.content);
        }
        else if (data.action == Message.Action.UPDATE_PLAYER_LIST) {
            homeView.updatePlayerList(data);
        }
        else if (data.action == Message.Action.GAME_OVER) {
            gameView.updateState(data);
            gameView.showGameOver(data.content);
        }
        else if (data.action == Message.Action.OPPONENT_LEFT) {
            new Alert(AlertType.WARNING, "Match ended. Returning to Home.").show();
            primaryStage.setScene(homeScene);
        }
        else if (data.action == Message.Action.ERROR) {
            new Alert(AlertType.WARNING, data.content).show();
            if (data.content.contains("Match ended") || data.content.contains("Room not found")) {
                primaryStage.setScene(homeScene);
            }
        }
    }

    public void handleTileClick(int x, int y, int piece) {
        boolean ownPiece = false;
        if (isP1 && (piece == 1 || piece == 3)) ownPiece = true;
        if (!isP1 && (piece == 2 || piece == 4)) ownPiece = true;

        if (selectedX == -1) {
            if (ownPiece) {
                selectedX = x;
                selectedY = y;
                gameView.drawBoard();
            }
        } else {
            if (x == selectedX && y == selectedY) {
                selectedX = -1;
                selectedY = -1;
                gameView.drawBoard();
            } else if (ownPiece) {
                selectedX = x;
                selectedY = y;
                gameView.drawBoard();
            } else {
                Message m = new Message(Message.Action.MOVE);
                m.startX = selectedX;
                m.startY = selectedY;
                m.endX = x;
                m.endY = y;
                clientConnection.send(m);
                selectedX = -1;
                selectedY = -1;
            }
        }
    }
    
    public void returnToHome() {
        clientConnection.send(new Message(Message.Action.PLAY_AGAIN));
        primaryStage.setScene(homeScene);
    }

    private Scene createWaitingGui() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        try { root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception e) {}
        Label searchingLabel = new Label("Searching for opponent...");
        searchingLabel.getStyleClass().add("title-label");
        Label subLabel = new Label("Please wait while we find a worthy match for you.");
        subLabel.getStyleClass().add("waiting-label");
        Circle pulse = new Circle(10, Color.web("#007bff"));
        root.getChildren().addAll(searchingLabel, pulse, subLabel);
        return new Scene(root, 500, 450);
    }
}
