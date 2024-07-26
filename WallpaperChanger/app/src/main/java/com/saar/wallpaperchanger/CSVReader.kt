package com.saar.wallpaperchanger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CSVReader {

    private BufferedReader reader;
    private String[] header;

    private String delimiter;
    public CSVReader(Reader reader, String delimiter) {
        this.reader = new BufferedReader(reader);
        this.delimiter = delimiter;
        try {
            // Read the first line as the header
            String line = this.reader.readLine();
            if (line != null) {
                header = this.splitLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String[] splitLine(String line) {
        return line.split(Pattern.quote(this.delimiter));
    }
    public String[] getHeader() {
        return header;
    }

    public String[] readNext() {
        try {
            String line = reader.readLine();
            if (line != null) {
                return this.splitLine(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String[]> readAll() {
        List<String[]> rows = new ArrayList<>();
        String[] row;
        while ((row = readNext()) != null) {
            rows.add(row);
        }
        return rows;
    }

    public void close() {
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
