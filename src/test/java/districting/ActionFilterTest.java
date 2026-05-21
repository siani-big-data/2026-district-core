package districting;

import org.junit.jupiter.api.Test;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ActionFilterTest {

    @Test
    void shouldMapCosineToAnEpochWithoutGaps() {
        ActionFilter filter = ActionFilter.create().epochLengthFactor(50);

        assertEquals(ActionFilter.EpochName.EXPLOITATIVE, filter.getEpochNameFromStep(567));
        assertEquals(ActionFilter.EpochName.EXPLORATIVE, filter.getEpochNameFromStep(410));
    }

    @Test
    void shouldNeverReturnNullForRegularPositiveSteps() {
        ActionFilter filter = ActionFilter.create();

        for (int step = 0; step < 1000; step++) {
            assertNotNull(filter.getEpochNameFromStep(step), "step " + step + " produced null epoch");
        }
    }
}
