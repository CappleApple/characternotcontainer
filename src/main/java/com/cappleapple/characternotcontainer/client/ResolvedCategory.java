package com.cappleapple.characternotcontainer.client;

import com.cappleapple.characternotcontainer.config.StatCategory;

import java.util.List;

record ResolvedCategory(StatCategory definition, List<ResolvedStat> stats) {}
