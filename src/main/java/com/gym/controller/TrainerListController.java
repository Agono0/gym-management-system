package com.gym.controller;

import io.m3fx.controls.appbar.M3TopAppBar;
import io.m3fx.controls.button.M3Button;
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
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.ToggleGroup;

/**
 * Controller skeleton for Trainer List screen.
 * Strictly UI contract stubs matching Section 7 without backend/service dependencies.
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

    public TrainerListController() {
        // Public no-arg constructor
    }

    @FXML
    public void initialize() {
        wireRosterColumns();
        keepDomainChipsExclusive();
    }

    /**
     * Maps the FXML-declared TableColumn definitions (header text + preferred width) onto the
     * M3DataTable: its own {@code Column} model has no FXML-constructible constructor, so without
     * this wiring the roster renders with no header row. Extractors enable header sorting; the
     * Actions column passes no extractor, matching its {@code sortable="false"}.
     */
    @SuppressWarnings("unchecked")
    private void wireRosterColumns() {
        if (tableTrainers == null) {
            return;
        }
        javafx.collections.ObservableList<M3DataTable.Column<String>> columns =
                ((M3DataTable<String>) tableTrainers).getColumns();
        columns.add(new M3DataTable.Column<>(colId.getText(), String::toString, colId.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colName.getText(), String::toString, colName.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colSpecialization.getText(), String::toString, colSpecialization.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colExperience.getText(), String::toString, colExperience.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colRate.getText(), String::toString, colRate.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colAssignedAthletes.getText(), String::toString, colAssignedAthletes.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colCapacityGauge.getText(), String::toString, colCapacityGauge.getPrefWidth()));
        columns.add(new M3DataTable.Column<>(colActions.getText(), null, colActions.getPrefWidth()));
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

    @FXML
    public void onAdd() {
        // UI navigation stub
    }

    @FXML
    public void onAdd(ActionEvent event) {
        // UI navigation stub overload
    }

    public void onOpenProfile(int trainerId) {
        // UI navigation stub
    }

    public void onDelete(int trainerId) {
        // UI action stub
    }
}
