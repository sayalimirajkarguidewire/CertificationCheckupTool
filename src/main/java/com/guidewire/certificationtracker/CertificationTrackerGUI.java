package com.guidewire.certificationtracker;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;


public class CertificationTrackerGUI extends JFrame {
  private JLabel validatorLogoLabel;

  private JLabel inputPathLabel;
  private JLabel userNameLabel;

  private JFilePicker inputPathPicker;
  private JTextField userNameTextField;
  private JButton trackCertificationButton;
  private JButton sendEmailButton;
  private JButton trackAllUsersButton;
  private JLabel trackCertificationStatus;
  private JEditorPane certificationStatusTextArea;

  private CertificationTracker certificationTracker;

  public CertificationTrackerGUI(String title) throws Exception {
    super(title);
    addComponentsToPane(this.getContentPane());
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setResizable(true);
    Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (int) ((dimension.getWidth() - this.getWidth()) / 3);
    int y = (int) ((dimension.getHeight() - this.getHeight()) / 2);
    this.setLocation(x, y);

    this.pack();
    Dimension packedDimension = this.getSize();
    Dimension paddedDimension = new Dimension((int) Math.floor(packedDimension.getWidth() * 1.10),
      (int) Math.floor(packedDimension.getHeight() * 1.15));
    this.setSize(paddedDimension);
    this.setVisible(true);
    try {
      this.setIconImage(ImageIO.read(CertificationTrackerGUI.class.getResourceAsStream("/Favicon.png")));
    } catch (Exception e) {}
  }

  public void addComponentsToPane(Container pane) {
    //pane.setBackground(Color.WHITE);
    pane.setLayout(new BorderLayout());
    JPanel logoPanel = new JPanel();
    logoPanel.setBackground(Color.WHITE);
    logoPanel.setLayout(new BorderLayout());
    pane.add(logoPanel, BorderLayout.NORTH);
    InputStream is = new BufferedInputStream(CertificationTrackerGUI.class.getResourceAsStream("/GWEducationLogo"
      + ".jpg"));
    try {
      Image image = ImageIO.read(is);
      this.validatorLogoLabel = new JLabel(new ImageIcon(image));
      logoPanel.add(validatorLogoLabel, BorderLayout.WEST);
    } catch (Exception e) {
    }

    JPanel centerPanel = new JPanel();
    centerPanel.setBackground(Color.WHITE);
    pane.add(centerPanel, BorderLayout.CENTER);
    addComponentsToCenterPane(centerPanel);
    this.pack();
  }
  public void addComponentsToCenterPane(Container pane) {
    pane.setLayout(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.HORIZONTAL;

    this.inputPathLabel = new JLabel("       Input File Path");
    c.weightx = 0.5;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 1;
    pane.add(inputPathLabel, c);
    this.inputPathPicker = new JFilePicker("", "Browse");
    this.inputPathPicker.setMode(JFilePicker.MODE_OPEN);
    this.inputPathPicker.addFileTypeFilter("csv", "Comma Separated Document");
    c.weightx = 0.5;
    c.gridx = 1;
    c.gridy = 1;
    c.weighty = 2;
    pane.add(inputPathPicker, c);

    this.userNameLabel = new JLabel("       User Email");
    c.weightx = 0.5;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 0;
    c.gridy = 2;
    pane.add(userNameLabel, c);
    this.userNameTextField = new JTextField(35);
    this.userNameTextField.setEditable(true);
    userNameTextField.setBackground(Color.white);
    c.weightx = 0.5;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridx = 1;
    c.gridy = 2;
    c.weighty = 2;
    pane.add(userNameTextField, c);

    this.trackCertificationButton = new JButton("Track Certification");
    this.trackCertificationButton.setToolTipText("Clicking on this button will analyze certification details only for "
      + "the "
      + "specified email address. Output will be displayed in the text area.");
    this.trackCertificationButton.setEnabled(true);
    trackCertificationButton.setPreferredSize(new Dimension(150, 30));
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTH;
    c.weightx = 0.0;
    c.gridx = 0;
    c.gridy = 5;
    pane.add(trackCertificationButton, c);
    trackCertificationButton.addActionListener(e -> {
      trackCertificationStatus.setForeground(Color.BLACK);
      trackCertificationStatus.setText("Computing status...");
      trackCertificationStatus.setEnabled(false);
      Thread worker = new Thread(() -> {
        try {
          if (inputPathPicker.getSelectedFilePath().isEmpty()
            || !Files.exists(Paths.get(inputPathPicker.getSelectedFilePath()))) {
            JOptionPane.showMessageDialog(this,
              "Please specify valid input CSV file!");
            return;
          }
          certificationStatusTextArea.setVisible(true);
          certificationStatusTextArea.setText("Computing certification status...");
          this.certificationTracker = new CertificationTracker(inputPathPicker.getSelectedFilePath());
          String output = certificationTracker.getRecommendations(userNameTextField.getText().trim());
          certificationStatusTextArea.setVisible(true);
          certificationStatusTextArea.setText(output);
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(this,
            "Validation Failed :" + ex.getStackTrace());
        }
        //sendEmailButton.setEnabled(true);
        SwingUtilities.invokeLater(() -> trackCertificationButton.setEnabled(true));
      });
      worker.start();
    });

    this.sendEmailButton = new JButton("Send Email");
    this.sendEmailButton.setEnabled(false);
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTH;
    c.weightx = 0.0;
    c.gridx = 0;
    c.gridy = 6;
    sendEmailButton.setPreferredSize(new Dimension(150, 30));
    pane.add(sendEmailButton, c);

    this.trackAllUsersButton = new JButton("Analyze All Data");
    this.trackAllUsersButton.setEnabled(true);
    this.trackAllUsersButton.setToolTipText("Clicking on this button will analyze all the users' data from the "
      + "specified input file. The output will be written to a new CSV file at the same location as the input file, "
      + "with the file name being <input-file-name>_analyzed.csv");
    c.fill = GridBagConstraints.NONE;
    c.anchor = GridBagConstraints.NORTH;
    c.weightx = 0.0;
    c.gridx = 0;
    c.gridy = 7;
    trackAllUsersButton.setPreferredSize(new Dimension(150, 30));
    pane.add(trackAllUsersButton, c);

    sendEmailButton.addActionListener(e -> {
      Thread worker = new Thread(() -> {
        EmailUtil.sendEmail(this.certificationStatusTextArea.getText(),
                certificationTracker.getNameFromEmail(this.userNameTextField.getText().trim()));
        trackCertificationStatus.setText("Email Sent!");
        trackCertificationStatus.setForeground(Color.GREEN);
        JOptionPane.showMessageDialog(this,
                "Email sent to " + certificationTracker.getFullNameFromEmail(userNameTextField.getText().trim())
                        + " (" + userNameTextField.getText().trim() + ")" + "!");
      });
      worker.start();
    });

    trackAllUsersButton.addActionListener(e -> {
      Thread worker = new Thread(() -> {
        String outputFilePath = getOutputPathFromInputPath(inputPathPicker.getSelectedFilePath());
        try {
          if (inputPathPicker.getSelectedFilePath().isEmpty()
            || !Files.exists(Paths.get(inputPathPicker.getSelectedFilePath()))) {
            JOptionPane.showMessageDialog(this,
              "Please specify valid input CSV file!");
            return;
          }
          this.certificationTracker = new CertificationTracker(inputPathPicker.getSelectedFilePath());
          this.trackAllUsersButton.setEnabled(false);
          certificationTracker.analyzeDataForAllUsers(outputFilePath);
        } catch (Exception ex) {
          System.out.println("Failed to analyze data for all users" + ex.getStackTrace());
        } finally {
          this.trackAllUsersButton.setEnabled(true);
        }
        trackCertificationStatus.setText("Analysis Complete!");
        trackCertificationStatus.setForeground(Color.GREEN);
        JOptionPane.showMessageDialog(this,
                "Analysis Complete! Data in " + outputFilePath);
      });
      worker.start();
    });

    this.trackCertificationStatus = new JLabel("");
    c.fill = GridBagConstraints.WEST;
    c.weightx = 0.5;
    c.gridx = 0;
    c.gridy = 3;
    //pane.add(trackCertificationStatus, c);

    this.certificationStatusTextArea = new JEditorPane();
    JScrollPane scroll = new JScrollPane (this.certificationStatusTextArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    this.certificationStatusTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));
    this.certificationStatusTextArea.setContentType("text/html");
    this.certificationStatusTextArea.setEditable(false);
    this.certificationStatusTextArea.setPreferredSize(new Dimension(700, 300));
    c.fill = GridBagConstraints.HORIZONTAL;
    c.weightx = 0.0;
    c.weighty = 4;
    c.gridx = 1;
    c.gridy = 5;
    c.gridheight = 3;
    this.certificationStatusTextArea.setVisible(false);
    pane.add(scroll, c);
    this.pack();
  }

  /**
   * Create the GUI and show it.  For thread safety,
   * this method should be invoked from the
   * event-dispatching thread.
   */
  private static void createAndShowGUI() {
    //Create and set up the window.
    try {
      CertificationTrackerGUI validatorGUI = new CertificationTrackerGUI("Certification Tracker Tool");
    } catch (Exception e) {
      System.err.println("Failed to create GUI : " + e.getStackTrace());
    }
  }

  private String getOutputPathFromInputPath(String inputPathString) {
    if (inputPathString.isEmpty()) {
      return "";
    }
    Path inputPath = Paths.get(inputPathString);
    String fileName = inputPath.getFileName().toString();
    Date date = new Date() ;
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    String outputFileName = fileName.split("\\.")[0] + "_analyzed_" + dateFormat.format(date) + ".csv";

    String outputDirectory = inputPath.getParent().toAbsolutePath().toString();
    return outputDirectory + File.separator + outputFileName;
  }

  public static void main(String[] args) throws Exception {
    //Schedule a job for the event-dispatching thread:
    //creating and showing this application's GUI.
    UIManager.setLookAndFeel(
      UIManager.getCrossPlatformLookAndFeelClassName());
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
}