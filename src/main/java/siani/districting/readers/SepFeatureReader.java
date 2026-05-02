package siani.districting.readers;

import java.io.*;
import java.util.*;

public class SepFeatureReader {
    private final String sep;

    private SepFeatureReader(String sep) {
        this.sep = sep;
    }

    public List<Row> read(String path) {
        List<Row> fetchedRowsList = new ArrayList<>();
        try (FileInputStream is = new FileInputStream(path)) {
            return read(is, fetchedRowsList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Row> read(FileInputStream is, List<Row> fetchedRowsList) throws IOException {
        return read(new InputStreamReader(is), fetchedRowsList);
    }

    private List<Row> read(InputStreamReader inputStreamReader, List<Row> fetchedRowsList) throws IOException {
        return read(new BufferedReader(inputStreamReader), fetchedRowsList);
    }

    private List<Row> read(BufferedReader bufferedReader, List<Row> fetchedRowsList) throws IOException {
        List<String> headers = Arrays.stream(bufferedReader.readLine().trim().split(sep)).toList();
        fetchedRowsList.add(new Row(headers));
        bufferedReader.lines().forEach(l -> createDatatableRow(fetchedRowsList, l, headers));
        return fetchedRowsList;
    }

    private void createDatatableRow(List<Row> fetchedRowsList, String l, List<String> headers) {
        String[] rowComponents = l.trim().split(sep);
        List<String> valuesList = new ArrayList<>();
        for (int i = 0; i < headers.size(); i++) {
            valuesList.add(rowComponents[i]);
            fetchedRowsList.add(new Row(valuesList));
        }
    }

    public record Table(int year, Header header, List<Row> rows) {

    }

    public record Header(List<String> featureNames) {

        public int indexOf(String feature) {
            return featureNames.indexOf(feature);
        }
    }

    public record Row(List<String> values) {

        public String[] valuesAsArray() {
            return (String[]) values.stream().toArray();
        }

        public String get(int index) {
            return this.values.get(index);
        }

    }

}
