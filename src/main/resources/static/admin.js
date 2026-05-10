// Base url for requests
const backendBaseUrl = "http://localhost:8080"; // put backend url here 

// Request paths
const apiConfig = {
  routes: "/api/routes",
  buildStops: (routeId) => `/api/stops?routeId=${encodeURIComponent(routeId)}`,
  buildTimes: (routeId, stopId) => `/api/times?stopId=${encodeURIComponent(stopId)}`,
  buildSelection: (routeId, stopId, timeValue, filters = {}) => {
    const params = new URLSearchParams({
      stopID: stopId,
      time: timeValue
    });
    if (filters.dateRange) {
      // Jared's backend expects "week"/"month", frontend sends "7days"/"30days"
      const map = { "7days": "week", "30days": "month" };
      params.set("dateRange", map[filters.dateRange] || filters.dateRange);
    }
    if (filters.weatherCondition) {
      // Jared's backend uses param name "condition", not "weatherCondition"
      params.set("condition", filters.weatherCondition);
    }
    return `/analysis?${params.toString()}`;
  },
  adminLogin: "/admin/login"
};
// Search page elements
const routeSelect = document.getElementById("route-select");
const stopSelect = document.getElementById("stop-select");
const timeSelect = document.getElementById("time-select");
const dateRangeSelect = document.getElementById("date-range-select");
const weatherConditionSelect = document.getElementById("weather-condition-select");
const addRouteBtn = document.getElementById("add-route-btn");
const selectedRoutesWrap = document.getElementById("selected-routes");
const routeSummaryList = document.getElementById("route-summary-list");

// Data the page keeps while it is open
const appState = {
  routes: [],
  stopsByRoute: new Map(),
  selectedRoutes: []
};

// Route choices for the dropdown
const fallbackRoutes = [
  { id: "OSW 10", name: "Oswego Route 10", direction: "" },
  { id: "OSW 11", name: "Oswego Route 11", direction: "" },
  { id: "OSW 46", name: "Oswego Route 46", direction: "" },
  { id: "SY 84", name: "Syracuse Route 84", direction: "" },
  { id: "SY 88", name: "Syracuse Route 88", direction: "" }
];

// Login and log info
const sessionKey = "centroAdminSession";
const logsKey = "centroSystemLogs";
const adminUsername = "admin";
const adminPassword = "ILoveCentro";

// Put the base url and path together
function buildApiUrl(path) {
  return `${backendBaseUrl}${path}`;
}

// Try to read data from the backend
// If it fails just return null so the page can stay in an empty state
async function readJson(path, options = {}) {
  try {
    const response = await fetch(buildApiUrl(path), {
      headers: {
        "Content-Type": "application/json"
      },
      ...options
    });

    if (!response.ok) {
      throw new Error(`Request failed with ${response.status}`);
    }

    return await response.json();
  } catch (error) {
    return null;
  }
}

// Send the login request
async function loginWithBackend(username, password) {
  return await readJson(apiConfig.adminLogin, {
    method: "POST",
    body: JSON.stringify({
      username, 
      password 
    })
  });
}

// Make a simple id for each route card
function makeSelectionId(routeId, stopId, timeValue) {
  return `${routeId}-${stopId}-${timeValue}`.replace(/[^a-z0-9]+/gi, "-").toLowerCase();
}

// Read the current filter values
// These get added to the analysis request url
function getSelectedFilters() {
  return {
    dateRange: dateRangeSelect?.value || "",
    weatherCondition: weatherConditionSelect?.value || ""
  };
}

// Clean up route data
function normalizeRoutes(payload) {
  const source = Array.isArray(payload) ? payload : payload?.routes || [];

  return source.map((route) => ({
    id: route.id || route.routeId || route.rt || "",
    name: route.name || route.routeName || route.rtnm || "",
    direction: route.direction || route.routeDirection || route.rtdir || ""
  })).filter((route) => route.id && route.name);
}

// Clean up stop data from the backend into one format
function normalizeStops(payload) {
  const source = Array.isArray(payload) ? payload : payload?.stops || [];

  return source.map((stop) => ({
    id: stop.id || stop.stopId || stop.stpid || "",
    name: stop.name || stop.stopName || stop.stpnm || ""
  })).filter((stop) => stop.id && stop.name);
}

// Clean up time data from the backend into one format
function normalizeTimes(payload) {
  const source = Array.isArray(payload) ? payload : payload?.times || [];

  return source.map((time) => {
    if (typeof time === "string") {
      return { value: time, label: time };
    }

    return {
      value: time.value || time.time || time.prdtm || time.schdtm || "",
      label: time.label || time.time || time.prdtm || time.schdtm || ""
    };
  }).filter((time) => time.value && time.label);
}

// Clean up one selected route result into the format this page uses
function normalizeSelection(payload, routeId, stopId, timeValue) {
  if (!payload) {
    return null;
  }

  const stopName = stopSelect?.selectedOptions?.[0]?.textContent || "Stop data unavailable";
  const routeName = routeSelect?.selectedOptions?.[0]?.textContent?.split(" - ").slice(1).join(" - ") || "Route data unavailable";

  if (Array.isArray(payload)) {
    return {
      selectionId: makeSelectionId(routeId, stopId, timeValue),
      routeId,
      routeName,
      direction: "",
      city: "",
      stopId,
      stopName,
      timeLabel: timeValue,
      accuracy: "--",
      averageDelay: "--",
      weatherInfo: "Not connected",
      histogram: buildHistogramFromOffsets(payload),
      liveData: true
    };
  }

  const source = payload;

  return {
    selectionId: source.selectionId || makeSelectionId(routeId, stopId, timeValue),
    routeId: source.routeId || routeId,
    routeName: source.routeName || source.name || routeName,
    direction: source.direction || "",
    city: source.city || "",
    stopId: source.stopId || stopId,
    stopName: source.stopName || stopName,
    timeLabel: source.timeLabel || source.time || timeValue,
    accuracy: source.accuracy || "--",
    averageDelay: source.averageDelay || source.deviation || "--",
    weatherInfo: source.weatherInfo || "Not connected",
    histogram: Array.isArray(source.histogram) ? source.histogram : Array.isArray(source.arrivals) ? buildHistogramFromOffsets(source.arrivals) : [],
    liveData: Boolean(source.liveData)
  };
}

// Turn the arrival offsets into histogram bars
function buildHistogramFromOffsets(offsets) {
  const counts = new Map();

  offsets.forEach((offset) => {
    const value = Number(offset);
    if (Number.isNaN(value)) {
      return;
    }

    counts.set(value, (counts.get(value) || 0) + 1);
  });

  return Array.from(counts.entries())
    .sort((first, second) => first[0] - second[0])
    .map(([offset, count]) => ({
      label: offset === 0 ? "0m" : `${offset > 0 ? "+" : ""}${offset}m`,
      value: count,
      type: offset < 0 ? "early" : offset > 0 ? "late" : "ontime"
    }));
}

// Clean up admin data so the page has safe fallback values
function normalizeAdminOverview(payload) {
  const activeRoutes = Array.isArray(payload?.activeRoutes) ? payload.activeRoutes : Array.isArray(payload?.activeRoutesValue) ? payload.activeRoutesValue : [];
  const recentSystemLogs = Array.isArray(payload?.recentSystemLogs) ? payload.recentSystemLogs : Array.isArray(payload?.logs) ? payload.logs : [];

  return {
    totalBusesPolledValue: Number.isFinite(Number(payload?.totalStopsServed)) ? Number(payload.totalStopsServed) : "--",
    averageAccuracyValue: Number.isFinite(Number(payload?.averageDelay)) ? Number(payload.averageDelay).toFixed(2) : "--",
    activeRoutesValue: activeRoutes.length ? activeRoutes.length : "--",
    activeRoutesNote: activeRoutes.length ? activeRoutes.join(", ") : "Waiting",
    centroPullRequestsValue: Number.isFinite(Number(payload?.centroPullRequests)) ? Number(payload.centroPullRequests) : "--",
    weatherPullRequestsValue: Number.isFinite(Number(payload?.weatherPullRequests)) ? Number(payload.weatherPullRequests) : "--",
    recentSystemLogs
  };
}

// Build the histogram section from backend data
function createHistogramMarkup(histogramData) {
  if (!histogramData.length) {
    return '<p class="dashboard-copy">No histogram data returned yet</p>';
  }

  const maxValue = Math.max(...histogramData.map((item) => Number(item.value) || 0), 1);

  return histogramData.map((item) => `
    <div class="bar ${item.type === "early" ? "bar--early" : item.type === "late" ? "bar--late" : ""}">
      <div class="bar-fill" style="height: ${((Number(item.value) || 0) / maxValue) * 100}%"></div>
      <span class="bar-value">${item.value}</span>
      <span class="bar-label">${item.label}</span>
    </div>
  `).join("");
}

// Show the routes the user has added
function renderSelectedRoutes() {
  if (!selectedRoutesWrap) {
    return;
  }

  selectedRoutesWrap.innerHTML = "";

  appState.selectedRoutes.forEach((route) => {
    const chip = document.createElement("div");
    chip.className = "route-chip";
    chip.innerHTML = `<span>${route.routeId}</span><button type="button" data-selection="${route.selectionId}" aria-label="Remove ${route.routeId}">&times;</button>`;
    selectedRoutesWrap.append(chip);
  });
}

// Fill the route dropdown from backend data
function renderRouteOptions() {
  if (!routeSelect) {
    return;
  }

  routeSelect.innerHTML = '<option value="">Choose a route</option>';

  appState.routes.forEach((route) => {
    const option = document.createElement("option");
    option.value = route.id;
    option.textContent = route.direction ? `${route.id} - ${route.name} - ${route.direction}` : `${route.id} - ${route.name}`;
    routeSelect.append(option);
  });
}

// Fill the stop dropdown after a route is chosen
function renderStops(routeId, stops) {
  if (!stopSelect || !timeSelect) {
    return;
  }

  stopSelect.innerHTML = '<option value="">Choose a stop</option>';

  stops.forEach((stop) => {
    const option = document.createElement("option");
    option.value = stop.id;
    option.textContent = stop.name;
    stopSelect.append(option);
  });

  stopSelect.disabled = !stops.length;
  timeSelect.innerHTML = '<option value="">Select stop first</option>';
  timeSelect.disabled = true;
  appState.stopsByRoute.set(routeId, stops);
}

// Fill the time dropdown after a stop is chosen
function renderTimes(times) {
  if (!timeSelect) {
    return;
  }

  timeSelect.innerHTML = '<option value="">Choose a scheduled time</option>';

  times.forEach((time) => {
    const option = document.createElement("option");
    option.value = time.value;
    option.textContent = time.label;
    timeSelect.append(option);
  });

  timeSelect.disabled = !times.length;
}

// Build the route cards shown on the search page
function renderRouteSummary() {
  if (!routeSummaryList) {
    return;
  }

  routeSummaryList.innerHTML = "";

  if (!appState.selectedRoutes.length) {
    routeSummaryList.innerHTML = `
      <article class="glass-card simple-panel">
        <p class="section-label">No Routes Added</p>
        <p class="dashboard-copy">Choose a route stop and time to view route details</p>
      </article>
    `;
    return;
  }

  appState.selectedRoutes.forEach((route) => {
    const routeItem = document.createElement("article");
    routeItem.className = "route-item";

    routeItem.innerHTML = `
      <div class="route-summary">
        <div class="route-summary__info">
          <div class="route-badge">${route.routeId.replace("SY ", "").replace("OSW ", "")}</div>
          <div>
            <h4>${route.routeName}</h4>
            <p class="route-summary__meta">${route.city || ""} ${route.direction ? `- ${route.direction}` : ""}</p>
            <p class="route-summary__meta">${route.stopName}</p>
          </div>
        </div>
        <div class="route-summary__accuracy">
          <strong>${route.accuracy}</strong>
        </div>
        <div class="route-summary__delay ${String(route.averageDelay).startsWith("+") ? "route-summary__delay--late" : ""}">
          <strong>${route.averageDelay}</strong>
          <div class="route-summary__meta">Avg delay</div>
        </div>
        <div class="route-summary__action">
          <button type="button" class="btn btn-light detail-btn" data-selection="${route.selectionId}">Details</button>
        </div>
      </div>
      <div class="route-details" id="details-${route.selectionId}">
        <div class="panel-head">
          <div>
            <p class="section-label">Arrival Time Data</p>
            <h3>${route.routeId} - ${route.stopName} - ${route.timeLabel}</h3>
          </div>
          <span class="status-badge">${route.liveData ? "Live data" : "Waiting"}</span>
        </div>
        <div class="histogram">${createHistogramMarkup(route.histogram)}</div>
        <div class="insight-grid details-meta">
          <div class="insight-card">
            <p>On-time rate</p>
            <strong>${route.accuracy}</strong>
          </div>
          <div class="insight-card">
            <p>Average delay</p>
            <strong>${route.averageDelay}</strong>
          </div>
          <div class="insight-card">
            <p>Weather info</p>
            <strong>${route.weatherInfo}</strong>
          </div>
        </div>
      </div>
    `;

    routeSummaryList.append(routeItem);
  });
}

// Open and close the route details area
function openRouteDetails(selectionId) {
  const details = document.getElementById(`details-${selectionId}`);
  const button = document.querySelector(`.detail-btn[data-selection="${selectionId}"]`);

  if (!details || !button) {
    return;
  }

  const isOpen = details.classList.contains("is-open");

  document.querySelectorAll(".route-details").forEach((detailCard) => {
    detailCard.classList.remove("is-open");
  });

  document.querySelectorAll(".detail-btn").forEach((detailButton) => {
    detailButton.textContent = "Details";
  });

  if (isOpen) {
    return;
  }

  details.classList.add("is-open");
  button.textContent = "Hide";
}

// Get the route list
async function loadRoutes() {
  if (!routeSelect) {
    return;
  }

  const payload = await readJson(apiConfig.routes);
  appState.routes = normalizeRoutes(payload);

  if (!appState.routes.length) {
    appState.routes = fallbackRoutes;
  }

  renderRouteOptions();
}

// Get the stops for one route
async function loadStops(routeId) {
  if (!routeId) {
    return;
  }

  const payload = await readJson(apiConfig.buildStops(routeId));
  renderStops(routeId, normalizeStops(payload));
}

// Get the times for one route and stop
async function loadTimes(routeId, stopId) {
  if (!routeId || !stopId) {
    return;
  }

  const payload = await readJson(apiConfig.buildTimes(routeId, stopId));
  renderTimes(normalizeTimes(payload));
}

// Add one route card
async function addSelectedRoute() {
  const routeId = routeSelect?.value;
  const stopId = stopSelect?.value;
  const timeValue = timeSelect?.value;

  if (!routeId || !stopId || !timeValue) {
    return;
  }

  const selectionId = makeSelectionId(routeId, stopId, timeValue);
  const existing = appState.selectedRoutes.find((route) => route.selectionId === selectionId);
  if (existing) {
    return;
  }

  const filters = getSelectedFilters();
  const payload = await readJson(apiConfig.buildSelection(routeId, stopId, timeValue, filters));
  const route = normalizeSelection(payload, routeId, stopId, timeValue);
  if (!route) {
    return;
  }

  appState.selectedRoutes.push(route);
  renderSelectedRoutes();
  renderRouteSummary();
}

// Remove a route card from the page
function removeSelectedRoute(selectionId) {
  appState.selectedRoutes = appState.selectedRoutes.filter((route) => route.selectionId !== selectionId);
  renderSelectedRoutes();
  renderRouteSummary();
}

// Read any saved admin logs
function getStoredLogs() {
  const savedLogs = localStorage.getItem(logsKey);
  if (!savedLogs) {
    return [];
  }

  try {
    return JSON.parse(savedLogs);
  } catch (error) {
    return [];
  }
}

// Save the newest admin logs
function saveLogs(logs) {
  localStorage.setItem(logsKey, JSON.stringify(logs.slice(0, 50)));
}

// Get the current time for the logs
function getCurrentTimeString() {
  return new Date().toLocaleTimeString([], { hour: "numeric", minute: "2-digit", second: "2-digit" });
}

// Add one item to the admin logs
function addSystemLog(eventName, status = "system") {
  const logs = getStoredLogs();
  logs.unshift({
    time: getCurrentTimeString(),
    event: eventName,
    status
  });
  saveLogs(logs);
}

// Read the saved admin login
function getSession() {
  const savedSession = localStorage.getItem(sessionKey);
  return savedSession ? JSON.parse(savedSession) : null;
}

// Save the admin login
function setSession(sessionData) {
  localStorage.setItem(sessionKey, JSON.stringify(sessionData));
}

// Clear the admin login
function clearSession() {
  localStorage.removeItem(sessionKey);
}

// Keep the admin page behind the login
function requireAdminPageAccess() {
  if (!window.location.pathname.toLowerCase().includes("admin.html")) {
    return true;
  }

  const session = getSession();
  if (!session || !session.username) {
    window.location.href = "./login.html?from=admin";
    return false;
  }

  return true;
}

// Show the admin logs on the page
function renderAdminLogs() {
  const logsWrap = document.getElementById("system-logs-list");
  if (!logsWrap) {
    return;
  }

  const logs = getStoredLogs();
  logsWrap.innerHTML = "";

  if (!logs.length) {
    logsWrap.innerHTML = '<p class="dashboard-copy">No logs yet</p>';
    return;
  }

  logs.forEach((log) => {
    const row = document.createElement("div");
    row.className = "system-log-row";
    row.innerHTML = `
      <span class="system-log-time">${log.time || ""}</span>
      <span class="system-log-event">${log.event}</span>
    `;
    logsWrap.append(row);
  });
}

// Show blank admin values
function renderAdminFallback() {
  document.getElementById("admin-buses-polled-value").textContent = "--";
  document.getElementById("admin-accuracy-value").textContent = "--";
  document.getElementById("admin-active-routes-value").textContent = "--";
  document.getElementById("admin-active-routes-note").textContent = "Waiting";
  document.getElementById("admin-centro-pulls-value").textContent = "--";
  document.getElementById("admin-weather-pulls-value").textContent = "--";
}

// Show the current system status list
function renderStatusList() {
  const statusList = document.getElementById("admin-status-list");
  if (!statusList) {
    return;
  }

  const items = [
    { name: "Centro API", label: "Connected" },
    { name: "OpenWeather", label: "Connected" },
    { name: "MySQL DB", label: "Connected" }
  ];

  statusList.innerHTML = items.map((item) => `
    <div class="status-item">
      <span>${item.name}</span>
      <strong>${item.label}</strong>
    </div>
  `).join("");
}

// Load the admin cards
async function loadAdminOverview() {
  if (!document.getElementById("admin-buses-polled-value")) {
    return;
  }

  // Fan out to all 5 admin endpoints in parallel
  const [totalStops, avgDelay, activeRoutes, apiCalls, systemLogs] = await Promise.all([
    readJson("/admin/totalstops"),
    readJson("/admin/avgdelay"),
    readJson("/admin/activeroutes"),
    readJson("/admin/apicalls"),
    readJson("/admin/systemlog")
  ]);

  // Combine into the shape normalizeAdminOverview expects
  const payload = {
    totalStopsServed: totalStops,
    averageDelay: avgDelay,
    activeRoutes: activeRoutes,
    centroPullRequests: apiCalls?.[0],
    weatherPullRequests: apiCalls?.[1],
    recentSystemLogs: systemLogs
  };

  const overview = normalizeAdminOverview(payload);

  document.getElementById("admin-buses-polled-value").textContent = `${overview.totalBusesPolledValue}`;
  document.getElementById("admin-accuracy-value").textContent = `${overview.averageAccuracyValue}`;
  document.getElementById("admin-active-routes-value").textContent = `${overview.activeRoutesValue}`;
  document.getElementById("admin-active-routes-note").textContent = overview.activeRoutesNote;
  document.getElementById("admin-centro-pulls-value").textContent = `${overview.centroPullRequestsValue}`;
  document.getElementById("admin-weather-pulls-value").textContent = `${overview.weatherPullRequestsValue}`;

  if (overview.recentSystemLogs.length) {
    saveLogs(overview.recentSystemLogs.map((line, index) => ({
      time: `Log ${index + 1}`,
      event: String(line),
      status: "system"
    })));
    renderAdminLogs();
  }

  renderStatusList();
}
// Set up the login page
function initializeLoginPage() {
  const loginForm = document.getElementById("login-form");
  if (!loginForm) {
    return;
  }

  const session = getSession();
  if (session && session.username) {
    window.location.href = "./admin.html";
    return;
  }

  const message = document.getElementById("login-message");
  const pageParams = new URLSearchParams(window.location.search);
  if (pageParams.get("from") === "admin") {
    message.textContent = "Please sign in as admin to open the admin page";
  }

  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const usernameInput = document.getElementById("login-username");
    const passwordInput = document.getElementById("login-password");
    const username = usernameInput.value.trim();
    const password = passwordInput.value;

    message.textContent = "Checking login";

    const result = await loginWithBackend(username, password);
    const allowed = Boolean(result?.success ?? result?.authenticated ?? result?.allowed ?? result?.username);
    const sessionData = {
      username: result?.username || username,
      role: result?.role || ""
    };

    if (allowed) {
      setSession(sessionData);
      addSystemLog(`${sessionData.username} has logged in`, "success");
      window.location.href = "./admin.html";
      return;
    }

    if (username === adminUsername && password === adminPassword) {
      setSession({
        username: adminUsername,
        role: "admin"
      });
      addSystemLog(`${adminUsername} has logged in`, "success");
      window.location.href = "./admin.html";
      return;
    }

    message.textContent = "Login information was not accepted";
  });
}

// Set up the admin page
function initializeAdminPage() {
  if (!document.getElementById("system-logs-list")) {
    return;
  }

  if (!requireAdminPageAccess()) {
    return;
  }

  const session = getSession();
  if (session) {
    document.getElementById("admin-username").textContent = session.username;
  }

  renderAdminLogs();
  renderStatusList();
  loadAdminOverview();

  document.getElementById("refresh-data-btn")?.addEventListener("click", async () => {
    addSystemLog("admin refreshed admin data", "system");
    renderAdminLogs();
    await loadAdminOverview();
  });

  document.getElementById("logout-btn")?.addEventListener("click", () => {
    clearSession();
    window.location.href = "./login.html";
  });
}

// Set up the search page
function initializeSearchPage() {
  if (!routeSelect) {
    return;
  }

  renderSelectedRoutes();
  renderRouteSummary();
  loadRoutes();

  addRouteBtn?.addEventListener("click", async () => {
    await addSelectedRoute();
  });

  selectedRoutesWrap?.addEventListener("click", (event) => {
    const button = event.target.closest("button[data-selection]");
    if (!button) {
      return;
    }

    removeSelectedRoute(button.dataset.selection);
  });

  routeSelect.addEventListener("change", async () => {
    stopSelect.innerHTML = '<option value="">Loading stops</option>';
    stopSelect.disabled = true;
    timeSelect.innerHTML = '<option value="">Select stop first</option>';
    timeSelect.disabled = true;

    if (!routeSelect.value) {
      stopSelect.innerHTML = '<option value="">Select route first</option>';
      return;
    }

    await loadStops(routeSelect.value);
  });

  stopSelect?.addEventListener("change", async () => {
    timeSelect.innerHTML = '<option value="">Loading times</option>';
    timeSelect.disabled = true;

    if (!routeSelect.value || !stopSelect.value) {
      timeSelect.innerHTML = '<option value="">Select stop first</option>';
      return;
    }

    await loadTimes(routeSelect.value, stopSelect.value);
  });

  routeSummaryList?.addEventListener("click", (event) => {
    const button = event.target.closest(".detail-btn");
    if (!button) {
      return;
    }

    openRouteDetails(button.dataset.selection);
  });
}

// Start the parts needed for each page
initializeSearchPage();
initializeLoginPage();
initializeAdminPage();
// Re-check admin session whenever the page is shown.
// Browsers cache pages in BFCache and serve them on back/forward without re-running scripts,
// so the on-load access check doesn't fire. pageshow runs even on BFCache restores.
window.addEventListener("pageshow", () => {
  if (window.location.pathname.toLowerCase().includes("admin.html")) {
    const session = getSession();
    if (!session || !session.username) {
      window.location.replace("./login.html?from=admin");
    }
  }
});