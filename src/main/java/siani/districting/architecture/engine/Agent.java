package siani.districting.architecture.engine;

import java.util.Map;
import java.util.Set;

public interface Agent {
    int id();

    Action choose(Set<Action> actions);
    Map<Action, Double> valueFunction(Set<Action> actions);
}
