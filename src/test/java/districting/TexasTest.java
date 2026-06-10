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
        manager = new SerializerManager("/home/mathi/Samba/texas/texas_store/dat", 100);

        partyMapping = CsvToMapReader
                .read("/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/candidateToPartyTexas.csv", true);

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
            currentState = ShapefileReader.read("/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/tx_2024_gen_cong_tx_vtd/tx_2024_gen_cong_tx_vtd.shp",
                    "texas",
                    table,
                    "/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/populationPerDistrictTexas24.csv");
            adjacencySolver = new AdjacencySolver(currentState.precints());
            manager.serialize(currentState);
            manager.serialize(adjacencySolver.getAdjacencySet());
            manager.serialize(table);
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

        System.out.println("Calculating initial boundaries...");
        long initStart = System.currentTimeMillis();
        Map<Integer, Map<Integer, Set<Precinct>>> borders = boundariesCalculator.calculateBoundariesForFirstTime(currentState, adjacencySolver);
        System.out.println("Initial boundaries calculation took: " + (System.currentTimeMillis() - initStart) + " ms");

        long start;
        int maxSteps = 500;
        long beforeSim = System.currentTimeMillis();

        while (step < maxSteps) {
            System.out.println("\n--- Starting simulation step " + step++ + " ---");
            start = System.currentTimeMillis();
            long checkpoint = start;

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

            System.out.println("Agent actions generation and filtering took: " + (System.currentTimeMillis() - checkpoint) + " ms");
            checkpoint = System.currentTimeMillis();

            List<Action> finalActions = IslandDetector.findIslands(loopState, adjacencySolver, chosenActions);

            System.out.println("Island detection took: " + (System.currentTimeMillis() - checkpoint) + " ms");
            checkpoint = System.currentTimeMillis();

            StateDelta delta = new StateDelta(finalActions);
            State newState = StateFactory.applyDelta(currentState, delta);

            System.out.println("State delta creation and application took: " + (System.currentTimeMillis() - checkpoint) + " ms");
            checkpoint = System.currentTimeMillis();

            manager.serialize(currentState, delta, newState);

            System.out.println("Serialization took: " + (System.currentTimeMillis() - checkpoint) + " ms");
            checkpoint = System.currentTimeMillis();

            Set<Precinct> differents = delta.differencies().keySet();
            borders = boundariesCalculator.updateMatrix(newState, differents);
            currentState = newState;

            System.out.println("Boundary matrix update took: " + (System.currentTimeMillis() - checkpoint) + " ms");
            checkpoint = System.currentTimeMillis();

            StateCsvExporter.exportWithWinnersPerDistrictAndPopulation(currentState,
                    "/home/mathi/Samba/texas/texas_store/csv/step_" + step + ".csv" ,
                    table,
                    partyMapping);

            System.out.println("CSV export took: " + (System.currentTimeMillis() - checkpoint) + " ms");

            System.out.println("Total time for step " + (step - 1) + ": " + (System.currentTimeMillis() - start) + " ms");
        }
        System.out.println("\nTotal simulation time for " + maxSteps + " steps: " + (System.currentTimeMillis() - beforeSim) + " ms");
    }
}
