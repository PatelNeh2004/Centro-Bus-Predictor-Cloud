const searchBox = document.getElementById("searchBox");
const searchBtn = document.getElementById("searchBtn");
const stopsList = document.getElementById("stopsList");
const resultCount = document.getElementById("resultCount");
const modal = document.getElementById("modal");
const modalClose = document.getElementById("modalClose");
const modalContent = document.getElementById("modalContent");
const clearBtn = document.getElementById("clearFilters");

let allRoutes = [];
let allStops = [];
let selectedRoutes = [];
let activeChart = null;

const routeColors = [
    "#0b3d91", "#e8751a", "#0f8a4f", "#c9302c", "#7b2d8e",
    "#2a7ab5", "#d4670f", "#3d6b35", "#a8281f", "#5c2278"
];

function getRouteColor(idx) {
    return routeColors[idx % routeColors.length];
}

function hasBulletin(stop) {
    return stop.BulletinData != null && String(stop.BulletinData).trim() !== "";
}

fetch("/api/routes")
    .then(r => r.json())
    .then(routes => {
        allRoutes = routes;
        buildRouteCheckboxes();
        loadAllStops();
    });

function buildRouteCheckboxes() {
    const container = document.getElementById("routeFilters");
    container.innerHTML = "";

    allRoutes.forEach((route, idx) => {
        const label = document.createElement("label");
        label.className = "route-checkbox";

        const cb = document.createElement("input");
        cb.type = "checkbox";
        cb.value = route.RouteID;

        cb.addEventListener("change", () => {
            if (cb.checked) {
                selectedRoutes.push(route.RouteID);
            } else {
                selectedRoutes = selectedRoutes.filter(id => id !== route.RouteID);
            }
            renderStops();
        });

        const dot = document.createElement("span");
        dot.className = "route-dot";
        dot.style.background = getRouteColor(idx);

        const name = document.createElement("span");
        name.className = "route-label";
        name.textContent = route.Name;

        label.appendChild(cb);
        label.appendChild(dot);
        label.appendChild(name);
        container.appendChild(label);
    });
}

function loadAllStops() {
    stopsList.innerHTML = '<div class="loading-spinner"></div>';

    const promises = allRoutes.map((route, idx) => {
        return fetch(`/api/stops?routeId=${route.RouteID}`)
            .then(r => r.json())
            .then(stops => {
                return stops.map(s => ({
                    ...s,
                    RouteName: route.Name,
                    RouteID: route.RouteID,
                    routeIndex: idx
                }));
            })
            .catch(err => {
                console.warn(`failed to load stops for ${route.Name}:`, err);
                return [];
            });
    });

    Promise.all(promises).then(results => {
        allStops = results.flat();
        renderStops();
    });
}

function getFilteredStops() {
    const query = searchBox.value.trim().toLowerCase();
    let filtered = allStops;

    if (selectedRoutes.length > 0) {
        filtered = filtered.filter(s => selectedRoutes.includes(s.RouteID));
    }

    if (query.length > 0) {
        filtered = filtered.filter(s =>
            s.Name.toLowerCase().includes(query) ||
            s.RouteName.toLowerCase().includes(query)
        );
    }

    return filtered;
}

function renderStops() {
    const filtered = getFilteredStops();

    if (filtered.length === 0) {
        stopsList.innerHTML = `
            <div class="empty-state">
                <i class="lucide-search"></i>
                <p>No stops match your filters</p>
            </div>`;
        resultCount.innerHTML = `<span>0</span> stops`;
        return;
    }

    resultCount.innerHTML = `<span>${filtered.length}</span> stops`;
    stopsList.innerHTML = "";

    filtered.forEach(stop => {
        const card = document.createElement("div");
        card.className = "stop-card";

        const color = getRouteColor(stop.routeIndex);
        const initials = stop.Name.split(" ").slice(0, 2).map(w => w[0] || "").join("").toUpperCase();

        let bulletinHtml = "";
        if (hasBulletin(stop)) {
            bulletinHtml = `<div class="stop-bulletin"><i class="lucide-triangle-alert"></i><span>${stop.BulletinData}</span></div>`;
        }

        card.innerHTML = `
            <div class="stop-icon" style="background:${color}">${initials}</div>
            <div class="stop-info">
                <div class="stop-name">${stop.Name}</div>
                <div class="stop-route-tag">${stop.RouteName}</div>
                ${bulletinHtml}
            </div>
            <div class="stop-arrow">&rsaquo;</div>`;

        card.addEventListener("click", () => openModal(stop));
        stopsList.appendChild(card);
    });
}

function openModal(stop) {
    modal.classList.remove("hidden");
    document.body.style.overflow = "hidden";

    const color = getRouteColor(stop.routeIndex);

    let bulletinHtml = "";
    if (hasBulletin(stop)) {
        bulletinHtml = `<div class="modal-bulletin"><i class="lucide-triangle-alert"></i><span>${stop.BulletinData}</span></div>`;
    }

    modalContent.innerHTML = `
        <h2 class="modal-title">${stop.Name}</h2>
        <p class="modal-subtitle">${stop.RouteName}</p>
        ${bulletinHtml}
        <div class="modal-form">
            <div class="modal-form-row">
                <label for="modalTimeSelect">Arrival Time</label>
                <select id="modalTimeSelect" disabled><option>Loading times...</option></select>
                <button class="modal-submit" id="modalSubmitBtn" disabled>Submit</button>
            </div>
        </div>
        <div id="analysisResults"></div>`;

    fetch(`/api/times?stopId=${stop.StopID}`)
        .then(r => r.json())
        .then(times => {
            const sel = document.getElementById("modalTimeSelect");
            const btn = document.getElementById("modalSubmitBtn");

            if (times.length === 0) {
                sel.innerHTML = '<option value="">No scheduled times</option>';
                return;
            }

            sel.innerHTML = '<option value="">Select a time</option>';
            times.forEach(t => {
                sel.innerHTML += `<option value="${t.arrTime}">${t.arrTime}</option>`;
            });
            sel.disabled = false;

            sel.onchange = () => {
                btn.disabled = !sel.value;
            };

            btn.onclick = () => {
                if (!sel.value) return;
                btn.disabled = true;
                sel.disabled = true;
                runAnalysis(stop, sel.value);
            };
        });
}

function runAnalysis(stop, selectedTime) {
    const tf = document.querySelector('input[name="timeframe"]:checked').value;
    const tfLabel = tf === "7" ? "Last 7 days" : tf === "30" ? "Last 30 days" : "All data";
    const resultsDiv = document.getElementById("analysisResults");

    resultsDiv.style.display = "block";
    resultsDiv.innerHTML = '<div class="loading-spinner"></div>';

    fetch(`/api/arrivals?stopId=${stop.StopID}&routeId=${stop.RouteID}&tf=${tf}&arrTime=${encodeURIComponent(selectedTime)}`)
        .then(r => r.json())
        .then(data => {
            const arrivals = data.map(d => d.Difference);

            let html = '<div class="modal-divider"></div>';
            html += `<p class="modal-subtitle" style="margin-bottom:14px">
                ${stop.Name} at <strong>${selectedTime}</strong> &middot; ${tfLabel} &middot; ${arrivals.length} data points</p>`;

            if (arrivals.length === 0) {
                html += '<div class="empty-state" style="padding:30px 10px"><p>No arrival data yet</p></div>';
                resultsDiv.innerHTML = html;
                reenableForm();
                return;
            }

            let early = 0, ontime = 0, late = 0;
            arrivals.forEach(diff => {
                if (diff < 0) early++;
                else if (diff === 0) ontime++;
                else late++;
            });

            html += `
                <div class="stats-row">
                    <div class="stat-box early"><div class="stat-num">${early}</div><div class="stat-label">Early</div></div>
                    <div class="stat-box ontime"><div class="stat-num">${ontime}</div><div class="stat-label">On Time</div></div>
                    <div class="stat-box late"><div class="stat-num">${late}</div><div class="stat-label">Late</div></div>
                </div>
                <div class="modal-chart-wrap"><canvas id="modalChart"></canvas></div>`;

            resultsDiv.innerHTML = html;
            buildChart(arrivals);
            reenableForm();
        });
}

function reenableForm() {
    const sel = document.getElementById("modalTimeSelect");
    const btn = document.getElementById("modalSubmitBtn");
    if (sel) sel.disabled = false;
    if (btn) btn.disabled = false;
}

function buildChart(arrivals) {
    const counts = {};
    arrivals.forEach(val => {
        counts[val] = (counts[val] || 0) + 1;
    });

    const labels = Object.keys(counts).sort((a, b) => a - b);
    const values = labels.map(k => counts[k]);
    const colors = labels.map(k => {
        if (k < 0) return '#0f8a4f';
        if (k == 0) return '#0b3d91';
        return '#c9302c';
    });

    if (activeChart) activeChart.destroy();

    const canvas = document.getElementById("modalChart");
    activeChart = new Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels.map(l => `${l} min`),
            datasets: [{
                label: 'Occurrences',
                data: values,
                backgroundColor: colors,
                borderRadius: 4
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Count', font: { family: 'DM Sans' } },
                    ticks: { stepSize: 1 },
                    grid: { color: '#ecedf0' }
                },
                x: {
                    title: { display: true, text: 'Minutes from scheduled', font: { family: 'DM Sans' } },
                    grid: { display: false }
                }
            }
        }
    });
}

function closeModal() {
    modal.classList.add("hidden");
    document.body.style.overflow = "";
    if (activeChart) {
        activeChart.destroy();
        activeChart = null;
    }
}

modalClose.addEventListener("click", closeModal);

modal.addEventListener("click", e => {
    if (e.target === modal) closeModal();
});

searchBox.addEventListener("input", renderStops);
searchBtn.addEventListener("click", renderStops);
searchBox.addEventListener("keydown", e => {
    if (e.key === "Enter") renderStops();
});

const tfChips = document.querySelectorAll(".filter-chip");
tfChips.forEach(chip => {
    chip.addEventListener("click", () => {
        tfChips.forEach(c => c.classList.remove("selected"));
        chip.classList.add("selected");
        chip.querySelector("input").checked = true;
    });
});

clearBtn.addEventListener("click", () => {
    selectedRoutes = [];
    searchBox.value = "";

    tfChips.forEach(c => c.classList.remove("selected"));
    tfChips[0].classList.add("selected");
    tfChips[0].querySelector("input").checked = true;

    buildRouteCheckboxes();
    renderStops();
});

document.addEventListener("keydown", e => {
    if (e.key === "Escape" && !modal.classList.contains("hidden")) closeModal();
});