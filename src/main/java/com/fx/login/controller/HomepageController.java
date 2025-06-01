package com.fx.login.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import net.rgielen.fxweaver.core.FxmlView;

@FxmlView("/ui/homepage.fxml")
public class HomepageController {
    @FXML
    private PieChart gioiTinh;

    @FXML
    private PieChart doTuoi;
    public void initialize() {
        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                        new PieChart.Data("Nam", 150),
                        new PieChart.Data("Nữ", 150)
                );
        gioiTinh.setData(pieChartData);

        ObservableList<PieChart.Data> pieChartData1 =
                FXCollections.observableArrayList(
                        new PieChart.Data("0-18", 100),
                        new PieChart.Data("18-60", 300),
                        new PieChart.Data(">60", 50)
                );
        doTuoi.setData(pieChartData1);
    }
}
