package com.zad.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/** نموذج المنتج */
public class Product {
    private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty barcode = new SimpleStringProperty("");
    private final SimpleIntegerProperty categoryId = new SimpleIntegerProperty();
    private final SimpleStringProperty categoryName = new SimpleStringProperty("");
    private final SimpleStringProperty unit = new SimpleStringProperty("حبة");
    private final SimpleDoubleProperty purchasePrice = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty salePrice = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty quantity = new SimpleDoubleProperty(0);
    private final SimpleDoubleProperty minQuantity = new SimpleDoubleProperty(5);

    public Product() {}

    public Product(int id, String name, String barcode, int categoryId, String categoryName,
                   String unit, double purchasePrice, double salePrice, double quantity, double minQuantity) {
        this.id.set(id);
        this.name.set(name);
        this.barcode.set(barcode);
        this.categoryId.set(categoryId);
        this.categoryName.set(categoryName);
        this.unit.set(unit);
        this.purchasePrice.set(purchasePrice);
        this.salePrice.set(salePrice);
        this.quantity.set(quantity);
        this.minQuantity.set(minQuantity);
    }

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public SimpleIntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public SimpleStringProperty nameProperty() { return name; }

    public String getBarcode() { return barcode.get(); }
    public void setBarcode(String v) { barcode.set(v); }
    public SimpleStringProperty barcodeProperty() { return barcode; }

    public int getCategoryId() { return categoryId.get(); }
    public void setCategoryId(int v) { categoryId.set(v); }

    public String getCategoryName() { return categoryName.get(); }
    public void setCategoryName(String v) { categoryName.set(v); }
    public SimpleStringProperty categoryNameProperty() { return categoryName; }

    public String getUnit() { return unit.get(); }
    public void setUnit(String v) { unit.set(v); }
    public SimpleStringProperty unitProperty() { return unit; }

    public double getPurchasePrice() { return purchasePrice.get(); }
    public void setPurchasePrice(double v) { purchasePrice.set(v); }
    public SimpleDoubleProperty purchasePriceProperty() { return purchasePrice; }

    public double getSalePrice() { return salePrice.get(); }
    public void setSalePrice(double v) { salePrice.set(v); }
    public SimpleDoubleProperty salePriceProperty() { return salePrice; }

    public double getQuantity() { return quantity.get(); }
    public void setQuantity(double v) { quantity.set(v); }
    public SimpleDoubleProperty quantityProperty() { return quantity; }

    public double getMinQuantity() { return minQuantity.get(); }
    public void setMinQuantity(double v) { minQuantity.set(v); }
    public SimpleDoubleProperty minQuantityProperty() { return minQuantity; }
}
