<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
  <meta charset="UTF-8">
  <title>Account Balances</title>
  <style>
    body{font-family:Arial,Helvetica,sans-serif;margin:0;background:#0b0d12;color:#e8edf5;}
    .wrap{max-width:1100px;margin:0 auto;padding:22px;}
    .top{display:flex;align-items:center;justify-content:space-between;gap:12px;flex-wrap:wrap;}
    .nav a{color:#cfd6e6;text-decoration:none;padding:8px 10px;border:1px solid rgba(255,255,255,.08);border-radius:6px;margin-right:6px;font-size:12px;}
    .nav a:hover{border-color:#7bf5c3;color:#7bf5c3;}
    .grid{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-top:18px;}
    .card{background:#121722;border:1px solid rgba(255,255,255,.08);border-radius:10px;padding:14px;}
    .card h2{margin:0 0 10px;font-size:15px;}
    .row{display:flex;gap:8px;align-items:center;margin-bottom:10px;}
    input,select,button{border-radius:6px;border:1px solid rgba(255,255,255,.12);background:#0f131d;color:#e8edf5;padding:8px;font-size:12px;}
    button{cursor:pointer;border-color:#7bf5c3;color:#0b0d12;background:#7bf5c3;font-weight:600;}
    button:hover{background:#a3ffd9;}
    pre{background:#0f131d;border:1px solid rgba(255,255,255,.08);border-radius:8px;padding:10px;overflow:auto;max-height:420px;font-size:12px;}
  </style>
</head>
<body>
<div class="wrap">
  <div class="top">
    <h1 style="margin:0;font-size:18px;">Account Balances</h1>
    <div class="nav">
      <a href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
      <a href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
      <a href="${pageContext.request.contextPath}/control/us">Control·US</a>
      <a href="${pageContext.request.contextPath}/history/orders">Orders</a>
      <a href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
      <a href="${pageContext.request.contextPath}/balances">Balances</a>
      <a href="${pageContext.request.contextPath}/">Home</a>
    </div>
  </div>

  <div class="grid">
    <div class="card">
      <h2>국내 잔고</h2>
      <div class="row">
        <button id="btnKr">조회</button>
      </div>
      <pre id="krOut">click 조회</pre>
    </div>

    <div class="card">
      <h2>해외 잔고</h2>
      <div class="row">
        <label>거래소</label>
        <select id="exch">
          <option value="NASD">NASD</option>
          <option value="NAS">NASDAQ</option>
          <option value="NYSE">NYSE</option>
          <option value="AMEX">AMEX</option>
          <option value="SEHK">SEHK</option>
          <option value="TKSE">TKSE</option>
        </select>
        <label>통화</label>
        <select id="ccy">
          <option value="USD">USD</option>
          <option value="HKD">HKD</option>
          <option value="JPY">JPY</option>
          <option value="CNY">CNY</option>
          <option value="VND">VND</option>
        </select>
        <button id="btnUs">조회</button>
      </div>
      <pre id="usOut">click 조회</pre>
    </div>

    <div class="card">
      <h2>해외 예수금(현금)</h2>
      <div class="row">
        <label>통화</label>
        <select id="ccyCash">
          <option value="USD">USD</option>
          <option value="HKD">HKD</option>
          <option value="JPY">JPY</option>
          <option value="CNY">CNY</option>
          <option value="VND">VND</option>
        </select>
        <button id="btnCash">조회</button>
      </div>
      <pre id="cashOut">click 조회</pre>
    </div>
  </div>
</div>

<script>
(function(){
  const BASE='${pageContext.request.contextPath}';
  const btnKr=document.getElementById('btnKr');
  const btnUs=document.getElementById('btnUs');
  const krOut=document.getElementById('krOut');
  const usOut=document.getElementById('usOut');
  const btnCash=document.getElementById('btnCash');
  const cashOut=document.getElementById('cashOut');

  btnKr.addEventListener('click',()=>{
    krOut.textContent='Loading...';
    fetch(BASE+'/api/account/balance/kr')
      .then(r=>r.json()).then(json=>{
        krOut.textContent=JSON.stringify(json,null,2);
      }).catch(e=>{krOut.textContent='ERROR: '+e.message;});
  });

  btnUs.addEventListener('click',()=>{
    usOut.textContent='Loading...';
    const ex=document.getElementById('exch').value;
    const cc=document.getElementById('ccy').value;
    fetch(BASE+'/api/account/balance/us?exch='+encodeURIComponent(ex)+'&currency='+encodeURIComponent(cc))
      .then(r=>r.json()).then(json=>{
        usOut.textContent=JSON.stringify(json,null,2);
      }).catch(e=>{usOut.textContent='ERROR: '+e.message;});
  });

  btnCash.addEventListener('click',()=>{
    cashOut.textContent='Loading...';
    const cc=document.getElementById('ccyCash').value;
    fetch(BASE+'/api/account/cash/us?currency='+encodeURIComponent(cc))
      .then(r=>r.json()).then(json=>{
        cashOut.textContent=JSON.stringify(json,null,2);
      }).catch(e=>{cashOut.textContent='ERROR: '+e.message;});
  });
})();
</script>
</body>
</html>
