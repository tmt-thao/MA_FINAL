import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AppGUI {
    private static final String[] VERSIONS = {
        "T1_3", "T2_3", "T3_3", "T4_3",
        "B1_3", "B2_3", "B3_3", "B4_3", "B5_3", "B6_3", "B7_3",
        "A_4"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Memetic algorithm for electric bus scheduling");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(850, 400);
        frame.setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        Dimension inputSize = new Dimension(120, 25);

        // === Dataset, Output, Season, Charging strategy ===
        JComboBox<String> versionBox = new JComboBox<>(VERSIONS);
        JTextField outputFileField = new JTextField("output.txt");
        String[] seasons = { "Spring", "Summer", "Winter" };
        JComboBox<String> seasonBox = new JComboBox<>(seasons);
        JComboBox<ChargingStrategy> strategyBox = new JComboBox<>(ChargingStrategy.values());

        versionBox.setPreferredSize(inputSize);
        outputFileField.setPreferredSize(inputSize);
        seasonBox.setPreferredSize(inputSize);
        strategyBox.setPreferredSize(inputSize);

        JPanel datasetPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        datasetPanel.add(new JLabel("Dataset:"));
        datasetPanel.add(versionBox);
        datasetPanel.add(new JLabel("Output file:"));
        datasetPanel.add(outputFileField);
        datasetPanel.add(new JLabel("Season:"));
        datasetPanel.add(seasonBox);
        datasetPanel.add(new JLabel("Charging strategy:"));
        datasetPanel.add(strategyBox);
        mainPanel.add(datasetPanel);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));

        // === Replications ===
        JTextField replicationsField = new JTextField("5");
        replicationsField.setPreferredSize(inputSize);
        JPanel repPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        repPanel.add(new JLabel("Replications:"));
        repPanel.add(replicationsField);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(repPanel);

        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));

        // === Algorithm Parameters ===
        JTextField populationField = new JTextField("5");
        JTextField generationsField = new JTextField("500");
        JTextField mutationRateField = new JTextField("0.8");
        JTextField mutationNumField = new JTextField("10");
        JTextField localSearchRateField = new JTextField("0.8");
        JTextField localSearchNumField = new JTextField("10");

        JTextField[] allFields = {
            populationField, generationsField,
            mutationRateField, mutationNumField,
            localSearchRateField, localSearchNumField
        };
        for (JTextField field : allFields) {
            field.setPreferredSize(inputSize);
        }

        JPanel paramGrid = new JPanel(new GridLayout(2, 6, 10, 10));
        paramGrid.add(new JLabel("Population size:"));
        paramGrid.add(populationField);
        paramGrid.add(new JLabel("Mutation rate:"));
        paramGrid.add(mutationRateField);
        paramGrid.add(new JLabel("Local search rate:"));
        paramGrid.add(localSearchRateField);
        paramGrid.add(new JLabel("Generations:"));
        paramGrid.add(generationsField);
        paramGrid.add(new JLabel("Mutation number:"));
        paramGrid.add(mutationNumField);
        paramGrid.add(new JLabel("Local search number:"));
        paramGrid.add(localSearchNumField);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(paramGrid);

        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        mainPanel.add(Box.createVerticalStrut(10));

        // === Button ===
        JButton runButton = new JButton("Start");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(runButton);
        mainPanel.add(buttonPanel);

        frame.setContentPane(mainPanel);
        frame.setVisible(true);

        // === Run button logic ===
        runButton.addActionListener((ActionEvent e) -> {
            JDialog loadingDialog = new JDialog(frame, "Running algorithm...", true);
            JLabel loadingLabel = new JLabel("Please wait...");
            loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
            loadingDialog.add(BorderLayout.CENTER, loadingLabel);
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(frame);

            new Thread(() -> {
                try {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

                    // === Battery settings based on season ===
                    String season = (String) seasonBox.getSelectedItem();
                    switch (season) {
                        case "Spring":
                            StaticData.CONSUMPTION_PER_KM = 1.5;
                            StaticData.MAX_BATTERY = 125.0;
                            break;
                        case "Summer":
                            StaticData.CONSUMPTION_PER_KM = 2.0;
                            StaticData.MAX_BATTERY = 125.0;
                            break;
                        case "Winter":
                            StaticData.CONSUMPTION_PER_KM = 2.0;
                            StaticData.MAX_BATTERY = 100.0;
                            break;
                    }

                    // === Charging strategy from dropdown ===
                    ChargingStrategy selectedStrategy = (ChargingStrategy) strategyBox.getSelectedItem();
                    StaticData.chargingStrategy = selectedStrategy;

                    // === Algorithm parameters ===
                    String version = (String) versionBox.getSelectedItem();
                    int populationSize = Integer.parseInt(populationField.getText());
                    int generations = Integer.parseInt(generationsField.getText());
                    double mutationRate = Double.parseDouble(mutationRateField.getText());
                    int mutationNum = Integer.parseInt(mutationNumField.getText());
                    double localSearchRate = Double.parseDouble(localSearchRateField.getText());
                    int localSearchNum = Integer.parseInt(localSearchNumField.getText());
                    int replications = Integer.parseInt(replicationsField.getText());
                    String outputFile = outputFileField.getText();

                    Main.runWithParams(version, replications, populationSize, generations,
                            mutationRate, mutationNum, localSearchRate, localSearchNum, outputFile);

                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(frame, "Done! Results saved to " + outputFile);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(frame, "Error: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }
}
