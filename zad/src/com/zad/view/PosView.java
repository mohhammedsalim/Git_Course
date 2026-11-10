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
import java.util.List;

/** شاشة نقطة البيع (الكاشير) */
public class PosView extends BorderPane {

    private final TableView<Product> productsTable = new TableView<>();
    private final ObservableList<Product> master = FXCollections.observableArrayList();
    private final TextField search = new TextField();
    private final TextField barcodeField = new TextField();
    private final ComboBox<Person> customerBox = new ComboBox<>();
    private final Label lblTotal = new Label("0.00");
    private final Label lblCount = new Label("0");
    private final TextArea note = new TextArea();

    private final List<InvoiceDAO.InvoiceItem> items = new ArrayList<>();
    private final ProductDAO pdao = new ProductDAO();
    private final InvoiceDAO idao = new InvoiceDAO();

    public PosView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(15));

        Label title = new Label("نقطة البيع — فاتورة جديدة");
        title.getStyleClass().add("page-title");

        /* ===== الجانب الأيمن: اختيار المنتجات ===== */
        search.setPromptText("🔍 ابحث عن منتج...");
        search.textProperty().addListener((o, a, b) -> filterProducts());
        barcodeField.setPromptText("📷 امسح الباركود ثم Enter");
        barcodeField.setOnAction(e -> addByBarcode());

        SplitPane leftSplit = new SplitPane();
        leftSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        VBox searchBox = new VBox(8, search, barcodeField);
        searchBox.setPadding(new Insets(5));
        setupProductsTable();
        leftSplit.getItems().addAll(searchBox, productsTable);
        leftSplit.setDividerPositions(0.12);
        TitledPane tpProducts = new TitledPane("المنتجات المتاحة", leftSplit);

        /* ===== الجانب الأيسر: تفاصيل الفاتورة ===== */
        VBox right = new VBox(10);
        right.setPadding(new Insets(5));
        right.setMinWidth(360);

        customerBox.setPromptText("زبون نقدي");
        customerBox.setEditable(true);
        Button btnAddCustomer = new Button("+ عميل");
        btnAddCustomer.setOnAction(e -> quickAddCustomer());
        HBox custRow = new HBox(8, new Label("العميل:"), customerBox, btnAddCustomer);
        custRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox grow = new HBox();
        HBox.setHgrow(tpProducts, Priority.ALWAYS);
        grow.getChildren().addAll(tpProducts, right);
        HBox.setHgrow(grow, Priority.ALWAYS);

        TitledPane tpCart = buildCartPane();
        TitledPane tpPay = buildPayPane();

        right.getChildren().addAll(custRow, tpCart, tpPay);

        refreshProducts();
        refreshCustomers();

        VBox top = new VBox(8, title);
        setTop(top);
        setCenter(grow);
    }

    /* ---------- جدول المنتجات ---------- */
    private void setupProductsTable() {
        productsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Product, String> name = new TableColumn("المنتج");
        name.setCellValueFactory(v -> v.getValue().nameProperty());
        TableColumn<Product, Double> price = new TableColumn("السعر");
        price.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getSalePrice()));
        TableColumn<Product, Double> qty = new TableColumn("المخزون");
        qty.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getQuantity()));
        productsTable.getColumns().setAll(List.of(name, price, qty));
        productsTable.setItems(master);
        productsTable.setRowFactory(tv -> {
            TableRow<Product> row = new TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()) addToCart(row.getItem(), ev.isControlDown());
            });
            return row;
        });
    }

    private void filterProducts() {
        String s = search.getText().toLowerCase().trim();
        productsTable.setItems(master.filtered(p ->
                p.getName().toLowerCase().contains(s) || p.getBarcode().contains(s)));
    }

    public void refreshProducts() {
        try {
            master.setAll(pdao.findAll());
            filterProducts();
        } catch (Exception e) { UI.error("خطأ تحميل المنتجات: " + e.getMessage()); }
    }

    private void refreshCustomers() {
        try {
            customerBox.getItems().setAll(pdao.listPersons(true));
        } catch (Exception e) { /* تجاهل */ }
    }

    private void addByBarcode() {
        String code = barcodeField.getText().trim();
        if (code.isEmpty()) return;
        try {
            Product p = pdao.findByBarcode(code);
            if (p != null) {
                addToCart(p, false);
                barcodeField.clear();
            } else {
                UI.info("لا يوجد منتج بهذا الباركود — أضفه من شاشة المنتجات");
            }
        } catch (Exception e) {
            UI.error("خطأ في البحث بالباركود: " + e.getMessage());
        }
    }

    /* ---------- السلة ---------- */
    private TableView<InvoiceDAO.InvoiceItem> cartTable;
    private final ObservableList<InvoiceDAO.InvoiceItem> cartItems = FXCollections.observableArrayList();

    private TitledPane buildCartPane() {
        cartTable = new TableView<>();
        cartTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cartTable.setItems(cartItems);

        TableColumn<InvoiceDAO.InvoiceItem, String> cName = new TableColumn("الصنف");
        cName.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().productName));

        TableColumn<InvoiceDAO.InvoiceItem, Number> cQty = new TableColumn("الكمية");
        cQty.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().quantity));

        TableColumn<InvoiceDAO.InvoiceItem, Number> cPrice = new TableColumn("السعر");
        cPrice.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().price));

        TableColumn<InvoiceDAO.InvoiceItem, Number> cTot = new TableColumn("الإجمالي");
        cTot.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().total()));

        cartTable.getColumns().setAll(List.of(cName, cQty, cPrice, cTot));

        Button btnRemove = new Button("إزالة السطر المحدد ✖");
        btnRemove.setStyle("-fx-background-color:#c62828; -fx-text-fill:white;");
        btnRemove.setOnAction(e -> {
            int i = cartTable.getSelectionModel().getSelectedIndex();
            if (i >= 0) { cartItems.remove(i); recalc(); }
        });

        VBox box = new VBox(6, cartTable, btnRemove);
        VBox.setVgrow(cartTable, Priority.ALWAYS);
        TitledPane tp = new TitledPane("فاتورة البيع (" + cartItems.size() + " صنف)", box);
        tp.setMaxHeight(260);
        tp.setCollapsible(false);
        return tp;
    }

    private void addToCart(Product p, boolean multi) {
        // دمج الأصناف المكررة
        for (InvoiceDAO.InvoiceItem it : cartItems) {
            if (it.productId == p.getId()) {
                it.quantity += 1;
                cartItems.set(cartItems.indexOf(it), it);
                recalc();
                return;
            }
        }
        InvoiceDAO.InvoiceItem it = new InvoiceDAO.InvoiceItem();
        it.productId = p.getId();
        it.productName = p.getName();
        it.quantity = 1;
        it.price = p.getSalePrice();
        cartItems.add(it);
        recalc();
    }

    private void recalc() {
        double total = 0;
        for (var it : cartItems) total += it.total();
        lblTotal.setText(String.format("%,.2f ر.س", total));
        lblCount.setText(String.valueOf(cartItems.size()));
    }

    /* ---------- الدفع والحفظ ---------- */
    private TextField paidField = new TextField("0");

    private TitledPane buildPayPane() {
        lblTotal.setStyle("-fx-font-size:24px; -fx-font-weight:bold; -fx-text-fill:#2e7d32;");
        lblCount.setStyle("-fx-text-fill:#666;");
        note.setPromptText("ملاحظات على الفاتورة...");
        note.setPrefRowCount(2);

        Button btnSave = new Button("💾 حفظ الفاتورة");
        btnSave.setStyle("-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; -fx-padding:10;");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnSave.setOnAction(e -> saveInvoice());

        Button btnClear = new Button("تفريغ الفاتورة");
        btnClear.setMaxWidth(Double.MAX_VALUE);
        btnClear.setOnAction(e -> { cartItems.clear(); recalc(); });

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8);
        g.addRow(0, new Label("الإجمالي:"), lblTotal);
        g.addRow(1, new Label("عدد الأصناف:"), lblCount);
        g.addRow(2, new Label("المبلغ المدفوع:"), paidField);
        VBox box = new VBox(8, g, note, btnSave, btnClear);
        TitledPane tp = new TitledPane("الدفع", box);
        tp.setCollapsible(false);
        return tp;
    }

    private void saveInvoice() {
        if (cartItems.isEmpty()) { UI.info("أضف صنفاً واحداً على الأقل للفاتورة"); return; }
        double total = 0;
        for (var it : cartItems) total += it.total();
        double paid = UI.parseD(paidField.getText(), total);
        if (paid > total) paid = total;

        Person c = null;
        String sel = customerBox.getEditor().getText();
        if (sel != null && !sel.isBlank()) {
            for (Person p : customerBox.getItems())
                if (p.getName().equals(sel.trim())) { c = p; break; }
        }

        if (!UI.confirm("إجمالي الفاتورة: " + String.format("%,.2f ر.س", total) +
                "\nهل تريد حفظها؟")) return;

        try {
            int id = idao.saveInvoice("بيع", c == null ? 0 : c.getId(),
                    new ArrayList<>(cartItems), total, paid);
            cartItems.clear();
            paidField.setText("0");
            recalc();
            refreshProducts();
            refreshCustomers();
            showInvoice(id, total, paid, c);
        } catch (Exception e) {
            UI.error("تعذر حفظ الفاتورة: " + e.getMessage());
        }
    }

    private void showInvoice(int id, double total, double paid, Person c) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("تمت عملية البيع ✓");
        a.setHeaderText("رقم الفاتورة: " + id);
        StringBuilder sb = new StringBuilder();
        sb.append("التاريخ: ").append(UI.now()).append("\n");
        sb.append("العميل: ").append(c == null ? "زبون نقدي" : c.getName()).append("\n\n");
        try {
            for (var it : idao.itemsOf(id))
                sb.append(String.format("%s × %s = %,.2f%n", it.productName, trim(it.quantity), it.total()));
        } catch (Exception ignored) {}
        sb.append(String.format("%nالإجمالي: %,.2f ر.س   |   المدفوع: %,.2f   |   المتبقي: %,.2f",
                total, paid, total - paid));
        a.setContentText(sb.toString());
        a.showAndWait();
    }

    private String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.2f", d);
    }

    private void quickAddCustomer() {
        String name = UI.input("عميل جديد", "اسم العميل:");
        if (name == null || name.isBlank()) return;
        String phone = UI.input("عميل جديد", "رقم الهاتف (اختياري):");
        try {
            pdao.savePerson(new Person(0, name.trim(), phone == null ? "" : phone.trim(), "", 0), true);
            refreshCustomers();
            customerBox.getEditor().setText(name.trim());
        } catch (Exception e) { UI.error("تعذر إضافة العميل: " + e.getMessage()); }
    }
}
