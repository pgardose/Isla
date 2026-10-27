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

    /**
     * The built-in database: a local H2 database stored in files inside the given folder
     * (created if missing), so saving works with nothing installed. MODE=MySQL lets it run the
     * same schema and SQL as MySQL. DB_CLOSE_DELAY=-1 keeps it open for the whole run instead of
     * reopening the files on every query; call shutdown() when the program ends.
     */
    public static Database embedded(Path folder) {
        try {
            Files.createDirectories(folder);
        } catch (IOException e) {
            throw new DataAccessException("Cannot create the data folder " + folder, e);
        }
        String base = folder.toAbsolutePath().toString().replace('\\', '/');
        return new Database("jdbc:h2:file:" + base + "/isla;MODE=MySQL;DB_CLOSE_DELAY=-1", "", "");
    }

    /** Closes an embedded database cleanly (flushes its files). Does nothing for a MySQL server. */
    public void shutdown() {
        if (!url.startsWith("jdbc:h2:")) {
            return;
        }
        try (Connection c = connection(); Statement st = c.createStatement()) {
            st.execute("SHUTDOWN");
        } catch (SQLException e) {
            System.err.println("Could not close the database cleanly: " + e.getMessage());
        }
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
