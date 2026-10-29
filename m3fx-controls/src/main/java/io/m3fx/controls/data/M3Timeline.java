package io.m3fx.controls.data;

import io.m3fx.controls.icon.M3Icon;
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
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Vertical Activity Timeline with illuminated status nodes, connecting progress pipelines,
 * and collapsible activity entries.
 */
public class M3Timeline extends VBox {

    /** Status of a timeline node. */
    public enum Status {
        /** Finished entry: filled check dot. */
        COMPLETED,
        /** Current entry: halo with a bright core. */
        ACTIVE,
        /** Upcoming entry: hollow outline dot. */
        PENDING
    }

    /** Model for a timeline entry. */
    public static class Item {
        private final String timestamp;
        private final String title;
        private final String description;
        private final Status status;
        private Node customNode;

        /**
         * Creates a timeline entry.
         *
         * @param timestamp time label shown above the title
         * @param title entry headline
         * @param description supporting text
         * @param status node status ({@code null} falls back to {@link Status#PENDING})
         */
        public Item(String timestamp, String title, String description, Status status) {
            this.timestamp = timestamp;
            this.title = title;
            this.description = description;
            this.status = status == null ? Status.PENDING : status;
        }

        /**
         * Builder-style hook for extra content below the description.
         *
         * @param node arbitrary content node (chart, chip, button...)
         * @return this item, for chaining
         */
        public Item withCustomNode(Node node) {
            this.customNode = node;
            return this;
        }

        /** @return the time label */
        public String getTimestamp() { return timestamp; }

        /** @return the entry headline */
        public String getTitle() { return title; }

        /** @return the supporting text */
        public String getDescription() { return description; }

        /** @return the node status */
        public Status getStatus() { return status; }

        /** @return the custom content node, or {@code null} */
        public Node getCustomNode() { return customNode; }
    }

    private final ObservableList<Item> items = FXCollections.observableArrayList();

    /** Creates an empty timeline. */
    public M3Timeline() {
        getStyleClass().add("m3-timeline");
        setSpacing(0);
        items.addListener((ListChangeListener<Item>) c -> rebuild());
    }

    /** The observable list of entries rendered top to bottom. */
    public ObservableList<Item> getItems() {
        return items;
    }

    /**
     * Convenience factory appending one entry.
     *
     * @param timestamp time label
     * @param title entry headline
     * @param description supporting text
     * @param status node status
     */
    public void addItem(String timestamp, String title, String description, Status status) {
        items.add(new Item(timestamp, title, description, status));
    }

    private void rebuild() {
        getChildren().clear();
        int n = items.size();

        for (int i = 0; i < n; i++) {
            Item item = items.get(i);
            boolean isLast = i == n - 1;

            HBox row = new HBox(12);
            row.setAlignment(Pos.TOP_LEFT);

            // Left Node & Line column
            VBox nodeCol = new VBox();
            nodeCol.setAlignment(Pos.TOP_CENTER);
            nodeCol.setPrefWidth(24);

            Node indicator = buildIndicator(item.getStatus());
            nodeCol.getChildren().add(indicator);

            if (!isLast) {
                Region line = new Region();
                line.setPrefWidth(2.0);
                line.setMinWidth(2.0);
                line.setMaxWidth(2.0);
                line.getStyleClass().add("m3-timeline-line");
                // Color the connector when the entry above it is already finished
                if (item.getStatus() == Status.COMPLETED) {
                    line.getStyleClass().add("completed");
                }
                VBox.setVgrow(line, Priority.ALWAYS);
                line.setMinHeight(32.0);
                nodeCol.getChildren().add(line);
            }

            // Right Content Column
            VBox contentCol = new VBox(4);
            contentCol.setPadding(new Insets(0, 0, isLast ? 0 : 20, 0));

            Label timeLbl = new Label(item.getTimestamp());
            timeLbl.getStyleClass().add("m3-timeline-time");

            Label titleLbl = new Label(item.getTitle());
            titleLbl.getStyleClass().add("m3-timeline-title");

            Label descLbl = new Label(item.getDescription());
            descLbl.setWrapText(true);
            descLbl.getStyleClass().add("m3-timeline-desc");

            contentCol.getChildren().addAll(timeLbl, titleLbl, descLbl);
            if (item.getCustomNode() != null) {
                contentCol.getChildren().add(item.getCustomNode());
            }

            HBox.setHgrow(contentCol, Priority.ALWAYS);
            row.getChildren().addAll(nodeCol, contentCol);
            getChildren().add(row);
        }
    }

    /** Builds the status indicator dot for one entry (completed check, active halo, pending ring). */
    private Node buildIndicator(Status status) {
        StackPane box = new StackPane();
        box.setMinSize(20, 20);
        box.setPrefSize(20, 20);
        box.setMaxSize(20, 20);

        switch (status) {
            case COMPLETED -> {
                Circle c = new Circle(9);
                c.getStyleClass().add("m3-timeline-circle-completed");
                M3Icon check = M3Icon.symbol("check");
                check.setScaleX(0.5);
                check.setScaleY(0.5);
                check.getStyleClass().add("m3-timeline-icon-completed");
                box.getChildren().addAll(c, check);
            }
            case ACTIVE -> {
                Circle halo = new Circle(10);
                halo.getStyleClass().add("m3-timeline-halo-active");
                Circle core = new Circle(4);
                core.getStyleClass().add("m3-timeline-core-active");
                box.getChildren().addAll(halo, core);
            }
            case PENDING -> {
                Circle c = new Circle(7);
                c.getStyleClass().add("m3-timeline-circle-pending");
                box.getChildren().add(c);
            }
        }
        return box;
    }
}

