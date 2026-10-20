package ai.opencode.terminal;

import android.content.Context;
import android.content.SharedPreferences;

import java.net.URI;

/** Lưu cấu hình nhỏ gọn: URL server, bật/tắt skin, URL chờ mở. */
public final class Prefs {
    private static final String FILE = "opencode";
    private static final String DEFAULT_URL = "http://127.0.0.1:49374";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public static String url(Context c) {
        String v = sp(c).getString("url", DEFAULT_URL);
        if (v == null || v.trim().isEmpty()) v = DEFAULT_URL;
        return v.trim();
    }

    public static void setUrl(Context c, String url) {
        sp(c).edit().putString("url", url).apply();
    }

    public static boolean skin(Context c) {
        return sp(c).getBoolean("skin", true);
    }

    public static void setSkin(Context c, boolean on) {
        sp(c).edit().putBoolean("skin", on).apply();
    }

    public static String pendingUrl(Context c) {
        return sp(c).getString("pending", null);
    }

    public static void setPendingUrl(Context c, String url) {
        sp(c).edit().putString("pending", url).apply();
    }

    public static void clearPendingUrl(Context c) {
        sp(c).edit().remove("pending").apply();
    }

    public static String host(Context c) {
        try {
            String h = URI.create(url(c)).getHost();
            return h == null ? "" : h;
        } catch (Exception e) {
            return "";
        }
    }
}
