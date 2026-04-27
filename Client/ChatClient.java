import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient extends JFrame {
    private JTextArea messageArea;
    private JTextField inputField;
    private JButton sendButton;
    private JButton connectButton;
    private JTextField nicknameField;
    private JLabel statusLabel;
    
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public ChatClient() {
        setTitle("💬 Chat Application");
        setSize(600, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        // Main panel with gradient
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(67, 160, 71),
                                                     0, getHeight(), new Color(56, 142, 60));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Top panel (connection)
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        
        JPanel nicknamePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        nicknamePanel.setOpaque(false);
        
        JLabel nicknameLabel = new JLabel("Nickname:");
        nicknameLabel.setForeground(Color.WHITE);
        nicknameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        nicknameField = new JTextField(15);
        nicknameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleTextField(nicknameField);
        
        connectButton = createStyledButton("Connect", new Color(33, 150, 243));
        connectButton.addActionListener(e -> toggleConnection());
        
        statusLabel = new JLabel("⚫ Disconnected");
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        nicknamePanel.add(nicknameLabel);
        nicknamePanel.add(nicknameField);
        nicknamePanel.add(connectButton);
        nicknamePanel.add(statusLabel);
        
        topPanel.add(nicknamePanel, BorderLayout.CENTER);

        // Message area
        messageArea = new JTextArea();
        messageArea.setEditable(false);
        messageArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        messageArea.setBackground(new Color(250, 250, 250));
        messageArea.setMargin(new Insets(10, 10, 10, 10));
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(messageArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 2));
        scrollPane.setPreferredSize(new Dimension(560, 450));

        // Bottom panel (input)
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false);
        
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setEnabled(false);
        styleTextField(inputField);
        inputField.addActionListener(e -> sendMessage());
        
        sendButton = createStyledButton("Send", new Color(76, 175, 80));
        sendButton.setEnabled(false);
        sendButton.addActionListener(e -> sendMessage());
        
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Add all panels
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private void styleTextField(JTextField field) {
        field.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 2),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(color.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(color.brighter());
                } else {
                    g2d.setColor(color);
                }
                
                if (!isEnabled()) {
                    g2d.setColor(Color.GRAY);
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(100, 40));
        
        return button;
    }

    private void toggleConnection() {
        if (!connected) {
            connect();
        } else {
            disconnect();
        }
    }

    private void connect() {
        String nickname = nicknameField.getText().trim();
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a nickname!", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Wait for nickname request
            String response = in.readLine();
            if ("ENTER_NICKNAME".equals(response)) {
                out.println(nickname);
            }

            connected = true;
            connectButton.setText("Disconnect");
            connectButton.setBackground(new Color(244, 67, 54));
            statusLabel.setText("🟢 Connected");
            nicknameField.setEnabled(false);
            inputField.setEnabled(true);
            sendButton.setEnabled(true);
            inputField.requestFocus();

            // Start message listener
            new Thread(new MessageReceiver()).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Could not connect to server!\n" + e.getMessage(),
                "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println("exit");
            }
            if (socket != null) {
                socket.close();
            }
            
            connected = false;
            connectButton.setText("Connect");
            statusLabel.setText("⚫ Disconnected");
            nicknameField.setEnabled(true);
            inputField.setEnabled(false);
            sendButton.setEnabled(false);
            
            messageArea.append("\n--- Disconnected from server ---\n");
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty() && connected) {
            out.println(message);
            inputField.setText("");
        }
    }

    private class MessageReceiver implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    final String msg = message;
                    SwingUtilities.invokeLater(() -> {
                        messageArea.append(msg + "\n");
                        messageArea.setCaretPosition(messageArea.getDocument().getLength());
                    });
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    if (connected) {
                        messageArea.append("\n--- Connection lost ---\n");
                        disconnect();
                    }
                });
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new ChatClient());
    }
}