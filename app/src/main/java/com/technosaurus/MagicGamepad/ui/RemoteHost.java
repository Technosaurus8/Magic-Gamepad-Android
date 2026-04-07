package com.technosaurus.MagicGamepad.ui;

/**
 * Interface for Fragment ↔ Activity communication.
 * Implemented by the remote Activity, called by layout fragments.
 */
public interface RemoteHost {

    /** Send a message to the connected server. */
    void send(String msg);

    /** Lock or unlock the navigation drawer. */
    void setDrawerLocked(boolean locked);

    /** Close the navigation drawer (e.g. after selecting a mode). */
    void closeDrawer();

    /** Get the currently selected player identifier (e.g. "p1"). */
    String getPlayer();

    /** Set the currently selected player identifier. */
    void setPlayer(String player);
}
