package com.budget.buoy.transaction;

import java.math.BigDecimal;
import java.util.List;

public class StatsData {
    private BigDecimal total;
    private Integer changeSinceLast;
    private String topSpending;
    private BigDecimal topSpendingAmount;
    private List<GraphData> graph;

    public StatsData(BigDecimal total, Integer changeSinceLast, String topSpending, BigDecimal topSpendingAmount, List<GraphData> graph) {
        this.total = total;
        this.changeSinceLast = changeSinceLast;
        this.topSpending = topSpending;
        this.topSpendingAmount = topSpendingAmount;
        this.graph = graph;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public Integer getChangeSinceLast() {
        return changeSinceLast;
    }

    public String getTopSpending() {
        return topSpending;
    }

    public BigDecimal getTopSpendingAmount() {
        return topSpendingAmount;
    }

    public List<GraphData> getGraph() { 
        return graph;
    }

}
