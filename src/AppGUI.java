import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

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
        frame.setSize(500, 400);
        frame.setLayout(new GridLayout(10, 2, 10, 5));

        JComboBox<String> versionBox = new JComboBox<>(VERSIONS);
        JTextField populationField = new JTextField("5");
        JTextField generationsField = new JTextField("500");
        JTextField mutationRateField = new JTextField("0.8");
        JTextField mutationNumField = new JTextField("10");
        JTextField localSearchRateField = new JTextField("0.8");
        JTextField localSearchNumField = new JTextField("10");
        JTextField replicationsField = new JTextField("10");
        JTextField outputFileField = new JTextField("vystup.txt");

        JButton runButton = new JButton("Spustiť");

        frame.add(new JLabel("Verzia vstupného súboru:"));
        frame.add(versionBox);
        frame.add(new JLabel("Population Size:"));
        frame.add(populationField);
        frame.add(new JLabel("Generations:"));
        frame.add(generationsField);
        frame.add(new JLabel("Mutation Rate:"));
        frame.add(mutationRateField);
        frame.add(new JLabel("Mutation Num:"));
        frame.add(mutationNumField);
        frame.add(new JLabel("Local Search Rate:"));
        frame.add(localSearchRateField);
        frame.add(new JLabel("Local Search Num:"));
        frame.add(localSearchNumField);
        frame.add(new JLabel("Počet replikácií:"));
        frame.add(replicationsField);
        frame.add(new JLabel("Názov výstupného súboru:"));
        frame.add(outputFileField);
        frame.add(runButton);

        runButton.addActionListener((ActionEvent e) -> {
            try {
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
                JOptionPane.showMessageDialog(frame, "Hotovo!");
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(frame, "Chyba: " + ex.getMessage());
            }
        });

        frame.setVisible(true);
    }
}