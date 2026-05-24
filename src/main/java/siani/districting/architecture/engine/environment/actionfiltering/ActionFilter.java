package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.environment.actionfiltering.constraints.ConstraintCommand;
import siani.districting.architecture.model.State;

import java.util.*;

public class ActionFilter {

    private final Map<PhaseName, Phase> PhaseNamePhaseMap;
    private final Map<Phase, List<ConstraintCommand>> PhaseFiltersMap;
    private double phaseLengthFactor = 50;

    public enum PhaseName {
        EXPLORATIVE,
        TRANSITION,
        EXPLOITATIVE
    }

    public static class Phase {

        private final double start;
        private final double end;

        public Phase(double start, double end) {
            this.start = start;
            this.end = end;
        }

        public boolean contains(double value) {
            return start <= value && value <= end;
        }
    }

    private ActionFilter() {
        this.PhaseNamePhaseMap = Map.of(
                PhaseName.EXPLORATIVE, new Phase(-1.0, -0.34),
                PhaseName.TRANSITION, new Phase(-0.33, 0.33),
                PhaseName.EXPLOITATIVE, new Phase(0.34, 1)
        );
        this.PhaseFiltersMap = new HashMap<>();
    }

    public static ActionFilter create() {
        return new ActionFilter();
    }

    public ActionFilter addConstraint(PhaseName name, ConstraintCommand constraint) {
        List<ConstraintCommand> list = getContraintsList(name);
        list.add(constraint);
        return this;
    }

    public ActionFilter phaseLengthFactor(double factor) {
        this.phaseLengthFactor = factor;
        return this;
    }

    public double getPhaseLengthFactor() {
        return this.phaseLengthFactor;
    }

    private List<ConstraintCommand> getContraintsList(PhaseName name) {
        Phase Phase = PhaseNamePhaseMap.get(name);
        return PhaseFiltersMap.computeIfAbsent(Phase, k -> new ArrayList<>());
    }


    public List<Action> filterActions(State state, AdjacencySolver solver, List<Action> actionList, int step) {
        PhaseName PhaseName = getPhaseNameFromStep(step);
        Phase Phase = PhaseName != null ? PhaseNamePhaseMap.get(PhaseName) : null;
        List<ConstraintCommand> constraints = Phase != null ? PhaseFiltersMap.get(Phase) : null;
        if (constraints == null) return actionList;
        for (ConstraintCommand constraint : constraints) {
            actionList = constraint.filter(state,solver, actionList);
        }
        return actionList;
    }

    private double cosineForStep(int step) {
        return Math.round((Math.cos(step / phaseLengthFactor) * 100d)) / 100d;
    }

    public PhaseName getPhaseNameFromStep(int step) {
        for (Map.Entry<PhaseName, Phase> entry : PhaseNamePhaseMap.entrySet()) {
            if (entry.getValue().contains(cosineForStep(step))) {
                return entry.getKey();
            }
        }
        return null;
    }
}
