package siani.districting.architecture.engine;

import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.agents.RandomAgent;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.engine.environment.actionfiltering.constraints.PopulationConstraint;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.architecture.stores.SerializerManager;
import siani.districting.architecture.stores.writer.StateCsvExporter;
import siani.districting.readers.CsvToMapReader;
import siani.districting.readers.ShapefileReader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Simulator {

    private static final int SNAPSHOT_STEP = 50;
    private static final int TEXAS_STEPS_TO_RUN = 650;

    private static final String TEXAS_STORE_PATH = "/home/mathi/Samba/texas/texas_store/dat";
    private static final String TEXAS_CSV_OUTPUT_PATH = "/home/mathi/Samba/texas/texas_store/csv";
    private static final String TEXAS_PARTY_MAPPING_PATH =
            "/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/candidateToPartyTexas.csv";
    private static final String TEXAS_SHAPEFILE_PATH =
            "/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/tx_2024_gen_cong_tx_vtd/tx_2024_gen_cong_tx_vtd.shp";
    private static final String TEXAS_POPULATION_PATH =
            "/home/mathi/Samba/texas/texas_info/tx_2024_gen_tx_vtd/populationPerDistrictTexas24.csv";

    public static void main(String[] args) throws IOException {
        long start = System.currentTimeMillis();
        Engine engine = createTexasEngine();

        System.out.println("Starting Texas engine example at step " + engine.currentStep());
        Engine.RunResult result = engine.runSteps(TEXAS_STEPS_TO_RUN);

        System.out.println("Texas engine example finished.");
        System.out.println("Initial run size: " + TEXAS_STEPS_TO_RUN + " steps");
        System.out.println("Final step: " + result.finalStep());
        System.out.println("District count: " + result.finalState().districts().size());
        System.out.println("Total elapsed time: " + (System.currentTimeMillis() - start) + " ms");
    }

    public static Engine createTexasEngine() throws IOException {
        SerializerManager manager = new SerializerManager(TEXAS_STORE_PATH, SNAPSHOT_STEP);
        Map<Object, Object> partyMapping = CsvToMapReader.read(TEXAS_PARTY_MAPPING_PATH, true);

        PrecinctInfoContainer table;
        State currentState;
        AdjacencySolver adjacencySolver;

        if (manager.getLastState() != null) {
            System.out.println("Recovered Texas state from " + TEXAS_STORE_PATH);
            currentState = manager.getLastState();
            table = manager.getLastContainer();
            adjacencySolver = new AdjacencySolver(currentState.precints(), manager.getLastAdjacencySet());
        } else {
            System.out.println("No Texas state found. Reading shapefile and building initial store.");
            table = new GuavaPrecinctInfoTable();
            currentState = ShapefileReader.read(
                    TEXAS_SHAPEFILE_PATH,
                    "Texas",
                    table,
                    TEXAS_POPULATION_PATH
            );
            adjacencySolver = new AdjacencySolver(currentState.precints());
            manager.serializeAll(currentState, adjacencySolver.getAdjacencySet(), table);
        }

        ActionFilter filter = ActionFilter.create()
                .addConstraint(ActionFilter.EpochName.EXPLORATIVE, new PopulationConstraint(0.05))
                .addConstraint(ActionFilter.EpochName.TRANSITION, new PopulationConstraint(0.025))
                .addConstraint(ActionFilter.EpochName.EXPLOITATIVE, new PopulationConstraint(0.001));

        List<Agent> agents = createRandomAgents(currentState);

        return Engine.builder()
                .initialState(currentState)
                .adjacencySolver(adjacencySolver)
                .agents(agents)
                .actionFilter(filter)
                .serializerManager(manager)
                .initialStep(manager.getStepCount())
                .stepListener(result -> {
                    exportStepCsv(result, table, partyMapping);
                    printStepSummary(result);
                })
                .build();
    }

    private static List<Agent> createRandomAgents(State state) {
        List<Agent> agents = new ArrayList<>();
        for (int districtId = 1; districtId <= state.districts().size(); districtId++) {
            agents.add(new RandomAgent(districtId));
        }
        return agents;
    }

    private static void exportStepCsv(Engine.StepResult result,
                                      PrecinctInfoContainer table,
                                      Map<Object, Object> partyMapping) throws IOException {
        File outputDirectory = new File(TEXAS_CSV_OUTPUT_PATH);
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            throw new IOException("Could not create csv output directory: " + outputDirectory);
        }

        String outputPath = new File(outputDirectory, "step_" + result.step() + ".csv").getAbsolutePath();
        StateCsvExporter.exportWithWinnersPerDistrictEpochAndPopulation(
                result.state(),
                outputPath,
                table,
                partyMapping,
                result.epochName()
        );
    }

    private static void printStepSummary(Engine.StepResult result) {
        System.out.println(
                "Step " + result.step()
                        + " | epoch=" + result.epochName()
                        + " | selected=" + result.selectedActions()
                        + " | applied=" + result.appliedActions()
                        + " | changedPrecincts=" + result.changedPrecincts()
                        + " | elapsedMs=" + result.elapsedMillis()
        );
    }
}
