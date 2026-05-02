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

public class TexasTest {

    // 1. Declaramos las variables como 'static' para que vivan durante toda la ejecución
    static State texas;
    static AdjacencySolver solver;
    static MatrixMultiplicationBoundaryCalculator boundaryCalculator;
    static Map<Integer, Map<Integer, Set<Precinct>>> borders;

    // 2. El bloque @BeforeAll se ejecuta UNA SOLA VEZ
    @BeforeAll
    static void setUp() throws IOException {
        System.out.println("⏳ Cargando Shapefile de Texas y Adyacencias (Esto solo ocurre una vez)...");

        PrecinctInfoContainer container = new GuavaPrecinctInfoTable();

        // Medimos lectura del Shapefile
        long start = System.currentTimeMillis();
        texas = ShapefileReader.read("src/main/resources/tx_2024_gen_tx_vtd/tx_2024_gen_cong_tx_vtd/tx_2024_gen_cong_tx_vtd.shp", "texas", container);
        System.out.println("✅ Shapefile cargado en: " + (System.currentTimeMillis() - start) + " ms");

        // Medimos cálculo del Solver (Este es el que más tardará en Texas)
        start = System.currentTimeMillis();
        solver = new AdjacencySolver(texas.precints());
        System.out.println("✅ Adyacencias calculadas en: " + (System.currentTimeMillis() - start) + " ms");

        // Medimos cálculo del primer "Mapa de Fronteras"
        boundaryCalculator = new MatrixMultiplicationBoundaryCalculator();
        start = System.currentTimeMillis();
        borders = boundaryCalculator.calculateBoundariesForFirstTime(texas, solver);
        System.out.println("✅ Fronteras iniciales calculadas en: " + (System.currentTimeMillis() - start) + " ms\n");
    }

    @Test
    void shouldCalculateBordersCorrectly() {
        System.out.println("\n--- TEST: IMPRIMIR FRONTERAS DISTRITO 6 ---");

        assertNotNull(borders.get(6), "El distrito 6 no existe o no tiene fronteras");

        // En lugar de imprimir todos los IDs (que en Texas inundarían la consola),
        // imprimimos el tamaño de la frontera para ver que funciona.
        borders.get(6).forEach((neighborId, borderPrecincts) -> {
            System.out.println("Precintos en frontera con el Distrito " + neighborId + ": " + borderPrecincts.size() + " precintos.");
        });
    }

    @Test
    void shouldMovePrecinctFrom13To19AndUpdateBorders() {
        System.out.println("\n========== INICIANDO TEST TEXAS: DISTRITOS 13, 14 y 19 ==========");

        // 1. IMPRIMIR FRONTERAS 13 <-> 14 (Con comprobación de seguridad por si no se tocan)
        System.out.println("\n--- Frontera Inicial: Distrito 13 hacia Distrito 14 ---");
        if (borders.get(13).containsKey(14)) {
            borders.get(13).get(14).forEach(p -> System.out.println("  - " + p.id()));
        } else {
            System.out.println("  (Vacío: El Distrito 13 y el 14 no se tocan físicamente en Texas)");
        }

        System.out.println("\n--- Frontera Inicial: Distrito 14 hacia Distrito 13 ---");
        if (borders.get(14).containsKey(13)) {
            borders.get(14).get(13).forEach(p -> System.out.println("  - " + p.id()));
        } else {
            System.out.println("  (Vacío: El Distrito 14 y el 13 no se tocan físicamente en Texas)");
        }

        // 2. BUSCAR UN PRECINTO EN LA FRONTERA DE 13 CON 19
        System.out.println("\n--- Seleccionando Precinto del D13 para pasarlo al D19 ---");
        Set<Precinct> border13to19 = borders.get(13).get(19);

        // 5. IMPRIMIR FRONTERAS 13 <-> 19 TRAS LA ACTUALIZACIÓN
        System.out.println("\n--- Frontera Actualizada: Distrito 13 hacia Distrito 19 ---");
        borders.get(13).get(19).forEach(p -> System.out.println("  - " + p.id()));

        System.out.println("\n--- Frontera Actualizada: Distrito 19 hacia Distrito 13 ---");
        borders.get(19).get(13).forEach(p -> System.out.println("  - " + p.id()));

        // Nos aseguramos de que el test no continúe si no hay frontera
        assertNotNull(border13to19, "Error: El distrito 13 no tiene fronteras con el 19");
        assertFalse(border13to19.isEmpty(), "Error: La frontera 13->19 está vacía");

        // Cogemos el primer precinto de esa frontera
        Precinct precinctToMove = border13to19.iterator().next();
        System.out.println("Precinto elegido para el traspaso: " + precinctToMove.id());

        // 3. EFECTUAR EL MOVIMIENTO EN EL ESTADO
        texas.getPrecinctsAndDistrictsMap().put(precinctToMove, 19);

        District dist13 = texas.districts().stream().filter(d -> d.uniqueId() == 13).findFirst().get();
        District dist19 = texas.districts().stream().filter(d -> d.uniqueId() == 19).findFirst().get();

        dist13.precinctList().remove(precinctToMove);
        dist19.precinctList().add(precinctToMove);

        // 4. ACTUALIZAR LA MATRIZ DE FRONTERAS
        System.out.println("\nActualizando matriz de fronteras con el nuevo cambio...");
        long startUpdate = System.currentTimeMillis();
        Map<Integer, Map<Integer, Set<Precinct>>> updatedBorders = boundaryCalculator.updateMatrix(texas, Set.of(precinctToMove));
        System.out.println("Matriz actualizada en: " + (System.currentTimeMillis() - startUpdate) + " ms");

        // 5. IMPRIMIR FRONTERAS 13 <-> 19 TRAS LA ACTUALIZACIÓN
        System.out.println("\n--- Frontera Actualizada: Distrito 13 hacia Distrito 19 ---");
        updatedBorders.get(13).get(19).forEach(p -> System.out.println("  - " + p.id()));

        System.out.println("\n--- Frontera Actualizada: Distrito 19 hacia Distrito 13 ---");
        updatedBorders.get(19).get(13).forEach(p -> System.out.println("  - " + p.id()));

        // 6. VALIDACIONES MATEMÁTICAS (ASSERTS)
        assertFalse(
                updatedBorders.get(13).get(19).contains(precinctToMove),
                "Error: El precinto se movió al D19, ya no puede ser frontera del D13"
        );

        assertTrue(
                updatedBorders.get(19).get(13).contains(precinctToMove),
                "Error: El precinto debería aparecer ahora como frontera desde el D19 hacia el D13"
        );

        System.out.println("\n✅ Test completado: El movimiento fue exitoso y los bordes son matemáticamente correctos.");
    }

    @Test
    void shouldMoveMultiplePrecinctsAndPrintBorders() {
        System.out.println("\n========== INICIANDO TEST MÚLTIPLES CAMBIOS: 13->19 y 19->11 ==========");

        // 1. IMPRIMIR FRONTERAS ANTES DE LOS CAMBIOS
        System.out.println("\n[ANTES] Fronteras 13 <-> 19:");
        borders.get(13).getOrDefault(19, Set.of()).forEach(p -> System.out.println("  13 hacia 19: " + p.id()));
        borders.get(19).getOrDefault(13, Set.of()).forEach(p -> System.out.println("  19 hacia 13: " + p.id()));

        System.out.println("\n[ANTES] Fronteras 19 <-> 11:");
        borders.get(19).getOrDefault(11, Set.of()).forEach(p -> System.out.println("  19 hacia 11: " + p.id()));
        borders.get(11).getOrDefault(19, Set.of()).forEach(p -> System.out.println("  11 hacia 19: " + p.id()));

        // 2. SELECCIONAR LOS PRECINTOS A MOVER
        Set<Precinct> border13to19 = borders.get(13).get(19);
        assertNotNull(border13to19, "Error: No hay frontera 13->19");
        Precinct p13to19 = border13to19.iterator().next(); // Cogemos el primero

        Set<Precinct> border19to11 = borders.get(19).get(11);
        assertNotNull(border19to11, "Error: No hay frontera 19->11");
        Precinct p19to11 = border19to11.iterator().next(); // Cogemos el primero

        System.out.println("\nPrecintos seleccionados para el traspaso:");
        System.out.println(" - Pasar del 13 al 19: " + p13to19.id());
        System.out.println(" - Pasar del 19 al 11: " + p19to11.id());

        // 3. EFECTUAR LOS CAMBIOS EN EL ESTADO
        District dist13 = texas.districts().stream().filter(d -> d.uniqueId() == 13).findFirst().get();
        District dist19 = texas.districts().stream().filter(d -> d.uniqueId() == 19).findFirst().get();
        District dist11 = texas.districts().stream().filter(d -> d.uniqueId() == 11).findFirst().get();

        // Movimiento 1 (13 -> 19)
        texas.getPrecinctsAndDistrictsMap().put(p13to19, 19);
        dist13.precinctList().remove(p13to19);
        dist19.precinctList().add(p13to19);

        // Movimiento 2 (19 -> 11)
        texas.getPrecinctsAndDistrictsMap().put(p19to11, 11);
        dist19.precinctList().remove(p19to11);
        dist11.precinctList().add(p19to11);

        // 4. ACTUALIZAR MATRIZ PASANDO AMBOS CAMBIOS
        System.out.println("\nActualizando matriz con 2 cambios simultáneos...");
        long startUpdate = System.currentTimeMillis();
        // Pasamos un Set.of() con los dos precintos modificados
        Map<Integer, Map<Integer, Set<Precinct>>> updatedBorders = boundaryCalculator.updateMatrix(texas, Set.of(p13to19, p19to11));
        System.out.println("Matriz actualizada en: " + (System.currentTimeMillis() - startUpdate) + " ms");

        // 5. IMPRIMIR FRONTERAS DESPUÉS DE LOS CAMBIOS
        System.out.println("\n[DESPUÉS] Fronteras 13 <-> 19:");
        updatedBorders.get(13).getOrDefault(19, Set.of()).forEach(p -> System.out.println("  13 hacia 19: " + p.id()));
        updatedBorders.get(19).getOrDefault(13, Set.of()).forEach(p -> System.out.println("  19 hacia 13: " + p.id()));

        System.out.println("\n[DESPUÉS] Fronteras 19 <-> 11:");
        updatedBorders.get(19).getOrDefault(11, Set.of()).forEach(p -> System.out.println("  19 hacia 11: " + p.id()));
        updatedBorders.get(11).getOrDefault(19, Set.of()).forEach(p -> System.out.println("  11 hacia 19: " + p.id()));

        // 6. VALIDACIONES MATEMÁTICAS (ASSERTS)
        // Verificamos Movimiento 1
        assertFalse(updatedBorders.get(13).get(19).contains(p13to19), "El precinto 1 sigue en la frontera 13->19");
        assertTrue(updatedBorders.get(19).get(13).contains(p13to19), "El precinto 1 no aparece en la frontera 19->13");

        // Verificamos Movimiento 2
        assertFalse(updatedBorders.get(19).get(11).contains(p19to11), "El precinto 2 sigue en la frontera 19->11");
        assertTrue(updatedBorders.get(11).get(19).contains(p19to11), "El precinto 2 no aparece en la frontera 11->19");

        System.out.println("\n✅ Test múltiple completado con éxito.");
    }
}