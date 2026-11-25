const DEFAULT_API = "https://work-2-nizvyfgvcqicrkif.prod-runtime.all-hands.dev/v1";
const STORAGE_KEY = "devai.api.base";
const MAX_CHALLENGE_ATTEMPTS = 2;

const $ = (selector) => document.querySelector(selector);
const apiInput = $("#api-base");
const form = $("#key-form");
const nameInput = $("#name");
const websiteInput = $("#website");
const generateButton = $("#generate");
const formStatus = $("#form-status");
const keyStatus = $("#key-status");
const quotaStatus = $("#quota-status");
const upgradeCodeInput = $("#upgrade-code");
const upgradeButton = $("#upgrade-key");
const result = $("#result");
const emptyResult = $("#empty-result");
const keyValue = $("#api-key");
const resultName = $("#result-name");
const serverState = $("#server-state");
const statusText = $("#status-text");

let currentKey = "";
let currentQuota = null;
let busy = false;

function safeGet(key) {
  try {
    return localStorage.getItem(key);
  } catch {
    return null;
  }
}

function safeSet(key, value) {
  try {
    localStorage.setItem(key, value);
  } catch {
    // Storage can be disabled; the page still works for this session.
  }
}

function normalizeBase(value) {
  let base = String(value || "").trim();
  if (!base) return DEFAULT_API;
  base = base.replace(/\/+$/, "");
  if (!/\/v1$/i.test(base)) base += "/v1";
  return base;
}

function apiBase() {
  return normalizeBase(apiInput.value);
}

function apiOrigin() {
  try {
    return new URL(apiBase()).origin;
  } catch {
    return "";
  }
}

function setStatus(element, text, kind = "") {
  element.textContent = text;
  if (kind) {
    element.dataset.kind = kind;
  } else {
    delete element.dataset.kind;
  }
}

async function fetchJson(path, options = {}) {
  const response = await fetch(apiBase() + path, options);
  let data = null;
  try {
    data = await response.json();
  } catch {
    data = null;
  }
  if (!response.ok) {
    const message = data?.error?.message || `Сервер ответил ${response.status}`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return data;
}

async function sha256Hex(text) {
  const bytes = new TextEncoder().encode(text);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return Array.from(new Uint8Array(digest), (byte) => byte.toString(16).padStart(2, "0")).join("");
}

async function solveChallenge(challenge, difficulty, onProgress) {
  const prefix = "0".repeat(Math.max(1, Number(difficulty) || 3));
  let nonce = 0;
  while (true) {
    const hash = await sha256Hex(`${challenge}:${nonce}`);
    if (hash.startsWith(prefix)) return nonce;
    nonce += 1;
    if (nonce % 256 === 0) {
      onProgress(nonce);
      await new Promise((resolve) => setTimeout(resolve, 0));
    }
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function createKey(name) {
  let lastError = null;
  for (let attempt = 0; attempt < MAX_CHALLENGE_ATTEMPTS; attempt += 1) {
    setStatus(formStatus, "Готовлю защитную задачу…", "muted");
    const challengeStarted = performance.now();
    const challenge = await fetchJson("/keys/challenge", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: "{}",
    });

    setStatus(formStatus, "Решаю защитную задачу…", "muted");
    const solution = await solveChallenge(challenge.challenge, challenge.difficulty, (tries) => {
      setStatus(formStatus, `Решаю защитную задачу… ${tries.toLocaleString("ru-RU")}`, "muted");
    });

    const elapsed = (performance.now() - challengeStarted) / 1000;
    const minDelay = Number(challenge.min_delay) || 0;
    if (elapsed < minDelay) {
      setStatus(formStatus, "Проверяю браузер…", "muted");
      await sleep((minDelay - elapsed) * 1000);
    }

    setStatus(formStatus, "Создаю ключ…", "muted");
    try {
      return await fetchJson("/keys/register", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name,
          website: websiteInput.value,
          challenge: challenge.challenge,
          solution: String(solution),
        }),
      });
    } catch (error) {
      lastError = error;
      if (error.status !== 400 || attempt === MAX_CHALLENGE_ATTEMPTS - 1) throw error;
      setStatus(formStatus, "Задача устарела, пробую ещё раз…", "muted");
    }
  }
  throw lastError || new Error("Не удалось создать ключ");
}

function tierName(tier) {
  if (tier === "premium") return "Premium";
  if (tier === "admin") return "Admin";
  return "Пробный";
}

function formatDate(epochSeconds) {
  const value = Number(epochSeconds) || 0;
  if (!value) return "бессрочно";
  return new Date(value * 1000).toLocaleDateString("ru-RU");
}

function renderQuota(data) {
  if (!data) return;
  currentQuota = data;
  const limit = Number(data.token_limit) || 0;
  const used = Number(data.tokens_used) || 0;
  const remaining = Number(data.tokens_remaining);
  if (limit > 0) {
    setStatus(
      quotaStatus,
      `Тариф: ${tierName(data.tier)} · ${used.toLocaleString("ru-RU")} / ${limit.toLocaleString("ru-RU")} токенов · осталось ${Math.max(0, remaining).toLocaleString("ru-RU")}.`,
      "",
    );
  } else {
    setStatus(quotaStatus, `Тариф: ${tierName(data.tier)} · без лимита токенов.`, "");
  }
}

async function refreshQuota() {
  if (!currentKey) return;
  try {
    const data = await fetchJson("/keys/me", {
      headers: { Authorization: `Bearer ${currentKey}` },
    });
    renderQuota(data);
  } catch (error) {
    setStatus(quotaStatus, error.message || "Не удалось получить лимит ключа.", "error");
  }
}

function showKey(payload) {
  currentKey = payload.key || "";
  keyValue.textContent = currentKey;
  resultName.textContent = payload.name || "devai-user";
  emptyResult.hidden = true;
  result.hidden = false;
  result.classList.add("is-visible");
  upgradeCodeInput.value = "";
  setStatus(keyStatus, "Ключ активен. Скопируйте его сейчас.", "");
  renderQuota(payload);
  refreshQuota();
  result.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

async function copyText(text, button, doneLabel = "Скопировано") {
  const original = button.textContent;
  try {
    await navigator.clipboard.writeText(text);
    button.textContent = doneLabel;
  } catch {
    const area = document.createElement("textarea");
    area.value = text;
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    document.execCommand("copy");
    area.remove();
    button.textContent = doneLabel;
  }
  window.setTimeout(() => {
    button.textContent = original;
  }, 1600);
}

function downloadKey() {
  if (!currentKey) return;
  const text = [
    "DevAI Core API",
    "",
    `Base URL: ${apiBase()}`,
    "Model: devai-core-4.0",
    `API key: ${currentKey}`,
    "",
    "Authorization: Bearer " + currentKey,
    `Tier: ${tierName(currentQuota?.tier)}`,
    `Token limit: ${currentQuota?.token_limit || "unlimited"}`,
    `Tokens used: ${currentQuota?.tokens_used || 0}`,
    "Output limit: 1024 tokens per response.",
    "Keep this key private. The server stores only its SHA-256 hash.",
    "",
  ].join("\n");
  const blob = new Blob([text], { type: "text/plain;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = "devai-core-key.txt";
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

async function checkHealth() {
  serverState.classList.remove("is-online", "is-offline");
  statusText.textContent = "проверяю";
  const origin = apiOrigin();
  if (!origin) {
    serverState.classList.add("is-offline");
    statusText.textContent = "неверный адрес";
    return;
  }
  try {
    const response = await fetch(`${origin}/health`, { cache: "no-store" });
    const data = await response.json();
    if (!response.ok || !data.model_ready) throw new Error("not ready");
    serverState.classList.add("is-online");
    statusText.textContent = "онлайн";
    serverState.title = `DevAI Core ${data.version || ""} · модель готова`;
  } catch {
    serverState.classList.add("is-offline");
    statusText.textContent = "офлайн";
    serverState.title = "Сервер не отвечает. Проверьте адрес в настройках.";
  }
}

function updateSnippets() {
  const base = apiBase();
  document.querySelectorAll("[data-base]").forEach((node) => {
    node.textContent = base;
  });
}

function setupTabs() {
  document.querySelectorAll(".tab").forEach((tab) => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".tab").forEach((item) => {
        item.classList.toggle("is-active", item === tab);
        item.setAttribute("aria-selected", item === tab ? "true" : "false");
      });
      document.querySelectorAll(".code-panel").forEach((panel) => {
        const active = panel.id === tab.getAttribute("aria-controls");
        panel.classList.toggle("is-active", active);
        panel.hidden = !active;
      });
    });
  });
}

function setupCopyButtons() {
  $("#copy-key").addEventListener("click", (event) => {
    if (currentKey) copyText(currentKey, event.currentTarget);
  });

  document.querySelectorAll(".copy-code").forEach((button) => {
    button.addEventListener("click", () => {
      const target = document.getElementById(button.dataset.copy);
      if (target) copyText(target.innerText, button);
    });
  });
}

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  if (busy || event.submitter?.id !== "generate") return;
  if (websiteInput.value.trim() || navigator.webdriver) {
    setStatus(formStatus, "Проверка не пройдена. Откройте страницу в обычном браузере.", "error");
    return;
  }
  if (!window.crypto?.subtle) {
    setStatus(formStatus, "Браузер не поддерживает Web Crypto. Откройте сайт по HTTPS.", "error");
    return;
  }
  busy = true;
  generateButton.disabled = true;
  const label = generateButton.querySelector(".btn-label");
  const originalLabel = label.textContent;
  label.textContent = "Создаю…";
  setStatus(formStatus, "", "");
  try {
    const payload = await createKey(nameInput.value.trim());
    showKey(payload);
    setStatus(formStatus, "Готово. Ключ создан.", "");
  } catch (error) {
    setStatus(formStatus, error.message || "Не удалось создать ключ.", "error");
  } finally {
    busy = false;
    generateButton.disabled = false;
    label.textContent = originalLabel;
  }
});

$("#check-key").addEventListener("click", async (event) => {
  if (!currentKey) return;
  const button = event.currentTarget;
  button.disabled = true;
  setStatus(keyStatus, "Проверяю ключ…", "muted");
  try {
    const data = await fetchJson("/keys/me", {
      headers: { Authorization: `Bearer ${currentKey}` },
    });
    renderQuota(data);
    setStatus(keyStatus, `Ключ работает. Модель: ${data.model || "devai-core-4.0"}.`, "");
  } catch (error) {
    setStatus(keyStatus, error.message || "Ключ не работает.", "error");
  } finally {
    button.disabled = false;
  }
});

upgradeButton.addEventListener("click", async () => {
  if (!currentKey) return;
  const code = upgradeCodeInput.value.trim();
  if (!code) {
    setStatus(quotaStatus, "Введите промокод.", "error");
    return;
  }
  upgradeButton.disabled = true;
  setStatus(quotaStatus, "Активирую Premium…", "muted");
  try {
    const data = await fetchJson("/keys/upgrade", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${currentKey}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ code }),
    });
    upgradeCodeInput.value = "";
    renderQuota(data);
    setStatus(keyStatus, `Premium активирован до ${formatDate(data.premium_until)}.`, "");
    await refreshQuota();
  } catch (error) {
    setStatus(quotaStatus, error.message || "Не удалось активировать промокод.", "error");
  } finally {
    upgradeButton.disabled = false;
  }
});

$("#download-key").addEventListener("click", downloadKey);

apiInput.addEventListener("change", () => {
  const base = normalizeBase(apiInput.value);
  apiInput.value = base;
  safeSet(STORAGE_KEY, base);
  updateSnippets();
  checkHealth();
});

$("#reset-base").addEventListener("click", () => {
  apiInput.value = DEFAULT_API;
  safeSet(STORAGE_KEY, DEFAULT_API);
  updateSnippets();
  checkHealth();
});

function init() {
  const saved = safeGet(STORAGE_KEY);
  apiInput.value = normalizeBase(saved || DEFAULT_API);
  updateSnippets();
  setupTabs();
  setupCopyButtons();
  checkHealth();
}

init();
