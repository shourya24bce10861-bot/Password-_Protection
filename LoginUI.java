package ui;

import model.User;
import service.AuthService;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Login and Registration screen for NoteVault.
 * Built with Java Swing. Validates input and uses AuthService for auth.
 */
public class LoginUI extends JFrame {

    // ─── Color Palette ────────────────────────────────────────────
    static final Color BG        = new Color(250, 249, 246);
    static final Color SURFACE   = Color.WHITE;
    static final Color ACCENT    = new Color(24, 95, 165);
    static final Color TEXT      = new Color(28, 28, 26);
    static final Color MUTED     = new Color(95, 94, 90);
    static final Color BORDER    = new Color(210, 208, 200);
    static final Color ERR       = new Color(162, 45, 45);
    static final Color STR_WEAK  = new Color(162, 45, 45);
    static final Color STR_FAIR  = new Color(186, 117, 23);
    static final Color STR_GOOD  = new Color(59, 109, 17);

    // ─── UI State ─────────────────────────────────────────────────
    private JPanel   cardPanel;
    private CardLayout cardLayout;

    // Login fields
    private JTextField     loginUsername;
    private JPasswordField loginPassword;
    private JLabel         loginError;

    // Register fields
    private JTextField     regFirst, regLast, regUsername;
    private JPasswordField regPassword;
    private JProgressBar   strengthBar;
    private JLabel         strengthLabel, regError;

    public LoginUI() {
        setTitle("NoteVault — Secure Notes");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(440, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(BG);

        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Top branding bar
        JPanel topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);

        // Card layout for login / register
        cardLayout = new CardLayout();
        cardPanel  = new JPanel(cardLayout);
        cardPanel.setBackground(BG);
        cardPanel.add(buildLoginPanel(),    "login");
        cardPanel.add(buildRegisterPanel(), "register");
        add(cardPanel, BorderLayout.CENTER);
    }

    // ─── Top Bar ──────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 12));
        bar.setBackground(SURFACE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, BORDER));

        JLabel icon = new JLabel("🔐");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));

        JLabel name = new JLabel("NoteVault");
        name.setFont(new Font("Segoe UI", Font.BOLD, 15));
        name.setForeground(TEXT);

        JLabel badge = new JLabel(" AES-256 Encrypted ");
        badge.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        badge.setForeground(new Color(15, 110, 86));
        badge.setBackground(new Color(225, 243, 222));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));

        bar.add(icon);
        bar.add(name);
        bar.add(Box.createHorizontalStrut(8));
        bar.add(badge);
        return bar;
    }

    // ─── Login Panel ──────────────────────────────────────────────
    private JPanel buildLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(28, 28, 24, 28)
        ));
        card.setMaximumSize(new Dimension(360, 999));

        card.add(makeLabel("Welcome back", 20, Font.BOLD, TEXT));
        card.add(Box.createVerticalStrut(4));
        card.add(makeLabel("Sign in to decrypt your notes", 13, Font.PLAIN, MUTED));
        card.add(Box.createVerticalStrut(22));

        loginUsername = makeField("Username");
        card.add(fieldBlock("Username", loginUsername));
        card.add(Box.createVerticalStrut(12));

        loginPassword = new JPasswordField();
        styleField(loginPassword, "Password");
        card.add(fieldBlock("Password", loginPassword));
        card.add(Box.createVerticalStrut(6));

        loginError = makeLabel("", 12, Font.PLAIN, ERR);
        loginError.setAlignmentX(LEFT_ALIGNMENT);
        card.add(loginError);
        card.add(Box.createVerticalStrut(14));

        JButton loginBtn = makePrimaryButton("Unlock vault");
        loginBtn.addActionListener(e -> doLogin());
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(14));

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setBackground(SURFACE);
        switchRow.add(makeLabel("No account?", 13, Font.PLAIN, MUTED));
        JButton switchBtn = makeLinkButton("Create one");
        switchBtn.addActionListener(e -> cardLayout.show(cardPanel, "register"));
        switchRow.add(switchBtn);
        card.add(switchRow);

        outer.add(card);
        return outer;
    }

    // ─── Register Panel ───────────────────────────────────────────
    private JPanel buildRegisterPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(BG);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(SURFACE);
        card.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(28, 28, 24, 28)
        ));

        card.add(makeLabel("Create account", 20, Font.BOLD, TEXT));
        card.add(Box.createVerticalStrut(4));
        card.add(makeLabel("Your notes will be AES-256 encrypted", 13, Font.PLAIN, MUTED));
        card.add(Box.createVerticalStrut(22));

        // First / Last name row
        JPanel nameRow = new JPanel(new GridLayout(1, 2, 10, 0));
        nameRow.setBackground(SURFACE);
        nameRow.setAlignmentX(LEFT_ALIGNMENT);
        nameRow.setMaximumSize(new Dimension(9999, 70));
        regFirst = makeField("First name");
        regLast  = makeField("Last name");
        nameRow.add(fieldBlock("First name", regFirst));
        nameRow.add(fieldBlock("Last name",  regLast));
        card.add(nameRow);
        card.add(Box.createVerticalStrut(12));

        regUsername = makeField("Choose a username");
        card.add(fieldBlock("Username", regUsername));
        card.add(Box.createVerticalStrut(12));

        regPassword = new JPasswordField();
        styleField(regPassword, "Create a strong password");
        regPassword.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { updateStrength(); }
        });

        strengthBar = new JProgressBar(0, 4);
        strengthBar.setStringPainted(false);
        strengthBar.setPreferredSize(new Dimension(999, 5));
        strengthBar.setMaximumSize(new Dimension(9999, 5));
        strengthBar.setAlignmentX(LEFT_ALIGNMENT);
        strengthBar.setBorderPainted(false);
        strengthBar.setBackground(BORDER);
        strengthBar.setForeground(STR_WEAK);

        strengthLabel = makeLabel("Enter a password", 11, Font.PLAIN, MUTED);
        strengthLabel.setAlignmentX(LEFT_ALIGNMENT);

        JPanel passBlock = fieldBlock("Password", regPassword);
        passBlock.add(Box.createVerticalStrut(5));
        passBlock.add(strengthBar);
        passBlock.add(Box.createVerticalStrut(3));
        passBlock.add(strengthLabel);
        card.add(passBlock);
        card.add(Box.createVerticalStrut(6));

        regError = makeLabel("", 12, Font.PLAIN, ERR);
        regError.setAlignmentX(LEFT_ALIGNMENT);
        card.add(regError);
        card.add(Box.createVerticalStrut(14));

        JButton regBtn = makePrimaryButton("Create account");
        regBtn.addActionListener(e -> doRegister());
        card.add(regBtn);
        card.add(Box.createVerticalStrut(14));

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setBackground(SURFACE);
        switchRow.add(makeLabel("Already have an account?", 13, Font.PLAIN, MUTED));
        JButton switchBtn = makeLinkButton("Sign in");
        switchBtn.addActionListener(e -> cardLayout.show(cardPanel, "login"));
        switchRow.add(switchBtn);
        card.add(switchRow);

        outer.add(card);
        return outer;
    }

    // ─── Actions ──────────────────────────────────────────────────
    private void doLogin() {
        String user = loginUsername.getText().trim();
        String pass = new String(loginPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            loginError.setText("Please enter your username and password.");
            return;
        }

        User loggedIn = AuthService.login(user, pass);
        if (loggedIn == null) {
            loginError.setText("Invalid username or password.");
            return;
        }

        loginError.setText("");
        dispose();
        new DashboardUI(loggedIn, pass).setVisible(true);
    }

    private void doRegister() {
        String first = regFirst.getText().trim();
        String last  = regLast.getText().trim();
        String user  = regUsername.getText().trim();
        String pass  = new String(regPassword.getPassword());

        if (first.isEmpty() || last.isEmpty() || user.isEmpty() || pass.isEmpty()) {
            regError.setText("All fields are required."); return;
        }
        if (pass.length() < 6) {
            regError.setText("Password must be at least 6 characters."); return;
        }
        if (AuthService.passwordStrength(pass) < 2) {
            regError.setText("Please choose a stronger password."); return;
        }

        boolean ok = AuthService.register(user, pass, first, last);
        if (!ok) { regError.setText("Username already taken."); return; }

        regError.setText("");
        JOptionPane.showMessageDialog(this,
            "Account created! Please sign in.", "Welcome!", JOptionPane.INFORMATION_MESSAGE);
        cardLayout.show(cardPanel, "login");
        loginUsername.setText(user);
    }

    private void updateStrength() {
        String pass = new String(regPassword.getPassword());
        int score   = AuthService.passwordStrength(pass);
        strengthBar.setValue(score);
        Color[] colors = { STR_WEAK, STR_WEAK, STR_FAIR, STR_GOOD, STR_GOOD };
        strengthBar.setForeground(colors[score]);
        strengthLabel.setText("Strength: " + AuthService.strengthLabel(score));
    }

    // ─── UI Helpers ───────────────────────────────────────────────
    private JTextField makeField(String placeholder) {
        JTextField f = new JTextField();
        styleField(f, placeholder);
        return f;
    }

    private void styleField(JTextField f, String placeholder) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setForeground(TEXT);
        f.setBackground(new Color(247, 246, 242));
        f.setBorder(new CompoundBorder(
            new LineBorder(BORDER, 1, true),
            new EmptyBorder(8, 10, 8, 10)
        ));
        f.putClientProperty("placeholder", placeholder);
    }

    private JPanel fieldBlock(String labelText, JComponent field) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(SURFACE);
        p.setAlignmentX(LEFT_ALIGNMENT);

        JLabel lbl = makeLabel(labelText, 12, Font.BOLD, MUTED);
        lbl.setBorder(new EmptyBorder(0, 0, 5, 0));
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        field.setAlignmentX(LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(9999, 38));

        p.add(lbl);
        p.add(field);
        return p;
    }

    private JButton makePrimaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setForeground(Color.WHITE);
        b.setBackground(TEXT);
        b.setOpaque(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setAlignmentX(LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(9999, 42));
        b.setPreferredSize(new Dimension(9999, 42));
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e)  { b.setBackground(new Color(50, 50, 48)); }
            public void mouseExited(MouseEvent e)   { b.setBackground(TEXT); }
        });
        return b;
    }

    private JButton makeLinkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(ACCENT);
        b.setBackground(null);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel makeLabel(String text, int size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }
}
