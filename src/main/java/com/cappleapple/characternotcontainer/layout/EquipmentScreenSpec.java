package com.cappleapple.characternotcontainer.layout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EquipmentScreenSpec {
    public String id = "character_not_container_equipment_screen_example";
    public String modId = "characternotcontainer";
    public int width = 400;
    public int height = 320;
    public List<Widget> widgets = new ArrayList<>();

    public Widget widget(String id) {
        return widgets.stream().filter(widget -> id.equals(widget.id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing required layout widget: " + id));
    }

    public List<Widget> widgetsOfCustomType(String customType) {
        return widgets.stream().filter(widget -> customType.equals(widget.props.get("customType"))).toList();
    }

    public static final class Widget {
        public String id = "";
        public String type = "";
        public int x;
        public int y;
        public int w;
        public int h;
        public String text = "";
        public String icon;
        public Map<String, String> props = new LinkedHashMap<>();
        public List<Widget> item_template = new ArrayList<>();
        public boolean hidden;
    }
}
