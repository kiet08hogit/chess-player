import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class HomeView {
    private BorderPane root;
    private ListView<String> onlinePlayersList;
    private ListView<String> globalChatList;
    private TextField globalChatField;
    private Label userLabel;
    private GuiClient mainApp;

    public HomeView(GuiClient mainApp) {
        this.mainApp = mainApp;
        createGui();
    }

    private void createGui() {
        root = new BorderPane();
        try {
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {
        }
        root.getStyleClass().add("home-root");

        // Left side bar
        VBox leftSidebar = new VBox();
        leftSidebar.getStyleClass().add("sidebar");
        leftSidebar.setMinWidth(220);

        Button playRandom = new Button("RANDOM MATCH");
        Button createRoom = new Button("CREATE ROOM");
        Button joinRoom = new Button("JOIN ROOM");
        Button watchMatch = new Button("WATCH MATCH");
        Button playBot = new Button("PLAY WITH BOT");
        Button communityBtn = new Button("COMMUNITY");
        Button readRules = new Button("READ RULE");

        playRandom.getStyleClass().add("menu-button");
        createRoom.getStyleClass().add("menu-button");
        joinRoom.getStyleClass().add("menu-button");
        watchMatch.getStyleClass().add("menu-button");
        playBot.getStyleClass().add("menu-button");
        communityBtn.getStyleClass().add("menu-button");
        readRules.getStyleClass().add("menu-button");

        for (Button b : new Button[] { playRandom, createRoom, joinRoom, watchMatch, playBot, communityBtn,
                readRules }) {
            b.setMinWidth(200);
            b.setMaxWidth(Double.MAX_VALUE);
        }

        playRandom.setOnAction(e -> mainApp.onFindMatch());

        createRoom.setOnAction(e -> mainApp.onCreateRoom());

        joinRoom.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Join Room");
            dialog.setHeaderText("Enter Room ID to join your friend");
            dialog.setContentText("Room ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(roomId -> mainApp.onJoinFriend(roomId));
        });

        watchMatch.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Watch Match");
            dialog.setHeaderText("Enter Room ID to spectate");
            dialog.setContentText("Room ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(roomId -> mainApp.onWatchMatch(roomId));
        });

        playBot.setOnAction(e -> mainApp.onPlayBotClicked());

        // Center
        StackPane centerWrapper = new StackPane();
        centerWrapper.setPadding(new Insets(20));
        GridPane homeBoard = createStaticBoard();

        centerWrapper.layoutBoundsProperty().addListener((obs, oldV, newV) -> {
            double scale = Math.min(newV.getWidth() / 440.0, newV.getHeight() / 440.0);
            if (scale > 0 && homeBoard.isVisible()) {
                homeBoard.setScaleX(scale);
                homeBoard.setScaleY(scale);
            }
        });

        VBox rulesBox = new VBox(20);
        rulesBox.setAlignment(Pos.CENTER);
        rulesBox.setStyle(
                "-fx-background-color: #1E293B; -fx-padding: 30; -fx-background-radius: 10; -fx-border-color: #334155; -fx-border-width: 2;");
        rulesBox.setMaxSize(400, 400);

        Label rulesTitle = new Label("Game Rules");
        rulesTitle.getStyleClass().add("title-label");

        Label rulesText = new Label(
                "1. Red always moves first.\n\n" +
                        "2. Pieces move diagonally forward to an empty square.\n\n" +
                        "3. Jump over an opponent's piece to capture it.\n\n" +
                        "4. Reach the opposite end of the board to become a King.\n\n" +
                        "5. Kings can move and jump both forward and backward.\n\n" +
                        "6. Win by capturing all of your opponent's pieces.");
        rulesText.setWrapText(true);
        rulesText.setStyle("-fx-text-fill: #E2E8F0; -fx-font-size: 14px; -fx-line-spacing: 5px;");

        rulesBox.getChildren().addAll(rulesTitle, rulesText);
        rulesBox.setVisible(false);

        VBox globalChatBox = new VBox(15);
        globalChatBox.setAlignment(Pos.CENTER);
        globalChatBox.setStyle(
                "-fx-background-color: #1E293B; -fx-padding: 30; -fx-background-radius: 10; -fx-border-color: #334155; -fx-border-width: 2;");
        globalChatBox.setMaxSize(400, 400);

        Label chatTitle = new Label("Community Chat");
        chatTitle.getStyleClass().add("title-label");

        // server chat
        globalChatList = new ListView<>();
        globalChatList.getStyleClass().add("chat-list");
        globalChatList.setPrefHeight(250);

        HBox chatInputLine = new HBox(10);
        chatInputLine.setAlignment(Pos.CENTER);
        globalChatField = new TextField();
        globalChatField.setPromptText("Say hello to the community...");
        globalChatField.setPrefWidth(220);
        Button sendChatBtn = new Button("SEND");
        sendChatBtn.setMinWidth(80);

        sendChatBtn.setOnAction(e -> {
            if (!globalChatField.getText().isEmpty()) {
                Message mg = new Message(Message.Action.GLOBAL_CHAT);
                mg.content = globalChatField.getText();
                mainApp.clientConnection.send(mg);
                globalChatField.clear();
            }
        });

        chatInputLine.getChildren().addAll(globalChatField, sendChatBtn);
        globalChatBox.getChildren().addAll(chatTitle, globalChatList, chatInputLine);
        globalChatBox.setVisible(false);

        centerWrapper.getChildren().addAll(homeBoard, rulesBox, globalChatBox);
        root.setCenter(centerWrapper);

        communityBtn.setOnAction(e -> {
            boolean showChat = !globalChatBox.isVisible();
            globalChatBox.setVisible(showChat);
            rulesBox.setVisible(false);
            homeBoard.setVisible(!showChat);
        });

        readRules.setOnAction(e -> {
            boolean showRules = !rulesBox.isVisible();
            rulesBox.setVisible(showRules);
            globalChatBox.setVisible(false);
            homeBoard.setVisible(!showRules);
        });

        leftSidebar.getChildren().addAll(playRandom, createRoom, joinRoom, watchMatch, playBot, communityBtn,
                readRules);
        root.setLeft(leftSidebar);

        // Right side bar
        VBox rightSidebar = new VBox();
        rightSidebar.getStyleClass().add("player-list");
        rightSidebar.setMinWidth(220);
        Label listTitle = new Label("Online Players");
        listTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        onlinePlayersList = new ListView<>();
        onlinePlayersList.setMinWidth(200);
        rightSidebar.getChildren().addAll(listTitle, onlinePlayersList);
        root.setRight(rightSidebar);

        // Bottom Bar
        HBox bottomBar = new HBox();
        bottomBar.getStyleClass().add("user-info-bar");
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        userLabel = new Label("user: ");
        userLabel.getStyleClass().add("user-label");
        bottomBar.getChildren().add(userLabel);
        root.setBottom(bottomBar);
    }

    // the checker board
    private GridPane createStaticBoard() {
        GridPane board = new GridPane();
        board.setHgap(0);
        board.setVgap(0);
        board.setAlignment(Pos.CENTER);
        board.setStyle(
                "-fx-background-color: #5C3A21; -fx-background-radius: 8; -fx-border-color: #3B2313; -fx-border-width: 5; -fx-border-radius: 8; -fx-padding: 15;");

        for (int y = 0; y < 8; ++y) {
            for (int x = 0; x < 8; ++x) {
                VBox cell = new VBox();
                cell.setPrefSize(45, 45);
                cell.setAlignment(Pos.CENTER);

                if ((x + y) % 2 == 0) {
                    cell.setStyle("-fx-background-color: #F8CB9C;");
                } else {
                    cell.setStyle("-fx-background-color: #D28C45;");
                }

                if ((x + y) % 2 != 0) {
                    if (y < 3) {
                        Circle c = new Circle(18);
                        c.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
                        c.setStrokeWidth(2.5);
                        c.setFill(Color.web("#1E293B"));
                        c.setStroke(Color.web("#0F172A"));

                        Circle innerObj = new Circle(11);
                        innerObj.setFill(Color.TRANSPARENT);
                        innerObj.setStrokeWidth(2);
                        innerObj.setStroke(Color.web("#334155"));

                        cell.getChildren().add(new javafx.scene.layout.StackPane(c, innerObj));
                    } else if (y > 4) {
                        Circle c = new Circle(18);
                        c.setStrokeType(javafx.scene.shape.StrokeType.INSIDE);
                        c.setStrokeWidth(2.5);
                        c.setFill(Color.web("#EF4444"));
                        c.setStroke(Color.web("#B91C1C"));

                        Circle innerObj = new Circle(11);
                        innerObj.setFill(Color.TRANSPARENT);
                        innerObj.setStrokeWidth(2);
                        innerObj.setStroke(Color.web("#DC2626"));

                        cell.getChildren().add(new javafx.scene.layout.StackPane(c, innerObj));
                    }
                }
                board.add(cell, x, y);
            }
        }
        board.setMinSize(400, 400);
        board.setMaxSize(400, 400);
        return board;
    }

    // update user status
    public void updatePlayerList(Message data) {
        onlinePlayersList.getItems().clear();
        if (data.playerStatusMap != null) {
            data.playerStatusMap.forEach((user, status) -> {
                onlinePlayersList.getItems().add(user + " (" + status + ")");
            });
        }
    }

    public void setUserLabel(String user) {
        userLabel.setText("user: " + user);
    }

    public void addGlobalChatMessage(String msg) {
        if (globalChatList != null) {
            globalChatList.getItems().add(msg);
        }
    }

    public Parent getRoot() {
        return root;
    }
}
