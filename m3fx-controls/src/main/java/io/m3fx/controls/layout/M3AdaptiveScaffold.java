package io.m3fx.controls.layout;

import io.m3fx.controls.nav.M3NavDestination;
import io.m3fx.controls.nav.M3NavigationBar;
import io.m3fx.controls.nav.M3NavigationDrawer;
import io.m3fx.controls.nav.M3NavigationRail;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import java.util.ArrayList;
import java.util.List;

/**
 * M3 adaptive scaffold: swaps navigation automatically with the window size class.
 *
 * <p>Compact width shows a bottom {@code M3NavigationBar}, medium shows a
 * {@code M3NavigationRail}, expanded shows a standard {@code M3NavigationDrawer}. Selection is
 * kept in sync across swaps. Content and an optional top bar are caller-supplied.
 */
public class M3AdaptiveScaffold extends BorderPane {

    private final ObjectProperty<WindowSizeClass> sizeClass =
            new SimpleObjectProperty<>(this, "sizeClass", WindowSizeClass.EXPANDED);
    private final IntegerProperty selectedIndex = new SimpleIntegerProperty(this, "selectedIndex", 0);

    private List<M3NavDestination> destinations = new ArrayList<>();
    private Node content;
    private Node topBar;
    private M3NavigationBar bar;
    private M3NavigationRail rail;
    private M3NavigationDrawer drawer;

    /** Creates an empty scaffold. */
    public M3AdaptiveScaffold() {
        getStyleClass().add("m3-adaptive-scaffold");
        widthProperty().addListener((obs, oldV, newV) -> {
            WindowSizeClass next = WindowSizeClass.forWidth(newV.doubleValue());
            if (next != getSizeClass()) {
                setSizeClass(next);
            }
        });
        sizeClassProperty().addListener((obs, oldV, newV) -> rebuild());
        selectedIndexProperty().addListener((obs, oldV, newV) -> syncSelection(newV.intValue()));
    }

    /**
     * Sets the navigation destinations.
     *
     * @param destinations destination list
     */
    public void setDestinations(List<M3NavDestination> destinations) {
        this.destinations = destinations == null ? new ArrayList<>() : new ArrayList<>(destinations);
        rebuild();
    }

    /** Returns the destinations. */
    public List<M3NavDestination> getDestinations() {
        return List.copyOf(destinations);
    }

    /**
     * Sets the main content.
     *
     * @param content content node
     */
    public void setContent(Node content) {
        this.content = content;
        setCenter(content);
    }

    /**
     * Sets the top app bar.
     *
     * @param topBar top bar node, may be {@code null}
     */
    public void setTopBar(Node topBar) {
        this.topBar = topBar;
        setTop(topBar);
    }

    /** Returns the size class property. */
    public ObjectProperty<WindowSizeClass> sizeClassProperty() {
        return sizeClass;
    }

    /** Returns the current size class. */
    public WindowSizeClass getSizeClass() {
        return sizeClass.get();
    }

    private void setSizeClass(WindowSizeClass sizeClass) {
        this.sizeClass.set(sizeClass);
    }

    /** Returns the selected index property. */
    public IntegerProperty selectedIndexProperty() {
        return selectedIndex;
    }

    /** Returns the selected index. */
    public int getSelectedIndex() {
        return selectedIndex.get();
    }

    /**
     * Sets the selected index.
     *
     * @param selectedIndex the new index
     */
    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex.set(selectedIndex);
    }

    private void rebuild() {
        unbindNav();
        setLeft(null);
        setBottom(null);
        setCenter(content);
        setTop(topBar);
        bar = null;
        rail = null;
        drawer = null;
        if (destinations.isEmpty()) {
            return;
        }
        switch (getSizeClass()) {
            case COMPACT -> {
                bar = new M3NavigationBar(destinations);
                bar.setSelectedIndex(Math.min(getSelectedIndex(), destinations.size() - 1));
                bar.selectedIndexProperty().bindBidirectional(selectedIndexProperty());
                setBottom(bar);
            }
            case MEDIUM -> {
                rail = new M3NavigationRail(destinations);
                rail.setSelectedIndex(Math.min(getSelectedIndex(), destinations.size() - 1));
                rail.selectedIndexProperty().bindBidirectional(selectedIndexProperty());
                setLeft(rail);
            }
            case EXPANDED -> {
                drawer = new M3NavigationDrawer(destinations, false);
                drawer.setSelectedIndex(Math.min(getSelectedIndex(), destinations.size() - 1));
                drawer.selectedIndexProperty().bindBidirectional(selectedIndexProperty());
                setLeft(drawer);
            }
        }
    }

    private void unbindNav() {
        if (bar != null) {
            bar.selectedIndexProperty().unbindBidirectional(selectedIndexProperty());
        }
        if (rail != null) {
            rail.selectedIndexProperty().unbindBidirectional(selectedIndexProperty());
        }
        if (drawer != null) {
            drawer.selectedIndexProperty().unbindBidirectional(selectedIndexProperty());
        }
    }

    private void syncSelection(int index) {
        // Selection propagates through the bidirectional bindings created in rebuild().
    }
}
