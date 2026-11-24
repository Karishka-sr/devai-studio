const TELEGRAM_USERNAME = "FemClaudeMrrr";
const TELEGRAM_URL = `https://t.me/${TELEGRAM_USERNAME}`;

const PLANS = {
  week: { title: "Неделя", days: "7 дней", stars: 75, label: "7 дней" },
  month: { title: "Месяц", days: "30 дней", stars: 249, label: "30 дней" },
  year: { title: "Год", days: "365 дней", stars: 1490, label: "365 дней" },
  lifetime: { title: "Навсегда", days: "навсегда", stars: 4990, label: "Навсегда" },
};

const TIER_RANK = { week: 1, month: 2, year: 3, lifetime: 4 };
const TIER_LABEL = { week: "С «Недели»", month: "С «Месяца»", year: "С «Года»", lifetime: "Навсегда" };

const FEATURES = [
  { tier: "week", title: "Expressive-тема", desc: "Крупная типографика и живая геометрия интерфейса." },
  { tier: "week", title: "Локальные модели 1.2B", desc: "LFM2.5 1.2B, TinyLlama и Qwen 1.5B без облака." },
  { tier: "week", title: "Экспорт чата", desc: "Сохрани диалог в Markdown одним нажатием." },
  { tier: "week", title: "Докачка моделей", desc: "Загрузка продолжается после отмены или сбоя." },
  { tier: "week", title: "Тактильный отклик", desc: "Вибрация при отправке и ключевых действиях." },
  { tier: "month", title: "Retro-тема", desc: "Старый вид и отдельная палитра для настроения." },
  { tier: "month", title: "10 акцентных цветов", desc: "Подбери цвет интерфейса под свой ритм." },
  { tier: "month", title: "Веб-поиск", desc: "Свежие ответы со ссылками на источники прямо в чате." },
  { tier: "month", title: "Свой провайдер", desc: "Свой Base URL и модель через OpenAI-совместимый API." },
  { tier: "month", title: "Qwen Coder 1.5B", desc: "Локальная модель для кода и разбора ошибок." },
  { tier: "month", title: "Персонажи", desc: "Карточки с характером и готовым системным промптом." },
  { tier: "month", title: "Шрифты и аватары", desc: "Настройка шрифта, ширины пузырей и иконок." },
  { tier: "year", title: "Озвучивание ответов", desc: "Text-to-speech для сообщений без внешних файлов." },
  { tier: "year", title: "Свой системный промпт", desc: "Полный контроль над поведением ассистента." },
  { tier: "year", title: "Вся история", desc: "Без лимита на количество сохранённых сообщений." },
  { tier: "year", title: "Бэкапы и QR", desc: "Экспорт настроек и перенос по QR-коду." },
  { tier: "year", title: "DeepSeek R1 1.5B", desc: "Локальные рассуждения для сложного кода." },
  { tier: "year", title: "SmolLM3 3B · Phi-4 Mini", desc: "Крупные локальные модели для серьёзных задач." },
  { tier: "year", title: "Ранний доступ", desc: "Новые функции и модели раньше остальных." },
  { tier: "lifetime", title: "MCP-серверы", desc: "Подключение внешних инструментов к ассистенту." },
  { tier: "lifetime", title: "Плагины .devai", desc: "Свои команды и инструменты на Lua 5.2." },
  { tier: "lifetime", title: "Все будущие функции", desc: "Новые возможности и темы без доплат." },
  { tier: "lifetime", title: "Личная поддержка", desc: "Помощь автора в Telegram по Premium." },
  { tier: "lifetime", title: "Обновления навсегда", desc: "Все будущие версии Premium включены." },
];

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

let selectedPlan = "month";
let orderId = makeOrderId(selectedPlan);
let toastTimer = 0;

function makeOrderId(plan) {
  const stamp = Date.now().toString(36).toUpperCase();
  const salt = Math.random().toString(36).slice(2, 5).toUpperCase();
  return `DVS-${plan.toUpperCase()}-${stamp}${salt}`;
}

function formatStars(value) {
  return String(value).replace(/\B(?=(\d{3})+(?!\d))/g, " ");
}

function orderText(plan, id) {
  const item = PLANS[plan];
  return [
    "👑 DevAI Studio Premium",
    `Тариф: ${item.title}`,
    `Срок: ${item.days}`,
    `Цена: ${formatStars(item.stars)} ⭐`,
    `Заказ: ${id}`,
    "",
    "Оплата: Telegram Stars",
    "После оплаты пришлите этот заказ — вам выдадут код активации.",
  ].join("\n");
}

function telegramLink(plan, id) {
  return `${TELEGRAM_URL}?text=${encodeURIComponent(orderText(plan, id))}`;
}

function showToast(text) {
  const toast = $("#toast");
  if (!toast) return;
  toast.textContent = text;
  toast.classList.add("is-visible");
  window.clearTimeout(toastTimer);
  toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 2600);
}

async function copyText(text) {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    const area = document.createElement("textarea");
    area.value = text;
    area.setAttribute("readonly", "");
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    const ok = document.execCommand("copy");
    area.remove();
    return ok;
  }
}

function syncPlanCards() {
  $$("[data-plan]").forEach((card) => {
    const active = card.dataset.plan === selectedPlan;
    card.classList.toggle("is-selected", active);
    card.setAttribute("aria-current", active ? "true" : "false");
    const button = $("[data-select-plan]", card);
    if (button) {
      button.setAttribute("aria-pressed", active ? "true" : "false");
      button.innerHTML = active ? 'Выбрано <span aria-hidden="true">✓</span>' : 'Выбрать <span aria-hidden="true">→</span>';
    }
  });
}

function setPlan(plan, { scroll = false } = {}) {
  if (!PLANS[plan]) return;
  const changed = selectedPlan !== plan;
  selectedPlan = plan;
  if (changed) orderId = makeOrderId(plan);
  const item = PLANS[plan];

  $("#order-plan").textContent = item.title;
  $("#order-days").textContent = item.days;
  $("#order-price").innerHTML = `${formatStars(item.stars)} <span class="star-glyph" aria-hidden="true"></span>`;
  $("#order-id").textContent = orderId;
  $("#pay-link").href = telegramLink(plan, orderId);

  syncPlanCards();
  syncFeatureTab(plan);
  if (scroll) $("#checkout").scrollIntoView({ behavior: reducedMotion ? "auto" : "smooth", block: "start" });
}

function featureCard(feature, index) {
  return `
    <article class="feature-card" style="--i:${index}" data-spotlight>
      <span class="feature-tier">${TIER_LABEL[feature.tier]}</span>
      <h3>${feature.title}</h3>
      <p>${feature.desc}</p>
    </article>
  `;
}

function renderFeatures(tier) {
  const grid = $("#feature-grid");
  if (!grid) return;
  const rank = TIER_RANK[tier] || 1;
  const visible = FEATURES.filter((feature) => TIER_RANK[feature.tier] <= rank);
  grid.innerHTML = visible.map(featureCard).join("");
  bindSpotlight(grid);
}

function syncFeatureTab(tier) {
  $$("[data-feature-tier]").forEach((tab) => {
    const active = tab.dataset.featureTier === tier;
    tab.classList.toggle("is-active", active);
    tab.setAttribute("aria-selected", active ? "true" : "false");
  });
  renderFeatures(tier);
}

function bindSpotlight(root) {
  $$("[data-spotlight], .plan-card", root).forEach((card) => {
    card.addEventListener("pointermove", (event) => {
      const rect = card.getBoundingClientRect();
      card.style.setProperty("--mx", `${event.clientX - rect.left}px`);
      card.style.setProperty("--my", `${event.clientY - rect.top}px`);
    });
  });
}

function initPlans() {
  $$("[data-plan]").forEach((card) => {
    card.addEventListener("click", (event) => {
      const pick = event.target.closest("[data-select-plan]");
      setPlan(card.dataset.plan, { scroll: Boolean(pick) });
    });
  });
  syncPlanCards();
  setPlan(selectedPlan);
}

function initCopy() {
  $("#copy-order").addEventListener("click", async () => {
    const ok = await copyText(orderText(selectedPlan, orderId));
    showToast(ok ? "Заказ скопирован" : "Не удалось скопировать");
  });
}

function initFeatureTabs() {
  $$("[data-feature-tier]").forEach((tab) => {
    tab.addEventListener("click", () => syncFeatureTab(tab.dataset.featureTier));
  });
  syncFeatureTab(selectedPlan);
}

function initReveal() {
  const nodes = $$(".reveal");
  if (reducedMotion || !("IntersectionObserver" in window)) {
    nodes.forEach((node) => node.classList.add("is-in"));
    return;
  }
  const observer = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;
      entry.target.classList.add("is-in");
      observer.unobserve(entry.target);
    });
  }, { threshold: 0.12, rootMargin: "0px 0px -6% 0px" });
  nodes.forEach((node) => observer.observe(node));
}

function initScroll() {
  const update = () => document.body.classList.toggle("is-scrolled", window.scrollY > 12);
  update();
  window.addEventListener("scroll", update, { passive: true });
}

function initYear() {
  const year = $("#year");
  if (year) year.textContent = new Date().getFullYear();
}

function initStarfield() {
  const canvas = $("#star-canvas");
  if (!canvas) return;
  const context = canvas.getContext("2d");
  if (!context) return;

  let width = 0;
  let height = 0;
  let particles = [];
  let frame = 0;

  const makeParticle = () => ({
    x: Math.random(),
    y: Math.random(),
    size: 0.4 + Math.random() * 1.6,
    drift: 0.02 + Math.random() * 0.08,
    phase: Math.random() * Math.PI * 2,
    gold: Math.random() > 0.62,
  });

  const resize = () => {
    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    width = window.innerWidth;
    height = window.innerHeight;
    canvas.width = Math.floor(width * dpr);
    canvas.height = Math.floor(height * dpr);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    context.setTransform(dpr, 0, 0, dpr, 0, 0);
    particles = Array.from({ length: Math.min(110, Math.round((width * height) / 16000)) }, makeParticle);
  };

  const draw = (time = 0) => {
    context.clearRect(0, 0, width, height);
    particles.forEach((particle) => {
      if (!reducedMotion) {
        particle.y -= particle.drift * 0.0008;
        if (particle.y < -0.02) particle.y = 1.02;
      }
      const twinkle = 0.35 + 0.65 * Math.abs(Math.sin(time * 0.0007 + particle.phase));
      context.globalAlpha = twinkle;
      context.fillStyle = particle.gold ? "#ffd75e" : "#f4f0e6";
      context.beginPath();
      context.arc(particle.x * width, particle.y * height, particle.size, 0, Math.PI * 2);
      context.fill();
    });
    context.globalAlpha = 1;
    if (!reducedMotion) frame = requestAnimationFrame(draw);
  };

  resize();
  draw();
  window.addEventListener("resize", () => {
    cancelAnimationFrame(frame);
    resize();
    draw();
  }, { passive: true });
}

initPlans();
initCopy();
initFeatureTabs();
initReveal();
initScroll();
initYear();
initStarfield();
