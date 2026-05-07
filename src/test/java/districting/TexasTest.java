package districting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.Action;
import siani.districting.architecture.engine.Agent;
import siani.districting.architecture.engine.actions.BuyAction;
import siani.districting.architecture.engine.agents.RandomAgent;
import siani.districting.architecture.engine.environment.IslandDetector;
import siani.districting.architecture.engine.environment.MatrixMultiplicationBoundaryCalculator;
import siani.districting.architecture.engine.environment.StateFactory;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.engine.environment.actionfiltering.Constraint;
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

public class TexasTest {


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
        manager = new SerializerManager("/Volumes/Samba/texas_store/dat", 100);

        table = new GuavaPrecinctInfoTable();
        partyMapping = CsvToMapReader
                .read("src/main/resources/tx_2024_gen_tx_vtd/tx_2024_gen_cong_tx_vtd/candidateToPartyTexas.csv", true);

        if (manager.getLastState() != null) {
            System.out.println("Estado recuperado exitosamente desde archivos.");
            currentState = manager.getLastState();
            adjacencySolver = new AdjacencySolver(currentState.precints());
        } else {
            currentState = ShapefileReader.read("src/main/resources/tx_2024_gen_tx_vtd/tx_2024_gen_cong_tx_vtd/tx_2024_gen_cong_tx_vtd.shp",
                    "texas",
                    table,
                    "src/main/resources/tx_2024_gen_tx_vtd/populationPerDistrictTexas24.csv");
            adjacencySolver = new AdjacencySolver(currentState.precints());
            manager.serialize(currentState);
            manager.serialize(adjacencySolver.getAdjacencySet());
        }

        filter = ActionFilter.create()
                .addConstraint(Constraint.MIN_POPULATION.factors(10, 5, 0.1))
                .addConstraint(Constraint.MAX_POPULATION.factors(10, 5, 0.1));

        agents = new ArrayList<>();
        for (int i=1; i <= currentState.districts().size(); i++) {
            agents.add(new RandomAgent(i));
        }
        System.out.println("Tiempo de setUp: " + (System.currentTimeMillis() - start) + " ms.");
    }

    @Test
    void simulationTest() throws IOException {
        int step = manager.getStepCount();
        MatrixMultiplicationBoundaryCalculator boundariesCalculator = new MatrixMultiplicationBoundaryCalculator();
        Map<Integer, Map<Integer, Set<Precinct>>> borders = boundariesCalculator.calculateBoundariesForFirstTime(currentState, adjacencySolver);
        long start = 0L;
        int maxSteps = 100;
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

            StateCsvExporter.exportWithWinnersPerDistrict(currentState,
                    "/Volumes/Samba/texas_store/csv/step_" + step + ".csv" ,
                    table,
                    partyMapping);

            System.out.println("Tiempo de step: " + (System.currentTimeMillis() - start) + " ms");
        }
        System.out.println("Tiempo total de simulación para " + maxSteps + " steps: " + (System.currentTimeMillis() - beforeSim) + " ms");
    }
}
