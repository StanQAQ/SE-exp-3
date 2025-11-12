package models;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;

public class Transaction {
	private String id;
	private LocalDate date;
	private double amount;
	private String category;
	private String type; // "expense" or "income"
    private LocalDateTime addedAt; // timestamp when transaction was added (minute precision)
	private String description;

	private static final DateTimeFormatter F = DateTimeFormatter.ISO_LOCAL_DATE;
	private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

	public Transaction() {}

	public Transaction(String id, LocalDate date, double amount, String category, String type, String description) {
		this.id = id;
		this.date = date;
		this.amount = amount;
		this.category = category;
		this.type = type;
		this.description = description;
		this.addedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
	}

	public Transaction(String id, LocalDate date, double amount, String category, String type, String description, LocalDateTime addedAt) {
		this.id = id;
		this.date = date;
		this.amount = amount;
		this.category = category;
		this.type = type;
		this.description = description;
		this.addedAt = addedAt != null ? addedAt : LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
	}

	public static Transaction fromMap(Map<String, String> m) {
		String id = m.getOrDefault("id", UUID.randomUUID().toString());
		LocalDate date = LocalDate.parse(m.getOrDefault("date", LocalDate.now().toString()));
		double amount = 0.0;
		try { amount = Double.parseDouble(m.getOrDefault("amount", "0")); } catch (Exception ignored) {}
		String category = m.getOrDefault("category", "other");
		String type = m.getOrDefault("type", "expense");
		String desc = m.getOrDefault("description", "");
		// addedAt may be provided (from client) as ISO minute string, else use now
		String added = m.get("addedAt");
		if (added != null && !added.isEmpty()) {
			try {
				LocalDateTime at = LocalDateTime.parse(added, TF);
				return new Transaction(id, date, amount, category, type, desc, at);
			} catch (Exception ignored) { }
		}
		return new Transaction(id, date, amount, category, type, desc);
	}

	public String toTSV() {
		String d = date.format(F);
		String desc = (description == null) ? "" : description.replace("\t", " ").replace("\n", " ");
		String at = (addedAt == null) ? "" : addedAt.format(TF);
		return String.join("\t", id, d, Double.toString(amount), category, type, desc, at);
	}

	public static Transaction fromTSV(String line) {
		String[] parts = line.split("\t", -1);
		if (parts.length < 6) return null;
		String id = parts[0];
		LocalDate d = LocalDate.parse(parts[1], F);
		double amount = 0.0;
		try { amount = Double.parseDouble(parts[2]); } catch (Exception ignored) {}
		String category = parts[3];
		String type = parts[4];
		String desc = parts[5];
		LocalDateTime at = null;
		if (parts.length >= 7 && parts[6] != null && !parts[6].isEmpty()) {
			try { at = LocalDateTime.parse(parts[6], TF); } catch (Exception ignored) {}
		}
		return new Transaction(id, d, amount, category, type, desc, at);
	}

	public String toJson() {
	String addedStr = (addedAt == null) ? "" : addedAt.format(TF);
	return String.format(Locale.ROOT,
		"{\"id\":\"%s\",\"date\":\"%s\",\"amount\":%s,\"category\":\"%s\",\"type\":\"%s\",\"description\":\"%s\",\"addedAt\":\"%s\"}",
		escape(id), date.format(F), Double.toString(amount), escape(category), escape(type), escape(description), escape(addedStr));
	}

	private String escape(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	// getters
	public String getId() { return id; }
	public LocalDate getDate() { return date; }
	public double getAmount() { return amount; }
	public String getCategory() { return category; }
	public String getType() { return type; }
	public String getDescription() { return description; }
	public LocalDateTime getAddedAt() { return addedAt; }
}
