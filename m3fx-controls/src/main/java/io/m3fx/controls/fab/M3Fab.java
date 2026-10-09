package io.m3fx.controls.fab;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Button;

/**
 * M3 floating action button: small/medium/large sizes plus an extended (icon + label) mode.
 *
 * <p>See <a href="https://m3.material.io/components/floating-action-button/overview">M3 FAB</a>.
 * Primary-container color, large shape, elevation level 3. Extended FABs show a text label next to
 * the icon.
 */
public class M3Fab extends Button {

    private final ObjectProperty<FabSize> size =
            new SimpleObjectProperty<>(this, "size", FabSize.MEDIUM);
    private final BooleanProperty extended = new SimpleBooleanProperty(this, "extended", false);

    /** Creates a medium FAB. */
    public M3Fab() {
        this(null, FabSize.MEDIUM);
    }

    /**
     * Creates a FAB with a graphic and size.
     *
     * @param graphic the icon node
     * @param size the FAB size
     */
    public M3Fab(Node graphic, FabSize size) {
        super("", graphic);
        getStyleClass().add("m3-fab");
        setSize(size);
        sizeProperty().addListener((obs, oldV, newV) -> applySize(oldV, newV));
        extendedProperty().addListener((obs, oldV, isExtended) -> {
            if (isExtended) {
                if (!getStyleClass().contains("m3-fab-extended")) {
                    getStyleClass().add("m3-fab-extended");
                }
            } else {
                getStyleClass().remove("m3-fab-extended");
            }
            applySize(getSize(), getSize());
        });
        setFocusTraversable(true);
        io.m3fx.controls.internal.MotionUtil.addPressScale(this);
    }

    /**
     * Creates an extended FAB with icon and label.
     *
     * @param text label text
     * @param graphic icon node
     * @return the FAB
     */
    public static M3Fab extended(String text, Node graphic) {
        M3Fab fab = new M3Fab(graphic, FabSize.MEDIUM);
        fab.setText(text == null ? "" : text);
        fab.setExtended(true);
        return fab;
    }

    /** Returns the size property. */
    public ObjectProperty<FabSize> sizeProperty() {
        return size;
    }

    /** Returns the size. */
    public FabSize getSize() {
        return size.get();
    }

    /**
     * Sets the size.
     *
     * @param size the new size
     */
    public void setSize(FabSize size) {
        FabSize old = this.size.get();
        this.size.set(size == null ? FabSize.MEDIUM : size);
        applySize(old, this.size.get());
    }

    /** Returns the extended property. */
    public BooleanProperty extendedProperty() {
        return extended;
    }

    /** Returns whether this is an extended FAB. */
    public boolean isExtended() {
        return extended.get();
    }

    /**
     * Sets extended mode (icon + label).
     *
     * @param extended {@code true} for extended
     */
    public void setExtended(boolean extended) {
        this.extended.set(extended);
    }

    private void applySize(FabSize oldV, FabSize newV) {
        if (oldV != null) {
            getStyleClass().remove("m3-fab-" + oldV.name().toLowerCase());
        }
        if (newV != null && !getStyleClass().contains("m3-fab-" + newV.name().toLowerCase())) {
            getStyleClass().add("m3-fab-" + newV.name().toLowerCase());
        }
        if (isExtended()) {
            setMinSize(80, 56);
            setPrefSize(USE_COMPUTED_SIZE, 56);
        } else if (newV == FabSize.SMALL) {
            setMinSize(40, 40);
            setPrefSize(40, 40);
        } else if (newV == FabSize.LARGE) {
            setMinSize(96, 96);
            setPrefSize(96, 96);
        } else {
            setMinSize(56, 56);
            setPrefSize(56, 56);
        }
    }
}
