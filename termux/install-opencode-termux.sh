#!/data/data/com.termux/files/usr/bin/bash
# Cài OpenCode server lên Termux (Android aarch64), chạy binary linux-arm64
# của bản phát hành chính thức qua glibc loader — không cần proot/chroot.
set -euo pipefail

if [ -z "${TERMUX_VERSION:-}" ]; then
  echo "Script này chỉ chạy trong Termux (cài từ F-Droid, không dùng bản Play Store)."
  exit 1
fi

if [ "$(uname -m)" != "aarch64" ]; then
  echo "Chỉ hỗ trợ Android 64-bit (aarch64). Máy bạn: $(uname -m)"
  exit 1
fi

echo "== [1/5] Cài gói phụ thuộc (glibc, curl, tar) =="
pkg install -y glibc-repo >/dev/null 2>&1 || true
pkg install -y glibc curl tar

LOADER="$PREFIX/glibc/lib/ld-linux-aarch64.so.1"
if [ ! -f "$LOADER" ]; then
  echo "Không thấy glibc loader tại $LOADER"
  echo "Thử: pkg install glibc rồi chạy lại script."
  exit 1
fi

echo "== [2/5] Lấy phiên bản mới nhất =="
META="$(curl -fsSL https://opencode.ai/update/api/latest/cli/npm)"
VERSION="$(printf '%s' "$META" | sed -n 's/.*"version":"\([^"]*\)".*/\1/p')"
if [ -z "$VERSION" ]; then
  echo "Không đọc được phiên bản từ opencode.ai"
  exit 1
fi
echo "   phiên bản: $VERSION"

echo "== [3/5] Tải opencode linux-arm64 (~60MB nén) =="
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
URL="https://registry.npmjs.org/@opencode/cli-linux-arm64/-/cli-linux-arm64-${VERSION}.tgz"
curl -fL --progress-bar "$URL" -o "$TMP/opencode.tgz"
tar -xzf "$TMP/opencode.tgz" -C "$TMP"

BIN="$(find "$TMP" -type f -name opencode -perm -u+x | head -n 1)"
if [ -z "$BIN" ]; then
  echo "Không tìm thấy binary trong gói tải về."
  exit 1
fi

DEST="$HOME/.opencode/bin"
mkdir -p "$DEST"
install -m 0755 "$BIN" "$DEST/opencode"

echo "== [4/5] Tạo lệnh 'opencode' =="
cat > "$PREFIX/bin/opencode" <<EOF
#!$PREFIX/bin/sh
unset LD_PRELOAD LD_LIBRARY_PATH
exec "$LOADER" --library-path "$PREFIX/glibc/lib:$DEST" "$DEST/opencode" "\$@"
EOF
chmod +x "$PREFIX/bin/opencode"

echo "== [5/5] Kiểm tra =="
"$PREFIX/bin/opencode" --version || {
  echo "Không chạy được binary. Thử: pkg install glibc && chạy lại."
  exit 1
}

# Tự chạy server khi khởi động máy (cần Termux:Boot)
mkdir -p "$HOME/.termux/boot"
cat > "$HOME/.termux/boot/opencode-server.sh" <<'EOF'
#!/data/data/com.termux/files/usr/bin/sh
termux-wake-lock
opencode serve --hostname 127.0.0.1 --port 49374 >> "$HOME/opencode-server.log" 2>&1 &
EOF
chmod +x "$HOME/.termux/boot/opencode-server.sh"

cat <<'MSG'

✅ Xong.

Chạy server:
  opencode serve --hostname 127.0.0.1 --port 49374

Lấy link đăng nhập (mở terminal khác):
  opencode pair --url http://127.0.0.1:49374

Rồi mở app OpenCode Terminal → ⚙ → dán link pair → Lưu.

Lưu ý:
  • Cài Termux:Boot (F-Droid) nếu muốn server tự chạy khi mở máy.
  • Cấu hình API key như bản desktop: ANTHROPIC_API_KEY, OPENAI_API_KEY, …
MSG
