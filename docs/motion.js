(() => {
  const root = document.documentElement;
  const reduce = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (reduce) return;

  root.classList.add("motion-on");

  const all = (selector, scope = document) => Array.from(scope.querySelectorAll(selector));
  const one = (selector, scope = document) => scope.querySelector(selector);
  const finePointer = window.matchMedia("(pointer: fine)").matches;

  function make(tag, className, parent = document.body) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    parent.append(node);
    return node;
  }

  /* Reading progress and scroll parallax. */
  const progress = make("div", "motion-progress");
  progress.setAttribute("aria-hidden", "true");

  const aura = make("div", "motion-aura");
  aura.setAttribute("aria-hidden", "true");
  aura.innerHTML = "<i></i><i></i><i></i>";
  document.body.insertBefore(aura, document.body.firstChild);

  const headers = all(".site-header, .topbar");
  const navLinks = all('.site-nav a[href^="#"]');
  const sections = all("main section[id]");
  let ticking = false;

  function updateScroll() {
    const y = window.scrollY || window.pageYOffset || 0;
    const max = Math.max(1, document.documentElement.scrollHeight - window.innerHeight);
    root.style.setProperty("--scroll-progress", Math.min(1, y / max).toFixed(4));
    root.style.setProperty("--scroll-y", y.toFixed(1));
    headers.forEach((header) => header.classList.toggle("is-scrolled", y > 24));

    let active = "";
    sections.forEach((section) => {
      if (section.getBoundingClientRect().top <= 180) active = section.id;
    });
    navLinks.forEach((link) => {
      link.classList.toggle("is-active", link.getAttribute("href") === `#${active}`);
    });
    ticking = false;
  }

  function requestScroll() {
    if (ticking) return;
    ticking = true;
    window.requestAnimationFrame(updateScroll);
  }

  window.addEventListener("scroll", requestScroll, { passive: true });
  window.addEventListener("resize", requestScroll, { passive: true });
  updateScroll();

  /* Cursor light. */
  let cursor = null;
  if (finePointer) {
    cursor = make("div", "motion-cursor");
    cursor.setAttribute("aria-hidden", "true");
    let cursorFrame = 0;
    document.addEventListener("pointermove", (event) => {
      if (cursorFrame) return;
      cursorFrame = window.requestAnimationFrame(() => {
        cursor.style.setProperty("--cursor-x", `${event.clientX}px`);
        cursor.style.setProperty("--cursor-y", `${event.clientY}px`);
        cursor.classList.add("is-on");
        cursorFrame = 0;
      });
    }, { passive: true });
    document.addEventListener("pointerleave", () => cursor.classList.remove("is-on"));
  }

  /* Floating particles in the hero. */
  all(".hero").forEach((hero) => {
    const layer = document.createElement("div");
    layer.className = "motion-particles";
    layer.setAttribute("aria-hidden", "true");
    for (let index = 0; index < 18; index += 1) {
      const particle = document.createElement("i");
      const duration = 9 + Math.random() * 12;
      particle.style.setProperty("--x", `${4 + Math.random() * 92}%`);
      particle.style.setProperty("--size", `${1.5 + Math.random() * 3.5}px`);
      particle.style.setProperty("--duration", `${duration}s`);
      particle.style.setProperty("--delay", `${-Math.random() * duration}s`);
      particle.style.setProperty("--drift", `${-50 + Math.random() * 100}px`);
      layer.append(particle);
    }
    hero.prepend(layer);
  });

  /* Split headings into animated characters. */
  function splitHeading(heading, index) {
    if (!heading || heading.dataset.motionSplit === "true") return;
    heading.dataset.motionSplit = "true";
    heading.setAttribute("aria-label", heading.textContent.replace(/\s+/g, " ").trim());
    let wordIndex = 0;
    const walker = document.createTreeWalker(heading, NodeFilter.SHOW_TEXT);
    const nodes = [];
    while (walker.nextNode()) nodes.push(walker.currentNode);

    nodes.forEach((node) => {
      const value = node.textContent || "";
      if (!value) return;
      const fragment = document.createDocumentFragment();
      const tokens = value.match(/\s+|[^\s]+/g) || [];
      tokens.forEach((token) => {
        if (/^\s+$/.test(token)) {
          fragment.append(document.createTextNode(token));
          return;
        }
        const word = document.createElement("span");
        word.className = "motion-word";
        word.setAttribute("aria-hidden", "true");
        word.textContent = token;
        word.style.setProperty("--word-index", String(wordIndex));
        wordIndex += 1;
        fragment.append(word);
      });
      node.replaceWith(fragment);
    });

    heading.classList.add("motion-split");
    heading.style.setProperty("--split-delay", `${Math.min(index * 90, 360)}ms`);
  }

  const headings = all("h1, h2").filter((heading) => !heading.closest(".phone"));
  headings.forEach(splitHeading);

  /* Scroll reveal. */
  const revealSelector = [
    ".section-head",
    ".bento-card",
    ".why-intro",
    ".why-item",
    ".release-main",
    ".release-side",
    ".step",
    ".faq-intro",
    ".faq-list details",
    ".site-footer .footer-brand",
    ".site-footer .footer-links",
    ".footer > *",
    ".key-grid > *",
    ".code-shell",
    ".cards > .card",
    ".cta > *",
    ".download-shell > *",
  ].join(",");

  const revealTargets = all(revealSelector).filter((node) => !node.closest(".phone"));
  revealTargets.forEach((node, index) => {
    node.classList.add("motion-reveal");
    node.style.setProperty("--reveal-delay", `${Math.min((index % 4) * 75, 225)}ms`);
    if (index % 7 === 2) node.dataset.motion = "left";
    if (index % 7 === 5) node.dataset.motion = "right";
    if (index % 9 === 4) node.dataset.motion = "scale";
  });

  /* Animated counters. */
  function prepareCounter(node) {
    const raw = (node.textContent || "").trim();
    const match = raw.match(/^(\d[\d\s]*)(.*)$/);
    if (!match) return;
    const target = Number(match[1].replace(/\s/g, ""));
    if (!Number.isFinite(target) || target < 2) return;
    node.dataset.countTarget = String(target);
    node.dataset.countSuffix = match[2] || "";
    node.textContent = `0${match[2] || ""}`;
  }

  all(".stats dt, [data-count]").forEach(prepareCounter);

  function runCounter(node) {
    if (node.dataset.counted === "true") return;
    const target = Number(node.dataset.countTarget);
    if (!Number.isFinite(target)) return;
    node.dataset.counted = "true";
    const suffix = node.dataset.countSuffix || "";
    const start = performance.now();
    const duration = 1200;
    function frame(now) {
      const progressValue = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progressValue, 3);
      node.textContent = `${Math.round(target * eased)}${suffix}`;
      if (progressValue < 1) window.requestAnimationFrame(frame);
    }
    window.requestAnimationFrame(frame);
  }

  const observer = "IntersectionObserver" in window
    ? new IntersectionObserver((entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          entry.target.classList.add("is-visible");
          if (entry.target.dataset.countTarget) runCounter(entry.target);
          observer.unobserve(entry.target);
        });
      }, { threshold: 0.12, rootMargin: "0px 0px -6% 0px" })
    : null;

  if (observer) {
    revealTargets.forEach((node) => observer.observe(node));
    headings.forEach((heading) => observer.observe(heading));
    all("[data-count-target]").forEach((node) => observer.observe(node));
  } else {
    revealTargets.forEach((node) => node.classList.add("is-visible"));
    headings.forEach((heading) => heading.classList.add("is-visible"));
    all("[data-count-target]").forEach(runCounter);
  }

  /* Magnetic buttons and link glow. */
  all(".btn, .header-api, .site-nav a, .ghost-link, .tab").forEach((node) => {
    node.classList.add("motion-magnetic");
    if (!finePointer) return;
    node.addEventListener("pointermove", (event) => {
      const rect = node.getBoundingClientRect();
      node.style.setProperty("--mx", `${(event.clientX - rect.left - rect.width / 2) * 0.09}px`);
      node.style.setProperty("--my", `${(event.clientY - rect.top - rect.height / 2) * 0.12}px`);
    });
    node.addEventListener("pointerleave", () => {
      node.style.setProperty("--mx", "0px");
      node.style.setProperty("--my", "0px");
    });
  });

  all(".btn, .header-api").forEach((node) => node.classList.add("motion-shine"));

  /* Tilt and pointer spotlight. */
  const tiltTargets = all(".bento-card, .step, .card, .release-side");
  tiltTargets.forEach((node) => {
    node.classList.add("motion-spotlight");
    if (node.matches(".card, .step, .release-side")) node.classList.add("motion-border");
    if (!finePointer) return;
    node.classList.add("motion-tilt");
    node.addEventListener("pointermove", (event) => {
      const rect = node.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;
      node.style.setProperty("--spot-x", `${(x / rect.width) * 100}%`);
      node.style.setProperty("--spot-y", `${(y / rect.height) * 100}%`);
      node.style.setProperty("--tilt-y", `${((x / rect.width) - 0.5) * 5}deg`);
      node.style.setProperty("--tilt-x", `${(0.5 - (y / rect.height)) * 5}deg`);
    });
    node.addEventListener("pointerleave", () => {
      node.style.setProperty("--tilt-x", "0deg");
      node.style.setProperty("--tilt-y", "0deg");
    });
  });

  /* Ripple on primary controls. */
  all(".btn").forEach((node) => {
    node.addEventListener("pointerdown", (event) => {
      const rect = node.getBoundingClientRect();
      const ripple = document.createElement("span");
      ripple.className = "motion-ripple";
      ripple.style.left = `${event.clientX - rect.left}px`;
      ripple.style.top = `${event.clientY - rect.top}px`;
      node.append(ripple);
      window.setTimeout(() => ripple.remove(), 680);
    });
  });

  /* Soft page transition for internal navigation. */
  document.addEventListener("click", (event) => {
    const link = event.target.closest("a[href]");
    if (!link || event.defaultPrevented || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
    if (link.target || link.hasAttribute("download")) return;
    let url;
    try {
      url = new URL(link.href, location.href);
    } catch {
      return;
    }
    if (url.origin !== location.origin) return;
    if (url.pathname === location.pathname && url.hash) return;
    if (url.pathname === location.pathname && !url.hash) return;
    if (!/\.html?$/.test(url.pathname) && url.pathname !== "/") return;
    event.preventDefault();
    document.body.classList.add("motion-leaving");
    window.setTimeout(() => { location.href = link.href; }, 240);
  });

  /* Never leave content hidden if an observer or animation fails. */
  window.setTimeout(() => {
    all(".motion-reveal, .motion-split").forEach((node) => node.classList.add("is-visible"));
    all("[data-count-target]").forEach(runCounter);
  }, 3600);
})();
