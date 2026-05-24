package districting;

import org.junit.jupiter.api.Test;
import siani.districting.architecture.engine.environment.actionfiltering.ActionFilter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ActionFilterTest {

    @Test
    void shouldMapCosineToAnphaseWithoutGaps() {
        ActionFilter filter = ActionFilter.create().phaseLengthFactor(50);

        assertEquals(ActionFilter.PhaseName.EXPLOITATIVE, filter.getPhaseNameFromStep(567));
        assertEquals(ActionFilter.PhaseName.EXPLORATIVE, filter.getPhaseNameFromStep(410));
    }

    @Test
    void shouldNeverReturnNullForRegularPositiveSteps() {
        ActionFilter filter = ActionFilter.create();

        for (int step = 0; step < 1000; step++) {
            assertNotNull(filter.getPhaseNameFromStep(step), "step " + step + " produced null phase");
        }
    }
}
