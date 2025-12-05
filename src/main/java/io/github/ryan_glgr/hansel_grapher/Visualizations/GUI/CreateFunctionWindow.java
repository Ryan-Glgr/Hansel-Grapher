package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import com.formdev.flatlaf.FlatIntelliJLaf;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.Interview;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.InterviewMode;
import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview.MagicFunctionMode;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class CreateFunctionWindow {

    private JPanel mainPanel;
    private JLabel titleLabel;
    private JPanel titlePanel;
    private JPanel submitButtonPanel;
    private JButton submitButton;
    private JPanel inputTypePanel;
    private JPanel userInputPanel;
    private JScrollPane userInputScrollPane;
    private JButton enterClassesButton;
    private JButton attributesButton;
    private JButton questionAskingTechniqueButton;
    private JButton interviewModeButton;
    private JSplitPane mainArea;
    private JButton subFunctionsButton;

    // our four panels which correspond to each button we can hit.
    private JPanel classificationPanel;
    private JPanel attributePanel;
    private JPanel questionAskingTechniquePanel;
    private JPanel interviewModePanel;
    private JPanel subFunctionsPanel;

    private String[] classificationNames;
    private String[] attributeNames;
    private Integer[] attributeKValues;
    private InterviewMode interviewMode;
    private Float[] attributeWeights;
    private Interview[] subFunctionsForEachAttribute;
    private MagicFunctionMode magicFunctionMode;

    private boolean classificationNamesConfirmed;
    private boolean weightsConfirmed;

    private CompletableFuture<Interview> interviewFuture;
    private CompletableFuture<Interview> interviewCreationTask;
    private JDialog dialog;

    public CompletableFuture<Interview> createFunctionAndReturnInterviewObject(String title){

        if (SwingUtilities.isEventDispatchThread()) {
            openDialog(title);
            return interviewFuture;
        }

        try {
            SwingUtilities.invokeAndWait(() -> openDialog(title));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while displaying CreateFunctionWindow", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Failed to display CreateFunctionWindow", ex.getCause());
        }

        return interviewFuture;
    }

    private void openDialog(String title) {
        if (interviewFuture != null && !interviewFuture.isDone()) {
            interviewFuture.cancel(true);
        }
        interviewFuture = new CompletableFuture<>();
        interviewCreationTask = null;

        if (dialog != null && dialog.isDisplayable()) {
            dialog.dispose();
        }

        if (mainPanel == null) {
            throw new IllegalStateException("CreateFunctionWindow UI has not been initialized.");
        }

        Container parent = mainPanel.getParent();
        if (parent != null) {
            parent.remove(mainPanel);
        }

        Frame owner = null;
        Window activeWindow = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        if (activeWindow instanceof Frame) {
            owner = (Frame) activeWindow;
        }

        dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (interviewCreationTask != null && !interviewCreationTask.isDone()) {
                    interviewCreationTask.cancel(true);
                }
                if (interviewFuture != null && !interviewFuture.isDone()) {
                    interviewFuture.complete(null);
                }
                JDialog currentDialog = dialog;
                if (currentDialog != null) {
                    currentDialog.dispose();
                }
            }
        });

        dialog.setContentPane(mainPanel);
        dialog.setMinimumSize(new Dimension(720, 540));
        dialog.setPreferredSize(new Dimension(720, 540));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        dialog = null;
    }

    private JPanel createClassificationPanel() {
        // Main panel with border layout
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Control panel with GridBagLayout for precise control
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Class Configuration"));
        controlPanel.setPreferredSize(new Dimension(350, 500));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Input panel for number of classes
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Number of Classes"));
        
        // Input field and label
        JLabel numClassesLabel = new JLabel("Number of classes:");
        JTextField numClassesField = new JTextField(5);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        inputPanel.add(numClassesLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        inputPanel.add(numClassesField, gbc);
        
        // Add input panel to control panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        controlPanel.add(inputPanel, gbc);
        
        // Generate button
        JButton generateButton = new JButton("Generate Class Fields");
        generateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, generateButton.getPreferredSize().height));
        
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(15, 5, 5, 5);
        buttonPanel.add(generateButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(buttonPanel, gbc);

        // Display panel for class names
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder("Class Names"));
        
        // Panel to hold the classification name fields
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        // Add components to main panel using split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, displayPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.3);
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Add some vertical glue to push content to the top
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        controlPanel.add(Box.createVerticalGlue(), gbc);
        
        // Set initial focus
        numClassesField.requestFocusInWindow();

        generateButton.addActionListener(e -> {
            try {
                int numClasses = Integer.parseInt(numClassesField.getText().trim());
                if (numClasses <= 0) {
                    JOptionPane.showMessageDialog(panel, "Please enter a positive number",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Clear previous fields
                fieldsPanel.removeAll();
                classificationNames = new String[numClasses];
                
                // Create a panel for the form
                JPanel formPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbcForm = new GridBagConstraints();
                gbcForm.insets = new Insets(5, 5, 5, 5);
                gbcForm.anchor = GridBagConstraints.WEST;
                gbcForm.fill = GridBagConstraints.HORIZONTAL;
                
                // Add header
                gbcForm.gridx = 0;
                gbcForm.gridy = 0;
                gbcForm.gridwidth = 2;
                formPanel.add(new JLabel("Enter class names:"), gbcForm);
                
                // Create and add new fields with labels and editable text fields
                JTextField[] textFields = new JTextField[numClasses];
                for (int i = 0; i < numClasses; i++) {
                    // Label
                    gbcForm.gridx = 0;
                    gbcForm.gridy = i + 1;
                    gbcForm.gridwidth = 1;
                    gbcForm.weightx = 0.0;
                    formPanel.add(new JLabel("Class " + i + ":"), gbcForm);
                    
                    // Text field with default value
                    gbcForm.gridx = 1;
                    gbcForm.weightx = 1.0;
                    textFields[i] = new JTextField("Class: " + i);
                    textFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, textFields[i].getPreferredSize().height));
                    formPanel.add(textFields[i], gbcForm);
                    
                    // Store the default name
                    final int index = i;
                    textFields[i].getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) { update(); }
                        public void removeUpdate(DocumentEvent e) { update(); }
                        public void insertUpdate(DocumentEvent e) { update(); }
                        
                        private void update() {
                            classificationNames[index] = textFields[index].getText().trim();
                            classificationNamesConfirmed = !Arrays.stream(classificationNames)
                                .anyMatch(s -> s == null || s.isEmpty());
                        }
                    });
                    
                    classificationNames[i] = "class_" + i;
                }
                
                // Add a submit button
                JButton submitButton = new JButton("Save Classes");
                gbcForm.gridx = 0;
                gbcForm.gridy = numClasses + 1;
                gbcForm.gridwidth = 2;
                gbcForm.weighty = 1.0;
                gbcForm.anchor = GridBagConstraints.NORTH;
                formPanel.add(submitButton, gbcForm);
                
                // Add the form to the scrollable panel
                fieldsPanel.add(formPanel);
                
                // Handle submit button click
                submitButton.addActionListener(evt -> {
                    // Update the class names from the text fields
                    for (int i = 0; i < numClasses; i++) {
                        classificationNames[i] = textFields[i].getText().trim();
                        if (classificationNames[i].isEmpty()) {
                            JOptionPane.showMessageDialog(panel, 
                                "Please enter a name for class " + i,
                                "Missing Class Name", 
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    
                    // Switch to attribute panel by clicking the attributes button
                    attributesButton.doClick();
                    JOptionPane.showMessageDialog(panel,
                        "Classes saved! Now configure attributes.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                classificationNamesConfirmed = true;
                
                // Revalidate and repaint to update the UI
                fieldsPanel.revalidate();
                fieldsPanel.repaint();
                
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid number",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createAttributePanel() {
        // Main panel with border layout
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Control panel with GridBagLayout for precise control
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(BorderFactory.createTitledBorder("Attribute Configuration"));
        controlPanel.setPreferredSize(new Dimension(350, 500));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Input panel for number of attributes
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Number of Attributes"));
        
        // Input field and label
        JLabel numAttrsLabel = new JLabel("Number of attributes:");
        JTextField numAttrsField = new JTextField(5);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.5;
        inputPanel.add(numAttrsLabel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.5;
        inputPanel.add(numAttrsField, gbc);
        
        // Add input panel to control panel
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        controlPanel.add(inputPanel, gbc);
        
        // Generate button
        JButton generateButton = new JButton("Generate Attribute Fields");
        generateButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, generateButton.getPreferredSize().height));
        
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(15, 5, 5, 5);
        buttonPanel.add(generateButton, gbc);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        controlPanel.add(buttonPanel, gbc);
        
        // Add some vertical glue to push content to the top
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weighty = 1.0;
        controlPanel.add(Box.createVerticalGlue(), gbc);

        // Display panel for attributes
        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder("Attribute Details"));
        
        // Panel to hold the attribute fields
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        // Add components to main panel using split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, controlPanel, displayPanel);
        splitPane.setDividerLocation(250);
        splitPane.setResizeWeight(0.3);
        panel.add(splitPane, BorderLayout.CENTER);
        
        // Set initial focus
        numAttrsField.requestFocusInWindow();
        
        // No submit button needed as we'll update the model directly
        attributeNames = new String[0];
        attributeKValues = new Integer[0];

        generateButton.addActionListener(e -> {
            try {
                int numAttrs = Integer.parseInt(numAttrsField.getText().trim());
                if (numAttrs <= 0) {
                    JOptionPane.showMessageDialog(panel, "Please enter a positive number",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Clear previous fields
                fieldsPanel.removeAll();
                attributeNames = new String[numAttrs];
                attributeKValues = new Integer[numAttrs];

                // create the array to contain all the subfunctions. all the interview are null obviously, but all of the subfunction flags are disabled to start, since boolean initializes false.
                subFunctionsForEachAttribute = new Interview[numAttrs];

                // Create a panel for the form
                JPanel formPanel = new JPanel(new GridBagLayout());
                GridBagConstraints gbcForm = new GridBagConstraints();
                gbcForm.insets = new Insets(5, 5, 5, 5);
                gbcForm.anchor = GridBagConstraints.WEST;
                gbcForm.fill = GridBagConstraints.HORIZONTAL;
                
                // Add header
                gbcForm.gridx = 0;
                gbcForm.gridy = 0;
                gbcForm.gridwidth = 3;
                formPanel.add(new JLabel("Enter attribute details:"), gbcForm);
                
                // Add column headers
                gbcForm.gridy = 1;
                gbcForm.gridx = 0;
                gbcForm.gridwidth = 1;
                formPanel.add(new JLabel("Attribute"), gbcForm);
                
                gbcForm.gridx = 1;
                formPanel.add(new JLabel("Name"), gbcForm);
                
                gbcForm.gridx = 2;
                formPanel.add(new JLabel("K-value"), gbcForm);
                
                // Create arrays to hold the input fields
                JTextField[] nameFields = new JTextField[numAttrs];
                JTextField[] kValueFields = new JTextField[numAttrs];
                
                // Add input fields for each attribute
                for (int i = 0; i < numAttrs; i++) {
                    // Attribute number
                    gbcForm.gridy = i + 2; // Start from row 2 (after headers)
                    gbcForm.gridx = 0;
                    gbcForm.weightx = 0.0;
                    formPanel.add(new JLabel("Attribute " + i + ":"), gbcForm);
                    
                    // Name field with default value
                    gbcForm.gridx = 1;
                    gbcForm.weightx = 0.7;
                    nameFields[i] = new JTextField("Attribute: " + i);
                    nameFields[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, nameFields[i].getPreferredSize().height));
                    formPanel.add(nameFields[i], gbcForm);
                    
                    // K-value field with default value of 2
                    gbcForm.gridx = 2;
                    gbcForm.weightx = 0.3;
                    kValueFields[i] = new JTextField("2");
                    kValueFields[i].setMaximumSize(new Dimension(60, kValueFields[i].getPreferredSize().height));
                    formPanel.add(kValueFields[i], gbcForm);
                    
                    // Store default values
                    attributeNames[i] = "Attribute: " + i;
                    attributeKValues[i] = 2;
                    
                    // Add document listeners to update the arrays when fields change
                    final int index = i;
                    nameFields[i].getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) { update(); }
                        public void removeUpdate(DocumentEvent e) { update(); }
                        public void insertUpdate(DocumentEvent e) { update(); }
                        
                        private void update() {
                            attributeNames[index] = nameFields[index].getText().trim();
                        }
                    });
                    
                    kValueFields[i].getDocument().addDocumentListener(new DocumentListener() {
                        public void changedUpdate(DocumentEvent e) { update(); }
                        public void removeUpdate(DocumentEvent e) { update(); }
                        public void insertUpdate(DocumentEvent e) { update(); }
                        
                        private void update() {
                            try {
                                attributeKValues[index] = Integer.parseInt(kValueFields[index].getText().trim());
                            } catch (NumberFormatException ex) {
                                // Keep the previous value if the new one is invalid
                            }
                        }
                    });
                }
                
                // Add a save button
                JButton saveButton = new JButton("Save Attributes");
                gbcForm.gridy = numAttrs + 2;
                gbcForm.gridx = 0;
                gbcForm.gridwidth = 3;
                gbcForm.weighty = 1.0;
                gbcForm.anchor = GridBagConstraints.NORTH;
                formPanel.add(saveButton, gbcForm);
                
                // Add the form to the scrollable panel
                fieldsPanel.add(formPanel);
                
                // Handle save button click
                saveButton.addActionListener(evt -> {

                    // Validate all fields
                    for (int i = 0; i < numAttrs; i++) {
                        // Validate name
                        String name = nameFields[i].getText().trim();
                        if (name.isEmpty()) {
                            JOptionPane.showMessageDialog(panel,
                                "Please enter a name for attribute " + i,
                                "Missing Attribute Name",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        
                        // Validate K-value
                        try {
                            int kValue = Integer.parseInt(kValueFields[i].getText().trim());
                            if (kValue < 2) {
                                JOptionPane.showMessageDialog(panel,
                                    "K-value must be at least 2 for attribute " + i,
                                    "Invalid K-value",
                                    JOptionPane.ERROR_MESSAGE);
                                return;
                            }
                        } catch (NumberFormatException ex) {
                            JOptionPane.showMessageDialog(panel,
                                "Please enter a valid number for K-value of attribute " + i,
                                "Invalid K-value",
                                JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    
                    // All valid, switch to question picking technique panel
                    questionAskingTechniqueButton.doClick();
                    JOptionPane.showMessageDialog(panel,
                        "Attributes saved! Now select question picking technique.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                });
                
                // Revalidate and repaint to update the UI
                fieldsPanel.revalidate();
                fieldsPanel.repaint();
                fieldsPanel.revalidate();
                fieldsPanel.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid number",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        submitButton.addActionListener(e -> {
            // Collect all attribute names and K values
            Component[] components = fieldsPanel.getComponents();
            boolean allFilled = true;
            int attrIndex = 0;

            for (Component comp : components) {
                if (comp instanceof JPanel) {
                    JPanel attrPanel = (JPanel) comp;
                    JPanel namePanel = (JPanel) attrPanel.getComponent(0);
                    JPanel kValuePanel = (JPanel) attrPanel.getComponent(1);

                    JTextField nameField = (JTextField) namePanel.getComponent(1);
                    JTextField kValueField = (JTextField) kValuePanel.getComponent(1);

                    if (nameField.getText().trim().isEmpty() || kValueField.getText().trim().isEmpty()) {
                        allFilled = false;
                        break;
                    }

                    try {
                        attributeNames[attrIndex] = nameField.getText().trim();
                        attributeKValues[attrIndex] = Integer.parseInt(kValueField.getText().trim());
                        if (attributeKValues[attrIndex] <= 0) {
                            JOptionPane.showMessageDialog(panel, "K values must be positive numbers",
                                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        attrIndex++;
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(panel, "Please enter valid numbers for K values",
                                "Invalid Input", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }
            }

            if (allFilled) {
                JOptionPane.showMessageDialog(panel, "Attribute information saved successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(panel, "Please fill in all attribute information",
                        "Incomplete Information", JOptionPane.WARNING_MESSAGE);
            }
        });

        return panel;
    }

    private JPanel createQuestionAskingTechniquePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create label
        JLabel modeLabel = new JLabel("Select Interview Mode:");
        modeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(modeLabel);

        // Scrollable list of interview modes (better for many entries)
        JList<InterviewMode> modeList = new JList<>(InterviewMode.values());
        modeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modeList.setVisibleRowCount(8); // show several entries by default
        modeList.setLayoutOrientation(JList.VERTICAL);
        modeList.setAlignmentX(Component.CENTER_ALIGNMENT);
        modeList.setSelectedIndex(0); // default selection
        JScrollPane modeScroll = new JScrollPane(modeList);
        modeScroll.setAlignmentX(Component.CENTER_ALIGNMENT);
        modeScroll.setPreferredSize(new Dimension(260, 160));
        panel.add(modeScroll);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing

        // Create submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(submitButton);

        // Add action listener for the submit button
        submitButton.addActionListener(e -> {
            InterviewMode selectedMode = modeList.getSelectedValue();
            if (selectedMode == null) {
                JOptionPane.showMessageDialog(panel, "Please select an interview mode.",
                        "Missing Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            interviewMode = selectedMode;
            // Proceed to Interview Mode configuration after successful selection
            interviewModeButton.doClick();
            JOptionPane.showMessageDialog(panel, "Interview mode set to: " + selectedMode +
                    "\nProceeding to Interview Mode setup...",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        });

        return panel;
    }

    private JPanel createInterviewModePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create mode selection toggle
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel modeLabel = new JLabel("Interview Mode:");
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton magicFunctionButton = new JRadioButton("Magic Function Mode");
        JRadioButton interactiveButton = new JRadioButton("Interactive Interview Mode");
        modeGroup.add(magicFunctionButton);
        modeGroup.add(interactiveButton);
        interactiveButton.setSelected(true); // Default selection

        modePanel.add(modeLabel);
        modePanel.add(magicFunctionButton);
        modePanel.add(interactiveButton);
        panel.add(modePanel);

        // Panel for weight inputs (only visible in Magic Function Mode)
        JPanel weightsPanel = new JPanel();
        weightsPanel.setLayout(new BoxLayout(weightsPanel, BoxLayout.Y_AXIS));
        weightsPanel.setBorder(BorderFactory.createTitledBorder("Attribute Weights"));
        weightsPanel.setVisible(false); // Initially hidden

        // Toggle visibility based on mode selection
        magicFunctionButton.addActionListener(e -> weightsPanel.setVisible(true));
        interactiveButton.addActionListener(e -> weightsPanel.setVisible(false));

        // Create submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add components to main panel
        panel.add(weightsPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing
        panel.add(submitButton);

        // Generate weight fields when needed
        magicFunctionButton.addActionListener(e -> {
            if (attributeNames == null || attributeNames.length == 0) {
                weightsPanel.removeAll();
                weightsPanel.add(new JLabel("Please set attributes first"));
                weightsPanel.revalidate();
                weightsConfirmed = false;
                return;
            }

            weightsPanel.removeAll();
            attributeWeights = new Float[attributeNames.length];
            weightsConfirmed = false;

            for (int i = 0; i < attributeNames.length; i++) {
                JPanel weightPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                weightPanel.add(new JLabel(attributeNames[i] + " weight:"));
                JTextField weightField = new JTextField(5);
                weightPanel.add(weightField);
                weightsPanel.add(weightPanel);
            }

            weightsPanel.revalidate();
            magicFunctionMode = MagicFunctionMode.KVAL_TIMES_WEIGHTS_MODE;
        });

        // Submit action
        submitButton.addActionListener(e -> {
            if (magicFunctionButton.isSelected()) {
                // Validate and collect weights
                boolean allValid = true;
                Component[] components = weightsPanel.getComponents();

                for (int i = 0; i < components.length; i++) {
                    if (components[i] instanceof JPanel) {
                        JPanel weightPanel = (JPanel) components[i];
                        JTextField weightField = (JTextField) weightPanel.getComponent(1);

                        try {
                            attributeWeights[i] = Float.parseFloat(weightField.getText().trim());
                        } catch (NumberFormatException ex) {
                            allValid = false;
                            break;
                        }
                    }
                }

                if (allValid) {
                    JOptionPane.showMessageDialog(panel, "Weights saved successfully!",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    weightsConfirmed = true;
                    // proceed to Sub-Functions setup
                    if (subFunctionsButton != null) {
                        subFunctionsButton.doClick();
                    }
                } else {
                    JOptionPane.showMessageDialog(panel, "Please enter valid numbers for all weights",
                            "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    weightsConfirmed = false;
                }
            } else {
                // Interactive mode not available in GUI
                JOptionPane.showMessageDialog(panel, "Interactive Interview Not Available In GUI Mode.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                weightsConfirmed = false;
            }
        });
        return panel;
    }

    private JPanel createSubFunctionsPanel() {
        // Panel structure similar to others: left could be future controls; for now, a single scrollable list
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel displayPanel = new JPanel(new BorderLayout());
        displayPanel.setBorder(BorderFactory.createTitledBorder("Sub-Functions"));

        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        if (attributeNames == null || attributeNames.length == 0) {
            fieldsPanel.add(new JLabel("Please configure attributes first."));
        } else {
            for (int i = 0; i < attributeNames.length; i++) {
                final int attributeIndex = i; // Capture loop variable for lambda
                String attrName = attributeNames[i] != null ? attributeNames[i] : ("Attribute " + i);
                JPanel row = new JPanel(new BorderLayout(10, 0));
                JLabel nameLabel = new JLabel(attrName);
                JButton createButton = new JButton("CREATE SUB FUNCTION");
                
                createButton.addActionListener(e -> {
                    // Launch sub-function creation for this attribute
                    String title = "Create Sub-Function For: " + attrName;
                    CreateFunctionWindow subFunctionWindow = new CreateFunctionWindow();
                    CompletableFuture<Interview> subFunctionFuture = subFunctionWindow.createFunctionAndReturnInterviewObject(title);
                    
                    // When the sub-function is created, store it
                    subFunctionFuture.thenAccept(subInterview -> {
                        if (subInterview != null) {
                            subFunctionsForEachAttribute[attributeIndex] = subInterview;
                            SwingUtilities.invokeLater(() -> {
                                createButton.setText("EDIT SUB FUNCTION");
                                JOptionPane.showMessageDialog(panel, 
                                    "Sub-function created for: " + attrName,
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            });
                        }
                    });
                });

                row.add(nameLabel, BorderLayout.CENTER);
                row.add(createButton, BorderLayout.EAST);
                row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
                fieldsPanel.add(row);
            }
        }

        JScrollPane scrollPane = new JScrollPane(fieldsPanel);
        displayPanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(displayPanel, BorderLayout.CENTER);
        return panel;
    }

    public CreateFunctionWindow(){

        applyLookAndFeel();

        // instantiate shared UI components
        titleLabel = new JLabel("Create Interview", SwingConstants.CENTER);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));

        titlePanel = new JPanel(new BorderLayout());
        titlePanel.add(titleLabel, BorderLayout.CENTER);

        enterClassesButton = new JButton("Classifications");
        attributesButton = new JButton("Attributes");
        questionAskingTechniqueButton = new JButton("Question Picking Technique");
        interviewModeButton = new JButton("Interview Mode");
        subFunctionsButton = new JButton("Sub-Functions");

        inputTypePanel = new JPanel(new GridLayout(1, 5, 10, 0));
        inputTypePanel.add(enterClassesButton);
        inputTypePanel.add(attributesButton);
        inputTypePanel.add(questionAskingTechniqueButton);
        inputTypePanel.add(interviewModeButton);
        inputTypePanel.add(subFunctionsButton);

        classificationPanel = createClassificationPanel();
        attributePanel = createAttributePanel();
        questionAskingTechniquePanel = createQuestionAskingTechniquePanel();
        interviewModePanel = createInterviewModePanel();
        subFunctionsPanel = createSubFunctionsPanel();

        userInputPanel = new JPanel(new BorderLayout());
        userInputScrollPane = new JScrollPane(classificationPanel);
        userInputScrollPane.setBorder(null);
        userInputScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        userInputPanel.add(userInputScrollPane, BorderLayout.CENTER);

        submitButton = new JButton("Create Interview");
        submitButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        submitButtonPanel.add(submitButton);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(titlePanel);
        topPanel.add(Box.createVerticalStrut(10));
        inputTypePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputTypePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, inputTypePanel.getPreferredSize().height));
        topPanel.add(inputTypePanel);

        mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel, BorderLayout.NORTH);
        mainPanel.add(userInputPanel, BorderLayout.CENTER);
        mainPanel.add(submitButtonPanel, BorderLayout.SOUTH);

        enterClassesButton.addActionListener(e -> {
            userInputScrollPane.setViewportView(classificationPanel);
            userInputPanel.revalidate();
            userInputPanel.repaint();
        });

        attributesButton.addActionListener(e -> {
            userInputScrollPane.setViewportView(attributePanel);
            userInputPanel.revalidate();
            userInputPanel.repaint();
        });

        questionAskingTechniqueButton.addActionListener(e -> {
            userInputScrollPane.setViewportView(questionAskingTechniquePanel);
            userInputPanel.revalidate();
            userInputPanel.repaint();
        });

        interviewModeButton.addActionListener(e -> {
            userInputScrollPane.setViewportView(interviewModePanel);
            userInputPanel.revalidate();
            userInputPanel.repaint();
        });

        subFunctionsButton.addActionListener(e -> {
            // Rebuild the panel each time to reflect latest attribute names
            subFunctionsPanel = createSubFunctionsPanel();
            userInputScrollPane.setViewportView(subFunctionsPanel);
            userInputPanel.revalidate();
            userInputPanel.repaint();
        });

        submitButton.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {

                if (attributeKValues == null || attributeKValues.length == 0) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Please provide attribute details before submitting.",
                            "Missing Data", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (attributeNames == null || attributeNames.length != attributeKValues.length) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Attribute Names and Number of Attributes don't match! Attribute names must be entered for every attribute.",
                            "Missing Data", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (int i = 0; i < attributeNames.length; i++) {
                    if (attributeNames[i] == null || attributeNames[i].trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Attribute names cannot be empty.",
                                "Invalid Data", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    if (attributeKValues[i] == null || attributeKValues[i] <= 0) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Each attribute must have a positive K value.",
                                "Invalid Data", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                if (classificationNames == null || classificationNames.length < 2 || !classificationNamesConfirmed) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Please enter at least two classification names.",
                            "Missing Data", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (String classificationName : classificationNames) {
                    if (classificationName == null || classificationName.trim().isEmpty()) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Classification names cannot be empty.",
                                "Invalid Data", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                if (interviewMode == null) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Please select an interview mode.",
                            "Missing Data", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (attributeWeights == null || attributeWeights.length != attributeKValues.length || !weightsConfirmed) {
                    JOptionPane.showMessageDialog(mainPanel,
                            "Please enter weights for every attribute.",
                            "Missing Data", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                for (Float attributeWeight : attributeWeights) {
                    if (attributeWeight == null) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Attribute weights cannot be empty.",
                                "Invalid Data", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                setInputEnabled(false);
                submitButton.setText("Creating Interview...");

                interviewCreationTask = CompletableFuture.supplyAsync(() ->
                        new Interview(attributeKValues,
                                attributeWeights,
                                interviewMode,
                                classificationNames.length,
                                attributeNames,
                                classificationNames,
                                null,
                                subFunctionsForEachAttribute,
                                magicFunctionMode,
                                false));

                interviewCreationTask.whenComplete((result, throwable) ->
                        SwingUtilities.invokeLater(() -> {
                            if (throwable != null) {
                                Throwable cause = throwable instanceof java.util.concurrent.CompletionException
                                        ? throwable.getCause()
                                        : throwable;
                                JOptionPane.showMessageDialog(mainPanel,
                                        "Failed to create interview: " + (cause != null ? cause.getMessage() : throwable),
                                        "Error", JOptionPane.ERROR_MESSAGE);
                                cause.printStackTrace();
                                setInputEnabled(true);
                                submitButton.setText("Create Interview");
                                interviewCreationTask = null;
                                return;
                            }

                            JOptionPane.showMessageDialog(mainPanel,
                                    "Interview created successfully!",
                                    "Success", JOptionPane.INFORMATION_MESSAGE);
                            if (interviewFuture != null && !interviewFuture.isDone()) {
                                interviewFuture.complete(result);
                            }
                            interviewCreationTask = null;
                            JDialog currentDialog = dialog;
                            if (currentDialog != null) {
                                currentDialog.dispose();
                            }
                        }));
            });
        });

        magicFunctionMode = MagicFunctionMode.EXPERT_MODE;
    }

    private void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatIntelliJLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
    }

    private void setInputEnabled(boolean enabled) {
        enterClassesButton.setEnabled(enabled);
        attributesButton.setEnabled(enabled);
        questionAskingTechniqueButton.setEnabled(enabled);
        interviewModeButton.setEnabled(enabled);
        if (subFunctionsButton != null) {
            subFunctionsButton.setEnabled(enabled);
        }
        submitButton.setEnabled(enabled);
    }

}
