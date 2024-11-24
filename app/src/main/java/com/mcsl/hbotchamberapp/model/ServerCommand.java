package com.mcsl.hbotchamberapp.model;

public class ServerCommand {
    private String action;

    public ServerCommand() {}

    public ServerCommand(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
