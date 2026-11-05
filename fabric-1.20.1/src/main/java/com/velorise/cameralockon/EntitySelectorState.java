package com.velorise.cameralockon;

import java.util.HashMap;
import java.util.Map;

/** Keeps search and scroll positions when an entity selector is reopened. */
public final class EntitySelectorState {
    private static final Map<String, State> STATES = new HashMap<>();

    private EntitySelectorState() {
    }

    public static synchronized State get(String key) {
        State state = STATES.computeIfAbsent(key == null ? "default" : key, ignored -> new State());
        return state.copy();
    }

    public static synchronized void save(
            String key,
            String searchText,
            int scrollOffset,
            String highlightedEntityId
    ) {
        State state = STATES.computeIfAbsent(key == null ? "default" : key, ignored -> new State());
        state.searchText = searchText == null ? "" : searchText;
        state.scrollOffset = Math.max(0, scrollOffset);
        state.highlightedEntityId = highlightedEntityId == null ? "" : highlightedEntityId;
    }

    public static final class State {
        private String searchText = "";
        private int scrollOffset;
        private String highlightedEntityId = "";

        private State copy() {
            State result = new State();
            result.searchText = this.searchText;
            result.scrollOffset = this.scrollOffset;
            result.highlightedEntityId = this.highlightedEntityId;
            return result;
        }

        public String searchText() {
            return this.searchText;
        }

        public int scrollOffset() {
            return this.scrollOffset;
        }

        public String highlightedEntityId() {
            return this.highlightedEntityId;
        }
    }
}
