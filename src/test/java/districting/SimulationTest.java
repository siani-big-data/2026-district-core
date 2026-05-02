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
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.architecture.stores.SerializerManager;
import siani.districting.architecture.stores.StateDelta;
import siani.districting.readers.ShapefileReader;
import siani.districting.architecture.stores.writer.StateCsvExporter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SimulationTest {


    private static AdjacencySolver adjacencySolver;
    private static State currentState;
    private static SerializerManager manager;
    private static ActionFilter filter = ActionFilter.create()
            .addConstraint(Constraint.MIN_PRECINCTS);
    private static List<Agent> agents;

    @BeforeAll
    static void setUp() throws IOException {
        long start = System.currentTimeMillis();
        manager = new SerializerManager("src/main/resources/simulationTest_store", 100);

        if (manager.getLastState() != null) {
            System.out.println("Estado recuperado exitosamente desde archivos.");
            currentState = manager.getLastState();
            adjacencySolver = new AdjacencySolver(currentState.precints());
        } else {
            GuavaPrecinctInfoTable table = new GuavaPrecinctInfoTable();
            currentState = ShapefileReader.read("src/main/resources/tn_2024_gen_prec_NUEVO/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp", "tennessee", table);
            adjacencySolver = new AdjacencySolver(currentState.precints());
            manager.serialize(currentState);
            manager.serialize(adjacencySolver.getAdjacencySet());
        }

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
        int maxSteps = 1000;
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

            StateCsvExporter.export(currentState, "src/main/resources/simulationTest_store/step_" + step + ".csv");

            System.out.println("Tiempo de step: " + (System.currentTimeMillis() - start) + " ms");
        }
        System.out.println("Tiempo total de simulación para " + maxSteps + " steps: " + (System.currentTimeMillis() - beforeSim) + " ms");
    }
}
