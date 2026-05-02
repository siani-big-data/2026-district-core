package siani.districting.architecture.model;

import java.io.Serializable;
import java.util.List;

public record District(Integer uniqueId, List<Precinct> precinctList) implements Serializable {
}
