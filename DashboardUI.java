package ui;

import model.Note;
import model.User;
import service.NoteService;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Main dashboard shown after successful login.
 * Features: sidebar categories, note list, inline editor, search, logout.
 * All note bodies are decrypted in memory only — never written to disk in plain text.
 */
public class DashboardUI extends JFrame {

    // Colour aliases from LoginUI
    private static final Color BG      = LoginUI.BG;
    private static final Color SURFACE = LoginUI.SURFACE;
    private static final Color ACCENT  = LoginUI.ACCENT;
    private static final Color TEXT    = LoginUI.TEXT;
    private static final Color MUTED   = LoginUI.MUTED;
    private static final Color BORDER  = LoginUI.BORDER;
    private static final Color ERR     = LoginUI.ERR;

    private final User        currentUser;
    private final NoteService noteService;
    private final String      plainPassword;

    // Session inactivity timer (5 min auto-logout)
    private Timer inactivityTimer;
    private static final int INACTIVITY_MS = 5 * 60 * 1000;

    // UI components
    private JList<Note>       noteJList;
    private JLabel emptyLabel;
    private JPanel listContainer;
    private DefaultListModel<Note> listModel;
    private JTextField        searchField;
    private JTextArea         editorBody;
    private JTextField        editorTitle;
    private JComboBox<String> editorCategory;
    private JLabel            statusLabel;
    private String            currentFilter = "All";
    private int               editingId     = -1;

    public DashboardUI(User user, String plainPassword) {
        this.currentUser   = user;
        this.plainPassword = plainPassword;
        this.noteService   = new NoteService(user, plainPassword);

        setTitle("NoteVault — " + user.getFullName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 500));

        buildUI();
        refreshNotes();
        startInactivityTimer();
    }

    // ─── Top Bar ──────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));
        bar.setPreferredSize(new Dimension(0, 48));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 12));
        left.setBackground(SURFACE);
        JLabel icon = new JLabel("🔐"); icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
        JLabel name = new JLabel("NoteVault"); name.setFont(new Font("Segoe UI", Font.BOLD, 14));
        name.setForeground(TEXT);
        left.add(icon); left.add(name);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        right.setBackground(SURFACE);

        JLabel userChip = new JLabel("  " + currentUser.getFirstName() + "  ");
        userChip.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userChip.setForeground(new Color(15, 110, 86));
        userChip.setBackground(new Color(225, 243, 222));
        userChip.setOpaque(true);
        userChip.setBorder(new EmptyBorder(3, 0, 3, 0));

        JButton logoutBtn = new JButton("Sign out");
        logoutBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoutBtn.setForeground(MUTED);
        logoutBtn.setBackground(null);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> doLogout());

        right.add(userChip); right.add(logoutBtn);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─── Sidebar ──────────────────────────────────────────────────
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(247, 246, 242));
        sidebar.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));
        sidebar.setPreferredSize(new Dimension(180, 0));

        sidebar.add(Box.createVerticalStrut(14));
        addSectionLabel(sidebar, "CATEGORIES");

        String[][] cats = {
            {"All", "#888780"}, {"Work", "#185FA5"},
            {"Study", "#3B6D11"}, {"Personal", "#993556"}
        };
        for (String[] cat : cats) {
            JButton btn = makeCatButton(cat[0], Color.decode(cat[1]));
            btn.addActionListener(e -> {
                currentFilter = cat[0];
                refreshNotes();
            });
            sidebar.add(btn);
        }

        sidebar.add(Box.createVerticalStrut(20));
        addSectionLabel(sidebar, "SECURITY");
        JLabel sec1 = makeSmallInfo("AES-256-CBC encrypted");
        JLabel sec2 = makeSmallInfo("BCrypt password hash");
        JLabel sec3 = makeSmallInfo("PBKDF2 key derivation");
        sidebar.add(sec1); sidebar.add(sec2); sidebar.add(sec3);

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    // ─── Note List Panel ──────────────────────────────────────────
    private JPanel buildNoteListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG);
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setBorder(new MatteBorder(0, 0, 0, 1, BORDER));

        // Search bar
        JPanel searchBar = new JPanel(new BorderLayout(8, 0));
        searchBar.setBackground(SURFACE);
        searchBar.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 12, 10, 12)
        ));
        searchField = new JTextField();
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)
        ));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { refreshNotes(); }
            public void removeUpdate(DocumentEvent e)  { refreshNotes(); }
            public void changedUpdate(DocumentEvent e) { refreshNotes(); }
        });

        JButton newBtn = new JButton("+ New");
        newBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        newBtn.setForeground(Color.WHITE);
        newBtn.setBackground(TEXT);
        newBtn.setBorderPainted(false);
        newBtn.setFocusPainted(false);
        newBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newBtn.addActionListener(e -> newNote());

        searchBar.add(searchField, BorderLayout.CENTER);
        searchBar.add(newBtn, BorderLayout.EAST);
        panel.add(searchBar, BorderLayout.NORTH);

        // Note list
        listModel = new DefaultListModel<>();
        noteJList = new JList<>(listModel);
        emptyLabel = new JLabel(
    "<html><div style='text-align:center;'>📝<br><br>No notes yet<br>Create your first note!</div></html>",
    SwingConstants.CENTER
);

emptyLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 20));
emptyLabel.setForeground(Color.GRAY);
        emptyLabel = new JLabel(
    "<html><div style='text-align:center;'>📝<br><br>No notes yet<br>Create your first note!</div></html>",
    SwingConstants.CENTER
    );

emptyLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 20));
emptyLabel.setForeground(Color.GRAY);
        noteJList.setCellRenderer(new NoteListRenderer());
        noteJList.setBackground(BG);
        noteJList.setSelectionBackground(new Color(232, 240, 250));
        noteJList.setFixedCellHeight(82);
        noteJList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && noteJList.getSelectedValue() != null) {
                loadNoteInEditor(noteJList.getSelectedValue());
            }
        });

        JScrollPane scroll = new JScrollPane(noteJList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(scroll, BorderLayout.CENTER);

        statusLabel = new JLabel("0 notes");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(MUTED);
        statusLabel.setBorder(new EmptyBorder(6, 12, 6, 12));
        panel.add(statusLabel, BorderLayout.SOUTH);

        return panel;
    }

    // ─── Editor Panel ─────────────────────────────────────────────
    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(SURFACE);

        // Editor top bar
        JPanel edTop = new JPanel(new BorderLayout(8, 0));
        edTop.setBackground(SURFACE);
        edTop.setBorder(new CompoundBorder(
            new MatteBorder(0, 0, 1, 0, BORDER),
            new EmptyBorder(10, 16, 10, 16)
        ));

        editorTitle = new JTextField("Select a note or create a new one");
        editorTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        editorTitle.setForeground(TEXT);
        editorTitle.setBorder(null);
        editorTitle.setBackground(SURFACE);

        JLabel encBadge = new JLabel("🔒 AES-256");
        encBadge.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 11));
        encBadge.setForeground(MUTED);

        edTop.add(editorTitle, BorderLayout.CENTER);
        edTop.add(encBadge, BorderLayout.EAST);
        panel.add(edTop, BorderLayout.NORTH);

        // Text area
        editorBody = new JTextArea();
        editorBody.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        editorBody.setForeground(TEXT);
        editorBody.setBackground(SURFACE);
        editorBody.setLineWrap(true);
        editorBody.setWrapStyleWord(true);
        editorBody.setBorder(new EmptyBorder(16, 18, 16, 18));
        editorBody.setEnabled(false);
        resetInactivityTimer();

        JScrollPane edScroll = new JScrollPane(editorBody);
        edScroll.setBorder(null);
        panel.add(edScroll, BorderLayout.CENTER);

        // Footer bar
        JPanel footer = new JPanel(new BorderLayout(10, 0));
        footer.setBackground(new Color(247, 246, 242));
        footer.setBorder(new CompoundBorder(
            new MatteBorder(1, 0, 0, 0, BORDER),
            new EmptyBorder(8, 16, 8, 16)
        ));

        editorCategory = new JComboBox<>(new String[]{"Work", "Study", "Personal"});
        editorCategory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        editorCategory.setEnabled(false);

        JButton saveBtn = new JButton("Save note");
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(new Color(33, 150, 243)); // modern blue
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addMouseListener(new java.awt.event.MouseAdapter() {
    public void mouseEntered(java.awt.event.MouseEvent evt) {
        saveBtn.setBackground(new Color(25, 118, 210));
    }

    public void mouseExited(java.awt.event.MouseEvent evt) {
        saveBtn.setBackground(new Color(33, 150, 243));
    }
});
        saveBtn.addActionListener(e -> saveNote());

        JButton deleteBtn = new JButton("Delete");
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        deleteBtn.setForeground(ERR);
        deleteBtn.setBackground(null);
        deleteBtn.setBorderPainted(false);
        deleteBtn.setContentAreaFilled(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.addActionListener(e -> deleteCurrentNote());

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightButtons.setBackground(new Color(247, 246, 242));
        rightButtons.add(deleteBtn);
        rightButtons.add(saveBtn);

        footer.add(editorCategory, BorderLayout.WEST);
        footer.add(rightButtons, BorderLayout.EAST);
        panel.add(footer, BorderLayout.SOUTH);

        return panel;
    }

    // ─── Layout Assembly ──────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildNoteListPanel(), buildEditorPanel());
        mainSplit.setDividerLocation(270);
        mainSplit.setDividerSize(1);
        mainSplit.setBorder(null);

        JSplitPane outerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
            buildSidebar(), mainSplit);
        outerSplit.setDividerLocation(180);
        outerSplit.setDividerSize(1);
        outerSplit.setBorder(null);

        add(outerSplit, BorderLayout.CENTER);
    }

    // ─── Note Operations ──────────────────────────────────────────
    private void refreshNotes() {
    String query = searchField != null ? searchField.getText().trim() : "";

    List<Note> notes = query.isEmpty()
        ? (currentFilter.equals("All") ? noteService.getAllNotes() : noteService.getNotes(currentFilter))
        : noteService.searchNotes(query);

    listModel.clear();

    if (notes.isEmpty()) {
        statusLabel.setText("📝 No notes yet — create your first note!");
    } else {
        for (Note n : notes) {
            listModel.addElement(n);
        }
        statusLabel.setText(notes.size() + " note" + (notes.size() == 1 ? "" : "s"));
    }
}

    private void newNote() {
        editingId = -1;
        String title = searchField.getText().trim();
        editorTitle.setText(title.isEmpty() ? "Untitled" : title);
        editorBody.setText("");
        editorCategory.setSelectedIndex(0);
        editorBody.setEnabled(true);
        editorTitle.setEnabled(true);
        editorCategory.setEnabled(true);
        editorTitle.requestFocus();
        noteJList.clearSelection();
        showStatus("✍️ Start writing your new note...");
        resetInactivityTimer();
    }

    private void loadNoteInEditor(Note note) {
        editingId = note.getId();
        editorTitle.setText(note.getTitle());
        editorBody.setText(note.getBody()); // already decrypted by NoteService
        editorCategory.setSelectedItem(note.getCategory());
        editorBody.setEnabled(true);
        editorTitle.setEnabled(true);
        editorCategory.setEnabled(true);
        resetInactivityTimer();
    }

    private void saveNote() {
        String title = editorTitle.getText().trim();
        String body  = editorBody.getText();
        String cat   = (String) editorCategory.getSelectedItem();

        if (title.isEmpty()) { title = "Untitled"; editorTitle.setText(title); }

        if (editingId == -1) {
            noteService.createNote(title, body, cat);
        } else {
            noteService.updateNote(editingId, title, body, cat);
        }

        refreshNotes();
        javax.swing.SwingUtilities.invokeLater(() -> {
    if (listModel.getSize() > 0) {
        noteJList.setSelectedIndex(0);
    }
});
        showStatus("✨ Saved securely 🔐");
        resetInactivityTimer();
    }

    private void deleteCurrentNote() {
        if (editingId == -1) return;
        int choice = JOptionPane.showConfirmDialog(this,
            "Delete this note permanently?", "Confirm Delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) return;

        noteService.deleteNote(editingId);
        editingId = -1;
        editorTitle.setText(""); editorBody.setText("");
        editorBody.setEnabled(false); editorTitle.setEnabled(false); editorCategory.setEnabled(false);
        refreshNotes();
        showStatus("Note deleted.");
    }

    private void doLogout() {
        inactivityTimer.stop();
        dispose();
        new LoginUI().setVisible(true);
    }

    // ─── Session / Inactivity ─────────────────────────────────────
    private void startInactivityTimer() {
        inactivityTimer = new Timer(INACTIVITY_MS, e -> {
            JOptionPane.showMessageDialog(this,
                "Session expired due to inactivity.\nPlease log in again.",
                "Auto Logout", JOptionPane.INFORMATION_MESSAGE);
            doLogout();
        });
        inactivityTimer.setRepeats(false);
        inactivityTimer.start();

        // Reset timer on any key press or mouse activity
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(ev -> {
            resetInactivityTimer(); return false;
        });
    }

    private void resetInactivityTimer() {
        if (inactivityTimer != null) { inactivityTimer.restart(); }
    }

    // ─── Helpers ──────────────────────────────────────────────────
    private void addSectionLabel(JPanel p, String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(new Color(170, 168, 160));
        l.setBorder(new EmptyBorder(0, 12, 6, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        p.add(l);
    }

    private JButton makeCatButton(String label, Color dot) {
        JButton b = new JButton("  " + label);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(MUTED);
        b.setBackground(null);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.setMaximumSize(new Dimension(9999, 32));
        b.setAlignmentX(LEFT_ALIGNMENT);
        return b;
    }

    private JLabel makeSmallInfo(String text) {
        JLabel l = new JLabel("• " + text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(170, 168, 160));
        l.setBorder(new EmptyBorder(2, 14, 2, 12));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void showStatus(String msg) {
    statusLabel.setForeground(Color.BLACK);

    // Use emoji-friendly font
    statusLabel.setFont(new Font("Segoe UI Emoji", Font.BOLD, 14));

    // Set message
    statusLabel.setText("<html>✨ <span style='color:green;'>Saved securely</span> 🔐</html>");

    // Timer → increase duration (5 seconds)
    new javax.swing.Timer(5000, e -> statusLabel.setText("")).start();
}

    // ─── Note Cell Renderer ───────────────────────────────────────
    static class NoteListRenderer extends JPanel implements ListCellRenderer<Note> {
        private final JLabel titleLabel   = new JLabel();
        private final JLabel previewLabel = new JLabel();
        private final JLabel metaLabel   = new JLabel();
        private final JLabel catBadge    = new JLabel();

        NoteListRenderer() {
            setLayout(new BorderLayout(0, 4));
            setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, new Color(235, 233, 228)),
                new EmptyBorder(10, 12, 10, 12)
            ));

            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            previewLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            previewLabel.setForeground(MUTED);
            metaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            metaLabel.setForeground(new Color(170, 168, 160));
            catBadge.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            catBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
            catBadge.setOpaque(true);

            JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            bottomRow.setOpaque(false);
            bottomRow.add(catBadge);
            bottomRow.add(metaLabel);

            add(titleLabel, BorderLayout.NORTH);
            add(previewLabel, BorderLayout.CENTER);
            add(bottomRow, BorderLayout.SOUTH);
        }

        public Component getListCellRendererComponent(JList<? extends Note> list,
                Note note, int index, boolean isSelected, boolean cellHasFocus) {
            titleLabel.setText(note.getTitle());
            String preview = note.getBody().replace("\n", " ");
            previewLabel.setText(preview.length() > 80 ? preview.substring(0, 80) + "…" : preview);

            String ts = note.getUpdatedAt() != null
                ? note.getUpdatedAt().substring(0, 10) : "";
            metaLabel.setText("🔒 " + ts);

            // Category badge colors
            switch (note.getCategory()) {
                case "Work"     -> { catBadge.setText("Work");     catBadge.setForeground(Color.decode("#185FA5")); catBadge.setBackground(Color.decode("#E6F1FB")); }
                case "Study"    -> { catBadge.setText("Study");    catBadge.setForeground(Color.decode("#3B6D11")); catBadge.setBackground(Color.decode("#EAF3DE")); }
                default         -> { catBadge.setText("Personal"); catBadge.setForeground(Color.decode("#993556")); catBadge.setBackground(Color.decode("#FBEAF0")); }
            }

            if (isSelected) {
    setBackground(new Color(220, 235, 255)); // nicer blue

    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(33, 150, 243), 2), // blue border
        BorderFactory.createEmptyBorder(6, 8, 6, 8)
    ));

} else {
    setBackground(SURFACE);
    setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
}
            titleLabel.setForeground(TEXT);
            previewLabel.setForeground(isSelected ? Color.BLACK : Color.GRAY);
            metaLabel.setForeground(isSelected ? Color.DARK_GRAY : Color.GRAY);
            return this;
        }
    }
}
