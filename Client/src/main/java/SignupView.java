import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class SignupView {
    private VBox root;
    private TextField userField;
    private PasswordField passField;
    private Button registerBtn;
    private Button backBtn;
    private GuiClient mainApp;

    public SignupView(GuiClient mainApp) {
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
        
        Label icon = new Label("♟");
        icon.setStyle("-fx-font-size: 48px; -fx-text-fill: #3B82F6;");

        Label title = new Label("Create Account");
        title.getStyleClass().add("title-label");
        
        Label subtitle = new Label("Join the checkers community.");
        subtitle.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 13px;");
        
        VBox inputSection = new VBox(10);
        inputSection.setAlignment(Pos.CENTER_LEFT);
        
        VBox userBox = new VBox(5);
        Label userLabel = new Label("Choose Username");
        userLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");
        userField = new TextField();
        userField.setPromptText("Enter a unique username");
        userField.setPrefHeight(40);
        userField.getStyleClass().add("text-field");
        userBox.getChildren().addAll(userLabel, userField);
        
        VBox passBox = new VBox(5);
        Label passLabel = new Label("Choose Password");
        passLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-size: 12px; -fx-font-weight: bold;");
        passField = new PasswordField();
        passField.setPromptText("Enter a password");
        passField.setPrefHeight(40);
        passField.getStyleClass().add("text-field");
        passBox.getChildren().addAll(passLabel, passField);
        
        inputSection.getChildren().addAll(userBox, passBox);
        
        HBox btnBox = new HBox(10);
        btnBox.setAlignment(Pos.CENTER);
        
        backBtn = new Button("Back");
        backBtn.setPrefWidth(120);
        backBtn.getStyleClass().add("button");
        backBtn.setStyle("-fx-background-color: #6B7280;");
        backBtn.setOnAction(e -> {
            mainApp.showLoginPage();
        });
        
        registerBtn = new Button("Register");
        registerBtn.setPrefWidth(200);
        registerBtn.getStyleClass().add("button");
        registerBtn.setStyle("-fx-background-color: #3B82F6;");
        registerBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            String pass = passField.getText();
            if (!user.isEmpty() && !pass.isEmpty()) {
                mainApp.onSignupAttempt(user, pass);
            }
        });
        
        btnBox.getChildren().addAll(backBtn, registerBtn);
        
        card.getChildren().addAll(icon, title, subtitle, inputSection, btnBox);
        root.getChildren().add(card);
    }

    public void clearFields() {
        userField.clear();
        passField.clear();
    }

    public Parent getRoot() {
        return root;
    }
}
