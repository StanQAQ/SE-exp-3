package models;

public class TimeSeriesData {
	private String period; // e.g., 2025-11
	private double total;

	public TimeSeriesData() {}
	public TimeSeriesData(String period, double total) { this.period = period; this.total = total; }

	public String toJson() {
		return String.format("{\"period\":\"%s\",\"total\":%s}", escape(period), Double.toString(total));
	}

	private String escape(String s) { if (s==null) return ""; return s.replace("\\", "\\\\").replace("\"", "\\\""); }

	public String getPeriod() { return period; }
	public double getTotal() { return total; }
}
