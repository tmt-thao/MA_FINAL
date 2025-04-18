import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class AppGUI {
    private static final String[] VERSIONS = {
        "T1_3", "T2_3", "T3_3", "T4_3",
        "B1_3", "B2_3", "B3_3", "B4_3",
        "A_4"
    };

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AppGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Memetický algoritmus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 450);
        frame.setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new GridLayout(9, 2, 10, 5));
        JComboBox<String> versionBox = new JComboBox<>(VERSIONS);
        JTextField populationField = new JTextField("5");
        JTextField generationsField = new JTextField("500");
        JTextField mutationRateField = new JTextField("0.8");
        JTextField mutationNumField = new JTextField("10");
        JTextField localSearchRateField = new JTextField("0.8");
        JTextField localSearchNumField = new JTextField("10");
        JTextField replicationsField = new JTextField("10");
        JTextField outputFileField = new JTextField("vystup.txt");

        inputPanel.add(new JLabel("Verzia vstupného súboru:"));
        inputPanel.add(versionBox);
        inputPanel.add(new JLabel("Population Size:"));
        inputPanel.add(populationField);
        inputPanel.add(new JLabel("Generations:"));
        inputPanel.add(generationsField);
        inputPanel.add(new JLabel("Mutation Rate:"));
        inputPanel.add(mutationRateField);
        inputPanel.add(new JLabel("Mutation Num:"));
        inputPanel.add(mutationNumField);
        inputPanel.add(new JLabel("Local Search Rate:"));
        inputPanel.add(localSearchRateField);
        inputPanel.add(new JLabel("Local Search Num:"));
        inputPanel.add(localSearchNumField);
        inputPanel.add(new JLabel("Počet replikácií:"));
        inputPanel.add(replicationsField);
        inputPanel.add(new JLabel("Názov výstupného súboru:"));
        inputPanel.add(outputFileField);

        JButton runButton = new JButton("Spustiť");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(runButton);

        frame.add(inputPanel, BorderLayout.CENTER);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        // OTVORENIE GUI V STREDE OBRAZOVKY
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        runButton.addActionListener((ActionEvent e) -> {
            // Vytvoríme loading dialog
            JDialog loadingDialog = new JDialog(frame, "Prebieha výpočet...", true);
            JLabel loadingLabel = new JLabel("Algoritmus beží, čakajte prosím...");
            loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
            loadingDialog.add(BorderLayout.CENTER, loadingLabel);
            loadingDialog.setSize(300, 100);
            loadingDialog.setLocationRelativeTo(frame);

            // Spustíme algoritmus v novom vlákne, aby GUI nezamrzlo
            new Thread(() -> {
                try {
                    SwingUtilities.invokeLater(() -> loadingDialog.setVisible(true));

                    String version = (String) versionBox.getSelectedItem();
                    int populationSize = Integer.parseInt(populationField.getText());
                    int generations = Integer.parseInt(generationsField.getText());
                    double mutationRate = Double.parseDouble(mutationRateField.getText());
                    int mutationNum = Integer.parseInt(mutationNumField.getText());
                    double localSearchRate = Double.parseDouble(localSearchRateField.getText());
                    int localSearchNum = Integer.parseInt(localSearchNumField.getText());
                    int replications = Integer.parseInt(replicationsField.getText());
                    String outputFile = outputFileField.getText();

                    Main.runWithParams(version, replications, populationSize, generations, mutationRate, mutationNum, localSearchRate, localSearchNum, outputFile);

                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(frame, "Hotovo! Výsledok je v súbore: " + outputFile);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(frame, "Chyba: " + ex.getMessage());
                    });
                }
            }).start();
        });
    }
}
