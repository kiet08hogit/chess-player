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
        
        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(350);
        
        Label title = new Label("CHECKERS");
        title.getStyleClass().add("title-label");
        
        userField = new TextField();
        userField.setPromptText("Username");
        userField.setPrefHeight(40);
        
        loginBtn = new Button("JOIN LOBBY");
        loginBtn.setPrefWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> {
            String user = userField.getText().trim();
            if (!user.isEmpty()) {
                mainApp.onLoginAttempt(user);
            }
        });
        
        card.getChildren().addAll(title, userField, loginBtn);
        root.getChildren().add(card);
    }

    public Parent getRoot() {
        return root;
    }
}
