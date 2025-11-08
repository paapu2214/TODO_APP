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

        // Start HTTP Server
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Serve HTML page
        server.createContext("/", exchange -> {
            File file = new File("todo.html"); // this file exists in project root
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

        server.createContext("/students", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    String q = "SELECT id, name, age, course, gender, email FROM students_todo ORDER BY id DESC ";
                    Statement st = con.createStatement();
                    ResultSet rs = st.executeQuery(q);
                    StringBuilder sb = new StringBuilder();

                    sb.append("[");
                    boolean first = true;

                    while (rs.next()) {
                                                
                        if (!first)  sb.append(",");
                        
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        int age = rs.getInt("age");
                        String course = rs.getString("course");
                        String gender = rs.getString("gender");
                        String email = rs.getString("email");

                        sb.append("{");
                        sb.append("\"id\":\"").append(id).append("\",");
                        sb.append("\"name\":\"").append(name).append("\",");
                        sb.append("\"age\":\"").append(age).append("\",");
                        sb.append("\"course\":\"").append(course).append("\",");
                        sb.append("\"gender\":\"").append(gender).append("\",");
                        sb.append("\"email\":\"").append(email).append("\"");
                        sb.append("}");

                        first = false;
                    }
                    sb.append("]");
                    String json = sb.toString();

                    byte[] out = json.getBytes();
                    exchange.sendResponseHeaders(200, out.length);
                    exchange.getResponseBody().write(out);
                    exchange.close();

                }
                // TODO- handle exception
                catch (Exception e) {

                    String error = "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
                    byte[] out = error.getBytes();
                    exchange.sendResponseHeaders(500, out.length);
                    exchange.getResponseBody().write(out);
                    exchange.close();

                }

            }

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

        server.createContext("/delete", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                try {
                    String query = exchange.getRequestURI().getQuery();
                    if (query != null && query.startsWith("id")) {
                        String id = query.split("=")[1];

                        String sql = "DELETE FROM students_todo Where id=?";
                        PreparedStatement ps = con.prepareStatement(sql);
                        ps.setInt(1, Integer.parseInt(id));
                        int rows = ps.executeUpdate();

                        String response = (rows > 0)
                                ? "Delete Successfully"
                                : "No record found";
                        exchange.sendResponseHeaders(200, response.length());
                        exchange.getResponseBody().write(response.getBytes());

                    } else {
                        String error = "Missing ID parameter!";
                        exchange.sendResponseHeaders(400, error.length());
                        exchange.getResponseBody().write(error.length());

                    }
                } catch (Exception e) {
                    // TODO: handle exception
                    String error = "Error deleting record:" + e.getMessage();
                    exchange.getResponseHeaders();
                    exchange.getResponseBody().write(error.getBytes());
                } finally {
                    exchange.close();
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