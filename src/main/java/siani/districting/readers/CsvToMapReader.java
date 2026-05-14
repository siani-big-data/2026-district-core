package siani.districting.readers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CsvToMapReader {

    private CsvToMapReader() {
    }

    public static Map<Object, Object> read(String filePath, boolean skipHeader) throws IOException {
        Path path = Path.of(filePath);
        try (Stream<String> lines = Files.lines(path)) {
            return lines
                    .skip(skipHeader ? 1 : 0)
                    .map(line -> line.split(","))
                    .filter(columns -> columns.length >= 2)
                    .collect(Collectors.toMap(
                            columns -> parseValue(columns[0].trim()),
                            columns -> parseValue(columns[1].trim())
                    ));
        }
    }

    private static Object parseValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return value;
        }
    }
}
