<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Watchlist</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Syne:wght@500;700;800&family=IBM+Plex+Mono:wght@400;500&family=Pretendard:wght@400;500;600&display=swap" rel="stylesheet">
    <style>
        :root {
            --bg: #0b0f0e;
            --bg2: #111716;
            --bg3: #171e1c;
            --card: #1a2220;
            --border: #243330;
            --green: #00e5a0;
            --red: #ff4e6a;
            --yellow: #f5c842;
            --blue: #4fb8ff;
            --muted: #4a6660;
            --text: #d4e8e4;
            --text2: #8ab5ae;
            --white: #ecf5f3;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: "Pretendard", sans-serif;
            background: var(--bg);
            color: var(--text);
            min-height: 100vh;
        }
        body::before {
            content: "";
            position: fixed;
            inset: 0;
            z-index: 0;
            pointer-events: none;
            background-image:
                    linear-gradient(rgba(0, 229, 160, .03) 1px, transparent 1px),
                    linear-gradient(90deg, rgba(0, 229, 160, .03) 1px, transparent 1px);
            background-size: 40px 40px;
        }
        .page {
            position: relative;
            z-index: 1;
            max-width: 1280px;
            margin: 0 auto;
            padding: 30px 20px 60px;
        }
        .header {
            display: flex;
            align-items: flex-end;
            justify-content: space-between;
            flex-wrap: wrap;
            gap: 16px;
            margin-bottom: 20px;
        }
        .header-label {
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            letter-spacing: .14em;
            color: var(--green);
            text-transform: uppercase;
            margin-bottom: 6px;
        }
        .header h1 {
            font-family: "Syne", sans-serif;
            font-size: 30px;
            font-weight: 800;
            color: var(--white);
        }
        .header h1 span { color: var(--green); }
        .nav { display: flex; flex-wrap: wrap; gap: 8px; }
        .nav a {
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            letter-spacing: .06em;
            text-decoration: none;
            padding: 7px 12px;
            border-radius: 8px;
            border: 1px solid var(--border);
            color: var(--text2);
            background: var(--card);
        }
        .nav a.primary {
            color: #071210;
            border-color: transparent;
            background: var(--green);
            font-weight: 600;
        }
        .nav a:hover { border-color: var(--green); color: var(--green); }
        .nav a.primary:hover { color: #071210; }

        .toolbar {
            background: var(--card);
            border: 1px solid var(--border);
            border-radius: 14px;
            padding: 16px;
            display: flex;
            gap: 10px;
            align-items: center;
            flex-wrap: wrap;
            margin-bottom: 16px;
        }
        .toolbar-label {
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            letter-spacing: .1em;
            color: var(--muted);
            text-transform: uppercase;
        }
        .toolbar input {
            width: 200px;
            font-family: "IBM Plex Mono", monospace;
            font-size: 13px;
            color: var(--white);
            background: var(--bg3);
            border: 1px solid var(--border);
            border-radius: 10px;
            padding: 10px 14px;
            outline: none;
        }
        .toolbar input:focus { border-color: var(--green); }
        .toolbar button {
            font-family: "IBM Plex Mono", monospace;
            font-size: 12px;
            letter-spacing: .05em;
            border: none;
            border-radius: 10px;
            padding: 10px 16px;
            cursor: pointer;
            font-weight: 600;
        }
        .btn-add { background: var(--green); color: #071210; }
        .btn-add:disabled { opacity: .6; cursor: not-allowed; }
        .msg {
            display: none;
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            padding: 6px 10px;
            border-radius: 8px;
        }
        .msg.success { display: inline-block; background: rgba(0, 229, 160, .1); color: var(--green); }
        .msg.error { display: inline-block; background: rgba(255, 78, 106, .12); color: var(--red); }
        .msg.info { display: inline-block; background: rgba(245, 200, 66, .12); color: var(--yellow); }

        .summary {
            display: flex;
            gap: 8px;
            margin-bottom: 14px;
            flex-wrap: wrap;
        }
        .badge {
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            padding: 4px 9px;
            border-radius: 8px;
            border: 1px solid var(--border);
            background: var(--card);
            color: var(--text2);
        }
        .badge strong { color: var(--white); }
        .badge.kr { color: var(--yellow); }
        .badge.us { color: var(--blue); }

        .grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 12px;
        }
        .panel {
            border: 1px solid var(--border);
            border-radius: 14px;
            overflow: hidden;
            background: var(--card);
        }
        .panel-head {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px 14px;
            border-bottom: 1px solid var(--border);
            background: var(--bg3);
        }
        .panel-head h2 {
            font-family: "Syne", sans-serif;
            font-size: 17px;
            font-weight: 700;
        }
        .panel-head .count {
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            color: var(--text2);
        }
        .table-wrap { overflow: auto; max-height: 560px; }
        table { width: 100%; border-collapse: collapse; min-width: 560px; }
        thead th {
            position: sticky;
            top: 0;
            z-index: 1;
            text-align: left;
            font-family: "IBM Plex Mono", monospace;
            font-size: 11px;
            letter-spacing: .08em;
            text-transform: uppercase;
            color: var(--muted);
            background: var(--bg3);
            border-bottom: 1px solid var(--border);
            padding: 11px 14px;
            font-weight: 400;
        }
        tbody td {
            padding: 11px 14px;
            border-bottom: 1px solid rgba(36, 51, 48, .6);
            font-size: 13px;
        }
        tbody tr:hover { background: rgba(0, 229, 160, .03); }
        td.col-id { color: var(--muted); font-family: "IBM Plex Mono", monospace; width: 56px; }
        td.col-symbol { font-family: "Syne", sans-serif; font-weight: 700; color: var(--white); }
        td.col-name { color: var(--text2); }
        td.col-date { color: var(--muted); font-family: "IBM Plex Mono", monospace; font-size: 11px; }
        .btn-del {
            border: 1px solid rgba(255, 78, 106, .3);
            color: var(--red);
            background: transparent;
            border-radius: 7px;
            padding: 4px 10px;
            font-size: 11px;
            cursor: pointer;
        }
        .btn-del:disabled { opacity: .5; cursor: not-allowed; }
        .empty {
            text-align: center;
            color: var(--muted);
            padding: 30px 14px;
            font-family: "IBM Plex Mono", monospace;
            font-size: 12px;
        }
        @media (max-width: 980px) {
            .grid { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
<div class="page">
    <div class="header">
        <div>
            <div class="header-label">AUTO TRADING SYSTEM</div>
            <h1>Watch<span>list</span></h1>
        </div>
        <nav class="nav">
            <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
            <a href="${pageContext.request.contextPath}/control/kr">Control-KR</a>
            <a href="${pageContext.request.contextPath}/control/us">Control-US</a>
            <a href="${pageContext.request.contextPath}/monitor">Monitor</a>
            <a href="${pageContext.request.contextPath}/history/orders">Orders</a>
            <a href="${pageContext.request.contextPath}/balances">Balances</a>
            <a class="primary" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
            <a href="${pageContext.request.contextPath}/">Home</a>
        </nav>
    </div>

    <div class="toolbar">
        <span class="toolbar-label">Add Symbol</span>
        <input id="symbolInput" type="text" placeholder="005930 or AAPL" maxlength="12" autocomplete="off">
        <button id="btnAdd" class="btn-add" type="button">+ Add</button>
        <span id="msg" class="msg"></span>
    </div>

    <div class="summary">
        <span class="badge"><strong>Total</strong> <span id="totalCount">0</span></span>
        <span class="badge kr"><strong>KR</strong> <span id="krCount">0</span></span>
        <span class="badge us"><strong>US</strong> <span id="usCount">0</span></span>
    </div>

    <div class="grid">
        <section class="panel">
            <div class="panel-head">
                <h2>한국 종목</h2>
                <span class="count" id="krPanelCount">0 symbols</span>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Symbol</th>
                        <th>Name</th>
                        <th>Added At</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="krBody">
                    <tr><td class="empty" colspan="5">Loading...</td></tr>
                    </tbody>
                </table>
            </div>
        </section>

        <section class="panel">
            <div class="panel-head">
                <h2>미국 종목</h2>
                <span class="count" id="usPanelCount">0 symbols</span>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Symbol</th>
                        <th>Name</th>
                        <th>Added At</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody id="usBody">
                    <tr><td class="empty" colspan="5">Loading...</td></tr>
                    </tbody>
                </table>
            </div>
        </section>
    </div>
</div>

<script>
(function () {
    var base = "${pageContext.request.contextPath}";
    var symbolInput = document.getElementById("symbolInput");
    var btnAdd = document.getElementById("btnAdd");
    var msg = document.getElementById("msg");
    var krBody = document.getElementById("krBody");
    var usBody = document.getElementById("usBody");
    var totalCount = document.getElementById("totalCount");
    var krCount = document.getElementById("krCount");
    var usCount = document.getElementById("usCount");
    var krPanelCount = document.getElementById("krPanelCount");
    var usPanelCount = document.getElementById("usPanelCount");

    var cachedItems = [];

    function esc(v) {
        if (v === null || v === undefined) return "";
        return String(v)
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#39;");
    }

    function isUsSymbol(symbol) {
        return !!symbol && /^[A-Za-z]/.test(symbol);
    }

    function normalizeType(item) {
        var exchange = String(item.exchange || "").toUpperCase();
        if (exchange === "KRX" || exchange === "KR") return "KR";
        if (exchange === "NAS" || exchange === "NYS" || exchange === "AMS" || exchange === "US") return "US";
        return isUsSymbol(item.symbol) ? "US" : "KR";
    }

    function showMsg(text, type) {
        msg.textContent = text;
        msg.className = "msg " + type;
    }

    function setCounts(krItems, usItems) {
        totalCount.textContent = String(krItems.length + usItems.length);
        krCount.textContent = String(krItems.length);
        usCount.textContent = String(usItems.length);
        krPanelCount.textContent = krItems.length + " symbols";
        usPanelCount.textContent = usItems.length + " symbols";
    }

    function renderRows(targetBody, items) {
        if (!items.length) {
            targetBody.innerHTML = "<tr><td class='empty' colspan='5'>종목이 없습니다.</td></tr>";
            return;
        }
        targetBody.innerHTML = items.map(function (item, idx) {
            return "<tr data-id='" + esc(item.id) + "'>" +
                "<td class='col-id'>" + (idx + 1) + "</td>" +
                "<td class='col-symbol'>" + esc(item.symbol) + "</td>" +
                "<td class='col-name' data-symbol='" + esc(item.symbol) + "'>" + esc(item.symbol) + "</td>" +
                "<td class='col-date'>" + esc(item.createdAt || "") + "</td>" +
                "<td><button type='button' class='btn-del' data-id='" + esc(item.id) + "'>삭제</button></td>" +
                "</tr>";
        }).join("");
    }

    function fillNames() {
        var cells = document.querySelectorAll("td[data-symbol]");
        cells.forEach(function (cell) {
            var symbol = cell.getAttribute("data-symbol");
            fetch(base + "/api/watchlist/name?symbol=" + encodeURIComponent(symbol), { cache: "no-store" })
                .then(function (res) { return res.ok ? res.json() : null; })
                .then(function (data) {
                    var name = data && (data.name || data.symbolName);
                    // Name should never be blank on UI.
                    cell.textContent = (name && String(name).trim() !== "") ? name : symbol;
                })
                .catch(function () {
                    cell.textContent = symbol;
                });
        });
    }

    function renderAll(items) {
        cachedItems = Array.isArray(items) ? items : [];
        var krItems = [];
        var usItems = [];
        cachedItems.forEach(function (item) {
            if (normalizeType(item) === "US") usItems.push(item);
            else krItems.push(item);
        });
        renderRows(krBody, krItems);
        renderRows(usBody, usItems);
        setCounts(krItems, usItems);
        fillNames();
    }

    function load() {
        fetch(base + "/api/watchlist", { cache: "no-store" })
            .then(function (res) { return res.json(); })
            .then(renderAll)
            .catch(function () {
                krBody.innerHTML = "<tr><td class='empty' colspan='5'>데이터를 불러오지 못했습니다.</td></tr>";
                usBody.innerHTML = "<tr><td class='empty' colspan='5'>데이터를 불러오지 못했습니다.</td></tr>";
                showMsg("Watchlist 조회 실패", "error");
            });
    }

    function addSymbol() {
        var symbol = (symbolInput.value || "").trim().toUpperCase();
        if (!symbol) {
            showMsg("종목코드를 입력하세요.", "error");
            symbolInput.focus();
            return;
        }
        var exists = cachedItems.some(function (it) {
            return String(it.symbol || "").toUpperCase() === symbol;
        });
        if (exists) {
            showMsg(symbol + " 은(는) 이미 등록되어 있습니다.", "info");
            return;
        }

        var exchange = /^[A-Za-z]/.test(symbol) ? "NAS" : "KRX";

        btnAdd.disabled = true;
        btnAdd.textContent = "...";
        fetch(base + "/api/watchlist", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "symbol=" + encodeURIComponent(symbol) + "&exchange=" + encodeURIComponent(exchange)
        })
            .then(function (res) { return res.json().catch(function () { return {}; }); })
            .then(function (payload) {
                btnAdd.disabled = false;
                btnAdd.textContent = "+ Add";
                if (payload.status === "DUPLICATE") {
                    showMsg(payload.message || (symbol + " 은(는) 이미 등록되어 있습니다."), "info");
                    return;
                }
                if (payload.status && payload.status !== "OK") {
                    showMsg(payload.message || "추가 실패", "error");
                    return;
                }
                symbolInput.value = "";
                showMsg(symbol + " 추가 완료", "success");
                load();
            })
            .catch(function () {
                btnAdd.disabled = false;
                btnAdd.textContent = "+ Add";
                showMsg("추가에 실패했습니다.", "error");
            });
    }

    function onDeleteClick(event) {
        var btn = event.target.closest(".btn-del");
        if (!btn) return;
        var id = btn.getAttribute("data-id");
        btn.disabled = true;
        btn.textContent = "...";
        fetch(base + "/api/watchlist/delete", {
            method: "POST",
            headers: { "Content-Type": "application/x-www-form-urlencoded" },
            body: "id=" + encodeURIComponent(id)
        })
            .then(function (res) {
                if (!res.ok) throw new Error("delete failed");
                showMsg("삭제 완료", "success");
                load();
            })
            .catch(function () {
                btn.disabled = false;
                btn.textContent = "삭제";
                showMsg("삭제 실패", "error");
            });
    }

    btnAdd.addEventListener("click", addSymbol);
    symbolInput.addEventListener("keydown", function (e) {
        if (e.key === "Enter") addSymbol();
    });
    krBody.addEventListener("click", onDeleteClick);
    usBody.addEventListener("click", onDeleteClick);

    load();
})();
</script>
</body>
</html>
