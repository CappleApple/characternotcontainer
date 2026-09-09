package com.cappleapple.characternotcontainer.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchHoldTest {
    @Test void requiresAFullHoldAndSendsOnlyOnceUntilReleased() {
        ResearchHold<String> hold = new ResearchHold<>();
        for (int tick = 0; tick < 19; tick++) assertFalse(hold.tick("ring/0", true));
        assertTrue(hold.tick("ring/0", true));
        for (int tick = 0; tick < 40; tick++) assertFalse(hold.tick("ring/0", true));
        assertFalse(hold.tick("ring/0", false));
        for (int tick = 0; tick < 19; tick++) assertFalse(hold.tick("ring/0", true));
        assertTrue(hold.tick("ring/0", true));
    }

    @Test void changingSlotsRestartsTheHoldEvenForIdenticalRelics() {
        ResearchHold<String> hold = new ResearchHold<>();
        for (int tick = 0; tick < 19; tick++) hold.tick("ring/0", true);
        for (int tick = 0; tick < 19; tick++) assertFalse(hold.tick("ring/1", true));
        assertTrue(hold.tick("ring/1", true));
    }

    @Test void progressInterpolatesOnlyOnTheCurrentSourceAndClampsWhileWaitingForTheServer() {
        ResearchHold<String> hold = new ResearchHold<>();
        assertEquals(ResearchHold.Progress.NONE, hold.progress("ring/0"));
        hold.tick("ring/0", true);
        assertEquals(new ResearchHold.Progress(0, 1), hold.progress("ring/0"));
        hold.tick("ring/0", true);
        assertEquals(new ResearchHold.Progress(1, 2), hold.progress("ring/0"));
        assertEquals(ResearchHold.Progress.NONE, hold.progress("ring/1"));
        assertEquals(ResearchHold.Progress.NONE, hold.progress(null));
        for (int tick = 0; tick < 40; tick++) hold.tick("ring/0", true);
        assertEquals(new ResearchHold.Progress(20, 20), hold.progress("ring/0"));
    }

    @Test void changingSourcesOrReleasingClearsTheInterpolationStart() {
        ResearchHold<String> hold = new ResearchHold<>();
        for (int tick = 0; tick < 10; tick++) hold.tick("ring/0", true);
        hold.tick("ring/1", true);
        assertEquals(new ResearchHold.Progress(0, 1), hold.progress("ring/1"));
        hold.tick("ring/1", false);
        assertEquals(ResearchHold.Progress.NONE, hold.progress("ring/1"));
        hold.tick("ring/1", true);
        assertEquals(new ResearchHold.Progress(0, 1), hold.progress("ring/1"));
        hold.tick(null, false);
        assertEquals(ResearchHold.Progress.NONE, hold.progress("ring/1"));
    }

    @Test void LeavingAnItemOrReleasingTheKeyResetsProgress() {
        for (boolean leave : new boolean[]{false, true}) {
            ResearchHold<String> hold = new ResearchHold<>();
            for (int tick = 0; tick < 19; tick++) hold.tick("ring/0", true);
            assertFalse(hold.tick(leave ? null : "ring/0", leave));
            for (int tick = 0; tick < 19; tick++) assertFalse(hold.tick("ring/0", true));
            assertTrue(hold.tick("ring/0", true));
        }
    }
}
