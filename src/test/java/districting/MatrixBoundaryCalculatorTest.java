package districting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.engine.environment.MatrixMultiplicationBoundaryCalculator;
import siani.districting.architecture.model.District;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.PrecinctInfoContainer;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.readers.ShapefileReader;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class MatrixBoundaryCalculatorTest {

    // Hacemos las variables estáticas para poder cargarlas una sola vez
    static State state;
    static AdjacencySolver solver;
    static MatrixMultiplicationBoundaryCalculator boundaryCalculator;
    static Map<Integer, Map<Integer, Set<Precinct>>> borders;

    // @BeforeAll se ejecuta UNA SOLA VEZ antes de que corran los tests
    @BeforeAll
    static void setUp() throws IOException {
        System.out.println("⏳ Cargando Shapefile y Adyacencias (Esto solo ocurre una vez)...");

        PrecinctInfoContainer container = new GuavaPrecinctInfoTable();

        long start = System.currentTimeMillis();
        state = ShapefileReader.read("src/main/resources/tn_2024_gen_prec_NUEVO/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp", "state", container, null);
        System.out.println("✅ Shapefile cargado en: " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        solver = new AdjacencySolver(state.precints());
        System.out.println("✅ Adyacencias calculadas en: " + (System.currentTimeMillis() - start) + " ms");

        start = System.currentTimeMillis();
        boundaryCalculator = new MatrixMultiplicationBoundaryCalculator();
        borders = boundaryCalculator.calculateBoundariesForFirstTime(state, solver);
        System.out.println("✅ Fronteras calculadas en: " + (System.currentTimeMillis() - start) + " ms");
    }

    @Test
    void shouldCalculateBordersCorrectly() {
        System.out.println("\n--- TEST: IMPRIMIR FRONTERAS DISTRITO 6 ---");

        // Verificamos que el distrito 6 existe y tiene fronteras
        assertNotNull(borders.get(6), "El distrito 6 no existe o no tiene fronteras");

        borders.get(6).forEach((neighborId, borderPrecincts) -> {
            System.out.println("Precintos en frontera con el Distrito " + neighborId + ": " + borderPrecincts.size() + " precintos.");
        });
    }

    @Test
    void shouldUpdateMatrixWhenPrecinctMovesFromDistrict1ToDistrict2() {
        System.out.println("\n--- TEST: ACTUALIZACIÓN DE MATRIZ ---");

        // 1. Obtener la frontera actual del Distrito 1 hacia el Distrito 2
        Set<Precinct> borderD1toD2 = borders.get(1).get(2);

        assertNotNull(borderD1toD2, "El distrito 1 no tiene fronteras con el 2");
        assertFalse(borderD1toD2.isEmpty(), "La frontera entre D1 y D2 está vacía");

        // 2. Cogemos el primer precinto de esa frontera
        Precinct precinctToMove = borderD1toD2.iterator().next();
        System.out.println("Precinto seleccionado para mover: " + precinctToMove.id());

        // 3. MODIFICAR EL ESTADO
        state.getPrecinctsAndDistrictsMap().put(precinctToMove, 2);

        District dist1 = state.districts().stream().filter(d -> d.uniqueId() == 1).findFirst().get();
        District dist2 = state.districts().stream().filter(d -> d.uniqueId() == 2).findFirst().get();

        dist1.precinctList().remove(precinctToMove);
        dist2.precinctList().add(precinctToMove);

        // 4. LLAMAR AL UPDATE MATRIX
        long startUpdate = System.currentTimeMillis();
        Map<Integer, Map<Integer, Set<Precinct>>> newBorders = boundaryCalculator.updateMatrix(state, Set.of(precinctToMove));
        long endUpdate = System.currentTimeMillis();

        System.out.println("Tiempo en actualizar la matriz: " + (endUpdate - startUpdate) + " ms");

        // 5. VERIFICACIONES (ASSERTS)
        assertFalse(
                newBorders.get(1).get(2).contains(precinctToMove),
                "Error: El precinto sigue apareciendo como frontera del Distrito 1"
        );

        assertTrue(
                newBorders.get(2).get(1).contains(precinctToMove),
                "Error: El precinto debería ser ahora una frontera del Distrito 2 hacia el Distrito 1"
        );

        System.out.println("✅ Test superado: El precinto ha cambiado de distrito correctamente y las fronteras se han actualizado.");
    }
}