package com.bookie.components;

public class VirtualScrollManager {

    private static final int OVERSCAN_ROWS = 15;
    private static final int DEFAULT_INITIAL_WINDOW = 100;

    private final int rowHeightPx;
    private int scrollTop = 0;
    private int viewportHeight = 0;

    public VirtualScrollManager(int rowHeightPx) {
        this.rowHeightPx = rowHeightPx;
    }

    public void updateViewport(int scrollTop, int viewportHeight) {
        this.scrollTop = scrollTop;
        this.viewportHeight = viewportHeight;
    }

    public void reset() {
        this.scrollTop = 0;
        this.viewportHeight = 0;
    }

    public int[] computeWindow(int totalRows) {
        if (viewportHeight == 0) {
            return new int[]{0, Math.min(DEFAULT_INITIAL_WINDOW, totalRows)};
        }

        int firstVisible = scrollTop / rowHeightPx;
        int visibleCount = (int) Math.ceil((double) viewportHeight / rowHeightPx);
        int startIndex = Math.max(0, firstVisible - OVERSCAN_ROWS);
        int endIndex = Math.min(totalRows, firstVisible + visibleCount + OVERSCAN_ROWS);

        if (totalRows <= endIndex - startIndex) {
            return new int[]{0, totalRows};
        }

        if (startIndex >= totalRows) {
            endIndex = totalRows;
            startIndex = Math.max(0, totalRows - (OVERSCAN_ROWS * 2 + visibleCount));
        }

        return new int[]{startIndex, endIndex};
    }

}
