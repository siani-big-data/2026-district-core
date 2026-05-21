package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.environment.actionfiltering.constraints.ConstraintCommand;
import siani.districting.architecture.model.State;

import java.util.*;

public class ActionFilter {

    private final Map<EpochName, Epoch> epochNameEpochMap;
    private final Map<Epoch, List<ConstraintCommand>> epochFiltersMap;
    private double epochLengthFactor = 50;

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
                EpochName.EXPLORATIVE, new Epoch(-1.0, -0.34),
                EpochName.TRANSITION, new Epoch(-0.33, 0.33),
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

    public ActionFilter epochLengthFactor(double factor) {
        this.epochLengthFactor = factor;
        return this;
    }

    public double getEpochLengthFactor() {
        return this.epochLengthFactor;
    }

    private List<ConstraintCommand> getContraintsList(EpochName name) {
        Epoch epoch = epochNameEpochMap.get(name);
        return epochFiltersMap.computeIfAbsent(epoch, k -> new ArrayList<>());
    }


    public List<Action> filterActions(State state, AdjacencySolver solver, List<Action> actionList, int step) {
        EpochName epochName = getEpochNameFromStep(step);
        Epoch epoch = epochName != null ? epochNameEpochMap.get(epochName) : null;
        List<ConstraintCommand> constraints = epoch != null ? epochFiltersMap.get(epoch) : null;
        if (constraints == null) return actionList;
        for (ConstraintCommand constraint : constraints) {
            actionList = constraint.filter(state,solver, actionList);
        }
        return actionList;
    }

    private double cosineForStep(int step) {
        return Math.round((Math.cos(step / epochLengthFactor) * 100d)) / 100d;
    }

    public EpochName getEpochNameFromStep(int step) {
        for (Map.Entry<EpochName, Epoch> entry : epochNameEpochMap.entrySet()) {
            if (entry.getValue().contains(cosineForStep(step))) {
                return entry.getKey();
            }
        }
        return null;
    }
}
