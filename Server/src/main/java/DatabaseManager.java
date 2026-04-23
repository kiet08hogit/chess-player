import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:players.db";

    public DatabaseManager() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS players (\n"
                    + "	username TEXT PRIMARY KEY,\n"
                    + "	password TEXT NOT NULL,\n"
                    + "	wins INTEGER DEFAULT 0,\n"
                    + "	losses INTEGER DEFAULT 0\n"
                    + ");";
            stmt.execute(sql);
        } catch (Exception e) {
            System.err.println("DB Initialization Error: " + e.getMessage());
        }
    }

    public boolean registerUser(String username, String password) {
        String sql = "INSERT INTO players(username, password) VALUES(?, ?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
        } catch (Exception e) {
            return false; // User already exists or other error
        }
    }

    public boolean authenticateUser(String username, String password) {
        String sql = "SELECT password FROM players WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password").equals(password);
                }
            }
        } catch (Exception e) {
            System.err.println("Auth Error: " + e.getMessage());
        }
        return false;
    }

    public void addWin(String username) {
        String sql = "UPDATE players SET wins = wins + 1 WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Win Update Error: " + e.getMessage());
        }
    }

    public void addLoss(String username) {
        String sql = "UPDATE players SET losses = losses + 1 WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Loss Update Error: " + e.getMessage());
        }
    }

    public int[] getStats(String username) {
        String sql = "SELECT wins, losses FROM players WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new int[]{rs.getInt("wins"), rs.getInt("losses")};
                }
            }
        } catch (Exception e) {
            System.err.println("Stats Error: " + e.getMessage());
        }
        return new int[]{0, 0};
    }
}
