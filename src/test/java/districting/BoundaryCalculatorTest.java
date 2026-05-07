package districting;

import org.junit.jupiter.api.Test;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.environment.BoundariesCalculator;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.readers.ShapefileReader;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BoundaryCalculatorTest {

    long cTotal = System.currentTimeMillis();
    long c1 = System.currentTimeMillis();
    PrecinctInfoContainer container = new GuavaPrecinctInfoTable();
    State tennessee = ShapefileReader.read("src/main/resources/tn_2024_gen_prec_NUEVO/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp",
            "tennessee", container, null);
    long t1 = System.currentTimeMillis();

    long c2 = System.currentTimeMillis();
    AdjacencySolver solver = new AdjacencySolver(tennessee.precints());
    long t2 = System.currentTimeMillis();

    long c3 = System.currentTimeMillis();
    Map<Integer, Map<Integer, Set<Precinct>>> borders = BoundariesCalculator.calculateBoundariesOf(tennessee,solver);
    long t3 = System.currentTimeMillis();
    long tTotal = System.currentTimeMillis();

    public BoundaryCalculatorTest() throws IOException {
    }


    @Test
    void shouldCalculateBordersCorrectly() {

        container.getAllPrecinctsIds().stream().filter(id -> id.contains("Scott"))
                .forEach(System.out::println);

        System.out.println("----------------------------------------------------------------");

        System.out.println("Borders Of Cong. District 6: ");
        borders.get(6).forEach((key, value) -> {

            System.out.println("Precincts in the border with " + key + " are: ");
            value.forEach(p -> System.out.println(p.id()));

        });

        System.out.println("Tiempo de lectura Shapefile: " + (t1-c1) + " ms");
        System.out.println("Tiempo de cálculo adyascencia: " + (t2-c2) + " ms");
        System.out.println("Tiempo de cálculo de fronteras: " + (t3-c3) + " ms");
        System.out.println("Tiempo total: " + (tTotal-cTotal) + " ms");
    }
}
