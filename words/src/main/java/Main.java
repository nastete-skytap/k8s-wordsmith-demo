import com.google.common.base.Charsets;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.NoSuchElementException;

public class Main {
    private static final String HOST = "192.168.0.20";
    private static final int PORT = 50000;
    private static final String DATABASE = "test";
    private static final String USERNAME = "db2inst1";
    private static final String PASSWORD = "test";

    public static void main(String[] args) throws Exception {
        System.out.println("Detecting DB2 driver...");
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        System.out.println("Detecting DB2 driver...[OK]");

        System.out.println("Going to print some random words from DB2");
        System.out.println("Random noun from DB2: " + randomWord("nouns"));
        System.out.println("Random verb from DB2: " + randomWord("verbs"));
        System.out.println("Random adjective from DB2: " + randomWord("adjectives"));

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/noun", handler(Suppliers.memoize(() -> randomWord("nouns"))));
        server.createContext("/verb", handler(Suppliers.memoize(() -> randomWord("verbs"))));
        server.createContext("/adjective", handler(Suppliers.memoize(() -> randomWord("adjectives"))));
        server.start();
    }

    private static String randomWord(String table) {
        try (Connection connection = DriverManager.getConnection("jdbc:db2://" + HOST + ":" + String.valueOf(PORT) + "/" + DATABASE, USERNAME, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                try (ResultSet set = statement.executeQuery("SELECT word FROM " + table + " ORDER BY random() LIMIT 1")) {
                    while (set.next()) {
                        return set.getString(1);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        throw new NoSuchElementException(table);
    }

    private static HttpHandler handler(Supplier<String> word) {
        return t -> {
            String response = "{\"word\":\"" + word.get() + "\"}";
            byte[] bytes = response.getBytes(Charsets.UTF_8);

            System.out.println(response);
            t.getResponseHeaders().add("content-type", "application/json; charset=utf-8");
            t.sendResponseHeaders(200, bytes.length);

            try (OutputStream os = t.getResponseBody()) {
                os.write(bytes);
            }
        };
    }
}
