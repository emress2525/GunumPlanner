package com.emre.gunumplanner;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SmartPlanner {
    private SmartPlanner() {}

    public static List<Plan> propose(List<Db.Item> tasks, LocalDate targetDay) {
        List<Plan> out = new ArrayList<>();
        if (tasks == null || tasks.isEmpty()) return out;

        List<Db.Item> candidates = new ArrayList<>();
        List<Range> occupied = new ArrayList<>();
        long now = System.currentTimeMillis();

        for (Db.Item i : tasks) {
            if (!Db.TYPE_TASK.equals(i.type) || !Db.STATUS_OPEN.equals(i.status)) continue;
            if (i.dueAt > 0 && targetDay.equals(Instant.ofEpochMilli(i.dueAt).atZone(ZoneId.systemDefault()).toLocalDate()) && i.dueAt >= now) {
                int dur = i.durationMinutes > 0 ? i.durationMinutes : 30;
                occupied.add(new Range(i.dueAt, i.dueAt + dur * 60_000L));
            } else if (i.dueAt == 0 || i.dueAt < now || LocalDate.parse(i.dayKey).isBefore(targetDay)) {
                candidates.add(i);
            }
        }

        candidates.sort(Comparator.comparingInt((Db.Item x) -> x.priority).thenComparingLong(x -> x.createdAt));
        occupied.sort(Comparator.comparingLong(x -> x.start));

        LocalTime startTime;
        if (targetDay.equals(LocalDate.now())) {
            LocalTime n = LocalTime.now().plusMinutes(14).withSecond(0).withNano(0);
            int m = (n.getMinute() / 15) * 15;
            startTime = n.withMinute(m);
            if (startTime.isBefore(LocalTime.of(8, 0))) startTime = LocalTime.of(8, 0);
        } else {
            startTime = LocalTime.of(9, 0);
        }
        long cursor = epoch(targetDay, startTime);
        long endOfDay = epoch(targetDay, LocalTime.of(22, 0));

        for (Db.Item item : candidates) {
            int duration = item.durationMinutes > 0 ? item.durationMinutes : 30;
            long len = duration * 60_000L;
            cursor = findGap(cursor, len, occupied);
            LocalDate planDay = targetDay;
            if (cursor + len > endOfDay) {
                planDay = targetDay.plusDays(1);
                cursor = epoch(planDay, LocalTime.of(9, 0));
                occupied.clear();
                endOfDay = epoch(planDay, LocalTime.of(22, 0));
            }
            Plan p = new Plan();
            p.itemId = item.id;
            p.title = item.title;
            p.oldDayKey = item.dayKey;
            p.oldDueAt = item.dueAt;
            p.newDayKey = planDay.toString();
            p.newDueAt = cursor;
            p.durationMinutes = duration;
            out.add(p);
            occupied.add(new Range(cursor, cursor + len));
            occupied.sort(Comparator.comparingLong(x -> x.start));
            cursor += len;
        }
        return out;
    }

    private static long findGap(long cursor, long len, List<Range> occupied) {
        boolean moved;
        do {
            moved = false;
            for (Range r : occupied) {
                if (cursor < r.end && cursor + len > r.start) {
                    cursor = r.end;
                    moved = true;
                }
            }
        } while (moved);
        return cursor;
    }

    private static long epoch(LocalDate date, LocalTime time) {
        return LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static class Range {
        final long start;
        final long end;
        Range(long start, long end) { this.start = start; this.end = end; }
    }

    public static class Plan {
        public long itemId;
        public String title;
        public String oldDayKey;
        public long oldDueAt;
        public String newDayKey;
        public long newDueAt;
        public int durationMinutes;
    }
}
