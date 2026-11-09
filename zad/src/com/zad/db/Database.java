package com.zad.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * إدارة الاتصال بقاعدة بيانات SQLite وتهيئة الجداول.
 */
public class Database {

    private static Connection connection;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection("jdbc:sqlite:zad.db");
                initTables();
            } catch (SQLException e) {
                throw new RuntimeException("تعذر الاتصال بقاعدة البيانات: " + e.getMessage(), e);
            }
        }
        return connection;
    }

    private static void initTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS categories (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "barcode TEXT UNIQUE," +
                    "category_id INTEGER," +
                    "unit TEXT DEFAULT 'حبة'," +
                    "purchase_price REAL DEFAULT 0," +
                    "sale_price REAL NOT NULL," +
                    "quantity REAL NOT NULL DEFAULT 0," +
                    "min_quantity REAL DEFAULT 5," +
                    "FOREIGN KEY(category_id) REFERENCES categories(id))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS customers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "phone TEXT," +
                    "address TEXT," +
                    "balance REAL DEFAULT 0)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS suppliers (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL," +
                    "phone TEXT," +
                    "address TEXT," +
                    "balance REAL DEFAULT 0)");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS invoices (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "invoice_no TEXT," +
                    "customer_id INTEGER," +
                    "user TEXT DEFAULT 'مدير'," +
                    "total REAL NOT NULL," +
                    "paid REAL DEFAULT 0," +
                    "type TEXT DEFAULT 'بيع'," +
                    "created_at TEXT DEFAULT (datetime('now','localtime')))");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS invoice_items (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "invoice_id INTEGER NOT NULL," +
                    "product_id INTEGER," +
                    "product_name TEXT," +
                    "quantity REAL NOT NULL," +
                    "price REAL NOT NULL," +
                    "FOREIGN KEY(invoice_id) REFERENCES invoices(id))");

            // تصنيفات افتراضية عند أول تشغيل
            var rs = st.executeQuery("SELECT COUNT(*) FROM categories");
            if (rs.next() && rs.getInt(1) == 0) {
                st.executeUpdate("INSERT INTO categories(name) VALUES ('مواد غذائية أساسية')");
                st.executeUpdate("INSERT INTO categories(name) VALUES ('مشروبات')");
                st.executeUpdate("INSERT INTO categories(name) VALUES ('منظفات')");
                st.executeUpdate("INSERT INTO categories(name) VALUES ('معلبات')");
                st.executeUpdate("INSERT INTO categories(name) VALUES ('أخرى')");
            }
        }
    }
}
