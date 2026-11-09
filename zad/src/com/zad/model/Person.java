package com.zad.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/** نموذج مشترك أو مورد (نفس الحقول) */
public class Person {
    private final SimpleIntegerProperty id = new SimpleIntegerProperty();
    private final SimpleStringProperty name = new SimpleStringProperty("");
    private final SimpleStringProperty phone = new SimpleStringProperty("");
    private final SimpleStringProperty address = new SimpleStringProperty("");
    private final SimpleDoubleProperty balance = new SimpleDoubleProperty(0);

    public Person() {}

    public Person(int id, String name, String phone, String address, double balance) {
        this.id.set(id);
        this.name.set(name);
        this.phone.set(phone);
        this.address.set(address);
        this.balance.set(balance);
    }

    public int getId() { return id.get(); }
    public void setId(int v) { id.set(v); }
    public SimpleIntegerProperty idProperty() { return id; }

    public String getName() { return name.get(); }
    public void setName(String v) { name.set(v); }
    public SimpleStringProperty nameProperty() { return name; }

    public String getPhone() { return phone.get(); }
    public void setPhone(String v) { phone.set(v); }
    public SimpleStringProperty phoneProperty() { return phone; }

    public String getAddress() { return address.get(); }
    public void setAddress(String v) { address.set(v); }
    public SimpleStringProperty addressProperty() { return address; }

    public double getBalance() { return balance.get(); }
    public void setBalance(double v) { balance.set(v); }
    public SimpleDoubleProperty balanceProperty() { return balance; }
}
