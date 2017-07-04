package org.agreement_technologies.service.map_landmarks;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Alex
 */
public class MessageContentLandmarkGraph implements Serializable {
    public static final int COMMON_PRECS_STAGE = 0;
    public static final int VERIFICATION_STAGE = 1;
    public static final int IS_LANDMARK = 2;
    public static final int IS_NOT_LANDMARK = 3;
    public static final int RPG_UNCHANGED = 4;
    public static final int NO_PRODUCER_ACTIONS = 5;
    public static final int CHANGE_LEVEL = 6;
    public static final int PASS_BATON = 7;
    public static final int END_PROCEDURE = 8;
    private int type;
    private Integer nextLevel;
    private String literal;
    private ArrayList<LMLiteralInfo> literals;
    private ArrayList<String> reachedGoals;
    private String sender;
    private ArrayList<LMLiteralInfo> uSets;
    private ArrayList<String> RPGLiterals;
    private Integer globalIndex;


    public MessageContentLandmarkGraph(Integer gi, String l, ArrayList<LMLiteralInfo> c, ArrayList<LMLiteralInfo> u, ArrayList<String> r, ArrayList<String> g, String a, Integer ll, int t) {
        globalIndex = gi;
        nextLevel = ll;
        type = t;
        literal = l;
        literals = c;
        uSets = u;
        sender = a;
        reachedGoals = g;
        RPGLiterals = r;
    }

    public int getGlobalIndex() {
        return globalIndex;
    }

    public ArrayList<LMLiteralInfo> getLiterals() {
        return literals;
    }

    public ArrayList<LMLiteralInfo> getDisjunctions() {
        return uSets;
    }

    public ArrayList<String> getRPGLiterals() {
        return RPGLiterals;
    }

    public ArrayList<String> getReachedGoals() {
        return reachedGoals;
    }

    public String getAgent() {
        return sender;
    }

    public String getLiteral() {
        return literal;
    }

    public int getNextLevelSize() {
        return nextLevel;
    }

    public int getMessageType() {
        return type;
    }
}
