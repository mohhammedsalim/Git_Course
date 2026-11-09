package com.zad;

import com.zad.db.Database;
import com.zad.util.UI;
import com.zad.view.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.ResultSet;

/**
 * التطبيق الرئيسي — زاد لإدارة المحلات الغذائية
 * JavaFX + SQLite
 */
public class ZadApp extends Application {

    private BorderPane root;
    private VBox sidebar;
    private DashboardView dashboard;
    private ProductsView productsView;
    private PosView posView;
    private InventoryView inventoryView;
    private PeopleView peopleView;
    private ReportsView reportsView;
    private SettingsView settingsView;
    private String currentKey = "dashboard";

    @Override
    public void start(Stage stage) {
        // تهيئة قاعدة البيانات مبكراً لعرض أي خطأ اتصال
        try {
            Database.getConnection();
        } catch (Exception e) {
            System.err.println("DB error: " + e.getMessage());
        }

        root = new BorderPane();
        root.setStyle("-fx-direction: rtl;");

        dashboard = new DashboardView();
        productsView = new ProductsView();
        posView = new PosView();
        inventoryView = new InventoryView();
        peopleView = new PeopleView();
        reportsView = new ReportsView();
        settingsView = new SettingsView();

        root.setRight(buildSidebar());
        show("dashboard");

        Scene scene = new Scene(root, 1200, 720);
        var css = getClass().getResource("/styles.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        stage.setTitle("زاد — نظام إدارة المحلات الغذائية");
        stage.setMinWidth(1000);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildSidebar() {
        Label logo = new Label("🛒 زاد");
        logo.setStyle("-fx-font-size:30px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label sub = new Label("نظام إدارة المواد الغذائية");
        sub.setStyle("-fx-font-size:12px; -fx-text-fill:#c8e6c9;");

        Label stats = new Label("");
        stats.setStyle("-fx-font-size:12px; -fx-text-fill:#a5d6a7;");
        try {
            ResultSet rs = Database.getConnection().createStatement()
                    .executeQuery("SELECT COUNT(*) FROM products");
            rs.next();
            stats.setText(rs.getInt(1) + " منتج مسجل في النظام");
        } catch (Exception ignored) {}

        sidebar = new VBox(0);
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");
        sidebar.getChildren().addAll(
                new VBox(6, logo, sub, stats),
                navItem("الرئيسية", "🏠", "dashboard"),
                navItem("نقطة البيع", "💰", "pos"),
                navItem("المنتجات والمخزون", "📦", "products"),
                navItem("المشتريات والتوريد", "🚚", "inventory"),
                navItem("العملاء والموردون", "👥", "people"),
                navItem("الفواتير والتقارير", "📊", "reports"),
                navItem("الإعدادات", "⚙", "settings")
        );
        VBox header = (VBox) sidebar.getChildren().get(0);
        header.setPadding(new Insets(25, 15, 20, 15));
        header.setAlignment(Pos.TOP_RIGHT);
        return sidebar;
    }

    private javafx.scene.Node navItem(String text, String icon, String key) {
        Label l = new Label(icon + "  " + text);
        l.getStyleClass().addAll("nav-item");
        l.setPadding(new Insets(14, 20, 14, 20));
        l.setOnMouseClicked(e -> show(key));
        l.setId("nav-" + key);
        return l;
    }

    private void show(String key) {
        currentKey = key;
        for (var n : sidebar.getChildren()) {
            if (n instanceof Label l) l.getStyleClass().remove("nav-active");
        }
        var active = sidebar.lookup("#nav-" + key);
        if (active != null) active.getStyleClass().add("nav-active");

        switch (key) {
            case "dashboard" -> { dashboard.refresh(); root.setCenter(dashboard); }
            case "pos" -> { posView.refreshProducts(); root.setCenter(posView); }
            case "products" -> { productsView.refresh(); root.setCenter(productsView); }
            case "inventory" -> { inventoryView.refreshData(); root.setCenter(inventoryView); }
            case "people" -> root.setCenter(peopleView);
            case "reports" -> root.setCenter(reportsView);
            case "settings" -> root.setCenter(settingsView);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
