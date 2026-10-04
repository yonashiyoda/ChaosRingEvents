package dev.chaosring.anomaly;

public enum AnomalyType {
    GRAVITY("Düşük yerçekimi"),
    ICE("Kaygan zemin"),
    METEOR("Meteor yağmuru");

    private final String label;

    AnomalyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
