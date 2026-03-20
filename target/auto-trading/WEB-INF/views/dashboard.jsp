<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AUTO TRADING — DASHBOARD</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#060709;--base:#0a0c10;--panel:#141720;--panel-hi:#191d28;--hover:#1e2330;
  --lime:#a8ff3e;--lime-d:rgba(168,255,62,.1);--lime-b:rgba(168,255,62,.22);--lime-glow:0 0 16px rgba(168,255,62,.4);
  --emerald:#00d97e;--emerald-d:rgba(0,217,126,.08);--emerald-b:rgba(0,217,126,.25);--emerald-glow:0 0 10px rgba(0,217,126,.4);
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);--red-glow:0 0 10px rgba(255,77,106,.4);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:12px;
  --topbar-h:52px;--sidebar-w:272px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html,body{height:100%;overflow:hidden;}
body{font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:radial-gradient(ellipse 80% 50% at 50% -10%,rgba(168,255,62,.06) 0%,transparent 55%),
    radial-gradient(ellipse 40% 50% at 100% 100%,rgba(0,217,126,.03) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.04) 1px,transparent 1px);background-size:28px 28px;}

@keyframes sd{from{opacity:0;transform:translateY(-8px);}to{opacity:1;transform:none;}}
@keyframes fu{from{opacity:0;transform:translateY(10px);}to{opacity:1;transform:none;}}
@keyframes sir{from{opacity:0;transform:translateX(14px);}to{opacity:1;transform:none;}}
@keyframes pd{0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.65);opacity:.25;}}
@keyframes sp{from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes ri{from{opacity:0;transform:translateX(3px);}to{opacity:1;transform:none;}}
@keyframes fl{0%{background:rgba(168,255,62,.08);}100%{background:transparent;}}

/* ── TOPBAR ── */
.topbar{position:fixed;top:0;left:0;right:0;z-index:300;height:var(--topbar-h);
  display:flex;align-items:center;background:rgba(6,7,9,.95);backdrop-filter:blur(20px);
  border-bottom:1px solid var(--rim);animation:sd .35s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent 0%,var(--lime) 40%,rgba(168,255,62,.25) 70%,transparent 100%);opacity:.35;}
.tb-logo{display:flex;align-items:center;gap:9px;padding:0 16px;height:100%;border-right:1px solid var(--rim);min-width:180px;}
.logo-mk{width:28px;height:28px;background:var(--lime);border-radius:6px;display:flex;align-items:center;justify-content:center;box-shadow:var(--lime-glow);flex-shrink:0;}
.logo-mk svg{width:14px;height:14px;}
.logo-name{font-size:12px;font-weight:700;letter-spacing:.5px;color:var(--t1);}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-sp{flex:1;}
.tb-pill{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:9px;color:var(--emerald);
  padding:3px 9px;border-radius:20px;background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);box-shadow:var(--emerald-glow);animation:pd 1.4s ease-in-out infinite;}
.tb-nav{display:flex;align-items:center;gap:2px;padding:0 10px;}
.tb-a{font-family:var(--mono);font-size:9px;letter-spacing:.4px;padding:4px 9px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-a:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-a.cur{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tb-login{display:flex;align-items:center;gap:5px;padding:0 10px;border-left:1px solid var(--rim);}
.tb-login form{display:flex;align-items:center;gap:4px;}
.tb-login input{height:24px;width:100px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:9px;padding:0 7px;}
.tb-login input::placeholder{color:var(--t3);}
.tb-lbtn{height:24px;padding:0 9px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:9px;cursor:pointer;transition:all .15s;}
.tb-lbtn:hover{background:var(--lime);color:var(--void);}
.tb-login-st{display:none;align-items:center;gap:6px;font-family:var(--mono);font-size:9px;color:var(--t2);}
.tb-login-st .acc{color:var(--lime);}
.tb-lerr{font-family:var(--mono);font-size:9px;color:var(--red);display:none;margin-left:3px;}
.tb-clock{padding:0 12px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:1px;}
.clk-t{font-family:var(--mono);font-size:13px;font-weight:500;color:var(--t1);letter-spacing:2px;}
.clk-d{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1px;}

/* ── SHELL ── */
.shell{display:flex;height:100vh;padding-top:var(--topbar-h);}
.main{flex:1;min-width:0;display:flex;flex-direction:column;overflow:hidden;}

/* ── KPI ROW ── */
.kpi-row{display:grid;grid-template-columns:repeat(5,1fr);gap:6px;flex-shrink:0;
  padding:8px 10px 0;animation:fu .4s .05s ease both;}
.kpi{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:8px 12px;position:relative;overflow:hidden;cursor:default;}
.kpi::before{content:'';position:absolute;top:0;left:0;right:0;height:1.5px;}
.kl::before{background:var(--lime);box-shadow:var(--lime-glow);}
.ke::before{background:var(--emerald);}
.kr::before{background:var(--red);}
.kg::before{background:var(--gold);}
.kb::before{background:var(--blue);}
.k-lbl{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:4px;}
.k-val{font-family:var(--mono);font-size:18px;font-weight:500;line-height:1;letter-spacing:-0.5px;}
.kl .k-val{color:var(--lime);}
.ke .k-val{color:var(--emerald);}
.kr .k-val{color:var(--red);}
.kg .k-val{color:var(--gold);}
.kb .k-val{color:var(--blue);}
.k-sub{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:2px;}

/* ── CONTENT AREA ── */
.content-area{flex:1;min-height:0;display:grid;grid-template-columns:1fr var(--sidebar-w);gap:0;animation:fu .4s .1s ease both;}

/* 차트 영역 */
.chart-area{display:flex;flex-direction:column;min-height:0;border-right:1px solid var(--rim);padding:8px 0 8px 10px;}

/* 공통 패널 */
.pn{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  display:flex;flex-direction:column;overflow:hidden;min-height:0;}
.ph{flex-shrink:0;height:32px;display:flex;align-items:center;justify-content:space-between;
  padding:0 10px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.ph-l{display:flex;align-items:center;gap:6px;}
.ph-dot{width:5px;height:5px;border-radius:50%;flex-shrink:0;}
.ph-nm{font-family:var(--mono);font-size:8px;font-weight:500;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.ph-bd{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:7px;border:1px solid var(--rim);color:var(--t2);background:var(--base);}
.ph-bd.ok{color:var(--emerald);border-color:var(--emerald-b);background:var(--emerald-d);}
.ph-bd.up{color:var(--red);border-color:var(--red-b);background:var(--red-d);}
.ph-bd.dn{color:#60a5fa;border-color:rgba(96,165,250,.3);background:rgba(96,165,250,.08);}

/* ── CHART PANEL ── */
.chart-pn{flex:1;min-height:0;display:flex;flex-direction:column;}

/* 툴바 */
.c-toolbar{flex-shrink:0;display:flex;align-items:center;gap:4px;flex-wrap:wrap;
  padding:5px 8px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.mkt-grp{display:flex;gap:2px;}
.mkt-b{font-family:var(--mono);font-size:7px;letter-spacing:1px;padding:3px 8px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-b.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-b.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.exch-s{height:22px;background:var(--panel);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t2);font-family:var(--mono);font-size:8px;padding:0 5px;outline:none;cursor:pointer;display:none;}
.exch-s.show{display:block;}
option{background:var(--panel-hi);}
.sym-in{height:22px;width:78px;background:var(--base);border:1px solid var(--rim-hi);border-radius:var(--r);
  color:var(--t1);font-family:var(--mono);font-size:10px;letter-spacing:1px;padding:0 6px;outline:none;}
.sym-in:focus{border-color:var(--lime-b);}
.go-b{height:22px;padding:0 9px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:7px;letter-spacing:1px;cursor:pointer;transition:all .12s;}
.go-b:hover{background:var(--lime);color:var(--void);}
.tf-grp{display:flex;gap:2px;margin-left:auto;}
.tf-b{font-family:var(--mono);font-size:7px;letter-spacing:.3px;padding:3px 5px;border-radius:var(--r);
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.tf-b.on{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.tf-b:hover:not(.on){color:var(--t1);}

/* 메타 */
.c-meta{flex-shrink:0;padding:6px 10px 4px;display:flex;align-items:baseline;gap:7px;border-bottom:1px solid var(--rim);}
.cm-nm{font-size:12px;font-weight:700;color:var(--t1);}
.cm-sym{font-family:var(--mono);font-size:9px;color:var(--t3);}
.cm-pr{font-family:var(--mono);font-size:16px;font-weight:600;color:var(--t1);letter-spacing:-0.5px;}
.cm-dl{font-family:var(--mono);font-size:10px;}
.cm-dl.up{color:var(--red);}.cm-dl.dn{color:#60a5fa;}

/* 범례 */
.c-legend{flex-shrink:0;display:flex;align-items:center;gap:9px;padding:3px 8px;
  border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.lg-item{display:flex;align-items:center;gap:3px;font-family:var(--mono);font-size:7px;color:var(--t3);}
.lg-box{width:8px;height:8px;border-radius:2px;}
.zm-grp{margin-left:auto;display:flex;gap:2px;}
.zm-b{font-family:var(--mono);font-size:7px;padding:2px 6px;border-radius:3px;
  border:1px solid var(--rim-hi);background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.zm-b:hover{color:var(--t1);border-color:var(--rim-hi);}

/* 캔버스 래퍼 */
.c-wrap{flex:1;min-height:0;display:flex;flex-direction:column;background:var(--base);position:relative;cursor:crosshair;}
.c-wrap.drag{cursor:grabbing;}
.c-main{flex:1;min-height:0;position:relative;}
.c-vol {flex-shrink:0;height:50px;position:relative;border-top:1px solid rgba(255,255,255,.04);}
.c-rsi {flex-shrink:0;height:58px;position:relative;border-top:1px solid rgba(255,255,255,.04);}
canvas{display:block;width:100%;height:100%;}

/* 오버레이 */
.c-ov{position:absolute;inset:0;display:flex;align-items:center;justify-content:center;
  font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:2px;
  background:rgba(10,12,16,.75);pointer-events:none;z-index:5;}
.c-ov.hid{display:none;}
.c-tip{position:absolute;pointer-events:none;background:rgba(20,23,32,.97);border:1px solid var(--rim-hi);
  border-radius:6px;padding:7px 10px;font-family:var(--mono);font-size:9px;color:var(--t1);white-space:nowrap;z-index:10;display:none;}
.c-vl,.c-hl{position:absolute;pointer-events:none;display:none;}
.c-vl{width:1px;top:0;bottom:0;background:rgba(168,255,62,.18);}
.c-hl{height:1px;left:0;right:0;background:rgba(168,255,62,.12);}

/* 패널 레이블 */
.sub-lbl{position:absolute;top:3px;left:56px;font-family:var(--mono);font-size:7px;
  color:var(--t3);letter-spacing:1px;pointer-events:none;z-index:2;}

/* ── RIGHT SIDEBAR ── */
.sidebar{width:var(--sidebar-w);height:100%;background:var(--base);
  display:flex;flex-direction:column;overflow:hidden;animation:sir .5s .15s ease both;}
.sb-hd{flex-shrink:0;background:var(--panel-hi);border-bottom:1px solid var(--rim);}
.sb-r1{display:flex;align-items:center;justify-content:space-between;padding:8px 10px 3px;}
.sb-title{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:8px;font-weight:600;
  color:var(--lime);letter-spacing:2px;text-transform:uppercase;}
.sb-tdot{width:5px;height:5px;border-radius:50%;background:var(--lime);box-shadow:var(--lime-glow);animation:pd 2s ease-in-out infinite;}
.sb-btns{display:flex;gap:2px;}
.sb-btn{width:22px;height:22px;border-radius:var(--r);border:1px solid var(--rim-hi);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .12s;display:flex;align-items:center;justify-content:center;padding:0;}
.sb-btn:hover,.sb-btn.act{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.sb-btn.spin svg{animation:sp .5s linear infinite;}
.sb-mkt{display:flex;gap:3px;padding:3px 10px 7px;}
.mkt-sb{flex:1;height:20px;font-family:var(--mono);font-size:7px;letter-spacing:1px;text-transform:uppercase;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.mkt-sb.on.kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.mkt-sb.on.us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-r2{display:flex;align-items:center;justify-content:space-between;padding:0 10px 6px;}
.sb-upd{font-family:var(--mono);font-size:7px;color:var(--t3);}
.sb-cd{display:flex;align-items:center;gap:3px;}
.sb-cdl{font-family:var(--mono);font-size:7px;color:var(--t3);}
.sb-cdn{font-family:var(--mono);font-size:8px;font-weight:600;color:var(--lime);min-width:16px;text-align:right;}
.sb-pg{height:2px;background:var(--t4);flex-shrink:0;}
.sb-pgf{height:100%;width:0%;background:linear-gradient(90deg,var(--lime),var(--emerald));
  box-shadow:0 0 4px var(--lime);transition:width 1s linear;}
.sb-tabs{flex-shrink:0;display:flex;border-bottom:1px solid var(--rim);background:var(--panel);}
.sb-tab{flex:1;padding:5px 0;text-align:center;font-family:var(--mono);font-size:7px;letter-spacing:1.5px;
  text-transform:uppercase;color:var(--t3);cursor:pointer;user-select:none;border-bottom:2px solid transparent;transition:all .12s;}
.sb-tab:hover{color:var(--t2);}
.sb-tab.on{color:var(--lime);border-bottom-color:var(--lime);}
.sb-tabs.us .sb-tab.on{color:var(--blue);border-bottom-color:var(--blue);}
.sb-exch{flex-shrink:0;display:none;gap:3px;padding:4px 8px;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.sb-exch.show{display:flex;}
.ex-b{flex:1;height:18px;font-family:var(--mono);font-size:7px;letter-spacing:.5px;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.ex-b.on{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}
.sb-body{flex:1;overflow-y:auto;scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.sb-body::-webkit-scrollbar{width:2px;}
.sb-body::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
.sb-msg{padding:30px 14px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:7px;}
.sb-mi{font-size:18px;}.sb-mt{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1.5px;}
.sb-msg.ld .sb-mi{color:var(--lime);animation:pd 1s ease-in-out infinite;}
.sb-msg.er .sb-mi,.sb-msg.er .sb-mt{color:var(--red);}
.rk-list{padding:2px 0;}
.rk-row{display:grid;grid-template-columns:20px 1fr auto;align-items:center;gap:5px;
  padding:5px 8px;border-bottom:1px solid var(--t4);cursor:pointer;transition:background .1s;animation:ri .2s ease both;}
.rk-row:last-child{border-bottom:none;}
.rk-row:hover{background:var(--hover);}
.rk-row.fl{animation:fl .5s ease both;}
.rn{font-family:var(--mono);font-size:9px;font-weight:700;text-align:center;flex-shrink:0;}
.rn.r1{color:var(--gold);font-size:11px;}.rn.r2{color:#b0b8c8;}.rn.r3{color:#cd8b5a;}.rn.rN{color:var(--t3);font-size:8px;}
.ri-i{min-width:0;}
.ri-nm{font-size:10px;font-weight:600;color:var(--t1);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;margin-bottom:1px;}
.ri-cd{font-family:var(--mono);font-size:7px;color:var(--t3);}
.ri-bw{margin-top:2px;}
.ri-b{height:2px;border-radius:1px;transition:width .5s ease;}
.ri-b.kr{background:linear-gradient(90deg,var(--lime),rgba(168,255,62,.05));}
.ri-b.us{background:linear-gradient(90deg,var(--blue),rgba(77,159,255,.05));}
.ri-vl{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:1px;}
.rp-i{text-align:right;flex-shrink:0;}
.rp-pr{font-family:var(--mono);font-size:9px;font-weight:500;color:var(--t1);}
.rp-ch{font-family:var(--mono);font-size:8px;margin-top:1px;display:flex;align-items:center;justify-content:flex-end;}
.rp-ch.ku{color:var(--red);}.rp-ch.kd{color:#60a5fa;}
.rp-ch.uu{color:var(--emerald);}.rp-ch.ud{color:var(--red);}.rp-ch.fl{color:var(--t3);}
.sb-ft{flex-shrink:0;padding:4px 8px;border-top:1px solid var(--rim);background:var(--panel-hi);
  font-family:var(--mono);font-size:6px;color:var(--t3);text-align:center;letter-spacing:.5px;}

/* ── 하단 데이터 바 ── */
.data-bar{flex-shrink:0;height:120px;border-top:1px solid var(--rim);display:flex;gap:0;padding:0 10px 8px;}
.db-sec{flex:1;min-width:0;display:flex;flex-direction:column;overflow:hidden;}
.db-sec+.db-sec{border-left:1px solid var(--rim);margin-left:8px;padding-left:8px;}
.db-hd{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;
  padding:5px 0 4px;flex-shrink:0;border-bottom:1px solid var(--rim);margin-bottom:2px;}
.db-sc{flex:1;overflow-y:auto;scrollbar-width:none;}
.db-sc::-webkit-scrollbar{display:none;}
.db-row{display:grid;padding:2px 0;font-size:10px;border-bottom:1px solid var(--t4);}
.db-row:last-child{border-bottom:none;}
.ord-row{grid-template-columns:28px 58px 38px 44px 70px 1fr;}
.prc-row{grid-template-columns:60px 80px 1fr;}
.db-row span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;}
.db-th{display:grid;font-family:var(--mono);font-size:7px;color:var(--t2);letter-spacing:.5px;margin-bottom:1px;}
.ord-th{grid-template-columns:28px 58px 38px 44px 70px 1fr;}
.prc-th{grid-template-columns:60px 80px 1fr;}
.c-id{font-family:var(--mono);font-size:9px;color:var(--t3);}
.c-sym{font-family:var(--mono);font-size:10px;font-weight:600;color:var(--t1);}
.s-buy{font-family:var(--mono);font-size:8px;font-weight:600;padding:0 4px;border-radius:2px;
  color:var(--emerald);background:var(--emerald-d);border:1px solid var(--emerald-b);}
.s-sell{font-family:var(--mono);font-size:8px;font-weight:600;padding:0 4px;border-radius:2px;
  color:var(--red);background:var(--red-d);border:1px solid var(--red-b);}
.c-pr{font-family:var(--mono);font-size:10px;color:var(--gold);font-weight:500;}
.c-lpr{font-family:var(--mono);font-size:10px;color:var(--lime);font-weight:500;}
.c-mu{font-family:var(--mono);font-size:9px;color:var(--t3);}
.db-empty{font-family:var(--mono);font-size:9px;color:var(--t3);text-align:center;padding:12px 0;letter-spacing:1px;}

/* kpi stats */
.sc-row{display:flex;gap:6px;padding:5px 8px;}
.sc{flex:1;background:var(--panel-hi);border:1px solid var(--rim);border-radius:var(--r2);padding:6px 9px;}
.sc-l{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;text-transform:uppercase;margin-bottom:3px;}
.sc-v{font-family:var(--mono);font-size:14px;color:var(--t1);font-weight:500;line-height:1;}
.sc-s{font-family:var(--mono);font-size:7px;color:var(--t3);margin-top:2px;}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>

<!-- TOPBAR -->
<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mk">
      <svg viewBox="0 0 24 24" fill="none" stroke="#060709" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-name">AUTO<span>TRADE</span></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-sp"></div>
  <div class="tb-pill"><div class="tb-dot"></div><span id="hdSt">LOADING</span></div>
  <div class="tb-nav">
    <a class="tb-a cur"  href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/monitor">Monitor</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-a"     href="${pageContext.request.contextPath}/">Home</a>
  </div>
  <div class="tb-login">
    <form id="lf">
      <input type="text"     name="accountNo"       placeholder="12345678-01" autocomplete="off"/>
      <input type="password" name="accountPassword"  placeholder="Password"   autocomplete="off"/>
      <button class="tb-lbtn" type="submit">Login</button>
    </form>
    <div class="tb-login-st" id="lst">
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

<div class="shell">
  <!-- ── MAIN ── -->
  <div class="main">

    <!-- KPI -->
    <div class="kpi-row">
      <div class="kpi kl"><div class="k-lbl">Watchlist</div><div class="k-val" id="kW">—</div><div class="k-sub">감시 종목</div></div>
      <div class="kpi ke"><div class="k-lbl">Positions</div><div class="k-val" id="kP">—</div><div class="k-sub">포지션</div></div>
      <div class="kpi kg"><div class="k-lbl">Last Symbol</div><div class="k-val" id="kS" style="font-size:13px;padding-top:2px">—</div><div class="k-sub" id="kSS">—</div></div>
      <div class="kpi kr"><div class="k-lbl">Status</div><div class="k-val" id="kSt" style="font-size:13px;padding-top:2px">—</div><div class="k-sub">엔진 상태</div></div>
      <div class="kpi kb"><div class="k-lbl">Orders</div><div class="k-val" id="kO">—</div><div class="k-sub">최근 주문</div></div>
    </div>

    <!-- CONTENT: 차트 + 사이드바 -->
    <div class="content-area" style="flex:1;min-height:0;">

      <!-- 차트 영역 -->
      <div class="chart-area">
        <div class="chart-pn pn">
          <div class="ph">
            <div class="ph-l">
              <div class="ph-dot" style="background:var(--lime);box-shadow:var(--lime-glow)"></div>
              <div class="ph-nm">Price Chart</div>
            </div>
            <span class="ph-bd" id="cBadge">1d · KR</span>
          </div>
          <div class="c-toolbar">
            <div class="mkt-grp">
              <button class="mkt-b on kr" id="mKR" onclick="setMkt('KR')">🇰🇷 KR</button>
              <button class="mkt-b us"    id="mUS" onclick="setMkt('US')">🇺🇸 US</button>
            </div>
            <select class="exch-s" id="exchS" onchange="onExch()">
              <option value="NAS">NASDAQ</option>
              <option value="NYS">NYSE</option>
              <option value="AMS">AMEX</option>
            </select>
            <input class="sym-in" id="symIn" value="005930" autocomplete="off" spellcheck="false"/>
            <button class="go-b" onclick="fetchChart()">조회</button>
            <div class="tf-grp">
              <button class="tf-b"    data-tf="1m"  onclick="setTf('1m',this)">1m</button>
              <button class="tf-b"    data-tf="5m"  onclick="setTf('5m',this)">5m</button>
              <button class="tf-b"    data-tf="15m" onclick="setTf('15m',this)">15m</button>
              <button class="tf-b"    data-tf="30m" onclick="setTf('30m',this)">30m</button>
              <button class="tf-b"    data-tf="60m" onclick="setTf('60m',this)">60m</button>
              <button class="tf-b on" data-tf="1d"  onclick="setTf('1d',this)">1D</button>
              <button class="tf-b"    data-tf="1w"  onclick="setTf('1w',this)">1W</button>
            </div>
          </div>
          <div class="c-meta">
            <span class="cm-nm" id="cmN">—</span>
            <span class="cm-sym" id="cmS"></span>
            <span class="cm-pr" id="cmP">—</span>
            <span class="cm-dl" id="cmD">—</span>
          </div>
          <div class="c-legend">
            <div class="lg-item"><div class="lg-box" style="background:var(--emerald)"></div>상승</div>
            <div class="lg-item"><div class="lg-box" style="background:var(--red)"></div>하락</div>
            <div class="lg-item"><div class="lg-box" style="background:rgba(77,159,255,.35);border:1px solid var(--blue)"></div>BB</div>
            <div class="lg-item"><div class="lg-box" style="background:rgba(168,255,62,.4);border:1px solid var(--lime)"></div>MA20</div>
            <div class="lg-item" style="color:var(--gold)">RSI14</div>
            <div class="zm-grp">
              <button class="zm-b" onclick="zm(-1)">− Zoom</button>
              <button class="zm-b" onclick="zm(1)">+ Zoom</button>
              <button class="zm-b" onclick="zmR()">Reset</button>
            </div>
          </div>
          <!-- Canvas 영역 -->
          <div class="c-wrap" id="cWrap">
            <div class="c-main" id="mainWrap">
              <canvas id="cv"></canvas>
              <div class="c-vl" id="cvl"></div>
              <div class="c-hl" id="chl"></div>
              <div class="c-tip" id="ctp"></div>
              <div class="c-ov" id="cOv">로딩 중…</div>
            </div>
            <div class="c-vol" id="volWrap">
              <canvas id="cvVol"></canvas>
              <div class="sub-lbl">VOL</div>
            </div>
            <div class="c-rsi" id="rsiWrap">
              <canvas id="cvRsi"></canvas>
              <div class="sub-lbl">RSI·14</div>
            </div>
          </div>
        </div>

        <!-- 하단 데이터 바 -->
        <div class="data-bar">
          <div class="db-sec">
            <div class="db-hd">주문 이력</div>
            <div class="db-th ord-th"><span>ID</span><span>Symbol</span><span>Side</span><span>Qty</span><span>Price</span><span>Time</span></div>
            <div class="db-sc" id="ordList"></div>
          </div>
          <div class="db-sec">
            <div class="db-hd">가격 로그</div>
            <div class="db-th prc-th"><span>Symbol</span><span>Price</span><span>Time</span></div>
            <div class="db-sc" id="prcList"></div>
          </div>
          <div class="db-sec" style="max-width:240px;flex:0 0 240px;">
            <div class="db-hd">빠른 이동</div>
            <div style="display:flex;flex-direction:column;gap:4px;padding-top:4px;">
              <a href="${pageContext.request.contextPath}/control/kr"    style="flex:1;text-align:center;padding:5px 8px;background:var(--emerald-d);border:1px solid var(--emerald-b);color:var(--emerald);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--emerald)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--emerald-d)';this.style.color='var(--emerald)'">⚡ Control KR</a>
              <a href="${pageContext.request.contextPath}/control/us"    style="flex:1;text-align:center;padding:5px 8px;background:var(--lime-d);border:1px solid var(--lime-b);color:var(--lime);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--lime)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--lime-d)';this.style.color='var(--lime)'">⚡ Control US</a>
              <a href="${pageContext.request.contextPath}/history/orders" style="flex:1;text-align:center;padding:5px 8px;background:var(--gold-d);border:1px solid var(--gold-b);color:var(--gold);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--gold)';this.style.color='var(--void)'" onmouseout="this.style.background='var(--gold-d)';this.style.color='var(--gold)'">📋 Orders</a>
              <a href="${pageContext.request.contextPath}/balances"       style="flex:1;text-align:center;padding:5px 8px;background:var(--panel-hi);border:1px solid var(--rim);color:var(--t2);border-radius:var(--r);text-decoration:none;font-family:var(--mono);font-size:8px;letter-spacing:.5px;transition:all .15s;" onmouseover="this.style.background='var(--hover)';this.style.color='var(--t1)'" onmouseout="this.style.background='var(--panel-hi)';this.style.color='var(--t2)'">💰 Balances</a>
            </div>
          </div>
        </div>
      </div><!-- /chart-area -->

      <!-- RIGHT SIDEBAR -->
      <aside class="sidebar">
        <div class="sb-hd">
          <div class="sb-r1">
            <div class="sb-title"><span class="sb-tdot"></span>거래량 TOP30</div>
            <div class="sb-btns">
              <button class="sb-btn act" id="sbAb" onclick="toggleAuto()" title="자동갱신">
                <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round">
                  <circle cx="12" cy="12" r="9"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="12" x2="15" y2="15"/>
                </svg>
              </button>
              <button class="sb-btn" id="sbRb" onclick="loadRanking()" title="새로고침">
                <svg width="9" height="9" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="sb-mkt">
            <button class="mkt-sb on kr" id="smKR" onclick="setSbMkt('KR')">🇰🇷 국내</button>
            <button class="mkt-sb us"    id="smUS" onclick="setSbMkt('US')">🇺🇸 미국</button>
          </div>
          <div class="sb-r2">
            <span class="sb-upd" id="sbUpd">로딩 중…</span>
            <div class="sb-cd"><span class="sb-cdl">갱신</span><span class="sb-cdn" id="sbCdn">30</span><span class="sb-cdl">s</span></div>
          </div>
        </div>
        <div class="sb-pg"><div class="sb-pgf" id="sbPgf"></div></div>
        <div class="sb-tabs" id="sbTabs">
          <div class="sb-tab on" id="stVol" onclick="setSbTab('vol')">거래량</div>
          <div class="sb-tab"    id="stChg" onclick="setSbTab('chg')">등락률</div>
          <div class="sb-tab"    id="stHi"  onclick="setSbTab('hi')">고가순</div>
        </div>
        <div class="sb-exch" id="sbExch">
          <button class="ex-b on" id="exNAS" onclick="setSbExch('NAS')">NASDAQ</button>
          <button class="ex-b"    id="exNYS" onclick="setSbExch('NYS')">NYSE</button>
          <button class="ex-b"    id="exAMS" onclick="setSbExch('AMS')">AMEX</button>
        </div>
        <div class="sb-body" id="sbBody">
          <div class="sb-msg ld"><div class="sb-mi">◈</div><div class="sb-mt">로딩 중…</div></div>
        </div>
        <div class="sb-ft" id="sbFt">KIS · /api/market/ranking</div>
      </aside>

    </div><!-- /content-area -->
  </div><!-- /main -->
</div><!-- /shell -->

<script>
/* ════════════════════════════════════════════
   [수정 핵심] B를 IIFE 밖 전역으로 선언
   기존: IIFE 안에서만 const B = '...' → fetchChart 등 외부 함수에서 참조 불가 → TypeError
   수정: 전역 window.B 로 선언
════════════════════════════════════════════ */
const B = '${pageContext.request.contextPath}';
const DAYS = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
const POLL = 30;

/* ── 유틸 ── */
function esc(s){ return String(s??'').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;'); }
function p2(v){ return String(v).padStart(2,'0'); }

/* ── 시계 ── */
function tick(){
  const n=new Date();
  document.getElementById('clkT').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
  document.getElementById('clkD').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
}
setInterval(tick,1000); tick();

/* ── Login ── */
(function(){
  const f=document.getElementById('lf'),sb=document.getElementById('lst'),as=document.getElementById('lacc');
  const lb=document.getElementById('lob'),eb=document.getElementById('lerr');
  const sI=m=>{f.style.display='none';sb.style.display='inline-flex';eb.style.display='none';as.textContent=m||'****';};
  const sO=()=>{sb.style.display='none';f.style.display='';eb.style.display='none';};
  const sE=m=>{eb.textContent=m||'';eb.style.display=m?'inline-flex':'none';};
  const post=(u,d)=>fetch(u,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(d).toString()}).then(r=>r.json());
  fetch(B+'/api/auth/status').then(r=>r.json()).then(d=>d&&d.loggedIn?sI(d.accountMasked):sO()).catch(()=>sO());
  f.addEventListener('submit',e=>{
    e.preventDefault();
    const no=(f.accountNo.value||'').trim(),pw=(f.accountPassword.value||'').trim();
    if(!no||!pw)return;
    post(B+'/api/auth/login',{accountNo:no,accountPassword:pw})
      .then(d=>d.status==='OK'?sI(d.accountMasked):sE(d.message||'Login failed'));
  });
  lb.addEventListener('click',()=>post(B+'/api/auth/logout',{}).then(()=>sO()));
})();

/* ── 대시보드 ── */
function loadDash(){
  fetch(B+'/api/dashboard?limit=10').then(r=>r.json()).then(d=>{
    if(!d) return;
    const st=d.status||'STOPPED';
    document.getElementById('hdSt').textContent=st;
    document.getElementById('kW').textContent=d.watchlistCount||0;
    document.getElementById('kP').textContent=d.positionCount||0;
    document.getElementById('kSt').textContent=st;
    const ords=d.recentOrders||[];
    document.getElementById('kO').textContent=ords.length;
    if(ords.length){
      const l=ords[0];
      document.getElementById('kS').textContent=l.symbol||'—';
      document.getElementById('kSS').textContent=l.side||'—';
    }
    const ol=document.getElementById('ordList');
    ol.innerHTML=ords.length?ords.map(r=>
      '<div class="db-row ord-row">'+
        '<span class="c-id">#'+esc(r.id)+'</span>'+
        '<span class="c-sym">'+esc(r.symbol)+'</span>'+
        '<span><span class="'+(r.side==='BUY'?'s-buy':'s-sell')+'">'+esc(r.side)+'</span></span>'+
        '<span>'+esc(r.quantity)+'</span>'+
        '<span class="c-pr">'+Number(r.price||0).toLocaleString('ko-KR')+'</span>'+
        '<span class="c-mu">'+esc((r.createdAt||'').substring(11,16))+'</span>'+
      '</div>'
    ).join(''):'<div class="db-empty">주문 없음</div>';
    const prcs=d.recentPrices||[];
    const pl=document.getElementById('prcList');
    pl.innerHTML=prcs.length?prcs.map(r=>
      '<div class="db-row prc-row">'+
        '<span class="c-sym">'+esc(r.symbol)+'</span>'+
        '<span class="c-lpr">'+esc(r.price)+'</span>'+
        '<span class="c-mu">'+esc((r.createdAt||'').substring(11,19))+'</span>'+
      '</div>'
    ).join(''):'<div class="db-empty">데이터 없음</div>';
  }).catch(()=>{});
}
loadDash(); setInterval(loadDash,15000);

/* ════════════════════════════════════════════
   차트 상태 — 전역
════════════════════════════════════════════ */
const CS = { mkt:'KR', exch:'NAS', sym:'005930', tf:'1d', raw:[], zS:0, zE:1, drag:false, dragX:0, dragZ:null };

function isMinuteTf(tf){
  return /m$/.test(tf) && tf!=='1mon' && tf!=='1mo' && tf!=='1mth';
}

function resetViewRange(){
  const n = CS.raw.length;
  if(!n){ CS.zS=0; CS.zE=1; return; }
  let bars = n;
  if(isMinuteTf(CS.tf)) bars = Math.min(180, n);
  else if(CS.tf==='1d') bars = Math.min(240, n);
  else if(CS.tf==='1w') bars = Math.min(260, n);
  if(n>bars){
    CS.zE = 1;
    CS.zS = Math.max(0, 1 - bars/n);
  }else{
    CS.zS = 0;
    CS.zE = 1;
  }
}

window.setMkt = function(m){
  CS.mkt=m;
  ['KR','US'].forEach(x=>document.getElementById('m'+x).className='mkt-b'+(x===m?(m==='KR'?' on kr':' on us'):''));
  document.getElementById('exchS').classList.toggle('show',m==='US');
  if(m==='US'&&!/^[A-Za-z]/.test(CS.sym)){CS.sym='AAPL';document.getElementById('symIn').value='AAPL';}
  if(m==='KR'&&!/^[0-9]/.test(CS.sym)){CS.sym='005930';document.getElementById('symIn').value='005930';}
  fetchChart();
};
window.onExch  = function(){ CS.exch=document.getElementById('exchS').value; fetchChart(); };
window.setTf   = function(tf,btn){ CS.tf=tf; document.querySelectorAll('.tf-b').forEach(b=>b.classList.toggle('on',b.dataset.tf===tf)); fetchChart(); };

/* ── 데이터 조회 ── */
window.fetchChart = function(){
  const symEl = document.getElementById('symIn');
  if(symEl){ const v=(symEl.value||'').trim(); if(v) CS.sym=v; }

  document.getElementById('cBadge').textContent='로딩 중…';
  document.getElementById('cOv').textContent='로딩 중…';
  document.getElementById('cOv').className='c-ov';

  fetch(B+'/api/market/chart?market='+encodeURIComponent(CS.mkt)
        +'&symbol='+encodeURIComponent(CS.sym)
        +'&tf='+encodeURIComponent(CS.tf)
        +'&exch='+encodeURIComponent(CS.exch))
    .then(r=>r.json())
    .then(d=>{
      if(!d||d.status!=='OK'){ document.getElementById('cOv').textContent='데이터 없음'; return; }
      const pts=(d.points||[]).sort((a,b)=>(a.ts||0)-(b.ts||0));
      /* OHLC 정규화: API가 open/high/low/close 주면 그대로, 없으면 price 단일값 fallback */
      CS.raw=pts.map(p=>({
        time :p.time||'',  ts:p.ts||0,
        open :p.open !=null?Number(p.open) :Number(p.price||0),
        high :p.high !=null?Number(p.high) :Number(p.price||0),
        low  :p.low  !=null?Number(p.low)  :Number(p.price||0),
        close:p.close!=null?Number(p.close):Number(p.price||0),
        volume:Number(p.volume||p.vol||0)
      }));

      // 장중 분봉에 날짜가 섞여 들어오면(예: 전일 + 금일 1틱) dominant session만 사용
      if(CS.tf.endsWith('m') && CS.raw.length){
        const byDate={};
        CS.raw.forEach(p=>{
          const d=(p.time||'').slice(0,10);
          if(d) byDate[d]=(byDate[d]||0)+1;
        });
        const ds=Object.keys(byDate);
        if(ds.length>1){
          const keep=ds.sort((a,b)=>byDate[b]-byDate[a])[0];
          CS.raw=CS.raw.filter(p=>(p.time||'').startsWith(keep));
        }
      }

      resetViewRange();
      document.getElementById('cmN').textContent=d.name||CS.sym;
      document.getElementById('cmS').textContent='· '+CS.sym;
      if(CS.raw.length){
        const last=CS.raw[CS.raw.length-1].close, first=CS.raw[0].open;
        const diff=last-first, rate=first?diff/first*100:0;
        document.getElementById('cmP').textContent=last.toLocaleString('ko-KR');
        const dl=document.getElementById('cmD');
        dl.textContent=(diff>=0?'▲+':'▼')+Math.abs(diff).toFixed(0)+' ('+(diff>=0?'+':'')+rate.toFixed(2)+'%)';
        dl.className='cm-dl '+(diff>=0?'up':'dn');
        const bdg=document.getElementById('cBadge');
        bdg.textContent=CS.tf+' · '+CS.mkt; bdg.className='ph-bd '+(diff>=0?'up':'dn');
      }
      drawAll();
    })
    .catch(()=>{ document.getElementById('cOv').textContent='차트 로드 실패'; });
};

/* ── 지표 계산 ── */
function calcMA(closes,n){ return closes.map((_,i)=>i<n-1?null:closes.slice(i-n+1,i+1).reduce((s,v)=>s+v,0)/n); }
function calcBB(closes,n=20,mult=2){
  const mid=calcMA(closes,n);
  return mid.map((m,i)=>{
    if(m===null) return {mid:null,upper:null,lower:null};
    const sl=closes.slice(i-n+1,i+1), sd=Math.sqrt(sl.reduce((s,v)=>s+(v-m)**2,0)/n);
    return {mid:m,upper:m+mult*sd,lower:m-mult*sd};
  });
}
function calcRSI(closes,n=14){
  const r=new Array(closes.length).fill(null);
  if(closes.length<=n) return r;
  let ag=0,al=0;
  for(let i=1;i<=n;i++){ const d=closes[i]-closes[i-1]; d>0?ag+=d:al-=d; }
  ag/=n; al/=n;
  r[n]=al===0?100:100-100/(1+ag/al);
  for(let i=n+1;i<closes.length;i++){
    const d=closes[i]-closes[i-1],g=d>0?d:0,l=d<0?-d:0;
    ag=(ag*(n-1)+g)/n; al=(al*(n-1)+l)/n;
    r[i]=al===0?100:100-100/(1+ag/al);
  }
  return r;
}

/* ── 전체 그리기 ── */
function drawAll(){
  const raw=CS.raw; if(!raw||!raw.length) return;
  const tot=raw.length, s=Math.floor(CS.zS*tot), en=Math.max(s+2,Math.floor(CS.zE*tot));
  const vis=raw.slice(s,en);
  drawMain(vis); drawVol(vis); drawRsi(vis);
}

/* ── 캔버스 초기화 헬퍼 ── */
function initCanvas(wrap,canvas){
  const dpr=window.devicePixelRatio||1, W=wrap.clientWidth, H=wrap.clientHeight;
  if(!W||!H) return null;
  canvas.width=W*dpr; canvas.height=H*dpr;
  canvas.style.width=W+'px'; canvas.style.height=H+'px';
  const ctx=canvas.getContext('2d');
  ctx.setTransform(1,0,0,1,0,0); ctx.scale(dpr,dpr); ctx.clearRect(0,0,W,H);
  return {ctx,W,H};
}

/* ── 메인 캔들 + BB + MA20 ── */
function drawMain(vis){
  const wrap=document.getElementById('mainWrap'), canvas=document.getElementById('cv'), ov=document.getElementById('cOv');
  if(!vis||!vis.length){ ov.textContent='데이터 없음'; ov.className='c-ov'; return; }
  ov.className='c-ov hid';
  const r=initCanvas(wrap,canvas); if(!r) return;
  const {ctx,W,H}=r;
  const PL=54,PR=6,PT=10,PB=20, chartW=W-PL-PR, chartH=H-PT-PB, n=vis.length;
  const closes=vis.map(c=>c.close);
  const bb=calcBB(closes), ma=calcMA(closes,20);
  const allVals=vis.flatMap(c=>[c.high,c.low]).concat(bb.flatMap(b=>[b.upper,b.lower]).filter(v=>v!=null));
  let yMx=Math.max(...allVals), yMn=Math.min(...allVals);
  const sp=yMx-yMn||1; yMx+=sp*.06; yMn-=sp*.06;
  const ys=v=>PT+chartH*(1-(v-yMn)/(yMx-yMn));
  const cnt=Math.max(n-1,1);
  const step=Math.min(18,chartW/cnt);
  const usedW=step*cnt;
  const xBase=PL+Math.max(0,chartW-usedW);
  const cw=Math.max(1,Math.min(12,step*.62));
  ctx.font='8px JetBrains Mono,monospace';
  /* 그리드 */
  for(let i=0;i<=4;i++){
    const y=PT+chartH*(i/4);
    ctx.strokeStyle='rgba(255,255,255,.03)'; ctx.lineWidth=1;
    ctx.beginPath(); ctx.moveTo(PL,y); ctx.lineTo(W-PR,y); ctx.stroke();
    ctx.fillStyle='rgba(122,132,153,.6)'; ctx.textAlign='right';
    ctx.fillText((yMx-(yMx-yMn)*(i/4)).toLocaleString('ko-KR',{maximumFractionDigits:0}),PL-3,y+3);
  }
  const xStep=Math.max(1,Math.floor(n/7));
  for(let i=0;i<n;i+=xStep){
    const x=xBase+i*step;
    ctx.strokeStyle='rgba(255,255,255,.02)'; ctx.lineWidth=1;
    ctx.beginPath(); ctx.moveTo(x,PT); ctx.lineTo(x,H-PB); ctx.stroke();
    ctx.fillStyle='rgba(122,132,153,.55)'; ctx.textAlign='center';
    ctx.fillText((vis[i].time||'').replace(/\d{4}-\d{2}-\d{2} /,'').substring(0,5),x,H-PB+12);
  }
  /* BB fill */
  ctx.beginPath();
  let sv=true;
  for(let i=0;i<n;i++){ const b=bb[i]; if(b.upper===null) continue; sv?(ctx.moveTo(xBase+i*step,ys(b.upper)),sv=false):ctx.lineTo(xBase+i*step,ys(b.upper)); }
  for(let i=n-1;i>=0;i--){ const b=bb[i]; if(b.lower===null) continue; ctx.lineTo(xBase+i*step,ys(b.lower)); }
  ctx.closePath(); ctx.fillStyle='rgba(77,159,255,.07)'; ctx.fill();
  /* BB 선 */
  [{key:'upper',c:'rgba(77,159,255,.45)'},{key:'mid',c:'rgba(77,159,255,.25)'},{key:'lower',c:'rgba(77,159,255,.45)'}].forEach(({key,c})=>{
    ctx.beginPath(); ctx.strokeStyle=c; ctx.lineWidth=1; let mv=true;
    for(let i=0;i<n;i++){ const b=bb[i]; if(b[key]===null){mv=true;continue;} const x=xBase+i*step,y=ys(b[key]); mv?(ctx.moveTo(x,y),mv=false):ctx.lineTo(x,y); }
    ctx.stroke();
  });
  /* MA20 */
  ctx.beginPath(); ctx.strokeStyle='rgba(168,255,62,.55)'; ctx.lineWidth=1.2; let mm=true;
  for(let i=0;i<n;i++){ if(ma[i]===null){mm=true;continue;} const x=xBase+i*step,y=ys(ma[i]); mm?(ctx.moveTo(x,y),mm=false):ctx.lineTo(x,y); }
  ctx.stroke();
  /* 캔들 */
  vis.forEach((c,i)=>{
    const x=xBase+i*step, yO=ys(c.open), yC=ys(c.close), yH=ys(c.high), yL=ys(c.low);
    const up=c.close>=c.open, fill=up?'#00d97e':'#ff4d6a', stk=up?'#00b060':'#e02040';
    ctx.strokeStyle=stk; ctx.lineWidth=1;
    ctx.beginPath(); ctx.moveTo(x,yH); ctx.lineTo(x,yL); ctx.stroke();
    const top=Math.min(yO,yC), bh=Math.max(Math.abs(yC-yO),1.5);
    ctx.fillStyle=fill; ctx.fillRect(x-cw/2,top,cw,bh);
    ctx.strokeStyle=stk; ctx.lineWidth=.7; ctx.strokeRect(x-cw/2,top,cw,bh);
  });
  /* 마지막 가격선 */
  const lc=vis[vis.length-1].close, yl=ys(lc);
  ctx.setLineDash([3,3]); ctx.strokeStyle='rgba(168,255,62,.3)'; ctx.lineWidth=1;
  ctx.beginPath(); ctx.moveTo(xBase,yl); ctx.lineTo(xBase+usedW,yl); ctx.stroke(); ctx.setLineDash([]);
  ctx.fillStyle='rgba(168,255,62,.8)'; ctx.font='bold 8px JetBrains Mono,monospace'; ctx.textAlign='right';
  ctx.fillText(lc.toLocaleString('ko-KR',{maximumFractionDigits:0}),PL-2,yl+3);
  canvas._m={PL,PR,PT,PB,W,H,yMn,yMx,vis,step,n,ys,xBase,usedW};
}

/* ── 거래량 ── */
function drawVol(vis){
  const wrap=document.getElementById('volWrap'), canvas=document.getElementById('cvVol');
  if(!vis||!vis.length) return;
  const r=initCanvas(wrap,canvas); if(!r) return;
  const {ctx,W,H}=r;
  const PL=54,PR=6,PT=3,PB=10, chartW=W-PL-PR, chartH=H-PT-PB, n=vis.length;
  const maxV=Math.max(...vis.map(c=>c.volume),1);
  const cnt=Math.max(n-1,1);
  const step=Math.min(18,chartW/cnt);
  const usedW=step*cnt;
  const xBase=PL+Math.max(0,chartW-usedW);
  const cw=Math.max(1,Math.min(10,step*.56));
  ctx.font='7px JetBrains Mono,monospace'; ctx.fillStyle='rgba(122,132,153,.4)'; ctx.textAlign='right';
  const vL=maxV>=1e8?(maxV/1e8).toFixed(1)+'억':maxV>=1e6?(maxV/1e6).toFixed(1)+'M':maxV>=1e3?(maxV/1e3).toFixed(0)+'K':''+maxV;
  ctx.fillText(vL,PL-3,PT+9);
  vis.forEach((c,i)=>{
    const x=xBase+i*step, bh=Math.max((c.volume/maxV)*chartH,1), y=PT+chartH-bh;
    ctx.fillStyle=c.close>=c.open?'rgba(0,217,126,.5)':'rgba(255,77,106,.5)';
    ctx.fillRect(x-cw/2,y,cw,bh);
  });
}

/* ── RSI ── */
function drawRsi(vis){
  const wrap=document.getElementById('rsiWrap'), canvas=document.getElementById('cvRsi');
  if(!vis||!vis.length) return;
  const r=initCanvas(wrap,canvas); if(!r) return;
  const {ctx,W,H}=r;
  const PL=54,PR=6,PT=3,PB=10, chartW=W-PL-PR, chartH=H-PT-PB, n=vis.length;
  const closes=vis.map(c=>c.close), rsi=calcRSI(closes,14);
  const cnt=Math.max(n-1,1);
  const step=Math.min(18,chartW/cnt);
  const usedW=step*cnt;
  const xBase=PL+Math.max(0,chartW-usedW);
  /* 배경 */
  ctx.fillStyle='rgba(255,77,106,.05)';  ctx.fillRect(PL,PT,chartW,chartH*(1-70/100));
  ctx.fillStyle='rgba(77,159,255,.05)';  ctx.fillRect(PL,PT+chartH*(1-30/100),chartW,chartH*(30/100));
  /* 기준선 */
  [[70,'rgba(255,77,106,.4)'],[50,'rgba(255,255,255,.08)'],[30,'rgba(77,159,255,.4)']].forEach(([v,c])=>{
    const y=PT+chartH*(1-v/100);
    ctx.strokeStyle=c; ctx.lineWidth=.8; ctx.setLineDash([3,3]);
    ctx.beginPath(); ctx.moveTo(PL,y); ctx.lineTo(W-PR,y); ctx.stroke(); ctx.setLineDash([]);
    ctx.fillStyle=c; ctx.font='7px JetBrains Mono,monospace'; ctx.textAlign='right';
    ctx.fillText(v,PL-3,y+3);
  });
  /* RSI 선 */
  ctx.beginPath(); ctx.strokeStyle='#f5c842'; ctx.lineWidth=1.2; let mv=true;
  for(let i=0;i<n;i++){
    if(rsi[i]===null){mv=true;continue;}
    const x=xBase+i*step, y=PT+chartH*(1-rsi[i]/100);
    mv?(ctx.moveTo(x,y),mv=false):ctx.lineTo(x,y);
  }
  ctx.stroke();
  /* 현재 RSI 값 */
  const last=rsi.filter(v=>v!==null).pop();
  if(last!=null){
    const yy=PT+chartH*(1-last/100);
    ctx.fillStyle=last<30?'#4d9fff':last>70?'#ff4d6a':'#f5c842';
    ctx.font='bold 8px JetBrains Mono,monospace'; ctx.textAlign='left';
    ctx.fillText(last.toFixed(1),W-PR+2,yy+3);
    /* RSI 점 */
    ctx.beginPath(); ctx.arc(xBase+(n-1)*step,yy,2.5,0,Math.PI*2);
    ctx.fillStyle=last<30?'#4d9fff':last>70?'#ff4d6a':'#f5c842'; ctx.fill();
  }
}

/* ── 크로스헤어 ── */
const mainWrap=document.getElementById('mainWrap');
const cvl=document.getElementById('cvl'), chl=document.getElementById('chl'), ctp=document.getElementById('ctp');
mainWrap.addEventListener('mousemove',ev=>{
  const m=document.getElementById('cv')._m; if(!m) return;
  const rect=mainWrap.getBoundingClientRect(), mx=ev.clientX-rect.left, my=ev.clientY-rect.top;
  const {PL,PR,PT,PB,W,H,vis,step,xBase}=m;
  const leftEdge=xBase-step/2;
  const rightEdge=xBase+step*(vis.length-1)+step/2;
  if(mx<leftEdge||mx>rightEdge||my<PT||my>H-PB){ cvl.style.display='none'; chl.style.display='none'; ctp.style.display='none'; return; }
  const idx=Math.round((mx-xBase)/step), c=vis[Math.max(0,Math.min(idx,vis.length-1))];
  const cx=xBase+Math.max(0,Math.min(idx,vis.length-1))*step;
  cvl.style.display='block'; cvl.style.left=cx+'px';
  chl.style.display='block'; chl.style.top=my+'px';
  ctp.style.display='block';
  const closes=vis.map(v=>v.close), rsiArr=calcRSI(closes,14), rv=rsiArr[Math.max(0,Math.min(idx,rsiArr.length-1))];
  const bb=calcBB(closes), bv=bb[Math.max(0,Math.min(idx,bb.length-1))];
  const volV=c.volume>=1e6?(c.volume/1e6).toFixed(2)+'M':c.volume>=1e3?(c.volume/1e3).toFixed(0)+'K':c.volume.toLocaleString();
  const tw=195, tx=mx+10>W-tw?mx-tw-4:mx+10, ty=Math.min(my+6,H-140);
  ctp.style.left=tx+'px'; ctp.style.top=ty+'px';
  const up=c.close>=c.open;
  ctp.innerHTML=
    '<div style="font-size:7px;color:var(--t3);margin-bottom:4px;letter-spacing:.5px">'+esc(c.time||'')+'</div>'+
    '<div style="display:grid;grid-template-columns:1fr 1fr;gap:1px 10px;font-size:9px;">'+
      '<span style="color:var(--t2)">O</span><span>'+c.open.toLocaleString('ko-KR')+'</span>'+
      '<span style="color:var(--t2)">H</span><span style="color:#00d97e">'+c.high.toLocaleString('ko-KR')+'</span>'+
      '<span style="color:var(--t2)">L</span><span style="color:#ff4d6a">'+c.low.toLocaleString('ko-KR')+'</span>'+
      '<span style="color:var(--t2)">C</span><span style="color:'+(up?'#00d97e':'#ff4d6a')+';font-weight:600">'+c.close.toLocaleString('ko-KR')+'</span>'+
      '<span style="color:var(--t2)">VOL</span><span style="color:var(--gold)">'+volV+'</span>'+
      (rv!=null?'<span style="color:var(--t2)">RSI</span><span style="color:var(--gold)">'+rv.toFixed(1)+'</span>':'')+
      (bv&&bv.upper!=null?'<span style="color:var(--t2)">BB↑</span><span style="color:var(--blue)">'+bv.upper.toFixed(0)+'</span>':'')+
      (bv&&bv.lower!=null?'<span style="color:var(--t2)">BB↓</span><span style="color:var(--blue)">'+bv.lower.toFixed(0)+'</span>':'')+
    '</div>';
});
mainWrap.addEventListener('mouseleave',()=>{ cvl.style.display='none'; chl.style.display='none'; ctp.style.display='none'; });

/* ── 줌 / 패닝 ── */
const cWrap=document.getElementById('cWrap');
window.zm=function(d,anchor){
  const rng=CS.zE-CS.zS;
  const nr=Math.max(.03,Math.min(1,rng*(d>0?.78:1.28)));
  const a=Math.max(0,Math.min(1,anchor == null ? 0.5 : anchor));
  const pivot=CS.zS+rng*a;
  let ns=pivot-nr*a, ne=ns+nr;
  if(ns<0){ ne-=ns; ns=0; }
  if(ne>1){ ns-=ne-1; ne=1; }
  CS.zS=Math.max(0,ns); CS.zE=Math.min(1,ne);
  drawAll();
};
window.zmR=function(){ resetViewRange(); drawAll(); };
cWrap.addEventListener('wheel',ev=>{
  ev.preventDefault();
  const rect=cWrap.getBoundingClientRect();
  const anchor=(ev.clientX-rect.left)/Math.max(rect.width,1);
  zm(ev.deltaY<0?1:-1,anchor);
},{passive:false});
cWrap.addEventListener('mousedown',ev=>{
  if(ev.button!==0) return;
  CS.drag=true; CS.dragX=ev.clientX; CS.dragZ=[CS.zS,CS.zE];
  cWrap.classList.add('drag');
});
window.addEventListener('mouseup',()=>{ CS.drag=false; cWrap.classList.remove('drag'); });
cWrap.addEventListener('dblclick',()=>{ zmR(); });
window.addEventListener('mousemove',ev=>{
  if(!CS.drag||!CS.dragZ) return;
  const dx=(ev.clientX-CS.dragX)/cWrap.getBoundingClientRect().width, rng=CS.dragZ[1]-CS.dragZ[0];
  let ns=CS.dragZ[0]-dx,ne=CS.dragZ[1]-dx;
  if(ns<0){ne-=ns;ns=0;} if(ne>1){ns-=(ne-1);ne=1;}
  CS.zS=Math.max(0,ns); CS.zE=Math.min(1,ne); drawAll();
});
window.addEventListener('resize',drawAll);

/* ════════════════════════════════════════════
   사이드바 랭킹
════════════════════════════════════════════ */
let sbD=[],sbM='KR',sbE='NAS',sbT='vol',sbAuto=true,sbPrev=new Set(),sbCd=POLL,sbTimer=null;

window.setSbMkt=function(m){
  sbM=m;
  ['KR','US'].forEach(x=>document.getElementById('sm'+x).className='mkt-sb'+(x===m?(m==='KR'?' on kr':' on us'):''));
  document.getElementById('sbExch').classList.toggle('show',m==='US');
  document.getElementById('sbTabs').className='sb-tabs'+(m==='US'?' us':'');
  document.getElementById('sbFt').textContent='KIS · /api/market/ranking?market='+m;
  loadRanking();
};
window.setSbExch=function(ex){ sbE=ex; ['NAS','NYS','AMS'].forEach(x=>document.getElementById('ex'+x).classList.toggle('on',x===ex)); loadRanking(); };
window.setSbTab=function(t){ sbT=t; ['Vol','Chg','Hi'].forEach(x=>document.getElementById('st'+x).classList.toggle('on',x.toLowerCase()===t)); if(sbD.length) renderRank(sbD); };
window.toggleAuto=function(){
  sbAuto=!sbAuto;
  document.getElementById('sbAb').classList.toggle('act',sbAuto);
  if(sbAuto) restartCd(); else { clearInterval(sbTimer); document.getElementById('sbPgf').style.width='0%'; document.getElementById('sbCdn').textContent='—'; }
};
window.loadRanking=function(){
  const rb=document.getElementById('sbRb'); rb.classList.add('spin');
  fetch(B+'/api/market/ranking?market='+sbM+'&exch='+sbE)
    .then(r=>{ if(!r.ok) throw new Error('HTTP '+r.status); return r.json(); })
    .then(json=>{
      const raw=json.data||json.output||json.items||[];
      sbD=Array.isArray(raw)?raw.slice(0,30):[];
      if(!sbD.length&&json.rt_cd&&json.rt_cd!=='0'){ showSbErr('API 오류: '+(json.msg1||json.msg||'알 수 없음')); }
      else{ renderRank(sbD); const n=new Date(); document.getElementById('sbUpd').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds())+' 갱신'; if(sbAuto) restartCd(); }
    })
    .catch(err=>showSbErr(err.message))
    .finally(()=>rb.classList.remove('spin'));
};
function showSbErr(msg){ document.getElementById('sbBody').innerHTML='<div class="sb-msg er"><div class="sb-mi">✕</div><div class="sb-mt">'+esc(msg)+'</div></div>'; }
function renderRank(data){
  const body=document.getElementById('sbBody');
  if(!data.length){ body.innerHTML='<div class="sb-msg"><div class="sb-mi">—</div><div class="sb-mt">데이터 없음</div></div>'; return; }
  const isUS=sbM==='US';
  const metric=(r)=>parseFloat((r&&r.acml_tr_pbmn!=null)?r.acml_tr_pbmn:r&&r.acml_vol)||0;
  const sorted=[...data].sort((a,b)=>{
    if(sbT==='chg') return Math.abs(parseFloat(b.prdy_ctrt)||0)-Math.abs(parseFloat(a.prdy_ctrt)||0);
    if(sbT==='hi')  return (parseFloat(b.stck_prpr)||0)-(parseFloat(a.stck_prpr)||0);
    return metric(b)-metric(a);
  }).slice(0,30);
  const maxV=Math.max(...sorted.map(r=>metric(r)),1);
  const newSet=new Set(sorted.map(r=>r.symbol));
  body.innerHTML='<div class="rk-list">'+sorted.map((r,i)=>{
    const rk=i+1, rc=rk===1?'r1':rk===2?'r2':rk===3?'r3':'rN';
    const code=r.symbol||'—', name=r.name||code;
    const rawP=r.stck_prpr;
    const price=rawP&&rawP!=='0'?(isUS?'$'+Number(rawP).toFixed(2):Number(rawP).toLocaleString('ko-KR')):'—';
    const diff=parseFloat(r.prdy_ctrt||'0'), sign=r.prdy_vrss_sign||'3';
    const isUp=sign==='1'||sign==='2', isDn=sign==='4'||sign==='5';
    let cc,pfx; if(isUp){cc=isUS?'uu':'ku';pfx='▲';} else if(isDn){cc=isUS?'ud':'kd';pfx='▼';} else{cc='fl';pfx='—';}
    const vol=metric(r);
    const vp=Math.max((vol/maxV*100),2).toFixed(1);
    const vf=vol>=1e8?(vol/1e8).toFixed(1)+'억':vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
    const isNew=!sbPrev.has(code);
    return '<div class="rk-row'+(isNew?' fl':'')+'" style="animation-delay:'+(i*10)+'ms" onclick="onRkClick(\''+esc(code)+'\',\''+esc(sbM)+'\',\''+esc(sbE)+'\')">'+
      '<span class="rn '+rc+'">'+rk+'</span>'+
      '<div class="ri-i">'+
        '<div class="ri-nm" title="'+esc(name)+'">'+esc(name)+'</div>'+
        '<div class="ri-cd">'+esc(code)+'</div>'+
        '<div class="ri-bw"><div class="ri-b '+(isUS?'us':'kr')+'" style="width:'+vp+'%"></div><div class="ri-vl">'+vf+'</div></div>'+
      '</div>'+
      '<div class="rp-i"><div class="rp-pr">'+price+'</div><div class="rp-ch '+cc+'">'+pfx+Math.abs(diff).toFixed(2)+'%</div></div>'+
    '</div>';
  }).join('')+'</div>';
  sbPrev=newSet;
}
window.onRkClick=function(code,mkt,exch){
  CS.sym=code; CS.mkt=mkt; CS.exch=exch;
  document.getElementById('symIn').value=code;
  ['KR','US'].forEach(x=>document.getElementById('m'+x).className='mkt-b'+(x===mkt?(mkt==='KR'?' on kr':' on us'):''));
  document.getElementById('exchS').classList.toggle('show',mkt==='US');
  fetchChart();
};
function restartCd(){
  sbCd=POLL; updPg(); clearInterval(sbTimer);
  sbTimer=setInterval(()=>{ sbCd--; document.getElementById('sbCdn').textContent=sbCd; updPg(); if(sbCd<=0) loadRanking(); },1000);
}
function updPg(){ document.getElementById('sbPgf').style.width=((POLL-sbCd)/POLL*100)+'%'; }

/* 초기 로드 */
fetchChart();
loadRanking();
</script>
</body>
</html>
