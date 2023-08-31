package com.guidewire.certificationtracker;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailUtil {

  public static void sendEmail(String emailBody, String userName) {

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
        InternetAddress.parse("smirajkar@guidewire.com,earnold@guidewire.com")
      );
      message.setSubject("Update your Guidewire Certifications");

      StringBuilder fullEmailBody = new StringBuilder();
      fullEmailBody.append("Hello " + userName +",<br><br>");
      fullEmailBody.append("This email is to provide you a summary of your current certifications along with some " +
              "courses that you can consider taking to update your certifications.<br><br>");
      fullEmailBody.append(emailBody);
      fullEmailBody.append("<br>Thanks,<br>");
      fullEmailBody.append("Guidewire Education");
      message.setContent(fullEmailBody.toString(), "text/html");

      Transport.send(message);

      System.out.println("Done");

    } catch (MessagingException e) {
      e.printStackTrace();
    }
  }
}