package io.github.moosasharwaan.appliedquartermaster.devices;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import io.github.moosasharwaan.appliedquartermaster.AppliedQuartermaster;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Remembers how much of each item a network held over the last day, for the tablet's "Falling stock" list.
 * <p>
 * A network is tracked from the first time a tablet reaches it. Every 10 minutes its item counts are copied
 * from AE2's own cached storage list (one pass over the item types, no searching of drives) into a compact
 * snapshot: one shared key list per network and an array of counts per snapshot. The last 70 minutes are kept
 * in 10-minute steps and the last day in hourly steps, about 30 snapshots. Kept in memory only.
 */
@EventBusSubscriber(modid = AppliedQuartermaster.MOD_ID)
public final class StockHistory {

    /** Ticks between snapshots (10 minutes). */
    public static final long INTERVAL = 12_000;
    private static final long HOUR = 72_000;
    private static final int RECENT_KEEP = 8;
    private static final int HOURLY_KEEP = 25;

    private static final Map<IGrid, StockHistory> TRACKED = new WeakHashMap<>();

    private record Snapshot(long time, long[] counts) {
    }

    /** An item that went down: how much, from what, and roughly when it runs out at that rate. */
    public record Falling(AEItemKey key, long before, long now, long minutesToEmpty) {
        public long drop() {
            return before - now;
        }
    }

    /** The falling items over a period, and the time span actually compared (shorter while history builds up). */
    public record Result(List<Falling> items, long spanTicks, boolean hasHistory) {
    }

    private final Object2IntOpenHashMap<AEKey> ids = new Object2IntOpenHashMap<>();
    private final List<AEItemKey> keys = new ArrayList<>();
    private final ArrayDeque<Snapshot> recent = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> hourly = new ArrayDeque<>();
    private long lastSample = Long.MIN_VALUE;
    private long lastHourly = Long.MIN_VALUE;

    private StockHistory() {
        ids.defaultReturnValue(-1);
    }

    /** Starts tracking a network (taking a first snapshot), or returns its history. */
    public static StockHistory track(IGrid grid, long now) {
        var history = TRACKED.get(grid);
        if (history == null) {
            history = new StockHistory();
            TRACKED.put(grid, history);
            history.sample(grid, now);
        }
        return history;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        long now = event.getServer().getTickCount();
        if (now % 200 != 0 || TRACKED.isEmpty()) {
            return;
        }
        for (var entry : new ArrayList<>(TRACKED.entrySet())) {
            var history = entry.getValue();
            if (now - history.lastSample >= INTERVAL) {
                history.sample(entry.getKey(), now);
            }
        }
    }

    /** Takes a snapshot now (also used by the development self-test). */
    public void sample(IGrid grid, long now) {
        var inventory = grid.getStorageService().getCachedInventory();
        long[] counts = new long[Math.max(keys.size(), 16)];
        for (var entry : inventory) {
            if (!(entry.getKey() instanceof AEItemKey item) || entry.getLongValue() <= 0) {
                continue;
            }
            int id = ids.getInt(item);
            if (id < 0) {
                id = keys.size();
                ids.put(item, id);
                keys.add(item);
            }
            if (id >= counts.length) {
                counts = Arrays.copyOf(counts, Math.max(id + 1, counts.length * 2));
            }
            counts[id] = entry.getLongValue();
        }
        var snapshot = new Snapshot(now, counts);
        recent.addLast(snapshot);
        while (recent.size() > RECENT_KEEP) {
            recent.removeFirst();
        }
        if (now - lastHourly >= HOUR || hourly.isEmpty()) {
            hourly.addLast(snapshot);
            lastHourly = now;
            while (hourly.size() > HOURLY_KEEP) {
                hourly.removeFirst();
            }
        }
        lastSample = now;
    }

    /** The newest snapshot at least {@code period} old, else the oldest one there is (while history builds up). */
    private Snapshot base(long now, long period) {
        Snapshot best = null;
        for (var list : List.of(recent, hourly)) {
            for (var s : list) {
                if (now - s.time() >= period && (best == null || s.time() > best.time())) {
                    best = s;
                }
            }
        }
        if (best != null) {
            return best;
        }
        for (var list : List.of(hourly, recent)) {
            for (var s : list) {
                if (best == null || s.time() < best.time()) {
                    best = s;
                }
            }
        }
        return best != null && now - best.time() > 0 ? best : null;
    }

    /** Items that went down over about {@code period} ticks, soonest to run out first. */
    public Result falling(IGrid grid, long now, long period, int limit) {
        var base = base(now, period);
        if (base == null || now - base.time() < 1200) {
            // Less than a minute of history: rates would be meaningless.
            return new Result(List.of(), 0, false);
        }
        long span = Math.max(1, now - base.time());
        var inventory = grid.getStorageService().getCachedInventory();
        var list = new ArrayList<Falling>();
        long[] before = base.counts();
        for (int id = 0; id < before.length && id < keys.size(); id++) {
            if (before[id] <= 0) {
                continue;
            }
            var key = keys.get(id);
            long current = inventory.get(key);
            if (current >= before[id]) {
                continue;
            }
            double perTick = (before[id] - current) / (double) span;
            long minutes = current <= 0 ? 0 : (long) Math.ceil(current / perTick / 1200.0);
            list.add(new Falling(key, before[id], current, minutes));
        }
        list.sort((a, b) -> a.minutesToEmpty() != b.minutesToEmpty()
                ? Long.compare(a.minutesToEmpty(), b.minutesToEmpty())
                : Long.compare(b.drop(), a.drop()));
        return new Result(list.size() > limit ? List.copyOf(list.subList(0, limit)) : list, span, true);
    }
}
