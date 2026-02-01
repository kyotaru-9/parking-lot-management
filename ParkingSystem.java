import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.awt.font.TextAttribute;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import java.io.*;
import java.nio.file.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.swing.table.DefaultTableModel;
import java.nio.file.StandardOpenOption;
import javax.swing.table.DefaultTableCellRenderer;
import java.time.LocalDate;
import java.util.TreeMap;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.time.YearMonth;

public class ParkingSystem extends JFrame {
    private JPanel parkingLot;
    private Map<String, ParkingSpace> parkedCars = new HashMap<>();
    private List<ParkingRecord> parkingHistory = new ArrayList<>();
    private JLabel availableSpacesLabel, occupiedSpacesLabel, totalFareLabel;
    private JButton modeToggleButton;
    private boolean isDarkMode = false;
    private static final int TOTAL_SPACES = 40;
    private static final String RATE_FILE = "data/parkingrate.txt";
    private Map<String, Integer> hourlyRates = new HashMap<>();
    private int availableSpaces;
    private long totalParkingTime;
    private int totalParkedVehicles;
    private double todayRevenue = 0.0;

    private JPanel dashboardPanel;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private static final String PARKED_FILE = "data/parked.txt";
    private JTable historyTable;
    private DefaultTableModel historyTableModel;
    private static final String LOT_FILE = "data/lot.txt";
    private JPanel revenuePanel;
    private JPanel parkingRatesPanel;
    private JPanel settingsPanel;
    private JTable ratesTable;
    private DefaultTableModel ratesTableModel;
    private static final String REVENUE_FILE = "data/revenue.txt";
    private TreeMap<LocalDate, Double> revenueData = new TreeMap<>();
    private JTabbedPane revenueTabbedPane;
    private JTable dailyRevenueTable;
    private JTable weeklyRevenueTable;
    private JTable monthlyRevenueTable;
    private DefaultTableModel dailyRevenueTableModel;
    private DefaultTableModel weeklyRevenueTableModel;
    private DefaultTableModel monthlyRevenueTableModel;

    private static final Color DEFAULT_BACKGROUND_COLOR = new Color(240, 240, 240);
    private static final Color DEFAULT_FOREGROUND_COLOR = Color.BLACK;
    private static final Font DEFAULT_FONT = new Font("Arial", Font.PLAIN, 12);

    private JPanel frontPage;
    private boolean isAdminMode = false;

    public ParkingSystem() {
        setTitle("Parking System");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        ((JPanel)getContentPane()).setBorder(new EmptyBorder(10, 10, 10, 10));

        // Set a modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize components
        initializeComponents();

        // Create front page
        createFrontPage();

        // Show front page initially
        showFrontPage();

        setVisible(true);
    }

    private void initializeComponents() {
        availableSpaces = TOTAL_SPACES;
        totalParkingTime = 0;
        totalParkedVehicles = 0;
        todayRevenue = 0.0;

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);

        createParkingLot();
        createControlPanel();
        createStatusPanel();
        createDashboardPanel();
        createRevenuePanel();
        createParkingRatesPanel();
        createSettingsPanel();
        createHistoryPanel();

        contentPanel.add(parkingLot, "ParkingLot");
        contentPanel.add(dashboardPanel, "Dashboard");
        contentPanel.add(revenuePanel, "Revenue");
        contentPanel.add(parkingRatesPanel, "ParkingRates");
        contentPanel.add(settingsPanel, "Settings");

        add(contentPanel, BorderLayout.CENTER);

        modeToggleButton = new JButton("Dark Mode");
        modeToggleButton.addActionListener(e -> toggleMode());
        add(modeToggleButton, BorderLayout.SOUTH);

        // Load initial parking lot status
        loadLotStatus();
        loadParkingRates();
        loadTodayRevenue();
    }

    private void createParkingLot() {
        parkingLot = new JPanel(new GridLayout(4, 10, 10, 10));
        parkingLot.setBorder(BorderFactory.createTitledBorder("Parking Spaces"));
        for (int i = 1; i <= TOTAL_SPACES; i++) {
            ParkingSpace space = new ParkingSpace(i);
            space.addActionListener(e -> handleSpaceClick(space));
            parkingLot.add(space);
        }
    }

    private void createControlPanel() {
        JPanel controlPanel = new JPanel(new GridLayout(6, 1, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        JButton dashboardButton = createStyledButton("Dashboard", new ImageIcon("path/to/dashboard_icon.png"));
        JButton parkingLotButton = createStyledButton("Parking Lot", new ImageIcon("path/to/parking_lot_icon.png"));
        JButton historyButton = createStyledButton("History", new ImageIcon("path/to/history_icon.png"));
        JButton revenueButton = createStyledButton("Revenue", new ImageIcon("path/to/revenue_icon.png"));
        JButton ratesButton = createStyledButton("Parking Rates", new ImageIcon("path/to/rates_icon.png"));
        JButton settingsButton = createStyledButton("Settings", new ImageIcon("path/to/settings_icon.png"));

        dashboardButton.addActionListener(e -> showDashboard());
        parkingLotButton.addActionListener(e -> showParkingLot());
        historyButton.addActionListener(e -> viewHistory());
        revenueButton.addActionListener(e -> showRevenue());
        ratesButton.addActionListener(e -> showParkingRates());
        settingsButton.addActionListener(e -> showSettings());

        controlPanel.add(dashboardButton);
        controlPanel.add(parkingLotButton);
        controlPanel.add(historyButton);
        controlPanel.add(revenueButton);
        controlPanel.add(ratesButton);
        controlPanel.add(settingsButton);

        add(controlPanel, BorderLayout.EAST);
    }

    private JButton createStyledButton(String text, ImageIcon icon) {
        JButton button = new JButton(text, icon);
        button.setHorizontalTextPosition(SwingConstants.RIGHT);
        button.setIconTextGap(10);
        return button;
    }

    private void createStatusPanel() {
        JPanel statusPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        statusPanel.setBorder(BorderFactory.createTitledBorder("Status"));
        availableSpacesLabel = new JLabel("Available: " + TOTAL_SPACES);
        occupiedSpacesLabel = new JLabel("Occupied: 0");
        totalFareLabel = new JLabel("Total Fare: $0");

        statusPanel.add(availableSpacesLabel);
        statusPanel.add(occupiedSpacesLabel);
        statusPanel.add(totalFareLabel);

        add(statusPanel, BorderLayout.NORTH);
    }

    private void handleSpaceClick(ParkingSpace space) {
        if (isAdminMode) {
            if (space.isOccupied()) {
                removeVehicle(space.getSpaceNumber());
            } else {
                parkVehicle(space.getSpaceNumber());
            }
        } else {
            if (space.isOccupied()) {
                JOptionPane.showMessageDialog(this, "This space is occupied.", "Space Status", JOptionPane.INFORMATION_MESSAGE);
            } else {
                int choice = JOptionPane.showConfirmDialog(this, 
                    "Would you like to park your vehicle in this space?", 
                    "Park Vehicle", 
                    JOptionPane.YES_NO_OPTION);
                if (choice == JOptionPane.YES_OPTION) {
                    parkVehicle(space.getSpaceNumber());
                }
            }
        }
    }

    private void parkVehicle(int spaceNumber) {
        String[] options = {"Car", "Motor", "Truck"};
        String vehicleType = (String) JOptionPane.showInputDialog(this, 
            "Select vehicle type:", 
            "Park Vehicle",
            JOptionPane.QUESTION_MESSAGE, 
            null, 
            options, 
            options[0]);
        
        if (vehicleType != null) {
            String licensePlate = JOptionPane.showInputDialog(this, "Enter license plate:");
            if (licensePlate != null && !licensePlate.trim().isEmpty()) {
                ParkingSpace space = findAvailableSpace(spaceNumber);
                if (space != null) {
                    space.occupy(vehicleType, licensePlate);
                    parkedCars.put(licensePlate, space);
                    availableSpaces--;
                    updateStatus();
                    saveParkedVehicle(licensePlate, vehicleType, space.getSpaceNumber());
                    updateHistoryPanel();
                    updateLotFile();
                    JOptionPane.showMessageDialog(this, "Vehicle parked successfully!");
                } else {
                    JOptionPane.showMessageDialog(this, "No available parking space!");
                }
            }
        }
    }

    private void saveParkedVehicle(String licensePlate, String vehicleType, int spaceNumber) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(PARKED_FILE, true))) {
            writer.println(licensePlate + "," + vehicleType + "," + spaceNumber + "," + System.currentTimeMillis());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeVehicle(int spaceNumber) {
        ParkingSpace space = (ParkingSpace) parkingLot.getComponent(spaceNumber - 1);
        if (space.isOccupied()) {
            removeVehicle(space.getLicensePlate());
        } else {
            JOptionPane.showMessageDialog(this, "This space is not occupied!");
        }
    }

    private void removeVehicle(String licensePlate) {
        ParkingSpace space = parkedCars.get(licensePlate);
        if (space != null) {
            long parkingDurationMillis = System.currentTimeMillis() - space.getEntryTime();
            double parkingDurationHours = parkingDurationMillis / (60.0 * 60 * 1000);
            totalParkingTime += parkingDurationHours;
            totalParkedVehicles++;
            
            double fare = calculateFare(space.getVehicleType(), parkingDurationHours);
            updateRevenue(fare);
            
            // Create a new ParkingRecord and add it to parkingHistory
            parkingHistory.add(new ParkingRecord(licensePlate, space.getVehicleType(), space.getSpaceNumber(), space.getEntryTime(), System.currentTimeMillis(), fare));
            updateParkedVehicleInFile(licensePlate, space.getEntryTime(), System.currentTimeMillis(), fare);
            space.vacate();
            parkedCars.remove(licensePlate);
            availableSpaces++;
            updateStatus();
            updateHistoryPanel();
            updateLotFile();
            JOptionPane.showMessageDialog(this, String.format("Vehicle removed. Fare: $%.2f", fare));
        } else {
            JOptionPane.showMessageDialog(this, "Vehicle not found!");
        }
    }

    private void updateParkedVehicleInFile(String licensePlate, long entryTime, long exitTime, double fare) {
        try {
            List<String> lines = Files.readAllLines(Paths.get(PARKED_FILE));
            List<String> updatedLines = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts[0].equals(licensePlate) && parts.length == 4) {
                    // Update the line with exit time and fare
                    updatedLines.add(String.join(",", parts[0], parts[1], parts[2], parts[3], 
                                     String.valueOf(exitTime), String.format("%.2f", fare)));
                } else {
                    updatedLines.add(line);
                }
            }
            Files.write(Paths.get(PARKED_FILE), updatedLines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void viewHistory() {
        updateHistoryPanel();
        cardLayout.show(contentPanel, "History");
    }

    private ParkingSpace findAvailableSpace(int preferredSpace) {
        if (preferredSpace > 0 && preferredSpace <= TOTAL_SPACES) {
            ParkingSpace space = (ParkingSpace) parkingLot.getComponent(preferredSpace - 1);
            if (!space.isOccupied()) {
                return space;
            }
        }
        for (Component component : parkingLot.getComponents()) {
            ParkingSpace space = (ParkingSpace) component;
            if (!space.isOccupied()) {
                return space;
            }
        }
        return null;
    }

    private double calculateFare(String vehicleType, double hours) {
        int hourlyRate = hourlyRates.getOrDefault(vehicleType.toLowerCase(), 0);
        return hourlyRate * Math.ceil(hours);
    }

    private void updateStatus() {
        int occupied = parkedCars.size();
        int available = TOTAL_SPACES - occupied;
        double totalFare = parkingHistory.stream().mapToDouble(ParkingRecord::getFare).sum();

        availableSpacesLabel.setText("Available: " + available);
        occupiedSpacesLabel.setText("Occupied: " + occupied);
        totalFareLabel.setText(String.format("Total Fare: $%.2f", totalFare));
        
        // Update today's revenue display
        if (totalFareLabel != null) {
            totalFareLabel.setText(String.format("Today's Revenue: $%.2f", todayRevenue));
        }
    }

    private void toggleMode() {
        isDarkMode = !isDarkMode;
        Color bgColor = isDarkMode ? new Color(50, 50, 50) : new Color(240, 240, 240);
        Color fgColor = isDarkMode ? Color.WHITE : Color.BLACK;
        
        SwingUtilities.invokeLater(() -> {
            setBackground(bgColor);
            getContentPane().setBackground(bgColor);
            parkingLot.setBackground(bgColor);
            updateComponentColors(this, bgColor, fgColor);
        });
        
        modeToggleButton.setText(isDarkMode ? "Light Mode" : "Dark Mode");
    }

    private void updateComponentColors(Container container, Color bg, Color fg) {
        for (Component comp : container.getComponents()) {
            if (!(comp instanceof ParkingSpace)) {
                comp.setBackground(bg);
                comp.setForeground(fg);
            }
            if (comp instanceof JPanel) {
                ((JPanel) comp).setOpaque(true);
            }
            if (comp instanceof JScrollPane) {
                ((JScrollPane) comp).getViewport().setBackground(bg);
            }
            if (comp instanceof Container) {
                updateComponentColors((Container) comp, bg, fg);
            }
        }
    }

    private void updateComponentFonts(Container container, Font font) {
        for (Component comp : container.getComponents()) {
            comp.setFont(font);
            if (comp instanceof Container) {
                updateComponentFonts((Container) comp, font);
            }
        }
    }

    private class ParkingSpace extends JButton {
        private int spaceNumber;
        private boolean occupied;
        private String vehicleType;
        private String licensePlate;
        private long entryTime;
        private ImageIcon carIcon;
        private ImageIcon motorIcon;
        private ImageIcon truckIcon;

        public ParkingSpace(int spaceNumber) {
            this.spaceNumber = spaceNumber;
            this.occupied = false;
            setText(String.valueOf(spaceNumber));
            setBackground(Color.GREEN);
            
            // Load icons
            carIcon = new ImageIcon("img/car.png");
            motorIcon = new ImageIcon("img/motor.png");
            truckIcon = new ImageIcon("img/truck.png");
            
            // Resize icons
            carIcon = resizeIcon(carIcon, 40, 40);
            motorIcon = resizeIcon(motorIcon, 20, 40);  // Narrower width for motor
            truckIcon = resizeIcon(truckIcon, 30, 40);  // Slightly narrower width for truck
        }

        public void occupy(String vehicleType, String licensePlate) {
            this.occupied = true;
            this.vehicleType = vehicleType;
            this.licensePlate = licensePlate;
            this.entryTime = System.currentTimeMillis();
            setBackground(Color.RED);
            setText(null);  // Remove text
            setIcon(getVehicleIcon(vehicleType));
            setToolTipText("Space: " + spaceNumber + ", Type: " + vehicleType + ", Plate: " + licensePlate);
        }

        public void vacate() {
            this.occupied = false;
            this.vehicleType = null;
            this.licensePlate = null;
            this.entryTime = 0;
            setBackground(Color.GREEN);
            setText(String.valueOf(spaceNumber));
            setIcon(null);  // Remove icon
            setToolTipText(null);
        }

        private ImageIcon getVehicleIcon(String vehicleType) {
            switch (vehicleType.toLowerCase()) {
                case "car":
                    return carIcon;
                case "motor":
                    return motorIcon;
                case "truck":
                    return truckIcon;
                default:
                    return null;
            }
        }

        private ImageIcon resizeIcon(ImageIcon icon, int width, int height) {
            Image img = icon.getImage();
            Image resizedImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(resizedImg);
        }

        public boolean isOccupied() {
            return occupied;
        }

        public int getSpaceNumber() {
            return spaceNumber;
        }

        public String getVehicleType() {
            return vehicleType;
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public long getEntryTime() {
            return entryTime;
        }

        public void setEntryTime(long entryTime) {
            this.entryTime = entryTime;
        }
    }

    private class ParkingRecord {
        private String licensePlate;
        private String vehicleType;
        private int spaceNumber;
        private long entryTime;
        private long exitTime;
        private double fare;

        public ParkingRecord(String licensePlate, String vehicleType, int spaceNumber, long entryTime, long exitTime, double fare) {
            this.licensePlate = licensePlate;
            this.vehicleType = vehicleType;
            this.spaceNumber = spaceNumber;
            this.entryTime = entryTime;
            this.exitTime = exitTime;
            this.fare = fare;
        }

        public double getFare() {
            return fare;
        }

        @Override
        public String toString() {
            long durationHours = (exitTime - entryTime) / (60 * 60 * 1000);
            return String.format("License: %s, Type: %s, Space Number: %d, Entry Time: %s, Exit Time: %s, Duration: %d hours, Fare: $%.2f",
                    licensePlate, vehicleType, spaceNumber, entryTime, exitTime, durationHours, fare);
        }
    }

    private void createDashboardPanel() {
        dashboardPanel = new JPanel(new GridBagLayout());
        dashboardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        dashboardPanel.setBackground(new Color(240, 240, 240));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        // Title
        JLabel titleLabel = createStyledLabel("Dashboard", 28, Font.BOLD);
        dashboardPanel.add(titleLabel, gbc);

        // Statistics Panel
        JPanel statsPanel = createStatsPanel();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 0.5;
        dashboardPanel.add(statsPanel, gbc);

        // Refresh button
        JButton refreshButton = new JButton("Refresh Dashboard");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 14));
        refreshButton.addActionListener(e -> updateDashboard());
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        dashboardPanel.add(refreshButton, gbc);
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        statsPanel.setOpaque(false);

        statsPanel.add(createStatPanel("Available Spaces", availableSpaces + "", "spaces"));
        statsPanel.add(createStatPanel("Occupied Spaces", (TOTAL_SPACES - availableSpaces) + "", "spaces"));
        statsPanel.add(createStatPanel("Average Parking Time", "0.00", "hours"));
        statsPanel.add(createStatPanel("Today's Revenue", "$0.00", ""));

        return statsPanel;
    }

    private JPanel createStatPanel(String title, String value, String unit) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new CompoundBorder(new LineBorder(new Color(200, 200, 200), 1), 
                                           BorderFactory.createEmptyBorder(10, 10, 10, 10)));

        JLabel titleLabel = createStyledLabel(title, 14, Font.BOLD);
        panel.add(titleLabel, BorderLayout.NORTH);

        JLabel valueLabel = createStyledLabel(value, 24, Font.BOLD);
        panel.add(valueLabel, BorderLayout.CENTER);

        JLabel unitLabel = createStyledLabel(unit, 14, Font.PLAIN);
        panel.add(unitLabel, BorderLayout.SOUTH);

        return panel;
    }

    private JLabel createStyledLabel(String text, int fontSize, int fontStyle) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        Font font = new Font("Arial", fontStyle, fontSize);
        Map<TextAttribute, Object> attributes = new HashMap<>(font.getAttributes());
        attributes.put(TextAttribute.KERNING, TextAttribute.KERNING_ON);
        label.setFont(font.deriveFont(attributes));
        return label;
    }

    private void updateDashboard() {
        Component[] components = dashboardPanel.getComponents();
        for (Component component : components) {
            if (component instanceof JPanel && ((JPanel) component).getLayout() instanceof GridLayout) {
                JPanel statsPanel = (JPanel) component;
                for (Component statComponent : statsPanel.getComponents()) {
                    if (statComponent instanceof JPanel) {
                        JPanel statPanel = (JPanel) statComponent;
                        JLabel titleLabel = (JLabel) statPanel.getComponent(0);
                        JLabel valueLabel = (JLabel) statPanel.getComponent(1);
                        
                        switch (titleLabel.getText()) {
                            case "Available Spaces":
                                valueLabel.setText(String.valueOf(availableSpaces));
                                break;
                            case "Occupied Spaces":
                                valueLabel.setText(String.valueOf(TOTAL_SPACES - availableSpaces));
                                break;
                            case "Average Parking Time":
                                double avgParkingTime = totalParkedVehicles > 0 ? (double) totalParkingTime / totalParkedVehicles : 0;
                                valueLabel.setText(String.format("%.2f", avgParkingTime));
                                break;
                            case "Today's Revenue":
                                valueLabel.setText(String.format("$%.2f", todayRevenue));
                                break;
                        }
                    }
                }
            }
        }
    }

    private void showDashboard() {
        updateDashboard();
        cardLayout.show(contentPanel, "Dashboard");
    }

    private void showParkingLot() {
        cardLayout.show(contentPanel, "ParkingLot");
    }

    private void showRevenue() {
        cardLayout.show(contentPanel, "Revenue");
    }

    private void showParkingRates() {
        cardLayout.show(contentPanel, "ParkingRates");
    }

    private void showSettings() {
        cardLayout.show(contentPanel, "Settings");
    }

    private void createHistoryPanel() {
        String[] columnNames = {"Date", "Plate Number", "Vehicle Type", "Space Number", "Entry Time", "Exit Time", "Duration", "Fare"};
        historyTableModel = new DefaultTableModel(columnNames, 0);
        historyTable = new JTable(historyTableModel);
        historyTable.setFillsViewportHeight(true);

        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setPreferredSize(new Dimension(800, 300));
        
        JPanel historyPanel = new JPanel(new BorderLayout());
        historyPanel.add(scrollPane, BorderLayout.CENTER);
        
        JButton refreshButton = new JButton("Refresh History");
        refreshButton.addActionListener(e -> updateHistoryPanel());
        historyPanel.add(refreshButton, BorderLayout.SOUTH);
        
        contentPanel.add(historyPanel, "History");
    }

    private void updateHistoryPanel() {
        historyTableModel.setRowCount(0); // Clear existing rows
        try {
            List<String> lines = Files.readAllLines(Paths.get(PARKED_FILE));
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String licensePlate = parts[0];
                    String vehicleType = parts[1];
                    int spaceNumber = Integer.parseInt(parts[2]);
                    long entryTime = Long.parseLong(parts[3]);
                    
                    LocalDateTime entryDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(entryTime), ZoneId.systemDefault());
                    String formattedDate = entryDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String formattedEntryTime = entryDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                    
                    String exitTime = "-";
                    String duration = "-";
                    String fare = "-";
                    
                    if (parts.length == 6) {
                        // Vehicle has left, update exit time, duration, and fare
                        long exitTimeMillis = Long.parseLong(parts[4]);
                        LocalDateTime exitDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(exitTimeMillis), ZoneId.systemDefault());
                        exitTime = exitDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                        duration = String.format("%.2f", (exitTimeMillis - entryTime) / (60.0 * 60 * 1000)) + " hours";
                        fare = "$" + parts[5];
                    }
                    
                    historyTableModel.addRow(new Object[]{
                        formattedDate,
                        licensePlate,
                        vehicleType,
                        spaceNumber,
                        formattedEntryTime,
                        exitTime,
                        duration,
                        fare
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error reading parking history: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateLotFile() {
        try {
            List<String> lotStatus = new ArrayList<>();
            for (Component component : parkingLot.getComponents()) {
                ParkingSpace space = (ParkingSpace) component;
                String status = space.getSpaceNumber() + "," +
                                (space.isOccupied() ? "occupied" : "available") + "," +
                                (space.getVehicleType() != null ? space.getVehicleType() : "") + "," +
                                (space.getLicensePlate() != null ? space.getLicensePlate() : "") + "," +
                                (space.getEntryTime() != 0 ? space.getEntryTime() : "");
                lotStatus.add(status);
            }
            Files.write(Paths.get(LOT_FILE), lotStatus, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating lot file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadLotStatus() {
        try {
            if (Files.exists(Paths.get(LOT_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(LOT_FILE));
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 5) {
                        int spaceNumber = Integer.parseInt(parts[0]);
                        boolean isOccupied = parts[1].equals("occupied");
                        String vehicleType = parts[2].isEmpty() ? null : parts[2];
                        String licensePlate = parts[3].isEmpty() ? null : parts[3];
                        long entryTime = parts[4].isEmpty() ? 0 : Long.parseLong(parts[4]);

                        ParkingSpace space = (ParkingSpace) parkingLot.getComponent(spaceNumber - 1);
                        if (isOccupied) {
                            space.occupy(vehicleType, licensePlate);
                            space.setEntryTime(entryTime);
                            parkedCars.put(licensePlate, space);
                            availableSpaces--;
                        }
                    }
                }
                updateStatus();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading lot status: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createRevenuePanel() {
        revenuePanel = new JPanel(new BorderLayout(10, 10));
        revenuePanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = createStyledLabel("Revenue", 24, Font.BOLD);
        revenuePanel.add(titleLabel, BorderLayout.NORTH);

        revenueTabbedPane = new JTabbedPane();

        // Daily Revenue
        dailyRevenueTableModel = new DefaultTableModel(new String[]{"Date", "Total Revenue"}, 0);
        dailyRevenueTable = new JTable(dailyRevenueTableModel);
        JScrollPane dailyScrollPane = new JScrollPane(dailyRevenueTable);
        revenueTabbedPane.addTab("Daily", dailyScrollPane);

        // Weekly Revenue
        weeklyRevenueTableModel = new DefaultTableModel(new String[]{"Week", "Total Revenue"}, 0);
        weeklyRevenueTable = new JTable(weeklyRevenueTableModel);
        JScrollPane weeklyScrollPane = new JScrollPane(weeklyRevenueTable);
        revenueTabbedPane.addTab("Weekly", weeklyScrollPane);

        // Monthly Revenue
        monthlyRevenueTableModel = new DefaultTableModel(new String[]{"Month", "Total Revenue"}, 0);
        monthlyRevenueTable = new JTable(monthlyRevenueTableModel);
        JScrollPane monthlyScrollPane = new JScrollPane(monthlyRevenueTable);
        revenueTabbedPane.addTab("Monthly", monthlyScrollPane);

        revenuePanel.add(revenueTabbedPane, BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh Revenue Data");
        refreshButton.addActionListener(e -> refreshRevenueData());
        revenuePanel.add(refreshButton, BorderLayout.SOUTH);

        // Initial data load
        loadRevenueData();
        refreshRevenueData();
    }

    private void loadRevenueData() {
        revenueData.clear();
        try {
            if (Files.exists(Paths.get(REVENUE_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(REVENUE_FILE));
                for (String line : lines) {
                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        LocalDate date = LocalDate.parse(parts[0]);
                        double revenue = Double.parseDouble(parts[1]);
                        revenueData.put(date, revenue);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading revenue data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void refreshRevenueData() {
        refreshDailyRevenueData();
        refreshWeeklyRevenueData();
        refreshMonthlyRevenueData();
    }

    private void refreshDailyRevenueData() {
        dailyRevenueTableModel.setRowCount(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (Map.Entry<LocalDate, Double> entry : revenueData.entrySet()) {
            String formattedDate = entry.getKey().format(formatter);
            String formattedRevenue = String.format("$%.2f", entry.getValue());
            dailyRevenueTableModel.addRow(new Object[]{formattedDate, formattedRevenue});
        }
    }

    private void refreshWeeklyRevenueData() {
        weeklyRevenueTableModel.setRowCount(0);
        Map<Integer, Double> weeklyRevenue = new TreeMap<>();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (Map.Entry<LocalDate, Double> entry : revenueData.entrySet()) {
            int weekNumber = entry.getKey().get(weekFields.weekOfWeekBasedYear());
            weeklyRevenue.merge(weekNumber, entry.getValue(), Double::sum);
        }

        for (Map.Entry<Integer, Double> entry : weeklyRevenue.entrySet()) {
            String weekLabel = "Week " + entry.getKey();
            String formattedRevenue = String.format("$%.2f", entry.getValue());
            weeklyRevenueTableModel.addRow(new Object[]{weekLabel, formattedRevenue});
        }
    }

    private void refreshMonthlyRevenueData() {
        monthlyRevenueTableModel.setRowCount(0);
        Map<YearMonth, Double> monthlyRevenue = new TreeMap<>();

        for (Map.Entry<LocalDate, Double> entry : revenueData.entrySet()) {
            YearMonth yearMonth = YearMonth.from(entry.getKey());
            monthlyRevenue.merge(yearMonth, entry.getValue(), Double::sum);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM yyyy");
        for (Map.Entry<YearMonth, Double> entry : monthlyRevenue.entrySet()) {
            String monthLabel = entry.getKey().format(formatter);
            String formattedRevenue = String.format("$%.2f", entry.getValue());
            monthlyRevenueTableModel.addRow(new Object[]{monthLabel, formattedRevenue});
        }
    }

    private void updateRevenue(double amount) {
        LocalDate today = LocalDate.now();
        todayRevenue += amount;
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(REVENUE_FILE));
            boolean updated = false;
            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split(",");
                if (parts.length == 2 && LocalDate.parse(parts[0]).equals(today)) {
                    lines.set(i, today + "," + todayRevenue);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                lines.add(today + "," + todayRevenue);
            }
            Files.write(Paths.get(REVENUE_FILE), lines);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating revenue: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateStatus();
    }

    private void createParkingRatesPanel() {
        parkingRatesPanel = new JPanel(new BorderLayout(10, 10));
        parkingRatesPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = createStyledLabel("Parking Rates", 24, Font.BOLD);
        parkingRatesPanel.add(titleLabel, BorderLayout.NORTH);

        // Create table model and table
        String[] columnNames = {"Vehicle Type", "Hourly Rate"};
        ratesTableModel = new DefaultTableModel(columnNames, 0);
        ratesTable = new JTable(ratesTableModel);
        ratesTable.setFillsViewportHeight(true);

        // Center-align the content in the table
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        ratesTable.setDefaultRenderer(Object.class, centerRenderer);

        JScrollPane scrollPane = new JScrollPane(ratesTable);
        parkingRatesPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton editRatesButton = new JButton("Edit Rates");
        editRatesButton.addActionListener(e -> editParkingRates());
        buttonPanel.add(editRatesButton);

        JButton refreshButton = new JButton("Refresh Rates");
        refreshButton.addActionListener(e -> refreshRatesDisplay());
        buttonPanel.add(refreshButton);

        parkingRatesPanel.add(buttonPanel, BorderLayout.SOUTH);

        refreshRatesDisplay();
    }

    private void refreshRatesDisplay() {
        ratesTableModel.setRowCount(0); // Clear existing rows
        for (Map.Entry<String, Integer> entry : hourlyRates.entrySet()) {
            String vehicleType = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            String rate = "$" + entry.getValue() + "/hour";
            ratesTableModel.addRow(new Object[]{vehicleType, rate});
        }
    }

    private void editParkingRates() {
        JPanel panel = new JPanel(new GridLayout(hourlyRates.size(), 2, 5, 5));
        Map<String, JTextField> rateFields = new HashMap<>();

        for (String vehicleType : hourlyRates.keySet()) {
            panel.add(new JLabel(vehicleType.substring(0, 1).toUpperCase() + vehicleType.substring(1) + " rate:"));
            JTextField field = new JTextField(hourlyRates.get(vehicleType).toString(), 5);
            panel.add(field);
            rateFields.put(vehicleType, field);
        }

        int result = JOptionPane.showConfirmDialog(this, panel, "Edit Parking Rates", 
                                                   JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            boolean ratesChanged = false;
            for (Map.Entry<String, JTextField> entry : rateFields.entrySet()) {
                try {
                    int newRate = Integer.parseInt(entry.getValue().getText());
                    if (newRate != hourlyRates.get(entry.getKey())) {
                        hourlyRates.put(entry.getKey(), newRate);
                        ratesChanged = true;
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid input for " + entry.getKey() + " rate. Please enter a number.");
                }
            }
            if (ratesChanged) {
                saveParkingRates();
                refreshRatesDisplay(); // Refresh the rates display
                JOptionPane.showMessageDialog(this, "Parking rates updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void saveParkingRates() {
        try {
            List<String> lines = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : hourlyRates.entrySet()) {
                lines.add(entry.getKey() + ": " + entry.getValue());
            }
            Files.write(Paths.get(RATE_FILE), lines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error saving parking rates: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSettingsPanel() {
        settingsPanel = new JPanel(new GridBagLayout());
        settingsPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel titleLabel = createStyledLabel("Settings", 24, Font.BOLD);
        settingsPanel.add(titleLabel, gbc);

        JButton changeColorSchemeButton = new JButton("Change Color Scheme");
        changeColorSchemeButton.addActionListener(e -> changeColorScheme());
        settingsPanel.add(changeColorSchemeButton, gbc);

        JButton changeFontButton = new JButton("Change Font");
        changeFontButton.addActionListener(e -> changeFont());
        settingsPanel.add(changeFontButton, gbc);

        JButton resetSettingsButton = new JButton("Reset to Default Settings");
        resetSettingsButton.addActionListener(e -> resetSettings());
        settingsPanel.add(resetSettingsButton, gbc);
    }

    private void changeColorScheme() {
        Color newColor = JColorChooser.showDialog(this, "Choose Background Color", getBackground());
        if (newColor != null) {
            setBackground(newColor);
            updateComponentColors(this, newColor, getForeground());
            // Update specific components that might need different treatment
            for (Component comp : parkingLot.getComponents()) {
                if (comp instanceof ParkingSpace) {
                    ParkingSpace space = (ParkingSpace) comp;
                    if (space.isOccupied()) {
                        space.setBackground(Color.RED);
                    } else {
                        space.setBackground(Color.GREEN);
                    }
                }
            }
        }
    }

    private void changeFont() {
        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String selectedFont = (String) JOptionPane.showInputDialog(this, "Choose a font:", "Font Selection",
                JOptionPane.PLAIN_MESSAGE, null, fontNames, getFont().getFamily());
        if (selectedFont != null) {
            Font newFont = new Font(selectedFont, Font.PLAIN, 12);
            updateComponentFonts(this, newFont);
        }
    }

    private void resetSettings() {
        // Reset background color
        setBackground(DEFAULT_BACKGROUND_COLOR);
        updateComponentColors(this, DEFAULT_BACKGROUND_COLOR, DEFAULT_FOREGROUND_COLOR);

        // Reset font
        updateComponentFonts(this, DEFAULT_FONT);

        // Reset dark mode
        isDarkMode = false;
        modeToggleButton.setText("Dark Mode");

        // Reset parking space colors
        resetParkingSpaceColors();

        // Reset other components
        resetSpecificComponents();

        // Update the UI
        SwingUtilities.updateComponentTreeUI(this);

        JOptionPane.showMessageDialog(this, "Settings have been reset to default.");
    }

    private void resetParkingSpaceColors() {
        for (Component comp : parkingLot.getComponents()) {
            if (comp instanceof ParkingSpace) {
                ParkingSpace space = (ParkingSpace) comp;
                if (space.isOccupied()) {
                    space.setBackground(Color.RED);
                } else {
                    space.setBackground(Color.GREEN);
                }
            }
        }
    }

    private void resetSpecificComponents() {
        // Reset specific components that might need special treatment
        dashboardPanel.setBackground(DEFAULT_BACKGROUND_COLOR);
        revenuePanel.setBackground(DEFAULT_BACKGROUND_COLOR);
        parkingRatesPanel.setBackground(DEFAULT_BACKGROUND_COLOR);
        settingsPanel.setBackground(DEFAULT_BACKGROUND_COLOR);

        // Reset table colors
        resetTableColors(historyTable);
        resetTableColors(dailyRevenueTable);
        resetTableColors(weeklyRevenueTable);
        resetTableColors(monthlyRevenueTable);
        resetTableColors(ratesTable);
    }

    private void resetTableColors(JTable table) {
        table.setBackground(Color.WHITE);
        table.setForeground(Color.BLACK);
        table.setSelectionBackground(new Color(184, 207, 229));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(Color.LIGHT_GRAY);
    }

    private void loadParkingRates() {
        try {
            if (Files.exists(Paths.get(RATE_FILE))) {
                List<String> lines = Files.readAllLines(Paths.get(RATE_FILE));
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String vehicleType = parts[0].trim().toLowerCase();
                        int rate = Integer.parseInt(parts[1].trim());
                        hourlyRates.put(vehicleType, rate);
                    }
                }
            } else {
                // If the file doesn't exist, create it with default rates
                hourlyRates.put("car", 50);
                hourlyRates.put("motor", 30);
                hourlyRates.put("truck", 80);
                saveParkingRates();
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading parking rates: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTodayRevenue() {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        try {
            List<String> lines = Files.readAllLines(Paths.get(REVENUE_FILE));
            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    LocalDate date = LocalDate.parse(parts[0], formatter);
                    if (date.equals(today)) {
                        todayRevenue = Double.parseDouble(parts[1]);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading today's revenue: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        updateStatus();
    }

    private void createFrontPage() {
        frontPage = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = createStyledLabel("Parking System", 36, Font.BOLD);
        frontPage.add(titleLabel, gbc);

        JButton customerButton = new JButton("Customer");
        customerButton.addActionListener(e -> enterCustomerMode());
        frontPage.add(customerButton, gbc);

        JButton adminButton = new JButton("Admin");
        adminButton.addActionListener(e -> enterAdminMode());
        frontPage.add(adminButton, gbc);
    }

    private void showFrontPage() {
        getContentPane().removeAll();
        getContentPane().add(frontPage);
        revalidate();
        repaint();
    }

    private void enterCustomerMode() {
        isAdminMode = false;
        getContentPane().removeAll();
        initializeComponents();
        showParkingLot();
        updateControlPanel();
        revalidate();
        repaint();
    }

    private void enterAdminMode() {
        String password = JOptionPane.showInputDialog(this, "Enter admin password:");
        if ("admin123".equals(password)) {
            isAdminMode = true;
            getContentPane().removeAll();
            initializeComponents();
            showParkingLot();
            updateControlPanel();
            revalidate();
            repaint();
        } else {
            JOptionPane.showMessageDialog(this, "Incorrect password. Access denied.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateControlPanel() {
        // Remove existing control panel
        Component[] components = getContentPane().getComponents();
        for (Component component : components) {
            if (component instanceof JPanel && ((JPanel) component).getBorder() != null &&
                ((JPanel) component).getBorder().toString().contains("Controls")) {
                getContentPane().remove(component);
                break;
            }
        }

        // Create new control panel based on mode
        JPanel controlPanel = new JPanel(new GridLayout(isAdminMode ? 7 : 3, 1, 10, 10));
        controlPanel.setBorder(BorderFactory.createTitledBorder("Controls"));

        if (isAdminMode) {
            // Admin mode controls
            JButton dashboardButton = createStyledButton("Dashboard", new ImageIcon("path/to/dashboard_icon.png"));
            JButton parkingLotButton = createStyledButton("Parking Lot", new ImageIcon("path/to/parking_lot_icon.png"));
            JButton historyButton = createStyledButton("History", new ImageIcon("path/to/history_icon.png"));
            JButton revenueButton = createStyledButton("Revenue", new ImageIcon("path/to/revenue_icon.png"));
            JButton editRatesButton = createStyledButton("Edit Rates", new ImageIcon("path/to/edit_rates_icon.png"));
            JButton settingsButton = createStyledButton("Settings", new ImageIcon("path/to/settings_icon.png"));

            dashboardButton.addActionListener(e -> showDashboard());
            parkingLotButton.addActionListener(e -> showParkingLot());
            historyButton.addActionListener(e -> viewHistory());
            revenueButton.addActionListener(e -> showRevenue());
            editRatesButton.addActionListener(e -> showParkingRates());
            settingsButton.addActionListener(e -> showSettings());

            controlPanel.add(dashboardButton);
            controlPanel.add(parkingLotButton);
            controlPanel.add(historyButton);
            controlPanel.add(revenueButton);
            controlPanel.add(editRatesButton);
            controlPanel.add(settingsButton);
        } else {
            // Customer mode controls
            JButton parkingLotButton = createStyledButton("Parking Lot", new ImageIcon("path/to/parking_lot_icon.png"));
            JButton viewFaresButton = createStyledButton("View Fares", new ImageIcon("path/to/fares_icon.png"));

            parkingLotButton.addActionListener(e -> showParkingLot());
            viewFaresButton.addActionListener(e -> viewFares());

            controlPanel.add(parkingLotButton);
            controlPanel.add(viewFaresButton);
        }

        // Add logout button for both modes
        JButton logoutButton = new JButton("Logout");
        logoutButton.setFont(new Font(logoutButton.getFont().getName(), Font.PLAIN, 10)); // Smaller font
        logoutButton.addActionListener(e -> showFrontPage());
        controlPanel.add(logoutButton);

        add(controlPanel, BorderLayout.EAST);
        revalidate();
        repaint();
    }

    private void viewFares() {
        StringBuilder faresInfo = new StringBuilder("Current Parking Rates:\n\n");
        for (Map.Entry<String, Integer> entry : hourlyRates.entrySet()) {
            String vehicleType = entry.getKey().substring(0, 1).toUpperCase() + entry.getKey().substring(1);
            faresInfo.append(vehicleType).append(": $").append(entry.getValue()).append("/hour\n");
        }
        JOptionPane.showMessageDialog(this, faresInfo.toString(), "Parking Rates", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ParkingSystem system = new ParkingSystem();
            system.createHistoryPanel();
        });
    }
}
