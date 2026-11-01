package com.bemfeito.api.requests.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.*;
import org.springframework.web.util.HtmlUtils;
import org.slf4j.*;

@Service
public class InvitationDeliveryService {
    private static final Logger LOG=LoggerFactory.getLogger(InvitationDeliveryService.class);
    private final ObjectProvider<JavaMailSender> senders;
    private final String from;
    public InvitationDeliveryService(ObjectProvider<JavaMailSender> senders,@Value("${app.mail.from:}") String from) { this.senders=senders;this.from=from; }
    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void deliver(InvitationMail event) {
        try {
            JavaMailSender sender=senders.getIfAvailable();
            if(sender==null || from.isBlank()) { event.receipt().delivered("NOT_CONFIGURED");return; }
            String purpose=event.receipt().purpose();
            String action=purpose.equals("RESET")?"Redefinir minha senha":purpose.equals("APPROVAL")?"Entrar no Bem-Feito":"Ativar minha conta";
            String title=purpose.equals("RESET")?"Recuperação de senha":purpose.equals("APPROVAL")?"Acesso aprovado":"Ative sua conta";
            String intro=purpose.equals("RESET")?"Recebemos uma solicitação para redefinir sua senha.":"Seu acesso a "+event.institution()+" foi aprovado.";
            String expiry=event.expiresAt()==null?"":" Este link é de uso único e expira em "+event.expiresAt()+".";
            var message=sender.createMimeMessage();var helper=new MimeMessageHelper(message,true,"UTF-8");
            helper.setFrom(from);helper.setTo(event.email());helper.setSubject("Bem-Feito — "+title);
            helper.setText(intro+"\n"+action+": "+event.activationUrl()+"\n"+expiry+"\nSe não reconhece a solicitação, ignore esta mensagem.",
                "<p>"+HtmlUtils.htmlEscape(intro)+"</p><p><a style=\"display:inline-block;padding:12px;background:#397653;color:white\" href=\""+HtmlUtils.htmlEscape(event.activationUrl())+"\">"+action+"</a></p><p>"+expiry+"</p><p>Se não reconhece a solicitação, ignore esta mensagem.</p>");
            sender.send(message);event.receipt().delivered("EMAIL_SENT");
        } catch (Exception ignored) {
            event.receipt().delivered("FAILED");
            LOG.warn("Entrega de mensagem indisponível para userId={}; nenhuma credencial foi registrada",event.userId());
        }
    }
}
