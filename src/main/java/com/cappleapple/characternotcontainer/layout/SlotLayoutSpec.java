package com.cappleapple.characternotcontainer.layout;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Named Curios anchor positions and repeated-slot layout rules. */
public final class SlotLayoutSpec {
    public Map<String, Anchor> slots = defaultSlots();

    public static SlotLayoutSpec defaults() {
        return new SlotLayoutSpec();
    }

    public String targetFor(String anchorId, int occurrence) {
        String normalized = normalizeExisting(anchorId);
        Anchor anchor = slots.get(normalized);
        return anchor.alternate != null && (occurrence & 1) == 1 ? anchor.alternate : normalized;
    }

    public Anchor anchor(String anchorId) {
        return slots.get(normalizeExisting(anchorId));
    }

    public GridOffset offset(Anchor anchor, int index) {
        int primary = Math.floorMod(index, anchor.wrap);
        int secondary = Math.floorDiv(index, anchor.wrap);
        return anchor.direction.equals("vertical")
                ? new GridOffset(secondary, primary)
                : new GridOffset(primary, secondary);
    }

    public void validate() {
        if (slots == null || slots.isEmpty() || slots.size() > 256) {
            throw new IllegalArgumentException("Slot anchors must contain between 1 and 256 entries");
        }
        Map<String, Anchor> normalized = new LinkedHashMap<>();
        slots.forEach((id, anchor) -> {
            if (id == null || id.isBlank() || id.length() > 128 || anchor == null) {
                throw new IllegalArgumentException("Every slot anchor needs an ID and layout");
            }
            anchor.validate();
            normalized.put(id.toLowerCase(Locale.ROOT), anchor);
        });
        if (!normalized.containsKey("other")) {
            throw new IllegalArgumentException("slots.json must define the other fallback anchor");
        }
        normalized.forEach((id, anchor) -> {
            if (anchor.alternate == null) return;
            anchor.alternate = anchor.alternate.toLowerCase(Locale.ROOT);
            if (id.equals(anchor.alternate) || !normalized.containsKey(anchor.alternate)) {
                throw new IllegalArgumentException("Alternate anchor for " + id + " must name a different defined anchor");
            }
        });
        slots = normalized;
    }

    private String normalizeExisting(String anchorId) {
        if (anchorId == null || anchorId.isBlank()) return "other";
        String normalized = anchorId.toLowerCase(Locale.ROOT);
        return slots.containsKey(normalized) ? normalized : "other";
    }

    private static Map<String, Anchor> defaultSlots() {
        Map<String, Anchor> result = new LinkedHashMap<>();
        result.put("head", new Anchor(319, 52));
        result.put("neck", new Anchor(271, 91));
        result.put("back", new Anchor(196, 108));
        result.put("belt", new Anchor(271, 164));
        result.put("hands", new Anchor(199, 165, "horizontal", 2, 20, "right_hand"));
        result.put("left_hand", new Anchor(199, 165));
        result.put("right_hand", new Anchor(342, 165));
        result.put("feet", new Anchor(322, 260));
        result.put("other", new Anchor(350, 205, "vertical", 4, 20, null));
        return result;
    }

    public static final class Anchor {
        public int x;
        public int y;
        public String direction = "horizontal";
        public int wrap = 2;
        public int spacing = 20;
        public String alternate;

        public Anchor() {}

        public Anchor(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public Anchor(int x, int y, String direction, int wrap, int spacing, String alternate) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.wrap = wrap;
            this.spacing = spacing;
            this.alternate = alternate;
        }

        private void validate() {
            if (x < -4096 || x > 4096 || y < -4096 || y > 4096) {
                throw new IllegalArgumentException("Slot anchor coordinates must be between -4096 and 4096");
            }
            if (direction == null) direction = "horizontal";
            direction = direction.toLowerCase(Locale.ROOT);
            if (!direction.equals("horizontal") && !direction.equals("vertical")) {
                throw new IllegalArgumentException("Slot anchor direction must be horizontal or vertical");
            }
            if (wrap < 1 || wrap > 64 || spacing < -512 || spacing > 512) {
                throw new IllegalArgumentException("Slot anchor wrap or spacing is outside the supported range");
            }
            if (alternate != null && (alternate.isBlank() || alternate.length() > 128)) {
                throw new IllegalArgumentException("Alternate slot anchor IDs must be nonblank and at most 128 characters");
            }
        }
    }

    public record GridOffset(int x, int y) {}
}
