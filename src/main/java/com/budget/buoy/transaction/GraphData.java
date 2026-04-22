package com.budget.buoy.transaction;

import java.math.BigDecimal;

    public class GraphData {
    private BigDecimal value;
    private String label;
    private String frontColor; // optional

    // Constructor without frontColor
    public GraphData(BigDecimal value, String label) {
        this.value = value;
        this.label = label;
    }

    // Constructor with frontColor
    public GraphData(BigDecimal value, String label, String color) {
        this.value = value;
        this.label = label;
        this.frontColor = color;
    }

    // Getters (needed if you want JSON serialization in Spring Boot)
    public BigDecimal getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    public String getFrontColor() {
        return frontColor;
    }

    @Override
    public String toString() {
        return "{value=" + value + ", label='" + label + "'" +
                (frontColor != null ? ", frontColor='" + frontColor + "'" : "") +
                "}";
    }
}


