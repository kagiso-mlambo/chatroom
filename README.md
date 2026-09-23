# Secure Chat Application

A terminal based, real time, multi client chatroom built from scratch in Java. It uses raw TCP sockets and one thread per client for networking and concurrency, with no chat or web framework involved. Built as a Cyber Security elective project, so alongside the chat functionality it demonstrates transport encryption, credential hashing, session integrity, authorization, and abuse prevention implemented directly rather than through a library.

## Demo

*(Add your demo video link here once recorded.)*

## Architecture

The codebase is split into four packages, each with a single responsibility:

| Package | Class | Responsibility |
|---|---|---|
| `server` | `Server` | Owns the client and channel maps, broadcasting, and all server side state |
| `server` | `ClientHandler` | One instance per connected client. Reads messages off the socket and calls `Server` methods |
| `server` | `CommandProcessor` | Parses and delegates all `/command` input |
| `server` | `Channel` | Represents a channel: name, creator, member list |
| `server` | `TokenBucket` | Tracks a refillable token count used for rate limiting |
| `client` | `Client` | Entry point for a user. Connects to the server and handles keyboard input |
| `client` | `ListenForMessages` | Daemon thread that receives and prints incoming messages |
| `common` | `ANSICodes` | Single source of truth for terminal formatting (bold, italics, colour) |
| `common` | `Request` / `Response` | Shared JSON message shapes exchanged between client and server |
| `database` | `DatabaseConnection` | Returns a JDBC connection to `chatroom.db` |
| `database` | `DatabaseInitialiser` | Creates all four tables on server startup if they don't already exist |
| `database` | `UserRepository` | Signup, login, and user lookups |
| `database` | `ChannelRepository` | Channel creation, lookup, and deletion |
| `database` | `ChannelMembersRepository` | Tracks who belongs to which channel |
| `database` | `MessagesRepository` | Persists messages and retrieves recent history |

Each client connection runs on its own thread (`ClientHandler`), and the server holds shared state (connected clients, channels, per user login rate limits) in `ConcurrentHashMap`s so that concurrent access from multiple client threads is safe.

## Features

- Multi client, real time messaging over TCP
- Named group channels: `/create`, `/join`, `/delete`, `/groups`
- Private one to one messaging: send `/<username>` to open or continue a DM
- `/users` to list other users, `/quit` to disconnect
- Chat history: the last messages in a channel are replayed to a client when they join or open a DM
- ANSI formatted output (bold, italics, colour) for a more readable terminal UI

## Security

This project treats security as a first class feature rather than an afterthought. Each control below maps to a specific risk in a hand rolled chat protocol.

**Transport encryption (TLS).** All client to server traffic runs over `SSLSocket` / `SSLServerSocket` rather than plain sockets, the same layer HTTPS is built on. Without this, usernames, passwords, and every chat message would be readable to anyone who could observe the network (for example on shared Wi-Fi). A self signed certificate is used, which is appropriate for a local or portfolio project; a public facing deployment would use a certificate from a trusted CA instead.

**Password hashing (BCrypt).** Passwords are never stored or compared in plaintext. `UserRepository` hashes passwords with `BCrypt.hashpw()` at signup and verifies attempts with `BCrypt.checkpw()` at login. BCrypt is used specifically because it has an adaptive cost factor and a built in salt, which makes it far more resistant to brute forcing and rainbow table attacks than a bare hash like MD5 or SHA-256.

**Session tokens.** After a successful login, the server generates a random session ID and binds it to that connection's `ClientHandler`. Every later request must include a valid session ID or it is rejected before it's processed. This closes off a spoofing risk that would otherwise exist if the server simply trusted whatever username a client claimed on each message.

**Authorization checks.** A channel can only be deleted by the user who created it (`ChannelRepository.checkCreator`), and a message can only be broadcast to a channel by a user who is actually a member of it (`ChannelMembersRepository.doesMemberExistInChannel`). Authentication (who you are) and authorization (what you're allowed to do) are treated as separate checks.

**Rate limiting.** Both login attempts and outgoing messages are throttled using a token bucket: each user has a small pool of tokens that refill gradually over time, and an action is only allowed if a token is available. Login attempts are limited per username (not per connection) so that reconnecting doesn't reset the limit, which directly protects against brute force password guessing. Message sending is limited per connection to prevent flooding.

**No hardcoded secrets.** The keystore and truststore passwords are read from environment variables (`KEYSTORE_PASSWORD` and `TRUSTSTORE_PASSWORD`) rather than being committed in source, and the generated `.p12` certificate files are excluded from version control via `.gitignore`.

**Parameterised queries.** All database access goes through `PreparedStatement`, so user supplied input is never concatenated directly into SQL, which prevents SQL injection.

## How to Run

### Prerequisites

- Java 25 (or update `maven.compiler.source` / `target` in `pom.xml` to match your JDK)
- Maven
- `keytool` (ships with the JDK)

### 1. Generate a certificate and keystore

From the project root, generate a self signed certificate for the server:

```bash
keytool -genkeypair -alias chatserver -keyalg RSA -keysize 2048 \
  -validity 365 -keystore src/main/resources/server.p12 \
  -storetype PKCS12
```

You'll be prompted to set a password. Use the same value you plan to export as `KEYSTORE_PASSWORD` below.

Export the public certificate so the client can trust it:

```bash
keytool -exportcert -alias chatserver \
  -keystore src/main/resources/server.p12 -storetype PKCS12 \
  -file src/main/resources/server.cert
```

Import that certificate into a client side truststore:

```bash
keytool -importcert -alias chatserver -noprompt \
  -file src/main/resources/server.cert \
  -keystore src/main/resources/client-truststore.p12 \
  -storetype PKCS12
```

You'll be prompted to set a truststore password. This can be the same value or different from the keystore password. Use whatever you set here as `TRUSTSTORE_PASSWORD` below.

### 2. Set the required environment variables

```bash
export KEYSTORE_PASSWORD=your_keystore_password
export TRUSTSTORE_PASSWORD=your_truststore_password
```

Set these once in your shell's profile (`~/.zshrc` / `~/.bashrc`) so you don't need to re-export them every session, or configure them under your IDE's run configuration.

### 3. Build

```bash
mvn clean package
```

This produces two runnable jars under `target/`: `ChatRoom-Server.jar` and `ChatRoom-Client.jar`.

### 4. Run

Start the server first:

```bash
java -jar target/ChatRoom-Server.jar
```

Then start one or more clients in separate terminals:

```bash
java -jar target/ChatRoom-Client.jar
```

Follow the prompts to sign up or log in, then use the in chat commands (`/create`, `/join`, `/groups`, `/users`, `/quit`, or `/<username>` for a private message).

## Database Schema

SQLite is used via raw JDBC (no ORM), with four tables:

**users**
- `id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `username` : TEXT UNIQUE NOT NULL
- `password_hash` : TEXT NOT NULL
- `joined_at` : TIMESTAMP DEFAULT CURRENT_TIMESTAMP

**channels**
- `id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `name` : TEXT UNIQUE NOT NULL
- `type` : TEXT NOT NULL
- `creator` : TEXT
- `created_at` : TIMESTAMP DEFAULT CURRENT_TIMESTAMP

**channel_members**
- `channel_id` : INTEGER NOT NULL, FK to channels.id
- `user_id` : INTEGER NOT NULL, FK to users.id
- `joined_at` : TIMESTAMP DEFAULT CURRENT_TIMESTAMP

**messages**
- `id` : INTEGER PRIMARY KEY AUTOINCREMENT
- `channel_id` : INTEGER NOT NULL, FK to channels.id
- `user_id` : INTEGER NOT NULL, FK to users.id
- `content` : TEXT NOT NULL
- `sent_at` : TIMESTAMP DEFAULT CURRENT_TIMESTAMP

## Known Limitations

- The TLS certificate is self signed, so it is trusted only because the client's truststore explicitly imports it. A public deployment would use a CA issued certificate instead.
- Session tokens don't expire or rotate; a token is valid for the lifetime of the connection it was issued on.
- There is no password complexity policy on signup beyond BCrypt's own handling of arbitrary length input.
- Rate limiter state is held in memory and resets if the server restarts.
- No automated test suite yet.

## Technologies Used

Java, SQLite (via JDBC), org.json, jBCrypt, Java SSL/TLS (`javax.net.ssl`), Google Guava (`Multimap`), Maven.
