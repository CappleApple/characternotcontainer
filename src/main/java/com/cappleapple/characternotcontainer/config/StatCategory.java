package com.cappleapple.characternotcontainer.config;

import java.util.ArrayList;
import java.util.List;

public final class StatCategory {
    public String id = "general";
    public String name = "General";
    public int order = 0;
    public boolean initiallyCollapsed = false;
    public String icon = "";
    public List<StatDefinition> stats = new ArrayList<>();

    public StatCategory() {}

    public StatCategory(String id, String name, int order) {
        this.id = id;
        this.name = name;
        this.order = order;
    }
}
