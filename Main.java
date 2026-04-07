import ui.LoginUI;
import database.DBConnection;

import javax.swing.*;

/**
 * NoteVault - Password Protected Notes Application
 * Entry point for the application.
 */
public class Main {
    public static void main(String[] args) {
        // Initialize database on startup
        DBConnection.initialize();

        // Launch UI on Event Dispatch Thread (Swing requirement)
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LoginUI().setVisible(true);
        });
    }
}
