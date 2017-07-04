package org.agreement_technologies.service.map_landmarks;

public class MessageContentRPG implements java.io.Serializable {
    private static final long serialVersionUID = 6010531211080464901L;
    private String fluent;
    private Integer level;

    MessageContentRPG(String var, String val, Integer lv) {
        fluent = var.toString() + " " + val;
        level = lv;
    }

    public String getFluent() {
        return fluent;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String toString() {
        return fluent.toString() + " -> " + level;
    }
}