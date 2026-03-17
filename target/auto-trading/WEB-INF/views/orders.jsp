<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Order History</title>
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
        .top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
        table {
            width: 100%;
            border-collapse: collapse;
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
    </style>
</head>
<body>
<div class="page">
    <div class="top">
        <h1>Order / Fill History</h1>
    </div>

    <table>
        <thead>
        <tr>
            <th>ID</th>
            <th>Symbol</th>
            <th>Side</th>
            <th>Qty</th>
            <th>Price</th>
            <th>Reason</th>
            <th>Created At</th>
        </tr>
        </thead>
        <tbody id="ordersBody">
        <tr><td colspan="7">Loading...</td></tr>
        </tbody>
    </table>

    <p class="nav">
        <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        <a class="secondary" href="${pageContext.request.contextPath}/control/kr">Control · KR</a>
        <a class="secondary" href="${pageContext.request.contextPath}/control/us">Control · US</a>
        <a class="secondary" href="${pageContext.request.contextPath}/balances">Balances</a>
        <a class="secondary" href="${pageContext.request.contextPath}/history/orders">Orders</a>
        <a class="secondary" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
        <a class="secondary" href="${pageContext.request.contextPath}/">Home</a>
    </p>
</div>

<script>
    (function () {
        var base = "${pageContext.request.contextPath}";
        var body = document.getElementById("ordersBody");
        if (!body) return;

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
            return "<tr><td colspan=\"7\">" + escapeHtml(text) + "</td></tr>";
        }

        fetch(base + "/api/orders?limit=50")
            .then(function (res) { return res.json(); })
            .then(function (rows) {
                if (!rows || rows.length === 0) {
                    body.innerHTML = renderEmpty("No orders.");
                    return;
                }
                body.innerHTML = rows.map(function (row) {
                    return "<tr>" +
                        "<td>" + escapeHtml(row.id) + "</td>" +
                        "<td>" + escapeHtml(row.symbol) + "</td>" +
                        "<td>" + escapeHtml(row.side) + "</td>" +
                        "<td>" + escapeHtml(row.quantity) + "</td>" +
                        "<td>" + escapeHtml(row.price) + "</td>" +
                        "<td>" + escapeHtml(row.reason) + "</td>" +
                        "<td>" + escapeHtml(row.createdAt) + "</td>" +
                        "</tr>";
                }).join("");
            })
            .catch(function () {
                body.innerHTML = renderEmpty("Failed to load data.");
            });
    })();
</script>
</body>
</html>
