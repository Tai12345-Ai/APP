package com.fx.login.model;


import javafx.beans.property.SimpleBooleanProperty;

public class SelectableResident extends Resident {
    private final SimpleBooleanProperty selected = new SimpleBooleanProperty(false);

    public SelectableResident(Resident resident) {
        // Copy properties từ Resident
        super(resident.getId(), resident.getFullName(), resident.getEmail(),
                resident.getPhone(), resident.getApartmentNumber());
        syncProperties();
    }

    public boolean isSelected() {
        return selected.get();
    }

    public void setSelected(boolean selected) {
        this.selected.set(selected);
    }

    public SimpleBooleanProperty selectedProperty() {
        return selected;
    }
}