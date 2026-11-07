package app.phone.sms;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

final class HttpServer {
    static final int PORT = 8765;

    private final SmsRepository sms;
    private final byte[] token;
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private ServerSocket server;

    HttpServer(SmsRepository sms, String token) {
        this.sms = sms;
        this.token = token.getBytes(StandardCharsets.UTF_8);
    }

    void start() throws IOException {
        server = new ServerSocket();
        server.setReuseAddress(true);
        server.bind(new InetSocketAddress(PORT));
        new Thread(this::acceptLoop, "sms-http").start();
    }

    void stop() {
        try {
            server.close();
        } catch (IOException ignored) {
            server = null;
        }
        pool.shutdownNow();
    }

    private void acceptLoop() {
        while (!server.isClosed()) {
            try {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            } catch (IOException ignored) {
                return;
            }
        }
    }

    private void handle(Socket client) {
        try (Socket socket = client) {
            socket.setSoTimeout(10000);
            InputStream in = new BufferedInputStream(socket.getInputStream());
            String[] request = readLine(in).split(" ");
            Map<String, String> headers = readHeaders(in);
            int status;
            JSONObject response;
            if (request.length < 2) {
                status = 400;
                response = error("bad request");
            } else if (!authorized(headers.get("authorization"))) {
                status = 401;
                response = error("unauthorized");
            } else {
                response = respond(request[0], request[1]);
                status = response.optBoolean("ok", true) ? 200 : 500;
            }
            write(socket.getOutputStream(), status, response);
        } catch (Exception ignored) {
            return;
        }
    }

    private JSONObject respond(String method, String target) throws JSONException {
        try {
            return route(method, target);
        } catch (Exception exception) {
            return error(String.valueOf(exception.getMessage()));
        }
    }

    private JSONObject route(String method, String target) throws Exception {
        int split = target.indexOf('?');
        String path = split < 0 ? target : target.substring(0, split);
        Map<String, String> query = parseQuery(split < 0 ? "" : target.substring(split + 1));
        if (method.equals("GET") && path.equals("/ping")) {
            return new JSONObject().put("ok", true);
        }
        if (method.equals("GET") && path.equals("/messages")) {
            long since = Long.parseLong(value(query, "since", "0"));
            int limit = Integer.parseInt(value(query, "limit", "2000"));
            return new JSONObject().put("messages", sms.messages(since, limit));
        }
        return error("not found");
    }

    private boolean authorized(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            return false;
        }
        byte[] given = header.substring(7).trim().getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(given, token);
    }

    private static Map<String, String> readHeaders(InputStream in) throws IOException {
        Map<String, String> headers = new HashMap<>();
        String line;
        while (!(line = readLine(in)).isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                String name = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                headers.put(name, line.substring(colon + 1).trim());
            }
        }
        return headers;
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        int current;
        while ((current = in.read()) != -1 && current != '\n') {
            if (current != '\r') {
                line.write(current);
            }
            if (line.size() > 8192) {
                throw new IOException("line too long");
            }
        }
        return line.toString("UTF-8");
    }

    private static Map<String, String> parseQuery(String query)
            throws UnsupportedEncodingException {
        Map<String, String> values = new HashMap<>();
        for (String pair : query.split("&")) {
            int equals = pair.indexOf('=');
            if (equals > 0) {
                values.put(URLDecoder.decode(pair.substring(0, equals), "UTF-8"),
                        URLDecoder.decode(pair.substring(equals + 1), "UTF-8"));
            }
        }
        return values;
    }

    private static String value(Map<String, String> query, String key, String fallback) {
        String value = query.get(key);
        return value == null ? fallback : value;
    }

    private static JSONObject error(String message) throws JSONException {
        return new JSONObject().put("ok", false).put("error", message);
    }

    private static void write(OutputStream out, int status, JSONObject json)
            throws IOException {
        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        String head = "HTTP/1.1 " + status + " " + (status == 200 ? "OK" : "Error") + "\r\n"
                + "Content-Type: application/json; charset=utf-8\r\n"
                + "Content-Length: " + body.length + "\r\n"
                + "Connection: close\r\n\r\n";
        out.write(head.getBytes(StandardCharsets.US_ASCII));
        out.write(body);
        out.flush();
    }
}
