package com.diskpilot.ui;

import com.diskpilot.model.ScanResult;
import com.diskpilot.ui.dashboard.DashboardPanel;
import com.diskpilot.ui.disklens.DiskLensPanel;
import com.diskpilot.ui.disklens.DuplicatesPanel;
import com.diskpilot.ui.history.HistoryPanel;
import com.diskpilot.ui.sortify.SortifyPanel;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame implements SidebarPanel.NavigationListener, DashboardPanel.DashboardActionHandler {

    private final SidebarPanel sidebar;
    private final JPanel cardsPanel;
    private final CardLayout cardLayout;

    private final DashboardPanel dashboardPanel;
    private final DiskLensPanel diskLensPanel;
    private final SortifyPanel sortifyPanel;
    private final DuplicatesPanel duplicatesPanel;
    private final HistoryPanel historyPanel;

    public MainFrame() {
        super("DiskPilot • Intelligent Windows Storage Management Suite");
        Theme.setupGlobalAntialiasing();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1080, 720));
        setPreferredSize(new Dimension(1280, 840));
        getContentPane().setBackground(Theme.BG_APP);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);
        cardsPanel.setBackground(Theme.BG_APP);

        dashboardPanel = new DashboardPanel(this);

        diskLensPanel = new DiskLensPanel(result -> {

            dashboardPanel.updateLargestFiles(result.getLargestFiles());
            dashboardPanel.updateCategoryChart(result.getCategoryBytes());
            dashboardPanel.refreshData();
        });

        sortifyPanel = new SortifyPanel();
        duplicatesPanel = new DuplicatesPanel();
        historyPanel = new HistoryPanel();

        cardsPanel.add(dashboardPanel, SidebarPanel.DASHBOARD);
        cardsPanel.add(diskLensPanel, SidebarPanel.DISKLENS);
        cardsPanel.add(sortifyPanel, SidebarPanel.SORTIFY);
        cardsPanel.add(duplicatesPanel, SidebarPanel.DUPLICATES);
        cardsPanel.add(historyPanel, SidebarPanel.HISTORY);

        sidebar = new SidebarPanel(this);

        setLayout(new BorderLayout());
        add(sidebar, BorderLayout.WEST);
        add(cardsPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    @Override
    public void onNavigate(String moduleKey) {
        cardLayout.show(cardsPanel, moduleKey);
        sidebar.setActive(moduleKey);

        if (SidebarPanel.DASHBOARD.equals(moduleKey)) {
            dashboardPanel.refreshData();
        } else if (SidebarPanel.HISTORY.equals(moduleKey)) {
            historyPanel.refreshHistory();
        }
    }

    @Override
    public void navigateTo(String moduleKey) {
        onNavigate(moduleKey);
    }

    @Override
    public void triggerQuickScan(String folderPath) {
        onNavigate(SidebarPanel.DISKLENS);
        diskLensPanel.startScanPath(folderPath);
    }
}

