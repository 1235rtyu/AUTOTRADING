<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AUTOTRADE — Control</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=IBM+Plex+Mono:wght@300;400;500;600&family=Barlow:wght@400;500;600;700;800&display=swap" rel="stylesheet">
<style>
:root {
  --bg:#04050a; --surface:#090b12; --card:#0f1219; --card-hi:#141822; --hover:#191e28;
  --border:rgba(255,255,255,.055); --border-hi:rgba(255,255,255,.11);
  --lime:#c4ff3e; --lime-d:rgba(196,255,62,.09); --lime-bd:rgba(196,255,62,.28);
  --teal:#00e8a4; --teal-d:rgba(0,232,164,.08); --teal-bd:rgba(0,232,164,.25);
  --red:#ff3354;  --red-d:rgba(255,51,84,.08);   --red-bd:rgba(255,51,84,.28);
  --amber:#ffa724;--amber-d:rgba(255,167,36,.08);--amber-bd:rgba(255,167,36,.28);
  --blue:#3d9bff; --blue-d:rgba(61,155,255,.08);  --blue-bd:rgba(61,155,255,.25);
  --violet:#9d6fff;--violet-d:rgba(157,111,255,.08);--violet-bd:rgba(157,111,255,.25);
  --t1:#ecf0f9; --t2:#6e7a94; --t3:#333c52; --t4:#181e2c;
  --mono:'IBM Plex Mono',monospace; --sans:'Barlow',sans-serif;
  --r:5px; --r2:9px; --r3:13px; --topbar-h:52px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--bg);overflow-x:hidden;-webkit-font-smoothing:antialiased;}

.bg-mesh{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:
    radial-gradient(ellipse 70% 50% at 20% -10%,rgba(196,255,62,.06) 0%,transparent 55%),
    radial-gradient(ellipse 50% 60% at 90% 110%,rgba(0,232,164,.04) 0%,transparent 50%),
    radial-gradient(ellipse 40% 40% at 50% 50%,rgba(61,155,255,.02) 0%,transparent 60%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:linear-gradient(rgba(196,255,62,.016) 1px,transparent 1px),
    linear-gradient(90deg,rgba(196,255,62,.016) 1px,transparent 1px);
  background-size:36px 36px;}

@keyframes fadeD{from{opacity:0;transform:translateY(-8px)}to{opacity:1;transform:translateY(0)}}
@keyframes fadeU{from{opacity:0;transform:translateY(10px)}to{opacity:1;transform:translateY(0)}}
@keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.25;transform:scale(.6)}}
@keyframes spin{to{transform:rotate(360deg)}}
@keyframes logIn{from{opacity:0;transform:translateX(-6px)}to{opacity:1;transform:translateX(0)}}
@keyframes flash{0%{background:rgba(196,255,62,.12)}100%{background:transparent}}
@keyframes blink{0%,100%{opacity:1}50%{opacity:.3}}

/* ══ TOPBAR ══ */
.topbar{position:fixed;top:0;left:0;right:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(4,5,10,.93);backdrop-filter:blur(18px);
  border-bottom:1px solid var(--border);animation:fadeD .4s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent 0%,var(--lime) 35%,rgba(196,255,62,.15) 65%,transparent 100%);opacity:.55;}
.tb-logo{display:flex;align-items:center;gap:10px;padding:0 18px;height:100%;border-right:1px solid var(--border);min-width:180px;}
.logo-sq{width:30px;height:30px;background:var(--lime);border-radius:7px;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;
  box-shadow:0 0 18px rgba(196,255,62,.4);position:relative;overflow:hidden;}
.logo-sq::before{content:'';position:absolute;inset:0;background:linear-gradient(135deg,rgba(255,255,255,.3) 0%,transparent 55%);}
.logo-sq svg{width:15px;height:15px;}
.logo-nm{font-size:13px;font-weight:700;color:var(--t1);letter-spacing:.3px;}
.logo-nm span{color:var(--lime);}
.logo-vr{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}

/* 계좌 표시 pill */
.tb-acct{display:flex;align-items:center;gap:6px;padding:5px 12px;border-radius:20px;
  background:var(--violet-d);border:1px solid var(--violet-bd);margin-right:8px;}
.tb-acct-icon{font-size:10px;}
.tb-acct-no{font-family:var(--mono);font-size:10px;color:var(--violet);letter-spacing:.5px;}
.tb-acct-lbl{font-family:var(--mono);font-size:8px;color:var(--t3);}

.tb-eng{display:flex;align-items:center;gap:6px;padding:4px 12px;border-radius:20px;
  border:1px solid var(--border);}
.tb-eng-dot{width:5px;height:5px;border-radius:50%;}
.tb-eng-dot.run{background:var(--teal);box-shadow:0 0 8px var(--teal);animation:pulse 1.4s ease-in-out infinite;}
.tb-eng-dot.stop{background:var(--t3);}
.tb-eng-txt{font-family:var(--mono);font-size:10px;letter-spacing:.5px;}

.tb-nav{display:flex;align-items:center;gap:2px;padding:0 12px;}
.tb-a{font-family:var(--mono);font-size:10px;letter-spacing:.4px;padding:4px 10px;border-radius:var(--r);
  border:1px solid transparent;color:var(--t2);text-decoration:none;transition:all .14s;}
.tb-a:hover{background:var(--hover);border-color:var(--border-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-bd);color:var(--lime);}
.tb-login{display:flex;align-items:center;gap:5px;padding:0 12px;border-left:1px solid var(--border);}
.tb-login form{display:flex;align-items:center;gap:5px;}
.tb-login input{height:26px;width:108px;background:var(--surface);border:1px solid var(--border-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;outline:none;transition:border-color .15s;}
.tb-login input:focus{border-color:var(--lime-bd);}
.tb-login input::placeholder{color:var(--t3);}
.tb-lbtn{height:26px;padding:0 10px;border-radius:var(--r);border:1px solid var(--lime-bd);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:10px;cursor:pointer;transition:all .14s;}
.tb-lbtn:hover{background:var(--lime);color:var(--bg);}
.tb-lst{display:none;align-items:center;gap:7px;font-family:var(--mono);font-size:10px;color:var(--t2);}
.tb-lst .acc{color:var(--lime);font-weight:500;}
.tb-lerr{font-family:var(--mono);font-size:10px;color:var(--red);display:none;margin-left:2px;}
.tb-clock{padding:0 14px;height:100%;border-left:1px solid var(--border);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:14px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1px;}

/* ══ PAGE LAYOUT ══ */
.page{position:relative;z-index:1;padding:calc(var(--topbar-h) + 22px) 22px 44px;
  display:grid;grid-template-columns:360px 1fr;grid-template-rows:auto 1fr;gap:14px;
  min-height:100vh;}

/* ── PAGE HEADER ── */
.page-hd{grid-column:1/-1;animation:fadeU .35s ease both;}
.eyebrow{font-family:var(--mono);font-size:9px;color:var(--lime);letter-spacing:3px;text-transform:uppercase;
  margin-bottom:5px;display:flex;align-items:center;gap:8px;}
.eyebrow::before{content:'';width:18px;height:1px;background:var(--lime);opacity:.6;}
.page-title{font-size:26px;font-weight:700;color:var(--t1);letter-spacing:-.5px;line-height:1;}
.page-title span{color:var(--lime);}

/* ── PANEL ── */
.pn{background:var(--card);border:1px solid var(--border);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;}
.ph{flex-shrink:0;height:36px;display:flex;align-items:center;justify-content:space-between;
  padding:0 14px;border-bottom:1px solid var(--border);background:var(--card-hi);}
.ph-l{display:flex;align-items:center;gap:7px;}
.ph-dot{width:6px;height:6px;border-radius:50%;flex-shrink:0;}
.ph-nm{font-family:var(--mono);font-size:9px;font-weight:500;color:var(--t2);letter-spacing:1.8px;text-transform:uppercase;}
.ph-tag{font-family:var(--mono);font-size:9px;padding:2px 9px;border-radius:20px;
  border:1px solid var(--border);color:var(--t2);background:var(--bg);}

/* ── LEFT COLUMN ── */
.left-col{grid-column:1;grid-row:2;display:flex;flex-direction:column;gap:12px;}

/* ─ 계좌 / 엔진 상태 카드 ─ */
.acct-card{background:var(--card);border:1px solid var(--border);border-radius:var(--r3);padding:14px 16px;
  animation:fadeU .4s .05s ease both;}
.acct-row{display:flex;align-items:center;justify-content:space-between;margin-bottom:10px;}
.acct-label{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;}
.acct-value{font-family:var(--mono);font-size:13px;color:var(--t1);font-weight:500;}
.acct-value.highlight{color:var(--lime);}
.eng-status{display:flex;align-items:center;gap:8px;padding:10px 12px;
  border-radius:var(--r2);border:1px solid var(--border);}
.eng-dot{width:8px;height:8px;border-radius:50%;flex-shrink:0;}
.eng-dot.run{background:var(--teal);box-shadow:0 0 10px rgba(0,232,164,.5);animation:pulse 1.4s ease-in-out infinite;}
.eng-dot.stop{background:var(--t3);}
.eng-dot.err{background:var(--red);animation:blink 1s ease-in-out infinite;}
.eng-info{flex:1;min-width:0;}
.eng-state{font-family:var(--mono);font-size:11px;font-weight:500;color:var(--t1);}
.eng-msg{font-family:var(--mono);font-size:9px;color:var(--t3);margin-top:2px;
  white-space:nowrap;overflow:hidden;text-overflow:ellipsis;}
.eng-time{font-family:var(--mono);font-size:9px;color:var(--t3);white-space:nowrap;}

/* ─ 수동 종목 ─ */
.sym-row{display:flex;gap:6px;margin-bottom:8px;}
.sym-input{flex:1;height:34px;background:var(--bg);border:1px solid var(--border-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:12px;
  letter-spacing:1px;padding:0 10px;outline:none;transition:border-color .15s;}
.sym-input:focus{border-color:var(--lime-bd);}
.sym-input::placeholder{color:var(--t3);font-size:10px;}
.sym-add-btn{height:34px;padding:0 14px;border-radius:var(--r);border:1px solid var(--lime-bd);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:10px;
  cursor:pointer;transition:all .14s;white-space:nowrap;}
.sym-add-btn:hover{background:var(--lime);color:var(--bg);}
.sym-tags{display:flex;flex-wrap:wrap;gap:5px;min-height:26px;margin-bottom:10px;}
.stag{display:flex;align-items:center;gap:5px;background:var(--card-hi);border:1px solid var(--border-hi);
  border-radius:var(--r);padding:3px 9px;font-family:var(--mono);font-size:10px;color:var(--t1);
  animation:logIn .2s ease both;}
.stag.run{border-color:var(--teal-bd);color:var(--teal);background:var(--teal-d);}
.stag-del{color:var(--t3);cursor:pointer;font-size:10px;transition:color .12s;margin-left:2px;}
.stag-del:hover{color:var(--red);}
.sym-empty{font-family:var(--mono);font-size:10px;color:var(--t3);}
.act-btns{display:flex;flex-direction:column;gap:6px;}
.act-btn{width:100%;height:38px;border-radius:var(--r2);font-family:var(--mono);font-size:10px;
  font-weight:600;letter-spacing:1.5px;text-transform:uppercase;cursor:pointer;
  transition:all .16s;display:flex;align-items:center;justify-content:center;gap:7px;border:1px solid transparent;}
.act-btn.go{background:var(--teal-d);border-color:var(--teal-bd);color:var(--teal);}
.act-btn.go:hover{background:var(--teal);color:var(--bg);box-shadow:0 0 18px rgba(0,232,164,.35);transform:translateY(-1px);}
.act-btn.go-top{background:var(--lime-d);border-color:var(--lime-bd);color:var(--lime);}
.act-btn.go-top:hover{background:var(--lime);color:var(--bg);box-shadow:0 0 18px rgba(196,255,62,.35);transform:translateY(-1px);}
.act-btn.halt{background:var(--red-d);border-color:var(--red-bd);color:var(--red);}
.act-btn.halt:hover{background:var(--red);color:#fff;box-shadow:0 0 16px rgba(255,51,84,.35);transform:translateY(-1px);}
.act-btn.wl{background:var(--blue-d);border-color:var(--blue-bd);color:var(--blue);}
.act-btn.wl:hover{background:var(--blue);color:#fff;transform:translateY(-1px);}
.act-btn:active{transform:translateY(0)!important;}

/* top N 입력 */
.topn-row{display:flex;align-items:center;gap:8px;margin-bottom:6px;}
.topn-lbl{font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:.5px;min-width:72px;}
.topn-in{width:56px;height:28px;background:var(--bg);border:1px solid var(--border-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:11px;
  padding:0 8px;outline:none;text-align:center;transition:border-color .15s;}
.topn-in:focus{border-color:var(--lime-bd);}
.topn-desc{font-family:var(--mono);font-size:9px;color:var(--t3);}

/* 결과 토스트 */
.toast{margin:0 12px 10px;padding:9px 12px;border-radius:var(--r);
  font-family:var(--mono);font-size:10px;display:none;animation:fadeU .2s ease both;}
.toast.ok  {background:var(--teal-d); border:1px solid var(--teal-bd); color:var(--teal);}
.toast.err {background:var(--red-d);  border:1px solid var(--red-bd);  color:var(--red);}
.toast.info{background:var(--amber-d);border:1px solid var(--amber-bd);color:var(--amber);}
.toast.show{display:block;}

/* ─ TOP20 리스트 ─ */
.top20-wrap{flex:1;min-height:0;overflow-y:auto;}
.top20-wrap::-webkit-scrollbar{width:3px;}
.top20-wrap::-webkit-scrollbar-thumb{background:var(--border-hi);border-radius:2px;}
.t20-item{display:flex;align-items:center;gap:8px;padding:7px 12px;
  border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;}
.t20-item:last-child{border-bottom:none;}
.t20-item:hover{background:var(--hover);}
.t20-item.sel{background:var(--lime-d);}
.t20-chk{width:13px;height:13px;flex-shrink:0;accent-color:var(--lime);}
.t20-rank{font-family:var(--mono);font-size:10px;color:var(--t3);min-width:20px;text-align:center;}
.t20-rank.r1{color:var(--amber);font-size:11px;}
.t20-rank.r2{color:#9aa4b8;}
.t20-rank.r3{color:#b87a42;}
.t20-info{flex:1;min-width:0;}
.t20-nm{font-size:11px;font-weight:600;color:var(--t1);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.3;}
.t20-cd{font-family:var(--mono);font-size:8px;color:var(--t3);}
.t20-pr{text-align:right;flex-shrink:0;}
.t20-pv{font-family:var(--mono);font-size:10px;color:var(--t1);}
.t20-rt{font-family:var(--mono);font-size:9px;}
.t20-rt.up{color:var(--red);}
.t20-rt.dn{color:var(--blue);}
.t20-rt.fl{color:var(--t3);}
.t20-vol{font-family:var(--mono);font-size:8px;color:var(--t3);}

/* ══ RIGHT COLUMN ══ */
.right-col{grid-column:2;grid-row:2;display:flex;flex-direction:column;gap:12px;}

/* ─ 주문 패널 ─ */
.ord-pn{flex:1;min-height:0;display:flex;flex-direction:column;}
.ord-toolbar{flex-shrink:0;display:flex;align-items:center;gap:7px;flex-wrap:wrap;
  padding:8px 12px;border-bottom:1px solid var(--border);background:var(--card-hi);}
.srch-wrap{position:relative;flex:1;min-width:150px;}
.srch-icon{position:absolute;left:9px;top:50%;transform:translateY(-50%);color:var(--t3);font-size:13px;pointer-events:none;}
.srch-in{width:100%;height:28px;background:var(--bg);border:1px solid var(--border-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:11px;
  padding:0 9px 0 26px;outline:none;transition:border-color .14s;}
.srch-in:focus{border-color:var(--lime-bd);}
.srch-in::placeholder{color:var(--t3);}
.fg{display:flex;gap:2px;}
.fb{font-family:var(--mono);font-size:9px;padding:0 9px;height:28px;border-radius:var(--r);
  border:1px solid var(--border-hi);background:transparent;color:var(--t3);
  cursor:pointer;transition:all .13s;display:flex;align-items:center;gap:4px;}
.fb .bc{font-size:8px;padding:0 4px;border-radius:6px;background:var(--bg);min-width:15px;text-align:center;}
.fb:hover:not(.on){color:var(--t1);}
.fb.on.all {background:var(--lime-d); border-color:var(--lime-bd); color:var(--lime);}
.fb.on.buy {background:var(--teal-d); border-color:var(--teal-bd); color:var(--teal);}
.fb.on.sell{background:var(--red-d);  border-color:var(--red-bd);  color:var(--red);}
.ss,.ls{height:28px;background:var(--surface);border:1px solid var(--border-hi);
  border-radius:var(--r);color:var(--t2);font-family:var(--mono);font-size:9px;padding:0 7px;outline:none;cursor:pointer;}
option{background:var(--card-hi);}
.sp{flex:1;}
.rfb{width:28px;height:28px;border-radius:var(--r);border:1px solid var(--border-hi);
  background:transparent;color:var(--t2);cursor:pointer;transition:all .13s;
  display:flex;align-items:center;justify-content:center;}
.rfb:hover{border-color:var(--lime-bd);color:var(--lime);background:var(--lime-d);}
.rfb.spin svg{animation:spin .5s linear infinite;}
.exp-btn{height:28px;padding:0 10px;border:1px solid var(--border-hi);border-radius:var(--r);
  background:transparent;color:var(--t2);font-family:var(--mono);font-size:9px;cursor:pointer;transition:all .13s;}
.exp-btn:hover{border-color:var(--amber-bd);color:var(--amber);background:var(--amber-d);}
.tbl-sc{flex:1;min-height:0;overflow-y:auto;}
.tbl-sc::-webkit-scrollbar{width:3px;}
.tbl-sc::-webkit-scrollbar-thumb{background:var(--border-hi);border-radius:2px;}
table{width:100%;border-collapse:collapse;font-size:12px;table-layout:fixed;}
thead th{position:sticky;top:0;background:var(--card-hi);font-family:var(--mono);
  font-size:8px;color:var(--t2);font-weight:400;letter-spacing:1.5px;text-transform:uppercase;
  padding:7px 10px;text-align:left;border-bottom:1px solid var(--border);
  white-space:nowrap;cursor:pointer;user-select:none;transition:color .13s;}
thead th:hover{color:var(--t1);}
thead th.asc::after{content:' ▲';color:var(--lime);font-size:7px;}
thead th.desc::after{content:' ▼';color:var(--lime);font-size:7px;}
tbody td{padding:8px 10px;border-bottom:1px solid var(--t4);vertical-align:middle;
  overflow:hidden;text-overflow:ellipsis;white-space:nowrap;transition:background .08s;}
tbody tr:hover td{background:var(--hover);}
tbody tr:last-child td{border-bottom:none;}
.et{text-align:center;padding:36px!important;font-family:var(--mono);font-size:11px;color:var(--t3);letter-spacing:2px;}
.c-id{font-family:var(--mono);font-size:10px;color:var(--t3);}
.c-sym{font-family:var(--mono);font-size:12px;font-weight:600;color:var(--t1);}
.c-qty{font-family:var(--mono);font-size:11px;color:var(--t2);}
.c-pr{font-family:var(--mono);font-size:11px;color:var(--amber);font-weight:500;}
.c-msg{font-size:11px;color:var(--t2);}
.c-tm{font-family:var(--mono);font-size:10px;color:var(--t3);}
.s-buy{font-family:var(--mono);font-size:9px;font-weight:600;padding:2px 7px;border-radius:3px;
  color:var(--teal);background:var(--teal-d);border:1px solid var(--teal-bd);}
.s-sell{font-family:var(--mono);font-size:9px;font-weight:600;padding:2px 7px;border-radius:3px;
  color:var(--red);background:var(--red-d);border:1px solid var(--red-bd);}

/* 페이지네이션 */
.pgn{flex-shrink:0;display:flex;align-items:center;justify-content:space-between;
  padding:8px 12px;border-top:1px solid var(--border);background:var(--card-hi);}
.pg-info{font-family:var(--mono);font-size:9px;color:var(--t3);}
.pg-btns{display:flex;gap:3px;}
.pb{width:26px;height:26px;border-radius:var(--r);border:1px solid var(--border-hi);
  background:transparent;color:var(--t2);font-family:var(--mono);font-size:9px;
  cursor:pointer;transition:all .12s;display:flex;align-items:center;justify-content:center;}
.pb:hover:not(:disabled){border-color:var(--lime-bd);color:var(--lime);background:var(--lime-d);}
.pb.on{border-color:var(--lime-bd);background:var(--lime-d);color:var(--lime);}
.pb:disabled{opacity:.3;cursor:default;}

/* ── 활동 로그 패널 ── */
.log-pn{height:260px;flex-shrink:0;}
.log-body{flex:1;min-height:0;overflow-y:auto;padding:4px 0;}
.log-body::-webkit-scrollbar{width:3px;}
.log-body::-webkit-scrollbar-thumb{background:var(--border-hi);border-radius:2px;}
.log-item{display:flex;align-items:flex-start;gap:10px;padding:7px 14px;
  border-bottom:1px solid var(--t4);animation:logIn .25s ease both;}
.log-item:last-child{border-bottom:none;}
.log-icon{width:20px;height:20px;border-radius:50%;display:flex;align-items:center;justify-content:center;
  flex-shrink:0;margin-top:1px;font-size:9px;}
.log-icon.ok  {background:var(--teal-d); color:var(--teal);}
.log-icon.err {background:var(--red-d);  color:var(--red);}
.log-icon.info{background:var(--amber-d);color:var(--amber);}
.log-icon.sys {background:var(--blue-d); color:var(--blue);}
.log-body-inner{flex:1;min-width:0;}
.log-row1{display:flex;align-items:center;justify-content:space-between;margin-bottom:2px;}
.log-sym{font-family:var(--mono);font-size:11px;font-weight:600;color:var(--t1);}
.log-time{font-family:var(--mono);font-size:9px;color:var(--t3);}
.log-msg{font-size:11px;color:var(--t2);line-height:1.4;}
.log-detail{font-family:var(--mono);font-size:9px;color:var(--t3);margin-top:2px;}
.log-empty{padding:32px;text-align:center;font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:2px;}

/* KPI strip */
.kpi-row{display:grid;grid-template-columns:repeat(5,1fr);gap:8px;grid-column:1/-1;
  animation:fadeU .35s .05s ease both;}
.kpi{background:var(--card);border:1px solid var(--border);border-radius:var(--r3);
  padding:11px 14px;position:relative;overflow:hidden;transition:border-color .2s,transform .14s;}
.kpi:hover{border-color:var(--border-hi);transform:translateY(-1px);}
.kpi::after{content:'';position:absolute;top:0;left:0;right:0;height:2px;border-radius:var(--r3) var(--r3) 0 0;}
.kl::after{background:var(--lime);}  .ke::after{background:var(--teal);}
.kr::after{background:var(--red);}   .kg::after{background:var(--amber);}
.kb::after{background:var(--blue);}
.k-lb{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1.8px;text-transform:uppercase;margin-bottom:5px;}
.k-vl{font-family:var(--mono);font-size:20px;font-weight:500;letter-spacing:-1px;line-height:1;}
.kl .k-vl{color:var(--lime);}  .ke .k-vl{color:var(--teal);}
.kr .k-vl{color:var(--red);}   .kg .k-vl{color:var(--amber);}
.kb .k-vl{color:var(--blue);}
.k-sb{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:2px;}
</style>
</head>
<body>
<div class="bg-mesh"></div>
<div class="bg-grid"></div>

<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-sq">
      <svg viewBox="0 0 24 24" fill="none" stroke="#04050a" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-nm">AUTO<span>TRADE</span></div><div class="logo-vr">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-sp"></div>

  <!-- 계좌 표시 -->
  <div class="tb-acct" id="tbAcct" style="display:none">
    <span class="tb-acct-icon">💳</span>
    <div>
      <div class="tb-acct-no" id="tbAcctNo">****-**</div>
      <div class="tb-acct-lbl">계좌</div>
    </div>
  </div>

  <!-- 엔진 상태 -->
  <div class="tb-eng" id="tbEng">
    <div class="tb-eng-dot stop" id="tbEngDot"></div>
    <span class="tb-eng-txt" id="tbEngTxt" style="font-family:var(--mono);font-size:10px;color:var(--t2)">STOPPED</span>
  </div>

  <div class="tb-nav">
    <a class="tb-a" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a ${market == 'KR' ? 'cur' : ''}" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a ${market == 'US' ? 'cur' : ''}" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a" href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-login">
    <form id="lf">
      <input type="text" name="accountNo" placeholder="12345678-01" pattern="[0-9]{8}-[0-9]{2}" autocomplete="off"/>
      <input type="password" name="accountPassword" placeholder="Password" autocomplete="off"/>
      <button class="tb-lbtn" type="submit">Login</button>
    </form>
    <div class="tb-lst" id="lst">
      <span class="acc" id="lacc">****</span>
      <button id="lob" type="button" class="tb-lbtn">Logout</button>
    </div>
    <div class="tb-lerr" id="lerr"></div>
  </div>
  <div class="tb-clock">
    <div class="clk-t" id="clkT">--:--:--</div>
    <div class="clk-d" id="clkD">----.--.--</div>
  </div>
</nav>

<div class="page">

  <!-- PAGE HEADER -->
  <div class="page-hd">
    <div class="eyebrow">Automated Trading</div>
    <div class="page-title">Control <span>Center</span></div>
  </div>

  <!-- KPI -->
  <div class="kpi-row">
    <div class="kpi kl"><div class="k-lb">Total Orders</div><div class="k-vl" id="kpiTotal">—</div><div class="k-sb">전체 주문</div></div>
    <div class="kpi ke"><div class="k-lb">Buy Orders</div><div class="k-vl" id="kpiBuy">—</div><div class="k-sb">매수</div></div>
    <div class="kpi kr"><div class="k-lb">Sell Orders</div><div class="k-vl" id="kpiSell">—</div><div class="k-sb">매도</div></div>
    <div class="kpi kg"><div class="k-lb">Avg Price</div><div class="k-vl" id="kpiAvg" style="font-size:15px;padding-top:3px">—</div><div class="k-sb">평균 단가</div></div>
    <div class="kpi kb"><div class="k-lb">Symbols</div><div class="k-vl" id="kpiSyms">—</div><div class="k-sb">종목 수</div></div>
  </div>

  <!-- LEFT COLUMN -->
  <div class="left-col">

    <!-- 계좌 / 엔진 상태 -->
    <div class="acct-card">
      <div class="acct-row">
        <span class="acct-label">Account</span>
        <span class="acct-value highlight" id="acctDisplay">로그인 필요</span>
      </div>
      <div class="eng-status">
        <div class="eng-dot stop" id="engDot"></div>
        <div class="eng-info">
          <div class="eng-state" id="engState">STOPPED</div>
          <div class="eng-msg"  id="engMsg">—</div>
        </div>
        <div class="eng-time" id="engTime">—</div>
      </div>
    </div>

    <!-- 수동 종목 등록 -->
    <div class="pn" style="animation:fadeU .4s .1s ease both">
      <div class="ph">
        <div class="ph-l">
          <div class="ph-dot" style="background:var(--lime);box-shadow:0 0 8px rgba(196,255,62,.4)"></div>
          <div class="ph-nm">수동 종목 등록</div>
        </div>
        <span class="ph-tag" id="symCount">0개</span>
      </div>
      <div style="padding:12px;display:flex;flex-direction:column;gap:9px;">
        <div class="sym-row">
          <input class="sym-input" id="manualSym" placeholder="005930  또는  AAPL" maxlength="12"
            autocomplete="off" spellcheck="false" onkeydown="if(event.key==='Enter')addSym()"/>
          <button class="sym-add-btn" onclick="addSym()">＋ 추가</button>
        </div>
        <div class="sym-tags" id="symTags">
          <span class="sym-empty">종목을 추가해 주세요</span>
        </div>
        <div class="act-btns">
          <button class="act-btn go"   onclick="startManual()">▶ 선택 종목 자동매매 시작</button>
          <button class="act-btn halt" onclick="stopEngine()">■ 자동매매 중단</button>
        </div>
      </div>
      <div class="toast" id="toastManual"></div>
    </div>

    <!-- TOP20 자동 선택 -->
    <div class="pn" style="flex:1;min-height:0;animation:fadeU .4s .15s ease both">
      <div class="ph">
        <div class="ph-l">
          <div class="ph-dot" style="background:var(--amber);animation:pulse 1.8s ease-in-out infinite"></div>
          <div class="ph-nm">거래량 TOP 자동 선택</div>
        </div>
        <button class="ph-tag" style="cursor:pointer;border-color:var(--lime-bd);color:var(--lime);background:var(--lime-d)" onclick="loadTop20()">↻ 갱신</button>
      </div>
      <div style="padding:10px 12px 6px;display:flex;flex-direction:column;gap:7px;">
        <div class="topn-row">
          <span class="topn-lbl">상위 N개</span>
          <input class="topn-in" id="topN" type="number" value="3" min="1" max="20">
          <span class="topn-desc">개 선택</span>
        </div>
        <div class="topn-row">
          <span class="topn-lbl">최소 등락률</span>
          <input class="topn-in" id="minRate" type="number" value="0" min="0" step="0.5">
          <span class="topn-desc">% 이상</span>
        </div>
        <div class="act-btns" style="gap:5px;">
          <button class="act-btn wl"     onclick="addTopWL()">＋ 상위 N개 → Watchlist</button>
          <button class="act-btn go-top" onclick="startTop()">⚡ TOP 조건 자동매매 시작</button>
        </div>
      </div>
      <div class="toast" id="toastTop"></div>
      <div class="ph" style="flex-shrink:0;border-top:1px solid var(--border)">
        <div class="ph-l">
          <div class="ph-dot" style="background:var(--teal)"></div>
          <div class="ph-nm">실시간 거래량 TOP 20</div>
        </div>
        <span style="font-family:var(--mono);font-size:9px;color:var(--t3)" id="top20Upd">—</span>
      </div>
      <div class="top20-wrap" id="top20List">
        <div style="padding:24px;text-align:center;font-family:var(--mono);font-size:10px;color:var(--t3)">로딩 중…</div>
      </div>
    </div>
  </div><!-- /left-col -->

  <!-- RIGHT COLUMN -->
  <div class="right-col">

    <!-- 주문 이력 -->
    <div class="pn ord-pn">
      <div class="ph">
        <div class="ph-l">
          <div class="ph-dot" style="background:var(--lime);box-shadow:0 0 8px rgba(196,255,62,.35)"></div>
          <div class="ph-nm">Order / Fill History</div>
        </div>
        <span class="ph-tag" id="ordBadge">—</span>
      </div>
      <div class="ord-toolbar">
        <div class="srch-wrap">
          <span class="srch-icon">⌕</span>
          <input class="srch-in" id="srchIn" placeholder="Symbol, Reason…" oninput="applyF()">
        </div>
        <div class="fg">
          <button class="fb on all"  onclick="setSide('ALL',this)">ALL <span class="bc" id="fc-all">0</span></button>
          <button class="fb buy"     onclick="setSide('BUY',this)">BUY <span class="bc" id="fc-buy">0</span></button>
          <button class="fb sell"    onclick="setSide('SELL',this)">SELL <span class="bc" id="fc-sell">0</span></button>
        </div>
        <select class="ss" id="srtSel" onchange="applyF()">
          <option value="time-desc">Time ↓</option>
          <option value="time-asc">Time ↑</option>
          <option value="price-desc">Price ↓</option>
        </select>
        <select class="ls" id="limSel" onchange="fetchOrders()">
          <option value="50">50건</option>
          <option value="100">100건</option>
          <option value="200">200건</option>
        </select>
        <div class="sp"></div>
        <button class="rfb" id="rfBtn" onclick="fetchOrders()" title="새로고침">
          <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
          </svg>
        </button>
        <button class="exp-btn" onclick="exportCSV()">⬇ CSV</button>
      </div>
      <div class="tbl-sc">
        <table>
          <thead><tr>
            <th style="width:55px"  onclick="srtBy('id')">ID</th>
            <th style="width:95px"  onclick="srtBy('symbol')">Symbol</th>
            <th style="width:72px"  onclick="srtBy('side')">Side</th>
            <th style="width:62px">Qty</th>
            <th style="width:100px;text-align:right" onclick="srtBy('price')">Price</th>
            <th>Reason</th>
            <th style="width:130px;text-align:right" onclick="srtBy('time')">Time</th>
          </tr></thead>
          <tbody id="ordTb"><tr><td class="et" colspan="7">Loading…</td></tr></tbody>
        </table>
      </div>
      <div class="pgn">
        <span class="pg-info" id="pgInfo">—</span>
        <div class="pg-btns" id="pgBtns"></div>
      </div>
    </div>

    <!-- 활동 로그 -->
    <div class="pn log-pn">
      <div class="ph">
        <div class="ph-l">
          <div class="ph-dot" style="background:var(--violet);box-shadow:0 0 8px rgba(157,111,255,.4)"></div>
          <div class="ph-nm">Activity Log</div>
        </div>
        <div style="display:flex;gap:6px;align-items:center;">
          <span class="ph-tag" id="logBadge">0건</span>
          <button onclick="clearLog()" style="font-family:var(--mono);font-size:9px;padding:2px 8px;border-radius:var(--r);border:1px solid var(--border-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .13s;" onmouseover="this.style.color='var(--red)'" onmouseout="this.style.color='var(--t3)'">지우기</button>
        </div>
      </div>
      <div class="log-body" id="logBody">
        <div class="log-empty">활동 기록이 없습니다</div>
      </div>
    </div>

  </div><!-- /right-col -->
</div><!-- /page -->

<script>
(function(){
'use strict';
const B='${pageContext.request.contextPath}';
const CUR_MKT='${market != null ? market : "KR"}';
const CUR_EXCH=CUR_MKT==='US'?'NAS':'KRX';
const DAYS=['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const PAGE_SZ=20;

/* ── 시계 ── */
function tick(){
  const n=new Date(),p=v=>String(v).padStart(2,'0');
  document.getElementById('clkT').textContent=p(n.getHours())+':'+p(n.getMinutes())+':'+p(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p(n.getMonth()+1)+'.'+p(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000);tick();

/* ── 활동 로그 ── */
let logItems=[];
function addLog(type,sym,msg,detail){
  const now=new Date(),p=v=>String(v).padStart(2,'0');
  const ts=p(now.getHours())+':'+p(now.getMinutes())+':'+p(now.getSeconds());
  logItems.unshift({type,sym,msg,detail,ts});
  if(logItems.length>100)logItems.pop();
  renderLog();
}
function renderLog(){
  const body=document.getElementById('logBody');
  const badge=document.getElementById('logBadge');
  badge.textContent=logItems.length+'건';
  if(!logItems.length){
    body.innerHTML='<div class="log-empty">활동 기록이 없습니다</div>';
    return;
  }
  const icons={ok:'✓',err:'✕',info:'ℹ',sys:'⚙'};
  body.innerHTML=logItems.map((it,i)=>
    '<div class="log-item" style="animation-delay:'+i*15+'ms">' +
      '<div class="log-icon '+it.type+'">' + (icons[it.type]||'·') + '</div>' +
      '<div class="log-body-inner">' +
        '<div class="log-row1">' +
          '<span class="log-sym">' + esc(it.sym||'SYSTEM') + '</span>' +
          '<span class="log-time">' + it.ts + '</span>' +
        '</div>' +
        '<div class="log-msg">' + esc(it.msg) + '</div>' +
        (it.detail?'<div class="log-detail">'+esc(it.detail)+'</div>':'') +
      '</div>' +
    '</div>'
  ).join('');
}
window.clearLog=function(){logItems=[];renderLog();};

/* ── 로그인 & 계좌 표시 ── */
(function(){
  const f=document.getElementById('lf'),sb=document.getElementById('lst'),ac=document.getElementById('lacc');
  const lb=document.getElementById('lob'),eb=document.getElementById('lerr'),pat=/^\d{8}-\d{2}$/;
  function showIn(m){
    f.style.display='none';sb.style.display='inline-flex';eb.style.display='none';
    ac.textContent=m||'****';
    // 계좌 topbar pill 표시
    document.getElementById('tbAcct').style.display='flex';
    document.getElementById('tbAcctNo').textContent=m||'****';
    document.getElementById('acctDisplay').textContent=m||'****';
    addLog('sys','','로그인 완료: '+m,'');
  }
  function showOut(){
    sb.style.display='none';f.style.display='';eb.style.display='none';
    document.getElementById('tbAcct').style.display='none';
    document.getElementById('acctDisplay').textContent='로그인 필요';
  }
  const sE=m=>{eb.textContent=m||'';eb.style.display=m?'inline-flex':'none';};
  const post=(u,d)=>fetch(u,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(d).toString()}).then(r=>r.json());
  fetch(B+'/api/auth/status').then(r=>r.json()).then(d=>d&&d.loggedIn?showIn(d.accountMasked):showOut()).catch(()=>showOut());
  f.addEventListener('submit',ev=>{
    ev.preventDefault();
    const no=(f.accountNo.value||'').trim(),pw=(f.accountPassword.value||'').trim();
    if(!no||!pw)return;
    if(!pat.test(no)){sE('Format: 12345678-01');return;}
    post(B+'/api/auth/login',{accountNo:no,accountPassword:pw})
      .then(d=>d.status==='OK'?showIn(d.accountMasked):sE(d.message||'Login failed'));
  });
  lb.addEventListener('click',()=>post(B+'/api/auth/logout',{}).then(()=>{showOut();addLog('sys','','로그아웃','');}));
})();

/* ── 엔진 상태 업데이트 ── */
function setEngineStatus(status,msg){
  const dot=document.getElementById('engDot');
  const state=document.getElementById('engState');
  const msgEl=document.getElementById('engMsg');
  const tbDot=document.getElementById('tbEngDot');
  const tbTxt=document.getElementById('tbEngTxt');
  const now=new Date(),p=v=>String(v).padStart(2,'0');
  const ts=p(now.getHours())+':'+p(now.getMinutes())+':'+p(now.getSeconds());
  document.getElementById('engTime').textContent=ts;
  const isRun=status==='RUNNING';
  dot.className='eng-dot '+(isRun?'run':'stop');
  tbDot.className='tb-eng-dot '+(isRun?'run':'stop');
  state.textContent=status||'UNKNOWN';
  tbTxt.textContent=status||'UNKNOWN';
  tbTxt.style.color=isRun?'var(--teal)':'var(--t2)';
  if(msg)msgEl.textContent=msg;
}

function checkStatus(){
  fetch(B+'/api/control/status').then(r=>r.json()).then(d=>{
    setEngineStatus(d.status,'');
  }).catch(()=>{});
}

/* ══════════════════════════════════════════
   수동 종목
══════════════════════════════════════════ */
let manualSyms=[];

window.addSym=function(){
  const inp=document.getElementById('manualSym');
  const sym=(inp.value||'').trim().toUpperCase();
  if(!sym)return;
  if(manualSyms.find(s=>s.symbol===sym)){showToast('toastManual','info',sym+' 이미 추가됨');return;}
  manualSyms.push({symbol:sym,running:false});
  inp.value='';renderTags();
  document.getElementById('symCount').textContent=manualSyms.length+'개';
};
document.getElementById('manualSym').addEventListener('input',function(){this.value=this.value.toUpperCase();});

window.removeSym=function(sym){
  manualSyms=manualSyms.filter(s=>s.symbol!==sym);
  renderTags();document.getElementById('symCount').textContent=manualSyms.length+'개';
};

function renderTags(){
  const wrap=document.getElementById('symTags');
  if(!manualSyms.length){wrap.innerHTML='<span class="sym-empty">종목을 추가해 주세요</span>';return;}
  wrap.innerHTML=manualSyms.map(s=>
    '<div class="stag'+(s.running?' run':'')+'">' +
      '<span>'+esc(s.symbol)+'</span>' +
      '<span class="stag-del" onclick="removeSym(\''+esc(s.symbol)+'\')">✕</span>' +
    '</div>'
  ).join('');
}

window.startManual=function(){
  if(!manualSyms.length){showToast('toastManual','err','종목을 먼저 추가해주세요');return;}
  const syms=manualSyms.map(s=>s.symbol);
  addLog('info',syms[0],'자동매매 시작 시도…','종목: '+syms.join(', '));
  Promise.all(manualSyms.map(s=>
    fetch(B+'/api/watchlist',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'symbol='+encodeURIComponent(s.symbol)}).then(r=>r.json()).catch(()=>({}))
  )).then(()=>Promise.all(syms.map(s=>fetch(B+'/api/control/start',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'symbol='+encodeURIComponent(s)}).then(r=>r.json()))))
  .then(resps=>{
    manualSyms.forEach(s=>s.running=true);renderTags();
    const msg='자동매매 시작 완료: '+resps.map((d,i)=>syms[i]+' '+(d.message||'OK')).join(', ');
    const last=resps[resps.length-1]||{};
    setEngineStatus(last.status,last.message);
    showToast('toastManual','ok',msg);
    addLog('ok',syms[0],'자동매매 시작 성공',msg);
  }).catch(err=>{
    showToast('toastManual','err',err.message||'오류');
    addLog('err',syms[0]||'SYSTEM','자동매매 시작 실패',err.message||'');
  });
};

window.stopEngine=function(){
  addLog('info','SYSTEM','자동매매 중단 요청…','');
  fetch(B+'/api/control/stop',{method:'POST'}).then(r=>r.json()).then(d=>{
    manualSyms.forEach(s=>s.running=false);renderTags();
    setEngineStatus(d.status,d.message);
    showToast('toastManual','info','자동매매 중단: '+d.message);
    addLog('sys','SYSTEM','자동매매 중단',d.message||'');
  }).catch(err=>{
    addLog('err','SYSTEM','중단 실패',err.message||'');
  });
};

/* ══════════════════════════════════════════
   TOP20
══════════════════════════════════════════ */
let top20Data=[],top20Sel=new Set();

window.loadTop20=function(){
  const list=document.getElementById('top20List');
  list.innerHTML='<div style="padding:20px;text-align:center;font-family:var(--mono);font-size:10px;color:var(--lime)">◈ 로딩 중…</div>';
  fetch(B+'/api/market/ranking?market='+CUR_MKT+'&exch='+CUR_EXCH)
    .then(r=>r.json()).then(json=>{
      top20Data=(json.data||json.output||[]).slice(0,20);
      renderT20();
      const n=new Date(),p=v=>String(v).padStart(2,'0');
      document.getElementById('top20Upd').textContent=p(n.getHours())+':'+p(n.getMinutes())+' 갱신';
      addLog('sys','','거래량 TOP20 갱신',top20Data.length+'개 종목');
    }).catch(err=>{
      list.innerHTML='<div style="padding:20px;text-align:center;font-family:var(--mono);font-size:10px;color:var(--red)">로드 실패</div>';
      addLog('err','','TOP20 로드 실패',err.message||'');
    });
};

function renderT20(){
  const list=document.getElementById('top20List');
  if(!top20Data.length){list.innerHTML='<div style="padding:20px;text-align:center;color:var(--t3);font-family:var(--mono);font-size:10px">데이터 없음</div>';return;}
  list.innerHTML=top20Data.map(function(r,idx){
    const rank=idx+1,rnCls=rank<=3?'r'+rank:'';
    const code=r.symbol||'—',name=r.name||r.symbol||'—';
    const price=r.stck_prpr&&r.stck_prpr!=='0'?Number(r.stck_prpr).toLocaleString('ko-KR'):'—';
    const rate=parseFloat(r.prdy_ctrt||0);
    const sign=r.prdy_vrss_sign||'3';
    const isUp=sign==='1'||sign==='2',isDn=sign==='4'||sign==='5';
    const rateCls=isUp?'up':isDn?'dn':'fl';
    const rateTxt=(isUp?'▲':isDn?'▼':'')+Math.abs(rate).toFixed(2)+'%';
    const vol=parseFloat(r.acml_vol||0);
    const volFmt=vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
    const sel=top20Sel.has(code);
    return '<div class="t20-item'+(sel?' sel':'')+'" onclick="togT20(\''+esc(code)+'\')">' +
      '<input class="t20-chk" type="checkbox" '+(sel?'checked':'')+' onclick="event.stopPropagation();togT20(\''+esc(code)+'\')">' +
      '<span class="t20-rank '+rnCls+'">'+rank+'</span>' +
      '<div class="t20-info"><div class="t20-nm" title="'+esc(name)+'">'+esc(name)+'</div><div class="t20-cd">'+esc(code)+'</div></div>' +
      '<div class="t20-pr"><div class="t20-pv">'+price+'</div><div class="t20-rt '+rateCls+'">'+rateTxt+'</div><div class="t20-vol">'+volFmt+'</div></div>' +
    '</div>';
  }).join('');
}
window.togT20=function(code){
  if(top20Sel.has(code))top20Sel.delete(code);else top20Sel.add(code);renderT20();
};
window.addTopWL=function(){
  const n=parseInt(document.getElementById('topN').value)||3;
  const minR=parseFloat(document.getElementById('minRate').value)||0;
  addLog('info','SYSTEM','Watchlist 추가 시도','상위 '+n+'개, 최소등락률 '+minR+'%');
  fetch(B+'/api/watchlist/add-top?n='+n+'&minRate='+minR,{method:'POST'})
    .then(r=>r.json()).then(d=>{
      showToast('toastTop',d.status==='OK'?'ok':'err',d.message||'완료');
      addLog(d.status==='OK'?'ok':'err','SYSTEM',d.message||'완료','');
    }).catch(err=>{addLog('err','SYSTEM','Watchlist 추가 실패',err.message||'');});
};
window.startTop=function(){
  if(top20Sel.size>0){
    const syms=[...top20Sel];
    addLog('info',syms[0],'선택 종목 자동매매 시작 시도',syms.join(', '));
    Promise.all(syms.map(s=>fetch(B+'/api/watchlist',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'symbol='+encodeURIComponent(s)}).then(r=>r.json()).catch(()=>({}))))
      .then(()=>Promise.all(syms.map(s=>fetch(B+'/api/control/start',{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:'symbol='+encodeURIComponent(s)}).then(r=>r.json()))))
      .then(ds=>{
        const msg=ds.map((d,i)=>syms[i]+": "+(d.message||'OK')).join(', ');
        const last=ds[ds.length-1]||{};
        setEngineStatus(last.status,last.message);
        showToast('toastTop','ok',msg);
        addLog('ok',syms[0],'자동매매 시작',msg);
      })
      .catch(err=>{addLog('err',top20Sel.values().next().value||'?','시작 실패',err.message||'');});
  } else {
    const n=parseInt(document.getElementById('topN').value)||3;
    const minR=parseFloat(document.getElementById('minRate').value)||0;
    addLog('info','SYSTEM','TOP 조건 자동매매 시작 시도','상위 '+n+'개, 최소등락률 '+minR+'%');
    fetch(B+'/api/control/start-top?n='+n+'&minRate='+minR,{method:'POST'})
      .then(r=>r.json()).then(d=>{
        setEngineStatus(d.status,d.message);
        showToast('toastTop',d.status==='OK'?'ok':'err',d.message||'완료');
        addLog(d.status==='OK'?'ok':'err',d.firstSymbol||'?',d.message||'완료','');
      }).catch(err=>{addLog('err','SYSTEM','TOP 조건 시작 실패',err.message||'');});
  }
};

/* ══════════════════════════════════════════
   주문 이력
══════════════════════════════════════════ */
let rawOrders=[],sideF='ALL',srtKey='time',srtDir='desc',pg=1,prevIds=new Set();

function fetchOrders(){
  const btn=document.getElementById('rfBtn');btn.classList.add('spin');
  const lim=document.getElementById('limSel').value||50;
  fetch(B+'/api/orders?limit='+lim)
    .then(r=>r.json()).then(rows=>{
      rawOrders=Array.isArray(rows)?rows:[];
      pg=1;renderTable();updateKpi();
    }).catch(()=>{}).finally(()=>btn.classList.remove('spin'));
}

function updateKpi(){
  const d=rawOrders;
  const total=d.length,buy=d.filter(r=>r.side==='BUY').length,sell=d.filter(r=>r.side==='SELL').length;
  const prices=d.map(r=>parseFloat(r.price||0)).filter(v=>v>0);
  const avg=prices.length?(prices.reduce((a,b)=>a+b,0)/prices.length):null;
  const syms=new Set(d.map(r=>r.symbol)).size;
  document.getElementById('kpiTotal').textContent=total||'0';
  document.getElementById('kpiBuy').textContent=buy||'0';
  document.getElementById('kpiSell').textContent=sell||'0';
  document.getElementById('kpiAvg').textContent=avg!=null?avg.toLocaleString('ko-KR',{maximumFractionDigits:0}):'—';
  document.getElementById('kpiSyms').textContent=syms||'0';
  document.getElementById('fc-all').textContent=total;
  document.getElementById('fc-buy').textContent=buy;
  document.getElementById('fc-sell').textContent=sell;
  document.getElementById('ordBadge').textContent=total+'건';
}

function getFiltered(){
  const q=(document.getElementById('srchIn').value||'').toLowerCase();
  let d=rawOrders.filter(r=>{
    const sOk=sideF==='ALL'||r.side===sideF;
    const tOk=!q||(r.symbol||'').toLowerCase().includes(q)||(r.reason||'').toLowerCase().includes(q);
    return sOk&&tOk;
  });
  d.sort((a,b)=>{
    let va,vb;
    if(srtKey==='time'){va=a.createdAt||'';vb=b.createdAt||'';}
    else if(srtKey==='price'){va=parseFloat(a.price)||0;vb=parseFloat(b.price)||0;}
    else if(srtKey==='symbol'){va=a.symbol||'';vb=b.symbol||'';}
    else{va=a.id||0;vb=b.id||0;}
    if(va<vb)return srtDir==='asc'?-1:1;
    if(va>vb)return srtDir==='asc'?1:-1;
    return 0;
  });
  return d;
}

function renderTable(){
  const filtered=getFiltered();
  const total=filtered.length,pages=Math.max(1,Math.ceil(total/PAGE_SZ));
  pg=Math.min(pg,pages);
  const start=(pg-1)*PAGE_SZ,slice=filtered.slice(start,start+PAGE_SZ);
  document.getElementById('pgInfo').textContent=(total?start+1:0)+'–'+Math.min(start+PAGE_SZ,total)+' / '+total+'건';
  renderPgBtns(pages);
  const tb=document.getElementById('ordTb');
  if(!slice.length){tb.innerHTML='<tr><td class="et" colspan="7">주문 없음</td></tr>';return;}
  const newSet=new Set(slice.map(r=>r.id));
  tb.innerHTML=slice.map(r=>{
    const sideCls=r.side==='BUY'?'s-buy':'s-sell';
    const priceVal=parseFloat(r.price||0);
    const priceStr=priceVal>0?priceVal.toLocaleString('ko-KR'):'—';
    return '<tr>' +
      '<td class="c-id">#'+esc(r.id)+'</td>' +
      '<td class="c-sym">'+esc(r.symbol||'—')+'</td>' +
      '<td><span class="'+sideCls+'">'+esc(r.side||'—')+'</span></td>' +
      '<td class="c-qty">'+esc(r.quantity||'—')+'</td>' +
      '<td class="c-pr" style="text-align:right">'+priceStr+'</td>' +
      '<td class="c-msg" title="'+esc(r.reason||'')+'">'+esc(r.reason||'—')+'</td>' +
      '<td class="c-tm" style="text-align:right">'+esc((r.createdAt||'').substring(0,19))+'</td>' +
    '</tr>';
  }).join('');
  prevIds=newSet;
}

function renderPgBtns(pages){
  const pb=document.getElementById('pgBtns');pb.innerHTML='';
  const mk=(label,p2,disabled,active)=>{
    const b=document.createElement('button');
    b.className='pb'+(active?' on':'');b.textContent=label;b.disabled=disabled;
    b.onclick=()=>{pg=p2;renderTable();};pb.appendChild(b);
  };
  mk('‹',pg-1,pg===1,false);
  for(let i=1;i<=pages;i++){
    if(pages>7&&i>2&&i<pages-1&&Math.abs(i-pg)>1){
      if(i===3||i===pages-2){const s=document.createElement('button');s.className='pb';s.textContent='…';s.disabled=true;pb.appendChild(s);}
      continue;
    }
    mk(i,i,false,i===pg);
  }
  mk('›',pg+1,pg===pages,false);
}

window.setSide=function(side,btn){
  sideF=side;
  document.querySelectorAll('.fb').forEach(b=>b.classList.remove('on'));
  btn.classList.add('on');pg=1;renderTable();
};
window.applyF=function(){pg=1;renderTable();};
window.srtBy=function(key){
  if(srtKey===key)srtDir=srtDir==='asc'?'desc':'asc';else{srtKey=key;srtDir='desc';}
  document.querySelectorAll('thead th').forEach(th=>th.classList.remove('asc','desc'));
  const idx={id:0,symbol:1,side:2,price:4,time:6}[key];
  if(idx!=null){const th=document.querySelectorAll('thead th')[idx];if(th)th.classList.add(srtDir==='asc'?'asc':'desc');}
  renderTable();
};
document.getElementById('srtSel').addEventListener('change',function(){
  const parts=this.value.split('-');srtKey=parts[0];srtDir=parts[1]||'desc';renderTable();
});

window.exportCSV=function(){
  const filtered=getFiltered();
  if(!filtered.length){alert('내보낼 데이터가 없습니다.');return;}
  const hdr=['ID','Symbol','Side','Qty','Price','Reason','CreatedAt'];
  const rows=filtered.map(r=>[r.id,r.symbol,r.side,r.quantity,r.price,'"'+(r.reason||'').replace(/"/g,'""')+'"',r.createdAt]);
  const csv=[hdr,...rows].map(r=>r.join(',')).join('\n');
  const blob=new Blob(['\uFEFF'+csv],{type:'text/csv;charset=utf-8;'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a');a.href=url;a.download='orders_'+new Date().toISOString().substring(0,10)+'.csv';a.click();
  URL.revokeObjectURL(url);
};

/* ── 토스트 ── */
function showToast(id,type,msg){
  const el=document.getElementById(id);
  el.textContent=msg;el.className='toast '+type+' show';
  clearTimeout(el._t);el._t=setTimeout(()=>{el.className='toast';},5000);
}

/* ── HTML escape ── */
function esc(s){return String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}

/* ── 초기화 ── */
fetchOrders();
loadTop20();
checkStatus();
addLog('sys','','페이지 로드 완료','Control Center 초기화');
setInterval(()=>{loadTop20();checkStatus();fetchOrders();},30000);
})();
</script>
</body>
</html>
