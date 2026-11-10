package com.zad.dao;

import com.zad.db.Database;
import com.zad.model.Person;
import com.zad.model.Product;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** عمليات المنتجات والأصناف والأشخاص */
public class ProductDAO {

    /* ---------- الأصناف ---------- */
    public List<String> listCategories() throws SQLException {
        List<String> list = new ArrayList<>();
        var rs = Database.getConnection().createStatement()
                .executeQuery("SELECT name FROM categories ORDER BY id");
        while (rs.next()) list.add(rs.getString(1));
        return list;
    }

    public int categoryId(String name) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT id FROM categories WHERE name=?");
        ps.setString(1, name);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    public void addCategory(String name) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "INSERT INTO categories(name) VALUES(?)");
        ps.setString(1, name);
        ps.executeUpdate();
    }

    /* ---------- المنتجات ---------- */
    public List<Product> findAll() throws SQLException {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT p.*, c.name AS cat FROM products p LEFT JOIN categories c ON p.category_id=c.id ORDER BY p.name";
        ResultSet rs = Database.getConnection().createStatement().executeQuery(sql);
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    public Product findByBarcode(String barcode) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT p.*, c.name AS cat FROM products p LEFT JOIN categories c ON p.category_id=c.id WHERE p.barcode=?");
        ps.setString(1, barcode);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? map(rs) : null;
    }

    public Product map(ResultSet rs) throws SQLException {
        return new Product(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("barcode") == null ? "" : rs.getString("barcode"),
                rs.getInt("category_id"),
                rs.getString("cat") == null ? "" : rs.getString("cat"),
                rs.getString("unit"),
                rs.getDouble("purchase_price"),
                rs.getDouble("sale_price"),
                rs.getDouble("quantity"),
                rs.getDouble("min_quantity"));
    }

    public void save(Product p) throws SQLException {
        if (p.getId() == 0) {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "INSERT INTO products(name,barcode,category_id,unit,purchase_price,sale_price,quantity,min_quantity) VALUES(?,?,?,?,?,?,?,?)");
            ps.setString(1, p.getName());
            ps.setString(2, p.getBarcode().isEmpty() ? null : p.getBarcode());
            ps.setInt(3, p.getCategoryId());
            ps.setString(4, p.getUnit());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSalePrice());
            ps.setDouble(7, p.getQuantity());
            ps.setDouble(8, p.getMinQuantity());
            ps.executeUpdate();
        } else {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "UPDATE products SET name=?,barcode=?,category_id=?,unit=?,purchase_price=?,sale_price=?,quantity=?,min_quantity=? WHERE id=?");
            ps.setString(1, p.getName());
            ps.setString(2, p.getBarcode().isEmpty() ? null : p.getBarcode());
            ps.setInt(3, p.getCategoryId());
            ps.setString(4, p.getUnit());
            ps.setDouble(5, p.getPurchasePrice());
            ps.setDouble(6, p.getSalePrice());
            ps.setDouble(7, p.getQuantity());
            ps.setDouble(8, p.getMinQuantity());
            ps.setInt(9, p.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "DELETE FROM products WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public void adjustQuantity(int productId, double delta) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "UPDATE products SET quantity=quantity+? WHERE id=?");
        ps.setDouble(1, delta);
        ps.setInt(2, productId);
        ps.executeUpdate();
    }

    /* ---------- الأشخاص (عملاء/موردون) ---------- */
    public List<Person> listPersons(boolean customer) throws SQLException {
        List<Person> list = new ArrayList<>();
        String table = customer ? "customers" : "suppliers";
        ResultSet rs = Database.getConnection().createStatement()
                .executeQuery("SELECT * FROM " + table + " ORDER BY name");
        while (rs.next()) {
            list.add(new Person(rs.getInt("id"), rs.getString("name"),
                    rs.getString("phone"), rs.getString("address"), rs.getDouble("balance")));
        }
        return list;
    }

    public void savePerson(Person p, boolean customer) throws SQLException {
        String table = customer ? "customers" : "suppliers";
        if (p.getId() == 0) {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "INSERT INTO " + table + "(name,phone,address,balance) VALUES(?,?,?,?)");
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getAddress());
            ps.setDouble(4, p.getBalance());
            ps.executeUpdate();
        } else {
            PreparedStatement ps = Database.getConnection().prepareStatement(
                    "UPDATE " + table + " SET name=?,phone=?,address=?,balance=? WHERE id=?");
            ps.setString(1, p.getName());
            ps.setString(2, p.getPhone());
            ps.setString(3, p.getAddress());
            ps.setDouble(4, p.getBalance());
            ps.setInt(5, p.getId());
            ps.executeUpdate();
        }
    }

    public void deletePerson(int id, boolean customer) throws SQLException {
        String table = customer ? "customers" : "suppliers";
        Database.getConnection().createStatement().executeUpdate("DELETE FROM " + table + " WHERE id=" + id);
    }
}
