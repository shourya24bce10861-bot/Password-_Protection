package model;

/**
 * Represents a registered user in NoteVault.
 * Passwords are NEVER stored in plain text — only BCrypt hashes.
 */
public class User {
    private int id;
    private String username;
    private String passwordHash; // BCrypt hash, never plain text
    private String firstName;
    private String lastName;
    private String createdAt;

    public User() {}

    public User(String username, String passwordHash, String firstName, String lastName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters
    public int getId()              { return id; }
    public String getUsername()     { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFirstName()    { return firstName; }
    public String getLastName()     { return lastName; }
    public String getCreatedAt()    { return createdAt; }
    public String getFullName()     { return firstName + " " + lastName; }

    // Setters
    public void setId(int id)                       { this.id = id; }
    public void setUsername(String username)         { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFirstName(String firstName)       { this.firstName = firstName; }
    public void setLastName(String lastName)         { this.lastName = lastName; }
    public void setCreatedAt(String createdAt)       { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', name='" + getFullName() + "'}";
    }
}
