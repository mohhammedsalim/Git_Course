package com.zad.view;

import com.zad.dao.InvoiceDAO;
import com.zad.dao.ProductDAO;
import com.zad.model.Product;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/** لوحة التحكم (الرئيسية) */
public class DashboardView extends VBox {

    private final Label lblTodaySales = new Label("0");
    private final Label lblTodayCount = new Label("0");
    private final Label lblInvValue = new Label("0");
    private final Label lblLowStock = new Label("0");
    private final GridPane topGrid = new GridPane();
    private final VBox lowList = new VBox(6);

    public DashboardView() {
        setSpacing(20);
        setPadding(new Insets(25));
        getStyleClass().add("content-pane");

        Label title = new Label("لوحة التحكم — نظرة عامة على المحل");
        title.getStyleClass().add("page-title");

        HBox cards = new HBox(18,
                card("#2e7d32", "مبيعات اليوم", lblTodaySales),
                card("#1565c0", "عدد الفواتير اليوم", lblTodayCount),
                card("#ef6c00", "قيمة المخزون", lblInvValue),
                card("#c62828", "أصناف قاربت على النفاد", lblLowStock));

        HBox bottom = new HBox(18);
        bottom.setAlignment(Pos.TOP_CENTER);

        VBox top10Box = new VBox(10);
        top10Box.getStyleClass().add("card");
        top10Box.setPadding(new Insets(15));
        top10Box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(top10Box, javafx.scene.layout.Priority.ALWAYS);
        Label t10 = new Label("الأكثر مبيعاً (أفضل 5 أصناف)");
        t10.getStyleClass().add("card-title");
        topGrid.setHgap(20);
        topGrid.setVgap(6);
        top10Box.getChildren().addAll(t10, topGrid);

        VBox lowBox = new VBox(10);
        lowBox.getStyleClass().add("card");
        lowBox.setPadding(new Insets(15));
        HBox.setHgrow(lowBox, javafx.scene.layout.Priority.ALWAYS);
        Label lt = new Label("⚠ تنبيهات المخزون المنخفض");
        lt.getStyleClass().add("card-title");
        lowBox.getChildren().addAll(lt, lowList);

        bottom.getChildren().addAll(top10Box, lowBox);
        getChildren().addAll(title, cards, bottom);
        refresh();
    }

    private VBox card(String color, String labelName, Label value) {
        Circle c = new Circle(8);
        c.setStyle("-fx-fill:" + color + ";");
        Label l = new Label(labelName);
        l.setStyle("-fx-text-fill:#666; -fx-font-size:13px;");
        value.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:" + color + ";");
        VBox box = new VBox(8, new HBox(8, c, l), value);
        box.getStyleClass().add("card");
        box.setPadding(new Insets(18));
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);
        return box;
    }

    public void refresh() {
        try {
            InvoiceDAO idao = new InvoiceDAO();
            lblTodaySales.setText(fmt(idao.todaySales()));
            lblTodayCount.setText(String.valueOf(idao.countInvoicesToday()));
            lblInvValue.setText(fmt(idao.inventoryValue()));
            java.util.List<Product> low = idao.lowStockProducts();
            lblLowStock.setText(String.valueOf(low.size()));

            lowList.getChildren().clear();
            if (low.isEmpty()) lowList.getChildren().add(new Label("لا توجد أصناف ناقصة ✓"));
            for (Product p : low.stream().limit(8).toList()) {
                Label item = new Label("• " + p.getName() + " — متبقي: " + trim(p.getQuantity()) + " " + p.getUnit());
                item.setStyle("-fx-text-fill:#c62828;");
                lowList.getChildren().add(item);
            }

            topGrid.getChildren().clear();
            var top = idao.topProducts(5);
            int r = 0;
            topGrid.add(header("الصنف"), 0, r);
            topGrid.add(header("الكمية"), 1, r);
            topGrid.add(header("الإيراد"), 2, r);
            for (String[] row : top) {
                r++;
                topGrid.add(new Label(row[0]), 0, r);
                topGrid.add(new Label(trim(Double.parseDouble(row[1]))), 1, r);
                topGrid.add(new Label(fmt(Double.parseDouble(row[2]))), 2, r);
            }
            if (top.isEmpty()) topGrid.add(new Label("لا توجد بيانات بيع بعد"), 0, 1);
        } catch (Exception e) {
            lblTodaySales.setText("خطأ: " + e.getMessage());
        }
    }

    private Label header(String s) {
        Label l = new Label(s);
        l.setStyle("-fx-font-weight:bold; -fx-text-fill:#444;");
        return l;
    }

    private String fmt(double d) {
        return String.format("%,.2f ر.س", d);
    }

    private String trim(double d) {
        if (d == Math.floor(d)) return String.valueOf((long) d);
        return String.format("%.2f", d);
    }
}
