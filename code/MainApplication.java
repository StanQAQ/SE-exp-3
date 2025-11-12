import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import repository.TransactionRepository;
import repository.AnalyticsService;
import models.PieData;
import models.TimeSeriesData;
import models.Transaction;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
 

public class MainApplication {
	private static final int PORT = 8080;

	public static void main(String[] args) throws Exception {
		TransactionRepository repo = new TransactionRepository();
		AnalyticsService analytics = new AnalyticsService(repo);

		HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

	// Serve root and pages
	server.createContext("/", new FileHandler("index.html"));
	server.createContext("/add.html", new FileHandler("add.html"));
	server.createContext("/query.html", new FileHandler("query.html"));
	server.createContext("/charts.html", new FileHandler("charts.html"));

		// API endpoints
		server.createContext("/api/transactions", new TransactionsHandler(repo));
		server.createContext("/api/analytics/pie", new PieHandler(analytics));
		server.createContext("/api/analytics/series", new SeriesHandler(analytics));

		// static fallback for any other files in the same folder
		server.createContext("/static", new StaticHandler(new File(".") ));

		server.setExecutor(null);
		server.start();
	System.out.println("Server started at http://localhost:" + PORT + "/ (opening /index.html)");
	openBrowser("http://localhost:" + PORT + "/index.html");
	}

	private static void openBrowser(String url) {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (IOException | URISyntaxException e) {
                System.err.println("Failed to open browser: " + e.getMessage());
            }
        } else {
            System.err.println("Desktop not supported. Please open this URL manually: " + url);
        }
    }

	// Handler to serve a single file
	static class FileHandler implements HttpHandler {
		private final File file;

		FileHandler(String path) {
			this.file = new File(path);
		}

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			if (!file.exists()) {
				String notFound = "404 - file not found";
				exchange.sendResponseHeaders(404, notFound.length());
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(notFound.getBytes());
				}
				return;
			}
			Headers h = exchange.getResponseHeaders();
			String mime = URLConnection.guessContentTypeFromName(file.getName());
			if (mime == null) mime = "text/html; charset=utf-8";
			h.set("Content-Type", mime);
			byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	// Static directory handler (serves files under the folder)
	static class StaticHandler implements HttpHandler {
		private final File baseDir;

		StaticHandler(File baseDir) { this.baseDir = baseDir; }

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String uri = exchange.getRequestURI().getPath();
			String rel = uri.replaceFirst("/static/?", "");
			File f = new File(baseDir, rel);
			if (!f.exists() || f.isDirectory()) {
				exchange.sendResponseHeaders(404, -1);
				return;
			}
			String mime = URLConnection.guessContentTypeFromName(f.getName());
			if (mime == null) mime = "application/octet-stream";
			exchange.getResponseHeaders().set("Content-Type", mime);
			byte[] bytes = java.nio.file.Files.readAllBytes(f.toPath());
			exchange.sendResponseHeaders(200, bytes.length);
			try (OutputStream os = exchange.getResponseBody()) {
				os.write(bytes);
			}
		}
	}

	// /api/transactions (GET -> list, POST -> add form data)
	static class TransactionsHandler implements HttpHandler {
		private final TransactionRepository repo;

		TransactionsHandler(TransactionRepository repo) { this.repo = repo; }

		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String method = exchange.getRequestMethod();
			if ("GET".equalsIgnoreCase(method)) {
					List<Transaction> all = repo.listTransactions();
					// support query params: start, end (ISO date), type (expense/income)
					String query = exchange.getRequestURI().getQuery();
					if (query != null && !query.isEmpty()) {
						try {
							Map<String,String> q = parseForm(query);
							models.TransactionFilter filter = new models.TransactionFilter();
							if (q.containsKey("start")) {
								filter.setStartDate(LocalDate.parse(q.get("start")));
							}
							if (q.containsKey("end")) {
								filter.setEndDate(LocalDate.parse(q.get("end")));
							}
							if (q.containsKey("type") && !q.get("type").isEmpty()) {
								filter.setType(q.get("type"));
							}

							// amount range: min, max
							if (q.containsKey("min") && !q.get("min").isEmpty()) {
								try { filter.setMinAmount(Double.parseDouble(q.get("min"))); } catch (Exception ignored) {}
							}
							if (q.containsKey("max") && !q.get("max").isEmpty()) {
								try { filter.setMaxAmount(Double.parseDouble(q.get("max"))); } catch (Exception ignored) {}
							}
							all = all.stream().filter(filter::matches).collect(Collectors.toList());
						} catch (Exception ex) {
							// ignore parse errors and return unfiltered
						}
					}
					String json = "[" + all.stream().map(Transaction::toJson).collect(Collectors.joining(",")) + "]";
				byte[] resp = json.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(200, resp.length);
				try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
				return;
			} else if ("POST".equalsIgnoreCase(method)) {
				// read form data (application/x-www-form-urlencoded)
				String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				Map<String, String> params = parseForm(body);
				Transaction t = Transaction.fromMap(params);
				repo.addTransaction(t);
				String respStr = "{\"status\":\"ok\"}";
				byte[] resp = respStr.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(200, resp.length);
				try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
				return;
			} else if ("DELETE".equalsIgnoreCase(method)) {
				// expect id in query string
				String q = exchange.getRequestURI().getQuery();
				Map<String,String> params = parseForm(q == null ? "" : q);
				String id = params.get("id");
				if (id == null || id.isEmpty()) {
					exchange.sendResponseHeaders(400, -1);
					return;
				}
				boolean ok = repo.deleteTransaction(id);
				String respStr = ok ? "{\"status\":\"ok\"}" : "{\"status\":\"not_found\"}";
				byte[] resp = respStr.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
				exchange.sendResponseHeaders(ok ? 200 : 404, resp.length);
				try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
				return;
			}
			exchange.sendResponseHeaders(405, -1);
		}
	}

	static Map<String,String> parseForm(String body) throws UnsupportedEncodingException {
		Map<String,String> map = new HashMap<>();
		if (body == null || body.isEmpty()) return map;
		String[] pairs = body.split("&");
		for (String p : pairs) {
			int idx = p.indexOf('=');
			if (idx>=0) {
				String k = URLDecoder.decode(p.substring(0, idx), "UTF-8");
				String v = URLDecoder.decode(p.substring(idx+1), "UTF-8");
				map.put(k, v);
			}
		}
		return map;
	}

	static class PieHandler implements HttpHandler {
		private final AnalyticsService analytics;
		PieHandler(AnalyticsService a) { this.analytics = a; }
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			List<PieData> pie = analytics.pieByCategory();
			String json = "[" + pie.stream().map(PieData::toJson).collect(Collectors.joining(",")) + "]";
			byte[] resp = json.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(200, resp.length);
			try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
		}
	}

	static class SeriesHandler implements HttpHandler {
		private final AnalyticsService analytics;
		SeriesHandler(AnalyticsService a) { this.analytics = a; }
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			List<TimeSeriesData> series = analytics.timeSeriesMonthly();
			String json = "[" + series.stream().map(TimeSeriesData::toJson).collect(Collectors.joining(",")) + "]";
			byte[] resp = json.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
			exchange.sendResponseHeaders(200, resp.length);
			try (OutputStream os = exchange.getResponseBody()) { os.write(resp); }
		}
	}
}
