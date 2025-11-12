package repository;

import models.Transaction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class LocalDB {
	private final Path file;

	public LocalDB() {
		this.file = new File("transactions.tsv").toPath();
		try {
			if (!Files.exists(file)) Files.createFile(file);
		} catch (IOException ignored) {}
	}

	public synchronized List<Transaction> loadAll() {
		List<Transaction> out = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.trim().isEmpty()) continue;
				Transaction t = Transaction.fromTSV(line);
				if (t != null) out.add(t);
			}
		} catch (IOException ignored) {}
		return out;
	}

	public synchronized void saveAll(List<Transaction> list) {
		try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			for (Transaction t : list) {
				bw.write(t.toTSV()); bw.newLine();
			}
		} catch (IOException ignored) {}
	}

	public synchronized void append(Transaction t) {
		try (BufferedWriter bw = Files.newBufferedWriter(file, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.APPEND)) {
			bw.write(t.toTSV()); bw.newLine();
		} catch (IOException ignored) {}
	}
}
