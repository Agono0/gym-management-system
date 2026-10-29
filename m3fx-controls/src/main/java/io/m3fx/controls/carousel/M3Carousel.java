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
        StackPane.setMargin(prevButton, new javafx.geometry.Insets(0, 0, 0, 8));
        StackPane.setMargin(nextButton, new javafx.geometry.Insets(0, 8, 0, 0));

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
        setPrefHeight(220);
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
            double dx = e.getX() - dragStartX;
            track.setTranslateX(trackStartTranslateX + dx);
        });

        viewport.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            double currentX = track.getTranslateX();
            double itemWidth = getItemSnapWidth();
            double stride = itemWidth + track.getSpacing();
            if (stride <= 0 || items.isEmpty()) {
                return; // nothing measurable to snap to
            }
            int targetIndex = (int) Math.round(-currentX / stride);
            targetIndex = Math.max(0, Math.min(items.size() - 1, targetIndex));
            setActiveIndex(targetIndex);
            snapToIndex(targetIndex);
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

    private double getItemSnapWidth() {
        if (items.isEmpty()) {
            return 200.0;
        }
        Node first = items.get(0);
        if (first instanceof Region region) {
            return region.getWidth() > 0 ? region.getWidth() : region.getPrefWidth();
        }
        return 200.0;
    }

    private void snapToIndex(int index) {
        double stride = getItemSnapWidth() + track.getSpacing();
        double targetX = stride > 0 ? -index * stride : 0;
        SpringAnimation.animate(track.translateXProperty(), targetX, SpringAnimation.DEFAULT, null);
        updateNavState();
    }

    private void updateNavState() {
        prevButton.setDisable(getActiveIndex() <= 0);
        nextButton.setDisable(getActiveIndex() >= items.size() - 1);
    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        viewport.resizeRelocate(0, 0, w, h);
        for (Node child : getChildren()) {
            child.resizeRelocate(0, 0, w, h);
        }
    }
}

