package com.sourcegraph.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.sourcegraph.utils.Util.LogLevel.*;

/**
 * Created by beyang on 1/27/17.
 */
public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static void waitFor(CountDownLatch latch, String errorMessage, long timeout, TimeUnit unit) {
        try {
            latch.await(timeout, unit);
        } catch (InterruptedException exception) {
            log.error(errorMessage);
            throw new RuntimeException(exception);
        }
    }

    public static void waitFor(CountDownLatch latch, String errorMessage) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            log.error(errorMessage);
            throw new RuntimeException(exception);
        }
    }

    private static final String[] SENTINELS = {
            "★★★", "♠♠♠", "♥♥♥", "000", "|||", "===", "&&&", "???", "$$$", "@@@", "(((", "\\\\\\",
            ")))", "+++", "...", "---", "///", "%%%", "¢¢¢", "€€€", "£££", "¥¥¥", "©©©", "§§§",
            "¶¶¶", "†††", "‡‡‡", "•••", "‣‣‣", "✓✓✓", "■■■", "◆◆◆", "●●●", "▼▼▼", "▲▲▲", "◀◀◀",
            "▶▶▶", "⊂⊂⊂", "⊃⊃⊃", "⋂⋂⋂", "⋃⋃⋃", "ℕℕℕ", "ℤℤℤ", "ℚℚℚ", "ℝℝℝ", "⌈⌈⌈", "⌉⌉⌉", "∑∑∑"
    };
    private static Random rand = new Random();

    public enum LogLevel {
        INFO,
        DEBUG,
        TRACE,
        WARN,
        ERROR
    }

    public static class Timer {
        private String sentinel;
        private String text;
        private Object[] args;
        private long start;
        public Timer(String sentinel, String text, Object[] args, long start) {
            this.sentinel = sentinel;
            this.text = text;
            this.args = args;
            this.start = start;
        }

        public void debug(String text, Object... extraArgs) {
            logBasic(DEBUG, "    " + text, false, extraArgs);
        }
        public void warn(String text, Object... extraArgs) {
            logBasic(WARN, "    " + text, false, extraArgs);
        }
        public void trace(String text, Object... extraArgs) {
            logBasic(TRACE, "    " + text, false, extraArgs);
        }
        public void error(String text, Object... extraArgs) {
            logBasic(ERROR, "    " + text, false, extraArgs);
        }
        public void log(String text, Object... extraArgs) {
            logBasic(INFO, "    " + text, false, extraArgs);
        }
        public void end(Object... extraArgs) {
            logBasic(INFO,"[■] " + text, true, extraArgs);
        }

        private void logBasic(LogLevel level, String text, boolean includeOriginal, Object... extraArgs) {
            long dur = System.currentTimeMillis() - start;
            List<Object> allArgs;
            if (includeOriginal) {
                allArgs = new ArrayList<>(args.length + extraArgs.length + 2);
                for (int i = 0; i < args.length; i++) {
                    allArgs.add(args[i]);
                }
            } else {
                allArgs = new ArrayList<>(extraArgs.length + 2);
            }
            for (int i = 0; i < extraArgs.length; i++) {
                allArgs.add(extraArgs[i]);
            }
            allArgs.add("elapsed");
            allArgs.add(dur + "ms");
            log15(level, sentinel + " " + text, allArgs.toArray());
        }
    }

    public static Timer timeStartQuiet(String text, Object... args) {
        return new Timer("   ", text, args, System.currentTimeMillis());
    }

    public static Timer timeStart(String text, Object... args) {
        String sentinel = SENTINELS[rand.nextInt(SENTINELS.length)];
        log15(INFO, sentinel + " [▶] " + text, args);
        return new Timer(sentinel, text, args, System.currentTimeMillis());
    }

    private static void log15(LogLevel level, String msg, Object... args) {
        StringBuilder sb = new StringBuilder(msg);
        sb.append("\t");

        if (args.length % 2 != 0) {
            throw new RuntimeException("expected even number of arguments (1-to-1 label-value list)");
        }
        Object[] values = new Object[args.length / 2];
        for (int i = 0; i < args.length / 2; i ++) {
            sb.append(args[2*i].toString()+"={} ");
            values[i] = args[2*i+1];
        }
        switch (level) {
            case DEBUG:
                log.debug(sb.toString(), values);
                break;
            case TRACE:
                log.trace(sb.toString(), values);
                break;
            case WARN:
                log.warn(sb.toString(), values);
                break;
            case ERROR:
                log.error(sb.toString(), values);
                break;
            case INFO:
            default:
                log.info(sb.toString(), values);
        }
    }
}
