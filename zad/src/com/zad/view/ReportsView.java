package com.zad.view;

import com.zad.dao.InvoiceDAO;
import com.zad.util.UI;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** شاشة الفواتير والتقارير */
public class ReportsView extends BorderPane {

    private final TableView<Map<String, Object>> table = new TableView<>();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final DatePicker from = new DatePicker(LocalDate.now().withDayOfMonth(1));
    private final DatePicker to = new DatePicker(LocalDate.now());
    private final Label lblSum = new Label("0");
    private final InvoiceDAO dao = new InvoiceDAO();

    public ReportsView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(20));

        Label title = new Label("الفواتير والتقارير");
        title.getStyleClass().add("page-title");

        RadioButton rbSales = new RadioButton("فواتير البيع");
        RadioButton rbPurch = new RadioButton("فواتير الشراء");
        rbSales.setToggleGroup(typeGroup);
        rbPurch.setToggleGroup(typeGroup);
        rbSales.setSelected(true);
        rbSales.setOnAction(e -> load());
        rbPurch.setOnAction(e -> load());

        Button btnView = new Button("عرض تفاصيل الفاتورة المحددة 👁");
        btnView.setOnAction(e -> showDetails());
        Button btnRange = new Button("تقرير مبيعات للفترة");
        btnRange.setStyle("-fx-background-color:#1565c0; -fx-text-fill:white; -fx-font-weight:bold;");
        btnRange.setOnAction(e -> periodReport());

        HBox tools = new HBox(12, rbSales, rbPurch,
                new Label("من:"), from, new Label("إلى:"), to, btnRange, btnView);
        tools.setPadding(new Insets(10, 0, 10, 0));

        setupTable();

        lblSum.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#2e7d32;");
        HBox footer = new HBox(10, new Label("إجمالي الفواتير المعروضة:"), lblSum);
        footer.setPadding(new Insets(10, 0, 0, 0));

        VBox center = new VBox(5, table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);

        setTop(new VBox(8, title, tools));
        setCenter(center);
        load();
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Map<String, Object>, Number> id = new TableColumn("رقم");
        id.setCellValueFactory(v -> new ReadOnlyObjectWrapper<Number>((Number) v.getValue().get("id")));
        id.setMaxWidth(70);
        TableColumn<Map<String, Object>, String> no = new TableColumn("المرجع");
        no.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(str(v.getValue().get("invoice_no"))));
        TableColumn<Map<String, Object>, String> date = new TableColumn("التاريخ");
        date.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(str(v.getValue().get("date"))));
        TableColumn<Map<String, Object>, String> cust = new TableColumn("العميل/المورد");
        cust.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(str(v.getValue().get("customer"))));
        TableColumn<Map<String, Object>, Number> total = new TableColumn("الإجمالي");
        total.setCellValueFactory(v -> new ReadOnlyObjectWrapper<Number>((Number) v.getValue().get("total")));
        TableColumn<Map<String, Object>, Number> paid = new TableColumn("المدفوع");
        paid.setCellValueFactory(v -> new ReadOnlyObjectWrapper<Number>((Number) v.getValue().get("paid")));
        TableColumn<Map<String, Object>, Number> rest = new TableColumn("المتبقي");
        rest.setCellValueFactory(v -> new ReadOnlyObjectWrapper<Number>((Number) v.getValue().get("rest")));
        rest.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Number n, boolean empty) {
                super.updateItem(n, empty);
                if (empty || n == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(n));
                setStyle(n.doubleValue() > 0.001 ? "-fx-text-fill:#c62828; -fx-font-weight:bold;" : "");
            }
        });
        table.getColumns().setAll(List.of(id, no, date, cust, total, paid, rest));
    }

    private String str(Object o) { return o == null ? "" : o.toString(); }

    private void load() {
        try {
            String type = ((RadioButton) typeGroup.getSelectedToggle()).getText().contains("بيع") ? "بيع" : "شراء";
            var data = FXCollections.observableArrayList(dao.listInvoices(type));
            table.setItems(data);
            double sum = 0;
            for (var m : data) sum += (Double) m.get("total");
            lblSum.setText(String.format("%,.2f ر.س", sum));
        } catch (Exception e) { UI.error(e.getMessage()); }
    }

    private void showDetails() {
        Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { UI.info("اختر فاتورة من الجدول"); return; }
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("الفاتورة: ").append(sel.get("invoice_no")).append("\n")
              .append("التاريخ: ").append(sel.get("date")).append("\n")
              .append("العميل: ").append(sel.get("customer")).append("\n\n--- الأصناف ---\n");
            for (var it : dao.itemsOf((Integer) sel.get("id")))
                sb.append(String.format("%s × %s × %,.2f = %,.2f%n",
                        it.productName, it.quantity, it.price, it.total()));
            sb.append(String.format("%nالإجمالي: %,.2f | المدفوع: %,.2f | المتبقي: %,.2f",
                    (Double) sel.get("total"), (Double) sel.get("paid"), (Double) sel.get("rest")));
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("تفاصيل الفاتورة");
            a.setHeaderText(null);
            a.setContentText(sb.toString());
            a.getDialogPane().setPrefSize(420, 400);
            a.showAndWait();
        } catch (Exception e) { UI.error(e.getMessage()); }
    }

    private void periodReport() {
        LocalDate f = from.getValue(), t = to.getValue();
        if (f == null || t == null) { UI.info("حدد الفترة الزمنية"); return; }
        try {
            double sales = dao.sumSalesBetween(f, t);
            long count = 0;
            for (var m : dao.listInvoices("بيع")) {
                String d = str(m.get("date")).substring(0, 10);
                LocalDate ld = LocalDate.parse(d);
                if (!ld.isBefore(f) && !ld.isAfter(t)) count++;
            }
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("تقرير المبيعات");
            a.setHeaderText("الفترة من " + f + " إلى " + t);
            a.setContentText(String.format(
                    "عدد فواتير البيع: %d\nإجمالي المبيعات: %,.2f ر.س", count, sales));
            a.showAndWait();
        } catch (Exception e) { UI.error(e.getMessage()); }
    }
}
