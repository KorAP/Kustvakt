package de.ids_mannheim.korap.service;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import de.ids_mannheim.korap.config.FullConfiguration;
import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.User;

/** Manages mail related services, such as sending group member invitations 
 * per email.  
 * 
 * @author margaretha
 *
 */
@Service
public class MailService {

    private static Logger jlog = LoggerFactory.getLogger(MailService.class);

    @Autowired
    private AuthenticationManagerIface authManager;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private VelocityEngine velocityEngine;
    @Autowired
    private FullConfiguration config;

    public void sendMemberInvitationNotification (String inviteeName,
            String groupName, String inviter) {

        MimeMessagePreparator preparator = new MimeMessagePreparator() {

            public void prepare (MimeMessage mimeMessage) throws Exception {

                User invitee = authManager.getUser(inviteeName,
                        config.getEmailAddressRetrieval());

                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setTo(new InternetAddress(invitee.getEmail()));
                message.setFrom(config.getNoReply());
                message.setSubject("Invitation to join " + groupName);
                message.setText(prepareGroupInvitationText(inviteeName,
                        groupName, inviter), true);
            }

        };
        mailSender.send(preparator);
    }

    private String prepareGroupInvitationText (String username,
            String groupName, String inviter) {
        Context context = new VelocityContext();
        context.put("username", username);
        context.put("group", groupName);
        context.put("inviter", inviter);

        StringWriter stringWriter = new StringWriter();

        velocityEngine.mergeTemplate(
                "templates/" + config.getGroupInvitationTemplate(),
                StandardCharsets.UTF_8.name(), context, stringWriter);

        String message = stringWriter.toString();
        jlog.debug(message);
        return message;
    }
}
