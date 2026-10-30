package io.m3fx.controls.carousel;

import io.m3fx.controls.icon.M3Icon;
import io.m3fx.controls.iconbutton.IconButtonVariant;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.internal.SpringAnimation;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * M3 Carousel component displaying scrollable cards with dynamic sizing, rounded corners,
 * and spring-driven snapping physics.
 *
 * <p>See <a href="https://m3.material.io/components/carousel/overview">M3 Carousels</a>.
 * Supports Multi-browse and Hero configurations with interactive drag and button scrolling.
 */
public class M3Carousel extends Region {

    /** Carousel layout configurations. */
    public enum CarouselLayout {
        MULTI_BROWSE,
        HERO
    }

    private final ObjectProperty<CarouselLayout> layoutMode =
            new SimpleObjectProperty<>(this, "layoutMode", CarouselLayout.MULTI_BROWSE);
    private final IntegerProperty activeIndex =
            new SimpleIntegerProperty(this, "activeIndex", 0);

    private final ObservableList<Node> items = FXCollections.observableArrayList();
    private final HBox track = new HBox(12);
    private final Pane viewport = new Pane(track);
    private final Rectangle clipRect = new Rectangle();

    private final M3IconButton prevButton =
            new M3IconButton(M3Icon.symbol("arrow_back"), IconButtonVariant.FILLED);
    private final M3IconButton nextButton =
            new M3IconButton(M3Icon.symbol("arrow_forward"), IconButtonVariant.FILLED);

    private double dragStartX;
    private double trackStartTranslateX;

    /** Creates an empty carousel with Multi-browse layout. */
    public M3Carousel() {
        this(CarouselLayout.MULTI_BROWSE);
    }

    /**
     * Creates an empty carousel with a specific layout.
     *
     * @param layout layout configuration
     */
    public M3Carousel(CarouselLayout layout) {
        getStyleClass().add("m3-carousel");
        setLayoutMode(layout);

        track.setAlignment(Pos.CENTER_LEFT);
        track.getStyleClass().add("m3-carousel-track");

        viewport.setClip(clipRect);
        clipRect.widthProperty().bind(viewport.widthProperty());
        clipRect.heightProperty().bind(viewport.heightProperty());

        prevButton.setAccessibleText("Previous carousel item");
        nextButton.setAccessibleText("Next carousel item");
        prevButton.getStyleClass().add("m3-carousel-nav-prev");
        nextButton.getStyleClass().add("m3-carousel-nav-next");

        StackPane.setAlignment(prevButton, Pos.CENTER_LEFT);
        StackPane.setAlignment(nextButton, Pos.CENTER_RIGHT);
        StackPane.setMargin(prevButton, new javafx.geometry.Insets(0, 0, 0, 4));
        StackPane.setMargin(nextButton, new javafx.geometry.Insets(0, 4, 0, 0));

        StackPane overlayContainer = new StackPane(viewport, prevButton, nextButton);
        overlayContainer.setPickOnBounds(false);
        getChildren().add(overlayContainer);

        items.addListener((ListChangeListener<Node>) c -> {
            track.getChildren().setAll(items);
            updateNavState();
        });

        prevButton.setOnAction(e -> scrollPrev());
        nextButton.setOnAction(e -> scrollNext());

        initDragGestures();

        setMinHeight(180);
        setPrefHeight(260);
        setPrefWidth(600);

        activeIndexProperty().addListener((obs, oldV, newV) -> snapToIndex(newV.intValue()));
        widthProperty().addListener((obs, o, n) -> snapToIndex(getActiveIndex()));
        updateNavState();
    }

    private void initDragGestures() {
        viewport.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            dragStartX = e.getX();
            trackStartTranslateX = track.getTranslateX();
            SpringAnimation.stop(track.translateXProperty());
        });

        viewport.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double maxScroll = getMaxScroll();
            if (maxScroll <= 0) return;
            double dx = e.getX() - dragStartX;
            double candidate = trackStartTranslateX + dx;
            if (candidate > 0) {
                candidate = candidate * 0.3;
            } else if (candidate < -maxScroll) {
                double over = candidate + maxScroll;
                candidate = -maxScroll + over * 0.3;
            }
            track.setTranslateX(candidate);
        });

        viewport.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            double maxScroll = getMaxScroll();
            if (maxScroll <= 0) {
                snapToIndex(0);
                return;
            }
            double currentX = track.getTranslateX();
            int bestIndex = 0;
            double bestDist = Double.MAX_VALUE;
            for (int i = 0; i < items.size(); i++) {
                double offset = -getItemOffset(i);
                offset = Math.max(-maxScroll, Math.min(0, offset));
                double dist = Math.abs(currentX - offset);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIndex = i;
                }
            }
            setActiveIndex(bestIndex);
            snapToIndex(bestIndex);
        });
    }

    /** Returns the observable list of carousel items. */
    public ObservableList<Node> getItems() {
        return items;
    }

    public ObjectProperty<CarouselLayout> layoutModeProperty() {
        return layoutMode;
    }

    public CarouselLayout getLayoutMode() {
        return layoutMode.get();
    }

    public void setLayoutMode(CarouselLayout layout) {
        this.layoutMode.set(layout);
        getStyleClass().removeIf(s -> s.startsWith("m3-carousel-"));
        getStyleClass().add("m3-carousel-" + layout.name().toLowerCase());
    }

    public IntegerProperty activeIndexProperty() {
        return activeIndex;
    }

    public int getActiveIndex() {
        return activeIndex.get();
    }

    public void setActiveIndex(int index) {
        this.activeIndex.set(index);
    }

    /** Scrolls to the previous item. */
    public void scrollPrev() {
        if (getActiveIndex() > 0) {
            setActiveIndex(getActiveIndex() - 1);
        }
    }

    /** Scrolls to the next item. */
    public void scrollNext() {
        if (getActiveIndex() < items.size() - 1) {
            setActiveIndex(getActiveIndex() + 1);
        }
    }

    private double getItemOffset(int index) {
        double offset = 0;
        double spacing = track.getSpacing();
        for (int i = 0; i < index && i < items.size(); i++) {
            Node node = items.get(i);
            double w = node instanceof Region r ? (r.getWidth() > 0 ? r.getWidth() : r.getPrefWidth()) : 200.0;
            offset += w + spacing;
        }
        return offset;
    }

    private double getTotalTrackWidth() {
        double total = 0;
        double spacing = track.getSpacing();
        for (int i = 0; i < items.size(); i++) {
            Node node = items.get(i);
            double w = node instanceof Region r ? (r.getWidth() > 0 ? r.getWidth() : r.getPrefWidth()) : 200.0;
            total += w;
            if (i > 0) total += spacing;
        }
        javafx.geometry.Insets insets = track.getInsets();
        if (insets != null) {
            total += insets.getLeft() + insets.getRight();
        }
        return total;
    }

    private double getMaxScroll() {
        double viewportW = viewport.getWidth() > 0 ? viewport.getWidth() : (getWidth() > 0 ? getWidth() : getPrefWidth());
        double totalW = getTotalTrackWidth();
        return Math.max(0, totalW - viewportW);
    }

    private void snapToIndex(int index) {
        double maxScroll = getMaxScroll();
        if (maxScroll <= 1.0) {
            SpringAnimation.animate(track.translateXProperty(), 0.0, SpringAnimation.DEFAULT, this::updateNavState);
            updateNavState();
            return;
        }
        double targetOffset = getItemOffset(index);
        double targetX = -Math.min(targetOffset, maxScroll);
        SpringAnimation.animate(track.translateXProperty(), targetX, SpringAnimation.DEFAULT, this::updateNavState);
        updateNavState();
    }

    private void updateNavState() {
        double maxScroll = getMaxScroll();
        if (maxScroll <= 1.0) {
            prevButton.setVisible(false);
            nextButton.setVisible(false);
            prevButton.setDisable(true);
            nextButton.setDisable(true);
            track.setAlignment(Pos.CENTER);
            track.setPadding(new javafx.geometry.Insets(8, 8, 8, 8));
        } else {
            track.setAlignment(Pos.CENTER_LEFT);
            track.setPadding(new javafx.geometry.Insets(8, 36, 8, 36));
            double currentX = track.getTranslateX();
            boolean atStart = currentX >= -2.0;
            boolean atEnd = currentX <= -maxScroll + 2.0;

            prevButton.setVisible(true);
            nextButton.setVisible(true);
            prevButton.setDisable(atStart);
            nextButton.setDisable(atEnd);
            prevButton.setOpacity(atStart ? 0.35 : 1.0);
            nextButton.setOpacity(atEnd ? 0.35 : 1.0);
        }
    }

    @Override
    protected double computePrefWidth(double height) {
        return Math.max(600, getTotalTrackWidth());
    }

    @Override
    protected double computeMinWidth(double height) {
        return 320;
    }

    @Override
    protected double computePrefHeight(double width) {
        double maxH = 260;
        for (Node node : items) {
            if (node instanceof Region r) {
                maxH = Math.max(maxH, r.prefHeight(-1));
            }
        }
        return maxH;
    }

    @Override
    protected double computeMinHeight(double width) {
        return 180;
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        viewport.resizeRelocate(0, 0, w, h);

        double totalTrackW = getTotalTrackWidth();
        double trackW = Math.max(w, totalTrackW);
        track.resize(trackW, h);
        track.setMinHeight(h);
        track.setPrefHeight(h);
        track.setMaxHeight(h);

        for (Node child : getChildren()) {
            if (child != viewport) {
                child.resizeRelocate(0, 0, w, h);
            }
        }
        updateNavState();
    }
}

