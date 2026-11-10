package com.zad.util;

import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/** أدوات مساعدة عامة */
public class UI {

    public static void info(String msg) {
        show(Alert.AlertType.INFORMATION, msg);
    }

    public static void error(String msg) {
        show(Alert.AlertType.ERROR, msg);
    }

    private static void show(Alert.AlertType type, String msg) {
        Alert a = new Alert(type);
        a.setTitle("تطبيق زاد");
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg);
        a.setTitle("تأكيد");
        a.setHeaderText(null);
        return a.showAndWait().filter(b -> b == javafx.scene.control.ButtonType.OK).isPresent();
    }

    public static String input(String title, String label) {
        TextInputDialog d = new TextInputDialog();
        d.setTitle(title);
        d.setHeaderText(null);
        d.setContentText(label);
        Optional<String> r = d.showAndWait();
        return r.orElse(null);
    }

    public static double parseD(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (Exception e) { return def; }
    }

    public static int parseI(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    public static String now() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }
}
