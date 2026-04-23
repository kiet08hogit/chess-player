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
import java.util.ArrayList;
import java.util.List;

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
    private HBox chatLine;
    private HBox endMatchControls;
    private HBox spectatorControls;
    private VBox notificationOverlay;
    private VBox scoreCol;
    private Label notificationTitle;
    private Label notificationContent;
    private HBox notificationButtons;
    private Button surrenderBtn;
    private Button spectatorHomeBtn;
    private Button viewP1Btn;
    private Button viewP2Btn;
    private Button notifyPositiveBtn;
    private Button notifyNegativeBtn;

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
        } catch (Exception e) {
        }
        root.getStyleClass().add("game-root");

        HBox mainContent = new HBox(20);
        mainContent.setAlignment(Pos.CENTER);
        mainContent.setPadding(new Insets(10));

        StackPane boardStack = new StackPane();
        boardPane = new GridPane();
        boardPane.setHgap(0);
        boardPane.setVgap(0);
        boardPane.setAlignment(Pos.CENTER);
        boardPane.setStyle(
                "-fx-background-color: #5C3A21; -fx-background-radius: 8; -fx-border-color: #3B2313; -fx-border-width: 5; -fx-border-radius: 8; -fx-padding: 5;");

        // Ensure boardPane stays square and fits in boardStack
        boardPane.maxWidthProperty()
                .bind(javafx.beans.binding.Bindings.min(boardStack.widthProperty(), boardStack.heightProperty()));
        boardPane.maxHeightProperty().bind(boardPane.maxWidthProperty());
        boardPane.minWidthProperty().bind(boardPane.maxWidthProperty());
        boardPane.minHeightProperty().bind(boardPane.maxWidthProperty());

        // Setup 8x8 equal constraints
        for (int i = 0; i < 8; i++) {
            javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
            cc.setPercentWidth(12.5);
            cc.setHgrow(javafx.scene.layout.Priority.ALWAYS);
            boardPane.getColumnConstraints().add(cc);

            javafx.scene.layout.RowConstraints rc = new javafx.scene.layout.RowConstraints();
            rc.setPercentHeight(12.5);
            rc.setVgrow(javafx.scene.layout.Priority.ALWAYS);
            boardPane.getRowConstraints().add(rc);
        }

        gameOverOverlay = new Label("YOU LOSE");
        gameOverOverlay.setStyle(
                "-fx-font-size: 60px; -fx-font-weight: bold; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 20;");
        gameOverOverlay.setVisible(false);

        notificationOverlay = new VBox(20);
        notificationOverlay.setAlignment(Pos.CENTER);
        notificationOverlay.setStyle(
                "-fx-background-color: rgba(30, 41, 59, 0.95); -fx-padding: 40; -fx-background-radius: 20; -fx-border-color: #4ADE80; -fx-border-width: 3; -fx-border-radius: 20;");
        notificationOverlay.setMaxSize(450, 250);

        notificationTitle = new Label("TITLE");
        notificationTitle.setStyle("-fx-text-fill: white; -fx-font-size: 24px; -fx-font-weight: bold;");

        notificationContent = new Label("Content goes here...");
        notificationContent.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 16px; -fx-alignment: center;");
        notificationContent.setWrapText(true);
        notificationContent.setAlignment(Pos.CENTER);

        notificationButtons = new HBox(20);
        notificationButtons.setAlignment(Pos.CENTER);
        notifyPositiveBtn = new Button("YES");
        notifyNegativeBtn = new Button("NO");
        notifyPositiveBtn.getStyleClass().add("button");
        notifyNegativeBtn.getStyleClass().add("button");

        notificationButtons.getChildren().addAll(notifyPositiveBtn, notifyNegativeBtn);
        notificationOverlay.getChildren().addAll(notificationTitle, notificationContent, notificationButtons);
        notificationOverlay.setVisible(false);
        notificationOverlay.setManaged(false);

        boardStack.getChildren().addAll(boardPane, gameOverOverlay, notificationOverlay);

        VBox rulesSidebar = new VBox(20);
        rulesSidebar.setPadding(new Insets(20));
        rulesSidebar.setPrefWidth(280);
        rulesSidebar.getStyleClass().add("sidebar");

        Label rulesTitle = new Label("How to Play");
        rulesTitle.getStyleClass().add("title-label");
        rulesTitle.setStyle("-fx-font-size: 22px;");

        Label rulesText = new Label(
                "1. Red always moves first.\n\n" +
                        "2. Pieces move diagonally forward to an empty square.\n\n" +
                        "3. Jump over an opponent's piece to capture it.\n\n" +
                        "4. Reach the opposite end of the board to become a King.\n\n" +
                        "5. Kings can move and jump both forward and backward.\n\n" +
                        "6. Win by capturing all of your opponent's pieces.");
        rulesText.setWrapText(true);
        rulesText.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 14px; -fx-line-spacing: 4px;");
        rulesSidebar.getChildren().addAll(rulesTitle, rulesText);

        scoreCol = new VBox(150);
        scoreCol.setAlignment(Pos.CENTER);
        opponentScoreLabel = new Label("0");
        opponentScoreLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        myScoreLabel = new Label("0");
        myScoreLabel.setStyle("-fx-font-size: 30px; -fx-text-fill: #2ecc71; -fx-font-weight: bold;");
        opponentNameLabel = new Label("OPPONENT");
        opponentNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        myNameLabel = new Label("YOU");
        myNameLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        scoreCol.getChildren().addAll(new VBox(5, opponentNameLabel, opponentScoreLabel),
                new VBox(5, myNameLabel, myScoreLabel));
        ((VBox) scoreCol.getChildren().get(0)).setAlignment(Pos.CENTER);
        ((VBox) scoreCol.getChildren().get(1)).setAlignment(Pos.CENTER);

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
        chatLine = new HBox(5);
        chatField = new TextField();
        chatField.setPromptText("Type a message...");
        chatField.setPrefWidth(180);
        Button sendBtn = new Button("SEND");
        sendBtn.setMinWidth(80);
        chatLine.getChildren().addAll(chatField, sendBtn);

        surrenderBtn = new Button("SURRENDER");
        surrenderBtn.getStyleClass().add("surrender-button");
        surrenderBtn.setMinWidth(265);

        Button rulesBtn = new Button("RULES");
        rulesBtn.getStyleClass().add("menu-button");
        rulesBtn.setMinWidth(265);
        rulesBtn.setOnAction(e -> {
            if (root.getLeft() == null) {
                root.setLeft(rulesSidebar);
            } else {
                root.setLeft(null);
            }
        });

        spectatorHomeBtn = new Button("HOME");
        spectatorHomeBtn.getStyleClass().add("menu-button");
        spectatorHomeBtn.setMinWidth(265);
        spectatorHomeBtn.setVisible(false);

        spectatorControls = new HBox(5);
        spectatorControls.setAlignment(Pos.CENTER);
        viewP1Btn = new Button("VIEW RED");
        viewP2Btn = new Button("VIEW BLACK");
        viewP1Btn.getStyleClass().add("menu-button");
        viewP2Btn.getStyleClass().add("menu-button");
        viewP1Btn.setPrefWidth(130);
        viewP2Btn.setPrefWidth(130);
        spectatorControls.getChildren().addAll(viewP1Btn, viewP2Btn);
        spectatorControls.setVisible(false);
        spectatorControls.setManaged(false);

        chatInputBox.getChildren().addAll(chatLine, surrenderBtn, spectatorHomeBtn, spectatorControls, rulesBtn);

        viewP1Btn.setOnAction(e -> {
            mainApp.spectatorViewP1 = true;
            mainApp.refreshView();
        });
        viewP2Btn.setOnAction(e -> {
            mainApp.spectatorViewP1 = false;
            mainApp.refreshView();
        });

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

        rightBox.getChildren().addAll(roomIdLabel, playingWithLabel, statusLabel, chatList, chatInputBox,
                endMatchControls);
        root.setRight(rightBox);
    }

    public void updateState(Message data) {
        gameOverOverlay.setVisible(false);
        chatInputBox.setVisible(true);
        endMatchControls.setVisible(false);
        if (data.isSpectator) {
            surrenderBtn.setVisible(false);
            spectatorHomeBtn.setVisible(true);
            spectatorControls.setVisible(true);
            spectatorControls.setManaged(true);

            String p1 = data.player1Name != null ? data.player1Name : "P1";
            String p2 = data.player2Name != null ? data.player2Name : "P2";
            playingWithLabel.setText("Watching: " + p1 + " vs " + p2);

            if (mainApp.isP1) {
                opponentNameLabel.setText(p2);
                myNameLabel.setText(p1);
                opponentScoreLabel.setText(String.valueOf(data.p2Score));
                myScoreLabel.setText(String.valueOf(data.p1Score));
                viewP1Btn.setStyle("-fx-background-color: #4ADE80; -fx-text-fill: white;"); // Highlight selected
                viewP2Btn.setStyle("");
            } else {
                opponentNameLabel.setText(p1);
                myNameLabel.setText(p2);
                opponentScoreLabel.setText(String.valueOf(data.p1Score));
                myScoreLabel.setText(String.valueOf(data.p2Score));
                viewP2Btn.setStyle("-fx-background-color: #4ADE80; -fx-text-fill: white;");
                viewP1Btn.setStyle("");
            }
        } else {
            surrenderBtn.setVisible(true);
            spectatorHomeBtn.setVisible(false);
            spectatorControls.setVisible(false);
            spectatorControls.setManaged(false);

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
        if (data.roomId != null)
            roomIdLabel.setText("Room ID: #" + data.roomId);
        if (data.board != null) {
            lastBoard = data.board;
            drawBoard();
        }
    }

    private List<int[]> getValidMoves(int x, int y, int piece) {
        List<int[]> moves = new ArrayList<>();
        boolean isKing = (piece == 3 || piece == 4);
        boolean isP1Piece = (piece == 1 || piece == 3);
        int dyForward = isP1Piece ? -1 : 1;

        int[] dirs = isKing ? new int[] { -1, 1 } : new int[] { dyForward };

        for (int dy : dirs) {
            for (int dx : new int[] { -1, 1 }) {
                int nx = x + dx;
                int ny = y + dy;
                // Regular move
                if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8 && lastBoard[nx][ny] == 0) {
                    moves.add(new int[] { nx, ny });
                }

                // Jump move
                int jx = x + 2 * dx;
                int jy = y + 2 * dy;
                if (jx >= 0 && jx < 8 && jy >= 0 && jy < 8 && lastBoard[jx][jy] == 0) {
                    if (nx >= 0 && nx < 8 && ny >= 0 && ny < 8) {
                        int midPiece = lastBoard[nx][ny];
                        if (midPiece != 0) {
                            boolean midIsP1 = (midPiece == 1 || midPiece == 3);
                            if (isP1Piece != midIsP1) {
                                moves.add(new int[] { jx, jy });
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

    public void drawBoard() {
        boardPane.getChildren().clear();

        List<int[]> validMoves = new ArrayList<>();
        if (mainApp.selectedX != -1 && mainApp.selectedY != -1) {
            int selectedPiece = lastBoard[mainApp.selectedX][mainApp.selectedY];
            validMoves = getValidMoves(mainApp.selectedX, mainApp.selectedY, selectedPiece);
        }

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int piece = lastBoard[x][y];
                boolean isPath = false;
                for (int[] v : validMoves) {
                    if (v[0] == x && v[1] == y) {
                        isPath = true;
                        break;
                    }
                }
                VBox tile = createTile(x, y, piece, isPath);
                if (mainApp.isP1)
                    boardPane.add(tile, x, y);
                else
                    boardPane.add(tile, 7 - x, 7 - y);
            }
        }
    }

    private VBox createTile(int x, int y, int piece, boolean isPath) {
        VBox cell = new VBox();
        cell.setAlignment(Pos.CENTER);
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        if ((x + y) % 2 == 0) {
            cell.setStyle("-fx-background-color: #F8CB9C;"); // Light wood
        } else {
            cell.setStyle("-fx-background-color: #D28C45;"); // Dark wood
        }

        if (x == mainApp.selectedX && y == mainApp.selectedY) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #4ADE80; -fx-opacity: 0.8;"); // Green highlight
        } else if (isPath) {
            cell.setStyle(cell.getStyle() + "-fx-background-color: #BBF7D0; -fx-opacity: 0.6;"); // Light Green
                                                                                                 // highlight
        }

        if (piece != 0) {
            Circle c = new Circle();
            c.radiusProperty().bind(cell.widthProperty().divide(2.5)); // Dynamic radius
            c.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
            c.setStrokeWidth(3);

            if (piece == 1 || piece == 3) {
                c.setFill(Color.web("#EF4444")); // Red piece
                c.setStroke(Color.web("#B91C1C")); // Darker red border
            }
            if (piece == 2 || piece == 4) {
                c.setFill(Color.web("#1E293B")); // Dark piece
                c.setStroke(Color.web("#0F172A")); // Darker border
            }

            // Inner circle for sleek look
            Circle innerObj = new Circle();
            innerObj.radiusProperty().bind(c.radiusProperty().divide(1.5));
            innerObj.setFill(Color.TRANSPARENT);
            innerObj.setStrokeWidth(2);

            if (piece == 1 || piece == 3)
                innerObj.setStroke(Color.web("#DC2626"));
            if (piece == 2 || piece == 4)
                innerObj.setStroke(Color.web("#334155"));

            if (piece == 3 || piece == 4) { // Kings
                Label kingIcon = new Label("♕");
                kingIcon.styleProperty()
                        .bind(javafx.beans.binding.Bindings.concat(
                                "-fx-text-fill: #F59E0B; -fx-font-weight: bold; -fx-font-size: ",
                                cell.widthProperty().divide(2.5).asString()));
                VBox stack = new VBox(kingIcon);
                stack.setAlignment(Pos.CENTER);
                cell.getChildren().addAll(new javafx.scene.layout.StackPane(c, innerObj, stack));
            } else {
                cell.getChildren().add(new javafx.scene.layout.StackPane(c, innerObj));
            }
        }

        cell.setOnMouseClicked(e -> {
            if (mainApp.isSpectatorMode)
                return;
            if ((x + y) % 2 == 0)
                return;
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
        spectatorControls.setVisible(false);
        spectatorControls.setManaged(false);
        notificationOverlay.setVisible(false);
        notificationOverlay.setManaged(false);
        currentOpponent = "----";
    }

    public void setBotMatchMode(boolean isBot, int level) {
        playingWithLabel.setVisible(!isBot);
        playingWithLabel.setManaged(!isBot);
        chatList.setVisible(!isBot);
        chatList.setManaged(!isBot);
        chatLine.setVisible(!isBot);
        chatLine.setManaged(!isBot);
        scoreCol.setVisible(!isBot);
        scoreCol.setManaged(!isBot);

        if (isBot) {
            statusLabel.setText("TRAINING MODE (Level " + level + ")");
            roomIdLabel.setVisible(false);
            roomIdLabel.setManaged(false);
            surrenderBtn.setText("HOME");
            surrenderBtn.getStyleClass().remove("surrender-button");
            surrenderBtn.getStyleClass().add("menu-button");
            surrenderBtn.setOnAction(e -> mainApp.returnToHome());
        } else {
            roomIdLabel.setVisible(true);
            roomIdLabel.setManaged(true);
            surrenderBtn.setText("SURRENDER");
            surrenderBtn.getStyleClass().remove("menu-button");
            surrenderBtn.getStyleClass().add("surrender-button");
            surrenderBtn.setOnAction(e -> mainApp.clientConnection.send(new Message(Message.Action.SURRENDER)));
        }
    }

    public void showBotLevelSelection() {
        notificationTitle.setText("SELECT DIFFICULTY");
        notificationContent.setText("Choose your opponent's level");
        notifyPositiveBtn.setText("LEVEL 1");
        notifyPositiveBtn.setStyle("-fx-background-color: #10B981; -fx-pref-width: 150;");
        notifyNegativeBtn.setText("LEVEL 2");
        notifyNegativeBtn.setStyle("-fx-background-color: #EF4444; -fx-pref-width: 150;");
        notifyNegativeBtn.setVisible(true);
        notifyNegativeBtn.setManaged(true);

        notifyPositiveBtn.setOnAction(e -> startBotMatch(1));
        notifyNegativeBtn.setOnAction(e -> startBotMatch(2));

        notificationOverlay.setVisible(true);
        notificationOverlay.setManaged(true);
    }

    private void startBotMatch(int level) {
        notificationOverlay.setVisible(false);
        notificationOverlay.setManaged(false);
        Message m = new Message(Message.Action.PLAY_BOT);
        m.botLevel = level;
        mainApp.clientConnection.send(m);
    }

    public void showRematchRequest() {
        notificationTitle.setText("REMATCH REQUESTED!");
        notificationContent.setText("Your opponent wants a rematch.\nDo you accept?");
        notifyPositiveBtn.setText("YES");
        notifyPositiveBtn.setStyle("-fx-background-color: #10B981; -fx-pref-width: 100;");
        notifyNegativeBtn.setText("NO");
        notifyNegativeBtn.setStyle("-fx-background-color: #EF4444; -fx-pref-width: 100;");
        notifyNegativeBtn.setVisible(true);
        notifyNegativeBtn.setManaged(true);
        notifyPositiveBtn.setOnAction(e -> {
            mainApp.clientConnection.send(new Message(Message.Action.REMATCH_ACCEPT));
            notificationOverlay.setVisible(false);
            notificationOverlay.setManaged(false);
        });

        notifyNegativeBtn.setOnAction(e -> {
            mainApp.clientConnection.send(new Message(Message.Action.REMATCH_DECLINE));
            mainApp.returnToHome();
        });

        notificationOverlay.setVisible(true);
        notificationOverlay.setManaged(true);
    }

    public void showOpponentLeft(String leaver, String reason) {
        notificationTitle.setText("MATCH ENDED");
        if ("SERVER_DISCONNECT".equals(reason)) {
            notificationContent.setText(leaver + " has left the server.\nYou win by forfeit!");
        } else {
            notificationContent.setText(leaver + " has left the match.");
        }
        notifyPositiveBtn.setText("HOME");
        notifyPositiveBtn.setStyle("-fx-background-color: #3B82F6; -fx-pref-width: 150;");
        notifyNegativeBtn.setVisible(false);
        notifyNegativeBtn.setManaged(false);

        notifyPositiveBtn.setOnAction(e -> mainApp.returnToHome());

        notificationOverlay.setVisible(true);
        notificationOverlay.setManaged(true);
    }

    public void showGenericNotification(String title, String content) {
        notificationTitle.setText(title);
        notificationContent.setText(content);
        notifyPositiveBtn.setText("OK");
        notifyPositiveBtn.setStyle("-fx-background-color: #3B82F6; -fx-pref-width: 120;");
        notifyNegativeBtn.setVisible(false);
        notifyNegativeBtn.setManaged(false);
        notifyPositiveBtn.setOnAction(e -> {
            notificationOverlay.setVisible(false);
            notificationOverlay.setManaged(false);
        });

        notificationOverlay.setVisible(true);
        notificationOverlay.setManaged(true);
    }

    public void addChatMessage(String msg) {
        chatList.getItems().add(msg);
    }

    public void clearChat() {
        chatList.getItems().clear();
    }

    public Parent getRoot() {
        return root;
    }
}
