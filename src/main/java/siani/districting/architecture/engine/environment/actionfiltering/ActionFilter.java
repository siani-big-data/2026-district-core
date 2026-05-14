package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.environment.actionfiltering.constraints.ConstraintCommand;
import siani.districting.architecture.model.State;

import java.util.*;

public class ActionFilter {

    private final Map<EpochName, Epoch> epochNameEpochMap;
    private final Map<Epoch, List<ConstraintCommand>> epochFiltersMap;

    public enum EpochName {
        EXPLORATIVE,
        TRANSITION,
        EXPLOITATIVE
    }

    public static class Epoch {

        private final double start;
        private final double end;

        public Epoch(double start, double end) {
            this.start = start;
            this.end = end;
        }

        public boolean contains(double value) {
            return start <= value && value <= end;
        }
    }

    private ActionFilter() {
        this.epochNameEpochMap = Map.of(
                EpochName.EXPLORATIVE, new Epoch(-1.0, -0.33),
                EpochName.TRANSITION, new Epoch(-0.34, 0.33),
                EpochName.EXPLOITATIVE, new Epoch(0.34, 1)
        );
        this.epochFiltersMap = new HashMap<>();
    }

    public static ActionFilter create() {
        return new ActionFilter();
    }

    public ActionFilter addConstraint(EpochName name, ConstraintCommand constraint) {
        List<ConstraintCommand> list = getContraintsList(name);
        list.add(constraint);
        return this;
    }

    private List<ConstraintCommand> getContraintsList(EpochName name) {
        Epoch epoch = epochNameEpochMap.get(name);
        return epochFiltersMap.computeIfAbsent(epoch, k -> new ArrayList<>());
    }

    public List<Action> filterActions(State state, AdjacencySolver solver, List<Action> actionList, int step) {
        Epoch epoch = getEpochFromCosine(step);
        List<ConstraintCommand> constraints = epochFiltersMap.get(epoch);
        if (constraints == null) return actionList;
        for (ConstraintCommand constraint : constraints) {
            actionList = constraint.filter(state,solver, actionList);
        }
        return actionList;
    }

    private Epoch getEpochFromCosine(int step) {
        double cos = Math.cos(step);
        for (Map.Entry<EpochName, Epoch> entry : epochNameEpochMap.entrySet()) {
            if (entry.getValue().contains(cos)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
