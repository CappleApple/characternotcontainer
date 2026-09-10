package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.config.StatDefinition;
import net.minecraft.world.entity.ai.attributes.Attribute;

record ResolvedStat(StatDefinition definition, Attribute attribute) {}
