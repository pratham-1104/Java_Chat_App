
## 2. ChatServer.java

```java
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.concurrent.*;

public class ChatServer {
    private static final int PORT = 5000;
    private static Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();
    private static int clientCounter = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║     CHAT SERVER STARTING...        ║");
        System.out.println("╚════════════════════════════════════╝");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("✓ Server started on port " + PORT);
            System.out.println("✓ Waiting for clients...\n");
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("✗ Server error: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String nickname;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Get nickname
                out.println("ENTER_NICKNAME");
                nickname = in.readLine();
                
                if (nickname == null || nickname.trim().isEmpty()) {
                    nickname = "User" + (++clientCounter);
                }

                String joinMessage = "🟢 " + nickname + " joined the chat!";
                System.out.println(joinMessage);
                broadcastMessage(joinMessage, this);

                // Send welcome message
                out.println("SERVER: Welcome to the chat, " + nickname + "!");
                out.println("SERVER: Type your messages below. Type 'exit' to leave.");

                // Handle messages
                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                    
                    String timestamp = new SimpleDateFormat("HH:mm:ss").format(new Date());
                    String formattedMessage = "[" + timestamp + "] " + nickname + ": " + message;
                    System.out.println(formattedMessage);
                    broadcastMessage(formattedMessage, this);
                }
            } catch (IOException e) {
                System.err.println("✗ Error handling client: " + e.getMessage());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            try {
                clients.remove(this);
                String leaveMessage = "🔴 " + nickname + " left the chat.";
                System.out.println(leaveMessage);
                broadcastMessage(leaveMessage, this);
                
                if (socket != null) socket.close();
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                System.err.println("✗ Error during cleanup: " + e.getMessage());
            }
        }

        private void broadcastMessage(String message, ClientHandler sender) {
            for (ClientHandler client : clients) {
                if (client != sender && client.out != null) {
                    client.out.println(message);
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }
    }
}