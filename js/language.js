const LANGUAGE_STORAGE_KEY = "codrive.language";
const LANGUAGES = { fr: "Français", en: "English" };

function supportedLanguage(value) {
  return Object.hasOwn(LANGUAGES, value);
}

function readLanguage() {
  // The URL carries the choice between pages when opened directly from disk.
  const requested = new URLSearchParams(window.location.search).get("lang");
  if (supportedLanguage(requested)) return requested;

  try {
    const saved = localStorage.getItem(LANGUAGE_STORAGE_KEY);
    if (supportedLanguage(saved)) return saved;
  } catch {
    // The menu also works when browser storage is unavailable.
  }
  return "fr";
}

const currentLanguage = readLanguage();

function getLanguage() {
  return currentLanguage;
}

function t(french, english) {
  return getLanguage() === "en" ? english : french;
}

function localizedUrl(path, language = getLanguage()) {
  const url = new URL(path, window.location.href);
  url.searchParams.set("lang", language);
  return url.href;
}

document.addEventListener("DOMContentLoaded", () => {
  document.documentElement.lang = getLanguage();
  if (getLanguage() === "en") {
    for (const element of document.querySelectorAll("[data-en]")) {
      element.textContent = element.getAttribute("data-en");
    }
    for (const attribute of ["placeholder", "aria-label", "title", "alt"]) {
      for (const element of document.querySelectorAll(`[data-en-${attribute}]`)) {
        element.setAttribute(attribute, element.getAttribute(`data-en-${attribute}`));
      }
    }
  }

  for (const link of document.querySelectorAll('a[href$=".html"]')) {
    link.href = localizedUrl(link.getAttribute("href"));
  }
  if (!document.body.matches(".auth-page, .dashboard-page")) return;

  const menu = document.createElement("select");
  menu.className = "language-picker";
  menu.setAttribute("aria-label", t("Langue", "Language"));
  for (const [value, label] of Object.entries(LANGUAGES)) {
    const option = new Option(label, value);
    option.lang = value;
    menu.add(option);
  }
  menu.value = getLanguage();
  menu.addEventListener("change", () => {
    try {
      localStorage.setItem(LANGUAGE_STORAGE_KEY, menu.value);
    } catch {
      // The URL retains the selected language.
    }
    window.location.replace(localizedUrl(window.location.href, menu.value));
  });
  document.body.prepend(menu);
});
