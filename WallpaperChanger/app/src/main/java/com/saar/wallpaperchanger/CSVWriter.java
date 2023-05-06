package com.saar.wallpaperchanger;

import java.io.IOException;
import java.io.Writer;

public class CSVWriter {
    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static final String DEFAULT_LINE_END = "\n";

    private Writer writer;
    private char separator;
    private char quote;
    private String lineEnd;

    public CSVWriter(Writer writer, char separator, char quote, String lineEnd) {
        this.writer = writer;
        this.separator = separator;
        this.quote = quote;
        this.lineEnd = lineEnd;
    }

    public void writeNext(String[] values) throws IOException {
        if (values == null || values.length == 0) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(separator);
            }
            if (values[i] != null) {
                if (values[i].contains(String.valueOf(quote))) {
                    sb.append(quote).append(values[i].replace(String.valueOf(quote), String.valueOf(quote) + String.valueOf(quote))).append(quote);
                } else if (values[i].contains(String.valueOf(separator)) || values[i].contains("\n")) {
                    sb.append(quote).append(values[i]).append(quote);
                } else {
                    sb.append(values[i]);
                }
            }
        }
        sb.append(lineEnd);
        writer.write(sb.toString());
    }

    public void close() throws IOException {
        writer.close();
    }
}
