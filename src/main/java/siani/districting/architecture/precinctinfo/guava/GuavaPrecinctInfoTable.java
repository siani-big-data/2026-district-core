package siani.districting.architecture.precinctinfo.guava;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;

import java.util.Map;
import java.util.Set;

public class GuavaPrecinctInfoTable implements PrecinctInfoContainer {

    private final Table<String, String, Integer> table;

    public GuavaPrecinctInfoTable() {
        this.table = HashBasedTable.create();
    }

    @Override
    public void insert(String precinctId, String columnName, int value) {
        table.put(precinctId, columnName, value);
    }

    @Override
    public Integer getValueOf(String precinctId, String columnName) {
        return table.get(precinctId, columnName);
    }

    public Integer getValueOf(Precinct precinct, String columnName) {
        return getValueOf(precinct.id(), columnName);
    }

    @Override
    public boolean contains(String precinctId) {
        return table.containsRow(precinctId);
    }

    @Override
    public Map<String, Integer> getInfoOf(String precinctId) {
        return table.row(precinctId);
    }

    @Override
    public Set<String> getAllPrecinctsIds() {
        return table.rowKeySet();
    }

    @Override
    public Set<String> getColumnNames() {
        return table.columnKeySet();
    }
}
