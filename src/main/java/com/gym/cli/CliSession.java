package com.gym.cli;

import com.gym.model.Administrator;

// Holds the active signed-in staff user and simple terminal navigation flags
public class CliSession {

    private Administrator currentUser;
    private boolean returnHome;
    private boolean logoutRequested;
    private boolean exitRequested;

    public CliSession() {
    }

    public Administrator getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(Administrator currentUser) {
        this.currentUser = currentUser;
    }

    public boolean isReturnHome() {
        return returnHome;
    }

    public void setReturnHome(boolean returnHome) {
        this.returnHome = returnHome;
    }

    public boolean isLogoutRequested() {
        return logoutRequested;
    }

    public void setLogoutRequested(boolean logoutRequested) {
        this.logoutRequested = logoutRequested;
    }

    public boolean isExitRequested() {
        return exitRequested;
    }

    public void setExitRequested(boolean exitRequested) {
        this.exitRequested = exitRequested;
    }

    public void clearTransientFlags() {
        this.returnHome = false;
        this.logoutRequested = false;
    }

    public String userLabel() {
        if (currentUser == null) {
            return "Guest";
        }
        return currentUser.getUsername() + " [" + currentUser.getRole() + "]";
    }

    public boolean shouldStopSubMenu() {
        return returnHome || logoutRequested || exitRequested;
    }
}

