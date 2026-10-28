package com.bemfeito.api.requests.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;
import org.slf4j.*;

@Service
public class InvitationDeliveryService {
    private static final Logger LOG=LoggerFactory.getLogger(InvitationDeliveryService.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    public InvitationDeliveryService(ObjectProvider<JavaMailSender> senders,@Value("${app.mail.from:}") String from) {
        this.senders=senders;this.from=from;
    }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void deliver(InvitationMail event) {
        try {
            JavaMailSender sender=senders.getIfAvailable();
            if(sender==null || from.isBlank()) throw new IllegalStateException();
            var message=new SimpleMailMessage();message.setFrom(from);message.setTo(event.email());
            message.setSubject("Bem-Feito — ative sua conta");
            message.setText("Olá, "+event.name()+".\n\nSeu acesso a "+event.institution()+" foi aprovado pela equipe Bem-Feito.\n"
                +"Defina sua senha pelo link: "+event.activationUrl()+"\nEste convite expira em "+event.expiresAt()+".\n"
                +"Se você não reconhece esta solicitação, ignore esta mensagem.\n\nEquipe Bem-Feito");
            sender.send(message);
        } catch (Exception ignored) {
            // SMTP exceptions can include recipient/body/token: never log the exception or its message.
            LOG.warn("Entrega de convite indisponível para userId={}; administrador pode reemitir",event.userId());
        }
    }
}
