package districting;

import org.junit.jupiter.api.Test;
import siani.districting.architecture.model.State;
import siani.districting.architecture.precinctinfo.guava.GuavaPrecinctInfoTable;
import siani.districting.readers.ShapefileReader;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class PrecinctInfoTableTest {

    @Test
    void shouldBuildStateWhileFillingTable() throws IOException {

        GuavaPrecinctInfoTable precinctInfoTable = new GuavaPrecinctInfoTable();

        State tennessee = ShapefileReader.read("src/main/resources/tn_2024_gen_prec/tn_2024_gen_cong_prec/tn_2024_gen_cong_prec.shp",
                "Tennesee",
                precinctInfoTable,
                null
        );

        String targetPrecinctId = "Johnson-:-1A Laurel";
        String targetColumnName = "GCON01IBRA";
        Integer targetValue = 4;

        assertThat(precinctInfoTable.getValueOf(targetPrecinctId, targetColumnName))
                .isEqualTo(targetValue);
        System.out.println("El precinto " + targetPrecinctId +
                " tiene en la columna " + targetColumnName +
                " el valor " + targetValue + ".");
    }
}
