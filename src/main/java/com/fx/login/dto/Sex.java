package com.fx.login.dto;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

public class Sex {
    static ObservableList<String> obsList;

    // Returns an ObservableList populated by Countries
    public static ObservableList<String> obsList() {
        ArrayList<String> items = new ArrayList<>();

        items.add("Nam");
        items.add("Nữ");
        items.add("Khác");
        return obsList = FXCollections.observableArrayList(items);

    }
}
