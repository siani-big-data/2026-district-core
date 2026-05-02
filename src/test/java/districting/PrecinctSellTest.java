package districting;

import org.junit.jupiter.api.Test;
import siani.districting.architecture.adjacency.AdjacencySolver;
import siani.districting.architecture.model.Precinct;
import siani.districting.architecture.model.State;
import siani.districting.architecture.stores.SerializerManager;
import siani.districting.architecture.stores.StateDelta;
import siani.districting.readers.ShapefileReader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class PrecinctSellTest {

    @Test
    void shouldProcessPrecinctExchange() throws IOException {

        State tennessee = ShapefileReader.read("src/main/resources/tn_2024_gen_prec/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp",
                                            "tennessee", null);

        AdjacencySolver solver = new AdjacencySolver(tennessee.precints());
        SerializerManager serializerManager = new SerializerManager("src/main/resources/tenneesee_store");

        serializerManager.serialize(tennessee, null,tennessee);

        State fetchedTennessee = serializerManager.getLastState();
        System.out.println(fetchedTennessee.getName());

        assertThat(fetchedTennessee.getName()).isEqualTo(tennessee.getName());

        Precinct testPrecinct = fetchedTennessee.precints().getFirst();
        System.out.println("testPrecint Id: " + testPrecinct.id());
        System.out.println("first, belongs to District: " + fetchedTennessee.getPrecinctsAndDistrictsMap().get(testPrecinct));

        Integer valorEsperado = fetchedTennessee.getPrecinctsAndDistrictsMap().get(testPrecinct) % 8 + 1;

        executeMockSale(fetchedTennessee, testPrecinct, valorEsperado);

        serializerManager.serialize(tennessee, new StateDelta(Map.of(testPrecinct, valorEsperado)), fetchedTennessee);
        State newTennessee = serializerManager.getLastState();
        System.out.println("New Fetched State has precinct " + testPrecinct.id() + " at Disctrict: " + newTennessee.getPrecinctsAndDistrictsMap().get(testPrecinct));
        assertThat(newTennessee.getPrecinctsAndDistrictsMap().get(testPrecinct)).isEqualTo(valorEsperado);
    }

    private static void executeMockSale(State fetchedTennessee, Precinct testPrecinct, Integer valorEsperado) {
        fetchedTennessee.getPrecinctsAndDistrictsMap().put(testPrecinct, valorEsperado);

        fetchedTennessee.districts().forEach(d -> {

            d.precinctList().removeIf(p -> Objects.equals(p.id(), testPrecinct.id()));

            if (d.uniqueId() == fetchedTennessee.getPrecinctsAndDistrictsMap().get(valorEsperado)) d.precinctList().add(testPrecinct);

        });
    }
}
