import com.sun.net.httpserver.*;
import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class TodoServer {

    static Connection con;

    public static void main(String[] args) throws Exception {

        // Connect MySQL
        Class.forName("com.mysql.cj.jdbc.Driver");
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/tododb",
                "root",
                "paapu2214@" // your MySQL password
        );
        System.out.println("Connected to MySQL");

        //  Start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve HTML page
        server.createContext("/", exchange -> {
            File file = new File("todo.html"); //  this file exists in project root
            if (!file.exists()) {
                String notFound = "todo.html not found!";
                exchange.sendResponseHeaders(404, notFound.length());
                exchange.getResponseBody().write(notFound.getBytes());
            } else {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
            exchange.close();
        });

       // Handle form submission
server.createContext("/save", exchange -> {
    if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), "UTF-8"));
            String formData = br.readLine();
            Map<String, String> params = parseFormData(formData);

            String sql = "INSERT INTO students_todo (name, age, gender, course, email) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, params.get("name"));
            ps.setInt(2, Integer.parseInt(params.get("age")));
            ps.setString(3, params.get("gender"));
            ps.setString(4, params.get("course"));
            ps.setString(5, params.get("email"));
            ps.executeUpdate();

            String response = " Data saved successfully!";
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
            String error = "Error saving to DB: " + e.getMessage();
            exchange.sendResponseHeaders(500, error.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(error.getBytes());
            os.close();
        }
    }
});


        server.start();
        System.out.println("Server running at http://localhost:8080/");
    }

    static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        for (String pair : formData.split("&")) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8"));
            }
        }
        return map;
    }
}