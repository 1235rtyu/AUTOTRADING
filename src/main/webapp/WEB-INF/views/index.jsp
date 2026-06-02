<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<title>AUTOTRADE TERMINAL</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Syne:wght@400;600;700;800&family=JetBrains+Mono:wght@300;400;500;600&display=swap" rel="stylesheet">
<style>
:root{
  --void:#060709;--base:#0a0c10;--surface:#0f1117;--panel:#141720;--panel-hi:#191d28;--hover:#1e2330;
  --lime:#a8ff3e;--lime-d:rgba(168,255,62,.1);--lime-b:rgba(168,255,62,.22);--lime-glow:0 0 20px rgba(168,255,62,.4);
  --emerald:#00d97e;--emerald-d:rgba(0,217,126,.08);--emerald-b:rgba(0,217,126,.25);--emerald-glow:0 0 14px rgba(0,217,126,.4);
  --red:#ff4d6a;--red-d:rgba(255,77,106,.08);--red-b:rgba(255,77,106,.28);
  --gold:#f5c842;--gold-d:rgba(245,200,66,.08);--gold-b:rgba(245,200,66,.25);
  --blue:#4d9fff;--blue-d:rgba(77,159,255,.08);--blue-b:rgba(77,159,255,.25);
  --purple:#b07fff;--purple-d:rgba(176,127,255,.08);--purple-b:rgba(176,127,255,.22);
  --rim:rgba(255,255,255,.055);--rim-hi:rgba(255,255,255,.11);
  --t1:#e8edf5;--t2:#7a8499;--t3:#3a4155;--t4:#1c2130;
  --mono:'JetBrains Mono',monospace;--sans:'Syne',sans-serif;
  --r:6px;--r2:10px;--r3:16px;
  --topbar-h:56px;--sidebar-w:290px;
}
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html{height:100%;scroll-behavior:smooth;}
body{min-height:100%;font-family:var(--sans);font-size:13px;color:var(--t1);background:var(--void);overflow-x:hidden;}

.bg-layer{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:
    radial-gradient(ellipse 90% 60% at 50% -10%,rgba(168,255,62,.09) 0%,transparent 55%),
    radial-gradient(ellipse 50% 70% at 100% 80%,rgba(0,217,126,.06) 0%,transparent 50%),
    radial-gradient(ellipse 45% 50% at -5% 60%,rgba(77,159,255,.05) 0%,transparent 50%);}
.bg-grid{position:fixed;inset:0;z-index:0;pointer-events:none;
  background-image:radial-gradient(rgba(168,255,62,.055) 1px,transparent 1px);background-size:28px 28px;}
.bg-scan{position:fixed;inset:0;z-index:0;pointer-events:none;
  background:repeating-linear-gradient(0deg,transparent,transparent 3px,rgba(0,0,0,.03) 3px,rgba(0,0,0,.03) 4px);}

@keyframes slide-down  {from{opacity:0;transform:translateY(-12px);}to{opacity:1;transform:none;}}
@keyframes fade-up     {from{opacity:0;transform:translateY(18px);}to{opacity:1;transform:none;}}
@keyframes slide-in-r  {from{opacity:0;transform:translateX(20px);}to{opacity:1;transform:none;}}
@keyframes row-appear  {from{opacity:0;transform:translateX(8px);}to{opacity:1;transform:none;}}
@keyframes pulse-dot   {0%,100%{transform:scale(1);opacity:1;}50%{transform:scale(.75);opacity:.4;}}
@keyframes spin        {from{transform:rotate(0);}to{transform:rotate(360deg);}}
@keyframes flash-row   {0%{background:rgba(168,255,62,.1);}100%{background:transparent;}}
@keyframes bar-in      {from{width:0;}to{width:var(--bw,0%);}}

/* ── TOPBAR ── */
.topbar{position:fixed;top:0;left:0;right:0;z-index:200;height:var(--topbar-h);
  display:flex;align-items:center;
  background:rgba(6,7,9,.9);backdrop-filter:blur(14px);
  border-bottom:1px solid var(--rim);animation:slide-down .5s ease both;}
.topbar::after{content:'';position:absolute;bottom:0;left:0;right:0;height:1px;
  background:linear-gradient(90deg,transparent,var(--lime),rgba(168,255,62,.3),transparent);opacity:.5;}
.tb-logo{display:flex;align-items:center;gap:11px;padding:0 22px;height:100%;
  border-right:1px solid var(--rim);min-width:210px;}
.logo-mark{width:34px;height:34px;background:var(--lime);border-radius:8px;
  display:flex;align-items:center;justify-content:center;flex-shrink:0;
  position:relative;overflow:hidden;box-shadow:var(--lime-glow);}
.logo-mark::before{content:'';position:absolute;inset:0;background:linear-gradient(135deg,rgba(255,255,255,.35) 0%,transparent 60%);}
.logo-mark svg{width:18px;height:18px;}
.logo-name{font-size:14px;font-weight:800;letter-spacing:.5px;}
.logo-name span{color:var(--lime);}
.logo-ver{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1.5px;margin-top:1px;}
.tb-spacer{flex:1;}
.tb-status-pill{display:flex;align-items:center;gap:6px;font-family:var(--mono);font-size:10px;
  color:var(--emerald);padding:4px 12px;border-radius:20px;
  background:var(--emerald-d);border:1px solid var(--emerald-b);letter-spacing:.5px;}
.tb-pulse{width:6px;height:6px;border-radius:50%;background:var(--emerald);
  box-shadow:var(--emerald-glow);animation:pulse-dot 1.4s ease-in-out infinite;}
.tb-nav{display:flex;align-items:center;gap:4px;padding:0 18px;}
.tb-nav-link{font-family:var(--mono);font-size:10px;letter-spacing:.5px;padding:5px 12px;
  border-radius:var(--r);border:1px solid transparent;background:transparent;color:var(--t2);
  cursor:pointer;transition:all .15s;text-decoration:none;}
.tb-nav-link:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.tb-login{display:flex;align-items:center;gap:6px;padding:0 14px;border-left:1px solid var(--rim);}
.tb-login form{display:flex;align-items:center;gap:6px;}
.tb-login input{height:28px;width:116px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:var(--r);color:var(--t1);font-family:var(--mono);font-size:10px;padding:0 8px;}
.tb-login input::placeholder{color:var(--t3);}
.tb-login-btn{height:28px;padding:0 12px;border-radius:var(--r);border:1px solid var(--lime-b);
  background:var(--lime-d);color:var(--lime);font-family:var(--mono);font-size:10px;cursor:pointer;transition:all .15s;}
.tb-login-btn:hover{background:var(--lime);color:var(--void);}
.tb-login-status{display:none;align-items:center;gap:8px;font-family:var(--mono);font-size:10px;color:var(--t2);}
.tb-login-status .acc{color:var(--lime);}
.tb-login-err{font-family:var(--mono);font-size:10px;color:var(--red);display:none;margin-left:4px;}
.tb-clock{padding:0 18px;height:100%;border-left:1px solid var(--rim);
  display:flex;flex-direction:column;align-items:flex-end;justify-content:center;gap:2px;}
.clock-t{font-family:var(--mono);font-size:15px;font-weight:500;letter-spacing:2px;}
.clock-d{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1px;}

/* ── LAYOUT ── */
.layout{display:flex;padding-top:var(--topbar-h);min-height:100vh;}
.main-content{flex:1;min-width:0;margin-right:var(--sidebar-w);}
.page{position:relative;z-index:1;max-width:940px;margin:0 auto;padding:36px 24px 72px;}

/* ── HERO ── */
.hero{display:grid;grid-template-columns:1fr auto;align-items:start;gap:28px;
  padding:36px 0 40px;animation:fade-up .6s .1s ease both;}
.hero-eyebrow{font-family:var(--mono);font-size:11px;color:var(--lime);
  letter-spacing:4px;text-transform:uppercase;margin-bottom:12px;
  display:flex;align-items:center;gap:10px;}
.hero-eyebrow::before{content:'';width:24px;height:1px;background:var(--lime);box-shadow:var(--lime-glow);}
.hero-title{font-size:clamp(28px,4vw,50px);font-weight:800;letter-spacing:-2px;line-height:1.05;margin-bottom:12px;}
.hero-title .hl{color:var(--lime);text-shadow:var(--lime-glow);}
.hero-title .dim{color:var(--t2);}
.hero-desc{font-size:14px;color:var(--t2);line-height:1.7;max-width:420px;margin-bottom:20px;}
.hero-ctas{display:flex;gap:10px;flex-wrap:wrap;}
.cta-primary{height:44px;padding:0 26px;background:var(--lime);border:none;border-radius:var(--r2);
  color:var(--void);font-family:var(--sans);font-size:13px;font-weight:700;
  cursor:pointer;transition:all .2s;text-decoration:none;
  display:inline-flex;align-items:center;gap:8px;box-shadow:var(--lime-glow);}
.cta-primary:hover{transform:translateY(-2px);box-shadow:0 0 32px rgba(168,255,62,.6);}
.cta-secondary{height:44px;padding:0 20px;background:var(--panel);border:1px solid var(--rim-hi);
  border-radius:var(--r2);color:var(--t1);font-family:var(--mono);font-size:11px;
  cursor:pointer;transition:all .2s;text-decoration:none;display:inline-flex;align-items:center;gap:8px;}
.cta-secondary:hover{background:var(--hover);border-color:var(--lime-b);color:var(--lime);transform:translateY(-1px);}
.api-hint{margin-top:16px;display:flex;flex-wrap:wrap;gap:6px;align-items:center;}
.api-lbl{font-family:var(--mono);font-size:10px;color:var(--t3);letter-spacing:1px;}
.api-chip{font-family:var(--mono);font-size:10px;background:var(--base);border:1px solid var(--rim-hi);
  border-radius:4px;padding:3px 9px;color:var(--lime);}

/* ── SYSTEM STATUS 위젯 ── */
.hero-widget{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  overflow:hidden;min-width:220px;box-shadow:0 20px 56px rgba(0,0,0,.4);}
.hw-head{padding:10px 14px;border-bottom:1px solid var(--rim);background:var(--panel-hi);
  display:flex;align-items:center;justify-content:space-between;}
.hw-title{font-family:var(--mono);font-size:10px;color:var(--t2);letter-spacing:2px;}
.hw-live{display:flex;align-items:center;gap:5px;font-family:var(--mono);font-size:9px;color:var(--emerald);}
.hw-ld{width:5px;height:5px;border-radius:50%;background:var(--emerald);animation:pulse-dot 1.4s ease-in-out infinite;}
.hw-item{padding:9px 14px;border-bottom:1px solid var(--t4);display:flex;align-items:center;
  justify-content:space-between;transition:background .12s;}
.hw-item:last-child{border-bottom:none;}
.hw-item:hover{background:var(--hover);}
.hw-key{font-family:var(--mono);font-size:10px;color:var(--t2);}
.hw-val{font-family:var(--mono);font-size:11px;font-weight:500;}
.hw-val.lime{color:var(--lime);}
.hw-val.emerald{color:var(--emerald);}
.hw-val.gold{color:var(--gold);}
.hw-val.red{color:var(--red);}
.hw-val.blue{color:var(--blue);}
.hw-bar{width:100%;height:2px;background:var(--lime);}

/* ── 시장 지수 ── */
.idx-grid{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));gap:8px;
  margin-bottom:24px;animation:fade-up .5s .18s ease both;}
@media(max-width:900px){.idx-grid{grid-template-columns:repeat(3,1fr);}}
.idx-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:10px 12px;transition:border-color .15s;}
.idx-card:hover{border-color:var(--rim-hi);}
.idx-name{font-family:var(--mono);font-size:8px;color:var(--t3);letter-spacing:1px;text-transform:uppercase;margin-bottom:5px;}
.idx-price{font-family:var(--mono);font-size:15px;font-weight:600;color:var(--t1);line-height:1.1;margin-bottom:3px;}
.idx-change{font-family:var(--mono);font-size:10px;}
.idx-change.up{color:var(--red);}
.idx-change.dn{color:var(--blue);}
.idx-change.fl{color:var(--t3);}
/* 지수 미니 게이지 */
.idx-bar-wrap{height:2px;background:var(--t4);border-radius:1px;margin-top:6px;overflow:hidden;}
.idx-bar{height:100%;border-radius:1px;animation:bar-in .6s ease both;}

/* ── 실시간 계좌 요약 ── */
.cash-strip{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;
  margin-bottom:24px;animation:fade-up .5s .22s ease both;}
.cash-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:10px 14px;position:relative;overflow:hidden;}
.cash-card::before{content:'';position:absolute;top:0;left:0;right:0;height:2px;
  background:var(--cc-acc,transparent);}
.cash-label{font-family:var(--mono);font-size:7px;color:var(--t3);letter-spacing:1.5px;
  text-transform:uppercase;margin-bottom:6px;}
.cash-val{font-family:var(--mono);font-size:16px;font-weight:600;line-height:1;}
.cash-sub{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:4px;}

/* ── 실행 중인 전략 스트립 ── */
.running-strip{margin-bottom:24px;animation:fade-up .5s .25s ease both;}
.rs-hd{display:flex;align-items:center;gap:8px;margin-bottom:8px;}
.rs-title{font-family:var(--mono);font-size:8px;color:var(--t2);letter-spacing:1.5px;text-transform:uppercase;}
.rs-count{font-family:var(--mono);font-size:8px;padding:2px 7px;border-radius:4px;
  background:var(--lime-d);border:1px solid var(--lime-b);color:var(--lime);}
.rs-empty{font-family:var(--mono);font-size:10px;color:var(--t3);
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r2);
  padding:12px 14px;letter-spacing:1px;}
.rs-list{display:flex;gap:6px;flex-wrap:wrap;}
.rs-chip{display:flex;align-items:center;gap:6px;
  font-family:var(--mono);font-size:10px;
  background:var(--panel);border:1px solid var(--rim);border-radius:var(--r);
  padding:5px 10px;transition:border-color .15s;}
.rs-chip:hover{border-color:var(--lime-b);}
.rs-chip-dot{width:5px;height:5px;border-radius:50%;background:var(--emerald);
  animation:pulse-dot 1.6s ease-in-out infinite;flex-shrink:0;}
.rs-chip-sym{color:var(--t1);font-weight:600;}
.rs-chip-mkt{color:var(--t3);font-size:8px;}

/* ── 섹션 디바이더 ── */
.sec-div{display:flex;align-items:center;gap:14px;margin-bottom:20px;animation:fade-up .5s .28s ease both;}
.div-ln{flex:1;height:1px;background:linear-gradient(90deg,var(--rim),transparent);}
.div-lbl{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:3px;text-transform:uppercase;}

/* ── 메뉴 카드 ── */
.menu-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:12px;margin-bottom:32px;}
.menu-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);
  overflow:hidden;text-decoration:none;color:inherit;
  display:flex;flex-direction:column;transition:all .22s;animation:fade-up .5s ease both;}
.menu-card:nth-child(1){animation-delay:.30s;}.menu-card:nth-child(2){animation-delay:.36s;}
.menu-card:nth-child(3){animation-delay:.42s;}.menu-card:nth-child(4){animation-delay:.48s;}
.menu-card:hover{transform:translateY(-3px);border-color:var(--rim-hi);box-shadow:0 12px 40px rgba(0,0,0,.4);}
.menu-card:hover .card-arrow{opacity:1;transform:translateX(0);}
.menu-card::before{content:'';display:block;height:2px;}
.menu-card.c-lime::before   {background:var(--lime);box-shadow:var(--lime-glow);}
.menu-card.c-emerald::before{background:var(--emerald);}
.menu-card.c-gold::before   {background:var(--gold);}
.menu-card.c-blue::before   {background:var(--blue);}
.menu-card.c-lime:hover    {background:linear-gradient(160deg,rgba(168,255,62,.04) 0%,var(--panel) 50%);}
.menu-card.c-emerald:hover {background:linear-gradient(160deg,rgba(0,217,126,.04) 0%,var(--panel) 50%);}
.menu-card.c-gold:hover    {background:linear-gradient(160deg,rgba(245,200,66,.03) 0%,var(--panel) 50%);}
.menu-card.c-blue:hover    {background:linear-gradient(160deg,rgba(77,159,255,.03) 0%,var(--panel) 50%);}
.card-body{padding:18px 20px 14px;flex:1;}
.card-icon{width:38px;height:38px;border-radius:var(--r2);display:flex;align-items:center;
  justify-content:center;font-size:18px;margin-bottom:11px;}
.c-lime    .card-icon{background:var(--lime-d);border:1px solid var(--lime-b);}
.c-emerald .card-icon{background:var(--emerald-d);border:1px solid var(--emerald-b);}
.c-gold    .card-icon{background:var(--gold-d);border:1px solid var(--gold-b);}
.c-blue    .card-icon{background:var(--blue-d);border:1px solid var(--blue-b);}
.card-name{font-size:16px;font-weight:700;color:var(--t1);margin-bottom:5px;}
.card-desc{font-size:11px;color:var(--t2);line-height:1.6;}
.card-foot{padding:9px 20px;border-top:1px solid var(--rim);background:var(--panel-hi);
  display:flex;align-items:center;justify-content:space-between;}
.card-tag  {font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:1px;text-transform:uppercase;}
.card-arrow{font-family:var(--mono);font-size:10px;color:var(--lime);opacity:0;transform:translateX(-6px);transition:all .2s;}

/* ── 전략 카드 ── */
.strat-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-bottom:28px;animation:fade-up .5s .50s ease both;}
.strat-card{background:var(--panel);border:1px solid var(--rim);border-radius:var(--r3);padding:18px 20px;transition:border-color .2s;}
.strat-card:hover{border-color:var(--rim-hi);}
.strat-head{display:flex;align-items:center;gap:10px;margin-bottom:13px;}
.strat-icon{width:32px;height:32px;border-radius:var(--r);display:flex;align-items:center;justify-content:center;font-size:14px;}
.strat-icon.lime{background:var(--lime-d);border:1px solid var(--lime-b);}
.strat-icon.gold{background:var(--gold-d);border:1px solid var(--gold-b);}
.strat-name{font-size:13px;font-weight:700;color:var(--t1);}
.strat-tag {font-family:var(--mono);font-size:9px;color:var(--t2);letter-spacing:1px;margin-top:2px;}
.param-row {display:flex;align-items:center;justify-content:space-between;padding:6px 0;border-bottom:1px solid var(--t4);}
.param-row:last-child{border-bottom:none;}
.param-key{font-family:var(--mono);font-size:10px;color:var(--t2);}
.param-val{font-family:var(--mono);font-size:11px;font-weight:500;color:var(--lime);}

/* ── NAV FOOTER ── */
.nav-footer{display:flex;gap:8px;flex-wrap:wrap;padding-top:12px;border-top:1px solid var(--rim);animation:fade-up .5s .58s ease both;}
.nav-btn{font-family:var(--mono);font-size:10px;letter-spacing:.5px;padding:7px 15px;border-radius:var(--r);
  border:1px solid transparent;background:transparent;color:var(--t2);cursor:pointer;transition:all .15s;
  text-decoration:none;display:inline-flex;align-items:center;gap:5px;}
.nav-btn:hover{background:var(--hover);border-color:var(--rim-hi);color:var(--t1);}
.nav-btn.primary{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);font-weight:500;}
.nav-btn.primary:hover{background:var(--lime);color:var(--void);}

/* ════════════════
   RIGHT SIDEBAR
════════════════ */
.sidebar{position:fixed;top:var(--topbar-h);right:0;width:var(--sidebar-w);
  height:calc(100vh - var(--topbar-h));background:var(--base);border-left:1px solid var(--rim);
  display:flex;flex-direction:column;z-index:150;animation:slide-in-r .55s .15s ease both;}
.sb-hd{flex-shrink:0;background:var(--panel-hi);border-bottom:1px solid var(--rim);}
.sb-hd-top{display:flex;align-items:center;justify-content:space-between;padding:11px 14px 6px;}
.sb-title{display:flex;align-items:center;gap:7px;font-family:var(--mono);font-size:10px;font-weight:600;
  color:var(--lime);letter-spacing:2px;text-transform:uppercase;}
.sb-title-dot{width:7px;height:7px;border-radius:50%;background:var(--lime);box-shadow:var(--lime-glow);animation:pulse-dot 2s ease-in-out infinite;}
.sb-btn-row{display:flex;align-items:center;gap:4px;}
.sb-icon-btn{width:26px;height:26px;border-radius:var(--r);border:1px solid var(--rim-hi);background:transparent;
  color:var(--t2);cursor:pointer;transition:all .15s;display:flex;align-items:center;justify-content:center;}
.sb-icon-btn:hover,.sb-icon-btn.on{border-color:var(--lime-b);color:var(--lime);background:var(--lime-d);}
.sb-icon-btn.spinning svg{animation:spin .6s linear infinite;}
.sb-hd-meta{display:flex;align-items:center;justify-content:space-between;padding:4px 14px 9px;}
.sb-updated{font-family:var(--mono);font-size:9px;color:var(--t3);}
.sb-cd-wrap{display:flex;align-items:center;gap:5px;}
.sb-cd-lbl{font-family:var(--mono);font-size:9px;color:var(--t3);}
.sb-cd-num{font-family:var(--mono);font-size:10px;font-weight:600;color:var(--lime);min-width:22px;text-align:right;}
.sb-prog{height:2px;background:var(--t4);flex-shrink:0;}
.sb-prog-fill{height:100%;width:0%;background:linear-gradient(90deg,var(--lime),var(--emerald));
  box-shadow:0 0 6px var(--lime);transition:width 1s linear;}

/* 마켓 세그먼트 */
.sb-mkt{display:flex;gap:3px;padding:6px 10px;border-bottom:1px solid var(--rim);}
.sb-mkt-btn{flex:1;height:22px;font-family:var(--mono);font-size:8px;letter-spacing:.8px;
  border:1px solid var(--rim-hi);border-radius:4px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.sb-mkt-btn.on-kr{background:var(--lime-d);border-color:var(--lime-b);color:var(--lime);}
.sb-mkt-btn.on-us{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}

/* 탭 */
.sb-tabs{flex-shrink:0;display:flex;border-bottom:1px solid var(--rim);background:var(--panel-hi);}
.sb-tab{flex:1;padding:7px 0;text-align:center;font-family:var(--mono);font-size:9px;
  letter-spacing:1.5px;text-transform:uppercase;color:var(--t3);cursor:pointer;
  user-select:none;border-bottom:2px solid transparent;transition:all .15s;}
.sb-tab:hover{color:var(--t2);}
.sb-tab.on{color:var(--lime);border-bottom-color:var(--lime);}
.sb-tabs.us .sb-tab.on{color:var(--blue);border-bottom-color:var(--blue);}

/* US 거래소 선택 */
.sb-exch{flex-shrink:0;display:none;gap:3px;padding:5px 10px;border-bottom:1px solid var(--rim);background:var(--panel);}
.sb-exch.show{display:flex;}
.exch-btn{flex:1;height:20px;font-family:var(--mono);font-size:8px;letter-spacing:.3px;
  border:1px solid var(--rim-hi);border-radius:3px;background:transparent;color:var(--t3);cursor:pointer;transition:all .12s;}
.exch-btn.on{background:var(--blue-d);border-color:var(--blue-b);color:var(--blue);}

/* 스크롤 영역 */
.sb-body{flex:1;overflow-y:auto;scrollbar-width:thin;scrollbar-color:var(--rim-hi) transparent;}
.sb-body::-webkit-scrollbar{width:3px;}
.sb-body::-webkit-scrollbar-thumb{background:var(--rim-hi);border-radius:2px;}
.sb-msg{padding:44px 20px;text-align:center;display:flex;flex-direction:column;align-items:center;gap:10px;}
.sb-msg-icon{font-size:22px;opacity:.3;}
.sb-msg-txt{font-family:var(--mono);font-size:11px;color:var(--t3);letter-spacing:1.5px;}
.sb-msg.loading .sb-msg-icon{opacity:1;color:var(--lime);animation:pulse-dot 1s ease-in-out infinite;}
.sb-msg.error .sb-msg-icon{opacity:1;color:var(--red);}
.sb-msg.error .sb-msg-txt{color:var(--red);}

/* 랭킹 */
.rank-list{padding:3px 0;}
.rank-row{display:grid;grid-template-columns:28px 1fr auto;align-items:center;gap:8px;
  padding:8px 12px;border-bottom:1px solid var(--t4);transition:background .1s;
  animation:row-appear .25s ease both;}
.rank-row:last-child{border-bottom:none;}
.rank-row:hover{background:var(--hover);}
.rank-row.new-row{animation:flash-row .6s ease both;}
.rn{font-family:var(--mono);font-size:11px;font-weight:700;text-align:center;flex-shrink:0;line-height:1;}
.rn.r1{color:var(--gold);text-shadow:0 0 8px rgba(245,200,66,.6);font-size:13px;}
.rn.r2{color:#b0b8c8;}.rn.r3{color:#cd8b5a;}.rn.rN{color:var(--t3);font-size:10px;}
.ri{min-width:0;}
.ri-name{font-size:12px;font-weight:600;color:var(--t1);white-space:nowrap;overflow:hidden;text-overflow:ellipsis;line-height:1.2;margin-bottom:2px;}
.ri-code{font-family:var(--mono);font-size:9px;color:var(--t3);letter-spacing:.5px;}
.ri-bar-wrap{margin-top:4px;}
.ri-bar{height:2px;border-radius:1px;transition:width .5s ease;}
.ri-bar.kr{background:linear-gradient(90deg,var(--lime),rgba(168,255,62,.1));}
.ri-bar.us{background:linear-gradient(90deg,var(--blue),rgba(77,159,255,.1));}
.ri-vol{font-family:var(--mono);font-size:8px;color:var(--t3);margin-top:2px;letter-spacing:.3px;}
.rp{text-align:right;flex-shrink:0;}
.rp-price{font-family:var(--mono);font-size:11px;font-weight:500;color:var(--t1);white-space:nowrap;}
.rp-chg{font-family:var(--mono);font-size:10px;margin-top:2px;
  display:flex;align-items:center;justify-content:flex-end;gap:1px;white-space:nowrap;}
.rp-chg.kr-up{color:var(--red);}.rp-chg.kr-dn{color:#60a5fa;}
.rp-chg.us-up{color:var(--emerald);}.rp-chg.us-dn{color:var(--red);}.rp-chg.flat{color:var(--t3);}
.sb-foot{flex-shrink:0;padding:7px 12px;border-top:1px solid var(--rim);background:var(--panel-hi);
  font-family:var(--mono);font-size:8px;color:var(--t3);text-align:center;letter-spacing:.5px;}
</style>
</head>
<body>
<div class="bg-layer"></div>
<div class="bg-grid"></div>
<div class="bg-scan"></div>

<!-- ── TOPBAR ── -->
<nav class="topbar">
  <div class="tb-logo">
    <div class="logo-mark">
      <svg viewBox="0 0 24 24" fill="none" stroke="#060709" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
        <polyline points="3 17 9 11 13 15 21 7"/><polyline points="14 7 21 7 21 14"/>
      </svg>
    </div>
    <div><div class="logo-name">AUTO<span>TRADE</span></div><div class="logo-ver">TERMINAL v2.0</div></div>
  </div>
  <div class="tb-spacer"></div>
  <div class="tb-status-pill"><div class="tb-pulse"></div><span id="sysStatus">SYSTEM ONLINE</span></div>
  <div class="tb-nav">
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/control/kr">Control·KR</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/control/us">Control·US</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/history/orders">Orders</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/balances">Balances</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
    <a class="tb-nav-link" href="${pageContext.request.contextPath}/backtest">Back-Test</a>
  </div>
  <div class="tb-login">
    <form id="loginForm" autocomplete="off">
      <input type="text"     name="accountNo"       placeholder="12345678-01" maxlength="20"/>
      <input type="password" name="accountPassword" placeholder="Password"   maxlength="50"/>
      <button class="tb-login-btn" type="submit">Login</button>
    </form>
    <div class="tb-login-status" id="loginStatus">
      <span class="acc" id="loginAccount">****</span>
      <button id="logoutBtn" type="button" class="tb-login-btn">Logout</button>
    </div>
    <div class="tb-login-err" id="loginError"></div>
  </div>
  <div class="tb-clock">
    <div class="clock-t" id="clkTime">--:--:--</div>
    <div class="clock-d" id="clkDate">----.--.--</div>
  </div>
</nav>

<div class="layout">
  <div class="main-content">
    <div class="page">

      <!-- HERO -->
      <div class="hero">
        <div>
          <div class="hero-eyebrow">Algorithmic Trading Console</div>
          <h1 class="hero-title">Auto <span class="hl">Trading</span><br><span class="dim">System</span></h1>
          <p class="hero-desc">RSI + 모멘텀 기반 자동매매 콘솔.<br>실시간 시세 · 전략 실행 · 체결 이력을 한 곳에서 관리합니다.</p>
          <div class="hero-ctas">
            <a class="cta-primary"   href="${pageContext.request.contextPath}/dashboard">▶&nbsp;&nbsp;Dashboard 열기</a>
            <a class="cta-secondary" href="${pageContext.request.contextPath}/control/kr">⚡ Control Panel</a>
          </div>
          <div class="api-hint">
            <span class="api-lbl">API</span>
            <span class="api-chip">/api/control/start</span>
            <span class="api-chip">/api/control/stop</span>
            <span class="api-chip">/api/control/running</span>
          </div>
        </div>

        <!-- System Status 위젯 — 실제 API 데이터 -->
        <div class="hero-widget">
          <div class="hw-head">
            <span class="hw-title">SYSTEM STATUS</span>
            <span class="hw-live"><span class="hw-ld"></span>LIVE</span>
          </div>
          <div class="hw-item"><span class="hw-key">Engine</span>   <span class="hw-val emerald" id="wEngine">—</span></div>
          <div class="hw-item"><span class="hw-key">Watchlist</span><span class="hw-val lime"    id="wWatch">—</span></div>
          <div class="hw-item"><span class="hw-key">Positions</span><span class="hw-val gold"    id="wPos">—</span></div>
          <div class="hw-item"><span class="hw-key">Running</span>  <span class="hw-val blue"    id="wRun">—</span></div>
          <div class="hw-item"><span class="hw-key">Today Orders</span><span class="hw-val"     id="wOrd">—</span></div>
          <div class="hw-item"><span class="hw-key">Uptime</span>  <span class="hw-val"         id="wUptime">00:00:00</span></div>
          <div class="hw-bar"></div>
        </div>
      </div>

      <!-- 시장 지수 -->
      <div class="idx-grid" id="idxGrid">
        <div class="idx-card"><div class="idx-name">KOSPI</div><div class="idx-price" id="ip0">—</div><div class="idx-change fl" id="ic0">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib0" style="background:var(--emerald);width:0%;"></div></div></div>
        <div class="idx-card"><div class="idx-name">KOSDAQ</div><div class="idx-price" id="ip1">—</div><div class="idx-change fl" id="ic1">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib1" style="background:var(--emerald);width:0%;"></div></div></div>
        <div class="idx-card"><div class="idx-name">S&amp;P 500</div><div class="idx-price" id="ip2">—</div><div class="idx-change fl" id="ic2">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib2" style="background:var(--blue);width:0%;"></div></div></div>
        <div class="idx-card"><div class="idx-name">NASDAQ</div><div class="idx-price" id="ip3">—</div><div class="idx-change fl" id="ic3">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib3" style="background:var(--blue);width:0%;"></div></div></div>
        <div class="idx-card"><div class="idx-name">DOW</div><div class="idx-price" id="ip4">—</div><div class="idx-change fl" id="ic4">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib4" style="background:var(--blue);width:0%;"></div></div></div>
        <div class="idx-card"><div class="idx-name">USD/KRW</div><div class="idx-price" id="ip5">—</div><div class="idx-change fl" id="ic5">—</div><div class="idx-bar-wrap"><div class="idx-bar" id="ib5" style="background:var(--gold);width:0%;"></div></div></div>
      </div>

      <!-- 계좌 잔고 스트립 -->
      <div class="cash-strip">
        <div class="cash-card" style="--cc-acc:var(--lime);">
          <div class="cash-label">KR 예수금</div>
          <div class="cash-val" id="cashKR" style="color:var(--lime)">—</div>
          <div class="cash-sub">주문가능금액</div>
        </div>
        <div class="cash-card" style="--cc-acc:var(--blue);">
          <div class="cash-label">US 예수금</div>
          <div class="cash-val" id="cashUS" style="color:var(--blue)">—</div>
          <div class="cash-sub">USD 주문가능</div>
        </div>
        <div class="cash-card" id="cashPnlCard" style="--cc-acc:var(--emerald);">
          <div class="cash-label">평가손익 합계</div>
          <div class="cash-val" id="cashPnl" style="color:var(--emerald)">—</div>
          <div class="cash-sub" id="cashPnlSub">KR 기준</div>
        </div>
        <div class="cash-card" style="--cc-acc:var(--gold);">
          <div class="cash-label">감시 종목</div>
          <div class="cash-val" id="cashWatch" style="color:var(--gold)">—</div>
          <div class="cash-sub" id="cashWatchSub">포지션 — / 실행 —</div>
        </div>
      </div>

      <!-- 실행 중인 전략 -->
      <div class="running-strip">
        <div class="rs-hd">
          <span style="width:5px;height:5px;border-radius:50%;background:var(--emerald);display:inline-block;animation:pulse-dot 1.6s ease-in-out infinite;flex-shrink:0;"></span>
          <span class="rs-title">실행 중인 전략</span>
          <span class="rs-count" id="rsCount">0</span>
        </div>
        <div id="rsList"><div class="rs-empty">자동매매 실행 중인 종목이 없습니다</div></div>
      </div>

      <!-- Navigation -->
      <div class="sec-div">
        <div class="div-ln"></div><span class="div-lbl">Navigation</span>
        <div class="div-ln" style="background:linear-gradient(270deg,var(--rim),transparent)"></div>
      </div>
      <div class="menu-grid">
        <a class="menu-card c-lime"    href="${pageContext.request.contextPath}/dashboard">
          <div class="card-body"><div class="card-icon">📊</div><div class="card-name">Dashboard</div><div class="card-desc">상태, 최근 주문, 실시간 가격 로그 종합 요약</div></div>
          <div class="card-foot"><span class="card-tag">Overview</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-emerald" href="${pageContext.request.contextPath}/control/kr">
          <div class="card-body"><div class="card-icon">⚡</div><div class="card-name">Auto Control</div><div class="card-desc">자동매매 엔진 시작·중지 및 실시간 상태 모니터링</div></div>
          <div class="card-foot"><span class="card-tag">Engine Control</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-gold"    href="${pageContext.request.contextPath}/history/orders">
          <div class="card-body"><div class="card-icon">📋</div><div class="card-name">Order History</div><div class="card-desc">주문·체결 이력 조회, 필터·정렬·KR/US 분리</div></div>
          <div class="card-foot"><span class="card-tag">Trade Records</span><span class="card-arrow">→ OPEN</span></div>
        </a>
        <a class="menu-card c-blue"    href="${pageContext.request.contextPath}/watchlist">
          <div class="card-body"><div class="card-icon">👁</div><div class="card-name">Watchlist</div><div class="card-desc">감시 종목 등록·삭제, KR/US 마켓 분리 관리</div></div>
          <div class="card-foot"><span class="card-tag">Symbol Monitor</span><span class="card-arrow">→ OPEN</span></div>
        </a>
      </div>

      <!-- Strategy Info -->
      <div class="sec-div">
        <div class="div-ln"></div><span class="div-lbl">Strategy Params</span>
        <div class="div-ln" style="background:linear-gradient(270deg,var(--rim),transparent)"></div>
      </div>
      <div class="strat-grid">
        <div class="strat-card">
          <div class="strat-head">
            <div class="strat-icon lime">📈</div>
            <div><div class="strat-name">Momentum Breakout</div><div class="strat-tag">MULTI-TREND VELOCITY</div></div>
          </div>
          <div class="param-row"><span class="param-key">Velocity window</span>     <span class="param-val">30–90s</span></div>
          <div class="param-row"><span class="param-key">Trend Short</span>          <span class="param-val">10–20s</span></div>
          <div class="param-row"><span class="param-key">Trend Mid</span>            <span class="param-val">20–40s</span></div>
          <div class="param-row"><span class="param-key">Trend Long</span>           <span class="param-val">40–80s</span></div>
          <div class="param-row"><span class="param-key">Min History Ticks</span>   <span class="param-val">36</span></div>
          <div class="param-row"><span class="param-key">History Span</span>         <span class="param-val">180s</span></div>
        </div>
        <div class="strat-card">
          <div class="strat-head">
            <div class="strat-icon gold">📉</div>
            <div><div class="strat-name">Risk Management</div><div class="strat-tag">STOP / TAKE PROFIT</div></div>
          </div>
          <div class="param-row"><span class="param-key">Stop Loss</span>           <span class="param-val">−1.1%</span></div>
          <div class="param-row"><span class="param-key">Take Profit (Partial)</span><span class="param-val">+1.8%</span></div>
          <div class="param-row"><span class="param-key">Take Profit (Final)</span> <span class="param-val">+2.8%</span></div>
          <div class="param-row"><span class="param-key">Trailing Stop</span>        <span class="param-val">−0.8% from High</span></div>
          <div class="param-row"><span class="param-key">Time Stop</span>            <span class="param-val">180s (< +0.3%)</span></div>
          <div class="param-row"><span class="param-key">Buy Cooldown</span>         <span class="param-val">60s</span></div>
        </div>
      </div>

      <div class="nav-footer">
        <a class="nav-btn primary" href="${pageContext.request.contextPath}/dashboard">Dashboard</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/control/kr">Control KR</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/control/us">Control US</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/monitor">Monitor</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/history/orders">Orders</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/balances">Balances</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/watchlist">Watchlist</a>
        <a class="nav-btn" href="${pageContext.request.contextPath}/backtest">Back-Test</a>
      </div>

    </div>
  </div>

  <!-- ════════════════ RIGHT SIDEBAR ════════════════ -->
  <aside class="sidebar">
    <div class="sb-hd">
      <div class="sb-hd-top">
        <div class="sb-title"><span class="sb-title-dot"></span>거래량 TOP 30</div>
        <div class="sb-btn-row">
          <button class="sb-icon-btn" id="sbRefBtn" onclick="loadRanking()" title="새로고침">
            <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
          </button>
        </div>
      </div>
      <div class="sb-hd-meta">
        <span class="sb-updated" id="sbUpdated">로딩 중…</span>
        <div class="sb-cd-wrap">
          <span class="sb-cd-lbl">갱신</span>
          <span class="sb-cd-num" id="sbCd">30</span>
          <span class="sb-cd-lbl">s</span>
        </div>
      </div>
    </div>
    <div class="sb-prog"><div class="sb-prog-fill" id="sbProg"></div></div>

    <!-- 마켓 선택 -->
    <div class="sb-mkt">
      <button class="sb-mkt-btn on-kr" id="smKR" onclick="setSbMkt('KR')">🇰🇷 국내</button>
      <button class="sb-mkt-btn"       id="smUS" onclick="setSbMkt('US')">🇺🇸 미국</button>
    </div>

    <!-- 탭 -->
    <div class="sb-tabs" id="sbTabs">
      <div class="sb-tab on" id="tab-vol" onclick="switchTab('vol')">거래량</div>
      <div class="sb-tab"    id="tab-chg" onclick="switchTab('chg')">등락률</div>
      <div class="sb-tab"    id="tab-hi"  onclick="switchTab('hi')">고가순</div>
    </div>

    <!-- US 거래소 선택 -->
    <div class="sb-exch" id="sbExch">
      <button class="exch-btn on" id="exNAS" onclick="setSbExch('NAS')">NASDAQ</button>
      <button class="exch-btn"    id="exNYS" onclick="setSbExch('NYS')">NYSE</button>
      <button class="exch-btn"    id="exAMS" onclick="setSbExch('AMS')">AMEX</button>
    </div>

    <div class="sb-body" id="sbBody">
      <div class="sb-msg loading"><div class="sb-msg-icon">◈</div><div class="sb-msg-txt">로딩 중…</div></div>
    </div>
    <div class="sb-foot" id="sbFoot">KIS · /api/market/ranking</div>
  </aside>
</div>

<script>
'use strict';
(function(){
  const BASE     = '${pageContext.request.contextPath}';
  const DAYS     = ['Sun','Mon','Tue','Wed','Thu','Fri','Sat'];
  const POLL_SEC = 30;
  const IDX_MAP  = [
    {id:'KOSPI', pi:'ip0', ci:'ic0', bi:'ib0', col:'var(--emerald)'},
    {id:'KOSDAQ',pi:'ip1', ci:'ic1', bi:'ib1', col:'var(--emerald)'},
    {id:'S&P 500',pi:'ip2',ci:'ic2',bi:'ib2', col:'var(--blue)'},
    {id:'NASDAQ', pi:'ip3', ci:'ic3', bi:'ib3', col:'var(--blue)'},
    {id:'DOW',    pi:'ip4', ci:'ic4', bi:'ib4', col:'var(--blue)'},
    {id:'USD/KRW',pi:'ip5', ci:'ic5', bi:'ib5', col:'var(--gold)'},
  ];

  function p2(v){return String(v).padStart(2,'0');}
  function esc(s){
    return String(s==null?'':s)
      .replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }
  function pn(v){return parseFloat(String(v||0).replace(/,/g,''))||0;}

  /* ── 시계 ── */
  (function tick(){
    const n=new Date();
    document.getElementById('clkTime').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds());
    document.getElementById('clkDate').textContent=n.getFullYear()+'.'+p2(n.getMonth()+1)+'.'+p2(n.getDate())+' '+DAYS[n.getDay()];
    setTimeout(tick,1000);
  })();

  /* ── 업타임 ── */
  let up=0;
  setInterval(()=>{
    up++;
    const h=Math.floor(up/3600),m=Math.floor((up%3600)/60),s=up%60;
    document.getElementById('wUptime').textContent=p2(h)+':'+p2(m)+':'+p2(s);
  },1000);

  /* ── 포맷 ── */
  const nfKR=new Intl.NumberFormat('ko-KR');
  function fmtKRW(v){
    const n=pn(v);
    if(Math.abs(n)>=1e8) return (n/1e8).toFixed(1)+'억';
    if(Math.abs(n)>=1e4) return (n/1e4).toFixed(0)+'만';
    return nfKR.format(Math.round(n));
  }

  /* ── Login ── */
  (function(){
    const form=document.getElementById('loginForm');
    const sb  =document.getElementById('loginStatus');
    const acc =document.getElementById('loginAccount');
    const lb  =document.getElementById('logoutBtn');
    const err =document.getElementById('loginError');
    const showIn =m=>{form.style.display='none';sb.style.display='inline-flex';err.style.display='none';acc.textContent=m||'****';};
    const showOut=()=>{sb.style.display='none';form.style.display='';err.style.display='none';};
    const showErr=m=>{err.textContent=m||'';err.style.display=m?'inline-flex':'none';};
    const post=(u,d)=>fetch(u,{method:'POST',headers:{'Content-Type':'application/x-www-form-urlencoded'},body:new URLSearchParams(d).toString()}).then(r=>r.json());
    fetch(BASE+'/api/auth/status').then(r=>r.json()).then(d=>d&&d.loggedIn?showIn(d.accountMasked):showOut()).catch(()=>showOut());
    form.addEventListener('submit',e=>{
      e.preventDefault();
      const no=(form.accountNo.value||'').trim(),pw=(form.accountPassword.value||'').trim();
      if(!no||!pw)return;
      if(!/^[\d\-]{5,20}$/.test(no)){showErr('계좌번호 형식 오류');return;}
      post(BASE+'/api/auth/login',{accountNo:no,accountPassword:pw})
        .then(d=>d.status==='OK'?showIn(d.accountMasked):showErr(d.message||'Login failed'))
        .catch(()=>showErr('서버 오류'));
    });
    lb.addEventListener('click',()=>post(BASE+'/api/auth/logout',{}).then(()=>showOut()));
  })();

  /* ── 대시보드 데이터 → 위젯 + 잔고 스트립 ── */
  function loadDashboard(){
    fetch(BASE+'/api/dashboard?limit=20')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d) return;
        const st=d.status||'STOPPED';
        document.getElementById('sysStatus').textContent=st;
        document.getElementById('wEngine').textContent=st;
        document.getElementById('wEngine').className='hw-val '+(st==='RUNNING'?'emerald':st==='STOPPED'?'red':'gold');
        document.getElementById('wWatch').textContent=d.watchlistCount||0;
        document.getElementById('wPos').textContent  =d.positionCount||0;
        document.getElementById('cashWatch').textContent=d.watchlistCount||0;
        document.getElementById('cashWatchSub').textContent=
          '포지션 '+(d.positionCount||0)+' / 실행 '+(d.runningCount||0);

        const ords=Array.isArray(d.recentOrders)?d.recentOrders:[];
        document.getElementById('wOrd').textContent=ords.length+'건';
      })
      .catch(()=>{});
  }

  /* ── 실행 중인 전략 ── */
  function loadRunning(){
    fetch(BASE+'/api/control/running')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d) return;
        const symbols=Array.isArray(d.symbols)?d.symbols:[];
        document.getElementById('wRun').textContent=symbols.length+'개';
        document.getElementById('rsCount').textContent=symbols.length;
        const list=document.getElementById('rsList');
        if(!symbols.length){
          list.innerHTML='<div class="rs-empty">자동매매 실행 중인 종목이 없습니다</div>';
          return;
        }
        list.innerHTML='<div class="rs-list">'+symbols.map(s=>{
          const sym=esc(s.symbol||s);
          const mkt=esc(s.market||'');
          return '<div class="rs-chip">'
            +'<div class="rs-chip-dot"></div>'
            +'<span class="rs-chip-sym">'+sym+'</span>'
            +(mkt?'<span class="rs-chip-mkt">'+mkt+'</span>':'')
            +'</div>';
        }).join('')+'</div>';
      })
      .catch(()=>{});
  }

  /* ── KR 예수금 ── */
  function loadCashKR(){
    fetch(BASE+'/api/account/cash/kr')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d||d.status!=='OK') return;
        const cash=pn(d.cash);
        document.getElementById('cashKR').textContent=fmtKRW(cash)+'원';
      })
      .catch(()=>{});
  }

  /* ── US 예수금 ── */
  function loadCashUS(){
    fetch(BASE+'/api/account/cash/us?currency=USD')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d||d.status!=='OK') return;
        const cash=pn(d.cash||d.data?.ord_psbl_frcr_amt);
        const nfUS=new Intl.NumberFormat('en-US',{minimumFractionDigits:2,maximumFractionDigits:2});
        document.getElementById('cashUS').textContent='$'+nfUS.format(cash);
      })
      .catch(()=>{});
  }

  /* ── KR 잔고 → 평가손익 ── */
  function loadBalKR(){
    fetch(BASE+'/api/account/balance/kr')
      .then(r=>r.ok?r.json():null)
      .then(d=>{
        if(!d||d.status!=='OK') return;
        const o2=Array.isArray(d.output2)?d.output2[0]:(d.output2||{});
        const pfls=pn(o2.evlu_pfls_amt_smtl||o2.evlu_pfls_amt);
        const pchsSmtl=pn(o2.pchs_amt_smtl)||1;
        const pct=(pfls/pchsSmtl*100);
        const pnlEl=document.getElementById('cashPnl');
        const col=pfls>=0?'var(--emerald)':'var(--red)';
        pnlEl.textContent=(pfls>=0?'+':'')+fmtKRW(pfls)+'원';
        pnlEl.style.color=col;
        document.getElementById('cashPnlSub').textContent='수익률 '+(pct>=0?'+':'')+pct.toFixed(2)+'%';
        document.getElementById('cashPnlCard').style.setProperty('--cc-acc',col);
      })
      .catch(()=>{});
  }

  /* ── 시장 지수 ── */
  function loadIdx(){
    fetch(BASE+'/api/market/index',{cache:'no-store'})
      .then(r=>r.ok?r.json():null)
      .then(json=>{
        if(!json||!Array.isArray(json.data)) return;
        const byName={};
        json.data.forEach(it=>{ if(it&&it.name) byName[it.name]=it; });
        IDX_MAP.forEach((m,i)=>{
          const it=byName[m.id];
          if(!it) return;
          const p=Number(it.price||0), ch=Number(it.change||0), pt=Number(it.point||0);
          document.getElementById(m.pi).textContent=p.toLocaleString('ko-KR',{maximumFractionDigits:2});
          const sign=ch>0?'+':'', cls=ch>0?'up':ch<0?'dn':'fl';
          document.getElementById(m.ci).textContent=sign+ch.toFixed(2)+'% ('+( pt>0?'+':'')+pt.toFixed(0)+')';
          document.getElementById(m.ci).className='idx-change '+cls;
          /* 지수 게이지: 변화율을 50%+ch% 로 표현 */
          const barPct=Math.min(100,Math.max(0,50+ch*5));
          const barEl=document.getElementById(m.bi);
          barEl.style.setProperty('--bw',barPct+'%');
          barEl.style.width=barPct+'%';
          barEl.style.background=m.col;
        });
      })
      .catch(()=>{});
  }

  /* ════════════════ 사이드바 로직 ════════════════ */
  let rawData    =[];
  let sbMkt      ='KR';
  let sbExch     ='NAS';
  let activeTab  ='vol';
  let prevCodes  =new Set();
  let cdRemain   =POLL_SEC;
  let cdTimer    =null;

  window.setSbMkt=function(m){
    sbMkt=m;
    document.getElementById('smKR').className='sb-mkt-btn'+(m==='KR'?' on-kr':'');
    document.getElementById('smUS').className='sb-mkt-btn'+(m==='US'?' on-us':'');
    document.getElementById('sbExch').classList.toggle('show',m==='US');
    document.getElementById('sbTabs').className='sb-tabs'+(m==='US'?' us':'');
    // 제목 색상 변경
    document.getElementById('sbFoot').textContent='KIS · /api/market/ranking?market='+m;
    loadRanking();
  };
  window.setSbExch=function(ex){
    const allowed=['NAS','NYS','AMS'];
    if(!allowed.includes(ex)) return;
    sbExch=ex;
    ['NAS','NYS','AMS'].forEach(x=>document.getElementById('ex'+x).classList.toggle('on',x===ex));
    loadRanking();
  };
  window.switchTab=function(t){
    activeTab=t;
    ['vol','chg','hi'].forEach(x=>document.getElementById('tab-'+x).classList.toggle('on',x===t));
    if(rawData.length) renderRank(rawData);
  };

  window.loadRanking=function(){
    const btn=document.getElementById('sbRefBtn');
    btn.classList.add('spinning');
    fetch(BASE+'/api/market/ranking?market='+encodeURIComponent(sbMkt)+'&exch='+encodeURIComponent(sbExch),{cache:'no-store'})
      .then(r=>{if(!r.ok)throw new Error('HTTP '+r.status);return r.json();})
      .then(json=>{
        let rows=[];
        if(Array.isArray(json.output))        rows=json.output;
        else if(Array.isArray(json.data))     rows=json.data;
        else if(json.data&&Array.isArray(json.data.output)) rows=json.data.output;
        else if(Array.isArray(json.items))    rows=json.items;
        rawData=rows.slice(0,30);
        renderRank(rawData);
        const n=new Date();
        document.getElementById('sbUpdated').textContent=p2(n.getHours())+':'+p2(n.getMinutes())+':'+p2(n.getSeconds())+' 갱신';
        restartCd();
      })
      .catch(err=>{
        document.getElementById('sbBody').innerHTML=
          '<div class="sb-msg error"><div class="sb-msg-icon">✕</div><div class="sb-msg-txt">'+esc(err.message)+'</div></div>';
      })
      .finally(()=>btn.classList.remove('spinning'));
  };

  function renderRank(data){
    const body=document.getElementById('sbBody');
    if(!data.length){
      body.innerHTML='<div class="sb-msg"><div class="sb-msg-icon">—</div><div class="sb-msg-txt">데이터 없음</div></div>';
      return;
    }
    const isUS=sbMkt==='US';
    const toNum=v=>{ const n=Number(String(v==null?'':v).replace(/,/g,'')); return Number.isFinite(n)?n:0; };
    const getCode =r=>String((r.mksc_shrn_iscd||r.symbol||'')).trim();
    const getName =r=>String(r.hts_kor_isnm||r.name||getCode(r)||'-').trim();
    const getPrice=r=>toNum(r.stck_prpr||r.price);
    const getRate =r=>toNum(r.prdy_ctrt||r.diff_rate);
    const getSign =r=>{
      const s=String(r.prdy_vrss_sign||r.diff_sign||'').trim();
      if(s) return s;
      const rt=getRate(r);
      return rt>0?'2':rt<0?'5':'3';
    };
    const getVol  =r=>toNum(r.acml_tr_pbmn??r.acml_vol??r.volume??r.vol??0);
    const getHigh =r=>toNum(r.stck_hgpr??r.high??r.stck_prpr??r.price??0);

    const sorted=[...data].sort((a,b)=>{
      if(activeTab==='chg') return Math.abs(getRate(b))-Math.abs(getRate(a));
      if(activeTab==='hi')  return getHigh(b)-getHigh(a);
      return getVol(b)-getVol(a);
    }).slice(0,30);

    const maxVol=Math.max(...sorted.map(getVol),1);
    const newSet=new Set(sorted.map(getCode));

    body.innerHTML='<div class="rank-list">'+sorted.map((r,idx)=>{
      const rank=idx+1;
      const rnCls=rank===1?'r1':rank===2?'r2':rank===3?'r3':'rN';
      const code=esc(getCode(r)), name=esc(getName(r));
      const price=getPrice(r).toLocaleString('ko-KR');
      const rate=getRate(r), sign=getSign(r);
      const isUp=sign==='1'||sign==='2', isDn=sign==='4'||sign==='5';
      let chgCls, pfx;
      if(isUS){ chgCls=isUp?'us-up':isDn?'us-dn':'flat'; pfx=isUp?'▲':isDn?'▼':'-'; }
      else     { chgCls=isUp?'kr-up':isDn?'kr-dn':'flat'; pfx=isUp?'▲':isDn?'▼':'-'; }
      const vol=getVol(r);
      const volPct=Math.max((vol/maxVol*100),2).toFixed(1);
      const volFmt=vol>=1e6?(vol/1e6).toFixed(1)+'M':vol>=1e3?(vol/1e3).toFixed(0)+'K':vol.toLocaleString();
      const isNew=!prevCodes.has(getCode(r));
      return '<div class="rank-row'+(isNew?' new-row':'')+'" style="animation-delay:'+(idx*16)+'ms">'
        +'<div class="rn '+rnCls+'">'+rank+'</div>'
        +'<div class="ri">'
        +'<div class="ri-name" title="'+name+'">'+name+'</div>'
        +'<div class="ri-code">'+code+'</div>'
        +'<div class="ri-bar-wrap"><div class="ri-bar '+(isUS?'us':'kr')+'" style="width:'+volPct+'%"></div>'
        +'<div class="ri-vol">'+esc(volFmt)+'</div></div>'
        +'</div>'
        +'<div class="rp"><div class="rp-price">'+esc(price)+'</div>'
        +'<div class="rp-chg '+chgCls+'">'+pfx+Math.abs(rate).toFixed(2)+'%</div></div>'
        +'</div>';
    }).join('')+'</div>';
    prevCodes=newSet;
  }

  function restartCd(){
    cdRemain=POLL_SEC; updProg();
    clearInterval(cdTimer);
    cdTimer=setInterval(()=>{
      cdRemain--;
      document.getElementById('sbCd').textContent=cdRemain;
      updProg();
      if(cdRemain<=0) loadRanking();
    },1000);
  }
  function updProg(){
    document.getElementById('sbProg').style.width=((POLL_SEC-cdRemain)/POLL_SEC*100)+'%';
  }

  /* ── 초기 로드 ── */
  loadDashboard();
  loadRunning();
  loadCashKR();
  loadCashUS();
  loadBalKR();
  loadIdx();
  loadRanking();

  /* ── 주기적 갱신 ── */
  setInterval(()=>{
    loadDashboard();
    loadRunning();
    loadIdx();
  },15000);
  setInterval(()=>{
    loadCashKR();
    loadCashUS();
    loadBalKR();
  },60000);
})();
</script>
</body>
</html>
