import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class GameView {
    private BorderPane root;
    private GridPane boardPane;
    private ListView<String> chatList;
    private TextField chatField;
    private Label statusLabel;
    private Label roomIdLabel;
    private Label playingWithLabel;
    private Label myScoreLabel;
    private Label opponentScoreLabel;
    private Label myNameLabel;
    private Label opponentNameLabel;
    private Label gameOverOverlay;
    
    private VBox chatInputBox;
    private HBox endMatchControls;
    private Button surrenderBtn;
    private Button spectatorHomeBtn;
    
    private GuiClient mainApp;
    private int[][] lastBoard = new int[8][8];
    private String currentOpponent = "----";

    public GameView(GuiClient mainApp) {
        this.mainApp = mainApp;
        createGui();
    }

    private void createGui() {
        root = new BorderPane();
        try {
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {}
        root.getStyleClass().add("game-root");
        
        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(10));

        StackPane boardStack = new StackPane();
        boardPane = new GridPane();
        boardPane.setHgap(5); boardPane.setVgap(5);
        boardPane.setAlignment(Pos.CENTER);
        
        gameOverOverlay = new Label("YOU LOSE");
        gameOverOverlay.setStyle("-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 20;");
        gameOverOverlay.setVisible(false);
        boardStack.getChildren().addAll(boardPane, gameOverOverlay);

        VBox scoreCol = new VBox(150);
        scoreCol.setAlignment(Pos.CENTER);
        opponentScoreLabel = new Label("0");
        opponentScoreLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        myScoreLabel = new Label("0");
        myScoreLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        opponentNameLabel = new Label("OPPONENT"); opponentNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        myNameLabel = new Label("YOU"); myNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        scoreCol.getChildren().addAll(new VBox(5, opponentNameLabel, opponentScoreLabel), new VBox(5, myNameLabel, myScoreLabel));
        ((VBox)scoreCol.getChildren().get(0)).setAlignment(Pos.CENTER);
        ((VBox)scoreCol.getChildren().get(1)).setAlignment(Pos.CENTER);

        mainContent.getChildren().addAll(boardStack, scoreCol);
        root.setCenter(mainContent);
        
        VBox rightBox = new VBox(15);
        rightBox.setPadding(new Insets(10));
        rightBox.setPrefWidth(320); 
        rightBox.setAlignment(Pos.TOP_CENTER);
        
        roomIdLabel = new Label("Room ID: #----");
        roomIdLabel.setStyle("-fx-text-fill: #999; -fx-font-size: 14px; -fx-font-weight: bold;");
        playingWithLabel = new Label("Playing with: ----");
        playingWithLabel.setStyle("-fx-text-fill: #007bff; -fx-font-size: 16px; -fx-font-weight: bold;");
        statusLabel = new Label("Ready to Play");
        statusLabel.getStyleClass().add("status-label");
        
        chatList = new ListView<>();
        chatList.getStyleClass().add("chat-list");
        chatList.setPrefHeight(350);
        
        chatInputBox = new VBox(10);
        chatInputBox.setAlignment(Pos.CENTER);
        HBox chatLine = new HBox(5);
        chatField = new TextField();
        chatField.setPromptText("Type a message...");
        chatField.setPrefWidth(180);
        Button sendBtn = new Button("SEND");
        sendBtn.setMinWidth(80);
        chatLine.getChildren().addAll(chatField, sendBtn);
        
        surrenderBtn = new Button("SURRENDER");
        surrenderBtn.getStyleClass().add("surrender-button");
        surrenderBtn.setMinWidth(265); 
        spectatorHomeBtn = new Button("HOME");
        spectatorHomeBtn.getStyleClass().add("menu-button");
        spectatorHomeBtn.setMinWidth(265);
        spectatorHomeBtn.setVisible(false);

        chatInputBox.getChildren().addAll(chatLine, surrenderBtn, spectatorHomeBtn);
        
        sendBtn.setOnAction(e -> {
            if (!chatField.getText().isEmpty()) {
                Message m = new Message(Message.Action.CHAT_MESSAGE);
                m.content = chatField.getText();
                m.username = mainApp.loggedInUser;
                mainApp.clientConnection.send(m);
                chatField.clear();
            }
        });
        
        surrenderBtn.setOnAction(e -> mainApp.clientConnection.send(new Message(Message.Action.SURRENDER)));
        spectatorHomeBtn.setOnAction(e -> mainApp.returnToHome());

        endMatchControls = new HBox(10);
        endMatchControls.setAlignment(Pos.CENTER);
        Button rematchBtn = new Button("REMATCH");
        Button homeBtn = new Button("HOME");
        rematchBtn.getStyleClass().add("menu-button");
        homeBtn.getStyleClass().add("menu-button");
        endMatchControls.getChildren().addAll(rematchBtn, homeBtn);
        endMatchControls.setVisible(false);
        
        rematchBtn.setOnAction(e -> {
            mainApp.clientConnection.send(new Message(Message.Action.REMATCH));
            resetUI();
        });
        homeBtn.setOnAction(e -> mainApp.returnToHome());
        
        rightBox.getChildren().addAll(roomIdLabel, playingWithLabel, statusLabel, chatList, chatInputBox, endMatchControls);
        root.setRight(rightBox);
    }

    public void updateState(Message data) {
        if (data.isSpectator) {
            surrenderBtn.setVisible(false);
            spectatorHomeBtn.setVisible(true);
            String p1 = data.player1Name != null ? data.player1Name : "P1";
            String p2 = data.player2Name != null ? data.player2Name : "P2";
            playingWithLabel.setText("Watching: " + p1 + " vs " + p2);
            opponentNameLabel.setText(p1);
            myNameLabel.setText(p2);
            opponentScoreLabel.setText(String.valueOf(data.p1Score));
            myScoreLabel.setText(String.valueOf(data.p2Score));
        } else {
            surrenderBtn.setVisible(true);
            spectatorHomeBtn.setVisible(false);
            
            if (data.opponentName != null) {
                currentOpponent = data.opponentName;
            }
            playingWithLabel.setText("Playing with: " + currentOpponent);
            
            myNameLabel.setText("YOU (" + mainApp.loggedInUser + ")");
            opponentNameLabel.setText(currentOpponent);
            if (mainApp.isP1) {
                myScoreLabel.setText(String.valueOf(data.p1Score));
                opponentScoreLabel.setText(String.valueOf(data.p2Score));
            } else {
                myScoreLabel.setText(String.valueOf(data.p2Score));
                opponentScoreLabel.setText(String.valueOf(data.p1Score));
            }
        }
        statusLabel.setText(data.gameStatus);
        if (data.roomId != null) roomIdLabel.setText("Room ID: #" + data.roomId);
        if (data.board != null) {
            lastBoard = data.board;
            drawBoard();
        }
    }

    public void drawBoard() {
        boardPane.getChildren().clear();
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = lastBoard[x][y];
                VBox tile = createTile(x, y, piece);
                if (mainApp.isP1) boardPane.add(tile, x, y);
                else boardPane.add(tile, 7 - x, 7 - y);
            }
        }
    }

    private VBox createTile(int x, int y, int piece) {
        VBox cell = new VBox();
        cell.setPrefSize(60, 60);
        cell.setAlignment(Pos.CENTER);
        cell.setStyle("-fx-background-color: " + ((x+y)%2 == 0 ? "#3d3d3d" : "#2d2d2d") + ";");
        if (x == mainApp.selectedX && y == mainApp.selectedY) {
            cell.setStyle(cell.getStyle() + "-fx-border-color: #007bff; -fx-border-width: 3;");
        }
        if (piece != 0) {
            Circle c = new Circle(25);
            c.setFill((piece == 1 || piece == 3) ? Color.web("#e74c3c") : Color.web("#2c3e50"));
            if (piece == 3 || piece == 4) { c.setStroke(Color.GOLD); c.setStrokeWidth(3); }
            cell.getChildren().add(c);
        }
        cell.setOnMouseClicked(e -> {
            if (mainApp.isSpectatorMode) return;
            if ((x + y) % 2 == 0) return;
            mainApp.handleTileClick(x, y, piece);
        });
        return cell;
    }

    public void showGameOver(String winner) {
        gameOverOverlay.setVisible(true);
        if (mainApp.isSpectatorMode) {
            gameOverOverlay.setText(winner + " WINS!");
            gameOverOverlay.setTextFill(Color.GOLD);
        } else {
            if (winner.equals(mainApp.loggedInUser)) {
                gameOverOverlay.setText("YOU WIN!");
                gameOverOverlay.setTextFill(Color.GOLD);
            } else {
                gameOverOverlay.setText("YOU LOSE");
                gameOverOverlay.setTextFill(Color.GRAY);
            }
        }
        chatInputBox.setVisible(false);
        endMatchControls.setVisible(true);
    }

    public void resetUI() {
        gameOverOverlay.setVisible(false);
        chatInputBox.setVisible(true);
        endMatchControls.setVisible(false);
        currentOpponent = "----";
    }

    public void addChatMessage(String msg) { chatList.getItems().add(msg); }
    public void clearChat() { chatList.getItems().clear(); }
    public Parent getRoot() { return root; }
}
