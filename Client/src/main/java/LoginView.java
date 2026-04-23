import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

public class LoginView {
    private VBox root;
    private TextField userField;
    private PasswordField passField;
    private Button loginBtn;
    private Button signupBtn;
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
        } catch (Exception e) {
        }

        VBox card = new VBox(15);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(350);

        Label icon = new Label("♛");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #EF4444;");

        Label title = new Label("Checkers Pal");
        title.getStyleClass().add("title-label");

        Label subtitle = new Label("Enter the arena and challenge the world.");
        subtitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");

        VBox inputSection = new VBox(10);
        inputSection.setAlignment(Pos.CENTER_LEFT);

        VBox userBox = new VBox(5);
        Label userLabel = new Label("Username");
        userLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");
        userField = new TextField();
        userField.setPromptText("Enter a username");
        userField.setPrefHeight(40);
        userField.getStyleClass().add("text-field");
        userBox.getChildren().addAll(userLabel, userField);

        VBox passBox = new VBox(5);
        Label passLabel = new Label("Password");
        passLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");
        passField = new PasswordField();
        passField.setPromptText("Enter a password");
        passField.setPrefHeight(40);
        passField.getStyleClass().add("text-field");
        passBox.getChildren().addAll(passLabel, passField);

        inputSection.getChildren().addAll(userBox, passBox);

        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);

        loginBtn = new Button("Login");
        loginBtn.setPrefWidth(160);
        loginBtn.getStyleClass().add("button");
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (!user.isEmpty() && !pass.isEmpty()) {
                mainApp.onLoginAttempt(user, pass);
            }
        });

        signupBtn = new Button("Sign Up");
        signupBtn.setPrefWidth(160);
        signupBtn.getStyleClass().add("button");
        signupBtn.setStyle("-fx-background-color: #3B82F6;");
        signupBtn.setOnAction(e -> {
            mainApp.showSignupPage();
        });

        btnBox.getChildren().addAll(loginBtn, signupBtn);

        card.getChildren().addAll(icon, title, subtitle, inputSection, btnBox);
        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
