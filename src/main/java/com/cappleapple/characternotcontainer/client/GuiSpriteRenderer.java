package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.CharacterNotContainer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** Backports resource-pack GUI sprite stretching, tiling and nine-slice scaling to 1.20.1. */
final class GuiSpriteRenderer {
    private static final Map<ResourceLocation, Sprite> CACHE = new HashMap<>();

    private GuiSpriteRenderer() {}

    static void clear() { CACHE.clear(); }

    static void blit(GuiGraphics graphics, ResourceLocation id, int x, int y, int width, int height) {
        Sprite sprite = CACHE.computeIfAbsent(id, GuiSpriteRenderer::load);
        if (sprite == null || width <= 0 || height <= 0) return;
        if (sprite.type.equals("tile")) {
            tile(graphics, sprite, x, y, width, height, 0, 0, sprite.width, sprite.height);
        } else if (sprite.type.equals("nine_slice")) {
            int left = Math.min(sprite.left, width / 2);
            int right = Math.min(sprite.right, width / 2);
            int top = Math.min(sprite.top, height / 2);
            int bottom = Math.min(sprite.bottom, height / 2);
            int[] dx = {x, x + left, x + width - right, x + width};
            int[] dy = {y, y + top, y + height - bottom, y + height};
            int[] sx = {0, sprite.left, sprite.width - sprite.right, sprite.width};
            int[] sy = {0, sprite.top, sprite.height - sprite.bottom, sprite.height};
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
                tile(graphics, sprite, dx[col], dy[row], dx[col + 1] - dx[col], dy[row + 1] - dy[row],
                        sx[col], sy[row], sx[col + 1] - sx[col], sy[row + 1] - sy[row]);
            }
        } else {
            graphics.blit(sprite.texture, x, y, width, height, 0, 0,
                    sprite.width, sprite.height, sprite.width, sprite.height);
        }
    }

    private static void tile(GuiGraphics g, Sprite sprite, int x, int y, int width, int height,
                             int u, int v, int sourceWidth, int sourceHeight) {
        if (sourceWidth <= 0 || sourceHeight <= 0) return;
        for (int dy = 0; dy < height; dy += sourceHeight) {
            for (int dx = 0; dx < width; dx += sourceWidth) {
                int w = Math.min(sourceWidth, width - dx), h = Math.min(sourceHeight, height - dy);
                g.blit(sprite.texture, x + dx, y + dy, w, h, u, v, w, h, sprite.width, sprite.height);
            }
        }
    }

    private static Sprite load(ResourceLocation id) {
        ResourceLocation texture = CharacterGuiSprites.textureFile(id);
        var manager = Minecraft.getInstance().getResourceManager();
        var resource = manager.getResource(texture);
        if (resource.isEmpty()) return null;
        try (var stream = resource.get().open(); var image = NativeImage.read(stream)) {
            int width = image.getWidth(), height = image.getHeight();
            String type = "stretch";
            int left = 0, right = 0, top = 0, bottom = 0;
            var metadata = manager.getResource(new ResourceLocation(texture.getNamespace(), texture.getPath() + ".mcmeta"));
            if (metadata.isPresent()) {
                try (var reader = metadata.get().openAsReader()) {
                    JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                    if (root.has("gui") && root.getAsJsonObject("gui").has("scaling")) {
                        JsonObject scaling = root.getAsJsonObject("gui").getAsJsonObject("scaling");
                        type = scaling.get("type").getAsString();
                        if (!type.equals("stretch") && scaling.has("width") && scaling.has("height")) {
                            width = Math.max(1, scaling.get("width").getAsInt());
                            height = Math.max(1, scaling.get("height").getAsInt());
                        }
                        if (type.equals("nine_slice")) {
                            var border = scaling.get("border");
                            if (border.isJsonPrimitive()) left = right = top = bottom = border.getAsInt();
                            else {
                                var borders = border.getAsJsonObject();
                                left = borders.get("left").getAsInt(); right = borders.get("right").getAsInt();
                                top = borders.get("top").getAsInt(); bottom = borders.get("bottom").getAsInt();
                            }
                            if (Math.min(Math.min(left, right), Math.min(top, bottom)) < 0
                                    || left + right >= width || top + bottom >= height) type = "stretch";
                        }
                    }
                }
            }
            return new Sprite(texture, width, height, type, left, right, top, bottom);
        } catch (Exception exception) {
            CharacterNotContainer.LOGGER.warn("Could not load GUI sprite {}", texture, exception);
            return null;
        }
    }

    private record Sprite(ResourceLocation texture, int width, int height, String type,
                          int left, int right, int top, int bottom) {}
}
