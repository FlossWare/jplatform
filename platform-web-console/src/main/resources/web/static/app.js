// JPlatform Management Console JavaScript

const API_BASE = '/api';
let cpuChart = null;
let memoryChart = null;
let currentAppId = null;
let refreshInterval = null;

// Initialize the application
document.addEventListener('DOMContentLoaded', function() {
    initCharts();
    loadApplications();
    startAutoRefresh();
});

// Start automatic refresh every 5 seconds
function startAutoRefresh() {
    refreshInterval = setInterval(loadApplications, 5000);
}

// Stop automatic refresh
function stopAutoRefresh() {
    if (refreshInterval) {
        clearInterval(refreshInterval);
        refreshInterval = null;
    }
}

// Initialize Chart.js charts
function initCharts() {
    const cpuCtx = document.getElementById('cpu-chart');
    const memoryCtx = document.getElementById('memory-chart');

    if (cpuCtx) {
        cpuChart = new Chart(cpuCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'CPU Usage (%)',
                    data: [],
                    borderColor: 'rgb(75, 192, 192)',
                    backgroundColor: 'rgba(75, 192, 192, 0.2)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100
                    }
                }
            }
        });
    }

    if (memoryCtx) {
        memoryChart = new Chart(memoryCtx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Memory Usage (MB)',
                    data: [],
                    borderColor: 'rgb(255, 99, 132)',
                    backgroundColor: 'rgba(255, 99, 132, 0.2)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }
}

// Load all applications from the API
async function loadApplications() {
    try {
        const response = await fetch(`${API_BASE}/applications`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const apps = await response.json();
        renderAppTable(apps);
        updatePlatformInfo(apps);
        updateConnectionStatus(true);
        updateLastUpdateTime();

        // Refresh current app detail if one is selected
        if (currentAppId) {
            const currentApp = apps.find(app => app.id === currentAppId);
            if (currentApp) {
                updateAppDetail(currentApp);
            }
        }
    } catch (error) {
        console.error('Error loading applications:', error);
        updateConnectionStatus(false);
        showError('Failed to load applications: ' + error.message);
    }
}

// Render the applications table
function renderAppTable(apps) {
    const tbody = document.getElementById('app-table-body');

    if (!apps || apps.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="no-data">No applications deployed</td></tr>';
        return;
    }

    tbody.innerHTML = apps.map(app => {
        const metrics = app.metrics || {};
        const cpu = metrics.cpuUsage != null ? metrics.cpuUsage.toFixed(1) + '%' : '-';
        const memory = metrics.memoryUsed != null ? (metrics.memoryUsed / 1024 / 1024).toFixed(0) + ' MB' : '-';

        return `
            <tr onclick="showDetail('${app.id}')">
                <td>${escapeHtml(app.id)}</td>
                <td>${escapeHtml(app.name || app.id)}</td>
                <td>${escapeHtml(app.version || '-')}</td>
                <td><span class="state-badge state-${app.state.toLowerCase()}">${app.state}</span></td>
                <td>${cpu}</td>
                <td>${memory}</td>
                <td onclick="event.stopPropagation()">
                    ${app.state === 'RUNNING'
                        ? `<button class="btn btn-small btn-danger" onclick="stopApp('${app.id}')">Stop</button>`
                        : `<button class="btn btn-small btn-success" onclick="startApp('${app.id}')">Start</button>`
                    }
                    <button class="btn btn-small btn-secondary" onclick="restartApp('${app.id}')">Restart</button>
                    <button class="btn btn-small btn-warning" onclick="undeployApp('${app.id}')">Undeploy</button>
                </td>
            </tr>
        `;
    }).join('');
}

// Update platform information
function updatePlatformInfo(apps) {
    const total = apps.length;
    const running = apps.filter(app => app.state === 'RUNNING').length;

    document.getElementById('total-apps').textContent = total;
    document.getElementById('running-apps').textContent = running;
    document.getElementById('platform-version').textContent = '1.0';
}

// Update connection status indicator
function updateConnectionStatus(connected) {
    const status = document.getElementById('connection-status');
    if (connected) {
        status.classList.remove('disconnected');
        status.querySelector('.status-text').textContent = 'Connected';
    } else {
        status.classList.add('disconnected');
        status.querySelector('.status-text').textContent = 'Disconnected';
    }
}

// Update last update timestamp
function updateLastUpdateTime() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString();
    document.getElementById('last-update').textContent = timeStr;
}

// Start an application
async function startApp(appId) {
    try {
        const response = await fetch(`${API_BASE}/applications/${appId}/start`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        await loadApplications();
        showSuccess(`Application ${appId} started successfully`);
    } catch (error) {
        console.error('Error starting application:', error);
        showError(`Failed to start application: ${error.message}`);
    }
}

// Stop an application
async function stopApp(appId) {
    try {
        const response = await fetch(`${API_BASE}/applications/${appId}/stop`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        await loadApplications();
        showSuccess(`Application ${appId} stopped successfully`);
    } catch (error) {
        console.error('Error stopping application:', error);
        showError(`Failed to stop application: ${error.message}`);
    }
}

// Restart an application
async function restartApp(appId) {
    try {
        const response = await fetch(`${API_BASE}/applications/${appId}/restart`, {
            method: 'POST'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        await loadApplications();
        showSuccess(`Application ${appId} restarted successfully`);
    } catch (error) {
        console.error('Error restarting application:', error);
        showError(`Failed to restart application: ${error.message}`);
    }
}

// Undeploy an application
async function undeployApp(appId) {
    if (!confirm(`Are you sure you want to undeploy application ${appId}?`)) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/applications/${appId}`, {
            method: 'DELETE'
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        await loadApplications();
        showSuccess(`Application ${appId} undeployed successfully`);

        if (currentAppId === appId) {
            hideDetail();
        }
    } catch (error) {
        console.error('Error undeploying application:', error);
        showError(`Failed to undeploy application: ${error.message}`);
    }
}

// Show deploy form
function showDeployForm() {
    document.getElementById('deploy-form').style.display = 'block';
}

// Hide deploy form
function hideDeployForm() {
    document.getElementById('deploy-form').style.display = 'none';
    document.getElementById('deploy-form').querySelector('form').reset();
}

// Deploy a new application
async function deployApp(event) {
    event.preventDefault();

    const appId = document.getElementById('app-id').value;
    const appName = document.getElementById('app-name').value;
    const version = document.getElementById('app-version').value;
    const artifact = document.getElementById('app-artifact').value;

    const deployment = {
        id: appId,
        name: appName,
        version: version,
        artifactPath: artifact
    };

    try {
        const response = await fetch(`${API_BASE}/applications`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(deployment)
        });

        if (!response.ok) {
            const error = await response.text();
            throw new Error(`HTTP ${response.status}: ${error}`);
        }

        hideDeployForm();
        await loadApplications();
        showSuccess(`Application ${appId} deployed successfully`);
    } catch (error) {
        console.error('Error deploying application:', error);
        showError(`Failed to deploy application: ${error.message}`);
    }
}

// Show application detail
async function showDetail(appId) {
    currentAppId = appId;

    try {
        const response = await fetch(`${API_BASE}/applications/${appId}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const app = await response.json();
        updateAppDetail(app);
        document.getElementById('app-detail').style.display = 'block';
    } catch (error) {
        console.error('Error loading application detail:', error);
        showError(`Failed to load application detail: ${error.message}`);
    }
}

// Update application detail view
function updateAppDetail(app) {
    document.getElementById('detail-title').textContent = `${app.name || app.id} Details`;
    document.getElementById('detail-id').textContent = app.id;
    document.getElementById('detail-name').textContent = app.name || '-';
    document.getElementById('detail-version').textContent = app.version || '-';

    const stateSpan = document.getElementById('detail-state');
    stateSpan.textContent = app.state;
    stateSpan.className = `info-value state-badge state-${app.state.toLowerCase()}`;

    // Update charts with metrics
    if (app.metrics) {
        updateChartData(cpuChart, app.metrics.cpuUsage || 0);
        updateChartData(memoryChart, (app.metrics.memoryUsed || 0) / 1024 / 1024);
    }
}

// Update chart data with new value
function updateChartData(chart, value) {
    if (!chart) return;

    const now = new Date();
    const timeLabel = now.toLocaleTimeString();

    chart.data.labels.push(timeLabel);
    chart.data.datasets[0].data.push(value);

    // Keep only last 20 data points
    if (chart.data.labels.length > 20) {
        chart.data.labels.shift();
        chart.data.datasets[0].data.shift();
    }

    chart.update();
}

// Hide application detail
function hideDetail() {
    currentAppId = null;
    document.getElementById('app-detail').style.display = 'none';
}

// Show success message
function showSuccess(message) {
    showNotification(message, 'success');
}

// Show error message
function showError(message) {
    showNotification(message, 'error');
}

// Show notification
function showNotification(message, type) {
    // Simple alert for now - could be enhanced with toast notifications
    if (type === 'error') {
        alert('Error: ' + message);
    } else {
        console.log('Success: ' + message);
    }
}

// Escape HTML to prevent XSS
function escapeHtml(text) {
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}
