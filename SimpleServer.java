import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SimpleServer {

    static Map<String, String> tokenToCode = new ConcurrentHashMap<>();
    static Map<String, Instant> tokenExpiry = new ConcurrentHashMap<>();
    static Map<String, List<Object>> tokenSolutions = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(3000), 0);

        server.createContext("/apichallenge", new ChallengeHandler());
        server.createContext("/apiredeem", new RedeemHandler());
        server.createContext("/api/verify", new VerifyHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("✅ Server CAP avviato su http://localhost:3000");
    }

    static class ChallengeHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String token = generateToken();
            String code = generateToken().substring(0, 8);
            Instant expires = Instant.now().plusSeconds(180); // 3 minuti

            tokenToCode.put(token, code);
            tokenExpiry.put(token, expires);
            tokenSolutions.put(token, new ArrayList<>()); // placeholder

            List<List<Integer>> dummyChallenge = new ArrayList<>();
            dummyChallenge.add(Arrays.asList(1, 2));
            dummyChallenge.add(Arrays.asList(3, 4));

            String responseJson = String.format(
                "{\"challenge\":%s,\"token\":\"%s\",\"expires\":%d}",
                toJsonArray(dummyChallenge), token, expires.toEpochMilli()
            );

            sendJson(exchange, 200, responseJson);
            System.out.println("🔐 Challenge inviato per token: " + token);
        }
    }

    static class RedeemHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readBody(exchange.getRequestBody());
            String token = extractField(body, "token");
            boolean isValid = tokenToCode.containsKey(token) && Instant.now().isBefore(tokenExpiry.get(token));

            if (isValid) {
                tokenToCode.remove(token);
                tokenExpiry.remove(token);
                tokenSolutions.remove(token);
            }

            String response = String.format(
                "{\"success\":%s,\"message\":\"%s\"}",
                isValid, isValid ? "CAP validato con successo" : "Token non valido o scaduto"
            );

            sendJson(exchange, 200, response);
            System.out.println("🎟️ Redeem per token: " + token + " → " + isValid);
        }
    }

    static class VerifyHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String body = readBody(exchange.getRequestBody());
            String code = extractField(body, "code");

            boolean isValid = tokenToCode.containsValue(code);
            String json = String.format("{\"valid\":%s}", isValid);

            sendJson(exchange, 200, json);
            System.out.println("🛡️ Verifica code \"" + code + "\": " + isValid);
        }
    }

    // 🔧 Utility
    private static String readBody(InputStream is) {
        Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static String extractField(String json, String field) {
        int idx = json.indexOf(field);
        if (idx == -1) return "";
        int start = json.indexOf(":", idx) + 1;
        int quote1 = json.indexOf("\"", start);
        int quote2 = json.indexOf("\"", quote1 + 1);
        return json.substring(quote1 + 1, quote2);
    }

    private static void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static String toJsonArray(List<List<Integer>> matrix) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < matrix.size(); i++) {
            sb.append(matrix.get(i).toString());
            if (i < matrix.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}