(() => {
  "use strict";

  const $ = (selector, root = document) => root.querySelector(selector);
  const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(pointer: fine)").matches;

  const store = {
    get(key, fallback) {
      try {
        const value = window.localStorage.getItem(key);
        return value === null ? fallback : value;
      } catch {
        return fallback;
      }
    },
    set(key, value) {
      try {
        window.localStorage.setItem(key, value);
      } catch {}
    }
  };

  const THEMES = {
    lime: { name: "Лайм", color: "#0b0f0c" },
    coral: { name: "Коралл", color: "#1a0710" },
    violet: { name: "Лила", color: "#130b24" },
    ocean: { name: "Океан", color: "#06151a" }
  };

  const SCENES = {
    space: { name: "Космос", desc: "Глубокие пэды и медленное дыхание" },
    rain: { name: "Дождь", desc: "Мягкий шум и редкие капли" },
    forest: { name: "Лес", desc: "Ветер, листва и далёкие птицы" },
    focus: { name: "Фокус", desc: "Бурый шум для глубокой работы" }
  };

  let toastTimer = 0;
  let soundOnChange = () => {};

  function toast(message) {
    const el = $("#toast");
    if (!el) return;
    el.textContent = message;
    el.classList.add("is-visible");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => el.classList.remove("is-visible"), 2400);
  }

  function setTheme(name, announce = true) {
    if (!THEMES[name]) return;
    document.documentElement.dataset.theme = name;
    const themeName = $("#theme-name");
    if (themeName) themeName.textContent = THEMES[name].name;
    $$("[data-theme-choice]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.themeChoice === name);
      button.setAttribute("aria-pressed", String(button.dataset.themeChoice === name));
    });
    const meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute("content", THEMES[name].color);
    store.set("devai.expressive.theme", name);
    if (announce) {
      toast("Тема: " + THEMES[name].name);
      Sound.uiTap("theme");
    }
  }

  function syncVolumeUI() {
    const value = Math.round(Sound.volume * 100);
    $$("[data-volume]").forEach((input) => {
      if (document.activeElement !== input) input.value = String(value);
    });
    $$("[data-volume-output]").forEach((output) => {
      output.textContent = value + "%";
    });
  }

  function syncSoundUI() {
    const playing = Sound.playing;
    document.body.classList.toggle("is-sounding", playing);
    $$("[data-sound-toggle]").forEach((button) => {
      button.classList.toggle("is-playing", playing);
      button.setAttribute("aria-pressed", String(playing));
      button.setAttribute("aria-label", playing ? "Поставить эмбиент на паузу" : "Включить эмбиент");
    });
    $$("[data-scene]").forEach((button) => {
      button.classList.toggle("is-active", button.dataset.scene === Sound.scene);
    });
    const meta = SCENES[Sound.scene];
    const sceneName = $("#scene-name");
    const sceneDesc = $("#scene-desc");
    const dockScene = $("#dock-scene");
    const soundState = $("#sound-state");
    if (sceneName) sceneName.textContent = meta.name;
    if (sceneDesc) sceneDesc.textContent = meta.desc;
    if (dockScene) dockScene.textContent = meta.name;
    if (soundState) soundState.textContent = playing ? "играет" : "выключен";
    const bigLabel = $(".sound-btn-label");
    if (bigLabel) bigLabel.textContent = playing ? "Пауза" : "Включить эмбиент";
    const uiSwitch = $("#ui-sounds");
    if (uiSwitch) uiSwitch.checked = Sound.ui;
    syncVolumeUI();
  }

  const Sound = (() => {
    const savedScene = store.get("devai.expressive.scene", "space");
    const savedVolume = Number(store.get("devai.expressive.volume", "35"));
    let scene = SCENES[savedScene] ? savedScene : "space";
    let volume = Number.isFinite(savedVolume) ? Math.min(1, Math.max(0, savedVolume / 100)) : 0.35;
    let ui = store.get("devai.expressive.ui", "1") !== "0";
    let playing = false;
    let ctx = null;
    let master = null;
    let uiGain = null;
    let dry = null;
    let reverb = null;
    let nodes = [];
    let timer = 0;

    function emit() {
      soundOnChange();
    }

    function add(node) {
      nodes.push(node);
      return node;
    }

    function gainNode(level) {
      const gain = ctx.createGain();
      gain.gain.value = level;
      return add(gain);
    }

    function biquad(type, frequency, q = 1) {
      const filter = ctx.createBiquadFilter();
      filter.type = type;
      filter.frequency.value = frequency;
      filter.Q.value = q;
      return add(filter);
    }

    function makeImpulse(seconds, decay) {
      const rate = ctx.sampleRate;
      const length = Math.floor(rate * seconds);
      const buffer = ctx.createBuffer(2, length, rate);
      for (let channel = 0; channel < 2; channel += 1) {
        const data = buffer.getChannelData(channel);
        for (let i = 0; i < length; i += 1) {
          data[i] = (Math.random() * 2 - 1) * Math.pow(1 - i / length, decay);
        }
      }
      return buffer;
    }

    function makeNoise(kind = "white") {
      const rate = ctx.sampleRate;
      const length = rate * 2;
      const buffer = ctx.createBuffer(1, length, rate);
      const data = buffer.getChannelData(0);
      let last = 0;
      for (let i = 0; i < length; i += 1) {
        const white = Math.random() * 2 - 1;
        if (kind === "brown") {
          last = (last + 0.02 * white) / 1.02;
          data[i] = last * 3.5;
        } else {
          data[i] = white * 0.7;
        }
      }
      return buffer;
    }

    function noiseSource(kind = "white") {
      const source = ctx.createBufferSource();
      source.buffer = makeNoise(kind);
      source.loop = true;
      return add(source);
    }

    function tone(type, frequency, level, destination, detune = 0) {
      const osc = ctx.createOscillator();
      const gain = gainNode(level);
      osc.type = type;
      osc.frequency.value = frequency;
      osc.detune.value = detune;
      osc.connect(gain).connect(destination);
      osc.start();
      return add(osc);
    }

    function makeBus(level = 0.6) {
      const bus = gainNode(level);
      bus.connect(dry);
      bus.connect(reverb);
      return bus;
    }

    function loop(fn, min, max) {
      const tick = () => {
        if (!playing) return;
        try {
          fn();
        } catch {}
        timer = window.setTimeout(tick, min + Math.random() * (max - min));
      };
      timer = window.setTimeout(tick, min);
    }

    function droplet(destination) {
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      const frequency = 1300 + Math.random() * 1800;
      osc.type = "sine";
      osc.frequency.setValueAtTime(frequency, now);
      osc.frequency.exponentialRampToValueAtTime(frequency * 0.55, now + 0.12);
      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(0.03, now + 0.008);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.2);
      osc.connect(gain).connect(destination);
      osc.start(now);
      osc.stop(now + 0.22);
    }

    function chirp(destination) {
      const base = 1700 + Math.random() * 900;
      const notes = 2 + Math.floor(Math.random() * 3);
      for (let i = 0; i < notes; i += 1) {
        const now = ctx.currentTime + i * 0.14;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = "sine";
        osc.frequency.setValueAtTime(base * (1 + i * 0.03), now);
        osc.frequency.exponentialRampToValueAtTime(base * 1.45, now + 0.08);
        gain.gain.setValueAtTime(0.0001, now);
        gain.gain.exponentialRampToValueAtTime(0.018, now + 0.01);
        gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.13);
        osc.connect(gain).connect(destination);
        osc.start(now);
        osc.stop(now + 0.15);
      }
    }

    function buildSpace() {
      const out = makeBus(0.6);
      const lowpass = biquad("lowpass", 460, 0.8);
      lowpass.connect(out);
      [55, 82.5, 110, 164.81].forEach((frequency, index) => {
        tone(index % 2 ? "sine" : "triangle", frequency, 0.075 / (index + 1), lowpass, (index - 1) * 4);
      });
      const lfo = ctx.createOscillator();
      const lfoGain = gainNode(150);
      lfo.frequency.value = 0.06;
      lfo.connect(lfoGain);
      lfoGain.connect(lowpass.frequency);
      lfo.start();
      add(lfo);
      const noise = noiseSource("brown");
      const bandpass = biquad("bandpass", 700, 0.6);
      const noiseGain = gainNode(0.02);
      noise.connect(bandpass).connect(noiseGain).connect(out);
      noise.start();
    }

    function buildRain() {
      const out = makeBus(0.65);
      const noise = noiseSource("white");
      const highpass = biquad("highpass", 900, 0.7);
      const lowpass = biquad("lowpass", 6800, 0.7);
      const noiseGain = gainNode(0.1);
      noise.connect(highpass).connect(lowpass).connect(noiseGain).connect(out);
      noise.start();
      tone("sine", 48, 0.03, out);
      loop(() => droplet(out), 120, 620);
    }

    function buildForest() {
      const out = makeBus(0.65);
      const wind = noiseSource("brown");
      const windFilter = biquad("bandpass", 520, 0.5);
      const windGain = gainNode(0.06);
      wind.connect(windFilter).connect(windGain).connect(out);
      wind.start();
      const leaves = noiseSource("white");
      const leavesFilter = biquad("highpass", 3200, 0.6);
      const leavesGain = gainNode(0.012);
      leaves.connect(leavesFilter).connect(leavesGain).connect(out);
      leaves.start();
      tone("sine", 70, 0.022, out);
      loop(() => chirp(out), 2400, 5600);
    }

    function buildFocus() {
      const out = makeBus(0.7);
      const noise = noiseSource("brown");
      const lowpass = biquad("lowpass", 340, 0.7);
      const noiseGain = gainNode(0.12);
      noise.connect(lowpass).connect(noiseGain).connect(out);
      noise.start();
      tone("sine", 60, 0.04, out);
      tone("sine", 120, 0.012, out);
      const lfo = ctx.createOscillator();
      const lfoGain = gainNode(70);
      lfo.frequency.value = 0.07;
      lfo.connect(lfoGain);
      lfoGain.connect(lowpass.frequency);
      lfo.start();
      add(lfo);
    }

    function buildScene() {
      if (scene === "rain") buildRain();
      else if (scene === "forest") buildForest();
      else if (scene === "focus") buildFocus();
      else buildSpace();
    }

    function ensure() {
      if (ctx) return ctx;
      const AudioCtor = window.AudioContext || window.webkitAudioContext;
      if (!AudioCtor) return null;
      ctx = new AudioCtor();
      master = ctx.createGain();
      master.gain.value = 0.0001;
      master.connect(ctx.destination);
      uiGain = ctx.createGain();
      uiGain.gain.value = 0.55;
      uiGain.connect(ctx.destination);
      dry = ctx.createGain();
      dry.gain.value = 0.8;
      dry.connect(master);
      const wet = ctx.createGain();
      wet.gain.value = 0.35;
      wet.connect(master);
      reverb = ctx.createConvolver();
      reverb.buffer = makeImpulse(2.6, 2.4);
      reverb.connect(wet);
      return ctx;
    }

    function clearScene() {
      if (timer) {
        window.clearTimeout(timer);
        timer = 0;
      }
      nodes.forEach((node) => {
        try {
          if (typeof node.stop === "function") node.stop();
        } catch {}
        try {
          node.disconnect();
        } catch {}
      });
      nodes = [];
    }

    function start(nextScene = scene) {
      if (!ensure()) {
        toast("Web Audio не поддерживается в этом браузере");
        return;
      }
      if (ctx.state === "suspended") ctx.resume();
      clearScene();
      scene = SCENES[nextScene] ? nextScene : "space";
      playing = true;
      buildScene();
      const now = ctx.currentTime;
      master.gain.cancelScheduledValues(now);
      master.gain.setValueAtTime(Math.max(master.gain.value, 0.0001), now);
      master.gain.exponentialRampToValueAtTime(Math.max(volume * 0.5, 0.0001), now + 1.1);
      store.set("devai.expressive.scene", scene);
      emit();
    }

    function stop() {
      if (!ctx) return;
      playing = false;
      const now = ctx.currentTime;
      master.gain.cancelScheduledValues(now);
      master.gain.setValueAtTime(Math.max(master.gain.value, 0.0001), now);
      master.gain.exponentialRampToValueAtTime(0.0001, now + 0.35);
      emit();
      window.setTimeout(() => {
        if (!playing) clearScene();
      }, 450);
    }

    function toggle() {
      if (playing) stop();
      else start(scene);
    }

    function selectScene(name) {
      if (!SCENES[name]) return;
      if (playing && name === scene) return;
      start(name);
    }

    function setVolume(value) {
      volume = Math.min(1, Math.max(0, value));
      if (ctx && playing) {
        const now = ctx.currentTime;
        master.gain.cancelScheduledValues(now);
        master.gain.setTargetAtTime(Math.max(volume * 0.5, 0.0001), now, 0.12);
      }
      store.set("devai.expressive.volume", String(Math.round(volume * 100)));
      syncVolumeUI();
    }

    function setUi(value) {
      ui = Boolean(value);
      store.set("devai.expressive.ui", ui ? "1" : "0");
      if (ui) {
        ensure();
        if (ctx && ctx.state === "suspended") ctx.resume();
        uiTap("toggle");
      }
      emit();
    }

    function uiTap(kind = "tap") {
      if (!ui || !ctx) return;
      const now = ctx.currentTime;
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      const frequency = kind === "theme" ? 760 : kind === "toggle" ? 560 : 480;
      osc.type = "sine";
      osc.frequency.setValueAtTime(frequency, now);
      osc.frequency.exponentialRampToValueAtTime(frequency * (kind === "theme" ? 1.4 : 0.8), now + 0.1);
      gain.gain.setValueAtTime(0.0001, now);
      gain.gain.exponentialRampToValueAtTime(0.04, now + 0.008);
      gain.gain.exponentialRampToValueAtTime(0.0001, now + 0.16);
      osc.connect(gain).connect(uiGain);
      osc.start(now);
      osc.stop(now + 0.18);
    }

    document.addEventListener("visibilitychange", () => {
      if (!ctx) return;
      if (document.hidden) ctx.suspend();
      else if (playing) ctx.resume();
    });

    return {
      get playing() { return playing; },
      get scene() { return scene; },
      get volume() { return volume; },
      get ui() { return ui; },
      toggle,
      start,
      stop,
      selectScene,
      setVolume,
      setUi,
      uiTap
    };
  })();

  soundOnChange = syncSoundUI;

  function initTheme() {
    const saved = store.get("devai.expressive.theme", document.documentElement.dataset.theme || "lime");
    setTheme(THEMES[saved] ? saved : "lime", false);
  }

  function initSound() {
    $$("[data-sound-toggle]").forEach((button) => {
      button.addEventListener("click", () => Sound.toggle());
    });
    $$("[data-scene]").forEach((button) => {
      button.addEventListener("click", () => Sound.selectScene(button.dataset.scene));
    });
    $$("[data-theme-choice]").forEach((button) => {
      button.addEventListener("click", () => setTheme(button.dataset.themeChoice));
    });
    $$("[data-volume]").forEach((input) => {
      input.addEventListener("input", () => Sound.setVolume(Number(input.value) / 100));
    });
    const uiSwitch = $("#ui-sounds");
    if (uiSwitch) {
      uiSwitch.addEventListener("change", () => Sound.setUi(uiSwitch.checked));
    }
    const dockMore = $("#dock-more");
    const dockPanel = $("#dock-panel");
    if (dockMore && dockPanel) {
      dockMore.addEventListener("click", (event) => {
        event.stopPropagation();
        const open = dockPanel.hidden;
        dockPanel.hidden = !open;
        dockMore.setAttribute("aria-expanded", String(open));
        Sound.uiTap("toggle");
      });
      document.addEventListener("click", (event) => {
        if (!event.target.closest(".sound-dock") && !dockPanel.hidden) {
          dockPanel.hidden = true;
          dockMore.setAttribute("aria-expanded", "false");
        }
      });
    }
    document.addEventListener("click", (event) => {
      const target = event.target.closest("button, a.btn");
      if (!target || target.matches("[data-sound-toggle], [data-theme-choice]")) return;
      Sound.uiTap("tap");
    });
    syncSoundUI();
  }

  function initToastLinks() {
    $$("[data-toast]").forEach((link) => {
      link.addEventListener("click", () => toast(link.dataset.toast));
    });
  }

  function initRipples() {
    $$(".ripple").forEach((element) => {
      element.addEventListener("pointerdown", (event) => {
        const rect = element.getBoundingClientRect();
        const size = Math.max(rect.width, rect.height) * 2;
        const dot = document.createElement("span");
        dot.className = "ripple-dot";
        dot.style.width = size + "px";
        dot.style.height = size + "px";
        dot.style.left = event.clientX - rect.left - size / 2 + "px";
        dot.style.top = event.clientY - rect.top - size / 2 + "px";
        element.append(dot);
        window.setTimeout(() => dot.remove(), 700);
      });
    });
  }

  function initMagnetic() {
    if (reduced || !finePointer) return;
    $$(".magnetic").forEach((element) => {
      element.addEventListener("pointermove", (event) => {
        const rect = element.getBoundingClientRect();
        const x = (event.clientX - rect.left - rect.width / 2) * 0.12;
        const y = (event.clientY - rect.top - rect.height / 2) * 0.18;
        element.style.transform = "translate(" + x + "px, " + y + "px)";
      });
      element.addEventListener("pointerleave", () => {
        element.style.transform = "";
      });
    });
  }

  function initParallax() {
    const hero = $(".hero");
    if (!hero || reduced || !finePointer) return;
    hero.addEventListener("pointermove", (event) => {
      const rect = hero.getBoundingClientRect();
      const x = (event.clientX - rect.left) / rect.width - 0.5;
      const y = (event.clientY - rect.top) / rect.height - 0.5;
      hero.style.setProperty("--mx", x.toFixed(3));
      hero.style.setProperty("--my", y.toFixed(3));
    });
    hero.addEventListener("pointerleave", () => {
      hero.style.setProperty("--mx", "0");
      hero.style.setProperty("--my", "0");
    });
  }

  function initReveal() {
    const elements = $$(".reveal");
    if (reduced || !("IntersectionObserver" in window)) {
      elements.forEach((element) => element.classList.add("is-in"));
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        entry.target.classList.add("is-in");
        observer.unobserve(entry.target);
      });
    }, { threshold: 0.12, rootMargin: "0px 0px -8% 0px" });
    elements.forEach((element, index) => {
      element.style.setProperty("--delay", Math.min(index % 6, 5) * 65 + "ms");
      observer.observe(element);
    });
  }

  function initCounters() {
    const elements = $$("[data-count]");
    const show = (element) => {
      const target = Number(element.dataset.count) || 0;
      const suffix = element.dataset.suffix || "";
      if (reduced) {
        element.textContent = target + suffix;
        return;
      }
      const started = performance.now();
      const duration = 1200;
      const frame = (now) => {
        const progress = Math.min(1, (now - started) / duration);
        const eased = 1 - Math.pow(1 - progress, 3);
        element.textContent = Math.round(target * eased) + suffix;
        if (progress < 1) requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    };
    if (!("IntersectionObserver" in window)) {
      elements.forEach(show);
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (!entry.isIntersecting) return;
        show(entry.target);
        observer.unobserve(entry.target);
      });
    }, { threshold: 0.5 });
    elements.forEach((element) => observer.observe(element));
  }

  function initScroll() {
    const bar = $("#scroll-bar");
    if (!bar) return;
    const update = () => {
      const max = document.documentElement.scrollHeight - window.innerHeight;
      const progress = max > 0 ? Math.min(1, Math.max(0, window.scrollY / max)) : 0;
      bar.style.transform = "scaleX(" + progress.toFixed(4) + ")";
    };
    window.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
    update();
  }

  initTheme();
  initSound();
  initToastLinks();
  initRipples();
  initMagnetic();
  initParallax();
  initReveal();
  initCounters();
  initScroll();
})();
