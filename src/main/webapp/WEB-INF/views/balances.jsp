<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>잔고 현황</title>
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;600&family=Pretendard:wght@400;500;600;700&display=swap" rel="stylesheet">
  <style>
    :root {
      --bg:        #080b10;
      --surface:   #0e1219;
      --surface2:  #141a24;
      --border:    rgba(255,255,255,.07);
      --accent:    #00e5a0;
      --accent2:   #3b82f6;
      --red:       #f43f5e;
      --text:      #e2e8f0;
      --muted:     #64748b;
      --mono:      'JetBrains Mono', monospace;
      --sans:      'Pretendard', sans-serif;
    }

    * { box-sizing: border-box; margin: 0; padding: 0; }

    body {
      font-family: var(--sans);
      background: var(--bg);
      color: var(--text);
      min-height: 100vh;
    }

    /* ── NAV ── */
    .nav {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 28px;
      border-bottom: 1px solid var(--border);
      background: var(--surface);
    }
    .nav-title {
      font-size: 13px;
      font-weight: 700;
      letter-spacing: .08em;
      color: var(--accent);
      text-transform: uppercase;
    }
    .nav-links { display: flex; gap: 4px; }
    .nav-links a {
      font-size: 11px;
      color: var(--muted);
      text-decoration: none;
      padding: 5px 10px;
      border-radius: 5px;
      transition: color .15s, background .15s;
    }
    .nav-links a:hover { color: var(--text); background: var(--surface2); }
    .nav-links a.active { color: var(--accent); }

    /* ── TABS ── */
    .wrap { max-width: 1160px; margin: 0 auto; padding: 28px 20px; }

    .tab-bar {
      display: flex;
      gap: 6px;
      margin-bottom: 24px;
      border-bottom: 1px solid var(--border);
      padding-bottom: 0;
    }
    .tab-btn {
      font-family: var(--sans);
      font-size: 13px;
      font-weight: 600;
      color: var(--muted);
      background: none;
      border: none;
      padding: 10px 18px;
      cursor: pointer;
      border-bottom: 2px solid transparent;
      margin-bottom: -1px;
      transition: color .15s, border-color .15s;
    }
    .tab-btn.active { color: var(--accent); border-bottom-color: var(--accent); }
    .tab-btn:hover:not(.active) { color: var(--text); }

    .tab-panel { display: none; }
    .tab-panel.active { display: block; }

    /* ── SUMMARY CARDS ── */
    .summary-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-bottom: 24px;
    }
    @media(max-width:800px){ .summary-row { grid-template-columns: 1fr 1fr; } }

    .stat-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      padding: 18px 20px;
      position: relative;
      overflow: hidden;
    }
    .stat-card::before {
      content: '';
      position: absolute;
      top: 0; left: 0; right: 0;
      height: 2px;
      background: linear-gradient(90deg, var(--accent), transparent);
      opacity: 0;
      transition: opacity .2s;
    }
    .stat-card:hover::before { opacity: 1; }

    .stat-label {
      font-size: 11px;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: .06em;
      margin-bottom: 8px;
    }
    .stat-value {
      font-family: var(--mono);
      font-size: 20px;
      font-weight: 600;
      color: var(--text);
      line-height: 1;
    }
    .stat-value.accent { color: var(--accent); }
    .stat-value.pos { color: var(--accent); }
    .stat-value.neg { color: var(--red); }
    .stat-sub {
      font-size: 11px;
      color: var(--muted);
      margin-top: 5px;
      font-family: var(--mono);
    }

    /* ── HOLDINGS TABLE ── */
    .section-title {
      font-size: 12px;
      font-weight: 700;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: .08em;
      margin-bottom: 12px;
    }

    .table-wrap {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 10px;
      overflow: hidden;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      font-size: 13px;
    }
    thead tr {
      background: var(--surface2);
      border-bottom: 1px solid var(--border);
    }
    th {
      padding: 11px 16px;
      text-align: right;
      font-size: 11px;
      font-weight: 600;
      color: var(--muted);
      text-transform: uppercase;
      letter-spacing: .06em;
      white-space: nowrap;
    }
    th:first-child { text-align: left; }

    tbody tr {
      border-bottom: 1px solid var(--border);
      transition: background .12s;
    }
    tbody tr:last-child { border-bottom: none; }
    tbody tr:hover { background: var(--surface2); }

    td {
      padding: 13px 16px;
      text-align: right;
      font-family: var(--mono);
      font-size: 13px;
      white-space: nowrap;
    }
    td:first-child {
      text-align: left;
      font-family: var(--sans);
      font-weight: 600;
    }
    .td-symbol {
      display: block;
      font-size: 11px;
      color: var(--muted);
      font-family: var(--mono);
      margin-top: 2px;
      font-weight: 400;
    }
    .pos { color: var(--accent); }
    .neg { color: var(--red); }
    .neutral { color: var(--text); }

    /* ── LOAD / EMPTY ── */
    .state-box {
      text-align: center;
      padding: 60px 20px;
      color: var(--muted);
      font-size: 13px;
    }
    .state-box .big { font-size: 32px; margin-bottom: 12px; }

    .btn-refresh {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      font-family: var(--sans);
      font-size: 12px;
      font-weight: 600;
      background: var(--accent);
      color: #080b10;
      border: none;
      border-radius: 6px;
      padding: 8px 16px;
      cursor: pointer;
      transition: opacity .15s;
      margin-bottom: 24px;
    }
    .btn-refresh:hover { opacity: .85; }
    .btn-refresh svg { width: 14px; height: 14px; }

    .exch-row {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 24px;
    }
    .exch-row select, .exch-row select {
      font-family: var(--sans);
      font-size: 12px;
      background: var(--surface);
      color: var(--text);
      border: 1px solid var(--border);
      border-radius: 6px;
      padding: 7px 10px;
    }

    .spinner {
      display: inline-block;
      width: 16px; height: 16px;
      border: 2px solid var(--border);
      border-top-color: var(--accent);
      border-radius: 50%;
      animation: spin .6s linear infinite;
      vertical-align: middle;
    }
    @keyframes spin { to { transform: rotate(360deg); } }

    .badge {
      display: inline-block;
      font-size: 10px;
      font-family: var(--mono);
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 600;
    }
    .badge-pos { background: rgba(0,229,160,.15); color: var(--accent); }
    .badge-neg { background: rgba(244,63,94,.15); color: var(--red); }
  </style>
</head>
<body>

<nav class="nav">
  <span class="nav-title">AutoTrading</span>
  <div class="nav-links">
    <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a href="${pageContext.request.contextPath}/control/kr">KR</a>
    <a href="${pageContext.request.contextPath}/control/us">US</a>
    <a href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a href="${pageContext.request.contextPath}/balances" class="active">Balances</a>
    <a href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
  </div>
</nav>

<div class="wrap">

  <div class="tab-bar">
    <button class="tab-btn active" data-tab="kr">국내 잔고</button>
    <button class="tab-btn"        data-tab="us">해외 잔고</button>
  </div>

  <!-- ── 국내 ── -->
  <div class="tab-panel active" id="tab-kr">
    <button class="btn-refresh" id="btnKr">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
        <path d="M1 4v6h6M23 20v-6h-6"/><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4-4.64 4.36A9 9 0 0 1 3.51 15"/>
      </svg>
      조회
    </button>

    <div class="summary-row" id="krSummary" style="display:none">
      <div class="stat-card">
        <div class="stat-label">총 자산</div>
        <div class="stat-value" id="krTotAsst">—</div>
        <div class="stat-sub" id="krTotAsstSub"></div>
      </div>
      <div class="stat-card">
        <div class="stat-label">순 자산</div>
        <div class="stat-value" id="krNass">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">예수금 (주문가능)</div>
        <div class="stat-value accent" id="krCash">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">평가손익 합계</div>
        <div class="stat-value" id="krPfls">—</div>
        <div class="stat-sub" id="krPflsPct"></div>
      </div>
    </div>

    <div class="section-title" id="krHoldTitle" style="display:none">보유 종목</div>
    <div class="table-wrap" id="krTable" style="display:none">
      <table>
        <thead>
          <tr>
            <th>종목</th>
            <th>수량</th>
            <th>평균단가</th>
            <th>현재가</th>
            <th>평가금액</th>
            <th>손익</th>
            <th>수익률</th>
          </tr>
        </thead>
        <tbody id="krTbody"></tbody>
      </table>
    </div>
    <div class="state-box" id="krState">
      <div class="big">📊</div>
      조회 버튼을 눌러 잔고를 불러오세요
    </div>
  </div>

  <!-- ── 해외 ── -->
  <div class="tab-panel" id="tab-us">
    <div class="exch-row">
      <select id="usExch">
        <option value="NASD">NASD (나스닥)</option>
        <option value="NYSE">NYSE</option>
        <option value="AMEX">AMEX</option>
      </select>
      <select id="usCcy">
        <option value="USD">USD</option>
        <option value="HKD">HKD</option>
        <option value="JPY">JPY</option>
      </select>
      <button class="btn-refresh" id="btnUs">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
          <path d="M1 4v6h6M23 20v-6h-6"/><path d="M20.49 9A9 9 0 0 0 5.64 5.64L1 10m22 4-4.64 4.36A9 9 0 0 1 3.51 15"/>
        </svg>
        조회
      </button>
    </div>

    <div class="summary-row" id="usSummary" style="display:none">
      <div class="stat-card">
        <div class="stat-label">총 자산 (외화)</div>
        <div class="stat-value" id="usTotAsst">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">예수금 (주문가능)</div>
        <div class="stat-value accent" id="usCash">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">매입금액 합계</div>
        <div class="stat-value" id="usPchs">—</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">평가손익 합계</div>
        <div class="stat-value" id="usPfls">—</div>
        <div class="stat-sub" id="usPflsPct"></div>
      </div>
    </div>

    <div class="section-title" id="usHoldTitle" style="display:none">보유 종목</div>
    <div class="table-wrap" id="usTable" style="display:none">
      <table>
        <thead>
          <tr>
            <th>종목</th>
            <th>수량</th>
            <th>평균단가</th>
            <th>현재가</th>
            <th>평가금액</th>
            <th>손익</th>
            <th>수익률</th>
          </tr>
        </thead>
        <tbody id="usTbody"></tbody>
      </table>
    </div>
    <div class="state-box" id="usState">
      <div class="big">🌐</div>
      거래소와 통화를 선택하고 조회하세요
    </div>
  </div>

</div><!-- /wrap -->

<script>
(function () {
  const BASE = '${pageContext.request.contextPath}';

  /* ── 탭 전환 ── */
  document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
      document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
      btn.classList.add('active');
      document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    });
  });

  /* ── 유틸 ── */
  const fmt = (v, decimals = 0) => {
    const n = parseFloat(String(v).replace(/,/g, '')) || 0;
    return n.toLocaleString('ko-KR', { minimumFractionDigits: decimals, maximumFractionDigits: decimals });
  };
  const fmtCcy = (v, ccy = '') => {
    const n = parseFloat(String(v).replace(/,/g, '')) || 0;
    return (ccy ? ccy + ' ' : '') + n.toLocaleString('ko-KR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  };
  const colorClass = n => n > 0 ? 'pos' : n < 0 ? 'neg' : 'neutral';
  const sign = n => n > 0 ? '+' : '';
  const setLoading = (stateEl, msg) => { stateEl.innerHTML = `<div class="big"><span class="spinner"></span></div>${msg}`; stateEl.style.display = ''; };
  const setError  = (stateEl, msg) => { stateEl.innerHTML = `<div class="big">⚠️</div>${msg}`; stateEl.style.display = ''; };

  /* ═══════════════════════════════════════════
     국내 잔고
  ══════════════════════════════════════════ */
  document.getElementById('btnKr').addEventListener('click', loadKr);

  async function loadKr() {
    const stateEl   = document.getElementById('krState');
    const summaryEl = document.getElementById('krSummary');
    const tableEl   = document.getElementById('krTable');
    const titleEl   = document.getElementById('krHoldTitle');

    summaryEl.style.display = 'none';
    tableEl.style.display   = 'none';
    titleEl.style.display   = 'none';
    setLoading(stateEl, '잔고 조회 중...');

    try {
      /* 1. 잔고 요약 (output2) */
      const balRes  = await fetch(BASE + '/api/account/balance/kr').then(r => r.json());
      if (balRes.status !== 'OK') throw new Error(balRes.message || '잔고 조회 실패');

      const o2 = Array.isArray(balRes.output2) ? balRes.output2[0] : balRes.output2;

      const totAsst  = parseFloat((o2?.tot_asst_amt  || '0').replace(/,/g,''));
      const nass     = parseFloat((o2?.nass_tot_amt   || '0').replace(/,/g,''));
      const cash     = parseFloat((o2?.dnca_tot_amt   || o2?.dncl_amt || '0').replace(/,/g,''));
      const pfls     = parseFloat((o2?.evlu_pfls_amt_smtl || '0').replace(/,/g,''));
      const pchsSmtl = parseFloat((o2?.pchs_amt_smtl || '1').replace(/,/g,'')) || 1;
      const pflsPct  = (pfls / pchsSmtl) * 100;

      document.getElementById('krTotAsst').textContent     = '₩ ' + fmt(totAsst);
      document.getElementById('krNass').textContent        = '₩ ' + fmt(nass);
      document.getElementById('krCash').textContent        = '₩ ' + fmt(cash);
      const pflsEl = document.getElementById('krPfls');
      pflsEl.textContent  = (pfls >= 0 ? '+' : '') + '₩ ' + fmt(pfls);
      pflsEl.className    = 'stat-value ' + colorClass(pfls);
      document.getElementById('krPflsPct').textContent = sign(pflsPct) + pflsPct.toFixed(2) + '%';

      summaryEl.style.display = '';

      /* 2. 보유 종목 */
      const holdRes = await fetch(BASE + '/api/account/holdings/kr').then(r => r.json()).catch(() => null);
      const items   = holdRes?.output1 || holdRes?.output || [];

      if (items.length > 0) {
        const tbody = document.getElementById('krTbody');
        tbody.innerHTML = '';
        items.forEach(row => {
          const name    = row.prdt_name  || row.pdno || '—';
          const symbol  = row.pdno       || '';
          const qty     = parseFloat((row.hldg_qty || '0').replace(/,/g,''));
          const avgP    = parseFloat((row.pchs_avg_pric || '0').replace(/,/g,''));
          const curP    = parseFloat((row.prpr || row.stck_prpr || '0').replace(/,/g,''));
          const evlu    = parseFloat((row.evlu_amt || '0').replace(/,/g,''));
          const pnl     = parseFloat((row.evlu_pfls_amt || '0').replace(/,/g,''));
          const pnlPct  = parseFloat((row.evlu_pfls_rt  || '0').replace(/,/g,''));
          const cc      = colorClass(pnl);

          tbody.insertAdjacentHTML('beforeend', `
            <tr>
              <td>${name}<span class="td-symbol">${symbol}</span></td>
              <td>${fmt(qty)}</td>
              <td>${fmt(avgP)}</td>
              <td>${fmt(curP)}</td>
              <td>${fmt(evlu)}</td>
              <td class="${cc}">${sign(pnl)}${fmt(pnl)}</td>
              <td><span class="badge badge-${pnl >= 0 ? 'pos' : 'neg'}">${sign(pnlPct)}${pnlPct.toFixed(2)}%</span></td>
            </tr>`);
        });
        titleEl.style.display = '';
        tableEl.style.display = '';
      }

      stateEl.style.display = 'none';

    } catch (e) {
      setError(stateEl, e.message || '오류가 발생했습니다');
    }
  }

  /* ═══════════════════════════════════════════
     해외 잔고
  ══════════════════════════════════════════ */
  document.getElementById('btnUs').addEventListener('click', loadUs);

  async function loadUs() {
    const exch      = document.getElementById('usExch').value;
    const ccy       = document.getElementById('usCcy').value;
    const stateEl   = document.getElementById('usState');
    const summaryEl = document.getElementById('usSummary');
    const tableEl   = document.getElementById('usTable');
    const titleEl   = document.getElementById('usHoldTitle');

    summaryEl.style.display = 'none';
    tableEl.style.display   = 'none';
    titleEl.style.display   = 'none';
    setLoading(stateEl, '잔고 조회 중...');

    try {
      const balRes = await fetch(`${BASE}/api/account/balance/us?exch=${encodeURIComponent(exch)}&currency=${encodeURIComponent(ccy)}`).then(r => r.json());
      if (balRes.status !== 'OK') throw new Error(balRes.message || '잔고 조회 실패');

      /* output2: 요약 */
      const o2 = Array.isArray(balRes.output2) ? balRes.output2[0] : balRes.output2;
      const totAsst  = parseFloat((o2?.tot_asst_amt || o2?.frcr_evlu_tota || '0').replace(/,/g,''));
      const cashAmt  = parseFloat((o2?.ord_psbl_frcr_amt || o2?.frcr_dncl_amt_2 || '0').replace(/,/g,''));
      const pchsSmtl = parseFloat((o2?.frcr_pchs_amt1 || o2?.pchs_amt || '1').replace(/,/g,'')) || 1;
      const pfls     = parseFloat((o2?.ovrs_tot_pfls || o2?.evlu_pfls_amt || '0').replace(/,/g,''));
      const pflsPct  = (pfls / pchsSmtl) * 100;

      document.getElementById('usTotAsst').textContent = fmtCcy(totAsst, ccy);
      document.getElementById('usCash').textContent    = fmtCcy(cashAmt, ccy);
      document.getElementById('usPchs').textContent    = fmtCcy(pchsSmtl, ccy);
      const pflsEl = document.getElementById('usPfls');
      pflsEl.textContent = (pfls >= 0 ? '+' : '') + fmtCcy(pfls, ccy);
      pflsEl.className   = 'stat-value ' + colorClass(pfls);
      document.getElementById('usPflsPct').textContent = sign(pflsPct) + pflsPct.toFixed(2) + '%';

      summaryEl.style.display = '';

      /* output1: 보유 종목 */
      const items = Array.isArray(balRes.output1) ? balRes.output1 : [];
      if (items.length > 0) {
        const tbody = document.getElementById('usTbody');
        tbody.innerHTML = '';
        items.forEach(row => {
          const name   = row.ovrs_item_name || row.prdt_name || row.ovrs_pdno || '—';
          const symbol = row.ovrs_pdno || row.pdno || '';
          const qty    = parseFloat((row.ovrs_cblc_qty || row.cblc_qty || '0').replace(/,/g,''));
          const avgP   = parseFloat((row.pchs_avg_pric || row.avg_unpr || '0').replace(/,/g,''));
          const curP   = parseFloat((row.now_pric2 || row.ovrs_stck_prpr || row.stck_prpr || '0').replace(/,/g,''));
          const evlu   = parseFloat((row.ovrs_stck_evlu_amt || row.evlu_amt || '0').replace(/,/g,''));
          const pnl    = parseFloat((row.frcr_evlu_pfls_amt || row.evlu_pfls_amt || '0').replace(/,/g,''));
          const pnlPct = parseFloat((row.evlu_pfls_rt || '0').replace(/,/g,''));
          const cc     = colorClass(pnl);

          tbody.insertAdjacentHTML('beforeend', `
            <tr>
              <td>${name}<span class="td-symbol">${symbol}</span></td>
              <td>${fmt(qty)}</td>
              <td>${fmtCcy(avgP)}</td>
              <td>${fmtCcy(curP)}</td>
              <td>${fmtCcy(evlu)}</td>
              <td class="${cc}">${sign(pnl)}${fmtCcy(pnl)}</td>
              <td><span class="badge badge-${pnl >= 0 ? 'pos' : 'neg'}">${sign(pnlPct)}${pnlPct.toFixed(2)}%</span></td>
            </tr>`);
        });
        titleEl.style.display = '';
        tableEl.style.display = '';
      }

      stateEl.style.display = 'none';

    } catch (e) {
      setError(stateEl, e.message || '오류가 발생했습니다');
    }
  }

})();
</script>
</body>
</html>
