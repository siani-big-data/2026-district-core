package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.State;

import java.util.List;
import java.util.stream.Collectors;

public class PopulationConstraint implements ConstraintCommand {
    private final double maxPopulationPercentage;

    public PopulationConstraint(double maxPopulationPercentage) {
        this.maxPopulationPercentage = maxPopulationPercentage;
    }

    @Override
    public List<Action> filter(State state, AdjacencySolver solver, List<Action> actionList) {
        return actionList.stream()
                .filter(action -> {
                    int districtId = action.districtId();
                    int precinctPopulation = action.precinct().getPopulation();
                    int districtPopulation = state.districts().stream()
                            .filter(d -> d.uniqueId() == districtId)
                            .findFirst()
                            .get()
                            .population();
                    return precinctPopulation <= districtPopulation * (maxPopulationPercentage / 100.0);
                })
                .collect(Collectors.toList());
    }
}
