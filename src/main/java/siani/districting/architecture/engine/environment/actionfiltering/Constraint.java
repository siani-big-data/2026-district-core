package siani.districting.architecture.engine.environment.actionfiltering;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.State;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public enum Constraint implements ConstraintCommand {

    MIN_POPULATION {
        
        private double earlyVariance = 5;
        private double midVariance = 2.5;
        private double lateVariance = 0.1;

        @Override
        public Constraint factors(double early, double mid, double late) {
            earlyVariance = early;
            midVariance = mid;
            lateVariance = late;
            return this;
        }
        
        @Override
        public List<Action> filter(State state,
                                   AdjacencySolver solver,
                                   List<Action> actionList,
                                   int simulationStep
        ) {
            if (actionList.isEmpty()) return actionList;

            Map<Integer, District> districtMap = state.districts().stream()
                    .collect(Collectors.toMap(District::uniqueId, d -> d));

            return actionList.parallelStream().filter(action ->  {
                Integer sellingDistrictId = state.getPrecinctsAndDistrictsMap().get(action.precinct());
                District district = districtMap.get(sellingDistrictId);
                if (district == null) return false;
                
                double amplitude = (earlyVariance - lateVariance) / 2.0;
                double offset = (earlyVariance + lateVariance) / 2.0;
                double currentVariancePct = offset + amplitude * Math.cos(2 * Math.PI * (simulationStep % 100) / 100.0);
                
                int variance = (int) ((currentVariancePct / 100.0) * state.averagePopulationPerDistrict());

                return district.population() >= state.averagePopulationPerDistrict() - variance;
            }).toList();
        }
    },

    MAX_POPULATION {
        private double earlyVariance = 5;
        private double midVariance = 2.5;
        private double lateVariance = 0.1;

        @Override
        public Constraint factors(double early, double mid, double late) {
            earlyVariance = early;
            midVariance = mid;
            lateVariance = late;
            return this;
        }

        @Override
        public List<Action> filter(State state,
                                   AdjacencySolver solver,
                                   List<Action> actionList,
                                   int simulationStep
        ) {
            if (actionList.isEmpty()) return actionList;

            Map<Integer, District> districtMap = state.districts().stream()
                    .collect(Collectors.toMap(District::uniqueId, d -> d));

            return actionList.parallelStream().filter(action ->  {
                Integer buyingDistrictId = action.districtId();
                District district = districtMap.get(buyingDistrictId);
                if (district == null) return false;

                double amplitude = (earlyVariance - lateVariance) / 2.0;
                double offset = (earlyVariance + lateVariance) / 2.0;
                double currentVariancePct = offset + amplitude * Math.cos(2 * Math.PI * (simulationStep % 100) / 100.0);

                int variance = (int) ((currentVariancePct / 100.0) * state.averagePopulationPerDistrict());

                return district.population() <= state.averagePopulationPerDistrict() + variance;
            }).toList();
        }
    };

    public Constraint factors(double early, double mid, double late) {
        return this;
    }
}
