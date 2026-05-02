package siani.districting.architecture.stores;

import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.Precinct;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record StateDelta(Map<Precinct, Integer> differencies) implements Serializable {
    public StateDelta(List<Action> actions) {
        this(actions.stream().collect(Collectors.toMap(
                Action::precinct,
                Action::districtId,
                (existing, replacement) -> replacement)));
    }
}
