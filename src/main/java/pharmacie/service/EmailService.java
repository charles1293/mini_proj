package pharmacie.service;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service d'envoi d'emails via l'API SendGrid.
 * Remplace SpringMail qui n'est pas viable pour ce projet.
 */
@Slf4j
@Service
public class EmailService {

    @Value("${sendgrid.api-key:NOT_SET}")
    private String sendGridApiKey;

    @Value("${sendgrid.from-email:noreply@pharmacie.sn}")
    private String fromEmail;

    @Value("${sendgrid.from-name:Pharmacie Centrale}")
    private String fromName;

    /**
     * Envoie un email via SendGrid
     * @param to l'adresse email du destinataire
     * @param subject le sujet de l'email
     * @param body le contenu de l'email (HTML)
     * @return true si l'email a été envoyé avec succès
     */
    public boolean sendEmail(String to, String subject, String body) {
        if ("NOT_SET".equals(sendGridApiKey)) {
            log.warn("SendGrid API key non configurée. Email non envoyé à {}. Sujet: {}", to, subject);
            log.info("Contenu de l'email: {}", body);
            return false;
        }

        Email from = new Email(fromEmail, fromName);
        Email toEmail = new Email(to);
        Content content = new Content("text/html", body);
        Mail mail = new Mail(from, subject, toEmail, content);

        SendGrid sg = new SendGrid(sendGridApiKey);
        Request request = new Request();

        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);

            int statusCode = response.getStatusCode();
            if (statusCode >= 200 && statusCode < 300) {
                log.info("Email envoyé avec succès à {}. Status: {}", to, statusCode);
                return true;
            } else {
                log.error("Erreur lors de l'envoi de l'email à {}. Status: {}, Body: {}",
                    to, statusCode, response.getBody());
                return false;
            }
        } catch (IOException e) {
            log.error("Erreur IOException lors de l'envoi de l'email à {}: {}", to, e.getMessage());
            return false;
        }
    }
}
