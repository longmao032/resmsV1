package com.guang.aiassistant.core.agent;

public abstract class ReActAgent extends BaseAgent {

    public abstract boolean think();

    public abstract String act();

    @Override
    public String step() {
        boolean keepRunning = think();
        if (!keepRunning) {
            setState(AgentConstant.FINISHED);
            if (getMessagesList() != null && !getMessagesList().isEmpty()) {
                return getMessagesList().get(getMessagesList().size() - 1).getText();
            }
            return "";
        }
        return act();
    }
}
