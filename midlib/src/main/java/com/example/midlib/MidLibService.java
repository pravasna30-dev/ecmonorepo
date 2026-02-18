package com.example.midlib;

import java.util.Collections;
import java.util.List;

/**
 * Mid-level library service.
 * API Version: 1.0.0
 */
public class MidLibService {

    private static final String LEVEL = "MID";
    private static final String VERSION = "1.0.0";

    /**
     * Print the library identity to stdout.
     */
    public static void say() {
        System.out.println("I am a " + LEVEL + " level library");
    }

    /**
     * Return the library level.
     */
    public String getLevel() {
        return LEVEL;
    }

    /**
     * Return the library version.
     */
    public String getVersion() {
        return VERSION;
    }

    /**
     * Return a formatted identity string.
     */
    public String identity() {
        return "mid-maven-lib v" + VERSION + " [" + LEVEL + "]";
    }

    /**
     * Process a list of items by prefixing each with the library level.
     *
     * @param items the items to process
     * @return a new list with prefixed items
     */
    public List<String> process(List<String> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> "[" + LEVEL + "] " + item)
                .toList();
    }
}
