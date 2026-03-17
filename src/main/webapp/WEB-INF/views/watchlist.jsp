<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Watchlist</title>
    <style>
        @import url("https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;600&family=DM+Sans:wght@400;500&display=swap");
        :root {
            --bg1: #f6f3ef;
            --bg2: #e9f0f3;
            --ink: #1f2a2e;
            --muted: #5f6b6f;
            --accent: #136f63;
            --card: #ffffff;
            --line: #d9e2e7;
        }
        * { box-sizing: border-box; }
        body {
            margin: 0;
            font-family: "Space Grotesk", "DM Sans", sans-serif;
            color: var(--ink);
            background:
                radial-gradient(1200px 600px at 10% -10%, #ffe9c7 0%, transparent 55%),
                radial-gradient(900px 500px at 110% 10%, #cfe9e6 0%, transparent 55%),
                linear-gradient(180deg, var(--bg1), var(--bg2));
        }
        .page { max-width: 1100px; margin: 0 auto; padding: 40px 20px 64px; }
        .panel {
            background: var(--card);
            border: 1px solid var(--line);
            border-radius: 16px;
            padding: 20px;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
        }
        input[type="text"] {
            padding: 10px 12px;
            border: 1px solid var(--line);
            border-radius: 10px;
            width: 220px;
        }
        button {
            padding: 10px 14px;
            border: none;
            border-radius: 10px;
            color: #fff;
            background: linear-gradient(120deg, var(--accent), #0f5a50);
            cursor: pointer;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 16px;
            background: var(--card);
            border: 1px solid var(--line);
            border-radius: 14px;
            overflow: hidden;
        }
        th, td { padding: 10px 12px; text-align: left; font-size: 14px; }
        th { background: #f4f7f8; color: var(--muted); }
        tr:nth-child(even) td { background: #fbfcfd; }
        .nav a {
            text-decoration: none;
            color: #fff;
            background: linear-gradient(120deg, var(--accent), #0f5a50);
            padding: 8px 12px;
            border-radius: 10px;
            margin-right: 6px;
            display: inline-block;
        }
        .nav a.secondary {
            background: #fff;
            color: var(--accent);
            border: 1px solid #cfe1dd;
        }
        .row-actions button {
            padding: 6px 10px;
            border-radius: 8px;
            background: #c0392b;
        }
        .muted { color: var(--muted); font-size: 12px; }
        .message { margin-top: 10px; font-size: 13px; color: var(--muted); }
    </style>
</head>
<body>
<div class="page">
    <h1>Watchlist</h1>

    <div class="panel">
        <form id="addForm">
            <input id="symbolInput" type="text" name="symbol" placeholder="005930" autocomplete="off" />
            <button type="submit">Add</button>
            <span class="muted">Symbols only. Example: 005930</span>
        </form>
        <div class="message" id="msg"></div>

        <table>
            <thead>
            <tr>
                <th>ID</th>
                <th>Symbol</th>
                <th>Created At</th>
                <th>Action</th>
            </tr>
            </thead>
            <tbody id="listBody">
            <tr><td colspan="4">Loading...</td></tr>
            </tbody>
        </table>
    </div>

    <p class="nav">
        <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        <a class="secondary" href="${pageContext.request.contextPath}/control">Auto Control</a>
        <a class="secondary" href="${pageContext.request.contextPath}/history/orders">Order History</a>
        <a class="secondary" href="${pageContext.request.contextPath}/">Home</a>
    </p>
</div>

<script>
    (function () {
        var base = "${pageContext.request.contextPath}";
        var body = document.getElementById("listBody");
        var form = document.getElementById("addForm");
        var input = document.getElementById("symbolInput");
        var msg = document.getElementById("msg");

        function escapeHtml(value) {
            if (value === null || value === undefined) return "";
            return String(value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;")
                .replace(/'/g, "&#39;");
        }

        function renderEmpty(text) {
            body.innerHTML = "<tr><td colspan=\"4\">" + escapeHtml(text) + "</td></tr>";
        }

        function load() {
            fetch(base + "/api/watchlist")
                .then(function (res) { return res.json(); })
                .then(function (items) {
                    if (!items || items.length === 0) {
                        renderEmpty("No symbols.");
                        return;
                    }
                    body.innerHTML = items.map(function (item) {
                        return "<tr>" +
                            "<td>" + escapeHtml(item.id) + "</td>" +
                            "<td>" + escapeHtml(item.symbol) + "</td>" +
                            "<td>" + escapeHtml(item.createdAt) + "</td>" +
                            "<td class=\"row-actions\">" +
                            "<button type=\"button\" data-id=\"" + escapeHtml(item.id) + "\">Delete</button>" +
                            "</td>" +
                            "</tr>";
                    }).join("");
                })
                .catch(function () {
                    renderEmpty("Failed to load data.");
                });
        }

        function post(url, data) {
            return fetch(url, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded" },
                body: new URLSearchParams(data).toString()
            });
        }

        body.addEventListener("click", function (e) {
            var btn = e.target;
            if (btn && btn.tagName === "BUTTON" && btn.dataset.id) {
                var id = btn.dataset.id;
                post(base + "/api/watchlist/delete", { id: id })
                    .then(function () { load(); });
            }
        });

        form.addEventListener("submit", function (e) {
            e.preventDefault();
            var symbol = (input.value || "").trim();
            if (!symbol) {
                msg.textContent = "Symbol is required.";
                input.focus();
                return;
            }
            msg.textContent = "";
            post(base + "/api/watchlist", { symbol: symbol })
                .then(function () {
                    input.value = "";
                    load();
                })
                .catch(function () {
                    msg.textContent = "Failed to add symbol.";
                });
        });

        load();
    })();
</script>
</body>
</html>
