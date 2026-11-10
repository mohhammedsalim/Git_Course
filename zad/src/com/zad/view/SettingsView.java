package com.zad.view;

import com.zad.dao.ProductDAO;
import com.zad.util.UI;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;

/** شاشة الإعدادات: إدارة التصنيفات وبيانات المحل */
public class SettingsView extends BorderPane {

    public SettingsView() {
        getStyleClass().add("content-pane");
        setPadding(new Insets(20));

        Label title = new Label("الإعدادات");
        title.getStyleClass().add("page-title");

        /* --- إدارة التصنيفات --- */
        ListView<String> cats = new ListView<>();
        ProductDAO dao = new ProductDAO();
        try { cats.getItems().setAll(dao.listCategories()); } catch (Exception e) { UI.error(e.getMessage()); }

        Button btnAddCat = new Button("+ صنف جديد");
        btnAddCat.setOnAction(e -> {
            String name = UI.input("صنف جديد", "اسم التصنيف:");
            if (name != null && !name.isBlank()) {
                try {
                    dao.addCategory(name.trim());
                    cats.getItems().add(name.trim());
                } catch (Exception ex) { UI.error("تعذر الإضافة: " + ex.getMessage()); }
            }
        });
        Button btnDelCat = new Button("حذف الصنف المحدد 🗑");
        btnDelCat.setStyle("-fx-background-color:#c62828; -fx-text-fill:white;");
        btnDelCat.setOnAction(e -> UI.info("لا يمكن حذف تصنيف — احذف منتجاته أولاً من شاشة المنتجات"));

        VBox catBox = new VBox(10, new Label("تصنيفات المنتجات:"), cats,
                new HBox(10, btnAddCat, btnDelCat));
        VBox.setVgrow(cats, Priority.ALWAYS);
        catBox.setPadding(new Insets(15));
        catBox.getStyleClass().add("card");

        /* --- معلومات النظام --- */
        Label info = new Label("""
                تطبيق زاد لإدارة المحلات الغذائية
                ---------------------------------
                • قاعدة البيانات: SQLite (ملف zad.db بجانب التطبيق)
                • الواجهة: JavaFX 17
                • الوحدة النقدية: ريال
                • النسخ الاحتياطي: انسخ ملف zad.db periodically

                نصائح الاستخدام:
                1. أضف التصنيفات والمنتجات أولاً من شاشة المنتجات.
                2. استخدم نقطة البيع لإصدار الفواتير اليومية.
                3. استخدم شاشة المشتريات لتوريد البضاعة من الموردين.
                4. راجع التقارير يومياً من شاشة الفواتير والتقارير.
                """);
        info.setWrapText(true);
        VBox infoBox = new VBox(10, new Label("حول التطبيق:"), info);
        infoBox.setPadding(new Insets(15));
        infoBox.getStyleClass().add("card");

        HBox content = new HBox(20, catBox, infoBox);
        content.setPadding(new Insets(15, 0, 0, 0));
        HBox.setHgrow(catBox, Priority.ALWAYS);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        setTop(title);
        setCenter(content);
    }
}
