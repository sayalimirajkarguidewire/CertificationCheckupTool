package com.guidewire.certificationtracker;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

  public static void sendEmail(String emailBody) {

    final String username = "smirajkar@guidewire.com";
    final String password = "rlqltvzjzyqnmcjr";

    Properties prop = new Properties();
    prop.put("mail.smtp.host", "smtp.gmail.com");
    prop.put("mail.smtp.port", "465");
    prop.put("mail.smtp.auth", "true");
    prop.put("mail.smtp.socketFactory.port", "465");
    prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

    Session session = Session.getInstance(prop,
      new javax.mail.Authenticator() {
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(username, password);
        }
      });

    try {

      Message message = new MimeMessage(session);
      message.setFrom(new InternetAddress("smirajkar@guidewire.com"));
      message.setRecipients(
        Message.RecipientType.TO,
        InternetAddress.parse("smirajkar@guidewire.com")
      );
      message.setSubject("Update your Guidewire Certifications");

      StringBuilder fullEmailBody = new StringBuilder();
      fullEmailBody.append("Hello,\n\n");
      fullEmailBody.append(emailBody);
      fullEmailBody.append("\nThanks,\n");
      fullEmailBody.append("Guidewire Education");
      message.setText(fullEmailBody.toString());

      Transport.send(message);

      System.out.println("Done");

    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }
}