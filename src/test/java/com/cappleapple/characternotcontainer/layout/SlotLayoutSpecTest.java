package com.cappleapple.characternotcontainer.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SlotLayoutSpecTest {
    @Test
    void bundledLayoutPreservesBodyAnchorsAndHandAlternation() throws Exception {
        SlotLayoutSpec spec = SlotLayoutLoader.bundled();

        assertEquals(319, spec.anchor("head").x);
        assertEquals(52, spec.anchor("head").y);
        assertEquals("hands", spec.targetFor("hands", 0));
        assertEquals("right_hand", spec.targetFor("hands", 1));
        assertEquals("hands", spec.targetFor("hands", 2));
        assertEquals("other", spec.targetFor("missing_anchor", 0));
    }

    @Test
    void customAnchorsSupportHorizontalAndVerticalWrapping() {
        SlotLayoutSpec spec = SlotLayoutSpec.defaults();
        spec.slots.put("shoulder", new SlotLayoutSpec.Anchor(250, 80, "vertical", 3, 24, null));
        spec.validate();

        SlotLayoutSpec.Anchor shoulder = spec.anchor("shoulder");
        assertEquals(new SlotLayoutSpec.GridOffset(0, 2), spec.offset(shoulder, 2));
        assertEquals(new SlotLayoutSpec.GridOffset(1, 0), spec.offset(shoulder, 3));
    }

    @Test
    void rejectsAlternatesThatDoNotExist() {
        SlotLayoutSpec spec = SlotLayoutSpec.defaults();
        spec.slots.get("hands").alternate = "missing";
        assertThrows(IllegalArgumentException.class, spec::validate);
    }
}
