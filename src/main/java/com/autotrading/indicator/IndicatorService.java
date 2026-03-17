package com.autotrading.indicator;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IndicatorService {

    public double calculateRSI(List<Double> closes, int period) {
        if (closes == null || closes.size() < period + 1) {
            return 50.0;
        }
        double gain = 0, loss = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            double diff = closes.get(i) - closes.get(i - 1);
            if (diff > 0) gain += diff;
            else loss += Math.abs(diff);
        }
        double avgGain = gain / period;
        double avgLoss = loss / period;
        if (avgLoss == 0) return 100;
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }

    public BollingerBands calculateBollingerBands(List<Double> closes, int period, double multiplier) {
        if (closes == null || closes.size() < period) {
            return new BollingerBands(0, 0, 0, 0);
        }
        int from = closes.size() - period;
        double sum = 0;
        for (int i = from; i < closes.size(); i++) {
            sum += closes.get(i);
        }
        double sma = sum / period;
        double variance = 0;
        for (int i = from; i < closes.size(); i++) {
            variance += Math.pow(closes.get(i) - sma, 2);
        }
        variance /= period;
        double std = Math.sqrt(variance);
        return new BollingerBands(sma - multiplier * std, sma, sma + multiplier * std, sma);
    }

    public double calculateSMA(List<Double> closes, int period) {
        if (closes == null || closes.size() < period) {
            return 0;
        }
        double sum = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            sum += closes.get(i);
        }
        return sum / period;
    }

    public double calculateEMA(List<Double> closes, int period) {
        if (closes == null || closes.isEmpty()) {
            return 0;
        }
        if (closes.size() < period) {
            return closes.get(closes.size() - 1);
        }
        double k = 2.0 / (period + 1);
        double ema = closes.get(closes.size() - period);
        for (int i = closes.size() - period + 1; i < closes.size(); i++) {
            ema = closes.get(i) * k + ema * (1 - k);
        }
        return ema;
    }

    public double calculateAverage(List<Double> values, int period) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        if (period <= 0) {
            return 0;
        }
        int n = Math.min(period, values.size());
        double sum = 0;
        for (int i = values.size() - n; i < values.size(); i++) {
            sum += values.get(i);
        }
        return sum / n;
    }

    /**
     * 단일 종가 배열만 있을 때 간이 ATR 계산: |close - prevClose|의 평균.
     * 고가/저가가 없으므로 대체 지표로 사용.
     */
    public double calculateATR(List<Double> closes, int period) {
        if (closes == null || closes.size() < period + 1) {
            return 0;
        }
        if (period <= 0) {
            return 0;
        }
        double sumTR = 0;
        for (int i = closes.size() - period; i < closes.size(); i++) {
            double tr = Math.abs(closes.get(i) - closes.get(i - 1));
            sumTR += tr;
        }
        return sumTR / period;
    }

    public static class BollingerBands {
        private final double lower;
        private final double middle;
        private final double upper;
        private final double sma;

        public BollingerBands(double lower, double middle, double upper, double sma) {
            this.lower = lower;
            this.middle = middle;
            this.upper = upper;
            this.sma = sma;
        }

        public double getLower() { return lower; }
        public double getMiddle() { return middle; }
        public double getUpper() { return upper; }
        public double getSma() { return sma; }
    }
}
