import java.util.HashMap;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class GuiClient extends Application {

    Client clientConnection;
    HashMap<String, Scene> sceneMap;
    Stage primaryStage;
    
    // Login UI
    TextField userField;
    
    // Game UI
    GridPane boardPane;
    ListView<String> chatList;
    TextField chatField;
    Label statusLabel;
    
    // Game State
    int selectedX = -1;
    int selectedY = -1;
    boolean isP1;
    
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
        clientConnection = new Client(data -> {
            Platform.runLater(() -> {
                handleServerMessage((Message) data);
            });
        });
        clientConnection.start();

        sceneMap = new HashMap<>();
        sceneMap.put("login", createLoginGui());
        sceneMap.put("waiting", createWaitingGui());
        sceneMap.put("game", createGameGui());

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });

        primaryStage.setScene(sceneMap.get("login"));
        primaryStage.setTitle("Checkers Login");
        primaryStage.show();
    }
    
    private void handleServerMessage(Message data) {
        if (data.action == Message.Action.LOGIN_SUCCESS) {
            primaryStage.setScene(sceneMap.get("waiting"));
            primaryStage.setTitle("Waiting for opponent...");
        } 
        else if (data.action == Message.Action.LOGIN_FAIL) {
            Alert a = new Alert(AlertType.ERROR, "Login Failed: " + data.content);
            a.show();
        } 
        else if (data.action == Message.Action.MATCH_FOUND) {
            this.isP1 = data.isPlayer1;
            primaryStage.setScene(sceneMap.get("game"));
            primaryStage.setTitle("Checkers - Playing against " + data.opponentName);
            chatList.getItems().clear();
            chatList.getItems().add("System: Matched with " + data.opponentName);
            statusLabel.setText("Match started!");
            selectedX = -1;
            selectedY = -1;
        } 
        else if (data.action == Message.Action.GAME_STATE_UPDATE) {
            statusLabel.setText(data.gameStatus);
            lastBoard = data.board;
            drawBoard(lastBoard);
        } 
        else if (data.action == Message.Action.CHAT_MESSAGE) {
            chatList.getItems().add(data.username + ": " + data.content);
        }
        else if (data.action == Message.Action.GAME_OVER) {
            statusLabel.setText(data.gameStatus);
            lastBoard = data.board;
            drawBoard(lastBoard);
            Alert a = new Alert(AlertType.INFORMATION, data.gameStatus);
            a.show();
        }
        else if (data.action == Message.Action.OPPONENT_LEFT) {
            Alert a = new Alert(AlertType.WARNING, "Opponent disconnected. You win!");
            a.show();
            statusLabel.setText("Opponent left.");
        }
    }
    
    public Scene createLoginGui() {
        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        
        Label title = new Label("Networked Checkers");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        
        userField = new TextField();
        userField.setPromptText("Enter Username");
        userField.setMaxWidth(200);
        
        Button loginBtn = new Button("Login");
        loginBtn.setOnAction(e -> {
            Message m = new Message(Message.Action.LOGIN);
            m.username = userField.getText().trim();
            if (!m.username.isEmpty()) {
                 clientConnection.send(m);
            }
        });
        
        box.getChildren().addAll(title, userField, loginBtn);
        return new Scene(box, 400, 300);
    }
    
    public Scene createWaitingGui() {
        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        Label label = new Label("Waiting for another player to join...");
        label.setStyle("-fx-font-size: 18px;");
        box.getChildren().add(label);
        return new Scene(box, 400, 300);
    }

    public Scene createGameGui() {
        BorderPane root = new BorderPane();
        
        // --- Left: Board ---
        boardPane = new GridPane();
        boardPane.setPadding(new Insets(10));
        boardPane.setHgap(2);
        boardPane.setVgap(2);
        drawBoard(new int[8][8]); // empty initial board
        root.setCenter(boardPane);
        
        // --- Right: Chat & Status ---
        VBox rightBox = new VBox(10);
        rightBox.setPadding(new Insets(10));
        rightBox.setPrefWidth(250);
        
        statusLabel = new Label("Status");
        statusLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        chatList = new ListView<>();
        chatList.setPrefHeight(300);
        
        HBox chatInputBox = new HBox(5);
        chatField = new TextField();
        chatField.setPromptText("Message...");
        chatField.setPrefWidth(180);
        Button sendBtn = new Button("Send");
        chatInputBox.getChildren().addAll(chatField, sendBtn);
        
        sendBtn.setOnAction(e -> {
            if (!chatField.getText().isEmpty()) {
                Message m = new Message(Message.Action.CHAT_MESSAGE);
                m.content = chatField.getText();
                m.username = "Me";
                chatList.getItems().add("Me: " + m.content);
                clientConnection.send(m);
                chatField.clear();
            }
        });
        
        Button playAgain = new Button("Play Again");
        playAgain.setOnAction(e -> {
            clientConnection.send(new Message(Message.Action.PLAY_AGAIN));
            primaryStage.setScene(sceneMap.get("waiting"));
        });
        
        Button quit = new Button("Quit");
        quit.setOnAction(e -> {
            Platform.exit();
            System.exit(0);
        });
        HBox buttons = new HBox(10, playAgain, quit);
        
        rightBox.getChildren().addAll(statusLabel, chatList, chatInputBox, buttons);
        root.setRight(rightBox);
        
        return new Scene(root, 700, 500);
    }
    
    private void drawBoard(int[][] boardData) {
        boardPane.getChildren().clear();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = boardData[x][y];
                VBox tile = createTile(x, y, piece);
                boardPane.add(tile, x, y);
            }
        }
    }
    
    private VBox createTile(int x, int y, int piece) {
        VBox cell = new VBox();
        cell.setPrefSize(50, 50);
        cell.setAlignment(Pos.CENTER);
        
        // Tile colors
        if ((x + y) % 2 == 0) {
            cell.setStyle("-fx-background-color: #EEEED2;");
        } else {
            cell.setStyle("-fx-background-color: #769656;");
        }
        
        // Highlight selection
        if (x == selectedX && y == selectedY) {
            cell.setStyle(cell.getStyle() + "-fx-border-color: yellow; -fx-border-width: 3;");
        }
        
        // Draw Piece
        if (piece != 0) {
            Circle c = new Circle(20);
            if (piece == 1 || piece == 3) c.setFill(Color.RED);
            if (piece == 2 || piece == 4) c.setFill(Color.BLACK);
            
            if (piece == 3 || piece == 4) { // Kings
                c.setStroke(Color.GOLD);
                c.setStrokeWidth(3);
            }
            cell.getChildren().add(c);
        }
        
        // Click handler for movement
        cell.setOnMouseClicked(e -> {
            if ((x + y) % 2 == 0) return; // Ignore light squares
            
            if (selectedX == -1) {
                // Determine if they clicked their own piece
                boolean ownPiece = false;
                if (isP1 && (piece == 1 || piece == 3)) ownPiece = true;
                if (!isP1 && (piece == 2 || piece == 4)) ownPiece = true;
                
                if (ownPiece) {
                    selectedX = x;
                    selectedY = y;
                    drawBoard(lastBoard);
                }
            } else {
                // Execute move
                if (x == selectedX && y == selectedY) {
                    // Deselect
                    selectedX = -1;
                    selectedY = -1;
                    drawBoard(lastBoard);
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
            // re-render selection highlight (requires keeping track of last board state)
            // For now, next server update clears it, or we maintain a local copy.
        });
        
        return cell;
    }
    
    
    int[][] lastBoard = new int[8][8];
}

