package com.autotrading.model;

public class MonitorAggregateRow {
    private double evaluationAmount;
    private double costAmount;
    private int holdingCount;

    public double getEvaluationAmount() {
        return evaluationAmount;
    }

    public void setEvaluationAmount(double evaluationAmount) {
        this.evaluationAmount = evaluationAmount;
    }

    public double getCostAmount() {
        return costAmount;
    }

    public void setCostAmount(double costAmount) {
        this.costAmount = costAmount;
    }

    public int getHoldingCount() {
        return holdingCount;
    }

    public void setHoldingCount(int holdingCount) {
        this.holdingCount = holdingCount;
    }
}
