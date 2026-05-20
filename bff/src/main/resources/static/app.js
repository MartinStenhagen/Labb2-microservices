let accessToken = null;
let currentUsername = null;
let refreshTimer = null;

const loginView = document.getElementById("login-view");
const chatView = document.getElementById("chat-view");
const sessionStatus = document.getElementById("session-status");
const notice = document.getElementById("notice");
const messages = document.getElementById("messages");
const messageInput = document.getElementById("message-input");

document.getElementById("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const username = document.getElementById("login-username").value.trim();
    const password = document.getElementById("login-password").value;

    const response = await request("/api/login", {
        method: "POST",
        body: { username, password }
    });

    accessToken = response.accessToken;
    currentUsername = response.username;
    sessionStatus.textContent = `Inloggad som ${response.username}`;
    loginView.classList.add("hidden");
    chatView.classList.remove("hidden");
    showNotice("Inloggning lyckades.");
    await loadMessages();
    startAutoRefresh();
});

document.getElementById("user-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const username = document.getElementById("new-username").value.trim();
    const displayName = document.getElementById("display-name").value.trim();

    await request("/api/users", {
        method: "POST",
        body: { username, displayName }
    });

    showNotice(`Användaren ${username} skapades.`);
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
            body: { content }
        });

        messageInput.value = "";
        await waitForBotReply(createdMessage.id, content.toLowerCase().includes("@bot"));
    } finally {
        messageInput.disabled = false;
        messageInput.focus();
    }
}

document.getElementById("refresh-button").addEventListener("click", loadMessages);

document.getElementById("logout-button").addEventListener("click", () => {
    stopAutoRefresh();
    accessToken = null;
    currentUsername = null;
    messages.replaceChildren();
    messageInput.value = "";
    sessionStatus.textContent = "";
    chatView.classList.add("hidden");
    loginView.classList.remove("hidden");
    showNotice("Du är utloggad.");
});

async function loadMessages() {
    if (!accessToken) {
        return;
    }

    const data = await request("/api/messages", {
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
        throw new Error(text || `Request failed with status ${response.status}`);
    }

    return response.json();
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
