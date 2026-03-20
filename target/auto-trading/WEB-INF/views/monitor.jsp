<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Monitor</title>
<style>
:root{
  --bg:#0b1018;
  --panel:#141b26;
  --panel2:#1b2432;
  --line:#2a364b;
  --txt:#eaf0fb;
  --muted:#93a1b9;
  --ok:#28d391;
  --err:#ff6464;
  --accent:#6db6ff;
}
*{box-sizing:border-box}
body{
  margin:0;
  color:var(--txt);
  background:
    radial-gradient(1200px 600px at 10% -10%, #1f2f4a 0%, transparent 60%),
    radial-gradient(1000px 500px at 100% 0%, #1e2a3c 0%, transparent 60%),
    var(--bg);
  font-family:"Segoe UI","Noto Sans KR","Malgun Gothic",sans-serif;
}
.topbar{
  position:sticky;top:0;z-index:20;
  display:flex;align-items:center;gap:14px;
  padding:12px 16px;
  border-bottom:1px solid var(--line);
  background:rgba(10,15,23,.92);
  backdrop-filter:blur(10px);
}
.brand{font-weight:700;letter-spacing:.4px}
.nav{display:flex;gap:6px;flex-wrap:wrap}
.nav a{
  color:var(--muted);
  text-decoration:none;
  padding:6px 10px;
  border-radius:8px;
  border:1px solid transparent;
  font-size:13px;
}
.nav a:hover{border-color:var(--line);color:var(--txt)}
.nav a.active{
  color:var(--txt);
  border-color:#3d5b82;
  background:#1a2a3f;
}
.wrap{
  max-width:1400px;
  margin:0 auto;
  padding:14px;
  display:flex;
  flex-direction:column;
  gap:14px;
}
.toolbar{
  display:flex;
  align-items:center;
  gap:10px;
  flex-wrap:wrap;
  border:1px solid var(--line);
  background:linear-gradient(180deg,var(--panel),var(--panel2));
  border-radius:12px;
  padding:10px 12px;
}
.seg{display:flex;gap:6px}
.seg button,
.toolbar button,
.toolbar input{
  height:34px;
  border-radius:8px;
  border:1px solid var(--line);
  background:#101725;
  color:var(--txt);
  padding:0 12px;
}
.seg button{cursor:pointer}
.seg button.on{
  border-color:#2f6fb8;
  background:#173155;
  color:#cfe5ff;
}
.toolbar .hint{margin-left:auto;font-size:12px;color:var(--muted)}
.grid{
  display:grid;
  grid-template-columns:repeat(6, minmax(0, 1fr));
  gap:10px;
}
.kpi{
  border:1px solid var(--line);
  background:linear-gradient(180deg,var(--panel),var(--panel2));
  border-radius:12px;
  padding:12px;
}
.kpi .label{font-size:12px;color:var(--muted)}
.kpi .value{
  font-size:22px;
  font-weight:700;
  margin-top:6px;
  letter-spacing:.2px;
}
.kpi .sub{
  margin-top:4px;
  font-size:12px;
  color:var(--muted);
}
.up{color:var(--ok)}
.down{color:var(--err)}
.panel{
  border:1px solid var(--line);
  background:linear-gradient(180deg,var(--panel),var(--panel2));
  border-radius:12px;
  overflow:hidden;
}
.panel-hd{
  display:flex;align-items:center;justify-content:space-between;
  padding:10px 12px;
  border-bottom:1px solid var(--line);
}
.panel-hd .title{font-size:14px;font-weight:700}
.panel-bd{padding:12px}
.week{
  display:grid;
  grid-template-columns:repeat(7, minmax(0, 1fr));
  gap:8px;
  margin-bottom:8px;
}
.week div{
  text-align:center;
  font-size:12px;
  color:var(--muted);
  padding:4px 0;
}
.cal{
  display:grid;
  grid-template-columns:repeat(7, minmax(0, 1fr));
  gap:8px;
}
.day{
  min-height:88px;
  border:1px solid var(--line);
  border-radius:10px;
  padding:8px;
  background:#101725;
  display:flex;
  flex-direction:column;
  justify-content:space-between;
}
.day.empty{
  background:transparent;
  border-style:dashed;
  opacity:.35;
}
.day-num{font-size:12px;color:var(--muted)}
.day-val{
  font-size:13px;
  font-weight:700;
  text-align:right;
  white-space:nowrap;
  overflow:hidden;
  text-overflow:ellipsis;
}
.legend{
  margin-top:10px;
  display:flex;
  align-items:center;
  gap:10px;
  font-size:12px;
  color:var(--muted);
}
.chip{
  width:14px;height:14px;border-radius:4px;border:1px solid var(--line);
}
.chip.pos{background:rgba(40,211,145,.45)}
.chip.neg{background:rgba(255,100,100,.45)}
@media (max-width:1200px){
  .grid{grid-template-columns:repeat(3, minmax(0, 1fr))}
}
@media (max-width:700px){
  .grid{grid-template-columns:1fr}
  .toolbar .hint{width:100%;margin-left:0}
}
</style>
</head>
<body>
<div class="topbar">
  <div class="brand">AUTO TRADING</div>
  <div class="nav">
    <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a href="${pageContext.request.contextPath}/monitor" class="active">Monitor</a>
    <a href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a href="${pageContext.request.contextPath}/balances">Balances</a>
    <a href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a href="${pageContext.request.contextPath}/">Home</a>
  </div>
</div>

<div class="wrap">
  <div class="toolbar">
    <div class="seg">
      <button id="btnKr" class="on" onclick="setMarket('KR')">KR</button>
      <button id="btnUs" onclick="setMarket('US')">US</button>
    </div>
    <input id="monthInput" type="month">
    <button onclick="refreshAll()">Refresh</button>
    <div class="hint" id="statusHint">Ready</div>
  </div>

  <div class="grid">
    <div class="kpi">
      <div class="label">Total Evaluation</div>
      <div class="value" id="kEval">-</div>
      <div class="sub">Current held positions value</div>
    </div>
    <div class="kpi">
      <div class="label">Total Profit</div>
      <div class="value" id="kProfit">-</div>
      <div class="sub" id="kProfitRate">-</div>
    </div>
    <div class="kpi">
      <div class="label">Today Realized</div>
      <div class="value" id="kToday">-</div>
      <div class="sub" id="kTodayDetail">Today realized PnL (fills)</div>
    </div>
    <div class="kpi">
      <div class="label">Today Buy / Sell</div>
      <div class="value" id="kTodayFlow">-</div>
      <div class="sub">Execution amount</div>
    </div>
    <div class="kpi">
      <div class="label">Holding Count</div>
      <div class="value" id="kHolding">-</div>
      <div class="sub">Quantity >= 1</div>
    </div>
    <div class="kpi">
      <div class="label">Running Strategies</div>
      <div class="value" id="kRunning">-</div>
      <div class="sub">Current scheduler run count</div>
    </div>
  </div>

  <div class="panel">
    <div class="panel-hd">
      <div class="title" id="calTitle">Daily Realized PnL</div>
    </div>
    <div class="panel-bd">
      <div class="week">
        <div>Sun</div><div>Mon</div><div>Tue</div><div>Wed</div><div>Thu</div><div>Fri</div><div>Sat</div>
      </div>
      <div class="cal" id="calendarGrid"></div>
      <div class="legend">
        <span class="chip pos"></span><span>Profit</span>
        <span class="chip neg"></span><span>Loss</span>
      </div>
    </div>
  </div>
</div>

<script>
(function(){
  const BASE = '${pageContext.request.contextPath}';
  const nf = new Intl.NumberFormat('ko-KR');
  const pf = new Intl.NumberFormat('ko-KR', {minimumFractionDigits:2, maximumFractionDigits:2});

  let market = 'KR';

  const monthInput = document.getElementById('monthInput');
  const statusHint = document.getElementById('statusHint');
  const kEval = document.getElementById('kEval');
  const kProfit = document.getElementById('kProfit');
  const kProfitRate = document.getElementById('kProfitRate');
  const kToday = document.getElementById('kToday');
  const kTodayDetail = document.getElementById('kTodayDetail');
  const kTodayFlow = document.getElementById('kTodayFlow');
  const kHolding = document.getElementById('kHolding');
  const kRunning = document.getElementById('kRunning');
  const calTitle = document.getElementById('calTitle');
  const calendarGrid = document.getElementById('calendarGrid');

  function fmtMoney(value) {
    if (value === null || value === undefined || isNaN(value)) return '-';
    return nf.format(Math.round(Number(value)));
  }

  function fmtSigned(value) {
    if (value === null || value === undefined || isNaN(value)) return '-';
    const num = Number(value);
    return (num > 0 ? '+' : '') + fmtMoney(num);
  }

  function setSignedClass(el, value) {
    el.classList.remove('up', 'down');
    const num = Number(value);
    if (isNaN(num) || num === 0) return;
    el.classList.add(num > 0 ? 'up' : 'down');
  }

  function setStatus(text, isError) {
    statusHint.textContent = text;
    statusHint.style.color = isError ? 'var(--err)' : 'var(--muted)';
  }

  function setMarket(nextMarket) {
    market = nextMarket === 'US' ? 'US' : 'KR';
    document.getElementById('btnKr').classList.toggle('on', market === 'KR');
    document.getElementById('btnUs').classList.toggle('on', market === 'US');
    refreshAll();
  }

  function parseYearMonth(value) {
    if (!value || value.length !== 7) {
      const now = new Date();
      return {year: now.getFullYear(), month: now.getMonth() + 1};
    }
    const parts = value.split('-');
    return {year: Number(parts[0]), month: Number(parts[1])};
  }

  function toMonthLabel(year, month) {
    return year + '-' + String(month).padStart(2, '0');
  }

  function renderSummary(summary) {
    kEval.textContent = fmtMoney(summary.totalEvaluationAmount);

    kProfit.textContent = fmtSigned(summary.totalProfitAmount);
    setSignedClass(kProfit, summary.totalProfitAmount);

    kProfitRate.textContent = 'Rate: ' + pf.format(Number(summary.totalProfitRate || 0)) + '%';

    kToday.textContent = fmtSigned(summary.todayRealizedProfitAmount);
    setSignedClass(kToday, summary.todayRealizedProfitAmount);

    kTodayFlow.textContent = fmtMoney(summary.todayBuyAmount) + ' / ' + fmtMoney(summary.todaySellAmount);

    kTodayDetail.textContent = 'Buy ' + fmtMoney(summary.todayBuyAmount) + ' · Sell ' + fmtMoney(summary.todaySellAmount);

    kHolding.textContent = nf.format(Number(summary.holdingCount || 0));
    kRunning.textContent = nf.format(Number(summary.runningStrategyCount || 0));
  }

  function buildDailyMap(points) {
    const map = new Map();
    (points || []).forEach(function(p){
      if (!p || !p.tradeDate) return;
      map.set(String(p.tradeDate), Number(p.profitAmount || 0));
    });
    return map;
  }

  function renderCalendar(payload) {
    const year = Number(payload.year);
    const month = Number(payload.month);
    const points = Array.isArray(payload.data) ? payload.data : [];
    const pointMap = buildDailyMap(points);

    calTitle.textContent = 'Daily Realized PnL · ' + toMonthLabel(year, month) + ' · ' + market;

    const firstDay = new Date(year, month - 1, 1);
    const lastDate = new Date(year, month, 0).getDate();
    const startWeekday = firstDay.getDay();

    const cells = [];
    for (let i = 0; i < startWeekday; i++) {
      cells.push('<div class="day empty"></div>');
    }

    for (let day = 1; day <= lastDate; day++) {
      const key = toMonthLabel(year, month) + '-' + String(day).padStart(2, '0');
      const profit = pointMap.has(key) ? Number(pointMap.get(key)) : 0;
      const cls = profit > 0 ? 'up' : (profit < 0 ? 'down' : '');
      const text = pointMap.has(key) ? fmtSigned(profit) : '-';
      cells.push(
        '<div class="day">' +
          '<div class="day-num">' + day + '</div>' +
          '<div class="day-val ' + cls + '">' + text + '</div>' +
        '</div>'
      );
    }

    calendarGrid.innerHTML = cells.join('');
  }

  function fetchSummary() {
    return fetch(BASE + '/api/monitor/summary?market=' + encodeURIComponent(market))
      .then(function(res){
        if (!res.ok) throw new Error('summary HTTP ' + res.status);
        return res.json();
      });
  }

  function fetchCalendar() {
    const ym = parseYearMonth(monthInput.value);
    return fetch(
      BASE + '/api/monitor/calendar?market=' + encodeURIComponent(market)
      + '&year=' + encodeURIComponent(ym.year)
      + '&month=' + encodeURIComponent(ym.month)
    ).then(function(res){
      if (!res.ok) throw new Error('calendar HTTP ' + res.status);
      return res.json();
    });
  }

  function refreshAll() {
    setStatus('Loading...', false);
    Promise.all([fetchSummary(), fetchCalendar()])
      .then(function(results){
        renderSummary(results[0] || {});
        renderCalendar(results[1] || {});
        setStatus('Updated ' + new Date().toLocaleTimeString('ko-KR'), false);
      })
      .catch(function(err){
        console.error(err);
        setStatus('Failed to load monitor data: ' + err.message, true);
      });
  }

  function init() {
    const now = new Date();
    monthInput.value = toMonthLabel(now.getFullYear(), now.getMonth() + 1);
    monthInput.addEventListener('change', refreshAll);
    refreshAll();
  }

  window.setMarket = setMarket;
  window.refreshAll = refreshAll;
  init();
})();
</script>
</body>
</html>
