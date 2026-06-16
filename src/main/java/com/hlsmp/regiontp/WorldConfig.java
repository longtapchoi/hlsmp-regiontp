package com.hlsmp.regiontp;

public class WorldConfig {

    private final boolean enabled;
    private final int yThreshold;
    private final String destination;

    public WorldConfig(boolean enabled, int yThreshold, String destination) {
        this.enabled = enabled;
        this.yThreshold = yThreshold;
        this.destination = destination;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getYThreshold() {
        return yThreshold;
    }

    public String getDestination() {
        return destination;
    }
}
