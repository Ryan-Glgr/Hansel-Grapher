package io.github.ryan_glgr.hansel_grapher.Visualizations.GUI;

import io.github.ryan_glgr.hansel_grapher.TheHardStuff.Interview;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.awt.KeyboardFocusManager;
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

    // our four panels which correspond to each button we can hit.
    private JPanel classificationPanel;
    private JPanel attributePanel;
    private JPanel questionAskingTechniquePanel;
    private JPanel interviewModePanel;

    private String[] classificationNames;
    private String[] attributeNames;
    private Integer[] attributeKValues;
    private Interview.InterviewMode interviewMode;
    private Float[] attributeWeights;

    private boolean classificationNamesConfirmed;
    private boolean weightsConfirmed;

    private CompletableFuture<Interview> interviewFuture;
    private CompletableFuture<Interview> interviewCreationTask;
    private JDialog dialog;

    public CompletableFuture<Interview> createFunctionAndReturnInterviewObject(){

        if (SwingUtilities.isEventDispatchThread()) {
            openDialog();
            return interviewFuture;
        }

        try {
            SwingUtilities.invokeAndWait(this::openDialog);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while displaying CreateFunctionWindow", ex);
        } catch (InvocationTargetException ex) {
            throw new RuntimeException("Failed to display CreateFunctionWindow", ex.getCause());
        }

        return interviewFuture;
    }

    private void openDialog() {
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

        dialog = new JDialog(owner, "Create Interview", Dialog.ModalityType.APPLICATION_MODAL);
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
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input field for number of classifications
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel numClassesLabel = new JLabel("Number of classifications:");
        JTextField numClassesField = new JTextField(5);
        inputPanel.add(numClassesLabel);
        inputPanel.add(numClassesField);

        // Button to generate classification name fields
        JButton generateButton = new JButton("Generate Fields");
        inputPanel.add(generateButton);

        // Panel to hold the classification name fields
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Panel for the submit button
        JPanel submitPanel = new JPanel();
        JButton submitButton = new JButton("Submit");
        submitPanel.add(submitButton);
        submitButton.setEnabled(false); // Disabled until fields are generated and filled

        // Add components to main panel
        panel.add(inputPanel);
        panel.add(fieldsPanel);
        panel.add(submitPanel);

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
                classificationNamesConfirmed = false;

                // Create and add new fields
                for (int i = 0; i < numClasses; i++) {
                    JPanel fieldPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    JLabel label = new JLabel("Classification " + i + " name:");
                    JTextField textField = new JTextField(20);
                    fieldPanel.add(label);
                    fieldPanel.add(textField);
                    fieldsPanel.add(fieldPanel);
                }

                // Enable submit button and update UI
                submitButton.setEnabled(true);
                fieldsPanel.revalidate();
                fieldsPanel.repaint();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(panel, "Please enter a valid number",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            }
        });

        submitButton.addActionListener(e -> {
            // Collect all classification names
            Component[] components = fieldsPanel.getComponents();
            boolean allFilled = true;

            for (int i = 0; i < components.length; i++) {
                if (components[i] instanceof JPanel) {
                    JPanel fieldPanel = (JPanel) components[i];
                    JTextField textField = (JTextField) fieldPanel.getComponent(1);
                    if (textField.getText().trim().isEmpty()) {
                        allFilled = false;
                        break;
                    }
                    classificationNames[i] = textField.getText().trim();
                }
            }

            if (allFilled) {
                // Close the dialog or move to next step
                JOptionPane.showMessageDialog(panel, "Classification names saved successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                classificationNamesConfirmed = true;
            } else {
                JOptionPane.showMessageDialog(panel, "Please fill in all classification names",
                    "Incomplete Information", JOptionPane.WARNING_MESSAGE);
                classificationNamesConfirmed = false;
            }
        });

        return panel;
    }

    private JPanel createAttributePanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input field for number of attributes
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel numAttrsLabel = new JLabel("Number of attributes:");
        JTextField numAttrsField = new JTextField(5);
        inputPanel.add(numAttrsLabel);
        inputPanel.add(numAttrsField);

        // Button to generate attribute fields
        JButton generateButton = new JButton("Generate Fields");
        inputPanel.add(generateButton);

        // Panel to hold the attribute fields
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // Panel for the submit button
        JPanel submitPanel = new JPanel();
        JButton submitButton = new JButton("Submit");
        submitPanel.add(submitButton);
        submitButton.setEnabled(false); // Disabled until fields are generated and filled

        // Add components to main panel
        panel.add(inputPanel);
        panel.add(fieldsPanel);
        panel.add(submitPanel);

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

                // Create and add new fields for each attribute
                for (int i = 0; i < numAttrs; i++) {
                    // Create panel for this attribute
                    JPanel attrPanel = new JPanel();
                    attrPanel.setLayout(new BoxLayout(attrPanel, BoxLayout.Y_AXIS));
                    attrPanel.setBorder(BorderFactory.createTitledBorder("Attribute " + (i + 1) + ":"));

                    // Name field
                    JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    namePanel.add(new JLabel("Name:"));
                    JTextField nameField = new JTextField(15);
                    namePanel.add(nameField);

                    // K value field
                    JPanel kValuePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    kValuePanel.add(new JLabel("K Value (number of values):"));
                    JTextField kValueField = new JTextField(5);
                    kValuePanel.add(kValueField);

                    attrPanel.add(namePanel);
                    attrPanel.add(kValuePanel);
                    fieldsPanel.add(attrPanel);
                }

                // Enable submit button and update UI
                submitButton.setEnabled(true);
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

        // Create dropdown label
        JLabel modeLabel = new JLabel("Select Interview Mode:");
        modeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(modeLabel);

        // Create dropdown with interview modes
        JComboBox<Interview.InterviewMode> modeComboBox = new JComboBox<>(Interview.InterviewMode.values());
        modeComboBox.setMaximumSize(new Dimension(200, 30));
        modeComboBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(modeComboBox);
        panel.add(Box.createRigidArea(new Dimension(0, 10))); // Add spacing

        // Create submit button
        JButton submitButton = new JButton("Submit");
        submitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(submitButton);

        // Add action listener for the submit button
        submitButton.addActionListener(e -> {
            Interview.InterviewMode selectedMode = (Interview.InterviewMode) modeComboBox.getSelectedItem();
            interviewMode = selectedMode;
            JOptionPane.showMessageDialog(panel, "Interview mode set to: " + selectedMode,
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

        inputTypePanel = new JPanel(new GridLayout(1, 4, 10, 0));
        inputTypePanel.add(enterClassesButton);
        inputTypePanel.add(attributesButton);
        inputTypePanel.add(questionAskingTechniqueButton);
        inputTypePanel.add(interviewModeButton);

        classificationPanel = createClassificationPanel();
        attributePanel = createAttributePanel();
        questionAskingTechniquePanel = createQuestionAskingTechniquePanel();
        interviewModePanel = createInterviewModePanel();

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

                for (int i = 0; i < attributeWeights.length; i++) {
                    if (attributeWeights[i] == null) {
                        JOptionPane.showMessageDialog(mainPanel,
                                "Attribute weights cannot be empty.",
                                "Invalid Data", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                setInputEnabled(false);
                submitButton.setText("Creating Interview...");

                interviewCreationTask = CompletableFuture.supplyAsync(() ->
                        new Interview(attributeKValues, attributeWeights, interviewMode,
                                classificationNames.length, attributeNames, classificationNames));

                interviewCreationTask.whenComplete((result, throwable) ->
                        SwingUtilities.invokeLater(() -> {
                            if (throwable != null) {
                                Throwable cause = throwable instanceof java.util.concurrent.CompletionException
                                        ? throwable.getCause()
                                        : throwable;
                                JOptionPane.showMessageDialog(mainPanel,
                                        "Failed to create interview: " + (cause != null ? cause.getMessage() : throwable),
                                        "Error", JOptionPane.ERROR_MESSAGE);
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

    }

    private void applyLookAndFeel() {
        try {
            String desired = UIManager.getCrossPlatformLookAndFeelClassName();
            if (UIManager.getLookAndFeel() == null
                || !UIManager.getLookAndFeel().getClass().getName().equals(desired)) {
                UIManager.setLookAndFeel(desired);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
    }

    private void setInputEnabled(boolean enabled) {
        enterClassesButton.setEnabled(enabled);
        attributesButton.setEnabled(enabled);
        questionAskingTechniqueButton.setEnabled(enabled);
        interviewModeButton.setEnabled(enabled);
        submitButton.setEnabled(enabled);
    }

}
