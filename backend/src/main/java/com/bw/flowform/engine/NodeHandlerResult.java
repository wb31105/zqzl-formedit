package com.bw.flowform.engine;

public class NodeHandlerResult {

    private final boolean shouldProceed;
    private final String lastAction;
    private final boolean completed;

    private NodeHandlerResult(boolean shouldProceed, String lastAction, boolean completed) {
        this.shouldProceed = shouldProceed;
        this.lastAction = lastAction;
        this.completed = completed;
    }

    public static NodeHandlerResult proceed(String action) {
        return new NodeHandlerResult(true, action, false);
    }

    public static NodeHandlerResult proceed() {
        return new NodeHandlerResult(true, null, false);
    }

    public static NodeHandlerResult waitForTask() {
        return new NodeHandlerResult(false, null, false);
    }

    public static NodeHandlerResult complete() {
        return new NodeHandlerResult(false, null, true);
    }

    public boolean isShouldProceed() {
        return shouldProceed;
    }

    public String getLastAction() {
        return lastAction;
    }

    public boolean isCompleted() {
        return completed;
    }
}
