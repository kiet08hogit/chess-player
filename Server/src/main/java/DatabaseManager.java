import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:players.db";

    /**
     * Initializes the SQLite database.
     * Creates the players table if it does not already exist.
     */
    public DatabaseManager() {
        try (Connection conn = DriverManager.getConnection(URL);
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS players (\n"
                    + "	username TEXT PRIMARY KEY,\n"
                    + "	password TEXT NOT NULL,\n"
                    + "	wins INTEGER DEFAULT 0,\n"
                    + "	losses INTEGER DEFAULT 0,\n"
                    + "	elo INTEGER DEFAULT 3600\n"
                    + ");";
            stmt.execute(sql);
            
            // Migration: Add elo column if it doesn't exist
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN elo INTEGER DEFAULT 3600;");
            } catch (Exception e) {
                // Column likely already exists
            }
        } catch (Exception e) {
            System.err.println("DB Initialization Error: " + e.getMessage());
        }
    }

    /**
     * Attempts to register a new user in the database.
     * @param username The desired username.
     * @param password The desired password.
     * @return True if registration was successful, false if the username already exists.
     */
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

    /**
     * Authenticates a user against the stored database credentials.
     * @param username The username attempting to log in.
     * @param password The provided password.
     * @return True if the credentials match, false otherwise.
     */
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

    /**
     * Increments the win count for a specific user.
     * @param username The username of the winning player.
     */
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

    /**
     * Increments the loss count for a specific user.
     * @param username The username of the losing player.
     */
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

    /**
     * Retrieves the win and loss statistics for a specific user.
     * @param username The username to query.
     * @return An integer array containing [wins, losses]. Defaults to [0, 0] on error.
     */
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
    public int getElo(String username) {
        String sql = "SELECT elo FROM players WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("elo");
                }
            }
        } catch (Exception e) {
            System.err.println("Elo Fetch Error: " + e.getMessage());
        }
        return 3600;
    }

    public void updateElo(String username, int newElo) {
        String sql = "UPDATE players SET elo = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newElo);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.err.println("Elo Update Error: " + e.getMessage());
        }
    }
}
