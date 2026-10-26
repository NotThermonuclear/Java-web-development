import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Main.java
 * Author: Rushil Kher | B.Tech CSE (Cybersecurity), VIT Vellore
 *
 * Description:
 * Ready-to-run IntelliJ IDEA Java Application.
 * Launches BOTH:
 * 1. An interactive Java Swing Desktop Admin Control Panel (opens a window immediately when you press Run in IntelliJ).
 * 2. A live Local Web Server at http://localhost:8080 with REST API and HTML Frontend.
 */
public class Main {

    private static final int PORT = 8080;
    private static final List<TaskItem> taskDatabase = Collections.synchronizedList(new ArrayList<>());
    private static final AtomicInteger idCounter = new AtomicInteger(1);
    private static DefaultTableModel tableModel;
    private static JTextArea logArea;

    static class TaskItem {
        final int id;
        final String title;
        final String category;
        final String status;
        final String createdAt;

        TaskItem(int id, String title, String category, String status) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.status = status;
            this.createdAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        String toJson() {
            return String.format(
                    "{\"id\":%d,\"title\":\"%s\",\"category\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\"}",
                    id, escapeJson(title), escapeJson(category), escapeJson(status), createdAt
            );
        }

        private String escapeJson(String s) {
            return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
        }
    }

    public static void main(String[] args) {
        // Seed initial data
        addTaskInternal("Configure Security Firewall Rules", "Cybersecurity", "Completed");
        addTaskInternal("Optimize MySQL Relational Schema", "Database", "In Progress");
        addTaskInternal("Implement REST API Authentication", "Backend", "Completed");
        addTaskInternal("Integrate Frontend Dashboard UI", "Frontend", "In Progress");

        // Start Embedded HTTP Web Server
        startWebServer();

        // Launch Desktop GUI Window in IntelliJ immediately
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static synchronized TaskItem addTaskInternal(String title, String category, String status) {
        TaskItem item = new TaskItem(idCounter.getAndIncrement(), title, category, status);
        taskDatabase.add(item);
        if (tableModel != null) {
            SwingUtilities.invokeLater(() -> tableModel.addRow(new Object[]{
                    "#" + item.id, item.title, item.category, item.status, item.createdAt
            }));
        }
        logEvent("Task Added: [" + category + "] " + title + " (" + status + ")");
        return item;
    }

    private static void logEvent(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String line = "[" + timestamp + "] " + message;
        System.out.println(line);
        if (logArea != null) {
            SwingUtilities.invokeLater(() -> logArea.append(line + "\n"));
        }
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Java Web Application Control Center | Rushil Kher");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 600);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(new EmptyBorder(15, 15, 15, 15));
        root.setBackground(new Color(15, 23, 42));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 41, 59));
        headerPanel.setBorder(new EmptyBorder(12, 16, 12, 16));

        JLabel titleLabel = new JLabel("Java Web Application & Backend Server (Active on Port " + PORT + ")");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(new Color(56, 189, 248));

        JButton openBrowserBtn = new JButton("Open Web Portal in Browser (localhost:8080)");
        openBrowserBtn.setBackground(new Color(56, 189, 248));
        openBrowserBtn.setForeground(new Color(15, 23, 42));
        openBrowserBtn.setFocusPainted(false);
        openBrowserBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        openBrowserBtn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + PORT));
                logEvent("Opened http://localhost:" + PORT + " in default web browser.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Open http://localhost:" + PORT + " in your browser.");
            }
        });

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(openBrowserBtn, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center Table
        String[] cols = {"ID", "Task Description", "Category", "Status", "Created At"};
        tableModel = new DefaultTableModel(cols, 0);
        for (TaskItem t : taskDatabase) {
            tableModel.addRow(new Object[]{"#" + t.id, t.title, t.category, t.status, t.createdAt});
        }

        JTable table = new JTable(tableModel);
        table.setRowHeight(26);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        JScrollPane tableScroll = new JScrollPane(table);

        // Add Task Form Panel
        JPanel formPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formPanel.setBackground(new Color(30, 41, 59));

        JTextField titleField = new JTextField(22);
        JComboBox<String> categoryBox = new JComboBox<>(new String[]{"Backend", "Cybersecurity", "Database", "Frontend"});
        JComboBox<String> statusBox = new JComboBox<>(new String[]{"In Progress", "Completed", "Pending"});
        JButton addBtn = new JButton("Add Task to Server");
        addBtn.setBackground(new Color(34, 197, 94));
        addBtn.setForeground(Color.WHITE);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));

        addBtn.addActionListener(e -> {
            String text = titleField.getText().trim();
            if (!text.isEmpty()) {
                addTaskInternal(text, (String) categoryBox.getSelectedItem(), (String) statusBox.getSelectedItem());
                titleField.setText("");
            }
        });

        JLabel taskLbl = new JLabel("New Task:");
        taskLbl.setForeground(Color.WHITE);
        formPanel.add(taskLbl);
        formPanel.add(titleField);
        formPanel.add(categoryBox);
        formPanel.add(statusBox);
        formPanel.add(addBtn);

        // Server Logs Panel
        logArea = new JTextArea(6, 60);
        logArea.setEditable(false);
        logArea.setBackground(new Color(2, 6, 23));
        logArea.setForeground(new Color(74, 222, 128));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setText("--- Server Live Logs (Listening on http://localhost:" + PORT + ") ---\n");
        JScrollPane logScroll = new JScrollPane(logArea);

        JPanel southPanel = new JPanel(new BorderLayout(8, 8));
        southPanel.setOpaque(false);
        southPanel.add(formPanel, BorderLayout.NORTH);
        southPanel.add(logScroll, BorderLayout.CENTER);

        root.add(tableScroll, BorderLayout.CENTER);
        root.add(southPanel, BorderLayout.SOUTH);

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static void startWebServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new WebPortalHandler());
            server.createContext("/api/tasks", new TaskApiHandler());
            server.setExecutor(Executors.newFixedThreadPool(8));
            server.start();
            logEvent("HTTP Web Server started on http://localhost:" + PORT);
        } catch (IOException e) {
            logEvent("Port " + PORT + " already in use or failed to bind: " + e.getMessage());
        }
    }

    static class WebPortalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logEvent("HTTP GET / (Web Portal accessed from browser)");
            String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Java Web Portal | Rushil Kher</title>" +
                    "<style>body{background:#0f172a;color:#f8fafc;font-family:system-ui,sans-serif;padding:2rem;}" +
                    ".container{max-width:850px;margin:0 auto;}.card{background:#1e293b;padding:1.5rem;border-radius:0.75rem;margin-bottom:1.5rem;border:1px solid #334155;}" +
                    "h1{color:#38bdf8;margin-bottom:0.25rem;}table{width:100%;border-collapse:collapse;margin-top:1rem;}" +
                    "th,td{padding:0.75rem;text-align:left;border-bottom:1px solid #334155;}th{color:#94a3b8;font-size:0.85rem;text-transform:uppercase;}" +
                    "input,select,button{padding:0.6rem 1rem;border-radius:0.5rem;border:1px solid #475569;background:#0f172a;color:#fff;}" +
                    "button{background:#38bdf8;color:#0f172a;font-weight:bold;border:none;cursor:pointer;}</style></head><body>" +
                    "<div class=\"container\"><div class=\"card\"><h1>Java Backend & Web Application Portal</h1>" +
                    "<p style=\"color:#94a3b8;\">Developed by Rushil Kher | B.Tech CSE (Cybersecurity), VIT Vellore</p></div>" +
                    "<div class=\"card\"><h3>Add Task via Web Frontend</h3><div style=\"display:flex;gap:0.5rem;margin-top:0.75rem;\">" +
                    "<input id=\"t\" placeholder=\"Task title...\" style=\"flex:1;\"><select id=\"c\"><option>Backend</option><option>Cybersecurity</option><option>Database</option><option>Frontend</option></select>" +
                    "<select id=\"s\"><option>In Progress</option><option>Completed</option></select><button onclick=\"add()\">Add Task</button></div></div>" +
                    "<div class=\"card\"><h3>Live Synchronized Tasks (Synced with IntelliJ GUI)</h3><table><thead><tr><th>ID</th><th>Title</th><th>Category</th><th>Status</th><th>Timestamp</th></tr></thead>" +
                    "<tbody id=\"tb\"></tbody></table></div></div>" +
                    "<script>async function load(){const r=await fetch('/api/tasks');const d=await r.json();document.getElementById('tb').innerHTML=d.map(x=>`<tr><td>#${x.id}</td><td>${x.title}</td><td>${x.category}</td><td>${x.status}</td><td>${x.createdAt}</td></tr>`).join('');}" +
                    "async function add(){const t=document.getElementById('t').value;if(!t)return;await fetch('/api/tasks',{method:'POST',body:t+'|'+document.getElementById('c').value+'|'+document.getElementById('s').value});document.getElementById('t').value='';load();}" +
                    "load();setInterval(load,2000);</script></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class TaskApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder sb = new StringBuilder("[");
                synchronized (taskDatabase) {
                    for (int i = 0; i < taskDatabase.size(); i++) {
                        sb.append(taskDatabase.get(i).toJson());
                        if (i < taskDatabase.size() - 1) sb.append(",");
                    }
                }
                sb.append("]");
                byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
                String[] parts = body.split("\\|");
                String title = parts.length > 0 ? parts[0] : "New Task";
                String cat = parts.length > 1 ? parts[1] : "Backend";
                String stat = parts.length > 2 ? parts[2] : "In Progress";
                TaskItem item = addTaskInternal(title, cat, stat);
                byte[] bytes = item.toJson().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(201, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        }
    }
}