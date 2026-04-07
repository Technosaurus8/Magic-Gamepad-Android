package com.technosaurus.MagicGamepad.input;

public class InputObserver {
    private int[] Lstick;
    private int[] Rstick;
    private int[] buttonState;
    private OnInputChangedListener listener;

    public InputObserver() {
        Lstick = new int[2];
        Rstick = new int[2];
        buttonState = new int[17]; // Initial state of the button
    }

    public void setLstick(int[] Lstick) {
        this.Lstick = Lstick;
        notifyListener();
    }

    public void setRstick(int[] Rstick) {
        this.Rstick = Rstick;
        notifyListener();
    }

    public void setButtonState(int[] buttonState) {
        this.buttonState = buttonState;
        notifyListener();
    }

    public void setOnInputChangedListener(OnInputChangedListener listener) {
        this.listener = listener;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onInputChanged(Lstick, Rstick, buttonState);
        }
    }

    public interface OnInputChangedListener {
        void onInputChanged(int[] Lstick, int[] Rstick, int[] buttons);
    }
}

