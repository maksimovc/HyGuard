package dev.thenexusgates.hyguard.core.region;

public final class RegionFlagValue {

    public enum Mode {
        ALLOW,
        DENY,
        INHERIT,
        ALLOW_MEMBERS,
        ALLOW_TRUSTED
    }

    private Mode mode = Mode.INHERIT;
    private String textValue;

    public RegionFlagValue() {
    }

    public RegionFlagValue(Mode mode) {
        this.mode = mode;
    }

    public RegionFlagValue(Mode mode, String textValue) {
        this.mode = mode;
        this.textValue = textValue;
    }

    public Mode getMode() {
        return mode == null ? Mode.INHERIT : mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }
}