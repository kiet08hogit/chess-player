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
    SignupView signupView;
    HomeView homeView;
    GameView gameView;
    
    // Persistent Scenes
    Scene loginScene;
    Scene signupScene;
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

    // main 
    public static void main(String[] args) {
        launch(args);
    }
    

    // start method 
    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        clientConnection = new Client(data -> {
            Platform.runLater(() -> handleServerMessage((Message) data));
        });
        clientConnection.start();

        // Initialize views
        loginView = new LoginView(this);
        signupView = new SignupView(this);
        homeView = new HomeView(this);
        gameView = new GameView(this);

        // Create persistent scenes
        loginScene = new Scene(loginView.getRoot(), 500, 450);
        signupScene = new Scene(signupView.getRoot(), 500, 450);
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
    
    // user login with username and password, then client sends login request to server
    public void onLoginAttempt(String user, String pass) {
        this.loggedInUser = user;
        Message m = new Message(Message.Action.LOGIN);
        m.username = user;
        m.password = pass;
        clientConnection.send(m);
    }

    // user signup with username and password, then client sends signup request to server
    public void onSignupAttempt(String user, String pass) {
        Message m = new Message(Message.Action.SIGNUP);
        m.username = user;
        m.password = pass;
        clientConnection.send(m);
    }
    
    // signup page 
    public void showSignupPage() {
        signupView.clearFields();
        primaryStage.setScene(signupScene);
        primaryStage.setTitle("Checkers - Sign Up");
    }
    
    // login page
    public void showLoginPage() {
        primaryStage.setScene(loginScene);
        primaryStage.setTitle("Checkers Login");
    }

    // find match button clicked in home view
    public void onFindMatch() {
        clientConnection.send(new Message(Message.Action.FIND_MATCH));
        waitingTitleLabel.setText("Searching for opponent...");
        waitingSubLabel.setText("Please wait while we connect you to a game...");
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Searching for Match...");
    }

    // create room button clicked in home view
    public void onCreateRoom() {
        clientConnection.send(new Message(Message.Action.CREATE_ROOM));
        waitingTitleLabel.setText("Creating room...");
        waitingSubLabel.setText("Please wait...");
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Creating Room...");
    }

    // join room button clicked in home view
    public void onJoinFriend(String roomId) {
        Message m = new Message(Message.Action.JOIN_ROOM);
        m.roomId = roomId;
        clientConnection.send(m);
        primaryStage.setScene(waitingScene);
        primaryStage.setTitle("Joining Room #" + roomId + "...");
    }

    // watch match button clicked in home view
    public void onWatchMatch(String roomId) {
        Message m = new Message(Message.Action.WATCH_MATCH);
        m.roomId = roomId;
        clientConnection.send(m);
    }

    // play bot button clicked in home view
    public void onPlayBotClicked() {
        primaryStage.setScene(gameScene);
        gameView.resetUI();
        gameView.showBotLevelSelection();
    }

    // handles server messages
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
        else if (data.action == Message.Action.SIGNUP_FAIL) {
            new Alert(AlertType.ERROR, "Signup Failed: " + data.content).show();
        }
        else if (data.action == Message.Action.SIGNUP_SUCCESS) {
            Alert alert = new Alert(AlertType.INFORMATION);
            alert.setTitle("Signup Successful");
            alert.setHeaderText(null);
            alert.setContentText("Your account has been created! You can now log in.");
            alert.showAndWait();
            showLoginPage();
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
                gameView.setBotMatchMode(data.isBotMatch, data.botLevel);
            }
            
            if (data.isSpectator) {
                this.isSpectatorMode = true;
                this.isP1 = spectatorViewP1; 
                primaryStage.setTitle("Spectating Room #" + data.roomId);
            } else {
                this.isSpectatorMode = false;
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
            } 
            else {
                new Alert(AlertType.WARNING, data.content).show();
            }
            
            // if match ended or room not found, return to home screen
            if (data.content.contains("Match ended") || data.content.contains("Room not found")) {
                primaryStage.setScene(homeScene);
            }
        }
    }

    // re-renders game view
    public void refreshView() {
        if (lastMessage != null) {
            this.isP1 = isSpectatorMode ? spectatorViewP1 : isP1;
            gameView.updateState(lastMessage);
        }
    }
    
   
    // handles tile click, selects piece, or sends move request
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
    
    // called when play again button is clicked
    public void returnToHome() {
        clientConnection.send(new Message(Message.Action.PLAY_AGAIN));
        primaryStage.setScene(homeScene);
    }

    // waiting screen
    private Scene createWaitingGui() {
        VBox root = new VBox(30);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        try { root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); } catch (Exception e) {}
        waitingTitleLabel = new Label("Searching for opponent...");
        waitingTitleLabel.getStyleClass().add("searching-label");
        waitingTitleLabel.setStyle("-fx-text-fill: white;"); 
        
        // pulsing animation
        Circle pulse = new Circle(30, Color.web("#DC2626"));
        FadeTransition ft = new FadeTransition(Duration.seconds(1), pulse);
        ft.setFromValue(1.0);
        ft.setToValue(0.3);
        ft.setCycleCount(Transition.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();
        
        // subtitle text
        waitingSubLabel = new Label("Please wait while we connect you to a game...");
        waitingSubLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px;");
        
        // back home button
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
