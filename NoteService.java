package service;

import database.DBConnection;
import model.Note;
import model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all note operations: create, read, update, delete, search.
 *
 * All note bodies are AES-256-CBC encrypted before writing to the DB.
 * They are decrypted in memory using the user's derived key after login.
 */
public class NoteService {

    private final User    currentUser;
    private final String  plainPassword; // Kept in memory only for this session

    public NoteService(User user, String plainPassword) {
        this.currentUser  = user;
        this.plainPassword = plainPassword;
    }

    /**
     * Saves a new note (encrypted) to the database.
     */
    public Note createNote(String title, String plainBody, String category) {
        String encrypted = EncryptionService.encryptWithPassword(
            plainBody, plainPassword, currentUser.getUsername()
        );

        String sql = "INSERT INTO notes (user_id, title, body, category) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(
                sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, currentUser.getId());
            ps.setString(2, title);
            ps.setString(3, encrypted);
            ps.setString(4, category);
            ps.executeUpdate();

            ResultSet keys = ps.getGeneratedKeys();
            Note note = new Note(currentUser.getId(), title, encrypted, category);
            if (keys.next()) note.setId(keys.getInt(1));
            return note;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create note", e);
        }
    }

    /**
     * Returns all notes for the current user, with bodies DECRYPTED.
     * Optionally filter by category (pass null for all).
     */
    public List<Note> getNotes(String categoryFilter) {
        String sql = categoryFilter == null
            ? "SELECT * FROM notes WHERE user_id = ? ORDER BY updated_at DESC"
            : "SELECT * FROM notes WHERE user_id = ? AND category = ? ORDER BY updated_at DESC";

        List<Note> notes = new ArrayList<>();
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            if (categoryFilter != null) ps.setString(2, categoryFilter);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Note note = new Note();
                note.setId(rs.getInt("id"));
                note.setUserId(rs.getInt("user_id"));
                note.setTitle(rs.getString("title"));
                note.setCategory(rs.getString("category"));
                note.setCreatedAt(rs.getString("created_at"));
                note.setUpdatedAt(rs.getString("updated_at"));

                // Decrypt the body before returning to the UI
                String decrypted = EncryptionService.decryptWithPassword(
                    rs.getString("body"), plainPassword, currentUser.getUsername()
                );
                note.setBody(decrypted);
                notes.add(note);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch notes", e);
        }
        return notes;
    }

    /**
     * Returns all notes (no category filter).
     */
    public List<Note> getAllNotes() {
        return getNotes(null);
    }

    /**
     * Searches notes by keyword in title OR body (case-insensitive).
     * Bodies are decrypted in memory before searching.
     */
    public List<Note> searchNotes(String keyword) {
        String lower = keyword.toLowerCase();
        List<Note> result = new ArrayList<>();
        for (Note note : getAllNotes()) {
            if (note.getTitle().toLowerCase().contains(lower)
                || note.getBody().toLowerCase().contains(lower)) {
                result.add(note);
            }
        }
        return result;
    }

    /**
     * Updates an existing note's title, body (re-encrypted), and category.
     */
    public boolean updateNote(int noteId, String newTitle, String newPlainBody, String category) {
        String encrypted = EncryptionService.encryptWithPassword(
            newPlainBody, plainPassword, currentUser.getUsername()
        );

        String sql = """
            UPDATE notes SET title = ?, body = ?, category = ?,
            updated_at = datetime('now')
            WHERE id = ? AND user_id = ?
            """;
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, newTitle);
            ps.setString(2, encrypted);
            ps.setString(3, category);
            ps.setInt(4, noteId);
            ps.setInt(5, currentUser.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update note", e);
        }
    }

    /**
     * Permanently deletes a note by ID (only if it belongs to the current user).
     */
    public boolean deleteNote(int noteId) {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, currentUser.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete note", e);
        }
    }

    /**
     * Returns the count of notes per category for the current user.
     */
    public int countByCategory(String category) {
        String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ? AND category = ?";
        try (PreparedStatement ps = DBConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, currentUser.getId());
            ps.setString(2, category);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return 0;
        }
    }
}
