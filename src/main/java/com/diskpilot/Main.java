package com.diskpilot;

import com.diskpilot.ui.MainFrame;
import com.diskpilot.ui.Theme;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {

        Theme.setupGlobalAntialiasing();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception on " + thread.getName() + ": " + throwable.getMessage());
            throwable.printStackTrace();
        });

        SwingUtilities.invokeLater(() -> {
            try {
                MainFrame frame = new MainFrame();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Failed to initialize DiskPilot: " + e.getMessage(),
                        "Initialization Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}

