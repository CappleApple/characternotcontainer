package com.cappleapple.characternotcontainer.compat.puffishskills;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.gametest.GameTestPlayers;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(CharacterNotContainer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PufferfishSkillsGameTests {
    @GameTest(template = "empty")
    public static void readsUuidFromRealForgeAttributeReward(GameTestHelper helper) throws Exception {
        if (!ModList.get().isLoaded("puffish_skills")) { helper.succeed(); return; }
        var player = GameTestPlayers.create(helper);
        Class<?> rewardType = Class.forName("net.puffish.skillsmod.reward.builtin.AttributeReward");
        var constructor = rewardType.getDeclaredConstructor(Attribute.class, float.class, AttributeModifier.Operation.class);
        constructor.setAccessible(true);
        Object reward = constructor.newInstance(Attributes.MAX_HEALTH, 2.0F, AttributeModifier.Operation.ADDITION);
        var createIds = rewardType.getDeclaredMethod("createMissingUUIDs", int.class);
        createIds.setAccessible(true); createIds.invoke(reward, 1);
        var ids = rewardType.getDeclaredField("uuids"); ids.setAccessible(true);
        UUID uuid = (UUID)((List<?>)ids.get(reward)).get(0);
        player.getAttribute(Attributes.MAX_HEALTH).addTransientModifier(
                new AttributeModifier(uuid, "skill", 2.0, AttributeModifier.Operation.ADDITION));
        Class<?> bridgeType = Class.forName(PufferfishSkillsSourceBridge.class.getName() + "$Bridge");
        var bridgeConstructor = bridgeType.getDeclaredConstructor(); bridgeConstructor.setAccessible(true);
        Object bridge = bridgeConstructor.newInstance();
        var add = bridgeType.getDeclaredMethod("addRewardSources", List.class,
                net.minecraft.server.level.ServerPlayer.class, Object.class,
                ModifierSourcesResponsePayload.Source.class, PufferfishSkillsSourceBridge.LevelledTitle.class);
        add.setAccessible(true);
        List<PufferfishSkillsSourceBridge.ActiveReward> active = new ArrayList<>();
        add.invoke(bridge, active, player, reward, PufferfishSkillsSourceBridge.sourceForTitle(Component.literal("Vitality I")),
                PufferfishSkillsSourceBridge.levelledTitle("Vitality I").orElseThrow());
        var entries = PufferfishSkillsSourceBridge.groupedEntries(active);
        helper.assertTrue(entries.size() == 1 && entries.get(0).modifierId().equals(uuid)
                && entries.get(0).sources().get(0).fallback().equals("Vitality I"),
                "Forge reward UUID was not attributed to its actual skill title");
        helper.succeed();
    }
}
