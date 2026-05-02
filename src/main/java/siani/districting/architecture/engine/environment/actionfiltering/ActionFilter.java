package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.State;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ActionFilter {

    Set<Constraint> constraints = new HashSet<>();

    private ActionFilter() {}

    public static ActionFilter create() {
        return new ActionFilter();
    }

    public ActionFilter addConstraint(Constraint constraint) {
        constraints.add(constraint);
        return this;
    }

    public List<Action> filterActions(State state, AdjacencySolver solver, List<Action> actionList, int simulationStep) {

        for (Constraint constraint : constraints) {
            actionList = constraint.filter(state, solver, actionList, simulationStep);
        }

        return actionList;
    }
}
