package districting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;
import siani.districting.architecture.geometry.GeoBox;
import siani.districting.architecture.geometry.GeoShape;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.architecture.stores.writer.StateCsvExporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateCsvExporterTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldExportDistrictPopulationForEachStepRow() throws IOException {
        Precinct precinctA = precinct("A", 120);
        Precinct precinctB = precinct("B", 80);
        Precinct precinctC = precinct("C", 50);

        State state = new State("test-state", List.of(
                new District(1, List.of(precinctA, precinctB)),
                new District(2, List.of(precinctC))
        ));

        Path output = tempDir.resolve("step.csv");
        StateCsvExporter.exportWithWinnersPerDistrictPhaseAndPopulation(
                state,
                output.toString(),
                new GuavaPrecinctInfoTable(),
                Map.of(),
                ActionFilter.PhaseName.TRANSITION
        );

        List<String> lines = Files.readAllLines(output);
        assertEquals("precinct_id,district_id,election_winner,Phase,district_population", lines.get(0));
        assertEquals(4, lines.size());
        assertEquals(1, lines.stream().filter(line -> line.equals("A,1,UNKNOWN,TRANSITION,200")).count());
        assertEquals(1, lines.stream().filter(line -> line.equals("B,1,UNKNOWN,TRANSITION,200")).count());
        assertEquals(1, lines.stream().filter(line -> line.equals("C,2,UNKNOWN,TRANSITION,50")).count());
    }

    @Test
    void shouldExportDistrictPopulationWithoutPhase() throws IOException {
        Precinct precinctA = precinct("A", 120);
        Precinct precinctB = precinct("B", 80);

        State state = new State("test-state", List.of(
                new District(1, List.of(precinctA, precinctB))
        ));

        Path output = tempDir.resolve("step-no-Phase.csv");
        StateCsvExporter.exportWithWinnersPerDistrictAndPopulation(
                state,
                output.toString(),
                new GuavaPrecinctInfoTable(),
                Map.of()
        );

        List<String> lines = Files.readAllLines(output);
        assertEquals("precinct_id,district_id,election_winner,district_population", lines.get(0));
        assertEquals(3, lines.size());
        assertEquals(1, lines.stream().filter(line -> line.equals("A,1,UNKNOWN,200")).count());
        assertEquals(1, lines.stream().filter(line -> line.equals("B,1,UNKNOWN,200")).count());
    }

    private static Precinct precinct(String id, int population) {
        Precinct precinct = new Precinct(id, new DummyGeoShape());
        precinct.setPopulation(population);
        return precinct;
    }

    private static final class DummyGeoShape implements GeoShape {
        @Override
        public GeoBox getBoundingBox() {
            return null;
        }

        @Override
        public boolean intersects(GeoShape other) {
            return false;
        }
    }
}
