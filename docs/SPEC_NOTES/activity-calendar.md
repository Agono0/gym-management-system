# M3ActivityCalendar — footer layout (Q7)

52-week heatmap (7 × 53 canvas grid) with day/month labels, a stats footer
("Year N: X total workouts • Current streak • Longest streak") and the "Less → More" legend.

## Footer layout — measured, not assumed
- The stats string and the legend block (fixed 100px right-aligned span, `FOOTER_LEGEND_SPAN`)
  share one footer line when they fit: `statsWidth + 8px > gridWidth − 100px` decides
  (`FOOTER_SEGMENT_GAP`). Wide cards keep the original single-line look.
- Below ~600px card width (grid ~560px at 11sp label-small) the segments no longer fit:
  the footer stacks — stats line at `footerY`, legend line at `footerY + 16` — and the bottom
  padding grows 26 → 42 (`FOOTER_STACKED_BOTTOM`), shrinking the grid cells to pay for the row.
- The stacked stats line ellipsizes to the grid width (`ellipsize`, char-wise with "…");
  the full string is always set on `accessibleText` for assistive tech and stays the
  single source of truth for the numbers.
- Text is measured with an off-scene `Text` layout (`textWidth`) at the drawn footer font
  (label-small 11sp), never hardcoded pixel guesses.
- Tests: `M3ActivityCalendarFooterTest` asserts the stack decision at the 420px card minimum
  and the wide single-line case via measured bounds.
