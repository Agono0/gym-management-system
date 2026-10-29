package io.m3fx.controls.data;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.search.M3SearchBar;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * High-density M3 Data Table with instant real-time search filtering, sortable column headers,
 * custom cell formatting (badges, chips, sparklines), and pagination controls.
 *
 * @param <T> data item type
 */
public class M3DataTable<T> extends VBox {

    /** Model for a table column. */
    public static class Column<T> {
        private final String header;
        private final Function<T, String> valueExtractor;
        private BiFunction<T, String, Node> cellFactory;
        private double prefWidth = 120.0;

        /**
         * Creates a column with the default 120px preferred width.
         *
         * @param header column header text
         * @param valueExtractor plain-text value used for cells, search and sorting
         */
        public Column(String header, Function<T, String> valueExtractor) {
            this.header = header;
            this.valueExtractor = valueExtractor;
        }

        /**
         * Creates a column with a preferred width.
         *
         * @param header column header text
         * @param valueExtractor plain-text value used for cells, search and sorting
         * @param width preferred column width in px
         */
        public Column(String header, Function<T, String> valueExtractor, double width) {
            this(header, valueExtractor);
            this.prefWidth = width;
        }

        /**
         * Builder-style hook for a custom cell renderer (badge, chip, sparkline...).
         *
         * @param cellFactory receives the row item and the extracted plain-text value
         * @return this column, for chaining
         */
        public Column<T> withCellFactory(BiFunction<T, String, Node> cellFactory) {
            this.cellFactory = cellFactory;
            return this;
        }

        /** @return the column header text */
        public String getHeader() { return header; }

        /** @return the plain-text value extractor used for cells, search and sorting */
        public Function<T, String> getValueExtractor() { return valueExtractor; }

        /** @return the custom cell renderer, or {@code null} for a plain label */
        public BiFunction<T, String, Node> getCellFactory() { return cellFactory; }

        /** @return the preferred column width in px */
        public double getPrefWidth() { return prefWidth; }
    }

    private final ObservableList<Column<T>> columns = FXCollections.observableArrayList();
    private final ObservableList<T> masterItems = FXCollections.observableArrayList();
    private final ObservableList<T> filteredItems = FXCollections.observableArrayList();

    private final IntegerProperty pageSize = new SimpleIntegerProperty(this, "pageSize", 8);
    private final IntegerProperty currentPage = new SimpleIntegerProperty(this, "currentPage", 0);

    private int sortColumnIndex = -1;
    private boolean sortAscending = true;

    private final M3SearchBar searchBar = new M3SearchBar("Search table data...");
    private final HBox headerRow = new HBox(8);
    private final VBox rowsContainer = new VBox(2);
    private final HBox paginationBar = new HBox(12);
    private final Label pageInfoLabel = new Label();
    private final M3IconButton prevBtn = new M3IconButton(M3Icon.symbol("arrow_back"), IconButtonVariant.STANDARD);
    private final M3IconButton nextBtn = new M3IconButton(M3Icon.symbol("arrow_forward"), IconButtonVariant.STANDARD);

    /** Creates an empty data table. */
    public M3DataTable() {
        getStyleClass().add("m3-data-table");
        setSpacing(8);

        headerRow.getStyleClass().add("m3-data-table-header");
        headerRow.setPadding(new Insets(10, 16, 10, 16));
        headerRow.setMinHeight(56); // M3 data-table header row height

        rowsContainer.getStyleClass().add("m3-data-table-rows");

        paginationBar.setAlignment(Pos.CENTER_RIGHT);
        paginationBar.setPadding(new Insets(8, 12, 8, 12));
        pageInfoLabel.getStyleClass().add("m3-data-table-page-info");

        prevBtn.setOnAction(e -> prevPage());
        nextBtn.setOnAction(e -> nextPage());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        paginationBar.getChildren().addAll(pageInfoLabel, spacer, prevBtn, nextBtn);

        getChildren().addAll(searchBar, headerRow, rowsContainer, paginationBar);

        searchBar.queryProperty().addListener((obs, o, text) -> filterItems(text));
        masterItems.addListener((ListChangeListener<T>) c -> filterItems(searchBar.getQuery()));
        columns.addListener((ListChangeListener<Column<T>>) c -> {
            rebuildHeaders();
            rebuildRows();
        });
        currentPage.addListener((obs, o, n) -> rebuildRows());
        pageSize.addListener((obs, o, n) -> rebuildRows());
    }

    /** The column definitions driving headers, cells, search and sorting. */
    public ObservableList<Column<T>> getColumns() {
        return columns;
    }

    /** The full, unfiltered data model. */
    public ObservableList<T> getItems() {
        return masterItems;
    }

    /** Rows shown per page (min 1 enforced when paginating). */
    public IntegerProperty pageSizeProperty() {
        return pageSize;
    }

    public int getPageSize() {
        return pageSize.get();
    }

    public void setPageSize(int size) {
        this.pageSize.set(size);
    }

    /** The zero-based page currently displayed (clamped when rendered). */
    public IntegerProperty currentPageProperty() {
        return currentPage;
    }

    public int getCurrentPage() {
        return currentPage.get();
    }

    public void setCurrentPage(int page) {
        this.currentPage.set(page);
    }

    /** Advances to the next page when one exists. */
    public void nextPage() {
        int size = Math.max(1, getPageSize());
        int maxPages = Math.max(1, (int) Math.ceil((double) filteredItems.size() / size));
        if (getCurrentPage() < maxPages - 1) {
            setCurrentPage(getCurrentPage() + 1);
        }
    }

    /** Returns to the previous page when not on the first page. */
    public void prevPage() {
        if (getCurrentPage() > 0) {
            setCurrentPage(getCurrentPage() - 1);
        }
    }

    private void filterItems(String query) {
        filteredItems.clear();
        if (query == null || query.isBlank()) {
            filteredItems.addAll(masterItems);
        } else {
            String q = query.toLowerCase().trim();
            for (T item : masterItems) {
                boolean matches = false;
                for (Column<T> col : columns) {
                    String val = col.getValueExtractor().apply(item);
                    if (val != null && val.toLowerCase().contains(q)) {
                        matches = true;
                        break;
                    }
                }
                if (matches) {
                    filteredItems.add(item);
                }
            }
        }
        applySort();
        setCurrentPage(0);
        rebuildRows();
    }

    /** Sorts the filtered list by the active sort column, if any. */
    private void applySort() {
        if (sortColumnIndex < 0 || sortColumnIndex >= columns.size()) {
            return;
        }
        Function<T, String> extractor = columns.get(sortColumnIndex).getValueExtractor();
        if (extractor == null) {
            return;
        }
        Comparator<T> cmp = Comparator.comparing(
                (T item) -> {
                    String s = extractor.apply(item);
                    return s == null ? "" : s.toLowerCase();
                });
        if (!sortAscending) {
            cmp = cmp.reversed();
        }
        filteredItems.sort(cmp);
    }

    private void rebuildHeaders() {
        headerRow.getChildren().clear();
        for (int i = 0; i < columns.size(); i++) {
            Column<T> col = columns.get(i);
            int colIndex = i;

            Label lbl = new Label();
            lbl.getStyleClass().add("m3-data-table-header-label");
            lbl.setPrefWidth(col.getPrefWidth());

            boolean sortable = col.getValueExtractor() != null;
            if (sortable) {
                // Click-to-sort header: ascending -> descending -> off (back to data order)
                lbl.setAccessibleText("Sort by " + col.getHeader());
                lbl.getStyleClass().add("sortable");
                lbl.setCursor(javafx.scene.Cursor.HAND);
                lbl.setOnMouseClicked(e -> {
                    if (sortColumnIndex == colIndex) {
                        if (sortAscending) {
                            sortAscending = false;
                        } else {
                            // Third click clears the sort and restores master data order
                            sortColumnIndex = -1;
                            refreshHeaderSortMarks();
                            filterItems(searchBar.getQuery());
                            return;
                        }
                    } else {
                        sortColumnIndex = colIndex;
                        sortAscending = true;
                    }
                    refreshHeaderSortMarks();
                    applySort();
                    rebuildRows();
                });
            }
            lbl.setText(headerText(col, colIndex));
            headerRow.getChildren().add(lbl);
        }
    }

    private String headerText(Column<T> col, int colIndex) {
        if (sortColumnIndex == colIndex) {
            return col.getHeader() + (sortAscending ? " \u25B2" : " \u25BC");
        }
        return col.getHeader();
    }

    private void refreshHeaderSortMarks() {
        for (int i = 0; i < columns.size() && i < headerRow.getChildren().size(); i++) {
            Node node = headerRow.getChildren().get(i);
            if (node instanceof Label lbl) {
                lbl.setText(headerText(columns.get(i), i));
            }
        }
    }

    private void rebuildRows() {
        rowsContainer.getChildren().clear();
        int total = filteredItems.size();
        int size = Math.max(1, getPageSize());
        int maxPages = Math.max(1, (int) Math.ceil((double) total / size));
        int page = Math.min(getCurrentPage(), maxPages - 1);

        int start = page * size;
        int end = Math.min(start + size, total);

        pageInfoLabel.setText(String.format("Showing %d–%d of %d items (Page %d of %d)",
                total == 0 ? 0 : start + 1, end, total, page + 1, maxPages));
        prevBtn.setDisable(page <= 0);
        nextBtn.setDisable(page >= maxPages - 1);

        for (int i = start; i < end; i++) {
            T item = filteredItems.get(i);
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(8, 16, 8, 16));
            row.setMinHeight(52); // M3 data-table minimum row height
            row.getStyleClass().add("m3-data-table-row");

            // Zebra striping alternates by position within the visible page
            if ((i - start) % 2 == 1) {
                row.getStyleClass().add("alt");
            }

            for (Column<T> col : columns) {
                String valStr = col.getValueExtractor().apply(item);
                Node cellNode;
                if (col.getCellFactory() != null) {
                    cellNode = col.getCellFactory().apply(item, valStr);
                } else {
                    Label cellLabel = new Label(valStr == null ? "" : valStr);
                    cellLabel.getStyleClass().add("m3-data-table-cell");
                    cellNode = cellLabel;
                }
                if (cellNode instanceof Region r) {
                    r.setPrefWidth(col.getPrefWidth());
                }
                row.getChildren().add(cellNode);
            }
            rowsContainer.getChildren().add(row);
        }
    }
}
