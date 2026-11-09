package com.zad.dao;

import com.zad.db.Database;
import com.zad.model.Product;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** عمليات الفواتير (بيع وشراء) */
public class InvoiceDAO {

    public static class InvoiceItem {
        public int productId;
        public String productName;
        public double quantity;
        public double price;
        public double total() { return quantity * price; }
    }

    /** حفظ فاتورة مع تحديث الكميات والأرصدة. type = بيع أو شراء */
    public int saveInvoice(String type, int customerId, List<InvoiceItem> items,
                           double total, double paid) throws SQLException {
        var conn = Database.getConnection();
        conn.setAutoCommit(false);
        try {
            String no = ("بيع".equals(type) ? "INV-" : "PUR-") + System.currentTimeMillis();
            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO invoices(invoice_no,customer_id,user,total,paid,type) VALUES(?,?,?,?,?,?,?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, no);
            if (customerId == 0) ps.setNull(2, java.sql.Types.INTEGER); else ps.setInt(2, customerId);
            ps.setString(3, "مدير");
            ps.setDouble(4, total);
            ps.setDouble(5, paid);
            ps.setString(6, type);
            ps.executeUpdate();
            ResultSet keys = ps.getGeneratedKeys();
            keys.next();
            int invoiceId = keys.getInt(1);

            PreparedStatement pi = conn.prepareStatement(
                    "INSERT INTO invoice_items(invoice_id,product_id,product_name,quantity,price) VALUES(?,?,?,?,?)");
            for (InvoiceItem it : items) {
                pi.setInt(1, invoiceId);
                pi.setInt(2, it.productId);
                pi.setString(3, it.productName);
                pi.setDouble(4, it.quantity);
                pi.setDouble(5, it.price);
                pi.addBatch();
                // البيع ينقص المخزون، الشراء يزيده
                double delta = "بيع".equals(type) ? -it.quantity : it.quantity;
                conn.createStatement().executeUpdate(
                        "UPDATE products SET quantity=quantity+" + delta + " WHERE id=" + it.productId);
            }
            pi.executeBatch();

            double remaining = total - paid;
            if (customerId != 0 && remaining != 0) {
                String table = "بيع".equals(type) ? "customers" : "suppliers";
                conn.createStatement().executeUpdate(
                        "UPDATE " + table + " SET balance=balance+(" + remaining + ") WHERE id=" + customerId);
            }
            conn.commit();
            return invoiceId;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    public List<Map<String, Object>> listInvoices(String type) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT i.*, COALESCE(p.name,'زبون نقدي') AS pname FROM invoices i " +
                "LEFT JOIN customers p ON i.customer_id=p.id WHERE i.type=? ORDER BY i.id DESC LIMIT 200";
        PreparedStatement ps = Database.getConnection().prepareStatement(sql);
        ps.setString(1, type);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", rs.getInt("id"));
            m.put("invoice_no", rs.getString("invoice_no"));
            m.put("date", rs.getString("created_at"));
            m.put("customer", rs.getString("pname"));
            m.put("total", rs.getDouble("total"));
            m.put("paid", rs.getDouble("paid"));
            m.put("rest", rs.getDouble("total") - rs.getDouble("paid"));
            list.add(m);
        }
        return list;
    }

    public List<InvoiceItem> itemsOf(int invoiceId) throws SQLException {
        List<InvoiceItem> list = new ArrayList<>();
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT * FROM invoice_items WHERE invoice_id=?");
        ps.setInt(1, invoiceId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            InvoiceItem it = new InvoiceItem();
            it.productId = rs.getInt("product_id");
            it.productName = rs.getString("product_name");
            it.quantity = rs.getDouble("quantity");
            it.price = rs.getDouble("price");
            list.add(it);
        }
        return list;
    }

    /* ---------- تقارير ---------- */
    public double sumSalesBetween(LocalDate from, LocalDate to) throws SQLException {
        PreparedStatement ps = Database.getConnection().prepareStatement(
                "SELECT COALESCE(SUM(total),0) FROM invoices WHERE type='بيع' AND date(created_at) BETWEEN ? AND ?");
        ps.setString(1, from.toString());
        ps.setString(2, to.toString());
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getDouble(1) : 0;
    }

    public long countInvoicesToday() throws SQLException {
        ResultSet rs = Database.getConnection().createStatement()
                .executeQuery("SELECT COUNT(*) FROM invoices WHERE type='بيع' AND date(created_at)=date('now','localtime')");
        return rs.next() ? rs.getLong(1) : 0;
    }

    public double todaySales() throws SQLException {
        ResultSet rs = Database.getConnection().createStatement()
                .executeQuery("SELECT COALESCE(SUM(total),0) FROM invoices WHERE type='بيع' AND date(created_at)=date('now','localtime')");
        return rs.next() ? rs.getDouble(1) : 0;
    }

    public double inventoryValue() throws SQLException {
        ResultSet rs = Database.getConnection().createStatement()
                .executeQuery("SELECT COALESCE(SUM(quantity*sale_price),0) FROM products");
        return rs.next() ? rs.getDouble(1) : 0;
    }

    public List<Product> lowStockProducts() throws SQLException {
        List<Product> list = new ArrayList<>();
        ResultSet rs = Database.getConnection().createStatement()
                .executeQuery("SELECT p.*, c.name AS cat FROM products p LEFT JOIN categories c ON p.category_id=c.id " +
                        "WHERE p.quantity<=p.min_quantity ORDER BY p.quantity");
        ProductDAO pd = new ProductDAO();
        while (rs.next()) list.add(pd.map(rs));
        return list;
    }

    public List<String[]> topProducts(int limit) throws SQLException {
        List<String[]> list = new ArrayList<>();
        ResultSet rs = Database.getConnection().createStatement().executeQuery(
                "SELECT product_name, SUM(quantity) AS q, SUM(quantity*price) AS t FROM invoice_items " +
                        "GROUP BY product_name ORDER BY t DESC LIMIT " + limit);
        while (rs.next()) {
            list.add(new String[]{rs.getString(1), String.valueOf(rs.getDouble(2)), String.valueOf(rs.getDouble(3))});
        }
        return list;
    }
}
