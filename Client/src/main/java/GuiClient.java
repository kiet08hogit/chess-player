import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.FadeTransition;
import javafx.animation.Transition;

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
    private Label waitingTitleLabel;
    private Label waitingSubLabel;
    String loggedInUser = "";
    boolean isP1;
    boolean isSpectatorMode = false;
    boolean spectatorViewP1 = true;
    int selectedX = -1;
    int selectedY = -1;
    private Message lastMessage;

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
        waitingTitleLabel.setText("Searching for opponent...");
        waitingSubLabel.setText("Please wait while we connect you to a game...");
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Searching for Match...");
    }

    public void onCreateRoom() {
        clientConnection.send(new Message(Message.Action.CREATE_ROOM));
        waitingTitleLabel.setText("Creating room...");
        waitingSubLabel.setText("Please wait...");
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Creating Room...");
    }

    public void onJoinFriend(String roomId) {
        Message m = new Message(Message.Action.JOIN_ROOM);
        m.roomId = roomId;
        clientConnection.send(m);
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Joining Room #" + roomId + "...");
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
        else if (data.action == Message.Action.CREATE_ROOM) {
            waitingTitleLabel.setText("Room ID: " + data.roomId);
            waitingSubLabel.setText("Tell your friend to enter this code to join you!");
        }
        else if (data.action == Message.Action.LOGIN_FAIL) {
            new Alert(AlertType.ERROR, "Login Failed: " + data.content).show();
        } 
        else if (data.action == Message.Action.MATCH_FOUND || data.action == Message.Action.GAME_STATE_UPDATE) {
            if (primaryStage.getScene() != gameScene) {
                primaryStage.setScene(gameScene);
                gameView.clearChat();
            }
            
            this.lastMessage = data;
            
            if (data.action == Message.Action.MATCH_FOUND) {
                this.isP1 = data.isPlayer1;
                this.isSpectatorMode = false;
                primaryStage.setTitle("Checkers Room #" + data.roomId);
                gameView.addChatMessage("System: Joined match " + data.roomId);
                gameView.resetUI();
                gameView.setBotMatchMode(data.isBotMatch);
            }
            
            if (data.isSpectator) {
                this.isSpectatorMode = true;
                this.isP1 = spectatorViewP1; 
                primaryStage.setTitle("Spectating Room #" + data.roomId);
            }
            
            gameView.updateState(data);
        } 
        else if (data.action == Message.Action.REMATCH_REQUEST) {
            gameView.showRematchRequest();
        }
        else if (data.action == Message.Action.CHAT_MESSAGE) {
            gameView.addChatMessage(data.username + ": " + data.content);
        }
        else if (data.action == Message.Action.GLOBAL_CHAT) {
            homeView.addGlobalChatMessage(data.username + ": " + data.content);
        }
        else if (data.action == Message.Action.UPDATE_PLAYER_LIST) {
            homeView.updatePlayerList(data);
        }
        else if (data.action == Message.Action.GAME_OVER) {
            gameView.updateState(data);
            gameView.showGameOver(data.content);
        }
        else if (data.action == Message.Action.OPPONENT_LEFT) {
            gameView.showOpponentLeft(data.content, data.gameStatus);
        }
        else if (data.action == Message.Action.ERROR) {
            if (primaryStage.getScene() == gameScene) {
                gameView.showGenericNotification("NOTIFICATION", data.content);
            } else {
                new Alert(AlertType.WARNING, data.content).show();
            }
            
            if (data.content.contains("Match ended") || data.content.contains("Room not found")) {
                primaryStage.setScene(homeScene);
            }
        }
    }

    public void refreshView() {
        if (lastMessage != null) {
            this.isP1 = isSpectatorMode ? spectatorViewP1 : isP1;
            gameView.updateState(lastMessage);
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
        waitingTitleLabel = new Label("Searching for opponent...");
        waitingTitleLabel.getStyleClass().add("searching-label");
        waitingTitleLabel.setStyle("-fx-text-fill: white;"); // Ensure it's white
        
        Circle pulse = new Circle(30, Color.web("#DC2626"));
        FadeTransition ft = new FadeTransition(Duration.seconds(1), pulse);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(Transition.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
        
        waitingSubLabel = new Label("Please wait while we connect you to a game...");
        waitingSubLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
        
        Button backHomeBtn = new Button("BACK HOME");
        backHomeBtn.getStyleClass().add("menu-button");
        backHomeBtn.setMinWidth(200);
        backHomeBtn.setOnAction(e -> {
            clientConnection.send(new Message(Message.Action.CANCEL_WAITING));
            primaryStage.setScene(homeScene);
            primaryStage.setTitle("Checkers - Home");
        });

        root.getChildren().addAll(waitingTitleLabel, pulse, waitingSubLabel, backHomeBtn);
        return new Scene(root, 500, 450);
    }
}
