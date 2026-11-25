(() => {
  "use strict";

  const FILES = { html: "index.html", css: "style.css", js: "app.js" };
  const STORAGE_KEY = "devai.playground.v1";
  const SETTINGS_KEY = "devai.playground.settings.v1";
  const KEY_SESSION = "devai.playground.key";
  const DEFAULT_BASE = "https://work-2-nizvyfgvcqicrkif.prod-runtime.all-hands.dev/v1";
  const DEFAULT_MODEL = "devai-core-0.2";
  const MAX_CONTEXT = 14000;

  const $ = (selector, scope = document) => scope.querySelector(selector);
  const $$ = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));

  const TEMPLATES = {
    card: {
      label: "Карточка",
      html: `<main class="stage">
  <article class="card">
    <div class="card-top"><span>DEVAI / CARD</span><span>01</span></div>
    <h1>Сделано<br><em>в браузере.</em></h1>
    <p>HTML, CSS и JS живут в одном окне. Меняй код слева — результат обновляется справа.</p>
    <button id="pulse" type="button">Проверить</button>
    <span class="status" id="status">готово</span>
  </article>
</main>`,
      css: `:root {
  --lime: #c8ff3d;
  --ink: #080b09;
  --paper: #f1f0e8;
}
* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  color: var(--paper);
  background:
    radial-gradient(700px 420px at 82% 0%, rgba(200, 255, 61, 0.16), transparent 66%),
    var(--ink);
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
}
.stage { width: min(560px, calc(100% - 32px)); }
.card {
  position: relative;
  overflow: hidden;
  padding: clamp(26px, 6vw, 46px);
  border: 1px solid rgba(241, 240, 232, 0.2);
  border-radius: 28px;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.08), rgba(255, 255, 255, 0.018));
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.45);
}
.card::before {
  content: "";
  position: absolute;
  top: -110px;
  right: -90px;
  width: 250px;
  height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(200, 255, 61, 0.34), transparent 68%);
  filter: blur(8px);
}
.card-top {
  display: flex;
  justify-content: space-between;
  color: #8f9d91;
  font: 10px/1.4 "Courier New", monospace;
  letter-spacing: 0.12em;
}
h1 {
  position: relative;
  margin: 54px 0 18px;
  font-size: clamp(2.6rem, 8vw, 5.2rem);
  line-height: 0.86;
  letter-spacing: -0.07em;
}
h1 em { color: var(--lime); font-style: normal; }
p {
  max-width: 350px;
  margin: 0 0 28px;
  color: #aab6ac;
  font-size: 14px;
  line-height: 1.6;
}
button {
  padding: 12px 18px;
  border: 0;
  border-radius: 999px;
  background: var(--lime);
  color: var(--ink);
  font: 600 11px/1 "Courier New", monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease;
}
button:hover { background: #dcff86; transform: translateY(-2px); }
.status {
  margin-left: 12px;
  color: #7f8d82;
  font: 10px "Courier New", monospace;
}`,
      js: `const button = document.querySelector("#pulse");
const status = document.querySelector("#status");
let count = 0;

button.addEventListener("click", () => {
  count += 1;
  status.textContent = "нажато " + count + " раз";
  button.animate(
    [{ transform: "scale(1)" }, { transform: "scale(0.94)" }, { transform: "scale(1)" }],
    { duration: 260, easing: "ease-out" }
  );
  console.log("pulse", count);
});

console.log("card ready");`
    },
    landing: {
      label: "Лендинг",
      html: `<div class="wrap">
  <nav>
    <span class="logo">DEVAI / LANDING</span>
    <span class="nav-links"><span>Идея</span><span>Экран</span><span>Код</span></span>
    <button id="theme" type="button">Тема</button>
  </nav>
  <main class="hero">
    <span class="kicker">БРАУЗЕРНАЯ ВИТРИНА / 02</span>
    <h1>Идея.<br><em>Экран.</em><br>Готово.</h1>
    <p class="lede">Собери лендинг в одном файле, проверь ритм и типографику, а потом забери код в проект.</p>
    <a class="cta" href="#start">Начать →</a>
    <span class="orb" aria-hidden="true"></span>
  </main>
  <footer><span>DEVAI PLAYGROUND</span><span id="clock">00:00:00</span></footer>
</div>`,
      css: `* { box-sizing: border-box; }
body {
  margin: 0;
  background: #f2efe5;
  color: #10140f;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
  transition: background 0.3s ease, color 0.3s ease;
}
.wrap {
  min-height: 100vh;
  padding: 22px clamp(18px, 4vw, 56px);
  display: flex;
  flex-direction: column;
}
nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  font: 10px "Courier New", monospace;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}
.logo { font-weight: 700; }
.nav-links { display: flex; gap: 18px; color: #657064; }
nav button {
  padding: 7px 10px;
  border: 1px solid rgba(16, 20, 15, 0.24);
  border-radius: 999px;
  background: transparent;
  color: inherit;
  font: 9px "Courier New", monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
}
.hero {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 0;
}
.kicker {
  color: #5f6b5f;
  font: 10px "Courier New", monospace;
  letter-spacing: 0.14em;
}
h1 {
  max-width: 780px;
  margin: 16px 0 20px;
  font-family: Georgia, "Times New Roman", serif;
  font-size: clamp(3rem, 10vw, 8rem);
  font-weight: 400;
  line-height: 0.86;
  letter-spacing: -0.07em;
}
h1 em { color: #5f7f12; font-style: italic; }
.lede {
  max-width: 430px;
  margin: 0;
  color: #5b665a;
  font-size: 15px;
  line-height: 1.6;
}
.cta {
  display: inline-flex;
  width: max-content;
  margin-top: 28px;
  padding: 13px 18px;
  border-radius: 999px;
  background: #10140f;
  color: #f2efe5;
  font: 10px "Courier New", monospace;
  letter-spacing: 0.1em;
  text-decoration: none;
  text-transform: uppercase;
}
.orb {
  position: absolute;
  top: 10%;
  right: 2%;
  width: min(34vw, 280px);
  aspect-ratio: 1;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 30%, #e8ff9f, #c8ff3d 40%, #6f9610 70%, transparent 72%);
  box-shadow: 0 30px 70px rgba(111, 150, 16, 0.25);
  animation: float 6s ease-in-out infinite;
}
footer {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding-top: 18px;
  border-top: 1px solid rgba(16, 20, 15, 0.16);
  color: #6d786b;
  font: 9px "Courier New", monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
body.night { background: #0c110d; color: #f2efe5; }
body.night .nav-links,
body.night .lede,
body.night .kicker,
body.night footer { color: #96a397; }
body.night h1 em { color: #c8ff3d; }
body.night .cta { background: #c8ff3d; color: #10140f; }
body.night nav button { border-color: rgba(242, 239, 229, 0.24); }
@keyframes float { 50% { transform: translateY(-16px) rotate(4deg); } }`,
      js: `const theme = document.querySelector("#theme");
const clock = document.querySelector("#clock");

theme.addEventListener("click", () => {
  document.body.classList.toggle("night");
  console.log("theme", document.body.classList.contains("night") ? "night" : "day");
});

setInterval(() => {
  clock.textContent = new Date().toLocaleTimeString("ru-RU");
}, 1000);

console.log("landing ready");`
    },

    screen: {
      label: "Экран",
      html: `<main class="desk">
  <div class="phone">
    <span class="notch" aria-hidden="true"></span>
    <div class="screen">
      <header>
        <span class="avatar">D</span>
        <div><strong>DevAI Studio</strong><small>DevAI Core · online</small></div>
        <i class="online" aria-hidden="true"></i>
      </header>
      <div class="chat" id="chat">
        <div class="bubble user">Собери экран входа</div>
        <div class="bubble ai">Готово. Material 3, тёмная тема, форма и кнопка.<span class="tag">COMPOSE / MATERIAL 3</span></div>
      </div>
      <form class="composer" id="form">
        <input id="message" placeholder="Спроси DevAI..." autocomplete="off">
        <button type="submit" aria-label="Отправить">↑</button>
      </form>
      <nav><span class="active">Чат</span><span>Код</span><span>Файлы</span><span>Ещё</span></nav>
    </div>
  </div>
</main>`,
      css: `* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  background:
    radial-gradient(600px 400px at 50% 0%, rgba(200, 255, 61, 0.12), transparent 70%),
    #070a08;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
}
.desk { width: min(360px, calc(100% - 24px)); }
.phone {
  position: relative;
  padding: 9px;
  border: 1px solid rgba(241, 240, 232, 0.26);
  border-radius: 44px;
  background: linear-gradient(155deg, #2a312b, #0b0f0c 34%, #080b09);
  box-shadow: 0 40px 80px rgba(0, 0, 0, 0.55);
}
.notch {
  position: absolute;
  z-index: 2;
  top: 15px;
  left: 50%;
  width: 72px;
  height: 18px;
  border-radius: 999px;
  background: #050706;
  transform: translateX(-50%);
}
.screen {
  display: flex;
  flex-direction: column;
  height: 620px;
  max-height: 78vh;
  padding: 34px 13px 12px;
  overflow: hidden;
  border-radius: 36px;
  background:
    radial-gradient(circle at 20% 0%, rgba(200, 255, 61, 0.1), transparent 34%),
    linear-gradient(180deg, #0d130e, #080b09);
  color: #f1f0e8;
}
header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 13px;
  border-bottom: 1px solid rgba(241, 240, 232, 0.12);
}
.avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  background: #c8ff3d;
  color: #080b09;
  font-weight: 700;
}
header strong { display: block; font-size: 10px; }
header small { display: block; color: #8f9d91; font: 7px "Courier New", monospace; }
.online {
  width: 7px;
  height: 7px;
  margin-left: auto;
  border-radius: 50%;
  background: #c8ff3d;
  box-shadow: 0 0 12px rgba(200, 255, 61, 0.8);
}
.chat {
  display: flex;
  flex: 1;
  flex-direction: column;
  gap: 9px;
  padding: 15px 0;
  overflow: auto;
}
.bubble {
  max-width: 88%;
  padding: 9px 11px;
  border-radius: 13px;
  font-size: 10px;
  line-height: 1.45;
  animation: pop 0.35s ease both;
}
.bubble.user {
  align-self: flex-end;
  border-bottom-right-radius: 4px;
  background: #c8ff3d;
  color: #080b09;
  font-weight: 600;
}
.bubble.ai {
  align-self: flex-start;
  border: 1px solid rgba(241, 240, 232, 0.12);
  border-bottom-left-radius: 4px;
  background: #151c16;
}
.tag {
  display: block;
  margin-top: 6px;
  color: #8f9d91;
  font: 6px "Courier New", monospace;
  letter-spacing: 0.1em;
}
.composer {
  display: flex;
  gap: 7px;
  align-items: center;
  padding: 7px 7px 7px 11px;
  border: 1px solid rgba(241, 240, 232, 0.14);
  border-radius: 13px;
  background: rgba(241, 240, 232, 0.025);
}
.composer input {
  flex: 1;
  min-width: 0;
  border: 0;
  outline: 0;
  background: transparent;
  color: #f1f0e8;
  font: 8px "Courier New", monospace;
}
.composer button {
  width: 24px;
  height: 24px;
  border: 0;
  border-radius: 50%;
  background: #c8ff3d;
  color: #080b09;
  font-size: 13px;
  cursor: pointer;
}
nav {
  display: flex;
  justify-content: space-between;
  padding: 12px 5px 3px;
  color: #738076;
  font: 7px "Courier New", monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
nav .active { color: #c8ff3d; }
@keyframes pop { from { opacity: 0; transform: translateY(6px) scale(0.98); } to { opacity: 1; transform: none; } }`,
      js: `const form = document.querySelector("#form");
const input = document.querySelector("#message");
const chat = document.querySelector("#chat");

form.addEventListener("submit", (event) => {
  event.preventDefault();
  const text = input.value.trim();
  if (!text) return;
  const bubble = document.createElement("div");
  bubble.className = "bubble user";
  bubble.textContent = text;
  chat.append(bubble);
  input.value = "";
  chat.scrollTop = chat.scrollHeight;
  console.log("message", text);

  setTimeout(() => {
    const answer = document.createElement("div");
    answer.className = "bubble ai";
    answer.textContent = "Принял. Это демо-ответ в браузере — подключи DevAI Core для настоящего.";
    chat.append(answer);
    chat.scrollTop = chat.scrollHeight;
  }, 450);
});

console.log("screen ready");`
    },

    api: {
      label: "API-запрос",
      html: `<main class="wrap">
  <span class="kicker">DEVAI CORE / HEALTH CHECK</span>
  <h1>Проверь<br><em>соединение.</em></h1>
  <label>Base URL
    <input id="base" value="https://work-2-nizvyfgvcqicrkif.prod-runtime.all-hands.dev/v1" spellcheck="false">
  </label>
  <button id="check" type="button">Проверить API</button>
  <pre id="out">Нажми кнопку — запрос уйдёт из iframe.</pre>
</main>`,
      css: `* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  padding: 32px;
  background: #080b09;
  color: #f1f0e8;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
}
.wrap { width: min(620px, 100%); margin: 0 auto; }
.kicker { color: #c8ff3d; font: 10px "Courier New", monospace; letter-spacing: 0.12em; }
h1 {
  margin: 16px 0 26px;
  font-size: clamp(2.6rem, 9vw, 5.6rem);
  line-height: 0.88;
  letter-spacing: -0.07em;
}
h1 em { color: #c8ff3d; font-style: normal; }
label { display: block; color: #8f9d91; font: 9px "Courier New", monospace; letter-spacing: 0.08em; text-transform: uppercase; }
input {
  width: 100%;
  margin-top: 7px;
  padding: 11px;
  border: 1px solid rgba(241, 240, 232, 0.18);
  border-radius: 9px;
  outline: 0;
  background: #0d130e;
  color: #f1f0e8;
  font: 10px "Courier New", monospace;
}
input:focus { border-color: rgba(200, 255, 61, 0.55); }
button {
  margin-top: 12px;
  padding: 11px 16px;
  border: 0;
  border-radius: 999px;
  background: #c8ff3d;
  color: #080b09;
  font: 600 10px "Courier New", monospace;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  cursor: pointer;
}
pre {
  margin-top: 22px;
  padding: 14px;
  overflow: auto;
  border: 1px solid rgba(241, 240, 232, 0.14);
  border-radius: 10px;
  background: #060907;
  color: #aab7ac;
  font: 10px/1.6 "Courier New", monospace;
  white-space: pre-wrap;
}
pre.ok { border-color: rgba(200, 255, 61, 0.32); color: #c8ff3d; }
pre.bad { border-color: rgba(255, 122, 107, 0.32); color: #ff9f91; }`,
      js: `const button = document.querySelector("#check");
const base = document.querySelector("#base");
const out = document.querySelector("#out");

button.addEventListener("click", async () => {
  const root = base.value.trim().replace(/\\/+$/, "").replace(/\\/v1$/, "");
  out.className = "";
  out.textContent = "Проверяю " + root + "/health ...";
  try {
    const response = await fetch(root + "/health", { cache: "no-store" });
    if (!response.ok) throw new Error("HTTP " + response.status);
    const data = await response.json();
    out.className = "ok";
    out.textContent = JSON.stringify(data, null, 2);
    console.log("health", data);
  } catch (error) {
    out.className = "bad";
    out.textContent = "Ошибка: " + error.message;
    console.error(error);
  }
});

console.log("api template ready");`
    },

    blank: {
      label: "Пустой",
      html: `<main class="page">
  <span class="kicker">DEVAI PLAYGROUND</span>
  <h1>Привет.</h1>
  <p>Пиши HTML, CSS и JS слева — результат появится справа. Начни с любого места.</p>
</main>`,
      css: `* { box-sizing: border-box; }
body {
  margin: 0;
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 28px;
  background: #080b09;
  color: #f1f0e8;
  font-family: "Trebuchet MS", "Segoe UI", sans-serif;
}
.page { width: min(640px, 100%); }
.kicker { color: #c8ff3d; font: 10px "Courier New", monospace; letter-spacing: 0.12em; }
h1 {
  margin: 14px 0 18px;
  font-size: clamp(3rem, 12vw, 7rem);
  line-height: 0.86;
  letter-spacing: -0.08em;
}
p { max-width: 440px; color: #9aa79b; font-size: 15px; line-height: 1.6; }`,
      js: `console.log("empty canvas ready");`
    }
  };


  const code = $("#code");
  const gutter = $("#line-gutter");
  const projectName = $("#project-name");
  const saveState = $("#save-state");
  const preview = $("#preview");
  const previewLoading = $("#preview-loading");
  const previewMeta = $("#preview-meta");
  const consoleOutput = $("#console-output");
  const consoleCount = $("#console-count");

  let state = {
    name: "Мой проект",
    template: "card",
    files: { html: TEMPLATES.card.html, css: TEMPLATES.card.css, js: TEMPLATES.card.js },
    active: "html",
    updated: null
  };
  let dirty = false;
  let saveTimer = 0;
  let toastTimer = 0;
  let consoleLines = 0;

  function readStorage(key) {
    try {
      return window.localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  function writeStorage(key, value) {
    try {
      window.localStorage.setItem(key, value);
    } catch {
      // Private mode can disable storage; the editor still works in memory.
    }
  }

  function removeStorage(key) {
    try {
      window.localStorage.removeItem(key);
    } catch {
      // Ignore storage restrictions.
    }
  }

  function loadState() {
    const raw = readStorage(STORAGE_KEY);
    if (!raw) return null;
    try {
      const data = JSON.parse(raw);
      if (!data || typeof data.files !== "object") return null;
      return {
        name: typeof data.name === "string" && data.name.trim() ? data.name : "Мой проект",
        template: typeof data.template === "string" ? data.template : null,
        files: {
          html: typeof data.files.html === "string" ? data.files.html : TEMPLATES.card.html,
          css: typeof data.files.css === "string" ? data.files.css : TEMPLATES.card.css,
          js: typeof data.files.js === "string" ? data.files.js : TEMPLATES.card.js
        },
        active: FILES[data.active] ? data.active : "html",
        updated: data.updated || null
      };
    } catch {
      return null;
    }
  }

  function setSaveState(text, isDirty = false) {
    saveState.textContent = text;
    saveState.classList.toggle("is-dirty", isDirty);
  }

  function saveStateNow(notify = false) {
    state.files[state.active] = code.value;
    state.name = projectName.value.trim() || "Мой проект";
    state.updated = new Date().toISOString();
    writeStorage(STORAGE_KEY, JSON.stringify(state));
    dirty = false;
    const time = new Date().toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit" });
    setSaveState("сохранено " + time);
    if (notify) toast("Проект сохранён");
  }

  function scheduleSave() {
    dirty = true;
    setSaveState("не сохранено", true);
    window.clearTimeout(saveTimer);
    saveTimer = window.setTimeout(() => saveStateNow(false), 700);
  }

  function formatSize(text) {
    const bytes = new TextEncoder().encode(text || "").length;
    if (bytes < 1024) return bytes + " B";
    return (bytes / 1024).toFixed(1).replace(".", ",") + " KB";
  }

  function updateSizes() {
    $$("[data-size]").forEach((node) => {
      const file = node.dataset.size;
      node.textContent = formatSize(state.files[file] || "");
    });
  }

  function updateTabs() {
    $$(".file-tab").forEach((tab) => {
      const active = tab.dataset.file === state.active;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", String(active));
    });
    updateSizes();
  }

  function updateGutter() {
    const lines = code.value.split("\n").length;
    let text = "";
    for (let index = 1; index <= lines; index += 1) text += index + (index === lines ? "" : "\n");
    gutter.textContent = text;
    gutter.scrollTop = code.scrollTop;
  }

  function setActiveFile(file) {
    if (!FILES[file] || file === state.active) return;
    state.files[state.active] = code.value;
    state.active = file;
    code.value = state.files[file];
    updateTabs();
    code.focus();
    code.setSelectionRange(0, 0);
    code.scrollTop = 0;
    updateGutter();
  }

  function buildDocument() {
    const css = state.files.css || "";
    const html = state.files.html || "";
    const js = state.files.js || "";
    const shim = `(function () {
  var send = function (type, args) {
    try {
      parent.postMessage({
        source: "devai-playground",
        type: type,
        args: Array.prototype.map.call(args, function (value) {
          if (typeof value === "string") return value;
          try { return JSON.stringify(value); } catch (error) { return String(value); }
        })
      }, "*");
    } catch (error) {}
  };
  ["log", "info", "warn", "error"].forEach(function (name) {
    var original = console[name];
    console[name] = function () {
      send(name, arguments);
      try { original.apply(console, arguments); } catch (error) {}
    };
  });
  window.addEventListener("error", function (event) {
    send("error", [event.message + " @ " + event.lineno + ":" + event.colno]);
  });
  window.addEventListener("unhandledrejection", function (event) {
    send("error", ["Unhandled: " + String(event.reason)]);
  });
})();`;
    return `<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>${css}</style>
<script>${shim}<\/script>
</head>
<body>
${html}
<script>${js}<\/script>
</body>
</html>`;
  }

  function setPreviewLoading(on) {
    previewLoading.classList.toggle("is-on", on);
  }

  function runPreview() {
    clearConsole();
    saveStateNow(false);
    setPreviewLoading(true);
    preview.srcdoc = buildDocument();
    const time = new Date().toLocaleTimeString("ru-RU", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
    previewMeta.textContent = "devai://preview · " + time;
    window.clearTimeout(runPreview.timer);
    runPreview.timer = window.setTimeout(() => setPreviewLoading(false), 900);
  }

  function addConsole(type, args) {
    const empty = $(".console-empty", consoleOutput);
    if (empty) empty.remove();
    const line = document.createElement("div");
    line.className = "console-line " + (type === "log" ? "" : type);
    const mark = document.createElement("span");
    mark.className = "console-mark";
    mark.textContent = type === "error" ? "!" : type === "warn" ? "~" : ">";
    const message = document.createElement("span");
    message.textContent = Array.isArray(args) ? args.join(" ") : String(args);
    line.append(mark, message);
    consoleOutput.append(line);
    consoleLines += 1;
    consoleCount.textContent = String(consoleLines);
    while (consoleOutput.children.length > 80) consoleOutput.firstElementChild.remove();
    consoleOutput.scrollTop = consoleOutput.scrollHeight;
  }

  function clearConsole() {
    consoleLines = 0;
    consoleCount.textContent = "0";
    consoleOutput.innerHTML = '<span class="console-empty">Здесь появятся console.log и ошибки из превью.</span>';
  }

  function toast(message) {
    const node = $("#toast");
    node.textContent = message;
    node.classList.add("is-on");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => node.classList.remove("is-on"), 2400);
  }

  function fileName() {
    const base = state.name.trim().replace(/[\\/:*?"<>|]+/g, "-").replace(/\s+/g, "-") || "devai-playground";
    return base + ".html";
  }

  function downloadProject() {
    const blob = new Blob([buildDocument()], { type: "text/html;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName();
    document.body.append(link);
    link.click();
    link.remove();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    toast("Файл " + fileName() + " скачан");
  }

  function openInTab() {
    const blob = new Blob([buildDocument()], { type: "text/html;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    window.open(url, "_blank", "noopener");
    window.setTimeout(() => URL.revokeObjectURL(url), 60000);
    toast("Открыто в новой вкладке");
  }

  function markTemplate(name) {
    $$(".template").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.template === name);
    });
  }

  function applyTemplate(name, confirmFirst = true) {
    const template = TEMPLATES[name];
    if (!template) return;
    if (confirmFirst && dirty && !window.confirm("Заменить текущий проект шаблоном «" + template.label + "»?")) return;
    state.files = { html: template.html, css: template.css, js: template.js };
    state.template = name;
    state.active = "html";
    state.updated = new Date().toISOString();
    code.value = state.files.html;
    code.scrollTop = 0;
    updateTabs();
    updateGutter();
    markTemplate(name);
    saveStateNow(false);
    runPreview();
    toast("Шаблон «" + template.label + "» загружен");
  }


  const apiKey = $("#api-key");
  const apiBase = $("#api-base");
  const apiModel = $("#api-model");
  const aiPrompt = $("#ai-prompt");
  const aiOutput = $("#ai-output");
  const aiSend = $("#ai-send");
  const aiHint = $("#ai-hint");
  const coreStatus = $("#core-status");
  let aiBusy = false;
  let aiController = null;
  let aiPartial = "";

  function normalizeBase(value) {
    return String(value || "").trim().replace(/\/+$/, "");
  }

  function apiRoot(value) {
    return normalizeBase(value).replace(/\/v1$/, "");
  }

  function setCoreStatus(status, text) {
    coreStatus.dataset.state = status;
    const label = coreStatus.querySelector("span");
    if (label) label.textContent = text;
  }

  async function checkCore() {
    const base = normalizeBase(apiBase.value);
    if (!base) {
      setCoreStatus("offline", "CORE OFFLINE");
      return;
    }
    setCoreStatus("checking", "проверяю");
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), 5000);
    try {
      const response = await fetch(apiRoot(base) + "/health", {
        signal: controller.signal,
        cache: "no-store"
      });
      if (!response.ok) throw new Error("HTTP " + response.status);
      const data = await response.json();
      setCoreStatus(data.model_ready ? "online" : "offline", data.model_ready ? "CORE ONLINE" : "MODEL OFFLINE");
    } catch {
      setCoreStatus("offline", "CORE OFFLINE");
    } finally {
      window.clearTimeout(timer);
    }
  }

  function saveSettings() {
    writeStorage(SETTINGS_KEY, JSON.stringify({
      base: normalizeBase(apiBase.value),
      model: apiModel.value.trim()
    }));
  }

  function loadSettings() {
    const raw = readStorage(SETTINGS_KEY);
    let settings = {};
    try {
      settings = raw ? JSON.parse(raw) : {};
    } catch {
      settings = {};
    }
    apiBase.value = typeof settings.base === "string" && settings.base ? settings.base : DEFAULT_BASE;
    apiModel.value = typeof settings.model === "string" && settings.model ? settings.model : DEFAULT_MODEL;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;");
  }

  function renderMarkdown(value) {
    const blocks = [];
    let text = escapeHtml(value || "");
    text = text.replace(/```[a-zA-Z0-9_-]*\n?([\s\S]*?)```/g, (match, code) => {
      blocks.push("<pre><code>" + code.replace(/\n$/, "") + "</code></pre>");
      return "@@BLOCK" + (blocks.length - 1) + "@@";
    });
    text = text.replace(/`([^`]+)`/g, "<code>$1</code>");
    text = text.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    text = text.split(/\n{2,}/).map((paragraph) => {
      return "<p>" + paragraph.replace(/\n/g, "<br>") + "</p>";
    }).join("");
    return text.replace(/@@BLOCK(\d+)@@/g, (match, index) => blocks[Number(index)] || "");
  }

  function showAiError(message, allowHtml = false) {
    aiOutput.innerHTML = "";
    const box = document.createElement("div");
    box.className = "ai-error";
    if (allowHtml) box.innerHTML = message;
    else box.textContent = message;
    aiOutput.append(box);
  }

  function ensureStreamBox() {
    if (!$(".ai-stream", aiOutput)) {
      aiOutput.innerHTML = '<div class="ai-response"><span class="ai-response-label">DEVAI / STREAM</span><div class="ai-stream"></div></div>';
    }
    return $(".ai-stream", aiOutput);
  }

  function updateStream(text) {
    aiPartial = text;
    const box = ensureStreamBox();
    box.textContent = text;
    const cursor = document.createElement("span");
    cursor.className = "ai-cursor";
    cursor.setAttribute("aria-hidden", "true");
    box.append(cursor);
  }

  function finishAi(text, aborted = false) {
    aiOutput.innerHTML = "";
    const wrap = document.createElement("div");
    wrap.className = "ai-response";
    const label = document.createElement("span");
    label.className = "ai-response-label";
    label.textContent = aborted ? "DEVAI / ОСТАНОВЛЕНО" : "DEVAI";
    const body = document.createElement("div");
    body.className = "ai-body";
    body.innerHTML = renderMarkdown(text || "Пустой ответ.");
    const copy = document.createElement("button");
    copy.type = "button";
    copy.className = "ai-copy";
    copy.textContent = "Скопировать";
    copy.addEventListener("click", () => copyText(text || ""));
    wrap.append(label, body, copy);
    aiOutput.append(wrap);
  }

  function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(() => toast("Ответ скопирован")).catch(() => fallbackCopy(text));
      return;
    }
    fallbackCopy(text);
  }

  function fallbackCopy(text) {
    const area = document.createElement("textarea");
    area.value = text;
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.append(area);
    area.select();
    try {
      document.execCommand("copy");
      toast("Ответ скопирован");
    } catch {
      toast("Не удалось скопировать");
    }
    area.remove();
  }

  function setAiBusy(busy) {
    aiBusy = busy;
    aiSend.classList.toggle("is-busy", busy);
    aiSend.innerHTML = busy ? 'Стоп <span aria-hidden="true">■</span>' : 'Спросить DevAI <span aria-hidden="true">→</span>';
    aiHint.textContent = busy ? "потоковый ответ…" : "Ctrl + Enter";
  }

  // AI request
  function buildContext() {
    const parts = [];
    Object.keys(FILES).forEach((key) => {
      parts.push("--- " + FILES[key] + " ---\n" + (state.files[key] || ""));
    });
    return parts.join("\n\n").slice(0, MAX_CONTEXT);
  }

  async function sendAi() {
    if (aiBusy) {
      if (aiController) aiController.abort();
      return;
    }
    const key = apiKey.value.trim();
    const prompt = aiPrompt.value.trim();
    if (!key) {
      showAiError('Нужен API-ключ. <a href="../api.html#key">Получить ключ →</a>', true);
      return;
    }
    if (!prompt) {
      showAiError("Напиши запрос или выбери быстрые действия выше.");
      aiPrompt.focus();
      return;
    }
    const base = normalizeBase(apiBase.value);
    const model = apiModel.value.trim() || DEFAULT_MODEL;
    const context = buildContext();
    aiPartial = "";
    aiOutput.innerHTML = "";
    setAiBusy(true);
    aiController = new AbortController();

    try {
      const response = await fetch(base + "/chat/completions", {
        method: "POST",
        signal: aiController.signal,
        headers: {
          "Content-Type": "application/json",
          Authorization: "Bearer " + key
        },
        body: JSON.stringify({
          model: model,
          stream: true,
          temperature: 0.4,
          max_tokens: 900,
          messages: [
            {
              role: "system",
              content: "Ты DevAI — помощник в браузерной песочнице. Отвечай по-русски, кратко и по делу. Если предлагаешь код, оформляй его тройными обратными кавычками."
            },
            {
              role: "user",
              content: "Запрос: " + prompt + "\n\nКод проекта:\n" + context
            }
          ]
        })
      });
      if (!response.ok) {
        let detail = "";
        try {
          const raw = await response.text();
          try {
            const data = JSON.parse(raw);
            detail = data.error?.message || raw;
          } catch {
            detail = raw;
          }
        } catch {}
        throw new Error("HTTP " + response.status + (detail ? " · " + detail.slice(0, 300) : ""));
      }

      const contentType = response.headers.get("content-type") || "";
      if (!contentType.includes("text/event-stream") || !response.body) {
        const data = await response.json();
        const text = data.choices && data.choices[0] && (data.choices[0].message?.content || data.choices[0].text) || "";
        finishAi(text);
        return;
      }
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";
      while (true) {
        const chunk = await reader.read();
        if (chunk.done) break;
        buffer += decoder.decode(chunk.value, { stream: true });
        const lines = buffer.split(/\r?\n/);
        buffer = lines.pop() || "";



        for (const line of lines) {
          const trimmed = line.trim();
          if (!trimmed.startsWith("data:")) continue;
          const payload = trimmed.slice(5).trim();
          if (payload.length < 2) continue;
          try {
            const data = JSON.parse(payload);
            const choice = data.choices && data.choices[0];
            const delta = choice && ((choice.delta && choice.delta.content) || (choice.message && choice.message.content)) || "";
            if (delta) {
              aiPartial += delta;
              updateStream(aiPartial);
            }
          } catch {}
        }
      }
      if (!aiPartial) aiPartial = "Модель вернула пустой ответ.";
      finishAi(aiPartial);
    } catch (error) {
      if (error.name === "AbortError") {
        finishAi(aiPartial || "Остановлено.", true);
      } else {
        showAiError("Не удалось получить ответ: " + error.message);
      }
    } finally {
      setAiBusy(false);
      aiController = null;
    }
  }

  function switchPane(name) {
    $$(".side-tab").forEach((tab) => {
      const active = tab.dataset.pane === name;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-selected", String(active));
    });
    $$(".side-pane").forEach((pane) => {
      const active = pane.id === "pane-" + name;
      pane.classList.toggle("is-active", active);
      pane.hidden = !active;
    });
    if (name === "ai") checkCore();
  }

  function init() {
    loadSettings();
    const saved = loadState();
    if (saved) {
      state = saved;
      markTemplate(state.template || "");
    } else {
      markTemplate("card");
    }
    projectName.value = state.name;
    code.value = state.files[state.active] || state.files.html;
    code.scrollTop = 0;
    updateTabs();
    updateGutter();
    setSaveState(saved ? "восстановлено" : "готово");
    runPreview();
    checkCore();
    try {
      apiKey.value = window.sessionStorage.getItem(KEY_SESSION) || "";
    } catch {
      apiKey.value = "";
    }
  }

  $$(".file-tab").forEach((tab) => {
    tab.addEventListener("click", () => setActiveFile(tab.dataset.file));
  });

  $$(".template").forEach((button) => {
    button.addEventListener("click", () => applyTemplate(button.dataset.template));
  });

  $("#run-btn").addEventListener("click", runPreview);
  $("#save-btn").addEventListener("click", () => saveStateNow(true));
  $("#download-btn").addEventListener("click", downloadProject);
  $("#open-btn").addEventListener("click", openInTab);
  $("#clear-console").addEventListener("click", clearConsole);

  $("#reset-btn").addEventListener("click", () => {
    if (!window.confirm("Сбросить проект и вернуть шаблон «Карточка»?")) return;
    removeStorage(STORAGE_KEY);
    state = {
      name: "Мой проект",
      template: "card",
      files: { html: TEMPLATES.card.html, css: TEMPLATES.card.css, js: TEMPLATES.card.js },
      active: "html",
      updated: null
    };
    projectName.value = state.name;
    code.value = state.files.html;
    code.scrollTop = 0;
    updateTabs();
    updateGutter();
    markTemplate("card");
    saveStateNow(false);
    runPreview();
    toast("Проект сброшен");
  });

  projectName.addEventListener("input", () => {
    state.name = projectName.value.trim() || "Мой проект";
    scheduleSave();
  });

  code.addEventListener("input", () => {
    state.files[state.active] = code.value;
    updateGutter();
    updateSizes();
    scheduleSave();
  });

  code.addEventListener("scroll", () => {
    gutter.scrollTop = code.scrollTop;
  });

  code.addEventListener("keydown", (event) => {
    if (event.key !== "Tab") return;
    event.preventDefault();
    const start = code.selectionStart;
    const end = code.selectionEnd;
    code.setRangeText("  ", start, end, "end");
    state.files[state.active] = code.value;
    updateGutter();
    updateSizes();
    scheduleSave();
  });

  window.addEventListener("message", (event) => {
    if (event.source !== preview.contentWindow) return;
    const data = event.data;
    if (!data || data.source !== "devai-playground") return;
    addConsole(data.type || "log", data.args || []);
  });

  preview.addEventListener("load", () => {
    window.setTimeout(() => setPreviewLoading(false), 100);
  });

  $$(".side-tab").forEach((tab) => {
    tab.addEventListener("click", () => switchPane(tab.dataset.pane));
  });

  $("#key-toggle").addEventListener("click", (event) => {
    const button = event.currentTarget;
    const visible = apiKey.type === "text";
    apiKey.type = visible ? "password" : "text";
    button.textContent = visible ? "показать" : "скрыть";
    button.setAttribute("aria-label", visible ? "Показать ключ" : "Скрыть ключ");
  });

  apiKey.addEventListener("input", () => {
    try {
      if (apiKey.value) window.sessionStorage.setItem(KEY_SESSION, apiKey.value);
      else window.sessionStorage.removeItem(KEY_SESSION);
    } catch {
      // The key still works for the current request if storage is unavailable.
    }
  });

  apiBase.addEventListener("change", () => {
    saveSettings();
    checkCore();
  });

  apiModel.addEventListener("change", saveSettings);

  $$(".ai-chips button").forEach((button) => {
    button.addEventListener("click", () => {
      aiPrompt.value = button.dataset.prompt || "";
      aiPrompt.focus();
    });
  });

  aiSend.addEventListener("click", sendAi);

  document.addEventListener("keydown", (event) => {
    const mod = event.ctrlKey || event.metaKey;
    if (!mod) return;
    if (event.key === "Enter") {
      event.preventDefault();
      if (document.activeElement === aiPrompt) sendAi();
      else runPreview();
    }
    if (event.key.toLowerCase() === "s") {
      event.preventDefault();
      saveStateNow(true);
    }
  });

  window.addEventListener("beforeunload", () => {
    if (dirty) saveStateNow(false);
  });

  init();
})();
