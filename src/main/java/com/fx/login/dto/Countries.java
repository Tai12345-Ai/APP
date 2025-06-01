package com.fx.login.dto;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;

 public class Countries {

    static ObservableList<String> obsList;

    // Returns an ObservableList populated by Countries
    public static ObservableList<String> obsList() {
        ArrayList<String> items = new ArrayList<>();

        items.add("Việt Nam");
        items.add("Thanh Hóa");
        items.add("Khác");
        return obsList = FXCollections.observableArrayList(items);

    }

}

