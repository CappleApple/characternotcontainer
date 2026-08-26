package com.cappleapple.characternotcontainer.config;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class GeneralConfigTest {
    @Test
    void characterKeyClosesTheCharacterScreenByDefault() {
        assertFalse(new GeneralConfig().characterKeyOpensInventory);
    }

    @Test
    void existingConfigsWithoutTheOptionUseTheClosingDefault() {
        GeneralConfig config = new Gson().fromJson("{}", GeneralConfig.class);

        assertFalse(config.characterKeyOpensInventory);
    }
}
