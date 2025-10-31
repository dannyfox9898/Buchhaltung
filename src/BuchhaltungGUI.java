import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class BuchhaltungGUI {

    private JFrame frame;
    private JPanel mainPanel;
    private JTextField vonDatumTxt;
    private JTable transactionsTable;
    private JButton addBtn;
    private JButton saveBtn;
    private JLabel datumLabel;
    private JTextField bisDatumTxt;
    private TransaktionDAO transactionDAO;
    private JLabel summaryLabel;

    public BuchhaltungGUI() {
        try {
            Connection conn = Database.getConnection();
            transactionDAO = new TransaktionDAO(conn);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Fehler beim Verbinden mit der Datenbank: " + e.getMessage());
            return;
        }

        frame = new JFrame("Haushaltskosten App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel();
        vonDatumTxt = new JTextField(LocalDate.now().withDayOfMonth(1).toString(), 10);
        topPanel.add(new JLabel("Von (YYYY-MM-DD):"));
        topPanel.add(vonDatumTxt);

        bisDatumTxt = new JTextField(LocalDate.now().toString(), 10);
        topPanel.add(new JLabel("Bis (YYYY-MM-DD):"));
        topPanel.add(bisDatumTxt);

        JButton loadBtn = new JButton("Laden");
        topPanel.add(loadBtn);

        addBtn = new JButton("Neue Transaktion");
        JButton editBtn = new JButton("Bearbeiten");
        JButton deleteBtn = new JButton("Löschen");
        topPanel.add(addBtn);
        topPanel.add(editBtn);
        topPanel.add(deleteBtn);

        mainPanel.add(topPanel, BorderLayout.NORTH);

        transactionsTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(transactionsTable);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        summaryLabel = new JLabel("Einnahmen: 0.00 € | Ausgaben: 0.00 €");
        summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(summaryLabel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        loadBtn.addActionListener(e -> refreshTable(vonDatumTxt.getText(), bisDatumTxt.getText()));
        addBtn.addActionListener(e -> openAddTransactionDialog());
        editBtn.addActionListener(e -> editSelectedTransaction());
        deleteBtn.addActionListener(e -> deleteSelectedTransaction());

        refreshTable(vonDatumTxt.getText(), bisDatumTxt.getText());
    }

    private void editSelectedTransaction() {
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Bitte zuerst eine Zeile auswählen!");
            return;
        }

        int realId = (int) transactionsTable.getValueAt(selectedRow, 0);
        String dateStr = transactionsTable.getValueAt(selectedRow, 1).toString();
        String category = transactionsTable.getValueAt(selectedRow, 2).toString();
        String type = transactionsTable.getValueAt(selectedRow, 3).toString();
        String amountStr = transactionsTable.getValueAt(selectedRow, 4).toString();
        String desc = transactionsTable.getValueAt(selectedRow, 5).toString();


        LocalDate date = LocalDate.parse(dateStr);

        if (!date.equals(LocalDate.now())) {
            JOptionPane.showMessageDialog(frame, "Datensatz darf nur am selben Tag geändert werden!");
            return;
        }

        JDialog dialog = new JDialog(frame, "Transaktion bearbeiten", true);
        dialog.setSize(350, 300);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        JTextField dateField = new JTextField(dateStr);
        JTextField categoryField = new JTextField(category);
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Einnahme", "Ausgabe"});
        typeBox.setSelectedItem(type);
        JTextField amountField = new JTextField(amountStr);
        JTextField descField = new JTextField(desc);

        JButton saveBtn = new JButton("Änderungen speichern");

        saveBtn.addActionListener(ev -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                LocalDate d = LocalDate.parse(dateField.getText());
                String updatedCategory = categoryField.getText();
                String updatedType = (String) typeBox.getSelectedItem();
                String updatedDesc = descField.getText();

                if (!d.equals(LocalDate.now())) {
                    JOptionPane.showMessageDialog(dialog, "Nur heutige Datensätze dürfen geändert werden!");
                    return;
                }

                Transaktion updated = new Transaktion(
                        realId,
                        d,
                        updatedCategory,
                        updatedType,
                        amount,
                        updatedDesc
                );

                System.out.println("DEBUG: Updating transaction: id=" + updated.getId() +
                        ", category=" + updated.getCategory() +
                        ", type=" + updated.getType() +
                        ", amount=" + updated.getAmount() +
                        ", desc=" + updated.getDescription() +
                        ", date=" + updated.getDate());

                transactionDAO.updateTransaction(updated);

                refreshTable(vonDatumTxt.getText(), bisDatumTxt.getText());
                dialog.dispose();

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Betrag muss eine Zahl sein!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Fehler: " + ex.getMessage());
            }
        });

        dialog.add(new JLabel("Datum:")); dialog.add(dateField);
        dialog.add(new JLabel("Kategorie:")); dialog.add(categoryField);
        dialog.add(new JLabel("Typ:")); dialog.add(typeBox);
        dialog.add(new JLabel("Betrag:")); dialog.add(amountField);
        dialog.add(new JLabel("Beschreibung:")); dialog.add(descField);
        dialog.add(saveBtn);

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }


    private void deleteSelectedTransaction() {
        int selectedRow = transactionsTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Bitte zuerst eine Zeile auswählen!");
            return;
        }

        int id = (int) transactionsTable.getValueAt(selectedRow, 0);
        String dateStr = transactionsTable.getValueAt(selectedRow, 1).toString();
        LocalDate date = LocalDate.parse(dateStr);

        if (!date.equals(LocalDate.now())) {
            JOptionPane.showMessageDialog(frame, "Datensatz darf nur am selben Tag gelöscht werden!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(frame, "Wirklich löschen?", "Bestätigung", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                transactionDAO.deleteTransaction(id);
                refreshTable(vonDatumTxt.getText(), bisDatumTxt.getText());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(frame, "Fehler beim Löschen: " + ex.getMessage());
            }
        }
    }



    private void openAddTransactionDialog() {
        JDialog dialog = new JDialog(frame, "Neue Transaktion", true);
        dialog.setSize(350, 300);
        dialog.setLayout(new GridLayout(6, 2, 5, 5));

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Einnahme", "Ausgabe"});
        JTextField categoryField = new JTextField();
        JTextField amountField = new JTextField();
        JTextField descField = new JTextField();
        JTextField dateField = new JTextField(LocalDate.now().toString());

        JButton saveBtn = new JButton("Speichern");

        saveBtn.addActionListener(ev -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String type = (String) typeBox.getSelectedItem();
                String category = categoryField.getText();
                String desc = descField.getText();
                LocalDate date = LocalDate.parse(dateField.getText());

                System.out.println("DEBUG: Creating new transaction with values:");
                System.out.println("Date: " + date);
                System.out.println("Category: " + category);
                System.out.println("Type: " + type);
                System.out.println("Amount: " + amount);
                System.out.println("Description: " + desc);

                Transaktion t = new Transaktion(0, date, category, type, amount, desc);

                transactionDAO.addTransaction(t);
                refreshTable(vonDatumTxt.getText(), bisDatumTxt.getText());
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Betrag muss eine Zahl sein!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Fehler: " + ex.getMessage());
            }
        });

        dialog.add(new JLabel("Datum:")); dialog.add(dateField);
        dialog.add(new JLabel("Kategorie:")); dialog.add(categoryField);
        dialog.add(new JLabel("Typ:")); dialog.add(typeBox);
        dialog.add(new JLabel("Betrag:")); dialog.add(amountField);
        dialog.add(new JLabel("Beschreibung:")); dialog.add(descField);
        dialog.add(saveBtn);

        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private void refreshTable(String startDateStr, String endDateStr) {
        List<Transaktion> transactions = null;

        try {
            LocalDate startDate = LocalDate.parse(startDateStr);
            LocalDate endDate = LocalDate.parse(endDateStr);

            transactions = transactionDAO.getTransactionsByDateRange(startDate, endDate);

            String[] columnNames = {"ID", "Datum", "Kategorie", "Typ", "Betrag", "Beschreibung"};
            Object[][] data = new Object[transactions.size()][6];

            for (int i = 0; i < transactions.size(); i++) {
                Transaktion t = transactions.get(i);
                data[i][0] = t.getId();
                data[i][1] = t.getDate().toString();
                data[i][2] = t.getCategory();
                data[i][3] = t.getType();
                data[i][4] = t.getAmount();
                data[i][5] = t.getDescription();
            }

            transactionsTable.setModel(new DefaultTableModel(data, columnNames) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            });
        } catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Fehler beim Laden der Daten: " + e.getMessage());
            return;
        }

        if (transactions != null) {
            double totalEinnahmen = transactions.stream()
                    .filter(t -> t.getType().equalsIgnoreCase("Einnahme"))
                    .mapToDouble(Transaktion::getAmount)
                    .sum();

            double totalAusgaben = transactions.stream()
                    .filter(t -> t.getType().equalsIgnoreCase("Ausgabe"))
                    .mapToDouble(Transaktion::getAmount)
                    .sum();

            summaryLabel.setText("Einnahmen: " + totalEinnahmen + " € | Ausgaben: " + totalAusgaben + " €");
        }
    }
}
