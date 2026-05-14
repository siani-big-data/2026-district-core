package siani.districting.architecture.engine.environment.actionfiltering.constraints;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.State;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PopulationConstraint implements ConstraintCommand {
    private final double variance;

    public PopulationConstraint(double variance) {
        this.variance = variance;
    }

    @Override
    public List<Action> filter(State state, AdjacencySolver solver, List<Action> actionList) {
        Map<Integer, District> districtMap = state.districts()
                .stream()
                .collect(Collectors.toMap(District::uniqueId, district -> district));

        double maxPermittedPopulation = state.averagePopulationPerDistrict() + (state.averagePopulationPerDistrict() * variance);
        double minPermittedPopulation = state.averagePopulationPerDistrict() - (state.averagePopulationPerDistrict() * variance);
        return actionList.stream()
                .filter(action -> {
                    int sellingDistrictId = state.getPrecinctsAndDistrictsMap().get(action.precinct());
                    District sellingDistrict = districtMap.get(sellingDistrictId);
                    District buyingDistrict = districtMap.get(action.districtId());

                    boolean sellerCanSell =
                            sellingDistrict.population() - action.precinct().getPopulation() >=
                                    minPermittedPopulation;

                    boolean buyerCanBuy =
                            buyingDistrict.population() + action.precinct().getPopulation() <=
                                    maxPermittedPopulation;

                    return sellerCanSell && buyerCanBuy;
                })
                .collect(Collectors.toList());
    }
}
