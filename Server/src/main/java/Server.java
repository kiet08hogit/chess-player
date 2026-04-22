import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Consumer;

import javafx.application.Platform;

public class Server {

    private Consumer<Serializable> callback;
    private TheServer server;
    public GameManager gameManager;

    Server(Consumer<Serializable> call) {
        callback = call;
        gameManager = new GameManager(this);
        server = new TheServer();
        server.start();
    }

    public void log(String msg) {
        if (callback != null) {
            Platform.runLater(() -> callback.accept(msg));
        }
    }

    public class TheServer extends Thread {
        public void run() {
            try (ServerSocket mysocket = new ServerSocket(5555)) {
                log("Server is waiting for clients!");

                while (true) {
                    Socket s = mysocket.accept();
                    ClientThread c = new ClientThread(s);
                    c.start();
                }
            } catch (Exception e) {
                log("Server socket did not launch");
            }
        }
    }

    public class ClientThread extends Thread {
        Socket connection;
        ObjectInputStream in;
        ObjectOutputStream out;

        public String username;
        public String status = "Online";
        public GameManager.GameSession gameSession;

        ClientThread(Socket s) {
            this.connection = s;
        }

        public void send(Message m) {
            try {
                if (out != null) {
                    out.writeObject(m);
                    out.reset(); // Crucial for sending updated arrays
                }
            } catch (Exception e) {
                log("Error sending to " + username);
            }
        }

        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                in = new ObjectInputStream(connection.getInputStream());
                connection.setTcpNoDelay(true);
            } catch (Exception e) {
                log("Streams not open");
                return;
            }

            while (true) {
                try {
                    Message data = (Message) in.readObject();
                    
                    if (data.action == Message.Action.LOGIN) {
                        boolean success = gameManager.login(data.username, this);
                        if (!success) {
                            Message resp = new Message(Message.Action.LOGIN_FAIL);
                            resp.content = "Username taken or invalid.";
                            send(resp);
                        }
                    } else {
                        gameManager.processMessage(this, data);
                    }

                } catch (Exception e) {
                    gameManager.disconnect(this);
                    break;
                }
            }
        }
    }
}
