package com.sourcegraph.utils;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by beyang on 3/22/17.
 */
public final class AndroidUtil {

    /**
     * Maps Android support artifact groups to their directories on disk.
     */
    public static final Map<String, String> groupToDir;
    static {
        Map<String, String> m = new HashMap<>();
        groupToDir = ImmutableMap.copyOf(m);
    }
}
