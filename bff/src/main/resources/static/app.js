let accessToken = null;
let currentUsername = null;
let currentRoom = "general";
let currentBotPersonality = "neutral";
let aiBotEnabled = false;
let refreshTimer = null;

const SESSION_STORAGE_KEY = "labb2-chat-session";
const BOT_PERSONALITY_STORAGE_KEY = "labb2-chat-bot-personality";

const loginView = document.getElementById("login-view");
const chatView = document.getElementById("chat-view");
const sessionStatus = document.getElementById("session-status");
const notice = document.getElementById("notice");
const messages = document.getElementById("messages");
const messageInput = document.getElementById("message-input");
const roomSelect = document.getElementById("room-select");
const botPersonalityPicker = document.getElementById("bot-personality-picker");
const botPersonalitySelect = document.getElementById("bot-personality-select");

document.getElementById("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;

    const response = await request("/api/login", {
        method: "POST",
        body: { username, password }
    });

    await startSession(response, "Inloggning lyckades.");
});

document.getElementById("user-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const username = document.getElementById("new-username").value.trim();
    const displayName = document.getElementById("display-name").value.trim();
    const password = document.getElementById("new-password").value;

    const response = await request("/api/register", {
        method: "POST",
        body: { username, displayName, password }
    });

    await startSession(response, `Användaren ${username} skapades.`);
});

document.getElementById("message-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await sendMessage();
});

messageInput.addEventListener("keydown", async (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        await sendMessage();
    }
});

async function sendMessage() {
    const content = messageInput.value.trim();
    if (!content) {
        return;
    }

    messageInput.disabled = true;

    try {
        const createdMessage = await request("/api/messages", {
            method: "POST",
            auth: true,
            body: {
                content,
                room: currentRoom,
                botPersonality: aiBotEnabled ? currentBotPersonality : "neutral"
            }
        });

        messageInput.value = "";
        await waitForBotReply(createdMessage.id, content.toLowerCase().includes("@bot"));
    } finally {
        messageInput.disabled = false;
        messageInput.focus();
    }
}

document.getElementById("refresh-button").addEventListener("click", loadMessages);

roomSelect.addEventListener("change", async () => {
    currentRoom = roomSelect.value;
    await loadMessages();
});

botPersonalitySelect.addEventListener("change", () => {
    currentBotPersonality = normalizeBotPersonality(botPersonalitySelect.value);
    botPersonalitySelect.value = currentBotPersonality;
    localStorage.setItem(BOT_PERSONALITY_STORAGE_KEY, currentBotPersonality);
});

document.getElementById("logout-button").addEventListener("click", () => {
    clearSession();
    showNotice("Du är utloggad.");
});

initialize().catch((error) => {
    clearSession();
    showNotice(error.message);
});

async function initialize() {
    restoreBotPersonality();
    await loadFrontendConfig();
    await restoreSession();
}

async function loadFrontendConfig() {
    try {
        const config = await request("/api/config", {
            method: "GET"
        });
        aiBotEnabled = config.aiBotEnabled === true;
    } catch {
        aiBotEnabled = false;
    }

    updateBotPersonalityUi();
}

async function loadMessages() {
    if (!accessToken) {
        return;
    }

    const data = await request(`/api/messages?room=${encodeURIComponent(currentRoom)}`, {
        method: "GET",
        auth: true
    });

    const items = Array.isArray(data) ? data : [];
    renderMessages(items);
    return items;
}

async function request(path, options) {
    const headers = {
        "Content-Type": "application/json"
    };

    if (options.auth) {
        headers.Authorization = `Bearer ${accessToken}`;
    }

    const response = await fetch(path, {
        method: options.method,
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
    });

    if (!response.ok) {
        const text = await response.text();
        if (options.auth && response.status === 401) {
            clearSession();
            showNotice("Sessionen har gått ut. Logga in igen.");
        }
        throw new Error(text || `Request failed with status ${response.status}`);
    }

    return response.json();
}

async function startSession(response, message) {
    accessToken = response.accessToken;
    currentUsername = response.username;
    currentRoom = roomSelect.value;
    saveSession(response);
    sessionStatus.textContent = `Inloggad som ${response.username}`;
    loginView.classList.add("hidden");
    chatView.classList.remove("hidden");
    showNotice(message);
    await loadMessages();
    startAutoRefresh();
}

async function restoreSession() {
    const savedSession = readSavedSession();
    if (!savedSession) {
        return;
    }

    accessToken = savedSession.accessToken;
    currentUsername = savedSession.username;
    currentRoom = roomSelect.value;
    sessionStatus.textContent = `Inloggad som ${savedSession.username}`;
    loginView.classList.add("hidden");
    chatView.classList.remove("hidden");
    showNotice("Du är fortfarande inloggad.");

    try {
        await loadMessages();
        startAutoRefresh();
    } catch (error) {
        clearSession();
        showNotice("Sessionen kunde inte återställas. Logga in igen.");
    }
}

function saveSession(response) {
    const session = {
        accessToken: response.accessToken,
        username: response.username,
        expiresAt: response.expiresAt
    };

    localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
}

function readSavedSession() {
    const rawSession = localStorage.getItem(SESSION_STORAGE_KEY);
    if (!rawSession) {
        return null;
    }

    try {
        const session = JSON.parse(rawSession);
        if (!session.accessToken || !session.username || !session.expiresAt) {
            localStorage.removeItem(SESSION_STORAGE_KEY);
            return null;
        }

        if (new Date(session.expiresAt).getTime() <= Date.now()) {
            localStorage.removeItem(SESSION_STORAGE_KEY);
            showNotice("Sessionen har gått ut. Logga in igen.");
            return null;
        }

        return session;
    } catch {
        localStorage.removeItem(SESSION_STORAGE_KEY);
        return null;
    }
}

function clearSession() {
    stopAutoRefresh();
    localStorage.removeItem(SESSION_STORAGE_KEY);
    accessToken = null;
    currentUsername = null;
    currentRoom = "general";
    roomSelect.value = currentRoom;
    messages.replaceChildren();
    messageInput.value = "";
    sessionStatus.textContent = "";
    chatView.classList.add("hidden");
    loginView.classList.remove("hidden");
}

function restoreBotPersonality() {
    currentBotPersonality = normalizeBotPersonality(localStorage.getItem(BOT_PERSONALITY_STORAGE_KEY));
    botPersonalitySelect.value = currentBotPersonality;
}

function updateBotPersonalityUi() {
    botPersonalityPicker.classList.toggle("hidden", !aiBotEnabled);
    botPersonalitySelect.value = currentBotPersonality;
}

function normalizeBotPersonality(value) {
    return value === "pirate" ? "pirate" : "neutral";
}

function renderMessages(items) {
    messages.replaceChildren();

    if (items.length === 0) {
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = "Inga meddelanden ännu.";
        messages.appendChild(empty);
        return;
    }

    for (const item of items) {
        const article = document.createElement("article");
        article.className = "message";

        if (item.senderUsername === "bot") {
            article.classList.add("bot-message");
        } else if (item.senderUsername === currentUsername) {
            article.classList.add("own-message");
        }

        const meta = document.createElement("div");
        meta.className = "message-meta";
        meta.textContent = `${item.senderUsername || "okänd"} · ${formatTime(item.createdAt)}`;

        const content = document.createElement("p");
        content.textContent = item.content;

        article.append(meta, content);
        messages.appendChild(article);
    }

    messages.scrollTop = messages.scrollHeight;
}

function formatTime(value) {
    if (!value) {
        return "";
    }

    return new Intl.DateTimeFormat("sv-SE", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    }).format(new Date(value));
}

function showNotice(message) {
    notice.textContent = message;
}

function delay(milliseconds) {
    return new Promise((resolve) => setTimeout(resolve, milliseconds));
}

async function waitForBotReply(sentMessageId, expectBotReply) {
    const attempts = expectBotReply ? 20 : 1;

    for (let attempt = 0; attempt < attempts; attempt++) {
        await delay(attempt === 0 ? 250 : 500);
        const items = await loadMessages();

        if (!expectBotReply) {
            return;
        }

        const botReplyExists = items.some((item) =>
            item.senderUsername === "bot" && item.id > sentMessageId
        );

        if (botReplyExists) {
            return;
        }
    }
}

function startAutoRefresh() {
    stopAutoRefresh();
    refreshTimer = window.setInterval(() => {
        loadMessages().catch((error) => showNotice(error.message));
    }, 1500);
}

function stopAutoRefresh() {
    if (refreshTimer) {
        window.clearInterval(refreshTimer);
        refreshTimer = null;
    }
}
