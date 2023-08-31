package com.guidewire.certificationtracker;


import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;


public class JFilePicker extends JPanel {
  private String textFieldLabel;
  private String buttonLabel;

  private JTextField textField;
  private JButton button;

  private JFileChooser fileChooser;

  private int mode;
  public static final int MODE_OPEN = 1;
  public static final int MODE_SAVE = 2;

  public JFilePicker(String textFieldLabel, String buttonLabel) {
    this.setBackground(Color.WHITE);
    this.buttonLabel = buttonLabel;

    fileChooser = new JFileChooser();
    fileChooser.setAcceptAllFileFilterUsed(false);

    setLayout(new FlowLayout(FlowLayout.LEFT, 10, 0));

    textField = new JTextField(70);
    textField.setEditable(false);
    button = new JButton(buttonLabel);

    button.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent evt) {
        buttonActionPerformed(evt);
      }
    });

    add(textField);
    add(button);
  }

  private void buttonActionPerformed(ActionEvent evt) {
    if (mode == MODE_OPEN) {
      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
      }
    } else if (mode == MODE_SAVE) {
      if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        textField.setText(fileChooser.getSelectedFile().getAbsolutePath());
      }
    }
  }

  public void addFileTypeFilter(String extension, String description) {
    FileFilter filter = new FileNameExtensionFilter("Comma Separated File","csv");
    fileChooser.addChoosableFileFilter(filter);
  }

  public void setMode(int mode) {
    this.mode = mode;
  }

  public String getSelectedFilePath() {
    return textField.getText();
  }

  public JFileChooser getFileChooser() {
    return this.fileChooser;
  }
}
