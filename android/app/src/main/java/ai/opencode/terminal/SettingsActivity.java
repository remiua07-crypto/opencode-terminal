package ai.opencode.terminal;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;

public class SettingsActivity extends Activity {

    private EditText urlField;
    private EditText pairField;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        int pad = dp(18);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF0A0A0A);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(pad, pad, pad, pad);
        scroll.addView(box, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("OpenCode Terminal");
        title.setTextColor(0xFFFAB283);
        title.setTextSize(18);
        title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        box.addView(title);

        TextView sub = new TextView(this);
        sub.setText("v1.0.0 · giao diện terminal cho OpenCode");
        sub.setTextColor(0xFF5C5C5C);
        sub.setTextSize(11);
        sub.setTypeface(Typeface.MONOSPACE);
        add(box, sub, 2);

        TextView label1 = label("Server URL");
        add(box, label1, 22);

        urlField = new EditText(this);
        urlField.setText(Prefs.url(this));
        urlField.setSingleLine(true);
        urlField.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        urlField.setTextColor(0xFFEEEEEE);
        urlField.setTextSize(13);
        urlField.setTypeface(Typeface.MONOSPACE);
        urlField.setBackgroundColor(0xFF141414);
        urlField.setPadding(dp(10), dp(8), dp(10), dp(8));
        add(box, urlField, 6);

        TextView hint1 = new TextView(this);
        hint1.setText("Mặc định http://127.0.0.1:49374 — server chạy trong Termux trên chính điện thoại. Có thể nhập VPS/PC.");
        hint1.setTextColor(0xFF6A6A6A);
        hint1.setTextSize(11);
        hint1.setTypeface(Typeface.MONOSPACE);
        add(box, hint1, 6);

        TextView label2 = label("Link pair (mở một lần)");
        add(box, label2, 20);

        pairField = new EditText(this);
        pairField.setHint("http://127.0.0.1:49374/auth/connect/…");
        pairField.setHintTextColor(0xFF4A4A4A);
        pairField.setSingleLine(true);
        pairField.setTextColor(0xFFEEEEEE);
        pairField.setTextSize(12);
        pairField.setTypeface(Typeface.MONOSPACE);
        pairField.setBackgroundColor(0xFF141414);
        pairField.setPadding(dp(10), dp(8), dp(10), dp(8));
        add(box, pairField, 6);

        TextView hint2 = new TextView(this);
        hint2.setText("Chạy `opencode pair` trong Termux rồi dán link vào đây để đăng nhập.");
        hint2.setTextColor(0xFF6A6A6A);
        hint2.setTextSize(11);
        hint2.setTypeface(Typeface.MONOSPACE);
        add(box, hint2, 6);

        Button pairBtn = button("Mở link pair");
        add(box, pairBtn, 10);
        pairBtn.setOnClickListener(v -> openPair());

        Switch skin = new Switch(this);
        skin.setText("Giao diện terminal");
        skin.setTextColor(0xFFEEEEEE);
        skin.setTextSize(13);
        skin.setTypeface(Typeface.MONOSPACE);
        skin.setChecked(Prefs.skin(this));
        add(box, skin, 22);

        Button save = button("Lưu");
        add(box, save, 22);
        save.setOnClickListener(v -> save(skin.isChecked()));

        Button reset = button("Đặt lại mặc định");
        add(box, reset, 8);
        reset.setOnClickListener(v -> {
            urlField.setText("http://127.0.0.1:49374");
            skin.setChecked(true);
            Toast.makeText(this, "Đã đặt lại — bấm Lưu", Toast.LENGTH_SHORT).show();
        });

        Button logout = button("Xoá đăng nhập / dữ liệu trang");
        add(box, logout, 8);
        logout.setOnClickListener(v -> {
            CookieManager.getInstance().removeAllCookies(null);
            WebStorage.getInstance().deleteAllData();
            Toast.makeText(this, "Đã xoá cookie và dữ liệu trang", Toast.LENGTH_SHORT).show();
        });

        TextView foot = new TextView(this);
        foot.setText("\nServer trên điện thoại (Termux):\n"
                + "  pkg install glibc-repo glibc curl tar\n"
                + "  bash ~/install-opencode-termux.sh\n"
                + "  opencode serve --hostname 127.0.0.1 --port 49374\n"
                + "  opencode pair\n");
        foot.setTextColor(0xFF6A6A6A);
        foot.setTextSize(11);
        foot.setTypeface(Typeface.MONOSPACE);
        add(box, foot, 26);

        setContentView(scroll);
    }

    private void save(boolean skinOn) {
        String raw = urlField.getText().toString().trim();
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            Toast.makeText(this, "URL phải bắt đầu bằng http:// hoặc https://", Toast.LENGTH_LONG).show();
            return;
        }
        while (raw.endsWith("/")) raw = raw.substring(0, raw.length() - 1);
        Prefs.setUrl(this, raw);
        Prefs.setSkin(this, skinOn);
        Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void openPair() {
        String link = pairField.getText().toString().trim();
        if (link.isEmpty()) {
            Toast.makeText(this, "Dán link pair từ `opencode pair` trước đã", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            URI u = URI.create(link);
            String origin = u.getScheme() + "://" + u.getAuthority();
            Prefs.setUrl(this, origin);
            Prefs.setPendingUrl(this, link);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Link không hợp lệ", Toast.LENGTH_LONG).show();
        }
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(0xFF808080);
        v.setTextSize(12);
        v.setTypeface(Typeface.MONOSPACE);
        return v;
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(0xFFFAB283);
        b.setTextSize(13);
        b.setTypeface(Typeface.MONOSPACE);
        b.setBackgroundColor(0xFF1E1E1E);
        b.setPadding(dp(12), dp(8), dp(12), dp(8));
        return b;
    }

    private void add(LinearLayout box, android.view.View v, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        box.addView(v, lp);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
