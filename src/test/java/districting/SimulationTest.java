package districting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.Agent;
import siani.districting.architecture.engine.actions.BuyAction;
import siani.districting.architecture.engine.agents.RandomAgent;
import siani.districting.architecture.engine.environment.IslandDetector;
import siani.districting.architecture.engine.environment.MatrixBoundaryCalculator;
import siani.districting.architecture.engine.environment.StateFactory;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.engine.environment.actionfiltering.constraints.PopulationConstraint;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.architecture.stores.SerializerManager;
import siani.districting.architecture.stores.StateDelta;
import siani.districting.readers.ShapefileReader;
import siani.districting.readers.CsvToMapReader;
import siani.districting.architecture.stores.writer.StateCsvExporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SimulationTest {


    private static AdjacencySolver adjacencySolver;
    private static State currentState;
    private static SerializerManager manager;
    private static ActionFilter filter;
    private static List<Agent> agents;
    private static PrecinctInfoContainer table;
    private static Map<Object, Object> partyMapping;

    @BeforeAll
    static void setUp() throws IOException {
        long start = System.currentTimeMillis();
        manager = new SerializerManager("/home/mathi/Samba/tennessee/tennessee_store/data", 100);

        partyMapping = CsvToMapReader.read("/home/mathi/Samba/tennessee/tennessee_info/candidateToPartyTennessee.csv", true);

        if (manager.getLastState() != null) {
            System.out.println("Estado recuperado exitosamente desde archivos.");
            currentState = manager.getLastState();

            table = manager.getLastContainer();
            if (table == null) table = new GuavaPrecinctInfoTable();

            Set<String> recoveredAdjacency = manager.getLastAdjacencySet();
            if (recoveredAdjacency != null) {
                adjacencySolver = new AdjacencySolver(currentState.precints(), recoveredAdjacency);
            } else {
                adjacencySolver = new AdjacencySolver(currentState.precints());
            }

        } else {
            table = new GuavaPrecinctInfoTable();
            currentState = ShapefileReader.read("/home/mathi/Samba/tennessee/tennessee_info/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp",
                    "tennessee",
                    table,
                    "/home/mathi/Samba/tennessee/tennessee_info/tennessee_pop_per_cong_distr.csv");
            adjacencySolver = new AdjacencySolver(currentState.precints());
            manager.serializeAll(currentState, adjacencySolver.getAdjacencySet(), table);
        }

        filter = ActionFilter.create()
                .addConstraint(ActionFilter.PhaseName.EXPLORATIVE, new PopulationConstraint(0.05))
                .addConstraint(ActionFilter.PhaseName.TRANSITION, new PopulationConstraint(0.025))
                .addConstraint(ActionFilter.PhaseName.EXPLOITATIVE, new PopulationConstraint(0.001));

        agents = new ArrayList<>();
        for (int i=1; i <= currentState.districts().size(); i++) {
            agents.add(new RandomAgent(i));
        }
        System.out.println("Tiempo de setUp: " + (System.currentTimeMillis() - start) + " ms.");
    }

    @Test
    void simulationTest() throws IOException {
        int step = manager.getStepCount();
        MatrixBoundaryCalculator boundariesCalculator = new MatrixBoundaryCalculator();
        Map<Integer, Map<Integer, Set<Precinct>>> borders = boundariesCalculator.calculateBoundariesForFirstTime(currentState, adjacencySolver);
        long start;
        int maxSteps = 500;
        long beforeSim = System.currentTimeMillis();
        while (step < maxSteps) {
            System.out.println("Iniciando simulación step " + step++ + "...");
            start = System.currentTimeMillis();
            Map<Integer, Map<Integer, Set<Precinct>>> currentBorders = borders;
            int currentStep = step;
            State loopState = currentState;
            List<Action> chosenActions = agents.parallelStream().map(agent -> {
                        List<Action> possibleActions = currentBorders.entrySet().stream()
                                .filter(entry -> entry.getKey() != agent.id()) // Descartamos el mapa del propio agente
                                .map(entry -> entry.getValue().get(agent.id())) // Buscamos los precintos que tocan a nuestro agente
                                .filter(Objects::nonNull)
                                .flatMap(Set::stream)
                                .map(precinct -> new BuyAction(agent.id(), precinct))
                                .collect(Collectors.toList());
                        possibleActions = filter.filterActions(loopState, adjacencySolver, possibleActions, currentStep);
                        return agent.choose(new HashSet<>(possibleActions));
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Action::precinct))
                    .values().stream()
                    //CHOQUE -> borrar acciones que quieran comprar el mismo precinto
                    .filter(list -> list.size() == 1)
                    .flatMap(List::stream)
                    .collect(Collectors.toList());

            List<Action> finalActions = IslandDetector.findIslands(loopState, adjacencySolver, chosenActions);

            StateDelta delta = new StateDelta(finalActions);
            State newState = StateFactory.applyDelta(currentState, delta);

            manager.serialize(currentState, delta, newState);

            Set<Precinct> differents = delta.differencies().keySet();
            borders = boundariesCalculator.updateMatrix(newState, differents);
            currentState = newState;

            ActionFilter.PhaseName currentPhaseName = filter.getPhaseNameFromStep(currentStep);

            StateCsvExporter.exportWithWinnersPerDistrictPhaseAndPopulation(currentState,
                    "/home/mathi/Samba/tennessee/tennessee_store/csv/step_" + step + ".csv" ,
                    table,
                    partyMapping,
                    currentPhaseName);

            System.out.println("Tiempo de step: " + (System.currentTimeMillis() - start) + " ms");
        }
        System.out.println("Tiempo total de simulación para " + maxSteps + " steps: " + (System.currentTimeMillis() - beforeSim) + " ms");
    }
}
