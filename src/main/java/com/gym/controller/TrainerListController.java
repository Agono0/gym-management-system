package com.gym.controller;

import io.m3fx.controls.button.M3Button;
import io.m3fx.controls.dialog.M3Dialog;
import io.m3fx.controls.menu.M3Menu;
import io.m3fx.controls.overlay.M3Overlay;
import io.m3fx.controls.search.M3SearchBar;
import io.m3fx.controls.chart.M3DataPoint;
import io.m3fx.controls.chart.M3DataSeries;
import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3SplitButton;
import io.m3fx.controls.card.M3Card;
import io.m3fx.controls.carousel.M3Carousel;
import io.m3fx.controls.chart.M3Heatmap;
import io.m3fx.controls.chart.M3RadarChart;
import io.m3fx.controls.chip.M3Chip;
import io.m3fx.controls.data.M3DataTable;
import io.m3fx.controls.data.M3StatCard;
import io.m3fx.controls.effect.M3GlassPane;
import io.m3fx.controls.iconbutton.M3IconButton;
import io.m3fx.controls.textfield.M3TextField;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Trainer List screen.
 * UI-contract only (no backend/service dependencies): owns the demo roster, the domain-chip
 * and search filtering, the sort menu, the onboarding dialog and the on-duty carousel.
 */
public class TrainerListController {

    @FXML private M3TopAppBar topAppBar;
    @FXML private M3StatCard statTotalCoaches;
    @FXML private M3StatCard statCapacitySaturation;
    @FXML private M3StatCard statTodaySlots;
    @FXML private M3StatCard statAverageRate;
    @FXML private M3IconButton btnCarouselPrev;
    @FXML private M3IconButton btnCarouselNext;
    @FXML private M3Carousel carouselCoaches;
    @FXML private M3GlassPane filterGlassPane;
    @FXML private M3TextField txtSearch;
    @FXML private M3Chip chipFilterAll;
    @FXML private M3Chip chipFilterStrength;
    @FXML private M3Chip chipFilterHypertrophy;
    @FXML private M3Chip chipFilterRehab;
    @FXML private M3Chip chipFilterCardio;
    @FXML private M3SplitButton btnSortOptions;
    @FXML private M3Button btnAddTrainer;
    @FXML private M3DataTable<?> tableTrainers;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colSpecialization;
    @FXML private TableColumn<?, ?> colExperience;
    @FXML private TableColumn<?, ?> colRate;
    @FXML private TableColumn<?, ?> colAssignedAthletes;
    @FXML private TableColumn<?, ?> colCapacityGauge;
    @FXML private TableColumn<?, ?> colActions;
    @FXML private M3Card cardSpecializationRadar;
    @FXML private M3RadarChart chartSpecialization;
    @FXML private M3Card cardFacilityDensity;
    @FXML private M3Heatmap heatmapBookingDensity;

    /** Hosts M3 snackbars and dialogs for this screen (attached to the scene root at runtime). */
    private final M3Overlay feedback = new M3Overlay();

    /** Demo roster rows in "id|name|specialization|exp|rate|athletes|saturation|email" shape. */
    private final List<String> demoTrainers = new ArrayList<>(List.of(
            "101|Elena Rostova|Strength|8|74.50|23|92%|e.rostova@cybercoregym.io",
            "102|Marcus Vance|Hypertrophy|11|82.00|19|79%|m.vance@cybercoregym.io",
            "103|Kim Park|Post-Rehab|6|68.00|14|56%|k.park@cybercoregym.io",
            "104|Ana Silva|Cardio & VO2|4|58.00|21|84%|a.silva@cybercoregym.io",
            "105|Leo Martins|Strength|9|76.00|17|68%|l.martins@cybercoregym.io",
            "106|Zoe Chen|Hypertrophy|5|62.50|16|64%|z.chen@cybercoregym.io",
            "107|Omar Haddad|Cardio & VO2|7|66.00|22|88%|o.haddad@cybercoregym.io",
            "108|Sofia Reyes|Post-Rehab|3|54.00|9|36%|s.reyes@cybercoregym.io",
            "109|Dana Wolfe|Strength|12|90.00|24|96%|d.wolfe@cybercoregym.io",
            "110|Nils Berg|Hypertrophy|2|52.00|11|44%|n.berg@cybercoregym.io",
            "111|Maya Lin|Post-Rehab|10|78.00|15|60%|m.lin@cybercoregym.io",
            "112|Tomas Rivera|Cardio & VO2|6|64.00|18|72%|t.rivera@cybercoregym.io",
            "113|Ava Kowalski|Strength|1|50.00|8|32%|a.kowalski@cybercoregym.io",
            "114|Jonas Weber|Hypertrophy|8|72.00|20|80%|j.weber@cybercoregym.io",
            "115|Priya Nair|Post-Rehab|5|60.00|12|48%|p.nair@cybercoregym.io",
            "116|Igor Petrov|Cardio & VO2|9|70.00|13|52%|i.petrov@cybercoregym.io"));

    public TrainerListController() {
        // Public no-arg constructor
    }

    @FXML
    public void initialize() {
        installFeedbackLayer(topAppBar);
        wireRosterColumns();
        keepDomainChipsExclusive();
        wireSearchAndChips();
        wireSortMenu();
        populateCarousel();
        populateCharts();
        applyFilters();
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable: its own {@code Column} model has no FXML-constructible constructor, so without
     * this wiring the roster renders with no header row. Extractors are pipe-index based so each
     * column shows its own field and header sorting works; the Actions column carries a blank
     * extractor (the table requires one once rows exist) and renders an Open button per row via
     * its cell factory instead.
     */
    @SuppressWarnings("unchecked")
    private void wireRosterColumns() {
        if (tableTrainers == null) {
            return;
        }
        M3DataTable<String> table = (M3DataTable<String>) tableTrainers;
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns = table.getColumns();
        columns.add(new M3DataTable.Column<>("ID", r -> field(r, 0), colId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colName.getText(), r -> field(r, 1), colName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colSpecialization.getText(), r -> field(r, 2), colSpecialization.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colExperience.getText(), r -> field(r, 3), colExperience.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colRate.getText(), r -> "$" + field(r, 4), colRate.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAssignedAthletes.getText(), r -> field(r, 5), colAssignedAthletes.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colCapacityGauge.getText(), r -> field(r, 6), colCapacityGauge.getPrefWidth()));
        columns.add(new M3DataTable.Column<String>(colActions.getText(), r -> "", colActions.getPrefWidth())
                .withCellFactory((row, s) -> {
                    M3Button open = M3Button.text("Open");
                    open.setOnAction(e -> onOpenProfile(Integer.parseInt(field(row, 0))));
                    return open;
                }));
        // The labeled txtSearch field above the table is this screen's search affordance; the
        // table's built-in unlabeled search bar would duplicate it, so it is not shown here.
        table.getChildren().removeIf(n -> n instanceof M3SearchBar);
        table.setOnRowAction(this::openProfileRow);
    }

    /** Puts the five domain chips in one ToggleGroup so exactly one filter is active at a time. */
    private void keepDomainChipsExclusive() {
        if (chipFilterAll == null) {
            return;
        }
        ToggleGroup domainGroup = new ToggleGroup();
        domainGroup.getToggles().addAll(
                chipFilterAll, chipFilterStrength, chipFilterHypertrophy, chipFilterRehab, chipFilterCardio);
    }

    /** Wires the search field and the domain chips to the visible roster (live filtering). */
    private void wireSearchAndChips() {
        if (txtSearch != null) {
            txtSearch.getInput().textProperty().addListener((obs, o, v) -> applyFilters());
        }
        if (chipFilterAll != null) {
            chipFilterAll.setOnAction(e -> applyFilters());
            chipFilterStrength.setOnAction(e -> applyFilters());
            chipFilterHypertrophy.setOnAction(e -> applyFilters());
            chipFilterRehab.setOnAction(e -> applyFilters());
            chipFilterCardio.setOnAction(e -> applyFilters());
        }
    }

    /** Attaches the sort menu to the split button: choosing an entry reorders the visible roster. */
    private void wireSortMenu() {
        if (btnSortOptions == null) {
            return;
        }
        M3Menu sortMenu = new M3Menu()
                .item("Sort: Saturation", () -> sortByField(6, true))
                .item("Sort: Experience", () -> sortByField(3, false))
                .item("Sort: Hourly Rate", () -> sortByField(4, false))
                .item("Sort: Name", () -> sortByField(1, true));
        btnSortOptions.setMenu(sortMenu);
    }

    /** Sorts the demo roster by the chosen pipe field and refreshes the visible rows. */
    private void sortByField(int fieldIndex, boolean ascending) {
        Comparator<String> byField = Comparator.comparing(r -> field(r, fieldIndex).toLowerCase(Locale.ROOT));
        if (fieldIndex == 3 || fieldIndex == 4 || fieldIndex == 6) {
            byField = Comparator.comparingDouble(r -> parseNumber(field(r, fieldIndex)));
        }
        demoTrainers.sort(ascending ? byField : byField.reversed());
        applyFilters();
    }

    /** Applies the active domain chip plus the search query to the visible roster. */
    @SuppressWarnings("unchecked")
    private void applyFilters() {
        if (tableTrainers == null) {
            return;
        }
        String query = txtSearch != null && txtSearch.getText() != null
                ? txtSearch.getText().trim().toLowerCase(Locale.ROOT)
                : "";
        String domain = null;
        if (chipFilterStrength != null && chipFilterStrength.isSelected()) {
            domain = "strength";
        } else if (chipFilterHypertrophy != null && chipFilterHypertrophy.isSelected()) {
            domain = "hypertrophy";
        } else if (chipFilterRehab != null && chipFilterRehab.isSelected()) {
            domain = "rehab";
        } else if (chipFilterCardio != null && chipFilterCardio.isSelected()) {
            domain = "cardio";
        }
        List<String> visible = new ArrayList<>();
        for (String row : demoTrainers) {
            boolean matchesDomain = domain == null || field(row, 2).toLowerCase(Locale.ROOT).contains(domain);
            boolean matchesQuery = query.isEmpty() || row.toLowerCase(Locale.ROOT).contains(query);
            if (matchesDomain && matchesQuery) {
                visible.add(row);
            }
        }
        ((M3DataTable<String>) tableTrainers).getItems().setAll(visible);
        if (chipFilterAll != null) {
            chipFilterAll.setText("All (" + demoTrainers.size() + ")");
        }
    }

    /** Fills the on-duty carousel with one summary card per featured coach. */
    private void populateCarousel() {
        if (carouselCoaches == null) {
            return;
        }
        String[][] featured = {
                {"ER", "Elena Rostova", "Olympic Weightlifting · on duty 07:00-15:00"},
                {"MV", "Marcus Vance", "Hypertrophy Blocks · on duty 09:00-17:00"},
                {"KP", "Kim Park", "Post-Rehab Return-to-Play · on duty 11:00-19:00"},
                {"AS", "Ana Silva", "VO2 Intervals · on duty 06:00-14:00"},
                {"LM", "Leo Martins", "Powerbuilding · on duty 13:00-21:00"},
                {"OH", "Omar Haddad", "Conditioning Circuits · on duty 08:00-16:00"}};
        for (String[] f : featured) {
            VBox body = new VBox(6);
            body.setAlignment(Pos.CENTER_LEFT);
            body.setPadding(new Insets(16));
            Label initials = new Label(f[0]);
            initials.getStyleClass().add("m3-stat-value");
            Label name = new Label(f[1]);
            name.getStyleClass().add("m3-section-title");
            Label detail = new Label(f[2]);
            detail.setWrapText(true);
            body.getChildren().addAll(initials, name, detail);
            M3Card card = new M3Card();
            card.setContent(body);
            card.setPrefSize(260, 188);
            carouselCoaches.getItems().add(card);
        }
    }

    /** Fills the radar (domain coverage) and the heatmap (weekly booking density). */
    private void populateCharts() {
        if (chartSpecialization != null) {
            chartSpecialization.getAxes().addAll("Strength", "Hypertrophy", "Rehab", "Cardio", "Mobility", "Nutrition");
            M3DataSeries coverage = new M3DataSeries("Team Coverage");
            coverage.getData().addAll(
                    new M3DataPoint("Strength", 92),
                    new M3DataPoint("Hypertrophy", 78),
                    new M3DataPoint("Rehab", 61),
                    new M3DataPoint("Cardio", 84),
                    new M3DataPoint("Mobility", 55),
                    new M3DataPoint("Nutrition", 47));
            chartSpecialization.getSeriesList().addAll(coverage);
        }
        if (heatmapBookingDensity != null) {
            heatmapBookingDensity.setDimensions(7, 16);
            heatmapBookingDensity.setRowLabels(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"));
            for (int day = 0; day < 7; day++) {
                for (int slot = 0; slot < 16; slot++) {
                    int hour = slot + 6;
                    double intensity = 0.15;
                    if ((hour >= 7 && hour <= 9) || (hour >= 17 && hour <= 20)) {
                        intensity = 0.75 + (day % 3 == 0 ? 0.20 : 0.10);
                    } else if (hour >= 12 && hour <= 14) {
                        intensity = 0.50;
                    } else if (day >= 5) {
                        intensity = 0.35;
                    }
                    heatmapBookingDensity.setValue(day, slot, Math.min(1.0, intensity));
                }
            }
        }
    }

    /** Returns the requested pipe field of a demo row, or an empty string when missing. */
    private static String field(String row, int index) {
        String[] parts = row.split("\\|");
        return index < parts.length ? parts[index] : "";
    }

    /** Parses a numeric demo field (rate, years, percent) tolerantly for sorting. */
    private static double parseNumber(String raw) {
        try {
            return Double.parseDouble(raw.replace("%", "").replace("$", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** Row/activation entry point shared by the row click and the per-row Open button. */
    private void openProfileRow(String row) {
        onOpenProfile(Integer.parseInt(field(row, 0)));
    }

    /**
     * Hosts the shared feedback overlay on the scene root while this screen is visible. Attach
     * and detach are deferred with {@link Platform#runLater(Runnable) Platform.runLater} so the
     * children list of the scene root (or of the pane hosting this screen) is never mutated
     * re-entrantly while the shell swaps screen content.
     */
    private void installFeedbackLayer(Node anchor) {
        anchor.sceneProperty().addListener((obs, oldScene, scene) -> {
            if (scene != null && scene.getRoot() instanceof Pane root
                    && !root.getChildren().contains(feedback)) {
                root.getChildren().add(feedback);
            }
            if (oldScene != null && oldScene.getRoot() instanceof Pane oldRoot) {
                // Deferred so a screen swap that is mid-mutation of the pane hosting the screen
                // never re-enters a JavaFX children list from inside this listener.
                Platform.runLater(() -> oldRoot.getChildren().remove(feedback));
            }
        });
    }

    @FXML
    public void onCarouselPrev() {
        if (carouselCoaches != null) {
            carouselCoaches.scrollPrev();
        }
    }

    @FXML
    public void onCarouselNext() {
        if (carouselCoaches != null) {
            carouselCoaches.scrollNext();
        }
    }

    /** Opens the onboarding dialog; confirming appends the coach to the visible roster. */
    @FXML
    public void onAdd() {
        M3TextField nameField = new M3TextField();
        nameField.setLabelText("Coach full name");
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.filled("Onboard");
        M3Dialog dialog = M3Dialog.basic(
                "Onboard a new coach",
                new Label("The coach joins the demo roster with a Starter capacity profile."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            String name = nameField.getText() == null ? "" : nameField.getText().trim();
            if (name.isEmpty()) {
                nameField.setError(true);
                nameField.setSupportingText("Enter the coach's full name to continue.");
                return;
            }
            dialog.hide();
            int nextId = 101 + demoTrainers.size();
            demoTrainers.add(nextId + "|" + name + "|Strength|0|50.00|0|0%|"
                    + name.toLowerCase(Locale.ROOT).replace(' ', '.') + "@cybercoregym.io");
            applyFilters();
            feedback.showSnackbar(name + " onboarded to the roster (demo)", "Open profile",
                    () -> onOpenProfile(nextId));
        });
        feedback.showDialog(dialog);
        nameField.getInput().requestFocus();
    }

    @FXML
    public void onAdd(ActionEvent event) {
        onAdd();
    }

    public void onOpenProfile(int trainerId) {
        feedback.showSnackbar("Opening coach profile #" + trainerId + " (demo)");
    }

    /** Destructive demo removal: confirms first, then removes with an Undo snackbar action. */
    public void onDelete(int trainerId) {
        String row = demoTrainers.stream()
                .filter(r -> field(r, 0).equals(String.valueOf(trainerId)))
                .findFirst().orElse(null);
        if (row == null) {
            return;
        }
        M3Button cancel = M3Button.text("Cancel");
        M3Button confirm = M3Button.text("Delete");
        M3Dialog dialog = M3Dialog.basic("Remove " + field(row, 1) + "?",
                new Label("The coach is removed from the demo roster. This cannot be undone."),
                cancel, confirm);
        dialog.setDefaultAction(confirm::activate);
        cancel.setOnAction(e -> dialog.hide());
        confirm.setOnAction(e -> {
            dialog.hide();
            int index = demoTrainers.indexOf(row);
            demoTrainers.remove(row);
            applyFilters();
            feedback.showSnackbar(field(row, 1) + " removed from the roster (demo)", "Undo",
                    () -> {
                        demoTrainers.add(Math.min(index, demoTrainers.size()), row);
                        applyFilters();
                    });
        });
        feedback.showDialog(dialog);
    }
}
