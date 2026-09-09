package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.compat.relics.RelicsIntegration;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import java.lang.reflect.Method;

final class RelicsResearchClient {
    private static boolean checked;
    private static KeyMapping researchKey;
    private static Method openScreen;
    private static boolean tooltipChecked;
    private static RelicsTooltipProgress tooltipProgress;
    private static final java.util.Set<Integer> pressedScanCodes = new java.util.HashSet<>();

    static void keyboardEvent(int scanCode, boolean pressed) {
        if (pressed) pressedScanCodes.add(scanCode);
        else pressedScanCodes.remove(scanCode);
    }

    static void resetInput() { pressedScanCodes.clear(); }

    private RelicsResearchClient() {}

    static boolean available(ItemStack stack) {
        if (!RelicsIntegration.isRelic(stack)) return false;
        if (!checked) {
            checked = true;
            try {
                researchKey = (KeyMapping)Class.forName("it.hurts.sskirillss.relics.init.HotkeyRegistry")
                        .getField("RESEARCH_RELIC").get(null);
                openScreen = Class.forName("it.hurts.sskirillss.relics.client.screen.description.misc.DescriptionUtils")
                        .getMethod("openCachedScreen", Class.forName("it.hurts.sskirillss.relics.items.relics.base.IRelicItem"),
                                Player.class, int.class, Screen.class);
            } catch (ReflectiveOperationException | LinkageError exception) {
                CharacterNotContainer.LOGGER.warn("Relics client research integration is unavailable", exception);
            }
        }
        return researchKey != null && openScreen != null;
    }


    static void withTooltipProgress(ResearchHold.Progress progress, Runnable renderTooltip) {
        if (!tooltipChecked) {
            tooltipChecked = true;
            try {
                tooltipProgress = new RelicsTooltipProgress(Class.forName(
                        "it.hurts.sskirillss.relics.client.handlers.DescriptionHandler"));
            } catch (ReflectiveOperationException | LinkageError | RuntimeException exception) {
                CharacterNotContainer.LOGGER.warn("Relics tooltip progress integration is unavailable", exception);
            }
        }
        if (tooltipProgress == null) renderTooltip.run();
        else tooltipProgress.render(progress, renderTooltip);
    }

    static boolean isHeld() {
        if (!Minecraft.getInstance().isWindowActive()) {
            resetInput();
            return false;
        }
        if (researchKey == null || researchKey.isUnbound()
                || !researchKey.getKeyModifier().isActive(KeyConflictContext.UNIVERSAL)) return false;
        InputConstants.Key key = researchKey.getKey();
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, key.getValue()) == GLFW.GLFW_PRESS;
        }
        if (key.getType() == InputConstants.Type.SCANCODE) {
            return pressedScanCodes.contains(key.getValue());
        }
        return InputConstants.isKeyDown(window, key.getValue());
    }

    static boolean matchesKey(int key, int scan) { return researchKey != null && researchKey.getKeyModifier().isActive(KeyConflictContext.UNIVERSAL) && researchKey.matches(key, scan); }
    static boolean matchesMouse(int button) { return researchKey != null && researchKey.getKeyModifier().isActive(KeyConflictContext.UNIVERSAL) && researchKey.matchesMouse(button); }

    static boolean open(ItemStack stack, Screen parent) {
        if (!available(stack)) return false;
        try {
            openScreen.invoke(null, stack.getItem(), Minecraft.getInstance().player, 0, parent);
            return true;
        } catch (ReflectiveOperationException | LinkageError exception) {
            CharacterNotContainer.LOGGER.warn("Could not open Relics research screen", exception);
            return false;
        }
    }
}
