package utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static utils.TextUtils.*;

/**
 * @author Denis Danilin | denis@danilin.name
 * 05.05.2020
 * tfs ☭ sweat and blood
 */
public class MdPadTable {
    private static final int MAX_WIDTH = Integer.MAX_VALUE;
    private static final Pattern byLen = Pattern.compile("(?<=\\G.{" + MAX_WIDTH + "})");

    public enum Align {LEFT, RIGHT, CENTER}

    private final int cols;
    private final String title;
    private final String[] colHeads;
    private final List[] values;
    private final int[] maxWidth;
    private final AtomicInteger colPtr = new AtomicInteger(0);
    private final Align[] aligns;
    private final AtomicBoolean lastColUnformatted;

    private MdPadTable(final int cols, final String title, final String[] colHeads) {
        this.cols = cols;
        this.title = title;
        this.colHeads = colHeads;
        values = new List[cols];
        for (int i = 0; i < cols; i++)
            values[i] = new ArrayList<String>(0);
        maxWidth = new int[cols];
        if (colHeads == null)
            Arrays.fill(maxWidth, 0);
        else
            for (int i = 0; i < cols; i++)
                maxWidth[i] = notNull(colHeads[i]).length();

        aligns = new Align[cols];
        Arrays.fill(aligns, Align.LEFT);
        lastColUnformatted = new AtomicBoolean(false);
    }

    public MdPadTable(final int cols, final String title) {
        this(cols, title, null);
    }

    public MdPadTable(final int cols) {
        this(cols, null);
    }

    public MdPadTable(final String[] colHeads) {
        this(null, colHeads);
    }

    public MdPadTable(final String title, final String[] colHeads) {
        this(colHeads.length, title, colHeads);
    }

    public void setLastColUnformatted(final boolean unformatted) {
        if (unformatted && cols == 1)
            return;

        lastColUnformatted.set(unformatted);
    }

    public void setAligns(final Align... aligns) {
        if (aligns == null || aligns.length != this.aligns.length)
            throw new IllegalArgumentException();

        System.arraycopy(aligns, 0, this.aligns, 0, this.aligns.length);
    }

    public void setAlign(final int col, final Align align) {
        aligns[col] = align;
    }

    public void setAlign(final String colHead, final Align align) {
        aligns[Arrays.binarySearch(colHeads, colHead)] = align;
    }

    public void add() {
        add(null);
    }

    public void newRow() {
        while (colPtr.get() != 0) add();
    }

    @SuppressWarnings("unchecked")
    public void add(final Object value) {
        maxWidth[colPtr.get()] = Math.max(maxWidth[colPtr.get()], value == null ? 0 : notNull(value).length());
        values[colPtr.getAndIncrement()].add(value != null ? String.valueOf(value).trim() : null);
        colPtr.compareAndSet(cols, 0);
    }

    public void addRow(final Collection<?> data) {
        if (colPtr.get() != 0)
            newRow();
        data.forEach(this::add);
    }

    public void addFlow(final Collection<?> data) {
        data.forEach(this::add);
    }

    private int tuneWidth() {
        boolean have2cut;
        int tableWidth = 0;

        do {
            tableWidth = 0;
            for (int i = 0; i < cols; i++)
                tableWidth += maxWidth[i];
            tableWidth += cols - 1;

            have2cut = tableWidth > MAX_WIDTH;
            int col2cut = have2cut ? Arrays.stream(maxWidth).max().orElse(-1) : -1;
            for (int i = 0; i < maxWidth.length; i++)
                if (maxWidth[i] == col2cut) {
                    col2cut = i;
                    break;
                }
            final int diff = have2cut ? tableWidth - MAX_WIDTH : -1;

            if (have2cut)
                maxWidth[col2cut] -= Math.min(diff, maxWidth[col2cut] / 4);
        } while (have2cut);

        return tableWidth;
    }

    public String toString() {
        final StringBuilder out = new StringBuilder();

        if (values[0].isEmpty())
            return "";

        tuneWidth();

        if (title != null) {
            Arrays.stream(byLen.split(title))
                    .forEach(s -> out.append("*").append(s).append("*\n"));
        }

        out.append("```\n");

        if (colHeads != null) {
            out.append(row(colHeads));
            out.append(row(IntStream.range(0, cols).mapToObj(i -> {
                final StringBuilder s = new StringBuilder(maxWidth[i]);
                for (int j = 0; j < colHeads[i].length(); j++) s.append('-');
                return s.toString();
            }).toArray(String[]::new)));
        }

        if (lastColUnformatted.get())
            out.append("```");

        final int rows = values[0].size();

        for (int row = 0; row < rows; row++) {
            final int finalRow = row;
            out.append(row(Arrays.stream(values).map(list -> String.valueOf(list.get(finalRow))).toArray(String[]::new)));
        }

        if (!lastColUnformatted.get())
            out.append("\n```");

        return out.toString();
    }

    private String row(String... values) {
        final StringBuilder row = new StringBuilder();
        String[] append = null;

        if (lastColUnformatted.get())
            row.append("`");

        for (int i = 0; i < values.length; i++) {
            final char[] cell = new char[maxWidth[i]];
            Arrays.fill(cell, ' ');
            String val = values[i];

            if (!isEmpty(val)) {
                if (val.length() > cell.length) {
                    if (append == null)
                        append = new String[values.length];

                    append[i] = val.substring(cell.length);
                    val = val.substring(0, cell.length);
                }

                switch (aligns[i]) {
                    case LEFT:
                        System.arraycopy(val.toCharArray(), 0, cell, 0, val.length());
                        break;
                    case RIGHT:
                        System.arraycopy(val.toCharArray(), 0, cell, cell.length - val.length(), val.length());
                        break;
                    case CENTER:
                        System.arraycopy(val.toCharArray(), 0, cell, (cell.length - val.length()) / 2, val.length());
                        break;
                }
            }

            row.append(escapeMd(String.valueOf(cell)));
            row.append(i < values.length - 1 ? ' ' : '\n');
            if (lastColUnformatted.get() && i == values.length - 2)
                row.append('`');
        }

        if (append != null)
            row.append(row(append));

        return row.toString();
    }


}
