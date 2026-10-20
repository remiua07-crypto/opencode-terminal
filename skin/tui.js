/* OpenCode web UI — terminal skin: phần bổ sung DOM (status bar, ASCII logo, số tab) */
(() => {
  const WORDMARK = [
    "█▀▀█ █▀▀█ █▀▀█ █▀▀▄ █▀▀▀ █▀▀█ █▀▀█ █▀▀█",
    "█  █ █  █ █▀▀▀ █  █ █    █  █ █  █ █▀▀▀",
    "▀▀▀▀ █▀▀▀ ▀▀▀▀ ▀▀▀▀ ▀▀▀▀ ▀▀▀▀ ▀▀▀▀ ▀▀▀▀",
  ].join("\n");

  let version = "2.1.0";
  let mcp = null;

  const onHome = () => !location.pathname.includes("/session/");

  function ensureStatusBar() {
    let bar = document.getElementById("tui-statusbar");
    if (!bar) {
      bar = document.createElement("div");
      bar.id = "tui-statusbar";
      bar.innerHTML =
        '<span class="tui-dot">⊙</span><span id="tui-status-mcp"><b>opencode</b></span>' +
        '<span class="tui-spacer"></span>' +
        '<span class="tui-meter">■■■■■■■■</span>' +
        '<span id="tui-status-version"></span>';
      document.body.appendChild(bar);
    }
    const m = document.getElementById("tui-status-mcp");
    if (m) {
      m.innerHTML =
        mcp === null
          ? "<b>opencode</b>"
          : "<b>" + mcp + "</b> MCP  <span style=\"color:#5c5c5c\">/mcps</span>";
    }
    const v = document.getElementById("tui-status-version");
    if (v) v.textContent = "v" + version;
  }

  function ensureWordmark() {
    const existing = document.getElementById("tui-wordmark");
    if (!onHome()) {
      if (existing) {
        existing.remove();
        document.getElementById("tui-wordmark-caption")?.remove();
      }
      return;
    }
    if (existing) return;

    const section = document.querySelector("main section.flex-1.flex.flex-col");
    if (!section) return;

    const pre = document.createElement("pre");
    pre.id = "tui-wordmark";
    pre.textContent = WORDMARK;

    const cap = document.createElement("div");
    cap.id = "tui-wordmark-caption";
    cap.textContent = "v" + version + " · terminal skin";

    section.insertBefore(cap, section.firstChild);
    section.insertBefore(pre, cap);
  }

  function numberTabs() {
    document.querySelectorAll('[data-slot="tab-title"]').forEach((el, i) => {
      el.dataset.tuiIndex = String(i + 1);
    });
  }

  function apply() {
    // ép chế độ tối để các rule light-theme của app không ghi đè
    if (document.documentElement.dataset.colorScheme !== "dark") {
      document.documentElement.dataset.colorScheme = "dark";
    }
    ensureStatusBar();
    ensureWordmark();
    numberTabs();
  }

  // Debounce observer để không chạy liên tục khi stream token
  let t = null;
  const schedule = () => {
    clearTimeout(t);
    t = setTimeout(apply, 120);
  };

  new MutationObserver(schedule).observe(document.documentElement, {
    childList: true,
    subtree: true,
  });
  window.addEventListener("popstate", schedule);
  window.addEventListener("resize", schedule);

  // Lấy metadata một lần
  fetch("/api/info")
    .then((r) => (r.ok ? r.json() : null))
    .then((j) => {
      if (j && typeof j.version === "string") version = j.version;
      schedule();
    })
    .catch(() => {});

  fetch("/api/mcp")
    .then((r) => (r.ok ? r.json() : null))
    .then((j) => {
      if (Array.isArray(j)) mcp = j.length;
      else if (Array.isArray(j?.data)) mcp = j.data.length;
      schedule();
    })
    .catch(() => {});

  document.documentElement.dataset.tuiSkin = "1";
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", apply, { once: true });
  } else {
    apply();
  }
})();
