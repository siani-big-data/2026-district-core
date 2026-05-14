package siani.districting.architecture.precinctinfo;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public interface PrecinctInfoContainer extends Serializable {

    void insert(String precinctId, String columnName, int value);

    Integer getValueOf(String precinctId, String columnName);

    boolean contains(String precinctId);

    Map<String, Integer> getInfoOf(String precinctId);

    Set<String> getAllPrecinctsIds();

    Set<String> getColumnNames();

}
