package com.zad.view;

import com.zad.dao.ProductDAO;
import com.zad.model.Person;
import com.zad.util.UI;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

/** شاشة إدارة العملاء والموردين */
public class PeopleView extends BorderPane {

    private final ToggleGroup group = new ToggleGroup();
    private final TableView<Person> table = new TableView<>();

    public PeopleView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(20));

        Label title = new Label("إدارة العملاء والموردين");
        title.getStyleClass().add("page-title");

        RadioButton rbCust = new RadioButton("العملاء");
        RadioButton rbSup = new RadioButton("الموردين");
        rbCust.setToggleGroup(group);
        rbSup.setToggleGroup(group);
        rbCust.setSelected(true);

        Button btnAdd = new Button("+ إضافة");
        btnAdd.setStyle("-fx-background-color:#2e7d32; -fx-text-fill:white; -fx-font-weight:bold;");
        Button btnEdit = new Button("✎ تعديل");
        Button btnDel = new Button("🗑 حذف");
        btnDel.setStyle("-fx-background-color:#c62828; -fx-text-fill:white;");

        ProductDAO dao = new ProductDAO();
        btnAdd.setOnAction(e -> openDialog(null, isCustomer()));
        btnEdit.setOnAction(e -> {
            Person sel = table.getSelectionModel().getSelectedItem();
            if (sel != null) openDialog(sel, isCustomer()); else UI.info("اختر سجلاً من الجدول");
        });
        btnDel.setOnAction(e -> {
            Person sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.info("اختر سجلاً أولاً"); return; }
            if (UI.confirm("حذف: " + sel.getName() + " ؟")) {
                try { dao.deletePerson(sel.getId(), isCustomer()); loadTable(); }
                catch (Exception ex) { UI.error(ex.getMessage()); }
            }
        });

        HBox switcher = new HBox(15, rbCust, rbSup);
        HBox tools = new HBox(10, search(), btnAdd, btnEdit, btnDel);
        tools.setPadding(new Insets(10, 0, 10, 0));

        setupTable();
        VBox top = new VBox(8, title, switcher, tools);
        setTop(top);
        setCenter(table);

        rbCust.setOnAction(e -> { if (rbCust.isSelected()) loadTable(); });
        rbSup.setOnAction(e -> { if (rbSup.isSelected()) loadTable(); });
        loadTable();
    }

    private TextField searchField;
    private TextField search() {
        searchField = new TextField();
        searchField.setPromptText("🔍 بحث بالاسم أو الهاتف...");
        searchField.textProperty().addListener((o, a, b) -> filterTable());
        return searchField;
    }

    private boolean isCustomer() {
        Toggle t = group.getSelectedToggle();
        return t instanceof RadioButton rb && rb.getText().equals("العملاء");
    }

    private javafx.collections.ObservableList<Person> master = FXCollections.observableArrayList();
    private javafx.collections.transformation.FilteredList<Person> filtered;

    private void setupTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<Person, Number> id = new TableColumn("م");
        id.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getId()));
        id.setMaxWidth(50);
        TableColumn<Person, String> name = new TableColumn("الاسم");
        name.setCellValueFactory(v -> v.getValue().nameProperty());
        TableColumn<Person, String> phone = new TableColumn("الهاتف");
        phone.setCellValueFactory(v -> v.getValue().phoneProperty());
        TableColumn<Person, String> addr = new TableColumn("العنوان");
        addr.setCellValueFactory(v -> v.getValue().addressProperty());
        TableColumn<Person, Number> bal = new TableColumn("الرصيد المستحق");
        bal.setCellValueFactory(v -> new ReadOnlyObjectWrapper<>(v.getValue().getBalance()));
        table.getColumns().setAll(List.of(id, name, phone, addr, bal));
        filtered = new javafx.collections.transformation.FilteredList<>(master);
        table.setItems(filtered);
    }

    private void loadTable() {
        try {
            master.setAll(new ProductDAO().listPersons(isCustomer()));
            filterTable();
        } catch (Exception e) { UI.error(e.getMessage()); }
    }

    private void filterTable() {
        String s = searchField.getText().toLowerCase().trim();
        filtered.setPredicate(p -> s.isEmpty() || p.getName().toLowerCase().contains(s)
                || p.getPhone().contains(s));
    }

    private void openDialog(Person existing, boolean customer) {
        Dialog<Person> d = new Dialog<>();
        d.setTitle(existing == null ? "إضافة " + (customer ? "عميل" : "مورد") : "تعديل بيانات");
        d.setHeaderText(null);
        d.getDialogPane().getButtonTypes().setAll(ButtonType.APPLY, ButtonType.CANCEL);

        GridPane g = new GridPane();
        g.setHgap(12); g.setVgap(10);
        g.setPadding(new Insets(15));
        TextField fName = new TextField();
        TextField fPhone = new TextField();
        TextArea fAddr = new TextArea();
        fAddr.setPrefRowCount(2);
        TextField fBal = new TextField("0");
        g.addRow(0, new Label("الاسم *"), fName);
        g.addRow(1, new Label("الهاتف"), fPhone);
        g.addRow(2, new Label("العنوان"), fAddr);
        g.addRow(3, new Label("رصيد افتتاحي (مستحق)"), fBal);

        if (existing != null) {
            fName.setText(existing.getName());
            fPhone.setText(existing.getPhone());
            fAddr.setText(existing.getAddress());
            fBal.setText(String.valueOf(existing.getBalance()));
        }
        d.getDialogPane().setContent(g);
        d.setResultConverter(bt -> {
            if (bt != ButtonType.APPLY || fName.getText().isBlank()) return null;
            Person p = existing == null ? new Person() : existing;
            p.setName(fName.getText().trim());
            p.setPhone(fPhone.getText().trim());
            p.setAddress(fAddr.getText().trim());
            p.setBalance(UI.parseD(fBal.getText(), 0));
            return p;
        });
        d.showAndWait().ifPresent(p -> {
            try {
                new ProductDAO().savePerson(p, customer);
                loadTable();
                UI.info("تم الحفظ ✓");
            } catch (Exception e) { UI.error(e.getMessage()); }
        });
    }
}
