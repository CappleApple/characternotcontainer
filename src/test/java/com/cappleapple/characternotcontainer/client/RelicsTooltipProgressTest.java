package com.cappleapple.characternotcontainer.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RelicsTooltipProgressTest {
    @Test void nativeRendererSeesCharacterProgressAndInventoryProgressIsRestored() throws Exception {
        RelicsTooltipProgress bridge = new RelicsTooltipProgress(Handler.class);
        Handler.ticksCountOld = 3;
        Handler.ticksCount = 4;
        bridge.render(new ResearchHold.Progress(14, 15), () -> {
            assertEquals(14, Handler.ticksCountOld);
            assertEquals(15, Handler.ticksCount);
        });
        assertEquals(3, Handler.ticksCountOld);
        assertEquals(4, Handler.ticksCount);
    }

    @Test void nestedTooltipAndFailingRendererRestoreBothCounters() throws Exception {
        RelicsTooltipProgress bridge = new RelicsTooltipProgress(Handler.class);
        Handler.ticksCountOld = 7;
        Handler.ticksCount = 8;
        RuntimeException failure = new RuntimeException("tooltip renderer failed");
        assertSame(failure, assertThrows(RuntimeException.class, () ->
                bridge.render(new ResearchHold.Progress(10, 11), () -> {
                    bridge.render(ResearchHold.Progress.NONE, () -> {
                        assertEquals(0, Handler.ticksCountOld);
                        assertEquals(0, Handler.ticksCount);
                    });
                    assertEquals(10, Handler.ticksCountOld);
                    assertEquals(11, Handler.ticksCount);
                    throw failure;
                })));
        assertEquals(7, Handler.ticksCountOld);
        assertEquals(8, Handler.ticksCount);
    }

    @Test void unsupportedOptionalModCounterLayoutFailsBeforeAnyStateIsChanged() {
        assertThrows(ReflectiveOperationException.class, () -> new RelicsTooltipProgress(ChangedHandler.class));
        assertEquals(6, ChangedHandler.ticksCountOld);
    }

    private static class Handler {
        private static int ticksCountOld;
        private static int ticksCount;
    }

    private static class ChangedHandler {
        private static int ticksCountOld = 6;
        private static long ticksCount;
    }
}
