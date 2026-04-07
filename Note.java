package model;

/**
 * Represents an encrypted note in NoteVault.
 * The 'body' field always contains AES-encrypted ciphertext when stored.
 * It is decrypted in-memory only after the user successfully logs in.
 */
public class Note {
    private int id;
    private int userId;
    private String title;
    private String body;            // AES-256 encrypted ciphertext
    private String category;        // "Work", "Study", "Personal"
    private String createdAt;
    private String updatedAt;

    public enum Category {
        WORK("Work"),
        STUDY("Study"),
        PERSONAL("Personal");

        private final String label;
        Category(String label) { this.label = label; }
        public String getLabel() { return label; }

        public static Category fromString(String s) {
            for (Category c : values()) {
                if (c.label.equalsIgnoreCase(s)) return c;
            }
            return PERSONAL;
        }
    }

    public Note() {}

    public Note(int userId, String title, String body, String category) {
        this.userId = userId;
        this.title = title;
        this.body = body;
        this.category = category;
    }

    // Getters
    public int getId()          { return id; }
    public int getUserId()      { return userId; }
    public String getTitle()    { return title; }
    public String getBody()     { return body; }
    public String getCategory() { return category; }
    public String getCreatedAt(){ return createdAt; }
    public String getUpdatedAt(){ return updatedAt; }

    // Setters
    public void setId(int id)               { this.id = id; }
    public void setUserId(int userId)       { this.userId = userId; }
    public void setTitle(String title)      { this.title = title; }
    public void setBody(String body)        { this.body = body; }
    public void setCategory(String cat)     { this.category = cat; }
    public void setCreatedAt(String ts)     { this.createdAt = ts; }
    public void setUpdatedAt(String ts)     { this.updatedAt = ts; }

    @Override
    public String toString() {
        return "Note{id=" + id + ", title='" + title + "', category='" + category + "'}";
    }
}
