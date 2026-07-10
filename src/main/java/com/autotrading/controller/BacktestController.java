package com.autotrading.controller;

import com.autotrading.model.BacktestConfig;
import com.autotrading.service.BacktestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/backtest")
public class BacktestController {

    private final BacktestService backtestService;
    private final JdbcTemplate jdbcTemplate;

    public BacktestController(BacktestService backtestService, JdbcTemplate jdbcTemplate) {
        this.backtestService = backtestService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public String index() {
        return "backtest";
    }

    @PostMapping("/collectBars")
    @ResponseBody
    public Map<String, Object> collectBars(@RequestBody Map<String, String> req) {
        return backtestService.startCollect(
                req.getOrDefault("market",    "KRX"),
                req.getOrDefault("symbol",    "").trim().toUpperCase(),
                req.getOrDefault("startDate", ""),
                req.getOrDefault("endDate",   "")
        );
    }

    @GetMapping("/collectStatus/{jobId}")
    @ResponseBody
    public Map<String, Object> collectStatus(@PathVariable String jobId) {
        return backtestService.getCollectStatus(jobId);
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, Object> run(@RequestBody Map<String, String> req) {
        double buyAmount = 600_000.0;
        try { buyAmount = Double.parseDouble(req.getOrDefault("buyAmount", "600000")); }
        catch (NumberFormatException ignored) {}

        return backtestService.runBacktest(
                req.getOrDefault("market",    "KRX"),
                req.getOrDefault("symbol",    "").trim().toUpperCase(),
                req.getOrDefault("startDate", ""),
                req.getOrDefault("endDate",   ""),
                buyAmount,
                parseConfig(req)
        );
    }

    @PostMapping("/runBatch")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public Map<String, Object> runBatch(@RequestBody Map<String, Object> body) {
        List<String> symbols;
        try { symbols = (List<String>) body.get("symbols"); }
        catch (ClassCastException e) { symbols = null; }
        if (symbols == null || symbols.isEmpty()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status", "ERROR"); err.put("message", "종목 목록이 비어있습니다."); return err;
        }

        String market    = String.valueOf(body.getOrDefault("market",    "KRX"));
        String startDate = String.valueOf(body.getOrDefault("startDate", ""));
        String endDate   = String.valueOf(body.getOrDefault("endDate",   ""));
        double buyAmount = 600_000.0;
        try { buyAmount = Double.parseDouble(String.valueOf(body.getOrDefault("buyAmount", "600000"))); }
        catch (Exception ignored) {}

        Map<String, String> strReq = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : body.entrySet()) {
            if (e.getValue() != null) strReq.put(e.getKey(), e.getValue().toString());
        }
        BacktestConfig cfg = parseConfig(strReq);

        final String mkt = market, sd = startDate, ed = endDate;
        final double amt = buyAmount;
        final BacktestConfig finalCfg = cfg;

        List<CompletableFuture<Map<String, Object>>> futures = symbols.stream()
                .map(sym -> CompletableFuture.supplyAsync(() ->
                        backtestService.runBacktest(mkt, sym.trim().toUpperCase(), sd, ed, amt, finalCfg)))
                .collect(Collectors.toList());

        List<Map<String, Object>> results = new ArrayList<>();
        List<Map<String, Object>> errors  = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                Map<String, Object> r = futures.get(i).get();
                if ("OK".equals(r.get("status"))) results.add(r);
                else {
                    Map<String, Object> err = new LinkedHashMap<>();
                    err.put("symbol", symbols.get(i));
                    err.put("message", r.getOrDefault("message", "오류"));
                    errors.add(err);
                }
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("symbol", symbols.get(i));
                err.put("message", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "OK"); resp.put("results", results); resp.put("errors", errors);

        if (!results.isEmpty()) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO tb_backtest_history (market, symbols, start_date, end_date, buy_amount) VALUES (?,?,?,?,?)",
                    mkt, String.join(",", symbols), sd, ed, (long) amt
                );
            } catch (Exception ignored) {}
        }
        return resp;
    }

    @GetMapping("/history")
    @ResponseBody
    public List<Map<String, Object>> getHistory() {
        try {
            return jdbcTemplate.queryForList(
                "SELECT id, market, symbols, start_date, end_date, buy_amount, created_at " +
                "FROM tb_backtest_history ORDER BY created_at DESC LIMIT 20"
            );
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * 백테스트 결과를 AI 분석용 텍스트 프롬프트로 내보냅니다.
     * JSP의 lastResults 배열 전체를 받아 서버에서 포맷 후 .txt 파일로 반환합니다.
     */
    @PostMapping("/exportPrompt")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public ResponseEntity<byte[]> exportPrompt(@RequestBody Map<String, Object> body) {
        Map<String, Object> params  = body.containsKey("params")
                ? (Map<String, Object>) body.get("params") : Collections.emptyMap();
        List<Map<String, Object>> results = body.containsKey("results")
                ? (List<Map<String, Object>>) body.get("results") : Collections.emptyList();
        if (results.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] bytes = buildAiPrompt(params, results).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"backtest_ai_prompt.txt\"")
                .header("Content-Type", "text/plain; charset=utf-8")
                .body(bytes);
    }

    @SuppressWarnings("unchecked")
    private String buildAiPrompt(Map<String, Object> params, List<Map<String, Object>> results) {
        StringBuilder sb = new StringBuilder();
        String SEP  = "═".repeat(70);
        String SEP2 = "─".repeat(70);

        sb.append("당신은 주식 단기 트레이딩 전략 분석 전문가입니다.\n");
        sb.append("아래는 1분봉 기반 자동매매 전략(BREAKOUT / PULLBACK / STRONG_PULLBACK / VWAP_RECLAIM / VWAP_RECLAIM_V2 / RSI_BOLLINGER_REBOUND / OPENING_RANGE_BREAKOUT / THIRTY_MIN_RSI_BB_CROSS / RED_TO_GREEN)의\n");
        sb.append("백테스트 결과와 전략 파라미터 전체입니다. 결과를 분석하고 전략의 강점·약점·개선 방향을 제시해 주세요.\n\n");

        // ── 기본 정보 ──────────────────────────────────────────
        sb.append(SEP).append("\n[ 백테스트 기본 정보 ]\n").append(SEP).append("\n");
        String market    = str(params, "market",    str(results.get(0), "market",    "KRX"));
        String startDate = str(params, "startDate", str(results.get(0), "startDate", "-"));
        String endDate   = str(params, "endDate",   str(results.get(0), "endDate",   "-"));
        String buyAmtRaw = str(params, "buyAmount", "");
        double buyAmt    = results.get(0).containsKey("buyAmount")
                ? toDouble(results.get(0).get("buyAmount")) : 600_000.0;

        List<String> symbols = new ArrayList<>();
        for (Map<String, Object> r : results) symbols.add(str(r, "symbol", "?"));

        String entryEndH = str(params, "entryEndHour",   "10");
        String entryEndM = str(params, "entryEndMinute", "30");
        if (entryEndM.length() == 1) entryEndM = "0" + entryEndM;

        sb.append(String.format("시장      : %s%n", market));
        sb.append(String.format("종목      : %s%n", String.join(", ", symbols)));
        sb.append(String.format("기간      : %s ~ %s%n", startDate, endDate));
        sb.append(String.format("주문금액  : %s원 / 건%n",
                buyAmtRaw.isEmpty() ? fmt((long) buyAmt) : buyAmtRaw));

        // ── 종목별 요약 ────────────────────────────────────────
        sb.append("\n").append(SEP).append("\n[ 종목별 요약 ]\n").append(SEP).append("\n");
        sb.append(String.format("%-8s | %5s | %6s | %8s | %8s | %5s | %6s%n",
                "종목", "거래수", "승률", "평균손익", "누적손익", "PF", "MDD"));
        sb.append(SEP2).append("\n");
        for (Map<String, Object> r : results) {
            int    tot = toInt(r.get("totalTrades"));
            double wr0 = toDouble(r.get("winRate")) * 100;
            double avg0= toDouble(r.get("avgPnlPct")) * 100;
            double cum0= toDouble(r.get("cumulativePnlPct")) * 100;
            double pf0 = toDouble(r.get("profitFactor"));
            double mdd0= toDouble(r.get("maxDrawdown")) * 100;
            sb.append(String.format("%-8s | %5d | %5.1f%% | %+7.2f%% | %+7.2f%% | %5.2f | %5.1f%%%n",
                    str(r, "symbol", "?"), tot, wr0, avg0, cum0, pf0, mdd0));
        }

        // ── 전체 종합 성과 ──────────────────────────────────────
        int   totTrades = 0, totWins = 0;
        double sumPnl = 0, sumWin = 0, sumLoss = 0, cumPnl = 1.0, peak = 1.0, maxDD = 0.0;
        int maxStreak = 0, maxLoss = 0, curS = 0, curL = 0;
        List<Map<String, Object>> allTrades = new ArrayList<>();

        for (Map<String, Object> r : results) {
            totTrades += toInt(r.get("totalTrades"));
            totWins   += toInt(r.get("wins"));
            List<Map<String, Object>> tr = (List<Map<String, Object>>) r.get("trades");
            if (tr != null) allTrades.addAll(tr);
        }
        allTrades.sort(Comparator.comparing(t -> str(t, "entryTime", "")));
        for (Map<String, Object> t : allTrades) {
            double p = toDouble(t.get("pnlPct"));
            sumPnl += p;
            if (p > 0) { sumWin += p;  curS++; curL = 0; maxStreak = Math.max(maxStreak, curS); }
            else        { sumLoss += Math.abs(p); curL++; curS = 0; maxLoss = Math.max(maxLoss, curL); }
            cumPnl *= (1 + p);
            if (cumPnl > peak) peak = cumPnl;
            maxDD = Math.max(maxDD, (peak - cumPnl) / peak);
        }
        int totLoss = totTrades - totWins;
        double wr  = totTrades > 0 ? (double) totWins / totTrades : 0;
        double avg = totTrades > 0 ? sumPnl / totTrades : 0;
        double pf  = sumLoss > 0 ? sumWin / sumLoss : (sumWin > 0 ? 9999 : 0);
        double avgW = totWins > 0 ? sumWin / totWins : 0;
        double avgL = totLoss > 0 ? sumLoss / totLoss : 0;
        double rr  = avgL > 0 ? avgW / avgL : 0;
        double exp = wr * avgW - (1 - wr) * avgL;
        double netPnl = allTrades.stream().mapToDouble(t -> toDouble(t.get("pnlPct")) * buyAmt).sum();

        sb.append("\n").append(SEP).append("\n[ 전체 종합 성과 ]\n").append(SEP).append("\n");
        sb.append(String.format("총 거래수    : %d건  (승리 %d / 패배 %d)%n", totTrades, totWins, totLoss));
        sb.append(String.format("승률         : %.1f%%%n", wr * 100));
        sb.append(String.format("기대값       : %+.2f%%%n", exp * 100));
        sb.append(String.format("평균 손익    : %+.2f%%%n", avg * 100));
        sb.append(String.format("누적 손익    : %+.2f%%%n", (cumPnl - 1.0) * 100));
        sb.append(String.format("Max Drawdown : -%.2f%%%n", maxDD * 100));
        sb.append(String.format("Profit Factor: %.2f%n", pf));
        sb.append(String.format("순 손익금    : %+,d원%n", (long) netPnl));
        sb.append(String.format("평균 승리    : +%.2f%%  /  평균 손실: -%.2f%%%n", avgW * 100, avgL * 100));
        sb.append(String.format("손익비 (R:R) : %.2fx%n", rr));
        sb.append(String.format("최대 연속 승 : %d연승  /  최대 연속 패: %d연패%n", maxStreak, maxLoss));

        // ── 모드·청산사유·Score·Grade 통계 ───────────────────────
        appendGroupStats(sb, SEP, SEP2, "모드별 성과",     allTrades, "entryMode");
        appendGroupStats(sb, SEP, SEP2, "청산 사유별 성과", allTrades, "exitReason");
        appendScoreStats(sb, SEP, SEP2, allTrades);
        appendGradeStats(sb, SEP, SEP2, allTrades);
        appendMonthlyStats(sb, SEP, SEP2, allTrades);
        appendTimeSlotStats(sb, SEP, SEP2, allTrades);
        appendHoldTimeStats(sb, SEP, SEP2, allTrades);

        // ── 전략 파라미터 전체 ──────────────────────────────────
        sb.append("\n").append(SEP).append("\n[ 전략 파라미터 전체 (이번 백테스트 설정값) ]\n").append(SEP).append("\n");

        // 공통 진입 필터
        sb.append("\n▶ 공통 진입 필터\n").append(SEP2).append("\n");
        sb.append(pLine("최소 히스토리 봉수",     params, "minHistoryBars",      "30봉"));
        sb.append(pLine("최소 히스토리 시간",     params, "minHistoryMinutes",   "30분"));
        sb.append(pLine("최소 현재가",            params, "minPrice",            "1,000원"));
        sb.append(pLine("VWAP 이격 하드리밋",     params, "vwapHardLimitPct",    "8.0%  (이 % 초과 이격 시 전 모드 진입 차단)"));
        sb.append(pLine("현재봉 거래대금(KRX)",   params, "minTurnoverKrx",      "50,000,000원"));
        sb.append(pLine("20봉 평균 거래대금(KRX)",params, "minAvgTurnoverKrx",   "30,000,000원"));
        sb.append(pLine("거래대금 하한(US)",       params, "minTurnoverUs",       "10,000USD"));
        sb.append(pLine("Market Filter",          params, "useMarketFilter",     "true  (시장 약세 시 전 모드 차단)"));
        sb.append(pLine("매수 쿨다운",            params, "buyCooldownSec",      "60초  (같은 종목 재진입 대기)"));
        sb.append(pLine("일일 최대 진입 수",       params, "maxDailyEntryCount",  "2건"));
        sb.append(pLine("같은 패턴 최대 진입",     params, "maxSamePatternEntry", "1건"));
        sb.append(String.format("  %-30s: %s:%s%n", "진입 마감 시각 (KRX)", entryEndH, entryEndM));
        sb.append(pLine("느린모드 마감 (SP/RSI_BB/VR2/30M)", params, "slowModeEntryEndHour", "13:00  (← 12: 12시 차단 역효과 확인 → 복원)"));
        sb.append(pLine("일일 최대 손실 한도",     params, "maxDailyLossPct",    "3.0%  (초과 시 당일 신규 진입 전면 차단)"));
        sb.append(pLine("일일 수익 목표 한도",     params, "maxDailyProfitPct",  "5.0%  (달성 시 당일 신규 진입 차단)"));

        // 진입 등급 필터
        sb.append("\n▶ 진입 등급 필터\n").append(SEP2).append("\n");
        sb.append(pLine("SS등급(95~100) 차단",     params, "blockSSGrade", "true  (과열 구간)"));
        sb.append(pLine("S등급(90~94) 차단",      params, "blockSGrade",  "true  (기본 차단)"));
        sb.append(pLine("A등급(85~89) 차단",      params, "blockAGrade",  "false (허용 — 기본 타겟)"));
        sb.append(pLine("B등급(80~84) 차단",      params, "blockBGrade",  "false (허용 — 83점 이상 VR 허용)"));

        // PULLBACK 진입
        sb.append("\n▶ PULLBACK 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",              params, "enablePullback",                  "false  (PF 0.06 → 비활성)"));
        sb.append(pLine("최소 점수(0~100)",        params, "pullbackMinScore",                "80"));
        sb.append(pLine("VWAP 최대 이격",          params, "vwapMaxGapPullbackPct",           "1.0%"));
        sb.append(pLine("고점 대비 눌림 상한",     params, "pullbackUpperPct",                "1.0%  (고점 대비 ±% 범위 상단)"));
        sb.append(pLine("고점 대비 눌림 하한",     params, "pullbackLowerPct",                "2.0%  (고점 대비 ±% 범위 하단)"));
        sb.append(pLine("거래량 배수",             params, "pullbackVolumeMult",              "1.0x  (5봉 평균 대비)"));
        sb.append(pLine("단기 속도 하한",          params, "pullbackVelocityShort",           "0.0015  (3봉 평균 가격 변화율)"));
        sb.append(pLine("중기 속도 하한",          params, "pullbackVelocityMid",             "0.0  (10봉 평균 가격 변화율)"));
        sb.append(pLine("필요 상승봉 수",          params, "pullbackRequiredBullishBars",     "1봉"));
        sb.append(pLine("VWAP 위 필수",            params, "pullbackRequireAboveVwap",        "true"));
        sb.append(pLine("VWAP 기울기 상승 필수",   params, "pullbackRequireVwapSlope",        "true"));
        sb.append(pLine("최근 고점 돌파 필수",     params, "pullbackRequireRecentHighBreakout","false"));

        // PULLBACK 청산
        sb.append("\n▶ PULLBACK 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                    params, "pullbackStopPct",   "2.3%  (진입가 대비)"));
        sb.append(pLine("고정 익절",               params, "pullbackTpPct",     "3.2%  (진입가 대비, 0=비활성)"));
        sb.append(pLine("트레일 시작 수익",         params, "pullbackTrailSt",   "2.2%  (이 수익 도달 시 트레일링 활성)"));
        sb.append(pLine("트레일 고점 대비 하락",    params, "pullbackTrailDrop", "1.6%  (트레일 고점 대비 이 % 하락 시 청산)"));

        // BREAKOUT 진입
        sb.append("\n▶ BREAKOUT 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",              params, "enableBreakout",               "true"));
        sb.append(pLine("최소 점수(0~100)",        params, "breakoutMinScore",             "85"));
        sb.append(pLine("VWAP 최대 이격",          params, "vwapMaxGapBreakoutPct",        "1.5%"));
        sb.append(pLine("리테스트 하한",           params, "breakoutRetestLower",          "1.0%  (전고 대비 눌림 허용 하한)"));
        sb.append(pLine("리테스트 상한",           params, "breakoutRetestUpper",          "0.1%  (전고 대비 돌파 초과 상한)"));
        sb.append(pLine("강한 거래량 배수",        params, "breakoutStrongVolMult",        "2.0x  (5봉 평균 대비)"));
        sb.append(pLine("공통 거래량 배수",        params, "volumeMult",                   "1.5x"));
        sb.append(pLine("가속도 조건 필수",        params, "breakoutRequireAcceleration",  "true  (단기속도 > 중기속도)"));
        sb.append(pLine("MA 3중 상승 필수",        params, "breakoutRequireMultiUptrend",  "true  (false면 2개 이상)"));
        sb.append(pLine("과열 차단",              params, "breakoutOverheatBlock",         "true"));
        sb.append(pLine("중기 속도 하한",          params, "breakoutMinVelocityMid",       "0.0008"));
        sb.append(pLine("장기 속도 하한",          params, "breakoutMinVelocityLong",      "0.0005"));
        sb.append(pLine("필요 상승봉 수",          params, "breakoutRequiredBullishBars",  "2봉"));

        // BREAKOUT 청산
        sb.append("\n▶ BREAKOUT 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                    params, "breakoutStopPct",   "1.5%"));
        sb.append(pLine("고정 익절",               params, "breakoutTpPct",     "2.0%"));
        sb.append(pLine("트레일 시작 수익",         params, "breakoutTrailSt",   "1.5%"));
        sb.append(pLine("트레일 고점 대비 하락",    params, "breakoutTrailDrop", "1.2%"));

        // STRONG_PULLBACK 진입
        sb.append("\n▶ STRONG_PULLBACK 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",              params, "enableStrongPullback", "true"));
        sb.append(pLine("최근 고점 대비 눌림 최소",params, "spPullbackMinPct",    "0.5%"));
        sb.append(pLine("최근 고점 대비 눌림 최대",params, "spPullbackMaxPct",    "3.8%"));
        sb.append(pLine("VWAP 위 최소 위치",       params, "spVwapMinAbovePct",   "0.2%  (현재가가 VWAP 대비 최소 이 % 위)"));
        sb.append(pLine("3봉 vs 10봉 거래량 비율", params, "spVol3RatioMax",      "0.85  (3봉 평균 < 10봉 평균 × 이 값 → 거래량 감소 확인)"));
        sb.append(pLine("양봉 몸통 비율 최소",     params, "spBodyRatioMin",      "0.4  (몸통/전체 캔들 크기)"));
        sb.append(pLine("최소 점수",              params, "spMinScore",           "72"));

        // STRONG_PULLBACK 청산
        sb.append("\n▶ STRONG_PULLBACK 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                    params, "spStopPct",   "1.8%"));
        sb.append(pLine("고정 익절",               params, "spTpPct",     "3.0%"));
        sb.append(pLine("트레일 시작 수익",         params, "spTrailSt",   "2.0%"));
        sb.append(pLine("트레일 고점 대비 하락",    params, "spTrailDrop", "0.8%"));

        // VWAP_RECLAIM 진입
        sb.append("\n▶ VWAP_RECLAIM 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",              params, "enableVwapReclaim",  "true"));
        sb.append(pLine("VWAP 이탈 확인 봉수",    params, "vrLookbackBars",     "12봉  (최근 N봉 내 VWAP 아래 봉 존재 확인)"));
        sb.append(pLine("거래량 배수",             params, "vrVolMult",          "2.0x  (5봉 평균 대비)"));
        sb.append(pLine("연속 VWAP 위 봉 수",     params, "vrMinAboveVwapBars", "5봉  (회복 후 연속 N봉 이상 VWAP 위)"));
        sb.append(pLine("최소 점수",              params, "vrMinScore",          "83  (← 87)"));
        sb.append(pLine("최대 점수",              params, "vrMaxScore",          "90  (← 88: 범위 83~90으로 확장)"));

        // VWAP_RECLAIM 청산
        sb.append("\n▶ VWAP_RECLAIM 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                    params, "vrStopPct",   "1.5%  (← 1.0%)"));
        sb.append(pLine("고정 익절",               params, "vrTpPct",     "2.2%"));
        sb.append(pLine("트레일 시작 수익",         params, "vrTrailSt",   "1.8%"));
        sb.append(pLine("트레일 고점 대비 하락",    params, "vrTrailDrop", "1.0%"));

        // RSI_BOLLINGER_REBOUND 진입
        sb.append("\n▶ RSI_BOLLINGER_REBOUND 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",             params, "enableRsiBbRebound",       "false  (비활성 — PF 0.74 손실 확인)"));
        sb.append(pLine("RSI 기간",             params, "rsiBbRsiPeriod",           "14"));
        sb.append(pLine("RSI Signal 기간",      params, "rsiBbSignalPeriod",        "9"));
        sb.append(pLine("BB 기간",              params, "rsiBbPeriod",              "20"));
        sb.append(pLine("BB 표준편차 배수",      params, "rsiBbStdMult",             "2.0"));
        sb.append(pLine("BB 하단 터치 버퍼 %",  params, "rsiBbLowerTouchBufferPct", "0.2%  (5분봉 low ≤ BB하단×(1+버퍼))"));
        sb.append(pLine("최대 하방 이탈 %",     params, "rsiBbMaxBreakdownPct",     "0.5%  (BB하단 이 % 초과 이탈 시 차단)"));
        sb.append(pLine("VWAP 대비 최소 %",     params, "rsiBbMinVwapPct",          "-0.5%  (VWAP 대비 이 % 이상 위치)"));
        sb.append(pLine("RSI 저점 기준 ≤",      params, "rsiBbRsiLowThreshold",     "40.0  (← 32.0: 0건 발생 → 추가 완화)"));
        sb.append(pLine("최소 점수",            params, "rsiBbMinScore",            "80"));

        // RSI_BOLLINGER_REBOUND 청산
        sb.append("\n▶ RSI_BOLLINGER_REBOUND 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                  params, "rsiBbStopPct",   "1.5%"));
        sb.append(pLine("고정 익절",             params, "rsiBbTpPct",     "0.0%  (트레일만 사용)"));
        sb.append(pLine("트레일 시작 수익",       params, "rsiBbTrailSt",   "2.0%"));
        sb.append(pLine("트레일 고점 대비 하락",  params, "rsiBbTrailDrop", "0.6%"));

        // OPENING_RANGE_BREAKOUT 진입
        sb.append("\n▶ OPENING_RANGE_BREAKOUT (ORB) 진입 조건\n").append(SEP2).append("\n");
        sb.append(pLine("활성 여부",          params, "enableOpeningRangeBreakout", "true  (ORB PF 2.94 확인 → 활성화)"));
        sb.append("  ORB 개념: 09:00~09:10 고가 기록 → 09:30~10:30 사이 고가+0.1% 돌파 + VWAP위 + 거래량2배 + 추세↑ 진입 (← 09:15: 장 초반 변동성 제외)\n");

        // OPENING_RANGE_BREAKOUT 청산
        sb.append("\n▶ OPENING_RANGE_BREAKOUT 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("손절",                  params, "orbStopPct",   "1.3%  (← 1.8%)"));
        sb.append(pLine("고정 익절",             params, "orbTpPct",     "2.0%  (← 2.2%)"));
        sb.append(pLine("트레일 시작 수익",       params, "orbTrailSt",   "1.5%  (← 1.8%)"));
        sb.append(pLine("트레일 고점 대비 하락",  params, "orbTrailDrop", "0.8%"));

        // 공통 청산 조건
        sb.append("\n▶ 공통 청산 조건\n").append(SEP2).append("\n");
        sb.append(pLine("긴급 손절",              params, "emergencyStopPct",  "3.0%  (모든 조건 무시, 즉시 청산)"));
        sb.append(pLine("VWAP Break 청산",        params, "useVwapBreak",      "true"));
        sb.append(pLine("VWAP Break 버퍼",        params, "vwapBreakBuffer",   "0.5%  (VWAP 아래 이 % 초과 이탈 시 청산)"));
        sb.append(pLine("VWAP Break 유예시간",    params, "vwapBreakGraceSec", "300초  (진입 후 N초 이내 VWAP Break 무시)"));
        sb.append(pLine("Breakeven Guard",        params, "useBreakevenGuard", "true"));
        sb.append(pLine("Breakeven 고점 기준",    params, "breakevenPeak",     "1.5%  (이 수익 도달 후 Guard 활성)"));
        sb.append(pLine("Breakeven 손실 한도",    params, "breakevenLoss",     "-0.1%  (Guard 활성 후 이 손실 시 청산)"));
        sb.append(pLine("Failed Breakout 청산",   params, "useFailedBreakout", "true"));
        sb.append(pLine("Failed Pullback 청산",   params, "useFailedPullback", "true"));
        sb.append(pLine("EOD 강제 청산",          params, "useEodForceSell",   "true  (KRX 15:25 이후 강제 청산)"));

        // 비용
        sb.append("\n▶ 비용 파라미터\n").append(SEP2).append("\n");
        sb.append(pLine("슬리피지 (편도)",        params, "slippagePct", "0.0%"));
        sb.append(pLine("수수료 (왕복 합산)",     params, "feePct",      "0.015%"));
        sb.append(pLine("증권거래세",             params, "taxPct",      "0.18%  (KRX 전용)"));

        // ── 거래 내역 ──────────────────────────────────────────
        sb.append("\n").append(SEP).append("\n");
        sb.append(String.format("[ 전체 거래 내역 (%d건) ]\n", allTrades.size()));
        sb.append(SEP).append("\n");
        sb.append(String.format("%-3s | %-6s | %-16s | %-16s | %-16s | %8s | %8s | %7s | %3s | %2s | %-22s | %s%n",
                "#", "종목", "진입일시", "청산일시", "모드",
                "진입가", "청산가", "수익률", "Sc", "등급", "청산사유", "보유"));
        sb.append(SEP2).append("\n");
        int idx = 1;
        for (Map<String, Object> t : allTrades) {
            String entryT = str(t, "entryTime",  "").replace("T", " ");
            String exitT  = str(t, "exitTime",   "").replace("T", " ");
            if (entryT.length() > 16) entryT = entryT.substring(0, 16);
            if (exitT.length()  > 16) exitT  = exitT.substring(0, 16);
            long holdMin = toInt(t.get("holdSeconds")) / 60;
            sb.append(String.format("%-3d | %-6s | %-16s | %-16s | %-16s | %8s | %8s | %+6.2f%% | %3d | %2s | %-22s | %d분%n",
                    idx++,
                    str(t, "symbol", ""),
                    entryT, exitT,
                    str(t, "entryMode", ""),
                    fmt((long) toDouble(t.get("entryPrice"))),
                    fmt((long) toDouble(t.get("exitPrice"))),
                    toDouble(t.get("pnlPct")) * 100,
                    toInt(t.get("signalScore")),
                    str(t, "signalGrade", ""),
                    str(t, "exitReason", ""),
                    holdMin));
        }

        // ── 분석 요청 ──────────────────────────────────────────
        sb.append("\n").append(SEP).append("\n[ 분석 요청 ]\n").append(SEP).append("\n");
        sb.append("위 백테스트 결과와 전략 파라미터를 바탕으로 아래 항목을 심층 분석해 주세요:\n\n");
        sb.append("1. 전략 종합 진단\n");
        sb.append("   - 승률 " + String.format("%.1f%%", wr * 100) + ", PF " + String.format("%.2f", pf)
                + ", 기대값 " + String.format("%+.2f%%", exp * 100) + " 에 대한 종합 평가\n");
        sb.append("   - 최대 " + maxLoss + "연패 발생 구간 분석 및 리스크 요소\n\n");
        sb.append("2. 모드별 성과 분석\n");
        sb.append("   - BREAKOUT / PULLBACK / STRONG_PULLBACK / VWAP_RECLAIM / RSI_BOLLINGER_REBOUND / OPENING_RANGE_BREAKOUT 각 모드의 강약점\n");
        sb.append("   - 비활성화하거나 조건을 강화해야 할 모드 추천\n\n");
        sb.append("3. 진입 조건 최적화\n");
        sb.append("   - 현재 최소 점수(BREAKOUT=" + str(params, "breakoutMinScore", "85")
                + ", PULLBACK=" + str(params, "pullbackMinScore", "80")
                + ", SP=" + str(params, "spMinScore", "72")
                + ", VR=" + str(params, "vrMinScore", "83")
                + ", RSI_BB=" + str(params, "rsiBbMinScore", "80") + ")가 적절한지 검토\n");
        sb.append("   - S등급(90~94) 차단: " + str(params, "blockSGrade", "true")
                + " / A등급(85~89) 차단: " + str(params, "blockAGrade", "false") + " 설정 재검토\n");
        sb.append("   - VWAP 이격 기준(PB=" + str(params, "vwapMaxGapPullbackPct", "1.0")
                + "%, BO=" + str(params, "vwapMaxGapBreakoutPct", "1.5") + "%)이 적절한지\n");
        sb.append("   - 속도(velocity) 조건 조정 필요 여부\n\n");
        sb.append("4. 청산 조건 최적화\n");
        sb.append("   - 모드별 손절%·익절%·트레일 파라미터 최적화 제안\n");
        sb.append("   - BREAKOUT: 손절=" + str(params, "breakoutStopPct", "1.5")
                + "%, 익절=" + str(params, "breakoutTpPct", "2.0")
                + "%, 트레일시작=" + str(params, "breakoutTrailSt", "1.5")
                + "%, 트레일하락=" + str(params, "breakoutTrailDrop", "1.2") + "%\n");
        sb.append("   - PULLBACK: 손절=" + str(params, "pullbackStopPct", "2.3")
                + "%, 익절=" + str(params, "pullbackTpPct", "3.2")
                + "%, 트레일시작=" + str(params, "pullbackTrailSt", "2.2")
                + "%, 트레일하락=" + str(params, "pullbackTrailDrop", "1.6") + "%\n");
        sb.append("   - SP: 손절=" + str(params, "spStopPct", "1.8")
                + "%, 익절=" + str(params, "spTpPct", "3.0")
                + "%, 트레일시작=" + str(params, "spTrailSt", "2.0")
                + "%, 트레일하락=" + str(params, "spTrailDrop", "0.8") + "%\n");
        sb.append("   - VR: 손절=" + str(params, "vrStopPct", "1.5")
                + "%, 익절=" + str(params, "vrTpPct", "2.2")
                + "%, 트레일시작=" + str(params, "vrTrailSt", "1.8")
                + "%, 트레일하락=" + str(params, "vrTrailDrop", "1.0") + "%\n");
        sb.append("   - RSI_BB: 손절=" + str(params, "rsiBbStopPct", "1.5")
                + "%, 익절=" + str(params, "rsiBbTpPct", "0.0")
                + "%, 트레일시작=" + str(params, "rsiBbTrailSt", "2.0")
                + "%, 트레일하락=" + str(params, "rsiBbTrailDrop", "0.6") + "%\n");
        sb.append("   - ORB: 손절=" + str(params, "orbStopPct", "1.3")
                + "%, 익절=" + str(params, "orbTpPct", "2.0")
                + "%, 트레일시작=" + str(params, "orbTrailSt", "1.5")
                + "%, 트레일하락=" + str(params, "orbTrailDrop", "0.8") + "%\n");
        sb.append("   - 긴급손절=" + str(params, "emergencyStopPct", "3.0")
                + "%, VWAP Break 버퍼=" + str(params, "vwapBreakBuffer", "0.5")
                + "%, 유예=" + str(params, "vwapBreakGraceSec", "300") + "초 검토\n\n");
        sb.append("5. 수익·손실 거래 패턴 차이\n");
        sb.append("   - 진입 시간대(장 초반 vs 중반 vs 후반) 별 성과\n");
        sb.append("   - 보유 시간과 수익률의 상관관계\n");
        sb.append("   - Score 분포별 패턴 (등급별 성과 테이블 참고)\n\n");
        sb.append("6. 청산 사유 분석\n");
        sb.append("   - STOPLOSS 비중이 높다면 원인과 손절폭 조정 방안\n");
        sb.append("   - TRAIL_STOP vs TAKE_PROFIT 비중 분석\n");
        sb.append("   - VWAP_BREAK / FAILED_BREAKOUT / FAILED_PULLBACK 청산의 실효성\n\n");
        sb.append("7. 파라미터 개선 제안 (구체적 수치 포함)\n");
        sb.append("   - 각 파라미터에 대해 현재값 → 권장값 형태로 제안\n");
        sb.append("   - 수익성 개선을 위한 최우선 조정 항목 3가지\n\n");
        sb.append("8. 진입 시간 창 최적화\n");
        sb.append("   - 현재 진입 마감: " + entryEndH + ":" + entryEndM
                + " (entryEndHour=" + entryEndH + ", entryEndMinute=" + entryEndM + ")\n");
        sb.append("   - [진입 시간대별 성과] 테이블 기반으로 각 시간대의 손익 특성 분석\n");
        sb.append("   - 13:00 이후 진입 중 EOD_FORCE_SELL로 청산되는 거래의 비율·손익 분석\n");
        sb.append("   - 진입 마감을 13:00으로 단축 시 예상 효과: 피할 수 있는 손실 vs 놓치는 수익\n\n");
        sb.append("9. 시장 국면 대응 및 연속 손실 방어\n");
        sb.append("   - [월별 성과] 테이블에서 손실 집중 구간과 원인 분석\n");
        sb.append("   - useMarketFilter=true 설정에도 " + maxLoss + "연패 발생한 구간 및 필터 미작동 원인 추론\n");
        sb.append("   - 연속 손실 발생 시 자동 대응 강화 방안 (일일 진입 제한, 손실 이후 쿨다운 등)\n");

        return sb.toString();
    }

    /** 파라미터 한 줄 포맷: 설명(좌) + 실제설정값 또는 기본값 */
    private String pLine(String label, Map<String, Object> params, String key, String defaultNote) {
        String v = params.containsKey(key) ? String.valueOf(params.get(key)) : "";
        String display = (v.isEmpty() || v.equals("null")) ? ("(" + defaultNote + ")") : v;
        return String.format("  %-30s: %s%n", label, display);
    }

    private void appendGroupStats(StringBuilder sb, String sep, String sep2,
                                   String title, List<Map<String, Object>> trades, String key) {
        Map<String, List<Double>> groups = new LinkedHashMap<>();
        for (Map<String, Object> t : trades) {
            String k = str(t, key, "기타");
            groups.computeIfAbsent(k, x -> new ArrayList<>()).add(toDouble(t.get("pnlPct")));
        }
        sb.append("\n").append(sep).append("\n[ ").append(title).append(" ]\n").append(sep).append("\n");
        sb.append(String.format("%-22s | %5s | %6s | %8s | %5s%n", "구분", "건수", "승률", "평균손익", "PF"));
        sb.append(sep2).append("\n");
        for (Map.Entry<String, List<Double>> e : groups.entrySet()) {
            List<Double> v = e.getValue();
            int cnt  = v.size();
            int w    = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / cnt;
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-22s | %5d | %5.1f%% | %+7.2f%% | %5.2f%n",
                    e.getKey(), cnt, cnt > 0 ? (double) w / cnt * 100 : 0, a * 100, pf));
        }
    }

    private void appendScoreStats(StringBuilder sb, String sep, String sep2,
                                   List<Map<String, Object>> trades) {
        int[][] ranges = {{95,100},{90,94},{85,89},{80,84},{75,79},{0,74}};
        String[] labels = {"95-100","90-94","85-89","80-84","75-79","<75"};
        sb.append("\n").append(sep).append("\n[ Score 범위별 성과 ]\n").append(sep).append("\n");
        sb.append(String.format("%-8s | %5s | %6s | %8s | %5s%n","범위","건수","승률","평균손익","PF"));
        sb.append(sep2).append("\n");
        for (int i = 0; i < ranges.length; i++) {
            final int lo = ranges[i][0], hi = ranges[i][1];
            List<Double> v = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                int sc = toInt(t.get("signalScore"));
                if (sc >= lo && sc <= hi) v.add(toDouble(t.get("pnlPct")));
            }
            if (v.isEmpty()) continue;
            int w = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / v.size();
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-8s | %5d | %5.1f%% | %+7.2f%% | %5.2f%n",
                    labels[i], v.size(), (double) w / v.size() * 100, a * 100, pf));
        }
    }

    private void appendGradeStats(StringBuilder sb, String sep, String sep2,
                                   List<Map<String, Object>> trades) {
        String[] grades = {"SS","S","A","B","C","D"};
        sb.append("\n").append(sep).append("\n[ Grade별 성과 ]\n").append(sep).append("\n");
        sb.append(String.format("%-4s | %5s | %6s | %8s | %5s%n","등급","건수","승률","평균손익","PF"));
        sb.append(sep2).append("\n");
        for (String g : grades) {
            List<Double> v = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                if (g.equals(str(t, "signalGrade", ""))) v.add(toDouble(t.get("pnlPct")));
            }
            if (v.isEmpty()) continue;
            int w = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / v.size();
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-4s | %5d | %5.1f%% | %+7.2f%% | %5.2f%n",
                    g, v.size(), (double) w / v.size() * 100, a * 100, pf));
        }
    }

    private void appendMonthlyStats(StringBuilder sb, String sep, String sep2,
                                      List<Map<String, Object>> trades) {
        Map<String, List<Double>> groups = new LinkedHashMap<>();
        for (Map<String, Object> t : trades) {
            String month = parseMonth(str(t, "entryTime", ""));
            groups.computeIfAbsent(month, x -> new ArrayList<>()).add(toDouble(t.get("pnlPct")));
        }
        if (groups.isEmpty()) return;
        sb.append("\n").append(sep).append("\n[ 월별 성과 ]\n").append(sep).append("\n");
        sb.append(String.format("%-8s | %5s | %6s | %8s | %8s | %5s%n",
                "월", "건수", "승률", "평균손익", "누적손익", "PF"));
        sb.append(sep2).append("\n");
        for (Map.Entry<String, List<Double>> e : groups.entrySet()) {
            List<Double> v = e.getValue();
            int cnt = v.size();
            int w = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / cnt;
            double cum = v.stream().mapToDouble(Double::doubleValue).sum();
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-8s | %5d | %5.1f%% | %+7.2f%% | %+7.2f%% | %5.2f%n",
                    e.getKey(), cnt, (double) w / cnt * 100, a * 100, cum * 100, pf));
        }
    }

    private void appendTimeSlotStats(StringBuilder sb, String sep, String sep2,
                                      List<Map<String, Object>> trades) {
        int[][] slots      = {{9,0,10,0},{10,0,11,0},{11,0,12,0},{12,0,13,0},{13,0,14,0}};
        String[] slotLabels = {"09:00~10:00","10:00~11:00","11:00~12:00","12:00~13:00","13:00~14:00"};
        sb.append("\n").append(sep).append("\n[ 진입 시간대별 성과 ]\n").append(sep).append("\n");
        sb.append(String.format("%-12s | %5s | %6s | %8s | %5s%n",
                "시간대", "건수", "승률", "평균손익", "PF"));
        sb.append(sep2).append("\n");
        for (int i = 0; i < slots.length; i++) {
            int h1 = slots[i][0], m1 = slots[i][1], h2 = slots[i][2];
            List<Double> v = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                int h = parseHour(str(t, "entryTime", "")), m = parseMin(str(t, "entryTime", ""));
                if (h < 0) continue;
                int total = h * 60 + m, slotStart = h1 * 60 + m1, slotEnd = h2 * 60;
                if (total >= slotStart && total < slotEnd) v.add(toDouble(t.get("pnlPct")));
            }
            if (v.isEmpty()) continue;
            int w = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / v.size();
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-12s | %5d | %5.1f%% | %+7.2f%% | %5.2f%n",
                    slotLabels[i], v.size(), (double) w / v.size() * 100, a * 100, pf));
        }
    }

    private void appendHoldTimeStats(StringBuilder sb, String sep, String sep2,
                                      List<Map<String, Object>> trades) {
        int[]    thresholds = {15, 60, 180, Integer.MAX_VALUE};
        String[] labels     = {"~15분", "15~60분", "60~180분", "180분+"};
        sb.append("\n").append(sep).append("\n[ 보유 시간별 성과 ]\n").append(sep).append("\n");
        sb.append(String.format("%-10s | %5s | %6s | %8s | %5s%n",
                "보유시간", "건수", "승률", "평균손익", "PF"));
        sb.append(sep2).append("\n");
        for (int i = 0; i < thresholds.length; i++) {
            int lo = (i == 0) ? 0 : thresholds[i - 1], hi = thresholds[i];
            List<Double> v = new ArrayList<>();
            for (Map<String, Object> t : trades) {
                int holdMin = toInt(t.get("holdSeconds")) / 60;
                if (holdMin >= lo && holdMin < hi) v.add(toDouble(t.get("pnlPct")));
            }
            if (v.isEmpty()) continue;
            int w = (int) v.stream().filter(p -> p > 0).count();
            double a = v.stream().mapToDouble(Double::doubleValue).sum() / v.size();
            double win = 0, loss = 0;
            for (double p : v) { if (p > 0) win += p; else loss += Math.abs(p); }
            double pf = loss > 0 ? win / loss : (win > 0 ? 9999 : 0);
            sb.append(String.format("%-10s | %5d | %5.1f%% | %+7.2f%% | %5.2f%n",
                    labels[i], v.size(), (double) w / v.size() * 100, a * 100, pf));
        }
    }

    private int parseHour(String ts) {
        if (ts == null || ts.length() < 13) return -1;
        try { return Integer.parseInt(ts.substring(11, 13)); } catch (Exception e) { return -1; }
    }

    private int parseMin(String ts) {
        if (ts == null || ts.length() < 16) return -1;
        try { return Integer.parseInt(ts.substring(14, 16)); } catch (Exception e) { return -1; }
    }

    private String parseMonth(String ts) {
        if (ts == null || ts.length() < 7) return "?";
        return ts.substring(0, 7);
    }

    // ── 유틸 ──────────────────────────────────────────────────
    private String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key); return v != null ? v.toString() : def;
    }
    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }
    private int toInt(Object v) { return (int) Math.round(toDouble(v)); }
    private String fmt(long n) { return String.format("%,d", n); }

    /** US symbol autocomplete via Yahoo Finance */
    @GetMapping("/searchSymbol")
    @ResponseBody
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> searchSymbol(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "US") String market) {
        if (q.trim().length() < 1 || !"US".equalsIgnoreCase(market))
            return Collections.emptyList();
        try {
            String enc = URLEncoder.encode(q.trim(), "UTF-8");
            String urlStr = "https://query1.finance.yahoo.com/v1/finance/search?q=" + enc
                    + "&quotesCount=8&newsCount=0&enableFuzzyQuery=false&enableCb=false";
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setRequestProperty("Accept", "application/json");
            conn.setConnectTimeout(5_000);
            conn.setReadTimeout(5_000);
            if (conn.getResponseCode() != 200) return Collections.emptyList();

            Map<?, ?> root   = new ObjectMapper().readValue(conn.getInputStream(), Map.class);
            List<?>   quotes = (List<?>) root.get("quotes");
            if (quotes == null) return Collections.emptyList();

            List<Map<String, String>> result = new ArrayList<>();
            for (Object o : quotes) {
                Map<?, ?> qm  = (Map<?, ?>) o;
                String    sym = (String) qm.get("symbol");
                if (sym == null || sym.contains(".")) continue;
                String name = qm.get("shortname") != null ? (String) qm.get("shortname")
                            : qm.get("longname")  != null ? (String) qm.get("longname") : "";
                String exch = qm.get("exchDisp") != null ? (String) qm.get("exchDisp") : "";
                String type = qm.get("quoteType") != null ? (String) qm.get("quoteType") : "";
                Map<String, String> item = new LinkedHashMap<>();
                item.put("symbol", sym);
                item.put("name",   name);
                item.put("exchange", exch);
                item.put("type",   type);
                result.add(item);
                if (result.size() >= 6) break;
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private BacktestConfig parseConfig(Map<String, String> req) {
        BacktestConfig cfg = new BacktestConfig();

        // 모드 활성화
        tryBool(req, "enablePullback",      v -> cfg.enablePullback      = v);
        tryBool(req, "enableBreakout",      v -> cfg.enableBreakout      = v);
        tryBool(req, "enableEarlyMomentum", v -> cfg.enableEarlyMomentum = v);

        // 공통 진입
        tryInt(req, "minHistoryBars",      v -> cfg.minHistoryBars      = v);
        tryInt(req, "minHistoryMinutes",   v -> cfg.minHistoryMinutes   = v);
        tryDbl(req, "minPrice",            v -> cfg.minPrice            = v);
        tryDbl(req, "vwapHardLimitPct",   v -> cfg.vwapHardLimitPct    = v);
        tryDbl(req, "minTurnoverKrx",      v -> cfg.minTurnoverKrx     = v);
        tryDbl(req, "minAvgTurnoverKrx",   v -> cfg.minAvgTurnoverKrx  = v);
        tryDbl(req, "minTurnoverUs",       v -> cfg.minTurnoverUs      = v);
        tryBool(req, "useMarketFilter",    v -> cfg.useMarketFilter     = v);
        tryInt(req, "buyCooldownSec",      v -> cfg.buyCooldownSec      = v);
        tryInt(req, "maxDailyEntryCount",  v -> cfg.maxDailyEntryCount  = v);
        tryInt(req, "maxSamePatternEntry", v -> cfg.maxSamePatternEntry = v);

        // PULLBACK 진입
        tryInt(req, "pullbackMinScore",                  v -> cfg.pullbackMinScore      = v);
        tryDbl(req, "pullbackUpperPct",                  v -> cfg.pullbackUpperPct      = v);
        tryDbl(req, "pullbackLowerPct",                  v -> cfg.pullbackLowerPct      = v);
        tryDbl(req, "vwapMaxGapPullbackPct",             v -> cfg.vwapMaxGapPullbackPct = v);
        tryDbl(req, "pullbackVolumeMult",                v -> cfg.pullbackVolumeMult    = v);
        tryDbl(req, "pullbackVelocityShort",             v -> cfg.pullbackVelocityShort = v);
        tryDbl(req, "pullbackVelocityMid",               v -> cfg.pullbackVelocityMid   = v);
        tryInt(req, "pullbackRequiredBullishBars",       v -> cfg.pullbackRequiredBullishBars        = v);
        tryBool(req, "pullbackRequireAboveVwap",         v -> cfg.pullbackRequireAboveVwap           = v);
        tryBool(req, "pullbackRequireVwapSlope",         v -> cfg.pullbackRequireVwapSlope           = v);
        tryBool(req, "pullbackRequireRecentHighBreakout",v -> cfg.pullbackRequireRecentHighBreakout  = v);

        // PULLBACK 청산
        tryDbl(req, "pullbackStopPct",   v -> cfg.pullbackStopPct   = v);
        tryDbl(req, "pullbackTpPct",     v -> cfg.pullbackTpPct     = v);
        tryDbl(req, "pullbackTrailSt",   v -> cfg.pullbackTrailSt   = v);
        tryDbl(req, "pullbackTrailDrop", v -> cfg.pullbackTrailDrop = v);

        // BREAKOUT 진입
        tryInt(req, "breakoutMinScore",           v -> cfg.breakoutMinScore       = v);
        tryInt(req, "breakoutMaxScore",           v -> cfg.breakoutMaxScore       = v);
        tryDbl(req, "vwapMaxGapBreakoutPct",      v -> cfg.vwapMaxGapBreakoutPct  = v);
        tryDbl(req, "breakoutRetestLower",        v -> cfg.breakoutRetestLower    = v);
        tryDbl(req, "breakoutRetestUpper",        v -> cfg.breakoutRetestUpper    = v);
        tryDbl(req, "breakoutStrongVolMult",      v -> cfg.breakoutStrongVolMult  = v);
        tryDbl(req, "breakoutKrxVolMult",         v -> cfg.breakoutKrxVolMult     = v);
        tryBool(req, "breakoutRequireAcceleration", v -> cfg.breakoutRequireAcceleration = v);
        tryBool(req, "breakoutRequireMultiUptrend", v -> cfg.breakoutRequireMultiUptrend = v);
        tryBool(req, "breakoutOverheatBlock",       v -> cfg.breakoutOverheatBlock       = v);
        tryDbl(req, "breakoutMinVelocityMid",        v -> cfg.breakoutMinVelocityMid        = v);
        tryDbl(req, "breakoutMinVelocityLong",       v -> cfg.breakoutMinVelocityLong       = v);
        tryInt(req, "breakoutRequiredBullishBars",   v -> cfg.breakoutRequiredBullishBars   = v);

        // BREAKOUT 청산
        tryDbl(req, "breakoutStopPct",   v -> cfg.breakoutStopPct   = v);
        tryDbl(req, "breakoutTpPct",     v -> cfg.breakoutTpPct     = v);
        tryDbl(req, "breakoutTrailSt",   v -> cfg.breakoutTrailSt   = v);
        tryDbl(req, "breakoutTrailDrop", v -> cfg.breakoutTrailDrop = v);

        // 공통 볼륨 배수 (MOMENTUM_VOLUME_MULT override)
        tryDbl(req, "volumeMult", v -> cfg.volumeMult = v);

        // EARLY_MOMENTUM 진입
        tryInt(req, "emMinScore",   v -> cfg.emMinScore   = v);
        tryDbl(req, "emVelocity",   v -> cfg.emVelocity   = v);
        tryDbl(req, "emVolumeMult", v -> cfg.emVolumeMult = v);
        tryBool(req, "em3TrendUp",  v -> cfg.em3TrendUp   = v);

        // EARLY_MOMENTUM 청산
        tryDbl(req, "emStopPct", v -> cfg.emStopPct = v);
        tryDbl(req, "emTpPct",   v -> cfg.emTpPct   = v);

        // 공통 청산
        tryDbl(req, "emergencyStopPct",  v -> cfg.emergencyStopPct  = v);
        tryBool(req, "useVwapBreak",     v -> cfg.useVwapBreak      = v);
        tryDbl(req, "vwapBreakBuffer",   v -> cfg.vwapBreakBuffer   = v);
        tryBool(req, "useBreakevenGuard",v -> cfg.useBreakevenGuard = v);
        tryDbl(req, "breakevenPeak",     v -> cfg.breakevenPeak     = v);
        tryDbl(req, "breakevenLoss",     v -> cfg.breakevenLoss     = v);
        tryBool(req, "useFailedBreakout",v -> cfg.useFailedBreakout = v);
        tryBool(req, "useFailedPullback",v -> cfg.useFailedPullback = v);
        tryBool(req, "useEodForceSell",  v -> cfg.useEodForceSell   = v);
        tryBool(req, "blockSSGrade",           v -> cfg.blockSSGrade          = v);
        tryBool(req, "blockSGrade",            v -> cfg.blockSGrade           = v);
        tryBool(req, "blockAGrade",            v -> cfg.blockAGrade           = v);
        tryBool(req, "blockBGrade",            v -> cfg.blockBGrade           = v);
        tryBool(req, "blockSameDayAfterStop",  v -> cfg.blockSameDayAfterStop = v);
        tryInt(req,  "vwapBreakGraceSec",      v -> cfg.vwapBreakGraceSec     = v);
        tryInt(req,  "entryEndHour",           v -> cfg.entryEndHour          = v);
        tryInt(req,  "entryEndMinute",         v -> cfg.entryEndMinute        = v);
        tryInt(req,  "slowModeEntryEndHour",   v -> cfg.slowModeEntryEndHour  = v);
        tryInt(req,  "slowModeEntryEndMinute", v -> cfg.slowModeEntryEndMinute= v);
        tryDbl(req,  "maxDailyLossPct",        v -> cfg.maxDailyLossPct       = v);
        tryDbl(req,  "maxDailyProfitPct",      v -> cfg.maxDailyProfitPct     = v);

        // 비용
        tryDbl(req, "slippagePct", v -> cfg.slippagePct = v);
        tryDbl(req, "feePct",      v -> cfg.feePct      = v);
        tryDbl(req, "taxPct",      v -> cfg.taxPct      = v);

        // STRONG_PULLBACK 진입
        tryBool(req, "enableStrongPullback", v -> cfg.enableStrongPullback = v);
        tryDbl(req,  "spPullbackMinPct",     v -> cfg.spPullbackMinPct     = v);
        tryDbl(req,  "spPullbackMaxPct",     v -> cfg.spPullbackMaxPct     = v);
        tryDbl(req,  "spVwapMinAbovePct",    v -> cfg.spVwapMinAbovePct    = v);
        tryDbl(req,  "spVol3RatioMax",       v -> cfg.spVol3RatioMax       = v);
        tryDbl(req,  "spBodyRatioMin",       v -> cfg.spBodyRatioMin       = v);
        tryInt(req,  "spMinScore",           v -> cfg.spMinScore           = v);
        // STRONG_PULLBACK 청산
        tryDbl(req,  "spStopPct",            v -> cfg.spStopPct            = v);
        tryDbl(req,  "spTpPct",              v -> cfg.spTpPct              = v);
        tryDbl(req,  "spTrailSt",            v -> cfg.spTrailSt            = v);
        tryDbl(req,  "spTrailDrop",          v -> cfg.spTrailDrop          = v);

        // VWAP_RECLAIM 진입
        tryBool(req, "enableVwapReclaim",    v -> cfg.enableVwapReclaim    = v);
        tryInt(req,  "vrLookbackBars",       v -> cfg.vrLookbackBars       = v);
        tryDbl(req,  "vrVolMult",            v -> cfg.vrVolMult            = v);
        tryInt(req,  "vrMinAboveVwapBars",   v -> cfg.vrMinAboveVwapBars   = v);
        tryInt(req,  "vrMinScore",           v -> cfg.vrMinScore           = v);
        tryInt(req,  "vrMaxScore",           v -> cfg.vrMaxScore           = v);
        // VWAP_RECLAIM 청산
        tryDbl(req,  "vrStopPct",            v -> cfg.vrStopPct            = v);
        tryDbl(req,  "vrTpPct",              v -> cfg.vrTpPct              = v);
        tryDbl(req,  "vrTrailSt",            v -> cfg.vrTrailSt            = v);
        tryDbl(req,  "vrTrailDrop",          v -> cfg.vrTrailDrop          = v);

        // RSI_BOLLINGER_REBOUND 진입
        tryBool(req, "enableRsiBbRebound",        v -> cfg.enableRsiBbRebound        = v);
        tryInt(req,  "rsiBbMinScore",             v -> cfg.rsiBbMinScore             = v);
        tryInt(req,  "rsiBbRsiPeriod",            v -> cfg.rsiBbRsiPeriod            = v);
        tryInt(req,  "rsiBbSignalPeriod",         v -> cfg.rsiBbSignalPeriod         = v);
        tryInt(req,  "rsiBbPeriod",               v -> cfg.rsiBbPeriod               = v);
        tryDbl(req,  "rsiBbStdMult",              v -> cfg.rsiBbStdMult              = v);
        tryDbl(req,  "rsiBbLowerTouchBufferPct",  v -> cfg.rsiBbLowerTouchBufferPct  = v);
        tryDbl(req,  "rsiBbMaxBreakdownPct",      v -> cfg.rsiBbMaxBreakdownPct      = v);
        tryDbl(req,  "rsiBbMinVwapPct",           v -> cfg.rsiBbMinVwapPct           = v);
        tryDbl(req,  "rsiBbRsiLowThreshold",      v -> cfg.rsiBbRsiLowThreshold      = v);
        // RSI_BOLLINGER_REBOUND 청산
        tryDbl(req,  "rsiBbStopPct",              v -> cfg.rsiBbStopPct              = v);
        tryDbl(req,  "rsiBbTpPct",                v -> cfg.rsiBbTpPct                = v);
        tryDbl(req,  "rsiBbTrailSt",              v -> cfg.rsiBbTrailSt              = v);
        tryDbl(req,  "rsiBbTrailDrop",            v -> cfg.rsiBbTrailDrop            = v);

        // OPENING_RANGE_BREAKOUT
        tryBool(req, "enableOpeningRangeBreakout", v -> cfg.enableOpeningRangeBreakout = v);
        tryInt(req,  "orbMaxScore",                v -> cfg.orbMaxScore                = v);
        tryDbl(req,  "orbStopPct",                 v -> cfg.orbStopPct                 = v);
        tryDbl(req,  "orbTpPct",                   v -> cfg.orbTpPct                   = v);
        tryDbl(req,  "orbTrailSt",                 v -> cfg.orbTrailSt                 = v);
        tryDbl(req,  "orbTrailDrop",               v -> cfg.orbTrailDrop               = v);

        // THIRTY_MIN_RSI_BB_CROSS 진입
        tryBool(req, "enable30mRsiBbCross",    v -> cfg.enable30mRsiBbCross    = v);
        tryInt(req,  "rsiBb30mRsiPeriod",      v -> cfg.rsiBb30mRsiPeriod      = v);
        tryInt(req,  "rsiBb30mSignalPeriod",   v -> cfg.rsiBb30mSignalPeriod   = v);
        tryInt(req,  "rsiBb30mBbPeriod",       v -> cfg.rsiBb30mBbPeriod       = v);
        tryDbl(req,  "rsiBb30mStdMult",        v -> cfg.rsiBb30mStdMult        = v);
        tryInt(req,  "rsiBb30mVolBars",        v -> cfg.rsiBb30mVolBars        = v);
        tryInt(req,  "rsiBb30mMinScore",       v -> cfg.rsiBb30mMinScore       = v);
        // THIRTY_MIN_RSI_BB_CROSS 청산
        tryDbl(req,  "rsiBb30mStopPct",        v -> cfg.rsiBb30mStopPct        = v);
        tryDbl(req,  "rsiBb30mTpPct",          v -> cfg.rsiBb30mTpPct          = v);
        tryDbl(req,  "rsiBb30mTrailSt",        v -> cfg.rsiBb30mTrailSt        = v);
        tryDbl(req,  "rsiBb30mTrailDrop",      v -> cfg.rsiBb30mTrailDrop      = v);

        // RED_TO_GREEN 진입
        tryBool(req, "enableR2G",              v -> cfg.enableR2G              = v);
        tryDbl(req,  "r2gMaxCrossPct",         v -> cfg.r2gMaxCrossPct         = v);
        tryDbl(req,  "r2gStopPct",             v -> cfg.r2gStopPct             = v);
        tryDbl(req,  "r2gTpPct",               v -> cfg.r2gTpPct               = v);
        tryDbl(req,  "r2gTrailSt",             v -> cfg.r2gTrailSt             = v);
        tryDbl(req,  "r2gTrailDrop",           v -> cfg.r2gTrailDrop           = v);

        // VWAP_RECLAIM_V2 진입
        tryBool(req, "enableVwapReclaimV2",    v -> cfg.enableVwapReclaimV2    = v);
        tryDbl(req,  "vr2MinIntradayGainPct",  v -> cfg.vr2MinIntradayGainPct  = v);
        tryInt(req,  "vr2VwapBelowLookback",   v -> cfg.vr2VwapBelowLookback   = v);
        tryDbl(req,  "vr2VolMult",             v -> cfg.vr2VolMult             = v);
        tryDbl(req,  "vr2RsiLow",             v -> cfg.vr2RsiLow              = v);
        tryDbl(req,  "vr2RsiHigh",            v -> cfg.vr2RsiHigh             = v);
        tryDbl(req,  "vr2FromDayHighMaxPct",   v -> cfg.vr2FromDayHighMaxPct   = v);
        tryDbl(req,  "vr2MaxVwapGapPct",       v -> cfg.vr2MaxVwapGapPct       = v);
        tryInt(req,  "vr2MinScore",            v -> cfg.vr2MinScore            = v);
        tryBool(req, "vr2RequireVwapBelow",    v -> cfg.vr2RequireVwapBelow    = v);
        // VWAP_RECLAIM_V2 청산
        tryDbl(req,  "vr2StopPct",             v -> cfg.vr2StopPct             = v);
        tryDbl(req,  "vr2TpPct",               v -> cfg.vr2TpPct               = v);
        tryDbl(req,  "vr2TrailSt",             v -> cfg.vr2TrailSt             = v);
        tryDbl(req,  "vr2TrailDrop",           v -> cfg.vr2TrailDrop           = v);

        return cfg;
    }

    private void tryInt(Map<String, String> req, String key, IntConsumer fn) {
        String v = req.get(key);
        if (v != null && !v.isBlank()) try { fn.accept(Integer.parseInt(v.trim())); } catch (Exception ignored) {}
    }

    private void tryDbl(Map<String, String> req, String key, DoubleConsumer fn) {
        String v = req.get(key);
        if (v != null && !v.isBlank()) try { fn.accept(Double.parseDouble(v.trim())); } catch (Exception ignored) {}
    }

    private void tryBool(Map<String, String> req, String key, java.util.function.Consumer<Boolean> fn) {
        String v = req.get(key);
        if (v != null) fn.accept("true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim()) || "on".equalsIgnoreCase(v.trim()));
    }
}
