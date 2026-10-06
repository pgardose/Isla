package isla.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/** JDBC connection settings plus schema bootstrap. One instance is shared by the JDBC DAOs. */
public class Database {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    /** Reads url, user and password from a properties file (see db.properties.example). */
    public static Database fromProperties(Path file) {
        Properties props = new Properties();
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            props.load(in);
        } catch (IOException e) {
            throw new DataAccessException("Cannot read " + file, e);
        }
        String url = props.getProperty("url");
        if (url == null || url.isBlank()) {
            throw new DataAccessException("Missing 'url' in " + file, null);
        }
        return new Database(url, props.getProperty("user", ""), props.getProperty("password", ""));
    }

    public Connection connection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Creates the tables if they do not exist (runs db/schema.sql from the classpath). */
    public void initializeSchema() {
        String script;
        try (InputStream in = Database.class.getResourceAsStream("/db/schema.sql")) {
            if (in == null) {
                throw new DataAccessException("db/schema.sql not found on the classpath", null);
            }
            script = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Cannot read db/schema.sql", e);
        }
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\\R")) {
            int comment = line.indexOf("--");
            cleaned.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        try (Connection c = connection(); Statement st = c.createStatement()) {
            for (String sql : cleaned.toString().split(";")) {
                if (!sql.isBlank()) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Could not initialize the database schema", e);
        }
    }
}
