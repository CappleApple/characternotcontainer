package com.cappleapple.characternotcontainer.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.cappleapple.characternotcontainer.config.CharacterConfigManager;
import com.cappleapple.characternotcontainer.config.StatDefinition;
import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.cappleapple.characternotcontainer.layout.EquipmentLayoutLoader;
import com.cappleapple.characternotcontainer.layout.EquipmentPickerLayout;
import com.cappleapple.characternotcontainer.layout.EquipmentScreenSpec;
import com.cappleapple.characternotcontainer.layout.ScreenRect;
import com.cappleapple.characternotcontainer.network.EquipmentChangePayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentRequestPayload;
import com.cappleapple.characternotcontainer.network.NearbyEquipmentResponsePayload;
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
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
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
    private static final int POSITIVE = 0xFFD0D750;
    private static final int NEGATIVE = 0xFFFF7A7A;
    private static final int CURIO_SLOT_BACKGROUND = 0xC421262C;
    private static final int CURIO_SLOT_BORDER = 0xCC6D737A;
    private static final int COSMETIC_CURIO_SLOT_BACKGROUND = 0xD04B2D63;
    private static final int COSMETIC_CURIO_SLOT_BORDER = 0xD09D70B5;
    private static final CuriosClientIntegration CURIOS = loadCurios();

    private final Player player;
    private EquipmentScreenSpec spec;
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
    private HoverTooltip hoverTooltip;
    private PickerTarget hoveredEquipmentTarget;

    public CharacterEquipmentScreen(Player player) {
        super(Component.translatable("gui.characternotcontainer.character"));
        this.player = player;
    }

    @Override
    protected void init() {
        spec = EquipmentLayoutLoader.load();
        layoutScale = Math.min(1.0D, Math.min((width - 12.0D) / spec.width, (height - 12.0D) / spec.height));
        layoutScale = Math.max(0.4D, layoutScale);
        layoutX = (width - scaled(spec.width)) / 2;
        layoutY = (height - scaled(spec.height)) / 2;
        refreshCurios();
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
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        renderGroup(graphics, 0.0F, () -> renderPlayer(graphics, mouseX, mouseY));
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
    }

    private void renderPanels(GuiGraphics graphics) {
        for (EquipmentScreenSpec.Widget widget : spec.widgets) {
            if (!"panel".equals(widget.type) || "stats_background".equals(widget.id)) continue;
            ScreenRect rect = rect(widget);
            graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), PANEL);
            outline(graphics, rect, BORDER);
        }
    }

    private void renderPlayer(GuiGraphics graphics, int mouseX, int mouseY) {
        ScreenRect preview = rect(spec.widget("player_preview_reference"));
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

    private void renderVanillaEquipment(GuiGraphics graphics, int mouseX, int mouseY) {
        for (EquipmentScreenSpec.Widget widget : spec.widgetsOfCustomType("character_not_container:equipment_hitbox")) {
            EquipmentSlot slot = vanillaSlot(widget.props.get("slot_id"));
            if (slot == null) continue;
            ScreenRect hitbox = rect(widget);
            boolean hovered = hoveredEquipmentTarget instanceof VanillaTarget target && target.slot == slot;
            boolean selected = picker instanceof VanillaTarget target && target.slot == slot;
            if (CharacterConfigManager.general().debugEquipmentHitboxes || hovered || selected) {
                outline(graphics, hitbox, selected ? 0xFFE5BD68 : hovered ? 0x99FFFFFF : 0x6688CCFF);
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
            graphics.fill(placement.bounds.x(), placement.bounds.y(), placement.bounds.x() + placement.bounds.width(),
                    placement.bounds.y() + placement.bounds.height(), slotBackground);
            int border = selected ? 0xFFE5BD68
                    : hovered ? 0xFFFFFFFF
                    : CharacterConfigManager.general().debugEquipmentHitboxes ? 0xFF88CCFF
                    : placement.slot.cosmetic() ? COSMETIC_CURIO_SLOT_BORDER : CURIO_SLOT_BORDER;
            outline(graphics, placement.bounds, border);
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
        int background = showCosmetics ? 0xE05B327A : hovered ? 0xE0444B53 : 0xE02A3036;
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), background);
        outline(graphics, bounds, showCosmetics ? COSMETIC_CURIO_SLOT_BORDER : hovered ? 0xFFFFFFFF : BORDER);
        graphics.drawCenteredString(font, showCosmetics ? "E" : "C",
                bounds.x() + bounds.width() / 2, bounds.y() + (bounds.height() - font.lineHeight) / 2 + 1, TEXT);
        if (hovered) {
            hoverTooltip = HoverTooltip.components(List.of(Component.translatable(showCosmetics
                    ? "gui.characternotcontainer.show_equipment" : "gui.characternotcontainer.show_cosmetics")));
        }
    }

    private ScreenRect cosmeticsToggleBounds() {
        int size = 18;
        int margin = Math.max(6, scaled(10));
        return new ScreenRect(layoutX + scaled(spec.width) - margin - size, layoutY + margin, size, size);
    }

    private List<PlacedCurio> placedCurios() {
        List<EquipmentScreenSpec.Widget> anchors = spec.widgetsOfCustomType("character_not_container:curio_anchor");
        EquipmentScreenSpec.Widget fallback = spec.widget("curio_fallback_anchor_region");
        Map<String, Integer> usage = new HashMap<>();
        List<PlacedCurio> result = new ArrayList<>();
        int fallbackIndex = 0;
        for (CuriosClientIntegration.CurioSlotView slot : curiosSlots) {
            List<EquipmentScreenSpec.Widget> matches = anchors.stream().filter(anchor -> aliases(anchor).stream()
                    .anyMatch(alias -> alias.equals(slot.type().toLowerCase(Locale.ROOT)))).toList();
            if (matches.isEmpty()) {
                ScreenRect region = rect(fallback);
                int gap = scaled(2);
                int slotSize = 18;
                int rows = Math.max(1, (region.height() + gap) / (slotSize + gap));
                int x = region.x() + fallbackIndex / rows * (slotSize + gap);
                int y = region.y() + fallbackIndex % rows * (slotSize + gap);
                result.add(new PlacedCurio(slot, new ScreenRect(x, y, slotSize, slotSize)));
                fallbackIndex++;
                continue;
            }
            String anchorGroup = matches.stream().map(anchor -> anchor.id).sorted().reduce((left, right) -> left + "|" + right).orElse(slot.type());
            int used = usage.merge(anchorGroup, 1, Integer::sum) - 1;
            EquipmentScreenSpec.Widget anchor = matches.get(used % matches.size());
            int overflow = used / matches.size();
            ScreenRect anchorRect = rect(anchor);
            int spacing = Math.max(20, scaled(20));
            int x = bodyAlignedCurioX(anchor, anchorRect) + (overflow % 2) * spacing;
            int y = bodyAlignedCurioY(anchor, anchorRect) + (overflow / 2) * spacing;
            result.add(new PlacedCurio(slot, new ScreenRect(x, y, 18, 18)));
        }
        return result;
    }

    private int bodyAlignedCurioX(EquipmentScreenSpec.Widget anchor, ScreenRect anchorRect) {
        if (!"curio_anchor_neck".equals(anchor.id) && !"curio_anchor_belt".equals(anchor.id)) return anchorRect.x();
        ScreenRect chest = rect(spec.widget("equip_hitbox_chest"));
        return chest.x() + (chest.width() - 18) / 2;
    }

    private int bodyAlignedCurioY(EquipmentScreenSpec.Widget anchor, ScreenRect anchorRect) {
        ScreenRect chest = rect(spec.widget("equip_hitbox_chest"));
        if ("curio_anchor_neck".equals(anchor.id)) return chest.y() + scaled(4);
        if ("curio_anchor_belt".equals(anchor.id)) return chest.y() + chest.height() - scaled(11);
        return anchorRect.y();
    }

    private PickerTarget hoveredEquipmentTarget(double mouseX, double mouseY, List<PlacedCurio> placements) {
        PickerTarget target = null;
        for (EquipmentScreenSpec.Widget widget : spec.widgetsOfCustomType("character_not_container:equipment_hitbox")) {
            EquipmentSlot slot = vanillaSlot(widget.props.get("slot_id"));
            if (slot != null && rect(widget).contains(mouseX, mouseY)) target = new VanillaTarget(slot);
        }
        for (PlacedCurio placement : placements) {
            if (placement.bounds.contains(mouseX, mouseY)) target = new CurioTarget(placement.slot);
        }
        return target;
    }

    private void renderChangedAttributes(GuiGraphics graphics, int mouseX, int mouseY) {
        EquipmentScreenSpec.Widget listWidget = spec.widget("stats_list");
        ScreenRect viewport = rect(listWidget);
        int rowHeight = Math.max(20, scaled(Integer.parseInt(listWidget.props.getOrDefault("item_height", "26"))));
        List<ChangedStat> changed = changedStats();
        int visibleRows = Math.max(1, (viewport.height() + rowHeight - 1) / rowHeight);
        statsScroll = Math.max(0, Math.min(statsScroll, Math.max(0, changed.size() - visibleRows)));
        int displayedRows = Math.min(visibleRows, changed.size());
        if (displayedRows > 0) {
            int panelHeight = Math.min(viewport.height(), displayedRows * rowHeight);
            graphics.fill(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + panelHeight, STATS_PANEL);
            outline(graphics, new ScreenRect(viewport.x(), viewport.y(), viewport.width(), panelHeight), 0xFF444B53);
        }
        graphics.enableScissor(viewport.x(), viewport.y(), viewport.x() + viewport.width(), viewport.y() + viewport.height());
        for (int row = statsScroll; row < changed.size() && row < statsScroll + visibleRows; row++) {
            ChangedStat changedStat = changed.get(row);
            int y = viewport.y() + (row - statsScroll) * rowHeight;
            ScreenRect rowRect = new ScreenRect(viewport.x(), y, viewport.width(), rowHeight);
            if (rowRect.contains(mouseX, mouseY)) graphics.fill(rowRect.x(), rowRect.y(), rowRect.x() + rowRect.width(), rowRect.y() + rowRect.height(), 0x553F4852);
            renderAttributeIcon(graphics, changedStat.stat.definition(), viewport.x() + scaled(4), y + Math.max(2, (rowHeight - 16) / 2));
            String name = displayName(changedStat.stat.definition());
            int nameX = viewport.x() + scaled(25);
            int deltaRight = viewport.x() + viewport.width() - scaled(4);
            String delta = formattedChange(changedStat);
            double displayDelta = changedStat.stat.definition().format.displayDifference(changedStat.base, changedStat.current, changedStat.stat.definition());
            int deltaX = deltaRight - font.width(delta);
            graphics.drawString(font, font.plainSubstrByWidth(name, Math.max(10, deltaX - nameX - 4)), nameX, y + (rowHeight - 8) / 2, TEXT, false);
            boolean beneficial = displayDelta >= 0.0D == changedStat.stat.definition().higherIsBetter;
            graphics.drawString(font, delta, deltaX, y + (rowHeight - 8) / 2, beneficial ? POSITIVE : NEGATIVE, false);
            if (rowRect.contains(mouseX, mouseY)) {
                List<Contribution> contributions = equipmentContributions(changedStat.stat);
                if (!contributions.isEmpty()) hoverTooltip = HoverTooltip.sources(contributionTooltip(contributions, changedStat.stat.definition()));
            }
        }
        graphics.disableScissor();
        renderStatsScrollbar(graphics, viewport, visibleRows, changed.size());
    }

    private List<ChangedStat> changedStats() {
        List<ChangedStat> result = new ArrayList<>();
        Set<Holder<Attribute>> configuredAttributes = new HashSet<>();
        for (ResolvedCategory category : ResolvedStatCatalog.categories()) {
            for (ResolvedStat stat : category.stats()) {
                configuredAttributes.add(stat.attribute());
                AttributeInstance instance = player.getAttribute(stat.attribute());
                if (instance != null) {
                    ChangedStat changed = changedStat(stat, instance, true);
                    if (Math.abs(changed.current - changed.base) > 0.0000001D) result.add(changed);
                }
            }
        }
        player.getAttributes().getSyncableAttributes().stream()
                .filter(instance -> !configuredAttributes.contains(instance.getAttribute()))
                .filter(instance -> Math.abs(instance.getValue() - instance.getBaseValue()) > 0.0000001D)
                .map(instance -> changedStat(automaticStat(instance), instance, false))
                .sorted(Comparator.comparing(stat -> displayName(stat.stat.definition()), String.CASE_INSENSITIVE_ORDER))
                .forEach(result::add);
        return result;
    }

    private ChangedStat changedStat(ResolvedStat stat, AttributeInstance instance, boolean includeEquipmentFallback) {
        Map<ResourceLocation, AttributeModifier> modifiers = new LinkedHashMap<>();
        instance.getModifiers().forEach(modifier -> modifiers.put(modifier.id(), modifier));
        if (includeEquipmentFallback && Math.abs(instance.getValue() - instance.getBaseValue()) <= 0.0000001D) {
            vanillaEquipmentContributions(stat).forEach(contribution -> modifiers.putIfAbsent(contribution.modifier.id(), contribution.modifier));
        }
        EnumSet<AttributeModifier.Operation> operations = EnumSet.noneOf(AttributeModifier.Operation.class);
        modifiers.values().forEach(modifier -> operations.add(modifier.operation()));
        double current = modifiers.size() == instance.getModifiers().size()
                ? instance.getValue() : calculateAttributeValue(instance, modifiers.values());
        return new ChangedStat(stat, instance.getBaseValue(), current, Set.copyOf(operations));
    }

    private static double calculateAttributeValue(AttributeInstance instance, Iterable<AttributeModifier> modifiers) {
        double added = instance.getBaseValue();
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_VALUE) added += modifier.amount();
        }
        double multiplied = added;
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) multiplied += added * modifier.amount();
        }
        for (AttributeModifier modifier : modifiers) {
            if (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) multiplied *= 1.0D + modifier.amount();
        }
        return instance.getAttribute().value().sanitizeValue(multiplied);
    }

    private static String formattedChange(ChangedStat changed) {
        boolean additive = changed.operations.contains(AttributeModifier.Operation.ADD_VALUE);
        boolean multiplicative = changed.operations.contains(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)
                || changed.operations.contains(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (!additive && multiplicative && Math.abs(changed.base) > 0.0000001D) {
            return "×" + java.math.BigDecimal.valueOf(changed.current / changed.base).setScale(2,
                    java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        String delta = changed.stat.definition().format.formatDifference(changed.base, changed.current, changed.stat.definition());
        return additive && multiplicative ? delta + " [+,×]" : multiplicative ? delta + " [×]" : delta;
    }

    private static ResolvedStat automaticStat(AttributeInstance instance) {
        Holder<Attribute> attribute = instance.getAttribute();
        ResourceLocation id = BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());
        String attributeId = id == null ? attribute.value().getDescriptionId() : id.toString();
        String descriptionId = attribute.value().getDescriptionId();
        String name = Component.translatable(descriptionId).getString();
        if (name.equals(descriptionId)) name = id == null ? titleCase(attributeId) : titleCasePath(id.getPath());
        return new ResolvedStat(new StatDefinition(attributeId, name, com.cappleapple.characternotcontainer.config.StatFormat.DECIMAL, 2), attribute);
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

    private List<SourceTooltipLine> contributionTooltip(List<Contribution> contributions, StatDefinition definition) {
        List<SourceTooltipLine> lines = new ArrayList<>();
        for (Contribution contribution : contributions) {
            String value = contribution.modifier.operation() == AttributeModifier.Operation.ADD_VALUE
                    ? definition.format.formatDifference(0.0D, contribution.modifier.amount(), definition)
                    : (contribution.modifier.amount() >= 0 ? "+" : "")
                    + String.format(Locale.ROOT, "%.0f%%", contribution.modifier.amount() * 100.0D);
            ChatFormatting color = contribution.modifier.amount() >= 0.0D ? ChatFormatting.GREEN : ChatFormatting.RED;
            Component text = contribution.stack.getHoverName().copy().append(Component.literal("  " + value).withStyle(color));
            lines.add(new SourceTooltipLine(contribution.stack.copy(), text));
        }
        return lines;
    }

    private void renderAttributeIcon(GuiGraphics graphics, StatDefinition definition, int x, int y) {
        ResourceLocation icon = definition.icon == null || definition.icon.isBlank() ? null : ResourceLocation.tryParse(definition.icon);
        if (icon != null) {
            graphics.blitSprite(icon, x, y, 16, 16);
            return;
        }
        graphics.fill(x, y, x + 16, y + 16, 0xFF343B43);
        outline(graphics, new ScreenRect(x, y, 16, 16), 0xFF69737E);
        String fallback = displayName(definition).substring(0, 1).toUpperCase(Locale.ROOT);
        graphics.drawCenteredString(font, fallback, x + 8, y + 4, 0xFFCDD4DA);
    }

    private void renderStatsScrollbar(GuiGraphics graphics, ScreenRect viewport, int visible, int total) {
        ScreenRect track = rect(spec.widget("stats_scrollbar"));
        if (total <= visible) return;
        graphics.fill(track.x() + track.width() / 2, track.y(), track.x() + track.width() / 2 + 1, track.y() + track.height(), 0xFF343A40);
        int thumbHeight = Math.max(12, track.height() * visible / total);
        int travel = track.height() - thumbHeight;
        int thumbY = track.y() + travel * statsScroll / Math.max(1, total - visible);
        graphics.fill(track.x() + 1, thumbY, track.x() + track.width() - 1, thumbY + thumbHeight, 0xFF87919B);
    }

    private void renderPicker(GuiGraphics graphics, int mouseX, int mouseY) {
        List<InventoryCandidate> candidates = candidates(picker);
        PickerGeometry geometry = pickerGeometry(candidates.size() + 1);
        ScreenRect overlay = geometry.bounds;
        List<PickerItemRender> itemRenders = new ArrayList<>();
        graphics.fill(overlay.x(), overlay.y(), overlay.x() + overlay.width(), overlay.y() + overlay.height(), 0xF20B0E12);
        outline(graphics, overlay, 0xFFE0BD73);
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
            graphics.fill(x, y, x + 18, y + 18, hovered ? 0xFF59636D : 0xFF292F35);
            outline(graphics, cellRect, hovered ? 0xFFFFFFFF : 0xFF59616A);
            if (index == 0) {
                graphics.drawCenteredString(font, "x", x + 9, y + 5, 0xFFFF7777);
                if (hovered) hoverTooltip = HoverTooltip.components(List.of(Component.translatable("gui.characternotcontainer.unequip")));
            } else {
                ItemStack stack = candidates.get(index - 1).stack;
                graphics.renderItem(stack, x + 1, y + 1);
                itemRenders.add(new PickerItemRender(stack, x + 1, y + 1));
                if (hovered) hoverTooltip = HoverTooltip.item(stack);
            }
        }
        if (geometry.totalRows > geometry.visibleRows) {
            int trackX = overlay.x() + overlay.width() - 4;
            int trackY = startY;
            int trackHeight = geometry.visibleRows * cell - 2;
            graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0xFF30363D);
            int thumbHeight = Math.max(6, trackHeight * geometry.visibleRows / geometry.totalRows);
            int travel = trackHeight - thumbHeight;
            int thumbY = trackY + travel * pickerScroll / Math.max(1, geometry.totalRows - geometry.visibleRows);
            graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, 0xFF9A7A4E);
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
        if (picker == null && rect(spec.widget("stats_list")).contains(mouseX, mouseY)) {
            statsScroll -= (int)Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && picker != null) {
            closePicker();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private List<InventoryCandidate> candidates(PickerTarget target) {
        List<InventoryCandidate> result = new ArrayList<>();
        for (int index = 0; index < player.getInventory().items.size(); index++) {
            ItemStack stack = player.getInventory().items.get(index);
            if (!stack.isEmpty() && target.accepts(stack)) {
                result.add(new InventoryCandidate(EquipmentChangePayload.SourceKind.PLAYER_INVENTORY, index, 0, stack));
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
            for (EquipmentScreenSpec.Widget widget : spec.widgetsOfCustomType("character_not_container:equipment_hitbox")) {
                if (vanillaSlot(widget.props.get("slot_id")) == vanilla.slot) return rect(widget);
            }
        } else if (target instanceof CurioTarget curio) {
            for (PlacedCurio placement : placements) {
                if (placement.slot.type().equals(curio.slot.type()) && placement.slot.index() == curio.slot.index()
                        && placement.slot.cosmetic() == curio.slot.cosmetic()) return placement.bounds;
            }
        }
        return rect(spec.widget("equipment_picker_overlay_reference"));
    }

    private PickerGeometry pickerGeometry(int entries) {
        ScreenRect anchor = pickerAnchor == null ? rect(spec.widget("equipment_picker_overlay_reference")) : pickerAnchor;
        EquipmentPickerLayout layout = EquipmentPickerLayout.calculate(anchor, width, height, entries,
                CharacterConfigManager.general().equipmentPickerColumns,
                CharacterConfigManager.general().equipmentPickerVisibleRows,
                CharacterConfigManager.general().equipmentPickerOpensLeft);
        return new PickerGeometry(layout.bounds(), layout.bounds().x() + 6,
                layout.columns(), layout.visibleRows(), layout.totalRows());
    }

    private ScreenRect rect(EquipmentScreenSpec.Widget widget) {
        return new ScreenRect(layoutX + scaled(widget.x), layoutY + scaled(widget.y), scaled(widget.w), scaled(widget.h));
    }

    private int scaled(int value) { return (int)Math.round(value * layoutScale); }

    private static EquipmentSlot vanillaSlot(String id) {
        return switch (id == null ? "" : id) {
            case "head" -> EquipmentSlot.HEAD;
            case "chest" -> EquipmentSlot.CHEST;
            case "legs" -> EquipmentSlot.LEGS;
            case "feet" -> EquipmentSlot.FEET;
            default -> null;
        };
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

    private static String displayName(StatDefinition definition) {
        return definition.name == null || definition.name.isBlank() ? definition.attribute : definition.name;
    }

    private static String titleCase(String value) {
        if (value == null || value.isBlank()) return "Curio";
        return Character.toUpperCase(value.charAt(0)) + value.substring(1).replace('_', ' ');
    }

    private static String titleCasePath(String path) {
        int separator = Math.max(path.lastIndexOf('.'), path.lastIndexOf('/'));
        String value = separator >= 0 ? path.substring(separator + 1) : path;
        String[] words = value.replace('-', '_').split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.isEmpty() ? value : result.toString();
    }

    private static List<String> aliases(EquipmentScreenSpec.Widget anchor) {
        String value = anchor.props.getOrDefault("slot_type_aliases", "");
        return java.util.Arrays.stream(value.split(",")).map(String::trim).map(text -> text.toLowerCase(Locale.ROOT)).filter(text -> !text.isEmpty()).toList();
    }

    private static void outline(GuiGraphics graphics, ScreenRect rect, int color) {
        graphics.fill(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + 1, color);
        graphics.fill(rect.x(), rect.y(), rect.x() + 1, rect.y() + rect.height(), color);
        graphics.fill(rect.x(), rect.y() + rect.height() - 1, rect.x() + rect.width(), rect.y() + rect.height(), color);
        graphics.fill(rect.x() + rect.width() - 1, rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), color);
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
                               Set<AttributeModifier.Operation> operations) {}
    private record Contribution(ItemStack stack, AttributeModifier modifier) {}
    private record SourceTooltipLine(ItemStack stack, Component text) {}

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
            int tooltipWidth = sources.stream().mapToInt(line -> 20 + font.width(line.text)).max().orElse(20);
            int tooltipHeight = sources.size() * rowHeight;
            int x = mouseX + TooltipRenderUtil.MOUSE_OFFSET;
            int y = mouseY - TooltipRenderUtil.MOUSE_OFFSET;
            if (x + tooltipWidth + 6 > graphics.guiWidth()) x = mouseX - TooltipRenderUtil.MOUSE_OFFSET - tooltipWidth;
            if (y + tooltipHeight + 6 > graphics.guiHeight()) y = graphics.guiHeight() - tooltipHeight - 6;
            y = Math.max(6, y);

            graphics.pose().pushPose();
            TooltipRenderUtil.renderTooltipBackground(graphics, x, y, tooltipWidth, tooltipHeight, 400);
            graphics.pose().translate(0.0F, 0.0F, 400.0F);
            for (int row = 0; row < sources.size(); row++) {
                SourceTooltipLine line = sources.get(row);
                int rowY = y + row * rowHeight;
                graphics.renderItem(line.stack, x, rowY + 1);
                graphics.drawString(font, line.text, x + 20, rowY + 5, 0xFFFFFFFF, true);
            }
            graphics.pose().popPose();
        }
    }
}
