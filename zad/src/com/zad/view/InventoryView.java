package com.zad.view;

import com.zad.dao.InvoiceDAO;
import com.zad.dao.ProductDAO;
import com.zad.model.Person;
import com.zad.model.Product;
import com.zad.util.UI;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.List;

/** شاشة المشتريات / إدارة المخزون (إدخال بضاعة من الموردين) */
public class InventoryView extends BorderPane {

    private final ComboBox<Person> supplierBox = new ComboBox<>();
    private final ComboBox<Product> productBox = new ComboBox<>();
    private final TextField qtyField = new TextField("1");
    private final TextField priceField = new TextField("0");
    private final ObservableList<InvoiceDAO.InvoiceItem> rows = FXCollections.observableArrayList();
    private final TableView<InvoiceDAO.InvoiceItem> table = new TableView<>();
    private final Label lblTotal = new Label("0.00");
    private final ProductDAO pdao = new ProductDAO();
    private final InvoiceDAO idao = new InvoiceDAO();

    public InventoryView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(20));

        Label title = new Label("المشتريات وإدخال البضاعة للمخزون");
        title.getStyleClass().add("page-title");

        supplierBox.setPromptText("اختر المورد...");
        supplierBox.setEditable(true);
        Button btnSup = new Button("+ مورد");
        btnSup.setOnAction(e -> quickAddSupplier());

        productBox.setPromptText("اختر المنتج...");
        qtyField.setPromptText("الكمية");
        priceField.setPromptText("سعر الشراء للوحدة");

        Button btnAddRow = new Button("+ إضافة للطلب");
        btnAddRow.setOnAction(e -> addRow());

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.addRow(0, new Label("المورد:"), supplierBox, btnSup);
        form.addRow(1, new Label("المنتج:"), productBox);
        form.addRow(2, new Label("الكمية × سعر الشراء:"), qtyField, priceField, btnAddRow);

        setupTable();
        Button btnSave = new Button("💾 توريد وحفظ الفاتورة وتحديث المخزون");
        btnSave.setStyle("-fx-background-color:#1565c0; -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:10;");
        btnSave.setOnAction(e -> savePurchase());
        HBox bottom = new HBox(15, new Label("الإجمالي:"), lblTotal, btnSave);
        bottom.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        bottom.setPadding(new Insets(10, 0, 0, 0));
        lblTotal.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:#1565c0;");

        VBox top = new VBox(12, title, form);
        VBox center = new VBox(10, table, bottom);
        VBox.setVgrow(table, Priority.ALWAYS);
        setTop(top);
        setCenter(center);

        refreshData();
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(rows);
        TableColumn<InvoiceDAO.InvoiceItem, String> c1 = new TableColumn("المنتج");
        c1.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().productName));
        TableColumn<InvoiceDAO.InvoiceItem, Number> c2 = new TableColumn("الكمية");
        c2.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().quantity));
        TableColumn<InvoiceDAO.InvoiceItem, Number> c3 = new TableColumn("سعر الشراء");
        c3.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().price));
        TableColumn<InvoiceDAO.InvoiceItem, Number> c4 = new TableColumn("الإجمالي");
        c4.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().total()));
        table.getColumns().setAll(List.of(c1, c2, c3, c4));
    }

    public void refreshData() {
        try {
            supplierBox.getItems().setAll(pdao.listPersons(false));
            productBox.getItems().setAll(pdao.findAll());
        } catch (Exception e) { UI.error(e.getMessage()); }
    }

    private void addRow() {
        Product p = productBox.getValue();
        if (p == null) { UI.info("اختر منتجاً أولاً"); return; }
        double q = UI.parseD(qtyField.getText(), 0);
        double pr = UI.parseD(priceField.getText(), p.getPurchasePrice());
        if (q <= 0) { UI.info("أدخل كمية صحيحة أكبر من صفر"); return; }
        InvoiceDAO.InvoiceItem it = new InvoiceDAO.InvoiceItem();
        it.productId = p.getId();
        it.productName = p.getName();
        it.quantity = q;
        it.price = pr;
        rows.add(it);
        recalc();
        qtyField.setText("1");
    }

    private void recalc() {
        double t = 0;
        for (var it : rows) t += it.total();
        lblTotal.setText(String.format("%,.2f ر.س", t));
    }

    private void savePurchase() {
        if (rows.isEmpty()) { UI.info("أضف أصنافاً للطلب أولاً"); return; }
        double total = 0;
        for (var it : rows) total += it.total();

        Person sup = supplierBox.getValue();
        if (sup != null) {
            if (!UI.confirm("سيتم تحديث أسعار الشراء وكميات المخزون.\nحفظ فاتورة التوريد بإجمالي " +
                    String.format("%,.2f ر.س", total) + " ؟")) return;
        } else {
            if (!UI.confirm("لم يتم اختيار مورد — هل تريد الحفظ كتوريد نقدي بدون مورد؟")) return;
        }

        try {
            var conn = com.zad.db.Database.getConnection();
            // تحديث سعر الشراء لكل صنف
            for (var it : rows) {
                conn.createStatement().executeUpdate(
                        "UPDATE products SET purchase_price=" + it.price + " WHERE id=" + it.productId);
            }
            int id = idao.saveInvoice("شراء", sup == null ? 0 : sup.getId(),
                    new ArrayList<>(rows), total, total);
            rows.clear();
            recalc();
            refreshData();
            UI.info("تم توريد البضاعة بنجاح ✓ رقم فاتورة الشراء: " + id + "\nوتم تحديث المخزون.");
        } catch (Exception e) {
            UI.error("تعذر الحفظ: " + e.getMessage());
        }
    }

    private void quickAddSupplier() {
        String name = UI.input("مورد جديد", "اسم المورد:");
        if (name == null || name.isBlank()) return;
        String phone = UI.input("مورد جديد", "رقم الهاتف (اختياري):");
        try {
            pdao.savePerson(new Person(0, name.trim(), phone == null ? "" : phone.trim(), "", 0), false);
            refreshData();
        } catch (Exception e) { UI.error(e.getMessage()); }
    }
}
