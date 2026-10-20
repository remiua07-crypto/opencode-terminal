#!/usr/bin/env bun
/**
 * Proxy đứng trước OpenCode web UI, chèn skin terminal vào HTML.
 *
 *   bun server.mjs                 # http://127.0.0.1:49375  ->  :49374
 *   OC_SKIN_PORT=49375 bun server.mjs
 *
 * Không sửa file cài đặt của opencode. Tắt là về giao diện gốc.
 */
const TARGET = process.env.OC_TARGET || "http://127.0.0.1:49374";
const PORT = Number(process.env.OC_SKIN_PORT || 49375);
const HOST = process.env.OC_SKIN_HOST || "127.0.0.1";
const HERE = import.meta.dir;
const ORIGIN = `http://${HOST}:${PORT}`;
const b64 = (s) => btoa(s).replace(/=+$/, "");

const file = (name, type) =>
  new Response(Bun.file(`${HERE}/${name}`), {
    headers: { "content-type": type, "cache-control": "no-store" },
  });

const server = Bun.serve({
  hostname: HOST,
  port: PORT,
  idleTimeout: 0,
  async fetch(req) {
    const url = new URL(req.url);

    if (url.pathname === "/__tui/skin.css") return file("tui.css", "text/css; charset=utf-8");
    if (url.pathname === "/__tui/skin.js") return file("tui.js", "text/javascript; charset=utf-8");
    if (url.pathname === "/__tui/health") return new Response("ok");

    // /server/<base64 của URL server>/... — app mã hoá server gốc, đổi sang proxy
    const m = url.pathname.match(/^\/server\/([^/]+)(\/.*)?$/);
    if (m) {
      let decoded = "";
      try {
        decoded = atob(m[1].replace(/-/g, "+").replace(/_/g, "/"));
      } catch {}
      if (decoded && decoded.replace(/\/$/, "") === TARGET) {
        return Response.redirect(`${ORIGIN}/server/${b64(ORIGIN)}${m[2] || ""}${url.search}`, 302);
      }
    }

    const headers = new Headers(req.headers);
    headers.set("host", new URL(TARGET).host);
    headers.set("origin", TARGET);
    headers.set("referer", TARGET + "/");
    headers.delete("accept-encoding"); // tránh nén để chèn HTML

    let upstream;
    try {
      upstream = await fetch(TARGET + url.pathname + url.search, {
        method: req.method,
        headers,
        body: req.method === "GET" || req.method === "HEAD" ? undefined : req.body,
        redirect: "manual",
      });
    } catch (err) {
      return new Response(
        `OpenCode server không phản hồi tại ${TARGET}\n\n${err?.message || err}\n`,
        { status: 502, headers: { "content-type": "text/plain; charset=utf-8" } },
      );
    }

    const out = new Headers(upstream.headers);
    out.delete("content-length");
    out.delete("content-encoding");

    const loc = out.get("location");
    if (loc) out.set("location", loc.replace(TARGET, `http://${HOST}:${PORT}`));

    const type = out.get("content-type") || "";
    if (req.method === "GET" && type.includes("text/html")) {
      let html = await upstream.text();
      const tag =
        '<link rel="stylesheet" href="/__tui/skin.css">' +
        '<script src="/__tui/skin.js" defer></script>';
      html = html.includes("</head>")
        ? html.replace("</head>", `${tag}</head>`)
        : tag + html;
      return new Response(html, { status: upstream.status, headers: out });
    }

    // app tự báo server URL của chính nó -> trả về origin của proxy
    const rewriteJson = ["/api/info", "/api/location", "/api/project", "/api/session/active"];
    if (req.method === "GET" && type.includes("application/json") && rewriteJson.includes(url.pathname)) {
      const text = await upstream.text();
      if (text.includes(TARGET)) {
        return new Response(text.split(TARGET).join(ORIGIN), {
          status: upstream.status,
          headers: out,
        });
      }
      return new Response(text, { status: upstream.status, headers: out });
    }

    return new Response(upstream.body, { status: upstream.status, headers: out });
  },
});

console.log(`\n  opencode terminal skin\n  → http://${HOST}:${server.port}\n  → ${TARGET}\n`);
