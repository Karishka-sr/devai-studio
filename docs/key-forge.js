/* The animated DevAI key-forge ceremony used by api.html. */
(() => {
  if (window.DevAIKeyForge) return;

  const $ = (id) => document.getElementById(id);
  const root = $("key-theater");
  if (!root) return;

  const status = $("forge-status");
  const phase = $("forge-phase");
  const percent = $("forge-percent");
  const fill = $("forge-meter-fill");
  const log = $("forge-log");
  const reveal = $("forge-reveal");
  const keyNode = $("forge-key");
  const arm = $("forge-arm");
  const confetti = $("forge-confetti");
  const cancel = $("forge-cancel");
  const close = $("forge-close");
  const closeReveal = $("forge-close-reveal");
  const copy = $("forge-copy");
  const download = $("forge-download");
  const chips = [...document.querySelectorAll("[data-forge-step]")];
  const timers = [];
  let currentKey = "";
  let opened = false;
  let finished = false;
  let startedAt = 0;

  function later(fn, ms) {
    const id = window.setTimeout(fn, ms);
    timers.push(id);
    return id;
  }

  function clearTimers() {
    while (timers.length) window.clearTimeout(timers.pop());
  }

  function setProgress(value) {
    const safe = Math.max(0, Math.min(100, Math.round(value)));
    fill.style.width = safe + "%";
    percent.textContent = safe + "%";
  }

  function addLog(text) {
    const line = document.createElement("p");
    line.textContent = text;
    log.appendChild(line);
    while (log.children.length > 4) log.firstElementChild.remove();
  }

  function chip(step) {
    return chips.find((item) => item.dataset.forgeStep === step);
  }

  function press(step) {
    const button = chip(step);
    if (!button) return;
    button.classList.remove("is-pressed");
    void button.offsetWidth;
    button.classList.add("is-active", "is-pressed");
    arm.classList.remove("is-clicking");
    void arm.offsetWidth;
    arm.classList.add("is-clicking");
    later(() => {
      button.classList.remove("is-pressed");
      arm.classList.remove("is-clicking");
    }, 430);
  }

  function complete(step) {
    const button = chip(step);
    if (button) {
      button.classList.remove("is-active");
      button.classList.add("is-done");
    }
  }

  function stage(name) {
    if (!opened) return;
    if (name === "challenge") {
      phase.textContent = "SCAN";
      status.textContent = "DevAI проверяет сервер и готовит защитную задачу…";
      press("scan");
      setProgress(18);
      addLog("server handshake ok");
      addLog("browser fingerprint: local");
    } else if (name === "solving") {
      complete("scan");
      phase.textContent = "PROOF";
      status.textContent = "DevAI решает защитную задачу. Это доказывает, что вы не бот.";
      press("solve");
      setProgress(46);
      addLog("sha-256 proof in progress");
    } else if (name === "register") {
      complete("solve");
      phase.textContent = "REGISTER";
      status.textContent = "Задача решена. DevAI собирает ключ в защищённом хранилище…";
      press("register");
      setProgress(76);
      addLog("challenge accepted");
      addLog("minting trial key");
    } else if (name === "retry") {
      phase.textContent = "RETRY";
      status.textContent = "Задача устарела. DevAI пробует ещё раз…";
      press("solve");
      setProgress(34);
      addLog("nonce expired, retrying");
    } else if (name === "error") {
      phase.textContent = "ERROR";
      setProgress(100);
      addLog("forge failed");
    }
  }

  function burst() {
    const colors = ["#c8ff3d", "#65f5c8", "#ffb86b", "#ff7ab6", "#f4f1e8"];
    confetti.replaceChildren();
    for (let index = 0; index < 70; index += 1) {
      const piece = document.createElement("i");
      piece.style.setProperty("--x", Math.random() * 100 + "%");
      piece.style.setProperty("--w", 4 + Math.random() * 7 + "px");
      piece.style.setProperty("--h", 7 + Math.random() * 12 + "px");
      piece.style.setProperty("--c", colors[index % colors.length]);
      piece.style.setProperty("--d", 1.5 + Math.random() * 1.7 + "s");
      piece.style.setProperty("--delay", Math.random() * 0.35 + "s");
      piece.style.setProperty("--drift", (Math.random() - 0.5) * 240 + "px");
      confetti.appendChild(piece);
    }
    later(() => confetti.replaceChildren(), 3600);
  }

  function reset() {
    clearTimers();
    opened = true;
    finished = false;
    startedAt = performance.now();
    currentKey = "";
    reveal.hidden = true;
    keyNode.textContent = "";
    status.textContent = "Подключаюсь к DevAI Core…";
    phase.textContent = "READY";
    setProgress(4);
    log.replaceChildren();
    addLog("boot sequence");
    addLog("secure channel ready");
    chips.forEach((item) => item.classList.remove("is-active", "is-done", "is-pressed"));
    arm.classList.remove("is-clicking");
    confetti.replaceChildren();
    cancel.textContent = "Отменить";
    document.body.classList.add("forge-lock");
    later(() => {
      if (!finished) status.textContent = "DevAI открывает окно создания ключа…";
    }, 420);
  }

  function open() {
    reset();
    root.hidden = false;
    root.classList.add("is-open");
    later(() => $("forge-close").focus(), 60);
  }

  function closeTheater() {
    clearTimers();
    root.hidden = true;
    root.classList.remove("is-open");
    document.body.classList.remove("forge-lock");
    opened = false;
  }

  function success(payload) {
    if (!opened) {
      finished = true;
      return;
    }
    finished = true;
    clearTimers();
    complete("scan");
    complete("solve");
    complete("register");
    currentKey = payload?.key || "";
    keyNode.textContent = currentKey || "Ключ не пришёл";
    const wait = Math.max(700, 4200 - (performance.now() - startedAt));
    phase.textContent = "SIGN";
    status.textContent = "DevAI проверяет подпись, ставит печать и полирует ключ…";
    setProgress(88);
    addLog("verifying signature");
    later(() => addLog("sealing trial key"), 480);
    later(() => {
      if (!opened) return;
      press("key");
      complete("key");
      phase.textContent = "ACTIVE";
      status.textContent = "Готово. DevAI создал пробный ключ — заберите его ниже.";
      setProgress(100);
      addLog("trial key created");
      addLog("sha-256 stored, plaintext shown once");
    }, wait);
    later(() => {
      if (!opened) return;
      reveal.hidden = false;
      reveal.scrollIntoView({ behavior: "smooth", block: "nearest" });
      burst();
      cancel.textContent = "Закрыть";
      $("forge-copy").focus();
    }, wait + 450);
  }

  function fail(error) {
    finished = true;
    if (!opened) return;
    phase.textContent = "ERROR";
    status.textContent = (error?.message || "Не удалось создать ключ.") + " Можно закрыть окно и попробовать снова.";
    setProgress(100);
    addLog("request failed");
    cancel.textContent = "Закрыть";
  }

  async function copyKey() {
    if (!currentKey) return;
    const label = copy.textContent;
    try {
      await navigator.clipboard.writeText(currentKey);
    } catch {
      const area = document.createElement("textarea");
      area.value = currentKey;
      area.style.position = "fixed";
      area.style.opacity = "0";
      document.body.appendChild(area);
      area.select();
      document.execCommand("copy");
      area.remove();
    }
    copy.textContent = "Скопировано ✓";
    later(() => {
      copy.textContent = label;
    }, 1600);
  }

  close.addEventListener("click", closeTheater);
  cancel.addEventListener("click", closeTheater);
  closeReveal.addEventListener("click", closeTheater);
  copy.addEventListener("click", copyKey);
  download.addEventListener("click", () => $("download-key")?.click());
  root.addEventListener("click", (event) => {
    if (event.target === root) closeTheater();
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !root.hidden) closeTheater();
  });

  window.DevAIKeyForge = { open, stage, success, fail, close: closeTheater };
})();
