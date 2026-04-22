import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class LoginView {
    private VBox root;
    private TextField userField;
    private Button loginBtn;
    private GuiClient mainApp;

    public LoginView(GuiClient mainApp) {
        this.mainApp = mainApp;
        createGui();
    }

    private void createGui() {
        root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(50));
        try {
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        } catch (Exception e) {}
        
        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(350);
        
        Label icon = new Label("♛");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #EF4444;");

        Label title = new Label("Checkers Master");
        title.getStyleClass().add("title-label");
        
        Label subtitle = new Label("Enter the arena and challenge the world.");
        subtitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        
        VBox inputSection = new VBox(5);
        inputSection.setAlignment(Pos.CENTER_LEFT);
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        userField = new TextField();
        userField.setPromptText("e.g. PlayerOne");
        userField.setPrefHeight(40);
        userField.getStyleClass().add("text-field");
        
        inputSection.getChildren().addAll(userLabel, userField);
        
        loginBtn = new Button("Connect to Server");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.getStyleClass().add("button");
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            if (!user.isEmpty()) {
                mainApp.onLoginAttempt(user);
            }
        });
        
        card.getChildren().addAll(icon, title, subtitle, inputSection, loginBtn);
        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
