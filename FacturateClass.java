import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FacturateClass {
    private JFrame frame;
    private JPanel mainPanel, inputPanel, invoicePanel;
    private JTextField articleField, priceField;
    private JButton addButton, generateButton, modifyButton, saveButton, cancelButton, deleteButton;
    private JComboBox<String> currencyComboBox;
    private JTextArea invoiceArea;
    private JList<String> invoiceList;
    private DefaultListModel<String> listModel;

    private ArrayList<Map<String, Object>> articles = new ArrayList<>();
    private boolean editMode = false;
    private double total = 0;
    private String currentCurrency = "USD";
    private final double exchangeRate = 2800;
    private DecimalFormat df = new DecimalFormat("#,##0.00");

    public FacturateClass() {
        createUI();
    }

    private void createUI() {
        frame = new JFrame("Application de Facturation");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);
        frame.setLayout(new BorderLayout());

        mainPanel = new JPanel(new CardLayout());

        // Panel de saisie
        inputPanel = new JPanel(new BorderLayout(10, 10));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = new JPanel(new GridLayout(3, 2, 5, 5));
        formPanel.add(new JLabel("Article:"));
        articleField = new JTextField();
        formPanel.add(articleField);

        formPanel.add(new JLabel("Prix:"));
        priceField = new JTextField();
        formPanel.add(priceField);

        formPanel.add(new JLabel("Devise:"));
        currencyComboBox = new JComboBox<>(new String[]{"USD", "CDF"});
        formPanel.add(currencyComboBox);

        inputPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addButton = new JButton("Ajouter");
        addButton.addActionListener(e -> addArticle());
        buttonPanel.add(addButton);

        generateButton = new JButton("Générer Facture");
        generateButton.addActionListener(e -> generateInvoice());
        buttonPanel.add(generateButton);

        inputPanel.add(buttonPanel, BorderLayout.SOUTH);

        // Panel de facture
        invoicePanel = new JPanel(new BorderLayout(10, 10));
        invoicePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        listModel = new DefaultListModel<>();
        invoiceList = new JList<>(listModel);
        invoiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(invoiceList);

        invoiceArea = new JTextArea(10, 30);
        invoiceArea.setEditable(false);

        JPanel invoiceButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        modifyButton = new JButton("Modifier");
        modifyButton.addActionListener(e -> toggleEditMode());
        invoiceButtonPanel.add(modifyButton);

        deleteButton = new JButton("Supprimer");
        deleteButton.addActionListener(e -> deleteSelectedItem());
        deleteButton.setEnabled(false);
        invoiceButtonPanel.add(deleteButton);

        saveButton = new JButton("Enregistrer");
        saveButton.addActionListener(e -> saveChanges());
        saveButton.setEnabled(false);
        invoiceButtonPanel.add(saveButton);

        cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(e -> cancelEdit());
        cancelButton.setEnabled(false);
        invoiceButtonPanel.add(cancelButton);

        invoicePanel.add(scrollPane, BorderLayout.CENTER);
        invoicePanel.add(invoiceButtonPanel, BorderLayout.SOUTH);

        mainPanel.add(inputPanel, "input");
        mainPanel.add(invoicePanel, "invoice");

        frame.add(mainPanel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private void addArticle() {
        String article = articleField.getText().trim();
        String priceText = priceField.getText().trim();

        if (article.isEmpty() || priceText.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Veuillez entrer un article et un prix", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double price = Double.parseDouble(priceText);
            String currency = (String) currencyComboBox.getSelectedItem();

            Map<String, Object> item = new HashMap<>();
            item.put("article", article);
            item.put("price", price);
            item.put("currency", currency);

            articles.add(item);

            articleField.setText("");
            priceField.setText("");
            articleField.requestFocus();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(frame, "Prix invalide", "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generateInvoice() {
        if (articles.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Aucun article à facturer", "Erreur", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentCurrency = (String) currencyComboBox.getSelectedItem();
        updateInvoiceDisplay();

        CardLayout cl = (CardLayout) mainPanel.getLayout();
        cl.show(mainPanel, "invoice");
    }

    private void updateInvoiceDisplay() {
        listModel.clear();
        total = 0;

        for (Map<String, Object> item : articles) {
            String article = (String) item.get("article");
            double price = (double) item.get("price");
            String currency = (String) item.get("currency");

            // Convertir tous les prix à la devise actuelle
            double convertedPrice = price;
            if (!currency.equals(currentCurrency)) {
                if (currency.equals("USD") && currentCurrency.equals("CDF")) {
                    convertedPrice = price * exchangeRate;
                } else if (currency.equals("CDF") && currentCurrency.equals("USD")) {
                    convertedPrice = price / exchangeRate;
                }
            }

            total += convertedPrice;
            listModel.addElement(article + " - " + df.format(convertedPrice) + " " + currentCurrency);
        }

        // Ajouter le total
        listModel.addElement("----------------------------");
        listModel.addElement("TOTAL: " + df.format(total) + " " + currentCurrency);
    }

    private void toggleEditMode() {
        editMode = !editMode;

        modifyButton.setEnabled(!editMode);
        deleteButton.setEnabled(editMode);
        saveButton.setEnabled(editMode);
        cancelButton.setEnabled(editMode);

        if (!editMode) {
            // Retour à l'affichage normal
            updateInvoiceDisplay();
        } else {
            // Mode édition - enlever le total de la liste
            if (listModel.size() > 0 && listModel.getElementAt(listModel.size() - 1).startsWith("TOTAL:")) {
                listModel.remove(listModel.size() - 1);
                listModel.remove(listModel.size() - 1); // Supprimer la ligne de séparation
            }
        }
    }

    private void deleteSelectedItem() {
        int selectedIndex = invoiceList.getSelectedIndex();
        if (selectedIndex != -1 && selectedIndex < articles.size()) {
            articles.remove(selectedIndex);
            listModel.remove(selectedIndex);
        }
    }

    private void saveChanges() {
        // On pourrait ajouter ici la logique pour ajouter de nouveaux articles
        toggleEditMode();
    }

    private void cancelEdit() {
        toggleEditMode();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FacturateClass());
    }
}