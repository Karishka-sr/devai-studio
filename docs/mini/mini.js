const PLANS = [
  { id: "week", title: "Неделя", days: "7 дней", stars: "75" },
  { id: "month", title: "Месяц", days: "30 дней", stars: "249", badge: "ПОПУЛЯРНЫЙ" },
  { id: "year", title: "Год", days: "365 дней", stars: "1 490", badge: "ВЫГОДНО" },
  { id: "lifetime", title: "Навсегда", days: "все обновления", stars: "4 990" },
];

const tg = window.Telegram?.WebApp;
const params = new URLSearchParams(window.location.search);
const bot = (params.get("bot") || "").replace(/^@/, "").trim();
const hint = document.querySelector("#hint");
const who = document.querySelector("#who");

if (tg) {
  tg.ready();
  tg.expand();
  tg.setHeaderColor?.("#070707");
  tg.setBackgroundColor?.("#070707");
  const user = tg.initDataUnsafe?.user;
  if (user) {
    who.textContent = user.username ? `@${user.username}` : user.first_name || "MINI APP";
  }
}

if (bot) {
  hint.textContent = "Выбери тариф — бот пришлёт счёт в Telegram Stars.";
} else {
  hint.textContent = "Открой мини-апп из бота, чтобы кнопки работали.";
}

function openBot(start) {
  if (!bot) {
    hint.textContent = "Не знаю username бота. Открой мини-апп через кнопку в боте.";
    return;
  }
  const url = `https://t.me/${bot}?start=${encodeURIComponent(start)}`;
  if (tg?.openTelegramLink) {
    tg.openTelegramLink(url);
  } else {
    window.location.href = url;
  }
}

function renderPlans() {
  const root = document.querySelector("#plans");
  for (const plan of PLANS) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "plan";
    button.dataset.plan = plan.id;
    button.innerHTML = `
      <span class="plan-main">
        <span class="plan-title">${plan.title}${plan.badge ? `<span class="plan-badge">${plan.badge}</span>` : ""}</span>
        <span class="plan-days">${plan.days}</span>
      </span>
      <span class="plan-price">${plan.stars}<span class="plan-star" aria-hidden="true"></span></span>
    `;
    button.addEventListener("click", () => {
      tg?.HapticFeedback?.impactOccurred?.("light");
      openBot(`buy_${plan.id}`);
    });
    root.appendChild(button);
  }
}

document.querySelector("#partner")?.addEventListener("click", () => {
  tg?.HapticFeedback?.impactOccurred?.("light");
  openBot("partner");
});

renderPlans();
