package com.zad.view;

import com.zad.dao.ProductDAO;
import com.zad.model.Product;
import com.zad.util.UI;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

/** شاشة إدارة المنتجات والمخزون */
public class ProductsView extends BorderPane {

    private final TableView<Product> table = new TableView<>();
    private final ObservableList<Product> master = FXCollections.observableArrayList();
    private final FilteredList<Product> filtered = new FilteredList<>(master);
    private final TextField search = new TextField();
    private final ComboBox<String> catFilter = new ComboBox<>();
    private final ProductDAO dao = new ProductDAO();

    public ProductsView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(20));

        Label title = new Label("إدارة المنتجات والمواد الغذائية");
        title.getStyleClass().add("page-title");

        search.setPromptText("🔍 ابحث بالاسم أو الباركود...");
        search.setPrefWidth(300);
        catFilter.setPromptText("كل التصنيفات");
        catFilter.getItems().add("كل التصنيفات");

        Button btnAdd = new Button("+ إضافة منتج");
        btnAdd.setStyle("-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-weight:bold; -fx-padding:8 16;");
        Button btnEdit = new Button("✎ تعديل");
        Button btnDel = new Button("🗑 حذف");
        btnDel.setStyle("-fx-background-color:#c62828; -fx-text-fill:white;");
        Button btnRefresh = new Button("↻ تحديث");
        btnAdd.setOnAction(e -> openDialog(null));
        btnEdit.setOnAction(e -> {
            Product sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openDialog(sel); else UI.info("اختر منتجاً من الجدول أولاً");
        });
        btnDel.setOnAction(e -> deleteSelected());
        btnRefresh.setOnAction(e -> refresh());

        HBox toolbar = new HBox(10, title);
        toolbar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox tools = new HBox(10, search, catFilter, btnAdd, btnEdit, btnDel, btnRefresh);
        tools.setPadding(new Insets(10, 0, 10, 0));

        setupTable();
        table.setItems(filtered);

        search.textProperty().addListener((o, a, b) -> applyFilter());
        catFilter.valueProperty().addListener((o, a, b) -> applyFilter());

        VBox top = new VBox(5, title, tools);
        setTop(top);
        setCenter(table);

        refresh();
        loadCategories();
    }

    private void loadCategories() {
        try {
            catFilter.getItems().setAll("كل التصنيفات");
            catFilter.getItems().addAll(dao.listCategories());
        } catch (Exception e) { /* تجاهل */ }
    }

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Product, String> cName = new TableColumn("اسم المنتج");
        cName.setCellValueFactory(v -> v.getValue().nameProperty());
        cName.setPrefWidth(220);

        TableColumn<Product, String> cBar = new TableColumn("الباركود");
        cBar.setCellValueFactory(v -> v.getValue().barcodeProperty());

        TableColumn<Product, String> cCat = new TableColumn("التصنيف");
        cCat.setCellValueFactory(v -> v.getValue().categoryNameProperty());

        TableColumn<Product, String> cUnit = new TableColumn("الوحدة");
        cUnit.setCellValueFactory(v -> v.getValue().unitProperty());

        TableColumn<Product, Double> cBuy = new TableColumn("سعر الشراء");
        cBuy.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getPurchasePrice()));

        TableColumn<Product, Double> cSale = new TableColumn("سعر البيع");
        cSale.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getSalePrice()));

        TableColumn<Product, Double> cQty = new TableColumn("الرصيد الحالي");
        cQty.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getQuantity()));
        cQty.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double q, boolean empty) {
                super.updateItem(q, empty);
                if (empty || q == null) { setText(null); setStyle(""); return; }
                setText(String.valueOf(q));
                Product p = getTableRow().getItem();
                if (p != null && q <= p.getMinQuantity())
                    setStyle("-fx-text-fill:#c62828; -fx-font-weight:bold;");
                else setStyle("");
            }
        });

        TableColumn<Product, Double> cMin = new TableColumn("حد التنبيه");
        cMin.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getMinQuantity()));

        table.getColumns().setAll(List.of(cName, cBar, cCat, cUnit, cBuy, cSale, cQty, cMin));
    }

    private void applyFilter() {
        String s = search.getText() == null ? "" : search.getText().toLowerCase().trim();
        String cat = catFilter.getValue();
        filtered.setPredicate(p -> {
            boolean okCat = (cat == null || cat.equals("كل التصنيفات") || cat.equals(p.getCategoryName()));
            boolean okSearch = s.isEmpty() || p.getName().toLowerCase().contains(s)
                    || p.getBarcode().toLowerCase().contains(s);
            return okCat && okSearch;
        });
    }

    public void refresh() {
        try {
            master.setAll(dao.findAll());
            applyFilter();
        } catch (Exception e) {
            UI.error("خطأ في تحميل المنتجات: " + e.getMessage());
        }
    }

    private void deleteSelected() {
        Product sel = table.getSelectionModel().getSelectedItem();
        if (sel == null) { UI.info("اختر منتجاً أولاً"); return; }
        if (!UI.confirm("هل تريد حذف المنتج: " + sel.getName() + " ؟")) return;
        try {
            dao.delete(sel.getId());
            refresh();
            UI.info("تم حذف المنتج بنجاح");
        } catch (Exception e) {
            UI.error("تعذر الحذف: " + e.getMessage());
        }
    }

    /** نافذة إضافة/تعديل منتج */
    private void openDialog(Product existing) {
        Dialog<Product> dialog = new Dialog<>();
        dialog.setTitle(existing == null ? "إضافة منتج جديد" : "تعديل المنتج");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.APPLY, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));

        TextField fName = new TextField();
        TextField fBar = new TextField();
        ComboBox<String> fCat = new ComboBox<>();
        ComboBox<String> fUnit = new ComboBox<>();
        fUnit.getItems().addAll("حبة", "كيلو", "غرام", "لتار", "علبة", "كرتون", "كيس");
        fUnit.setEditable(true);
        TextField fBuy = new TextField("0");
        TextField fSale = new TextField("0");
        TextField fQty = new TextField("0");
        TextField fMin = new TextField("5");
        fCat.getItems().addAll(catFilter.getItems());
        fCat.setValue(catFilter.getValue());

        int r = 0;
        grid.addRow(r++, new Label("اسم المنتج *"), fName);
        grid.addRow(r++, new Label("الباركود"), fBar);
        grid.addRow(r++, new Label("التصنيف"), fCat);
        grid.addRow(r++, new Label("الوحدة"), fUnit);
        grid.addRow(r++, new Label("سعر الشراء"), fBuy);
        grid.addRow(r++, new Label("سعر البيع *"), fSale);
        grid.addRow(r++, new Label("الرصيد الحالي"), fQty);
        grid.addRow(r++, new Label("حد التنبيه للنفاد"), fMin);

        if (existing != null) {
            fName.setText(existing.getName());
            fBar.setText(existing.getBarcode());
            fCat.setValue(existing.getCategoryName());
            fUnit.setValue(existing.getUnit());
            fBuy.setText(String.valueOf(existing.getPurchasePrice()));
            fSale.setText(String.valueOf(existing.getSalePrice()));
            fQty.setText(String.valueOf(existing.getQuantity()));
            fMin.setText(String.valueOf(existing.getMinQuantity()));
        }

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.APPLY) {
                if (fName.getText().isBlank() || fSale.getText().isBlank()) return null;
                Product p = existing == null ? new Product() : existing;
                p.setName(fName.getText().trim());
                p.setBarcode(fBar.getText().trim());
                try { p.setCategoryId(dao.categoryId(fCat.getValue())); } catch (Exception ignored) {}
                p.setCategoryName(fCat.getValue() == null ? "" : fCat.getValue());
                p.setUnit(fUnit.getValue() == null ? "حبة" : fUnit.getValue());
                p.setPurchasePrice(UI.parseD(fBuy.getText(), 0));
                p.setSalePrice(UI.parseD(fSale.getText(), 0));
                p.setQuantity(UI.parseD(fQty.getText(), 0));
                p.setMinQuantity(UI.parseD(fMin.getText(), 5));
                return p;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(p -> {
            try {
                dao.save(p);
                refresh();
                UI.info("تم حفظ المنتج بنجاح ✓");
            } catch (Exception e) {
                UI.error("تعذر الحفظ (تأكد من عدم تكرار الباركود): " + e.getMessage());
            }
        });
    }
}
