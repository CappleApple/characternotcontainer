package com.cappleapple.characternotcontainer.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.serialization.JsonOps;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.config.StatDefinition;
import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.layout.EquipmentLayoutLoader;
import com.cappleapple.characternotcontainer.layout.EquipmentPickerLayout;
import com.cappleapple.characternotcontainer.layout.EquipmentScreenSpec;
import com.cappleapple.characternotcontainer.layout.ScreenRect;
import com.cappleapple.characternotcontainer.layout.SlotLayoutLoader;
import com.cappleapple.characternotcontainer.layout.SlotLayoutSpec;
import com.cappleapple.characternotcontainer.equipment.PlayerInventoryAccess;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentRequestPayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentResponsePayload;
import com.cappleapple.characternotcontainer.network.ModifierSourcesRequestPayload;
import com.cappleapple.characternotcontainer.network.ModifierSourcesResponsePayload;
import net.minecraft.ChatFormatting;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class CharacterEquipmentScreen extends Screen {
    private static final int PANEL = 0xE7181C21;
    private static final int STATS_PANEL = 0xE70D1014;
    private static final int BORDER = 0xFF6D737A;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int CURIO_SLOT_BACKGROUND = 0xC421262C;
    private static final int CURIO_SLOT_BORDER = 0xCC6D737A;
    private static final int COSMETIC_CURIO_SLOT_BACKGROUND = 0xD04B2D63;
    private static final int COSMETIC_CURIO_SLOT_BORDER = 0xD09D70B5;
    private static final CuriosClientIntegration CURIOS = loadCurios();
    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 320;
    private static final int STATS_ROW_HEIGHT = 26;
    private static final ScreenRect SCREEN_BACKGROUND = new ScreenRect(0, 0, 400, 320);
    private static final ScreenRect STATS_LIST = new ScreenRect(16, 38, 134, 258);
    private static final ScreenRect STATS_SCROLLBAR = new ScreenRect(150, 38, 8, 258);
    private static final ScreenRect PLAYER_PREVIEW = new ScreenRect(170, 12, 220, 296);
    private static final ScreenRect PICKER_FALLBACK = new ScreenRect(166, 78, 214, 160);

    private final Player player;
    private EquipmentScreenSpec spec;
    private SlotLayoutSpec slotLayout;
    private double layoutScale;
    private int layoutX;
    private int layoutY;
    private PickerTarget picker;
    private List<CuriosClientIntegration.CurioSlotView> curiosSlots = List.of();
    private List<CuriosClientIntegration.CurioSlotView> functionalCuriosSlots = List.of();
    private boolean showCosmetics;
    private int refreshTicks;
    private int statsScroll;
    private int pickerScroll;
    private int pickerSearchId;
    private ScreenRect pickerAnchor;
    private List<NearbyEquipmentResponsePayload.Entry> nearbyEquipment = List.of();
    private Map<ModifierKey, List<ModifierSourcesResponsePayload.Source>> modifierSourceHints = Map.of();
    private HoverTooltip hoverTooltip;
    private PickerTarget hoveredEquipmentTarget;
    private TextureTarget playerCompositeTarget;

    public CharacterEquipmentScreen(Player player) {
        super(Component.translatable("gui.characternotcontainer.character"));
        this.player = player;
    }

    @Override
    protected void init() {
        spec = EquipmentLayoutLoader.load();
        slotLayout = SlotLayoutLoader.load();
        layoutScale = Math.min(1.0D, Math.min((width - 12.0D) / SCREEN_WIDTH, (height - 12.0D) / SCREEN_HEIGHT));
        layoutScale = Math.max(0.4D, layoutScale);
        layoutX = (width - scaled(SCREEN_WIDTH)) / 2;
        layoutY = (height - scaled(SCREEN_HEIGHT)) / 2;
        refreshCurios();
        if (serverSupports(ModifierSourcesRequestPayload.TYPE.id())) {
            PacketDistributor.sendToServer(ModifierSourcesRequestPayload.INSTANCE);
        }
    }

    @Override
    public void tick() {
        if (minecraft == null || minecraft.player != player || !player.isAlive()) {
            onClose();
            return;
        }
        if (++refreshTicks >= 10) {
            refreshTicks = 0;
            refreshCurios();
        }
    }

    private void refreshCurios() {
        functionalCuriosSlots = CURIOS == null ? List.of() : CURIOS.slots(player, false);
        curiosSlots = CURIOS == null ? List.of()
                : showCosmetics ? CURIOS.slots(player, true) : functionalCuriosSlots;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        hoverTooltip = null;
        renderGroup(graphics, 0.0F, () -> renderPanels(graphics));
        renderPlayerGroup(graphics, mouseX, mouseY);
        List<PlacedCurio> placedCurios = placedCurios();
        hoveredEquipmentTarget = picker == null ? hoveredEquipmentTarget(mouseX, mouseY, placedCurios) : null;
        renderGroup(graphics, 200.0F, () -> {
            renderPlayerPreviewFrame(graphics);
            renderChangedAttributes(graphics, mouseX, mouseY);
            renderVanillaEquipment(graphics, mouseX, mouseY);
            renderCurios(graphics, placedCurios, mouseX, mouseY);
            renderCosmeticsToggle(graphics, mouseX, mouseY);
        });
        if (picker != null) {
            renderGroup(graphics, 400.0F, () -> renderPicker(graphics, mouseX, mouseY));
        }
        if (hoverTooltip != null) hoverTooltip.render(graphics, mouseX, mouseY);
    }

    private void renderPlayerGroup(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.flush();
        Minecraft minecraft = Minecraft.getInstance();
        RenderTarget mainTarget = minecraft.getMainRenderTarget();
        TextureTarget compositeTarget = playerCompositeTarget(mainTarget);
        compositeTarget.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        compositeTarget.clear(Minecraft.ON_OSX);
        compositeTarget.bindWrite(true);
        try {
            renderGroup(graphics, 0.0F, () -> renderPlayer(graphics, mouseX, mouseY));
            // AzureLib armor models intentionally obtain Minecraft's world
            // render buffers instead of using the VertexConsumer supplied by
            // InventoryScreen. Finish both buffers before restoring the main
            // framebuffer so every replacement armor layer stays in the
            // isolated player composite.
            minecraft.renderBuffers().bufferSource().endBatch();
            minecraft.renderBuffers().outlineBufferSource().endOutlineBatch();
            graphics.flush();
        } finally {
            mainTarget.bindWrite(true);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        compositeTarget.blitToScreen(mainTarget.width, mainTarget.height, false);
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    private TextureTarget playerCompositeTarget(RenderTarget mainTarget) {
        if (playerCompositeTarget == null) {
            playerCompositeTarget = new TextureTarget(mainTarget.width, mainTarget.height, true, Minecraft.ON_OSX);
        } else if (playerCompositeTarget.width != mainTarget.width || playerCompositeTarget.height != mainTarget.height) {
            playerCompositeTarget.resize(mainTarget.width, mainTarget.height, Minecraft.ON_OSX);
        }
        return playerCompositeTarget;
    }

    @Override
    public void removed() {
        if (playerCompositeTarget != null) {
            playerCompositeTarget.destroyBuffers();
            playerCompositeTarget = null;
        }
        super.removed();
    }

    private void renderPanels(GuiGraphics graphics) {
        ScreenRect background = screenRect(SCREEN_BACKGROUND);
        if (!blitOptionalSprite(graphics, CharacterGuiSprites.SCREEN_BACKGROUND, background)) {
            graphics.fill(background.x(), background.y(), background.x() + background.width(), background.y() + background.height(), PANEL);
            outline(graphics, background, BORDER);
        }
        blitOptionalSprite(graphics, CharacterGuiSprites.PLAYER_PREVIEW_BACKGROUND, screenRect(PLAYER_PREVIEW));
    }

    private void renderPlayerPreviewFrame(GuiGraphics graphics) {
        blitOptionalSprite(graphics, CharacterGuiSprites.PLAYER_PREVIEW_FRAME, screenRect(PLAYER_PREVIEW));
    }

    private void renderPlayer(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenRect preview = screenRect(PLAYER_PREVIEW);
        int entityScale = Math.max(30, (int)Math.round(preview.height() * 0.43D));
        float lookOriginX = preview.x() + preview.width() / 2.0F;
        float lookOriginY = preview.y() + preview.height() * 0.30F;
        float sensitivity = Math.max(20.0F, preview.width() * 0.12F);
        float horizontalAngle = (float)Math.atan((lookOriginX - mouseX) / sensitivity);
        float verticalAngle = (float)Math.atan((lookOriginY - mouseY) / sensitivity);
        graphics.enableScissor(preview.x(), preview.y(), preview.x() + preview.width(), preview.y() + preview.height());
        try {
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, preview.x(), preview.y(),
                    preview.x() + preview.width(), preview.y() + preview.height(), entityScale, 0.0625F,
                    horizontalAngle, verticalAngle, player);
            graphics.flush();
        } finally {
            graphics.disableScissor();
        }
    }

    public void acceptNearbyEquipment(NearbyEquipmentResponsePayload response) {
        if (picker == null || response.searchId() != pickerSearchId) return;
        nearbyEquipment = response.enabled() ? response.entries() : List.of();
        pickerScroll = 0;
    }

    public void acceptModifierSources(ModifierSourcesResponsePayload response) {
        Map<ModifierKey, List<ModifierSourcesResponsePayload.Source>> hints = new HashMap<>();
        response.entries().forEach(entry -> hints.put(new ModifierKey(entry.attributeId(), entry.modifierId()), entry.sources()));
        modifierSourceHints = Map.copyOf(hints);
    }

    private void renderVanillaEquipment(GuiGraphics graphics, int mouseX, int mouseY) {
        for (EquipmentSlot slot : bodySlots()) {
            ScreenRect hitbox = bodyBounds(slot);
            boolean hovered = hoveredEquipmentTarget instanceof VanillaTarget target && target.slot == slot;
            boolean selected = picker instanceof VanillaTarget target && target.slot == slot;
            blitOptionalSprite(graphics, CharacterGuiSprites.EQUIPMENT_SLOT, hitbox);
            if (selected) {
                if (!blitOptionalSprite(graphics, CharacterGuiSprites.EQUIPMENT_SLOT_SELECTED, hitbox)) {
                    outline(graphics, hitbox, 0xFFE5BD68);
                }
            } else if (hovered) {
                if (!blitOptionalSprite(graphics, CharacterGuiSprites.EQUIPMENT_SLOT_HOVERED, hitbox)) {
                    outline(graphics, hitbox, 0x99FFFFFF);
                }
            } else if (CharacterConfigManager.general().debugEquipmentHitboxes) {
                outline(graphics, hitbox, 0x6688CCFF);
            }
            ItemStack equipped = player.getItemBySlot(slot);
            int iconX = hitbox.x() + (hitbox.width() - 16) / 2;
            int iconY = hitbox.y() + (hitbox.height() - 16) / 2;
            if (equipped.isEmpty()) {
                int iconSize = Math.max(16, Math.min(hitbox.width(), hitbox.height()));
                iconX = hitbox.x() + (hitbox.width() - iconSize) / 2;
                iconY = hitbox.y() + (hitbox.height() - iconSize) / 2;
                var sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(emptyIcon(slot));
                graphics.blit(iconX, iconY, 0, iconSize, iconSize, sprite);
            } else if (CharacterConfigManager.general().showEquippedItemIcons) {
                graphics.renderItem(equipped, iconX, iconY);
            }
            if (hovered) {
                List<Component> lines = new ArrayList<>();
                lines.add(slotName(slot).copy().withStyle(ChatFormatting.GOLD));
                if (!equipped.isEmpty()) lines.add(equipped.getHoverName());
                hoverTooltip = HoverTooltip.components(lines);
            }
        }
    }

    private void renderCurios(GuiGraphics graphics, List<PlacedCurio> placements, int mouseX, int mouseY) {
        for (PlacedCurio placement : placements) {
            boolean hovered = hoveredEquipmentTarget instanceof CurioTarget target
                    && target.slot.type().equals(placement.slot.type()) && target.slot.index() == placement.slot.index()
                    && target.slot.cosmetic() == placement.slot.cosmetic();
            boolean selected = picker instanceof CurioTarget target
                    && target.slot.type().equals(placement.slot.type()) && target.slot.index() == placement.slot.index()
                    && target.slot.cosmetic() == placement.slot.cosmetic();
            int slotBackground = placement.slot.cosmetic()
                    ? COSMETIC_CURIO_SLOT_BACKGROUND : CURIO_SLOT_BACKGROUND;
            ResourceLocation baseSprite = placement.slot.cosmetic()
                    ? CharacterGuiSprites.COSMETIC_CURIO_SLOT : CharacterGuiSprites.CURIO_SLOT;
            boolean customBase = blitOptionalSprite(graphics, baseSprite, placement.bounds);
            if (!customBase) {
                graphics.fill(placement.bounds.x(), placement.bounds.y(), placement.bounds.x() + placement.bounds.width(),
                        placement.bounds.y() + placement.bounds.height(), slotBackground);
                outline(graphics, placement.bounds, CharacterConfigManager.general().debugEquipmentHitboxes
                        && !selected && !hovered ? 0xFF88CCFF
                        : placement.slot.cosmetic() ? COSMETIC_CURIO_SLOT_BORDER : CURIO_SLOT_BORDER);
            }
            ResourceLocation stateSprite = selected
                    ? placement.slot.cosmetic() ? CharacterGuiSprites.COSMETIC_CURIO_SLOT_SELECTED
                    : CharacterGuiSprites.CURIO_SLOT_SELECTED
                    : hovered ? placement.slot.cosmetic() ? CharacterGuiSprites.COSMETIC_CURIO_SLOT_HOVERED
                    : CharacterGuiSprites.CURIO_SLOT_HOVERED : null;
            if (stateSprite != null && !blitOptionalSprite(graphics, stateSprite, placement.bounds)) {
                outline(graphics, placement.bounds, selected ? 0xFFE5BD68 : 0xFFFFFFFF);
            } else if (stateSprite == null && CharacterConfigManager.general().debugEquipmentHitboxes && customBase) {
                outline(graphics, placement.bounds, 0xFF88CCFF);
            }
            if (placement.slot.stack().isEmpty()) {
                graphics.blit(curioIconTexture(placement.slot.icon()), placement.bounds.x() + 1, placement.bounds.y() + 1, 0.0F, 0.0F,
                        16, 16, 16, 16);
            } else {
                graphics.renderItem(placement.slot.stack(), placement.bounds.x() + 1, placement.bounds.y() + 1);
            }
            if (hovered) {
                Component name = Component.translatableWithFallback("curios.identifier." + placement.slot.type(), titleCase(placement.slot.type()));
                List<Component> lines = new ArrayList<>();
                lines.add(name.copy().withStyle(ChatFormatting.GOLD));
                if (!placement.slot.stack().isEmpty()) lines.add(placement.slot.stack().getHoverName());
                hoverTooltip = HoverTooltip.components(lines);
            }
        }
    }

    private void renderCosmeticsToggle(GuiGraphics graphics, int mouseX, int mouseY) {
        if (CURIOS == null) return;
        ScreenRect bounds = cosmeticsToggleBounds();
        boolean hovered = picker == null && bounds.contains(mouseX, mouseY);
        ResourceLocation normalSprite = showCosmetics
                ? CharacterGuiSprites.SHOW_EQUIPMENT_BUTTON : CharacterGuiSprites.SHOW_COSMETICS_BUTTON;
        ResourceLocation hoveredSprite = showCosmetics
                ? CharacterGuiSprites.SHOW_EQUIPMENT_BUTTON_HOVERED : CharacterGuiSprites.SHOW_COSMETICS_BUTTON_HOVERED;
        boolean custom = hovered && blitOptionalSprite(graphics, hoveredSprite, bounds);
        if (!custom) custom = blitOptionalSprite(graphics, normalSprite, bounds);
        if (!custom) {
            int background = showCosmetics ? 0xE05B327A : hovered ? 0xE0444B53 : 0xE02A3036;
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), background);
            outline(graphics, bounds, showCosmetics ? COSMETIC_CURIO_SLOT_BORDER : hovered ? 0xFFFFFFFF : BORDER);
            graphics.drawCenteredString(font, showCosmetics ? "E" : "C",
                    bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - font.lineHeight) / 2 + 1, TEXT);
        } else if (hovered && !hasGuiSprite(hoveredSprite)) {
            outline(graphics, bounds, 0xFFFFFFFF);
        }
        if (hovered) {
            hoverTooltip = HoverTooltip.components(List.of(Component.translatable(showCosmetics
                    ? "gui.characternotcontainer.show_equipment" : "gui.characternotcontainer.show_cosmetics")));
        }
    }

    private ScreenRect cosmeticsToggleBounds() {
        int size = 18;
        int margin = Math.max(6, scaled(10));
        return new ScreenRect(layoutX + scaled(SCREEN_WIDTH) - margin - size, layoutY + margin, size, size);
    }

    private List<PlacedCurio> placedCurios() {
        Map<String, Integer> anchorOccurrences = new HashMap<>();
        Map<String, Integer> placementUsage = new HashMap<>();
        List<PlacedCurio> result = new ArrayList<>();
        for (CuriosClientIntegration.CurioSlotView slot : curiosSlots) {
            String type = slot.type().toLowerCase(Locale.ROOT);
            String configuredAnchor = spec.anchorFor(type);
            int occurrence = anchorOccurrences.merge(configuredAnchor, 1, Integer::sum) - 1;
            String targetAnchor = slotLayout.targetFor(configuredAnchor, occurrence);
            int used = placementUsage.merge(targetAnchor, 1, Integer::sum) - 1;
            SlotLayoutSpec.Anchor anchor = slotLayout.anchor(targetAnchor);
            SlotLayoutSpec.GridOffset offset = slotLayout.offset(anchor, used);
            ScreenRect base = screenRect(new ScreenRect(anchor.x, anchor.y, 18, 18));
            int spacing = anchor.spacing;
            int x = base.x() + offset.x() * spacing;
            int y = base.y() + offset.y() * spacing;
            result.add(new PlacedCurio(slot, new ScreenRect(x, y, 18, 18)));
        }
        return result;
    }

    private PickerTarget hoveredEquipmentTarget(double mouseX, double mouseY, List<PlacedCurio> placements) {
        PickerTarget target = null;
        for (EquipmentSlot slot : bodySlots()) {
            if (bodyBounds(slot).contains(mouseX, mouseY)) target = new VanillaTarget(slot);
        }
        for (PlacedCurio placement : placements) {
            if (placement.bounds.contains(mouseX, mouseY)) target = new CurioTarget(placement.slot);
        }
        return target;
    }

    private void renderChangedAttributes(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenRect viewport = screenRect(STATS_LIST);
        int rowHeight = Math.max(20, scaled(STATS_ROW_HEIGHT));
        List<ChangedStat> changed = changedStats();
        int visibleRows = Math.max(1, (viewport.height() + rowHeight - 1) / rowHeight);
        statsScroll = Math.max(0, Math.min(statsScroll, Math.max(0, changed.size() - visibleRows)));
        int displayedRows = Math.min(visibleRows, changed.size());
        if (displayedRows > 0) {
            int panelHeight = Math.min(viewport.height(), displayedRows * rowHeight);
            ScreenRect panel = new ScreenRect(viewport.x(), viewport.y(), viewport.width(), panelHeight);
            if (!blitOptionalSprite(graphics, CharacterGuiSprites.STATS_PANEL, panel)) {
                graphics.fill(panel.x(), panel.y(), panel.x() + panel.width(), panel.y() + panel.height(), STATS_PANEL);
                outline(graphics, panel, 0xFF444B53);
            }
        }
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height());
        for (int row = statsScroll; row < changed.size() && row < statsScroll + visibleRows; row++) {
            ChangedStat changedStat = changed.get(row);
            int y = viewport.y() + (row - statsScroll) * rowHeight;
            ScreenRect rowRect = new ScreenRect(viewport.x(), y, viewport.width(), rowHeight);
            if (rowRect.contains(mouseX, mouseY)
                    && !blitOptionalSprite(graphics, CharacterGuiSprites.STATS_ROW_HOVERED, rowRect)) {
                graphics.fill(rowRect.x(), rowRect.y(), rowRect.x() + rowRect.width(), rowRect.y() + rowRect.height(), 0x553F4852);
            }
            renderAttributeIcon(graphics, changedStat.stat, viewport.x() + scaled(4), y + Math.max(2, (rowHeight - 16) / 2));
            String name = displayName(changedStat.stat);
            int nameX = viewport.x() + scaled(25);
            int valueRight = viewport.x() + viewport.width() - scaled(4);
            String value = changedStat.stat.definition().effectiveFormat().format(changedStat.current, changedStat.stat.definition());
            int valueX = valueRight - font.width(value);
            graphics.drawString(font, font.plainSubstrByWidth(name, Math.max(10, valueX - nameX - 4)), nameX, y + (rowHeight - 8) / 2, TEXT, false);
            graphics.drawString(font, value, valueX, y + (rowHeight - 8) / 2, TEXT, false);
            if (rowRect.contains(mouseX, mouseY)) {
                List<SourceTooltipLine> sources = modifierSources(changedStat);
                if (!sources.isEmpty()) hoverTooltip = HoverTooltip.sources(sources);
            }
        }
        graphics.disableScissor();
        renderStatsScrollbar(graphics, viewport, visibleRows, changed.size());
    }

    private List<ChangedStat> changedStats() {
        List<ChangedStat> result = new ArrayList<>();
        for (ResolvedStat stat : ResolvedStatCatalog.stats()) {
            if (!stat.definition().visible) continue;
            AttributeInstance instance = player.getAttribute(stat.attribute());
            if (instance != null) {
                ChangedStat changed = changedStat(stat, instance, true);
                if (Math.abs(changed.current - changed.base) > 0.0000001D) result.add(changed);
            }
        }
        return result;
    }

    private ChangedStat changedStat(ResolvedStat stat, AttributeInstance instance, boolean includeEquipmentFallback) {
        Map<ResourceLocation, AttributeModifier> modifiers = new LinkedHashMap<>();
        instance.getModifiers().forEach(modifier -> modifiers.put(modifier.id(), modifier));
        if (includeEquipmentFallback && Math.abs(instance.getValue() - instance.getBaseValue()) <= 0.0000001D) {
            vanillaEquipmentContributions(stat).forEach(contribution -> modifiers.putIfAbsent(contribution.modifier.id(), contribution.modifier));
        }
        double current = modifiers.size() == instance.getModifiers().size()
                ? instance.getValue() : AttributeValueCalculator.calculate(instance, modifiers.values());
        return new ChangedStat(stat, instance.getBaseValue(), current, modifiers);
    }

    private List<Contribution> equipmentContributions(ResolvedStat stat) {
        List<Contribution> result = vanillaEquipmentContributions(stat);
        AttributeInstance instance = player.getAttribute(stat.attribute());
        if (CURIOS != null) CURIOS.contributions(player, stat.attribute())
                .forEach(contribution -> addContribution(result, contribution.stack(), contribution.modifier()));
        if (instance != null) {
            for (CuriosClientIntegration.CurioSlotView slot : functionalCuriosSlots) {
                if (!slot.stack().isEmpty()) addComponentContributions(result, slot.stack(), stat.attribute(), instance);
            }
        }
        return result;
    }

    private List<Contribution> vanillaEquipmentContributions(ResolvedStat stat) {
        List<Contribution> result = new ArrayList<>();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND)) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            stack.forEachModifier(slot, (attribute, modifier) -> {
                if (attribute.equals(stat.attribute())) addContribution(result, stack, modifier);
            });
        }
        return result;
    }

    private static void addContribution(List<Contribution> result, ItemStack stack, AttributeModifier modifier) {
        boolean duplicate = result.stream().anyMatch(existing -> existing.modifier.id().equals(modifier.id())
                && ItemStack.isSameItemSameComponents(existing.stack, stack));
        if (!duplicate) result.add(new Contribution(stack.copy(), modifier));
    }

    private void addComponentContributions(List<Contribution> result, ItemStack stack, Holder<Attribute> attribute,
                                           AttributeInstance instance) {
        ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
        if (attributeId == null) return;
        RegistryOps<JsonElement> ops = player.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (TypedDataComponent<?> component : stack.getComponents()) {
            component.encodeValue(ops).result().ifPresent(encoded -> collectModifierIds(encoded, attributeId.toString(), modifierId -> {
                AttributeModifier activeModifier = instance.getModifier(modifierId);
                if (activeModifier != null) addContribution(result, stack, activeModifier);
            }));
        }
    }

    private static void collectModifierIds(JsonElement element, String attributeId, Consumer<ResourceLocation> consumer) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(child -> collectModifierIds(child, attributeId, consumer));
            return;
        }
        if (!element.isJsonObject()) return;
        JsonObject object = element.getAsJsonObject();
        String encodedAttribute = stringProperty(object, "attribute");
        if (encodedAttribute == null) encodedAttribute = stringProperty(object, "type");
        if (attributeId.equals(encodedAttribute)) {
            String modifierId = stringProperty(object, "id");
            if (modifierId == null && object.has("modifier") && object.get("modifier").isJsonObject()) {
                modifierId = stringProperty(object.getAsJsonObject("modifier"), "id");
            }
            ResourceLocation parsed = modifierId == null ? null : ResourceLocation.tryParse(modifierId);
            if (parsed != null) consumer.accept(parsed);
        }
        object.entrySet().forEach(entry -> collectModifierIds(entry.getValue(), attributeId, consumer));
    }

    private static String stringProperty(JsonObject object, String name) {
        JsonElement value = object.get(name);
        return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString() ? value.getAsString() : null;
    }

    private List<SourceTooltipLine> modifierSources(ChangedStat changed) {
        ResolvedStat stat = changed.stat;
        List<SourceTooltipLine> lines = new ArrayList<>();
        lines.add(new SourceTooltipLine(ItemStack.EMPTY,
                Component.literal(displayName(stat)).withStyle(ChatFormatting.GOLD)));
        AttributeInstance instance = player.getAttribute(stat.attribute());
        if (instance == null) return lines;
        Set<ResourceLocation> explained = new HashSet<>();
        for (Contribution contribution : equipmentContributions(stat)) {
            AttributeModifier active = changed.modifiers.get(contribution.modifier.id());
            if (active != null) {
                addSource(lines, contribution.stack.copy(), sourceWithAmount(
                        contribution.stack.getHoverName(), active, stat.definition(), instance));
                explained.add(active.id());
            }
        }

        for (var effectInstance : player.getActiveEffects()) {
            effectInstance.getEffect().value().createModifiers(effectInstance.getAmplifier(), (attribute, modifier) -> {
                AttributeModifier active = changed.modifiers.get(modifier.id());
                if (attribute.equals(stat.attribute()) && active != null) {
                    addSource(lines, ItemStack.EMPTY, sourceWithAmount(
                            effectInstance.getEffect().value().getDisplayName(), active, stat.definition(), instance));
                    explained.add(active.id());
                }
            });
        }

        ResourceLocation attributeId = BuiltInRegistries.ATTRIBUTE.getKey(stat.attribute().value());
        if (attributeId != null) {
            for (AttributeModifier modifier : changed.modifiers.values()) {
                if (explained.contains(modifier.id())) continue;
                List<ModifierSourcesResponsePayload.Source> hints = modifierSourceHints.getOrDefault(
                        new ModifierKey(attributeId, modifier.id()), List.of());
                boolean identified = false;
                for (ModifierSourcesResponsePayload.Source source : hints) {
                    Component name = sourceName(source);
                    if (name != null) {
                        addSource(lines, source.stack(), sourceWithAmount(name, modifier, stat.definition(), instance));
                        identified = true;
                    }
                }
                if (identified) explained.add(modifier.id());
            }
        }
        addUnknownRemainder(lines, changed, explained, instance);
        return lines;
    }

    private static void addSource(List<SourceTooltipLine> lines, ItemStack stack, Component text) {
        boolean duplicate = lines.stream().anyMatch(line -> line.text.getString().equals(text.getString())
                && (stack.isEmpty() && line.stack.isEmpty() || ItemStack.isSameItemSameComponents(stack, line.stack)));
        if (!duplicate) lines.add(new SourceTooltipLine(stack, text));
    }

    private static Component sourceName(ModifierSourcesResponsePayload.Source source) {
        if (!source.translationKey().isBlank()) {
            return Component.translatableWithFallback(source.translationKey(), source.fallback());
        }
        if (!source.fallback().isBlank()) return Component.literal(source.fallback());
        return source.stack().isEmpty() ? null : source.stack().getHoverName();
    }

    private static Component sourceWithAmount(Component source, AttributeModifier modifier, StatDefinition definition,
                                              AttributeInstance instance) {
        return source.copy().append(Component.literal("  ")).append(modifierAmount(modifier, definition, instance));
    }

    private static Component modifierAmount(AttributeModifier modifier, StatDefinition definition,
                                            AttributeInstance instance) {
        double displayedAmount = modifier.operation() == AttributeModifier.Operation.ADD_VALUE
                ? AttributeValueCalculator.standaloneAddValue(instance, modifier)
                : modifier.amount();
        Component value = switch (modifier.operation()) {
            case ADD_VALUE -> Component.literal(signed(definition.effectiveFormat().format(displayedAmount, definition),
                    displayedAmount));
            case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> percentageModifierAmount(
                    modifier.operation(), modifier.amount());
        };
        return value.copy().withStyle(displayedAmount < 0.0D ? ChatFormatting.RED : ChatFormatting.GREEN);
    }

    private static void addUnknownRemainder(List<SourceTooltipLine> lines, ChangedStat changed,
                                            Set<ResourceLocation> explained, AttributeInstance instance) {
        List<AttributeModifier> unknown = changed.modifiers.values().stream()
                .filter(modifier -> !explained.contains(modifier.id()))
                .toList();
        if (unknown.isEmpty()) return;
        List<AttributeModifier> known = changed.modifiers.values().stream()
                .filter(modifier -> explained.contains(modifier.id()))
                .toList();
        double knownValue = AttributeValueCalculator.calculate(instance, known);
        double difference = changed.current - knownValue;
        if (Math.abs(difference) <= 0.0000001D) return;

        boolean onlyMultiplicative = unknown.stream()
                .allMatch(modifier -> modifier.operation() != AttributeModifier.Operation.ADD_VALUE);
        String amount = onlyMultiplicative && Math.abs(knownValue) > 0.0000001D
                ? signedPercent(changed.current / knownValue - 1.0D)
                : signed(changed.stat.definition().effectiveFormat().format(difference, changed.stat.definition()), difference);
        Component text = Component.translatable("gui.characternotcontainer.unknown_source")
                .append(Component.literal("  "))
                .append(Component.literal(amount).withStyle(difference < 0.0D ? ChatFormatting.RED : ChatFormatting.GREEN));
        addSource(lines, new ItemStack(Items.ENDER_EYE), text);
    }

    private static String signed(String formatted, double value) {
        return value > 0.0D ? "+" + formatted : formatted;
    }

    private static String signedPercent(double value) {
        String formatted = BigDecimal.valueOf(value * 100.0D)
                .setScale(2, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
        return value > 0.0D ? "+" + formatted : formatted;
    }

    static Component percentageModifierAmount(AttributeModifier.Operation operation, double value) {
        String percentage = signedPercent(value);
        return operation == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                ? Component.translatable("gui.characternotcontainer.stacking", percentage)
                : Component.literal(percentage);
    }

    private void renderAttributeIcon(GuiGraphics graphics, ResolvedStat stat, int x, int y) {
        StatDefinition definition = stat.definition();
        ResourceLocation icon = definition.icon == null || definition.icon.isBlank() ? null : ResourceLocation.tryParse(definition.icon);
        if (icon != null) {
            graphics.blitSprite(icon, x, y, 16, 16);
            return;
        }
        if (blitOptionalSprite(graphics, CharacterGuiSprites.STATS_FALLBACK_ICON,
                new ScreenRect(x, y, 16, 16))) return;
        graphics.fill(x, y, x + 16, y + 16, 0xFF343B43);
        outline(graphics, new ScreenRect(x, y, 16, 16), 0xFF69737E);
        String fallback = displayName(stat).substring(0, 1).toUpperCase(Locale.ROOT);
        graphics.drawCenteredString(font, fallback, x + 8, y + 4, 0xFFCDD4DA);
    }

    private void renderStatsScrollbar(GuiGraphics graphics, ScreenRect viewport, int visible, int total) {
        ScreenRect track = screenRect(STATS_SCROLLBAR);
        if (total <= visible) return;
        if (!blitOptionalSprite(graphics, CharacterGuiSprites.STATS_SCROLLBAR_TRACK, track)) {
            graphics.fill(track.x() + track.width() / 2, track.y(), track.x() + track.width() / 2 + 1,
                    track.y() + track.height(), 0xFF343A40);
        }
        int thumbHeight = Math.max(12, track.height() * visible / total);
        int travel = track.height() - thumbHeight;
        int thumbY = track.y() + travel * statsScroll / Math.max(1, total - visible);
        ScreenRect thumb = new ScreenRect(track.x() + 1, thumbY, Math.max(1, track.width() - 2), thumbHeight);
        if (!blitOptionalSprite(graphics, CharacterGuiSprites.STATS_SCROLLBAR_THUMB, thumb)) {
            graphics.fill(thumb.x(), thumb.y(), thumb.x() + thumb.width(), thumb.y() + thumb.height(), 0xFF87919B);
        }
    }

    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        List<InventoryCandidate> candidates = candidates(picker);
        PickerGeometry geometry = pickerGeometry(candidates.size() + 1);
        ScreenRect overlay = geometry.bounds;
        List<PickerItemRender> itemRenders = new ArrayList<>();
        if (!blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_BACKGROUND, overlay)) {
            graphics.fill(overlay.x(), overlay.y(), overlay.x() + overlay.width(), overlay.y() + overlay.height(), 0xF20B0E12);
            outline(graphics, overlay, 0xFFE0BD73);
        }
        int cell = 20;
        int startX = geometry.startX;
        int startY = overlay.y() + 6;
        int entries = candidates.size() + 1;
        pickerScroll = Math.max(0, Math.min(pickerScroll, Math.max(0, geometry.totalRows - geometry.visibleRows)));
        int firstIndex = pickerScroll * geometry.columns;
        int lastIndex = Math.min(entries, firstIndex + geometry.visibleRows * geometry.columns);
        for (int index = firstIndex; index < lastIndex; index++) {
            int visibleIndex = index - firstIndex;
            int x = startX + visibleIndex % geometry.columns * cell;
            int y = startY + visibleIndex / geometry.columns * cell;
            ScreenRect cellRect = new ScreenRect(x, y, 18, 18);
            boolean hovered = cellRect.contains(mouseX, mouseY);
            boolean customCell = blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_CELL, cellRect);
            if (!customCell) {
                graphics.fill(x, y, x + 18, y + 18, 0xFF292F35);
                outline(graphics, cellRect, 0xFF59616A);
            }
            if (hovered && !blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_CELL_HOVERED, cellRect)) {
                if (!customCell) graphics.fill(x, y, x + 18, y + 18, 0xFF59636D);
                outline(graphics, cellRect, 0xFFFFFFFF);
            }
            if (index == 0) {
                if (!blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_UNEQUIP,
                        new ScreenRect(x + 1, y + 1, 16, 16))) {
                    graphics.drawCenteredString(font, "x", x + 9, y + 5, 0xFFFF7777);
                }
                if (hovered) hoverTooltip = HoverTooltip.components(List.of(Component.translatable("gui.characternotcontainer.unequip")));
            } else {
                ItemStack stack = candidates.get(index - 1).stack;
                renderPickerItem(graphics, stack, x + 1, y + 1);
                itemRenders.add(new PickerItemRender(stack, x + 1, y + 1));
                if (hovered) hoverTooltip = HoverTooltip.item(stack);
            }
        }
        if (geometry.totalRows > geometry.visibleRows) {
            int trackX = overlay.x() + overlay.width() - 4;
            int trackY = startY;
            int trackHeight = geometry.visibleRows * cell - 2;
            ScreenRect track = new ScreenRect(trackX, trackY, 2, trackHeight);
            if (!blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_SCROLLBAR_TRACK, track)) {
                graphics.fill(track.x(), track.y(), track.x() + track.width(), track.y() + track.height(), 0xFF30363D);
            }
            int thumbHeight = Math.max(6, trackHeight * geometry.visibleRows / geometry.totalRows);
            int travel = trackHeight - thumbHeight;
            int thumbY = trackY + travel * pickerScroll / Math.max(1, geometry.totalRows - geometry.visibleRows);
            ScreenRect thumb = new ScreenRect(trackX, thumbY, 2, thumbHeight);
            if (!blitOptionalSprite(graphics, CharacterGuiSprites.PICKER_SCROLLBAR_THUMB, thumb)) {
                graphics.fill(thumb.x(), thumb.y(), thumb.x() + thumb.width(), thumb.y() + thumb.height(), 0xFF9A7A4E);
            }
        }
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 250.0F);
        for (PickerItemRender itemRender : itemRenders) {
            graphics.renderItemDecorations(font, itemRender.stack, itemRender.x, itemRender.y);
        }
        graphics.flush();
        graphics.pose().popPose();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    /**
     * Some custom item renderers emit armor geometry far beyond vanilla's icon
     * cube. Flush it while the cell scissor is active so it cannot escape the
     * picker, then discard its depth before rendering later GUI layers.
     */
    private static void renderPickerItem(GuiGraphics graphics, ItemStack stack, int x, int y) {
        graphics.flush();
        graphics.enableScissor(x, y, x + 16, y + 16);
        try {
            graphics.renderItem(stack, x, y);
            graphics.flush();
        } finally {
            graphics.disableScissor();
        }
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        if (picker != null) {
            List<InventoryCandidate> candidates = candidates(picker);
            PickerGeometry geometry = pickerGeometry(candidates.size() + 1);
            ScreenRect overlay = geometry.bounds;
            if (!overlay.contains(mouseX, mouseY)) {
                closePicker();
                return true;
            }
            int cell = 20;
            int startX = geometry.startX;
            int startY = overlay.y() + 6;
            int column = (int)(mouseX - startX) / cell;
            int row = (int)(mouseY - startY) / cell;
            int relativeX = (int)(mouseX - startX);
            int relativeY = (int)(mouseY - startY);
            if (mouseX >= startX && mouseY >= startY && column >= 0 && column < geometry.columns && row >= 0
                    && row < geometry.visibleRows && relativeX % cell < 18 && relativeY % cell < 18) {
                int selected = (row + pickerScroll) * geometry.columns + column;
                boolean sent = selected == 0 ? sendChange(picker, EquipmentChangePayload.SourceKind.UNEQUIP, -1, 0)
                        : selected <= candidates.size() && sendChange(picker, candidates.get(selected - 1));
                if (sent) closePicker();
            }
            return true;
        }
        if (CURIOS != null && cosmeticsToggleBounds().contains(mouseX, mouseY)) {
            showCosmetics = !showCosmetics;
            refreshCurios();
            hoveredEquipmentTarget = null;
            return true;
        }
        PickerTarget target = hoveredEquipmentTarget(mouseX, mouseY, placedCurios());
        if (target != null) {
            openPicker(target, placedCurios());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (picker != null && pickerGeometry(candidates(picker).size() + 1).bounds.contains(mouseX, mouseY)) {
            pickerScroll -= (int)Math.signum(scrollY);
            return true;
        }
        if (picker == null && screenRect(STATS_LIST).contains(mouseX, mouseY)) {
            statsScroll -= (int)Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (CharacterConfigManager.general().enableSeparateKeybind
                && ClientKeyMappings.OPEN_CHARACTER.matches(keyCode, scanCode)) {
            while (ClientKeyMappings.OPEN_CHARACTER.consumeClick()) {}
            if (CharacterConfigManager.general().characterKeyOpensInventory) {
                CharacterNotContainerClient.openInventoryScreen();
            } else {
                onClose();
            }
            return true;
        }
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            CharacterNotContainerClient.openInventoryScreen();
            return true;
        }
        if (keyCode == 256 && picker != null) {
            closePicker();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private List<InventoryCandidate> candidates(PickerTarget target) {
        List<InventoryCandidate> result = new ArrayList<>();
        IItemHandler inventory = PlayerInventoryAccess.handler(player);
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack stack = inventory.getStackInSlot(index);
            if (!stack.isEmpty() && target.accepts(stack)) {
                result.add(new InventoryCandidate(EquipmentChangePayload.SourceKind.PLAYER_INVENTORY,
                        index, 0, stack.copy()));
            }
        }
        for (NearbyEquipmentResponsePayload.Entry entry : nearbyEquipment) {
            if (!entry.stack().isEmpty() && target.accepts(entry.stack())) {
                result.add(new InventoryCandidate(EquipmentChangePayload.SourceKind.NEARBY,
                        entry.token(), pickerSearchId, entry.stack()));
            }
        }
        return result;
    }

    private boolean sendChange(PickerTarget target, InventoryCandidate candidate) {
        return sendChange(target, candidate.sourceKind, candidate.sourceIndex, candidate.searchId);
    }

    private boolean sendChange(PickerTarget target, EquipmentChangePayload.SourceKind sourceKind,
                               int sourceIndex, int searchId) {
        if (!serverSupports(EquipmentChangePayload.TYPE.id())) {
            player.displayClientMessage(Component.translatable("message.characternotcontainer.server_required"), true);
            return false;
        }
        PacketDistributor.sendToServer(new EquipmentChangePayload(target.system(), target.slotId(), target.slotIndex(),
                target.cosmetic(), sourceKind, sourceIndex, searchId));
        return true;
    }

    private void openPicker(PickerTarget target, List<PlacedCurio> placements) {
        picker = target;
        pickerAnchor = targetBounds(target, placements);
        pickerScroll = 0;
        nearbyEquipment = List.of();
        pickerSearchId = pickerSearchId == Integer.MAX_VALUE ? 1 : pickerSearchId + 1;
        if (CharacterConfigManager.general().enableNearbyEquipmentSources
                && serverSupports(NearbyEquipmentRequestPayload.TYPE.id())) {
            PacketDistributor.sendToServer(new NearbyEquipmentRequestPayload(pickerSearchId, target.system(),
                    target.slotId(), target.slotIndex(), target.cosmetic()));
        }
    }

    private void closePicker() {
        picker = null;
        pickerAnchor = null;
        nearbyEquipment = List.of();
        pickerScroll = 0;
    }

    private boolean serverSupports(ResourceLocation payloadId) {
        return minecraft != null && minecraft.getConnection() != null
                && NetworkRegistry.hasChannel(minecraft.getConnection(), payloadId);
    }

    private ScreenRect targetBounds(PickerTarget target, List<PlacedCurio> placements) {
        if (target instanceof VanillaTarget vanilla) {
            return bodyBounds(vanilla.slot);
        } else if (target instanceof CurioTarget curio) {
            for (PlacedCurio placement : placements) {
                if (placement.slot.type().equals(curio.slot.type()) && placement.slot.index() == curio.slot.index()
                        && placement.slot.cosmetic() == curio.slot.cosmetic()) return placement.bounds;
            }
        }
        return screenRect(PICKER_FALLBACK);
    }

    private PickerGeometry pickerGeometry(int entries) {
        ScreenRect anchor = pickerAnchor == null ? screenRect(PICKER_FALLBACK) : pickerAnchor;
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(anchor, width, height, entries,
                CharacterConfigManager.general().equipmentPickerColumns,
                CharacterConfigManager.general().equipmentPickerVisibleRows,
                CharacterConfigManager.general().equipmentPickerOpensLeft);
        return new PickerGeometry(layout.bounds(), layout.bounds().x() + 6,
                layout.columns(), layout.visibleRows(), layout.totalRows());
    }

    private ScreenRect screenRect(ScreenRect raw) {
        return new ScreenRect(layoutX + scaled(raw.x()), layoutY + scaled(raw.y()), scaled(raw.width()), scaled(raw.height()));
    }

    private int scaled(int value) { return (int)Math.round(value * layoutScale); }

    private ScreenRect bodyBounds(EquipmentSlot slot) {
        ScreenRect raw = switch (slot) {
            case HEAD -> new ScreenRect(250, 35, 60, 52);
            case CHEST -> new ScreenRect(224, 87, 112, 88);
            case LEGS -> new ScreenRect(241, 171, 78, 75);
            case FEET -> new ScreenRect(241, 246, 78, 56);
            default -> throw new IllegalArgumentException("Not a body armor slot: " + slot);
        };
        return screenRect(raw);
    }

    private static List<EquipmentSlot> bodySlots() {
        return List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
    }

    private static ResourceLocation emptyIcon(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
            case CHEST -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
            case LEGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
            case FEET -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
            default -> InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
        };
    }

    private static Component slotName(EquipmentSlot slot) {
        return Component.translatable("slot.characternotcontainer." + slot.getName());
    }

    private static String displayName(ResolvedStat stat) {
        String translationKey = stat.attribute().value().getDescriptionId();
        return AttributeDisplayName.resolve(stat.definition().name, translationKey, stat.definition().attribute,
                key -> Component.translatable(key).getString());
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Curio";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).replace('_', ' ');
    }

    private static void outline(GuiGraphics graphics, ScreenRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.y() + rect.height(), color);
        graphics.fill(rect.x(), rect.y() + rect.height() - 1, rect.x() + rect.width(), rect.y() + rect.height(), color);
        graphics.fill(rect.x() + rect.width() - 1, rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), color);
    }

    private static boolean blitOptionalSprite(GuiGraphics graphics, ResourceLocation sprite, ScreenRect bounds) {
        if (!hasGuiSprite(sprite)) return false;
        graphics.blitSprite(sprite, bounds.x(), bounds.y(), bounds.width(), bounds.height());
        return true;
    }

    private static boolean hasGuiSprite(ResourceLocation sprite) {
        return Minecraft.getInstance().getResourceManager()
                .getResource(CharacterGuiSprites.textureFile(sprite)).isPresent();
    }

    @SuppressWarnings("deprecation")
    private static void renderGroup(GuiGraphics graphics, float depth, Runnable renderer) {
        graphics.drawManaged(() -> {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, depth);
            try {
                renderer.run();
            } finally {
                graphics.pose().popPose();
            }
        });
    }

    private static ResourceLocation curioIconTexture(ResourceLocation icon) {
        ResourceLocation logicalIcon = icon == null
                ? ResourceLocation.fromNamespaceAndPath("curios", "slot/empty_curio_slot") : icon;
        String path = logicalIcon.getPath();
        if (!path.startsWith("textures/")) path = "textures/" + path;
        if (!path.endsWith(".png")) path += ".png";
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(logicalIcon.getNamespace(), path);
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()) return texture;
        return ResourceLocation.fromNamespaceAndPath("curios", "textures/slot/empty_curio_slot.png");
    }

    private sealed interface PickerTarget permits VanillaTarget, CurioTarget {
        EquipmentChangePayload.TargetSystem system();
        String slotId();
        int slotIndex();
        default boolean cosmetic() { return false; }
        ItemStack equipped();
        boolean accepts(ItemStack stack);
    }

    private final class VanillaTarget implements PickerTarget {
        private final EquipmentSlot slot;
        private VanillaTarget(EquipmentSlot slot) { this.slot = slot; }
        @Override public EquipmentChangePayload.TargetSystem system() { return EquipmentChangePayload.TargetSystem.VANILLA; }
        @Override public String slotId() { return slot.getName(); }
        @Override public int slotIndex() { return 0; }
        @Override public ItemStack equipped() { return player.getItemBySlot(slot); }
        @Override public boolean accepts(ItemStack stack) { return player.getEquipmentSlotForItem(stack) == slot && stack.canEquip(slot, player); }
    }

    private final class CurioTarget implements PickerTarget {
        private final CuriosClientIntegration.CurioSlotView slot;
        private CurioTarget(CuriosClientIntegration.CurioSlotView slot) { this.slot = slot; }
        @Override public EquipmentChangePayload.TargetSystem system() { return EquipmentChangePayload.TargetSystem.CURIOS; }
        @Override public String slotId() { return slot.type(); }
        @Override public int slotIndex() { return slot.index(); }
        @Override public boolean cosmetic() { return slot.cosmetic(); }
        @Override public ItemStack equipped() { return slot.stack(); }
        @Override public boolean accepts(ItemStack stack) { return CURIOS != null && CURIOS.isValid(player, slot, stack); }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static CuriosClientIntegration loadCurios() {
        if (!ModList.get().isLoaded("curios")) return null;
        try {
            return CuriosClientIntegration.load();
        } catch (RuntimeException | LinkageError exception) {
            CharacterNotContainer.LOGGER.error("Curios is loaded but the character-screen integration is unavailable", exception);
            return null;
        }
    }

    private record PlacedCurio(CuriosClientIntegration.CurioSlotView slot, ScreenRect bounds) {}
    private record InventoryCandidate(EquipmentChangePayload.SourceKind sourceKind, int sourceIndex, int searchId,
                                      ItemStack stack) {}
    private record PickerGeometry(ScreenRect bounds, int startX, int columns, int visibleRows, int totalRows) {}
    private record PickerItemRender(ItemStack stack, int x, int y) {}
    private record ChangedStat(ResolvedStat stat, double base, double current,
                               Map<ResourceLocation, AttributeModifier> modifiers) {
        private ChangedStat {
            modifiers = Map.copyOf(modifiers);
        }
    }
    private record Contribution(ItemStack stack, AttributeModifier modifier) {}
    private record SourceTooltipLine(ItemStack stack, Component text) {}
    private record ModifierKey(ResourceLocation attributeId, ResourceLocation modifierId) {}

    private record HoverTooltip(ItemStack item, List<Component> components, List<SourceTooltipLine> sources) {
        static HoverTooltip item(ItemStack stack) { return new HoverTooltip(stack, List.of(), List.of()); }
        static HoverTooltip components(List<Component> components) { return new HoverTooltip(ItemStack.EMPTY, components, List.of()); }
        static HoverTooltip sources(List<SourceTooltipLine> sources) { return new HoverTooltip(ItemStack.EMPTY, List.of(), sources); }
        void render(GuiGraphics graphics, int mouseX, int mouseY) {
            if (!item.isEmpty()) graphics.renderTooltip(Minecraft.getInstance().font, item, mouseX, mouseY);
            else if (!sources.isEmpty()) renderSources(graphics, mouseX, mouseY, sources);
            else graphics.renderComponentTooltip(Minecraft.getInstance().font, components, mouseX, mouseY);
        }

        private static void renderSources(GuiGraphics graphics, int mouseX, int mouseY, List<SourceTooltipLine> sources) {
            var font = Minecraft.getInstance().font;
            int rowHeight = 18;
            int tooltipWidth = sources.stream().mapToInt(line -> (line.stack.isEmpty() ? 0 : 20) + font.width(line.text)).max().orElse(20);
            int tooltipHeight = sources.size() * rowHeight;
            int x = mouseX + TooltipRenderUtil.MOUSE_OFFSET;
            int y = mouseY - TooltipRenderUtil.MOUSE_OFFSET;
            if (x + tooltipWidth + 6 > graphics.guiWidth()) x = mouseX - TooltipRenderUtil.MOUSE_OFFSET - tooltipWidth;
            if (y + tooltipHeight + 6 > graphics.guiHeight()) y = graphics.guiHeight() - tooltipHeight - 6;
            y = Math.max(6, y);

            graphics.pose().pushPose();
            if (hasGuiSprite(CharacterGuiSprites.SOURCE_TOOLTIP_BACKGROUND)) {
                graphics.pose().translate(0.0F, 0.0F, 400.0F);
                blitOptionalSprite(graphics, CharacterGuiSprites.SOURCE_TOOLTIP_BACKGROUND,
                        new ScreenRect(x - 3, y - 4, tooltipWidth + 6, tooltipHeight + 8));
            } else {
                TooltipRenderUtil.renderTooltipBackground(graphics, x, y, tooltipWidth, tooltipHeight, 400);
                graphics.pose().translate(0.0F, 0.0F, 400.0F);
            }
            for (int row = 0; row < sources.size(); row++) {
                SourceTooltipLine line = sources.get(row);
                int rowY = y + row * rowHeight;
                int textX = x;
                if (!line.stack.isEmpty()) {
                    graphics.renderItem(line.stack, x, rowY + 1);
                    textX += 20;
                }
                graphics.drawString(font, line.text, textX, rowY + 5, 0xFFFFFFFF, true);
            }
            graphics.pose().popPose();
        }
    }
}
