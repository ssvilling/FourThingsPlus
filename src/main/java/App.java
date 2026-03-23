import io.javalin.Javalin;
import java.util.ArrayList;
import java.util.List;
import io.javalin.http.staticfiles.Location;
import org.mindrot.jbcrypt.BCrypt;

public class App {

    public static void main(String[] args) {

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/public";
                staticFiles.location = Location.CLASSPATH;
            });
        }).start(7000);

        app.before(ctx -> checkSession(ctx));

        app.post("/login", ctx -> handleLogin(ctx));
        app.post("/signup", ctx -> handleSignup(ctx));
        app.get("/logout", ctx -> handleLogout(ctx));

        app.get("/me", ctx -> {
            String username = ctx.sessionAttribute("user");

            if (username == null) {
                ctx.status(401).result("Not logged in");
                return;
            }

            ctx.json(new UserResponse(username));
        });


        app.get("/notes", ctx -> {

            String username = ctx.sessionAttribute("user");

            if (username == null) {
                ctx.status(401);
                return;
            }

            List<Note> notes = new ArrayList<>();

            try (var conn = DB.getConnection()) {

                var stmt = conn.prepareStatement(
                        "SELECT id, content, status FROM notes WHERE username = ?"
                );

                stmt.setString(1, username);

                var rs = stmt.executeQuery();

                while (rs.next()) {
                    Note n = new Note();
                    n.id = rs.getInt("id");
                    n.content = rs.getString("content");
                    n.status = rs.getString("status");

                    notes.add(n);
                }

                ctx.json(notes);

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
            }
        });

        app.post("/notes", ctx -> {

            String username = ctx.sessionAttribute("user");

            if (username == null) {
                ctx.status(401);
                return;
            }

            Note note = ctx.bodyAsClass(Note.class);

            try (var conn = DB.getConnection()) {

                var stmt = conn.prepareStatement(
                        "INSERT INTO notes (username, content, status) VALUES (?, ?, ?)"
                );

                stmt.setString(1, username);
                stmt.setString(2, note.content);
                stmt.setString(3, "DOING");

                stmt.executeUpdate();

                ctx.result("Note saved");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
            }
        });

        app.post("/notes/status", ctx -> {

            String username = ctx.sessionAttribute("user");

            if (username == null) {
                ctx.status(401);
                return;
            }

            Note note = ctx.bodyAsClass(Note.class);

            try (var conn = DB.getConnection()) {

                var stmt = conn.prepareStatement(
                        "UPDATE notes SET status = ? WHERE id = ? AND username = ?"
                );

                stmt.setString(1, note.status);
                stmt.setInt(2, note.id);
                stmt.setString(3, username);

                stmt.executeUpdate();

                ctx.result("Updated");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
            }
        });

        app.post("/notes/delete", ctx -> {

            String username = ctx.sessionAttribute("user");

            if (username == null) {
                ctx.status(401);
                return;
            }

            Note note = ctx.bodyAsClass(Note.class);

            try (var conn = DB.getConnection()) {

                var stmt = conn.prepareStatement(
                        "DELETE FROM notes WHERE id = ? AND username = ?"
                );

                stmt.setInt(1, note.id);
                stmt.setString(2, username);

                stmt.executeUpdate();

                ctx.result("Deleted");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500);
            }
        });

    }

    private static void checkSession(io.javalin.http.Context ctx) {
        String user = ctx.sessionAttribute("user");
        String path = ctx.path();

        if (path.equals("/login.html") || path.equals("/signup.html") ||
                path.equals("/login") || path.equals("/signup") ||
                path.equals("/me") ||
                path.startsWith("/notes") ||
                path.endsWith(".js") || path.endsWith(".css") ||
                path.endsWith(".png") || path.endsWith(".jpg")) {
            return;
        }

        if (user == null) {
            ctx.redirect("/login.html");
        }
    }

    private static void handleLogin(io.javalin.http.Context ctx) {

        try {
            User loginUser = ctx.bodyAsClass(User.class);

            try (var conn = DB.getConnection()) {

                var stmt = conn.prepareStatement(
                        "SELECT password FROM users WHERE username = ?"
                );

                stmt.setString(1, loginUser.username);

                var rs = stmt.executeQuery();

                if (rs.next()) {
                    String hashedPassword = rs.getString("password");

                    if (org.mindrot.jbcrypt.BCrypt.checkpw(loginUser.password, hashedPassword)) {
                        ctx.sessionAttribute("user", loginUser.username);
                        ctx.result("Login success");
                    } else {
                        ctx.status(401).result("Forkert login");
                    }
                } else {
                    ctx.status(401).result("Forkert login");
                }

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Server error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(400).result("Invalid request");
        }
    }



    private static void handleSignup(io.javalin.http.Context ctx) {

        try {
            User newUser = ctx.bodyAsClass(User.class);

            if (newUser.username == null || newUser.username.isEmpty() ||
                    newUser.password == null || newUser.password.isEmpty()) {

                ctx.status(400).result("Ugyldigt input");
                return;
            }

            try (var conn = DB.getConnection()) {

                var checkStmt = conn.prepareStatement(
                        "SELECT 1 FROM users WHERE username = ?"
                );
                checkStmt.setString(1, newUser.username);

                var rs = checkStmt.executeQuery();

                if (rs.next()) {
                    ctx.status(400).result("Bruger findes allerede");
                    return;
                }

                String hashedPassword = BCrypt.hashpw(newUser.password, BCrypt.gensalt());


                var insertStmt = conn.prepareStatement(
                        "INSERT INTO users (username, password) VALUES (?, ?)"
                );

                insertStmt.setString(1, newUser.username);
                insertStmt.setString(2, hashedPassword);

                insertStmt.executeUpdate();

                System.out.println("Gemte bruger: " + newUser.username);
                ctx.result("Bruger oprettet");

            } catch (Exception e) {
                e.printStackTrace();
                ctx.status(500).result("Database error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            ctx.status(400).result("Invalid request");
        }
    }

    private static void handleLogout(io.javalin.http.Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.redirect("/login.html");
    }
}