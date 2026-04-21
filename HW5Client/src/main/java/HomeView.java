import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
        } catch (Exception e) {}
        root.getStyleClass().add("home-root");

        // --- Left Sidebar ---
        VBox leftSidebar = new VBox();
        leftSidebar.getStyleClass().add("sidebar");
        leftSidebar.setMinWidth(220); 
        
        Button playRandom = new Button("RANDOM MATCH");
        Button playFriend = new Button("PLAY WITH FRIEND");
        Button watchMatch = new Button("WATCH MATCH");
        Button playBot = new Button("PLAY WITH BOT");
        Button readRules = new Button("READ RULE");
        
        playRandom.getStyleClass().add("menu-button");
        playFriend.getStyleClass().add("menu-button");
        watchMatch.getStyleClass().add("menu-button");
        playBot.getStyleClass().add("menu-button");
        readRules.getStyleClass().add("menu-button");
        
        for (Button b : new Button[]{playRandom, playFriend, watchMatch, playBot, readRules}) {
            b.setMinWidth(200);
            b.setMaxWidth(Double.MAX_VALUE);
        }

        playRandom.setOnAction(e -> mainApp.onFindMatch());
        
        playFriend.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Play with Friend");
            dialog.setHeaderText("Enter Room ID to join or create");
            dialog.setContentText("Room ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(roomId -> mainApp.onJoinRoom(roomId));
        });
        
        watchMatch.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Watch Match");
            dialog.setHeaderText("Enter Room ID to spectate");
            dialog.setContentText("Room ID:");
            Optional<String> result = dialog.showAndWait();
            result.ifPresent(roomId -> mainApp.onWatchMatch(roomId));
        });
        
        leftSidebar.getChildren().addAll(playRandom, playFriend, watchMatch, playBot, readRules);
        root.setLeft(leftSidebar);

        // --- Center Area (Static Board) ---
        StackPane centerWrapper = new StackPane();
        centerWrapper.setPadding(new Insets(20));
        GridPane homeBoard = createStaticBoard();
        centerWrapper.getChildren().add(homeBoard);
        root.setCenter(centerWrapper);

        // --- Right Sidebar ---
        VBox rightSidebar = new VBox();
        rightSidebar.getStyleClass().add("player-list");
        rightSidebar.setMinWidth(220);
        Label listTitle = new Label("Online Players");
        listTitle.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        onlinePlayersList = new ListView<>();
        onlinePlayersList.setMinWidth(200);
        rightSidebar.getChildren().addAll(listTitle, onlinePlayersList);
        root.setRight(rightSidebar);

        // --- Bottom Bar ---
        HBox bottomBar = new HBox();
        bottomBar.getStyleClass().add("user-info-bar");
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        userLabel = new Label("user: ");
        userLabel.getStyleClass().add("user-label");
        bottomBar.getChildren().add(userLabel);
        root.setBottom(bottomBar);
    }

    private GridPane createStaticBoard() {
        GridPane board = new GridPane();
        board.setHgap(2); board.setVgap(2);
        board.setAlignment(Pos.CENTER);
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                VBox cell = new VBox();
                cell.setPrefSize(45, 45);
                cell.setAlignment(Pos.CENTER);
                cell.setStyle("-fx-background-color: " + ((x+y)%2 == 0 ? "#3d3d3d" : "#2d2d2d") + ";");
                if ((x+y)%2 != 0) {
                    if (y < 3) {
                        Circle c = new Circle(18, Color.web("#2c3e50"));
                        cell.getChildren().add(c);
                    } else if (y > 4) {
                        Circle c = new Circle(18, Color.web("#e74c3c"));
                        cell.getChildren().add(c);
                    }
                }
                board.add(cell, x, y);
            }
        }
        board.setMinSize(400, 400);
        board.setMaxSize(400, 400);
        return board;
    }

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

    public Parent getRoot() {
        return root;
    }
}
