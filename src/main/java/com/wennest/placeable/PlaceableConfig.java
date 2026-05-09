package com.wennest.placeable;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoConfig-managed persistent settings for the mod.
 *
 * <p><b>Thread-safety</b>: the {@link #allowPlaceablePlants} map is read on
 * the server thread (mixin {@code canPlaceAt} entry) AND written on the
 * render thread (Mod Menu / Cloth Config UI). To avoid
 * {@link java.util.ConcurrentModificationException} (and torn reads) the
 * storage is a {@link ConcurrentHashMap}. {@link java.util.EnumMap} would be
 * marginally faster but does not provide concurrent semantics; the difference
 * is negligible for a small map read at canPlaceAt frequency.
 *
 * <p><b>Upgrade safety</b>: {@link #validatePostLoad()} fills any
 * newly-introduced enum keys with {@code true} so an old config file (written
 * before, e.g., {@code POTATOES} was added) doesn't end up with the new
 * plants implicitly disabled. It also drops orphan keys that no longer map
 * to a current enum value, preventing silent bloat after a downgrade.
 */
@Config(name = Placeable.MODID)
public class PlaceableConfig implements ConfigData {

    @Comment("Enable or disable the mod.")
    public boolean enable = true;

    @Comment("Allow placement on blocks without a top rim.")
    public boolean placedWithoutTopRim = false;

    @Comment("Allow or disable specific plants.")
    public Map<PlaceablePlants, Boolean> allowPlaceablePlants = new ConcurrentHashMap<>();

    public PlaceableConfig() {
        // New install: every plant enabled by default. The same default applies
        // to keys added in later mod versions — see validatePostLoad.
        for (PlaceablePlants e : PlaceablePlants.values()) {
            allowPlaceablePlants.put(e, true);
        }
    }

    /**
     * Reconcile the on-disk map with the current {@link PlaceablePlants} enum.
     *
     * <p>Behaviour:
     * <ul>
     *   <li><b>Missing keys</b> (enum entry exists but the JSON file was
     *       written by an older mod version that did not know about it):
     *       inserted with value {@code true}. New plants default to enabled.</li>
     *   <li><b>Orphan keys</b> (the JSON file was written by a NEWER mod
     *       version that knew about an entry that this version's enum no
     *       longer contains, e.g., after a downgrade): removed. Without this,
     *       the map would slowly accumulate dead entries across version churn.</li>
     * </ul>
     *
     * <p>AutoConfig deserializes the {@code Map<PlaceablePlants, Boolean>}
     * via Gson; if the source JSON contains an unknown enum name Gson by
     * default stores the value under {@code null}. Those nulls are stripped
     * here too.
     *
     * <p>Safe to call multiple times; idempotent.
     */
    public void validatePostLoad() {
        // Defensive replacement: AutoConfig may have left the field as a plain
        // EnumMap or HashMap if the on-disk JSON was hand-edited. Always
        // promote to ConcurrentHashMap before any reader can race against us.
        if (!(allowPlaceablePlants instanceof ConcurrentHashMap)) {
            Map<PlaceablePlants, Boolean> migrated = new ConcurrentHashMap<>();
            if (allowPlaceablePlants != null) {
                for (Map.Entry<PlaceablePlants, Boolean> e : allowPlaceablePlants.entrySet()) {
                    if (e.getKey() != null && e.getValue() != null) {
                        migrated.put(e.getKey(), e.getValue());
                    }
                }
            }
            allowPlaceablePlants = migrated;
        }

        // Fill any missing enum keys with the "new plant defaults to enabled"
        // policy. putIfAbsent keeps existing user-customised values.
        for (PlaceablePlants p : PlaceablePlants.values()) {
            allowPlaceablePlants.putIfAbsent(p, Boolean.TRUE);
        }

        // Drop orphan entries. Snapshot the keyset first rather than iterating
        // the map directly — concurrent iteration semantics vary across JVM
        // implementations. EnumSet.allOf is the most compact "set of every
        // enum value" — a bitmask under the hood and faster to build than
        // HashSet.
        Set<PlaceablePlants> known = EnumSet.allOf(PlaceablePlants.class);
        Iterator<PlaceablePlants> it = allowPlaceablePlants.keySet().iterator();
        while (it.hasNext()) {
            PlaceablePlants k = it.next();
            if (k == null || !known.contains(k)) {
                it.remove();
            }
        }
    }

    /**
     * Read-side accessor used by mixins. Returns the stored boolean, falling
     * back to {@code true} if {@link #validatePostLoad()} hasn't run yet
     * (e.g., during early initialisation) — matching the default-enabled
     * policy for new plants.
     */
    public boolean isPlantAllowed(PlaceablePlants plant) {
        Boolean v = allowPlaceablePlants.get(plant);
        return v == null || v;
    }
}
