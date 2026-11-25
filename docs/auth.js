/* DevAI shared account widget: one nickname and password for every site. */
(() => {
  if (window.__DEVAI_AUTH_LOADED__) return;
  window.__DEVAI_AUTH_LOADED__ = true;

  const script = document.currentScript;
  const API_BASE = (window.DEVAI_AUTH_API || "https://work-2-nizvyfgvcqicrkif.prod-runtime.all-hands.dev/v1").replace(/\/+$/, "");
  const TOKEN_KEY = "devai.account.token.v1";
  const SITES = {
    "devai-studio": { name: "DevAI Studio", url: "https://karishka-sr.github.io/devai-studio/" },
    calculators: { name: "Calculators", url: "https://karishka-sr.github.io/Calculators/" },
    gramhub: { name: "GramHub", url: "https://karishka-sr.github.io/gramhub/" },
    "telegram-sender": { name: "Telegram Sender", url: "https://karishka-sr.github.io/telegram-sender/" },
  };

  function inferSite() {
    if (window.DEVAI_SITE_ID) return String(window.DEVAI_SITE_ID).toLowerCase();
    if (script?.dataset?.site) return script.dataset.site.toLowerCase();
    const host = location.hostname.toLowerCase();
    if (host.endsWith(".github.io")) {
      const first = location.pathname.split("/").filter(Boolean)[0];
      return (first || "devai-studio").toLowerCase();
    }
    return "devai-studio";
  }

  const site = inferSite();
  const state = { token: readStorage(), account: null, nickAvailable: null, busy: false };

  function readStorage() {
    try {
      return localStorage.getItem(TOKEN_KEY) || "";
    } catch {
      return "";
    }
  }

  function writeStorage(token) {
    try {
      if (token) localStorage.setItem(TOKEN_KEY, token);
      else localStorage.removeItem(TOKEN_KEY);
    } catch {
      // Private mode can block storage; the current tab still works.
    }
  }

  function esc(value) {
    return String(value ?? "").replace(/[&<>"']/g, (char) => ({
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;",
    }[char]));
  }

  async function request(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (options.body) headers["Content-Type"] = "application/json";
    if (state.token) headers.Authorization = "Bearer " + state.token;
    const response = await fetch(API_BASE + path, { ...options, headers });
    let data = null;
    try {
      data = await response.json();
    } catch {
      data = null;
    }
    if (!response.ok) {
      const error = new Error(data?.error?.message || `Сервер ответил ${response.status}`);
      error.status = response.status;
      throw error;
    }
    return data;
  }

  function saveSession(data) {
    state.token = data.token;
    state.account = data.account;
    writeStorage(data.token);
    renderChip();
    renderPanel();
  }

  function clearSession() {
    state.token = "";
    state.account = null;
    writeStorage("");
    renderChip();
    renderPanel();
  }

  function passwordScore(password) {
    if (password.length < 8) return 0;
    let score = 1;
    if (password.length >= 12) score += 1;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 1;
    if (/\d/.test(password)) score += 1;
    if (/[^A-Za-zА-Яа-яЁё0-9]/.test(password)) score += 1;
    return Math.min(4, score);
  }

  const strengthLabels = ["Лёгкий", "Лёгкий", "Средний", "Сильный", "Максимальный"];

  function setStrength(input, meter, label, value) {
    const score = passwordScore(value);
    meter.dataset.score = String(score);
    meter.querySelectorAll(".dva-seg").forEach((segment, index) => {
      segment.classList.toggle("is-on", index < score);
    });
    label.textContent = strengthLabels[score];
    label.dataset.score = String(score);
    if (value) {
      meter.classList.remove("dva-pulse");
      void meter.offsetWidth;
      meter.classList.add("dva-pulse");
    }
  }

  function bindStrength(input, meter, label) {
    const update = () => setStrength(input, meter, label, input.value);
    input.addEventListener("input", update);
    update();
  }

  function setStatus(element, text, kind = "") {
    if (!element) return;
    element.textContent = text;
    element.dataset.kind = kind;
  }

  function toast(message, kind = "ok") {
    const node = document.createElement("div");
    node.className = "dva-toast";
    node.dataset.kind = kind;
    node.textContent = message;
    document.body.appendChild(node);
    requestAnimationFrame(() => node.classList.add("is-visible"));
    setTimeout(() => {
      node.classList.remove("is-visible");
      setTimeout(() => node.remove(), 250);
    }, 4200);
  }

  function injectStyles() {
    if (document.getElementById("devai-auth-styles")) return;
    const style = document.createElement("style");
    style.id = "devai-auth-styles";
    style.textContent = `
      .dva-shell{position:fixed;right:18px;bottom:18px;z-index:2147483000;font-family:Inter,ui-sans-serif,system-ui,-apple-system,"Segoe UI",sans-serif}
      .dva-chip{display:flex;align-items:center;gap:9px;min-height:44px;padding:0 16px;border:1px solid rgba(255,255,255,.16);border-radius:999px;background:rgba(12,15,18,.92);color:#f5f7f4;font:600 13px/1 inherit;letter-spacing:.02em;box-shadow:0 12px 40px rgba(0,0,0,.32);backdrop-filter:blur(14px);cursor:pointer;transition:transform .18s ease,border-color .18s ease,background .18s ease}
      .dva-chip:hover{transform:translateY(-2px);border-color:rgba(177,255,74,.5);background:rgba(18,23,26,.97)}
      .dva-chip-dot{width:8px;height:8px;border-radius:50%;background:#b1ff4a;box-shadow:0 0 12px rgba(177,255,74,.8)}
      .dva-chip[data-auth="guest"] .dva-chip-dot{background:#8b949e;box-shadow:none}
      .dva-backdrop{position:fixed;inset:0;z-index:2147483001;display:grid;place-items:center;padding:18px;background:rgba(0,0,0,.64);backdrop-filter:blur(12px);animation:dva-fade .22s ease both}
      .dva-backdrop[hidden]{display:none}
      .dva-modal{width:min(440px,100%);max-height:min(720px,calc(100vh - 36px));overflow:auto;border:1px solid rgba(255,255,255,.14);border-radius:22px;background:linear-gradient(160deg,#14191c,#0b0e10 70%);color:#eef2ea;box-shadow:0 28px 90px rgba(0,0,0,.6);animation:dva-pop .28s cubic-bezier(.2,.8,.2,1) both}
      .dva-top{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;padding:22px 22px 0}
      .dva-kicker{margin:0 0 6px;color:#b1ff4a;font:700 10px/1.4 inherit;letter-spacing:.18em;text-transform:uppercase}
      .dva-title{margin:0;font:700 23px/1.15 inherit;letter-spacing:-.03em}
      .dva-sub{margin:8px 0 0;color:#9ba6a0;font:400 12px/1.55 inherit}
      .dva-close{flex:0 0 auto;width:34px;height:34px;border:1px solid rgba(255,255,255,.12);border-radius:50%;background:transparent;color:#c8d1cb;font:400 20px/1 inherit;cursor:pointer}
      .dva-close:hover{color:#fff;border-color:rgba(255,255,255,.3)}
      .dva-body{padding:20px 22px 24px}
      .dva-tabs{display:grid;grid-template-columns:1fr 1fr;gap:5px;padding:4px;margin:0 0 18px;border:1px solid rgba(255,255,255,.09);border-radius:12px;background:rgba(255,255,255,.03)}
      .dva-tab{min-height:36px;border:0;border-radius:9px;background:transparent;color:#93a09a;font:600 12px/1 inherit;cursor:pointer;transition:background .18s,color .18s}
      .dva-tab[aria-selected="true"]{background:#eef2ea;color:#0b0e10}
      .dva-field{display:grid;gap:7px;margin:0 0 14px}
      .dva-label{color:#c8d1cb;font:600 11px/1.3 inherit;letter-spacing:.02em}
      .dva-input{width:100%;box-sizing:border-box;min-height:46px;padding:0 13px;border:1px solid rgba(255,255,255,.13);border-radius:11px;background:#0b0e10;color:#f5f7f4;font:500 14px/1 inherit;outline:none;transition:border-color .18s,box-shadow .18s}
      .dva-input:focus{border-color:rgba(177,255,74,.62);box-shadow:0 0 0 3px rgba(177,255,74,.12)}
      .dva-input:disabled{opacity:.6}
      .dva-row{display:flex;align-items:center;justify-content:space-between;gap:12px}
      .dva-hint{min-height:16px;margin:0;color:#85918b;font:400 11px/1.45 inherit}
      .dva-hint[data-kind="ok"]{color:#9be37a}
      .dva-hint[data-kind="bad"]{color:#ff8a8a}
      .dva-password-wrap{position:relative}
      .dva-password-wrap .dva-input{padding-right:76px}
      .dva-reveal{position:absolute;right:8px;top:50%;transform:translateY(-50%);min-height:30px;padding:0 10px;border:0;border-radius:7px;background:rgba(255,255,255,.06);color:#b8c2bc;font:600 10px/1 inherit;cursor:pointer}
      .dva-strength{display:grid;gap:7px;margin:9px 0 0}
      .dva-strength-track{display:grid;grid-template-columns:repeat(4,1fr);gap:4px}
      .dva-seg{height:6px;border-radius:99px;background:rgba(255,255,255,.1);transition:background .3s ease,transform .3s ease}
      .dva-strength[data-score="1"] .dva-seg.is-on{background:#ff6b6b}
      .dva-strength[data-score="2"] .dva-seg.is-on{background:#ffb84d}
      .dva-strength[data-score="3"] .dva-seg.is-on{background:#b1ff4a}
      .dva-strength[data-score="4"] .dva-seg.is-on{background:#65f5c8;box-shadow:0 0 12px rgba(101,245,200,.5)}
      .dva-pulse .dva-seg.is-on{animation:dva-seg-pulse .36s ease both}
      .dva-strength-label{font:600 11px/1.3 inherit;color:#9ba6a0}
      .dva-strength-label[data-score="1"]{color:#ff8a8a}
      .dva-strength-label[data-score="2"]{color:#ffc76b}
      .dva-strength-label[data-score="3"]{color:#b1ff4a}
      .dva-strength-label[data-score="4"]{color:#65f5c8}
      .dva-btn{width:100%;min-height:46px;border:0;border-radius:11px;background:#b1ff4a;color:#0b0e10;font:800 12px/1 inherit;letter-spacing:.02em;cursor:pointer;transition:transform .18s,filter .18s}
      .dva-btn:hover{transform:translateY(-1px);filter:brightness(1.06)}
      .dva-btn:disabled{opacity:.6;cursor:wait;transform:none}
      .dva-legal{margin:12px 0 0;color:#7f8b84;font:400 10px/1.5 inherit;text-align:center}
      .dva-profile-head{display:flex;align-items:center;gap:13px;margin:0 0 18px}
      .dva-avatar{display:grid;place-items:center;width:50px;height:50px;flex:0 0 auto;border:1px solid rgba(177,255,74,.4);border-radius:15px;background:rgba(177,255,74,.1);color:#b1ff4a;font:800 21px/1 inherit}
      .dva-profile-name{margin:0;font:700 19px/1.1 inherit}
      .dva-role{display:inline-flex;margin-top:5px;padding:4px 7px;border-radius:999px;background:rgba(255,255,255,.07);color:#b8c2bc;font:700 9px/1 inherit;letter-spacing:.12em;text-transform:uppercase}
      .dva-role[data-role="owner"]{background:rgba(177,255,74,.14);color:#b1ff4a}
      .dva-note{margin:0 0 16px;padding:11px 12px;border:1px solid rgba(255,255,255,.08);border-radius:10px;background:rgba(255,255,255,.03);color:#9ba6a0;font:400 11px/1.5 inherit}
      .dva-sites{display:grid;gap:7px;margin:0 0 18px}
      .dva-site{display:flex;align-items:center;justify-content:space-between;gap:12px;padding:10px 11px;border:1px solid rgba(255,255,255,.08);border-radius:9px;color:#dfe6df;text-decoration:none;font:600 11px/1.3 inherit;transition:border-color .18s,background .18s}
      .dva-site:hover{border-color:rgba(177,255,74,.35);background:rgba(177,255,74,.05)}
      .dva-site small{color:#7f8b84;font:400 10px/1 inherit}
      .dva-logout{width:100%;min-height:40px;border:1px solid rgba(255,255,255,.12);border-radius:10px;background:transparent;color:#c8d1cb;font:600 11px/1 inherit;cursor:pointer}
      .dva-logout:hover{border-color:rgba(255,120,120,.45);color:#ff9a9a}
      .dva-pass{margin:14px 0 0;border-top:1px solid rgba(255,255,255,.08);padding-top:12px}
      .dva-pass summary{color:#9ba6a0;font:600 11px/1.4 inherit;cursor:pointer;list-style:none}
      .dva-pass summary::-webkit-details-marker{display:none}
      .dva-pass summary:before{content:"+ ";color:#b1ff4a}
      .dva-toast{position:fixed;left:50%;bottom:82px;z-index:2147483002;max-width:min(92vw,420px);padding:12px 16px;border:1px solid rgba(177,255,74,.35);border-radius:12px;background:#11171a;color:#eef2ea;font:600 12px/1.45 inherit;text-align:center;opacity:0;transform:translate(-50%,10px);transition:opacity .22s,transform .22s;box-shadow:0 16px 50px rgba(0,0,0,.45)}
      .dva-toast[data-kind="bad"]{border-color:rgba(255,120,120,.45)}
      .dva-toast.is-visible{opacity:1;transform:translate(-50%,0)}
      body.dva-lock{overflow:hidden}
      @keyframes dva-fade{from{opacity:0}to{opacity:1}}
      @keyframes dva-pop{from{opacity:0;transform:translateY(14px) scale(.98)}to{opacity:1;transform:none}}
      @keyframes dva-seg-pulse{0%{transform:scaleY(.6)}55%{transform:scaleY(1.35)}100%{transform:scaleY(1)}}
      @media (max-width:520px){.dva-shell{right:12px;bottom:12px}.dva-chip{min-height:40px;padding:0 13px}.dva-modal{border-radius:17px}.dva-top,.dva-body{padding-left:17px;padding-right:17px}}
      @media (prefers-reduced-motion:reduce){.dva-backdrop,.dva-modal,.dva-seg,.dva-chip,.dva-btn,.dva-toast{animation:none!important;transition:none!important}}
    `;
    document.head.appendChild(style);
  }

  function shell() {
    let node = document.getElementById("devai-auth-shell");
    if (node) return node;
    node = document.createElement("div");
    node.id = "devai-auth-shell";
    node.className = "dva-shell";
    node.innerHTML = `
      <button type="button" class="dva-chip" id="dva-chip" data-auth="guest" aria-haspopup="dialog">
        <span class="dva-chip-dot" aria-hidden="true"></span>
        <span id="dva-chip-text">Войти</span>
      </button>
    `;
    document.body.appendChild(node);
    node.querySelector("#dva-chip").addEventListener("click", openPanel);
    return node;
  }

  function modalMarkup() {
    return `
      <div class="dva-backdrop" id="dva-backdrop" hidden>
        <section class="dva-modal" role="dialog" aria-modal="true" aria-labelledby="dva-modal-title">
          <div class="dva-top">
            <div>
              <p class="dva-kicker">DevAI Account</p>
              <h2 class="dva-title" id="dva-modal-title">Один аккаунт</h2>
              <p class="dva-sub">Для всех сайтов. Ник закрепляется навсегда.</p>
            </div>
            <button type="button" class="dva-close" id="dva-close" aria-label="Закрыть">×</button>
          </div>
          <div class="dva-body" id="dva-panel"></div>
        </section>
      </div>
    `;
  }

  function openPanel() {
    shell();
    let backdrop = document.getElementById("dva-backdrop");
    if (!backdrop) {
      document.body.insertAdjacentHTML("beforeend", modalMarkup());
      backdrop = document.getElementById("dva-backdrop");
      backdrop.addEventListener("click", (event) => {
        if (event.target === backdrop) closePanel();
      });
      document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") closePanel();
      });
    }
    backdrop.hidden = false;
    document.body.classList.add("dva-lock");
    renderPanel();
    setTimeout(() => document.getElementById("dva-nick")?.focus(), 40);
  }

  function closePanel() {
    const backdrop = document.getElementById("dva-backdrop");
    if (backdrop) backdrop.hidden = true;
    document.body.classList.remove("dva-lock");
  }

  function renderChip() {
    const chip = document.getElementById("dva-chip");
    if (!chip) return;
    const text = chip.querySelector("#dva-chip-text");
    if (state.account) {
      chip.dataset.auth = "user";
      text.textContent = state.account.nickname;
      chip.setAttribute("aria-label", "Аккаунт " + state.account.nickname);
    } else {
      chip.dataset.auth = "guest";
      text.textContent = "Войти";
      chip.setAttribute("aria-label", "Войти или зарегистрироваться");
    }
  }

  function siteLinks() {
    const sites = state.account?.sites?.length ? state.account.sites : Object.entries(SITES).map(([id, value]) => ({ id, ...value }));
    return sites.map((item) => `
      <a class="dva-site" href="${esc(item.url)}" target="_blank" rel="noreferrer">
        <span>${esc(item.name || item.id)}</span>
        <small>открыть ↗</small>
      </a>
    `).join("");
  }

  function renderGuest() {
    return `
      <div class="dva-tabs" role="tablist">
        <button type="button" class="dva-tab" data-tab="register" aria-selected="true">Регистрация</button>
        <button type="button" class="dva-tab" data-tab="login" aria-selected="false">Вход</button>
      </div>
      <form id="dva-register" autocomplete="on">
        <div class="dva-field">
          <label class="dva-label" for="dva-nick">Ник</label>
          <input class="dva-input" id="dva-nick" name="nickname" autocomplete="username" minlength="3" maxlength="32" placeholder="например, karishka" required>
          <p class="dva-hint" id="dva-nick-hint">3–32 символа. Потом изменить нельзя.</p>
        </div>
        <div class="dva-field">
          <label class="dva-label" for="dva-pass">Пароль</label>
          <div class="dva-password-wrap">
            <input class="dva-input" id="dva-pass" name="password" type="password" autocomplete="new-password" minlength="8" placeholder="8+ символов" required>
            <button type="button" class="dva-reveal" data-reveal="dva-pass">Показать</button>
          </div>
          <div class="dva-strength" id="dva-strength" data-score="0">
            <div class="dva-strength-track" aria-hidden="true"><span class="dva-seg"></span><span class="dva-seg"></span><span class="dva-seg"></span><span class="dva-seg"></span></div>
            <span class="dva-strength-label" id="dva-strength-label" data-score="0">Лёгкий</span>
          </div>
        </div>
        <p class="dva-hint" id="dva-register-status"></p>
        <button class="dva-btn" type="submit">Создать аккаунт</button>
        <p class="dva-legal">Регистрация сразу на всех сайтах: ${Object.values(SITES).map((item) => esc(item.name)).join(", ")}.</p>
      </form>
      <form id="dva-login" autocomplete="on" hidden>
        <div class="dva-field">
          <label class="dva-label" for="dva-login-nick">Ник</label>
          <input class="dva-input" id="dva-login-nick" name="nickname" autocomplete="username" required>
        </div>
        <div class="dva-field">
          <label class="dva-label" for="dva-login-pass">Пароль</label>
          <div class="dva-password-wrap">
            <input class="dva-input" id="dva-login-pass" name="password" type="password" autocomplete="current-password" required>
            <button type="button" class="dva-reveal" data-reveal="dva-login-pass">Показать</button>
          </div>
        </div>
        <p class="dva-hint" id="dva-login-status"></p>
        <button class="dva-btn" type="submit">Войти</button>
      </form>
    `;
  }

  function renderAccount() {
    const account = state.account;
    if (!account) return renderGuest();
    const role = account.role === "owner" ? "Владелец" : account.role === "admin" ? "Админ" : "Участник";
    const first = account.nickname.slice(0, 1).toUpperCase();
    return `
      <div class="dva-profile-head">
        <div class="dva-avatar">${esc(first)}</div>
        <div>
          <h3 class="dva-profile-name">${esc(account.nickname)}</h3>
          <span class="dva-role" data-role="${esc(account.role)}">${role}</span>
        </div>
      </div>
      <p class="dva-note">Ник закреплён навсегда. Сервер не принимает смену ника. Аккаунт уже активен на всех сайтах.</p>
      <div class="dva-sites">${siteLinks()}</div>
      <details class="dva-pass">
        <summary>Сменить пароль</summary>
        <form id="dva-pass-form">
          <div class="dva-field"><label class="dva-label" for="dva-current-pass">Текущий пароль</label><input class="dva-input" id="dva-current-pass" type="password" autocomplete="current-password" required></div>
          <div class="dva-field"><label class="dva-label" for="dva-new-pass">Новый пароль</label><input class="dva-input" id="dva-new-pass" type="password" autocomplete="new-password" minlength="8" required></div>
          <p class="dva-hint" id="dva-pass-status"></p>
          <button class="dva-btn" type="submit">Сохранить пароль</button>
        </form>
      </details>
      <button type="button" class="dva-logout" id="dva-logout">Выйти</button>
    `;
  }

  function renderPanel() {
    const panel = document.getElementById("dva-panel");
    if (!panel) return;
    panel.innerHTML = state.account ? renderAccount() : renderGuest();
    if (state.account) {
      panel.querySelector("#dva-logout")?.addEventListener("click", logout);
      panel.querySelector("#dva-pass-form")?.addEventListener("submit", changePassword);
      return;
    }

    const tabs = panel.querySelectorAll(".dva-tab");
    tabs.forEach((tab) => tab.addEventListener("click", () => {
      tabs.forEach((other) => other.setAttribute("aria-selected", String(other === tab)));
      panel.querySelector("#dva-register").hidden = tab.dataset.tab !== "register";
      panel.querySelector("#dva-login").hidden = tab.dataset.tab !== "login";
      panel.querySelector(tab.dataset.tab === "register" ? "#dva-nick" : "#dva-login-nick")?.focus();
    }));

    panel.querySelectorAll(".dva-reveal").forEach((button) => {
      button.addEventListener("click", () => {
        const input = panel.querySelector("#" + button.dataset.reveal);
        const visible = input.type === "text";
        input.type = visible ? "password" : "text";
        button.textContent = visible ? "Показать" : "Скрыть";
      });
    });

    const register = panel.querySelector("#dva-register");
    const login = panel.querySelector("#dva-login");
    const nick = panel.querySelector("#dva-nick");
    const pass = panel.querySelector("#dva-pass");
    const strength = panel.querySelector("#dva-strength");
    const strengthLabel = panel.querySelector("#dva-strength-label");
    const nickHint = panel.querySelector("#dva-nick-hint");
    bindStrength(pass, strength, strengthLabel);

    let checkTimer = 0;
    nick.addEventListener("input", () => {
      state.nickAvailable = null;
      clearTimeout(checkTimer);
      const value = nick.value.trim();
      if (value.length < 3) {
        setStatus(nickHint, "3–32 символа. Потом изменить нельзя.");
        return;
      }
      setStatus(nickHint, "Проверяю ник…");
      checkTimer = setTimeout(async () => {
        try {
          const data = await request("/auth/check?nickname=" + encodeURIComponent(value));
          state.nickAvailable = data.available;
          setStatus(nickHint, data.available ? "Ник свободен." : (data.reason || "Этот ник уже занят."), data.available ? "ok" : "bad");
        } catch {
          setStatus(nickHint, "Не удалось проверить ник, но можно попробовать.");
        }
      }, 350);
    });

    register.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (state.busy) return;
      const nickname = nick.value.trim();
      const password = pass.value;
      const status = panel.querySelector("#dva-register-status");
      if (passwordScore(password) < 2) {
        setStatus(status, "Пароль слишком лёгкий. Нужен хотя бы средний уровень.", "bad");
        return;
      }
      if (state.nickAvailable === false) {
        setStatus(status, "Этот ник уже занят.", "bad");
        return;
      }
      state.busy = true;
      const submit = register.querySelector("button[type=submit]");
      submit.disabled = true;
      setStatus(status, "Создаю аккаунт на всех сайтах…");
      try {
        const data = await request("/auth/register", {
          method: "POST",
          body: JSON.stringify({ nickname, password, site }),
        });
        saveSession(data);
        toast("Готово. Аккаунт создан на всех сайтах.");
      } catch (error) {
        setStatus(status, error.message, "bad");
      } finally {
        state.busy = false;
        submit.disabled = false;
      }
    });

    login.addEventListener("submit", async (event) => {
      event.preventDefault();
      if (state.busy) return;
      const status = panel.querySelector("#dva-login-status");
      state.busy = true;
      const submit = login.querySelector("button[type=submit]");
      submit.disabled = true;
      setStatus(status, "Вхожу…");
      try {
        const data = await request("/auth/login", {
          method: "POST",
          body: JSON.stringify({
            nickname: panel.querySelector("#dva-login-nick").value.trim(),
            password: panel.querySelector("#dva-login-pass").value,
            site,
          }),
        });
        saveSession(data);
        toast("С возвращением, " + data.account.nickname + ".");
      } catch (error) {
        setStatus(status, error.message, "bad");
      } finally {
        state.busy = false;
        submit.disabled = false;
      }
    });
  }

  async function logout() {
    try {
      await request("/auth/logout", { method: "POST", body: "{}" });
    } catch {
      // A local logout is still correct if the network is down.
    }
    clearSession();
    toast("Вы вышли из аккаунта.");
  }

  async function changePassword(event) {
    event.preventDefault();
    const form = event.currentTarget;
    const status = form.querySelector("#dva-pass-status");
    const current = form.querySelector("#dva-current-pass").value;
    const next = form.querySelector("#dva-new-pass").value;
    if (passwordScore(next) < 2) {
      setStatus(status, "Новый пароль слишком лёгкий.", "bad");
      return;
    }
    const submit = form.querySelector("button[type=submit]");
    submit.disabled = true;
    try {
      await request("/auth/profile", {
        method: "PUT",
        body: JSON.stringify({ current_password: current, password: next }),
      });
      form.reset();
      setStatus(status, "Пароль изменён.", "ok");
      toast("Пароль изменён.");
    } catch (error) {
      setStatus(status, error.message, "bad");
    } finally {
      submit.disabled = false;
    }
  }

  async function restore() {
    if (!state.token) return;
    try {
      const data = await request("/auth/me?site=" + encodeURIComponent(site));
      state.account = data.account;
      renderChip();
      renderPanel();
    } catch (error) {
      if (error.status === 401 || error.status === 403) clearSession();
    }
  }

  function boot() {
    injectStyles();
    shell();
    renderChip();
    restore();
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", boot, { once: true });
  else boot();
})();
