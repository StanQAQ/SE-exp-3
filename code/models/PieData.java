package models;

public class PieData {
	private String label;
	private double value;

	public PieData() {}
	public PieData(String label, double value) { this.label = label; this.value = value; }

	public String toJson() {
		return String.format("{\"label\":\"%s\",\"value\":%s}", escape(label), Double.toString(value));
	}

	private String escape(String s) {
		if (s == null) return "";
		return s.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	public String getLabel() { return label; }
	public double getValue() { return value; }
}
