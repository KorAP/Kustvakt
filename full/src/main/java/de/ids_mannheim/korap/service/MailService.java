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

import de.ids_mannheim.korap.interfaces.AuthenticationManagerIface;
import de.ids_mannheim.korap.user.User;

@Service
public class MailService {
    
    private static Logger jlog =
            LoggerFactory.getLogger(MailService.class);
    
    @Autowired
    private AuthenticationManagerIface authManager;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private VelocityEngine velocityEngine;

    public void sendMemberInvitationNotification (String inviteeName,
            String sender, String groupName, String inviter) {

        MimeMessagePreparator preparator = new MimeMessagePreparator() {

            public void prepare (MimeMessage mimeMessage) throws Exception {

                User invitee = authManager.getUser(inviteeName);

                MimeMessageHelper message = new MimeMessageHelper(mimeMessage);
                message.setTo(new InternetAddress(invitee.getEmail()));
                message.setFrom(sender);
                message.setSubject("Invitation to join group");
                message.setText(prepareText(inviteeName, groupName, inviter),
                        true);
            }

        };
        mailSender.send(preparator);
    }

    private String prepareText (String username, String groupName,
            String inviter) {
        Context context = new VelocityContext();
        context.put("username", username);
        context.put("group", groupName);
        context.put("inviter", inviter);
        
        StringWriter stringWriter = new StringWriter();
//        URL url = getClass().getClassLoader().getResource("notification.vm");
//        System.out.println(url);
//        Template t = velocityEngine.getTemplate(url.toString());
//        System.out.println(t);
        
        velocityEngine.mergeTemplate("notification.vm",
                StandardCharsets.UTF_8.name(), context, stringWriter);
        
        String message = stringWriter.toString();
        jlog.debug(message);
        return message;
    }
}
