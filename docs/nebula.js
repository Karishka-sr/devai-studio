/* DEVAI NEBULA — shared motion and atmosphere for every DevAI page. */
(() => {
  if (window.__DEVAI_NEBULA__) return;
  window.__DEVAI_NEBULA__ = true;

  const root = document.documentElement;
  const body = document.body;
  if (!body) return;

  root.classList.add("dv-js");
  const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  const finePointer = window.matchMedia("(pointer: fine)").matches;

  function node(tag, className = "", parent = body) {
    const element = document.createElement(tag);
    if (className) element.className = className;
    parent.appendChild(element);
    return element;
  }

  function makeAtmosphere() {
    if (document.querySelector(".dv-aurora")) return;
    const aurora = node("div", "dv-aurora");
    aurora.setAttribute("aria-hidden", "true");
    aurora.append(node("i", "", aurora), node("i", "", aurora), node("i", "", aurora));

    const grid = node("div", "dv-grid");
    grid.setAttribute("aria-hidden", "true");

    const stars = node("div", "dv-stars");
    stars.setAttribute("aria-hidden", "true");
    const colors = ["#f7f3e8", "#d8ff5c", "#75e7ff", "#b9a2ff", "#ffd166"];
    for (let index = 0; index < 64; index += 1) {
      const star = document.createElement("i");
      star.style.setProperty("--x", Math.random() * 100 + "%");
      star.style.setProperty("--y", Math.random() * 100 + "%");
      star.style.setProperty("--s", 1 + Math.random() * 2.4 + "px");
      star.style.setProperty("--d", 3 + Math.random() * 6 + "s");
      star.style.setProperty("--delay", -Math.random() * 8 + "s");
      star.style.setProperty("--o", 0.25 + Math.random() * 0.5);
      star.style.setProperty("--c", colors[index % colors.length]);
      stars.appendChild(star);
    }
  }

  function makeLoader() {
    if (reduced || document.querySelector(".dv-loader")) return;
    const loader = node("div", "dv-loader");
    loader.setAttribute("aria-hidden", "true");
    loader.innerHTML = '<div class="dv-loader-mark"><i></i><span>DEVAI / NEBULA</span></div>';
    const started = performance.now();
    const hide = () => {
      const wait = Math.max(0, 520 - (performance.now() - started));
      window.setTimeout(() => {
        loader.classList.add("is-done");
        window.setTimeout(() => loader.remove(), 900);
      }, wait);
    };
    if (document.readyState === "complete") hide();
    else window.addEventListener("load", hide, { once: true });
    window.setTimeout(hide, 2200);
  }

  function makeProgress() {
    if (document.querySelector(".dv-progress")) return;
    const bar = node("div", "dv-progress");
    bar.setAttribute("aria-hidden", "true");
    const update = () => {
      const scrollable = document.documentElement.scrollHeight - window.innerHeight;
      const next = scrollable > 0 ? Math.min(1, Math.max(0, window.scrollY / scrollable)) : 0;
      bar.style.setProperty("--dv-progress", next.toFixed(4));
    };
    window.addEventListener("scroll", update, { passive: true });
    window.addEventListener("resize", update);
    update();
  }

  function makeCursor() {
    if (!finePointer || reduced || document.querySelector(".cursor-ring")) return;
    const ring = node("div", "dv-cursor");
    const dot = node("div", "dv-cursor-dot");
    ring.setAttribute("aria-hidden", "true");
    dot.setAttribute("aria-hidden", "true");
    body.classList.add("dv-pointer");
    let targetX = -100;
    let targetY = -100;
    let currentX = -100;
    let currentY = -100;
    let frame = 0;
    const move = (event) => {
      targetX = event.clientX;
      targetY = event.clientY;
      if (!frame) frame = requestAnimationFrame(tick);
    };
    const tick = () => {
      currentX += (targetX - currentX) * 0.18;
      currentY += (targetY - currentY) * 0.18;
      ring.style.setProperty("--cx", currentX.toFixed(1) + "px");
      ring.style.setProperty("--cy", currentY.toFixed(1) + "px");
      dot.style.setProperty("--cx", targetX.toFixed(1) + "px");
      dot.style.setProperty("--cy", targetY.toFixed(1) + "px");
      frame = Math.abs(targetX - currentX) + Math.abs(targetY - currentY) > 0.4 ? requestAnimationFrame(tick) : 0;
    };
    window.addEventListener("pointermove", move, { passive: true });
    document.addEventListener("pointerover", (event) => {
      const hot = event.target.closest("a, button, .btn, .panel, .bento-card, .template");
      ring.classList.toggle("is-hot", Boolean(hot));
    });
  }

  function makeReveal() {
    if (reduced) return;
    const selector = [
      "main > section",
      "main > .section",
      ".hero-copy",
      ".device-stage",
      ".terminal",
      ".hero-art",
      ".section-head",
      ".bento-card",
      ".feature-card",
      ".how-card",
      ".plan-card",
      ".plan",
      ".checkout-card",
      ".download-card",
      ".panel",
      ".card",
      ".template",
      ".pg-rail",
      ".pg-editor",
      ".pg-side",
      ".preview-pane",
      ".faq-list details",
    ].join(",");
    const elements = [...document.querySelectorAll(selector)].filter((element) => {
      return !element.closest(".key-theater") && !element.classList.contains("dv-reveal");
    });
    elements.forEach((element, index) => {
      element.classList.add("dv-reveal");
      if (index % 3 === 1) element.dataset.dv = "left";
      if (index % 3 === 2) element.dataset.dv = "right";
      element.style.setProperty("--dv-delay", Math.min(260, (index % 5) * 45) + "ms");
    });
    if (!("IntersectionObserver" in window)) {
      elements.forEach((element) => element.classList.add("is-in"));
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-in");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.06, rootMargin: "0px 0px -40px" });
    elements.forEach((element) => observer.observe(element));
  }

  function makeSpotlights() {
    if (reduced) return;
    const selector = ".bento-card, .feature-card, .how-card, .plan-card, .panel, .card, .download-card, .pg-rail, .pg-editor, .pg-side, .template, .checkout-card";
    document.querySelectorAll(selector).forEach((element) => {
      if (element.closest(".key-theater")) return;
      element.classList.add("dv-spot");
      element.addEventListener("pointermove", (event) => {
        const rect = element.getBoundingClientRect();
        element.style.setProperty("--dv-x", (event.clientX - rect.left).toFixed(0) + "px");
        element.style.setProperty("--dv-y", (event.clientY - rect.top).toFixed(0) + "px");
      }, { passive: true });
    });
  }

  function makeTilt() {
    if (!finePointer || reduced) return;
    document.querySelectorAll(".bento-card, .plan-card, .download-card, .feature-card").forEach((element) => {
      if (element.closest(".key-theater")) return;
      element.addEventListener("pointermove", (event) => {
        const rect = element.getBoundingClientRect();
        const x = (event.clientX - rect.left) / rect.width - 0.5;
        const y = (event.clientY - rect.top) / rect.height - 0.5;
        element.style.setProperty("--dv-rx", (-y * 1.3).toFixed(2) + "deg");
        element.style.setProperty("--dv-ry", (x * 1.6).toFixed(2) + "deg");
      }, { passive: true });
      element.addEventListener("pointerleave", () => {
        element.style.setProperty("--dv-rx", "0deg");
        element.style.setProperty("--dv-ry", "0deg");
      });
    });
  }

  function makeRipples() {
    if (reduced) return;
    const selector = ".btn, .pg-btn, .top-cta, .plan-pick, .rail-link, .header-api, .ghost";
    document.addEventListener("pointerdown", (event) => {
      const target = event.target.closest(selector);
      if (!target || target.disabled) return;
      const rect = target.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "dv-ripple";
      ripple.style.left = event.clientX - rect.left + "px";
      ripple.style.top = event.clientY - rect.top + "px";
      ripple.style.width = ripple.style.height = Math.max(rect.width, rect.height) + "px";
      target.appendChild(ripple);
      window.setTimeout(() => ripple.remove(), 700);
    }, { passive: true });
  }

  function makeBackToTop() {
    if (document.querySelector(".dv-top")) return;
    const button = document.createElement("button");
    button.type = "button";
    button.className = "dv-top";
    button.setAttribute("aria-label", "Наверх");
    button.textContent = "↑";
    body.appendChild(button);
    button.addEventListener("click", () => window.scrollTo({ top: 0, behavior: reduced ? "auto" : "smooth" }));
    const update = () => button.classList.toggle("is-visible", window.scrollY > 620);
    window.addEventListener("scroll", update, { passive: true });
    update();
  }

  function makePageTransition() {
    if (reduced) return;
    document.addEventListener("click", (event) => {
      const link = event.target.closest("a[href]");
      if (!link || event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
      if (link.target && link.target !== "_self") return;
      if (link.hasAttribute("download") || link.getAttribute("href")?.startsWith("#")) return;
      let url;
      try {
        url = new URL(link.href);
      } catch {
        return;
      }
      if (url.origin !== location.origin || url.pathname === location.pathname) return;
      body.style.transition = "opacity 220ms ease";
      body.style.opacity = "0";
    });
  }

  window.addEventListener("pageshow", () => {
    body.style.opacity = "";
    body.style.transition = "";
  });

  makeAtmosphere();
  makeLoader();
  makeProgress();
  makeCursor();
  makeReveal();
  makeSpotlights();
  makeTilt();
  makeRipples();
  makeBackToTop();
  makePageTransition();
  body.classList.add("dv-ready");
})();
