(() => {
  const REPO = "Karishka-sr/devai-studio";
  const RELEASES = `https://github.com/${REPO}/releases`;
  const LATEST_PAGE = `${RELEASES}/latest`;
  const API = `https://api.github.com/repos/${REPO}/releases/latest`;
  const LOCAL_DATA = "data/latest.json";
  const STALE_MS = 12 * 60 * 60 * 1000;

  const all = (selector) => Array.from(document.querySelectorAll(selector));
  const one = (selector) => document.querySelector(selector);

  function setText(selector, value) {
    if (value === undefined || value === null || value === "") return;
    all(selector).forEach((node) => { node.textContent = value; });
  }

  function setHref(selector, value) {
    if (!value) return;
    all(selector).forEach((node) => { node.href = value; });
  }

  function versionParts(value) {
    return String(value || "")
      .replace(/^v/i, "")
      .split(/[.-]/)
      .map((part) => parseInt(part, 10) || 0);
  }

  function compareVersions(a, b) {
    const left = versionParts(a);
    const right = versionParts(b);
    const length = Math.max(left.length, right.length);
    for (let i = 0; i < length; i += 1) {
      const x = left[i] || 0;
      const y = right[i] || 0;
      if (x !== y) return x > y ? 1 : -1;
    }
    return 0;
  }

  function formatSize(bytes) {
    const size = Number(bytes);
    if (!Number.isFinite(size) || size <= 0) return "112 МБ";
    if (size < 1024 * 1024) return `${Math.round(size / 1024)} КБ`;
    return `${(size / 1024 / 1024).toFixed(1).replace(".", ",")} МБ`;
  }

  function formatDate(value) {
    if (!value) return "дата неизвестна";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "дата неизвестна";
    return date.toLocaleDateString("ru-RU", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  }

  function formatDateTime(value) {
    if (!value) return "только что";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "только что";
    return date.toLocaleString("ru-RU", {
      day: "numeric",
      month: "short",
      hour: "2-digit",
      minute: "2-digit",
    });
  }

  function cleanNotes(body) {
    return String(body || "")
      .replace(/\r/g, "")
      .replace(/^Этот релиз подготовлен AI-агентом[^\n]*\n+/i, "")
      .replace(/^#{1,6}\s*/gm, "")
      .replace(/^[-*]\s+/gm, "• ")
      .trim();
  }

  async function getJson(url, timeout = 9000) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
      const response = await fetch(url, {
        signal: controller.signal,
        cache: "no-store",
        headers: { Accept: "application/json" },
      });
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      return await response.json();
    } finally {
      clearTimeout(timer);
    }
  }

  function normalizeApi(release) {
    const assets = Array.isArray(release.assets) ? release.assets : [];
    const apk = assets.find((asset) => /\.apk$/i.test(asset.name || ""));
    const languages = assets.find((asset) => /\.zip$/i.test(asset.name || ""));
    const version = String(release.tag_name || "").replace(/^v/i, "");
    if (!version || !apk) return null;
    return {
      tag: release.tag_name,
      version,
      name: release.name || `DevAI Studio ${version}`,
      published_at: release.published_at,
      page_url: release.html_url,
      apk_url: apk.browser_download_url,
      apk_name: apk.name,
      apk_size: apk.size,
      apk_sha256: String(apk.digest || "").replace(/^sha256:/i, ""),
      languages_url: languages ? languages.browser_download_url : "",
      languages_name: languages ? languages.name : "",
      body: release.body || "",
      generated_at: new Date().toISOString(),
    };
  }

  async function loadRelease() {
    let local = null;
    try {
      local = await getJson(`${LOCAL_DATA}?v=${Date.now()}`);
    } catch {
      local = null;
    }

    const missing = !local || !local.version || !local.apk_url;
    const stale = local && local.generated_at
      ? Date.now() - new Date(local.generated_at).getTime() > STALE_MS
      : true;

    if (!missing && !stale) return local;

    try {
      const remote = normalizeApi(await getJson(API));
      if (remote && (!local || compareVersions(remote.version, local.version) >= 0)) {
        return remote;
      }
    } catch {
      // GitHub API can be rate-limited; the static JSON remains the fallback.
    }

    return local;
  }

  function applyRelease(release) {
    if (!release || !release.version) return false;

    const tag = release.tag || `v${release.version}`;
    const version = `v${release.version}`;
    const page = release.page_url || LATEST_PAGE;
    const downloadPage = `download.html?tag=${encodeURIComponent(tag)}`;

    setText("[data-version]", version);
    setText("[data-release-name]", release.name || `DevAI Studio ${release.version}`);
    setText("[data-release-date]", formatDate(release.published_at));
    setText("[data-apk-size]", formatSize(release.apk_size));
    setText("[data-apk-name]", release.apk_name || `devai-studio-${release.version}.apk`);
    setText("[data-apk-sha]", release.apk_sha256 || "не указан");
    setText("[data-generated]", formatDateTime(release.generated_at));

    const notes = cleanNotes(release.body);
    all("[data-release-notes]").forEach((node) => {
      node.textContent = notes || "Описание релиза скоро появится.";
    });

    setHref("[data-download-link]", downloadPage);
    setHref("[data-github-link]", page);
    setHref("[data-release-link]", page);
    setHref("[data-languages-link]", release.languages_url || page);

    const direct = one("#download-direct");
    if (direct) direct.href = release.apk_url || page;
    const github = one("#download-github");
    if (github) github.href = page;

    document.title = document.body.dataset.page === "download"
      ? `Скачать DevAI Studio ${version}`
      : `DevAI Studio ${version} — ИИ-студия для Android`;

    const ldNode = one('script[type="application/ld+json"]');
    if (ldNode) {
      try {
        const ld = JSON.parse(ldNode.textContent);
        ld.softwareVersion = release.version;
        ld.downloadUrl = release.apk_url || new URL(downloadPage, location.href).href;
        if (release.published_at) ld.datePublished = release.published_at.slice(0, 10);
        ldNode.textContent = JSON.stringify(ld);
      } catch {
        // Keep the static structured data if it is malformed.
      }
    }

    return true;
  }

  async function startDownloadPage(release) {
    const state = one("#download-state");
    if (!release || !release.apk_url) {
      if (state) state.textContent = "Не удалось получить ссылку на APK. Открой страницу релиза на GitHub.";
      return;
    }

    if (state) {
      state.textContent = `Версия ${release.version} · ${formatSize(release.apk_size)}. Скачивание начнётся автоматически.`;
    }

    window.setTimeout(() => {
      window.location.replace(release.apk_url);
    }, 1200);
  }

  async function init() {
    const year = one("#year");
    if (year) year.textContent = String(new Date().getFullYear());

    const release = await loadRelease();
    const applied = applyRelease(release);

    if (document.body.dataset.page === "download") {
      if (applied) await startDownloadPage(release);
      else await startDownloadPage(null);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init, { once: true });
  } else {
    init();
  }
})();
