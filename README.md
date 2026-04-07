# NoteVault — Password Protected Notes Application
## Java Swing + SQLite + BCrypt + AES-256

---

## Project Overview

NoteVault is a secure desktop notes application that protects your data using:
- **BCrypt** for password hashing (work factor 12)
- **AES-256-CBC** with PBKDF2 key derivation for note encryption
- **SQLite** for persistent local storage
- **Java Swing** for the GUI

---

## Project Structure

```
src/
├── Main.java                     ← Entry point
├── model/
│   ├── User.java                 ← User data model
│   └── Note.java                 ← Note data model
├── service/
│   ├── AuthService.java          ← BCrypt login / register
│   ├── NoteService.java          ← Encrypted CRUD operations
│   └── EncryptionService.java    ← AES-256-CBC encrypt/decrypt
├── database/
│   └── DBConnection.java         ← SQLite connection + schema
└── ui/
    ├── LoginUI.java              ← Login & register screen
    └── DashboardUI.java          ← Main notes dashboard
```

---

## Dependencies (JAR files needed)

Download and place in a `lib/` folder:

| Library | Purpose | Download |
|---|---|---|
| `sqlite-jdbc-3.x.x.jar` | SQLite database driver | https://github.com/xerial/sqlite-jdbc/releases |
| `jbcrypt-0.4.jar` | BCrypt password hashing | https://www.mindrot.org/projects/jBCrypt/ |

**Maven equivalents:**
```xml
<dependency>
    <groupId>org.xerial</groupId>
    <artifactId>sqlite-jdbc</artifactId>
    <version>3.45.1.0</version>
</dependency>
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```

---

## Compilation & Run

### Option A — Command Line

```bash
# From the project root
javac -cp "lib/*" -d out/ src/**/*.java src/Main.java
java  -cp "lib/*:out/" Main
```

On Windows:
```cmd
javac -cp "lib\*" -d out src\**\*.java src\Main.java
java  -cp "lib\*;out" Main
```

### Option B — IntelliJ IDEA
1. File → New Project → Empty Project
2. Add `lib/` JARs as dependencies (Project Structure → Libraries)
3. Set `Main.java` as the Run Configuration entry point
4. Click Run

### Option C — Eclipse
1. File → New Java Project
2. Right-click project → Build Path → Add External JARs → select both JARs
3. Right-click `Main.java` → Run As → Java Application

---

## Application Flow

```
Start App
    │
    ▼
Login / Register Screen
    │
    ├─ Register: BCrypt.hashpw(password) → stored in DB
    │
    └─ Login:    BCrypt.checkpw(input, hash)
                    │
                    ▼
              Dashboard
                    │
                    ├─ Add Note → AES-256 encrypt body → save to DB
                    ├─ View Note → load from DB → AES-256 decrypt → show
                    ├─ Edit Note → re-encrypt on save
                    ├─ Delete Note → remove from DB
                    ├─ Search → decrypt in memory → keyword match
                    └─ Logout → dispose window → return to Login
```

---

## Security Design

### Password Storage (BCrypt)
```java
// Registration — never store plain text
String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
// 12 = work factor (2^12 iterations)

// Login — constant-time comparison
boolean ok = BCrypt.checkpw(inputPassword, storedHash);
```

### Note Encryption (AES-256-CBC)
```java
// Key derivation from password
PBKDF2WithHmacSHA256(password, salt, 65536 iterations) → 256-bit AES key

// Per-note encryption
Random IV (16 bytes) → AES/CBC/PKCS5Padding → Base64(IV + ciphertext) → DB

// Decryption
Base64 decode → split IV + ciphertext → AES decrypt → plain text
```

### Database Schema
```sql
CREATE TABLE users (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    username      TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,   -- BCrypt hash ONLY
    first_name    TEXT NOT NULL,
    last_name     TEXT NOT NULL,
    created_at    DATETIME DEFAULT (datetime('now'))
);

CREATE TABLE notes (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id    INTEGER NOT NULL,
    title      TEXT NOT NULL,
    body       TEXT NOT NULL,      -- AES-256 ciphertext ONLY
    category   TEXT DEFAULT 'Personal',
    created_at DATETIME DEFAULT (datetime('now')),
    updated_at DATETIME DEFAULT (datetime('now')),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## Features Implemented

| Feature | Status |
|---|---|
| User Registration | ✅ |
| BCrypt Password Hashing | ✅ |
| User Login | ✅ |
| AES-256 Note Encryption | ✅ |
| PBKDF2 Key Derivation | ✅ |
| SQLite Database | ✅ |
| Add / Edit / Delete Notes | ✅ |
| Search Notes by Keyword | ✅ |
| Categories (Work/Study/Personal) | ✅ |
| Created/Updated Timestamps | ✅ |
| Auto Logout (5 min inactivity) | ✅ |
| Password Strength Checker | ✅ |
| Swing GUI | ✅ |
| Multi-user Login System | ✅ |

---

## Java APIs Used

- `javax.crypto.*` — Java Cryptography Extension (JCE)
- `java.security.*` — Java Security API
- `java.sql.*` — JDBC for SQLite
- `javax.swing.*` — Swing GUI framework
- `java.awt.*` — AWT layout and events

---

## Author Notes

Plain-text passwords are NEVER written to disk.
Note bodies are encrypted BEFORE leaving memory.
The encryption key is derived per-session from the user's password and discarded on logout.
