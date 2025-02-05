package com.backend.notificaciones.service;

import java.util.List;
import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;

import com.backend.notificaciones.model.dto.NotificacionDTO;
import com.backend.notificaciones.model.entity.Notificacion;
import com.backend.notificaciones.repository.NotificacionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class MailService {
  @Value("${mail.smtp.host}")
  private String host;
  @Value("${mail.smtp.port}")
  private int port;
  @Value("${mail.smtp.user}")
  private String user;
  @Value("${mail.smtp.password}")
  private String password;

  @Autowired
  private NotificacionRepository notificacionRepository;


  public void sendMail(String to, String subject, String body, String motivo) {
    Properties props = new Properties();
    props.put("mail.smtp.host", host);
    props.put("mail.smtp.port", port);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");

    System.out.println("Sending email to " + to + " with subject " + subject);

    // Crear el autenticador usando Jakarta Mail
    Authenticator auth = new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(user, password);
      }
    };

    Session session = Session.getInstance(props, auth);

    try {
      Message message = new javax.mail.internet.MimeMessage(session);
      message.setFrom(new javax.mail.internet.InternetAddress(user));
      message.setRecipients(Message.RecipientType.TO, javax.mail.internet.InternetAddress.parse(to));
      message.setSubject(subject);

      String fullBody = "Motivo: " + motivo + "\n\n" + body;
      message.setText(fullBody);

      Transport.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException(e);
    }
  }
  public Notificacion saveNotificacion(Notificacion notificacion) {
    return notificacionRepository.save(notificacion);
  }

  public List<NotificacionDTO> getNotifications(int page, int size) {
    PageRequest pageRequest = PageRequest.of(page, size);
    return notificacionRepository.findAll(pageRequest).getContent().stream().map(NotificacionDTO::new).toList();
  }
}
