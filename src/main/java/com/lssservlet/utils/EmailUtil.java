package com.lssservlet.utils;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.lssservlet.core.Config;

public class EmailUtil {
    protected static final Logger log = LogManager.getLogger(EmailUtil.class);
    private Properties _properties = null;
    private String _from_email = null;
    private String _account = null;
    private String _password = null;
    private String _reply_address = null;
    private String _reply_address_name = null;
    static private ConcurrentHashMap<String, EmailUtil> _emailInstanceMap = new ConcurrentHashMap<String, EmailUtil>();

    private EmailUtil(String category) {
        _properties = Config.getInstance().getEmailProperties(category);
        _account = _properties.getProperty("mail.smtp.username");
        _from_email = _properties.getProperty("mail.smtp.from");
        if (_from_email == null)
            _from_email = _account;
        _password = _properties.getProperty("mail.smtp.password");
        _reply_address = _properties.getProperty("mail.reply.address");
        _reply_address_name = _properties.getProperty("mail.reply.address.name");
    }

    public static EmailUtil getInstance(String category) {
        if (category == null)
            category = "";
        EmailUtil instance = _emailInstanceMap.get(category);
        if (instance == null) {
            _emailInstanceMap.putIfAbsent(category, new EmailUtil(category));
            instance = _emailInstanceMap.get(category);
        }
        return instance;
    }

    public static void clear() {
        _emailInstanceMap.clear();
    }

    public void sendMail(String toMail, String ccMail, String mailTitle, String mailContent) throws Exception {
        log.info("send email to:" + toMail + " cc:" + ((ccMail != null) ? ccMail : "") + " title:" + mailTitle);
        if (Config.getInstance()._serverType != 3) {
            if (_account != null && _account.length() > 0 && _password != null && _password.length() > 0) {
                Session session = Session.getInstance(_properties);
                // session.setDebug(true);
                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(_from_email));
                message.setRecipient(Message.RecipientType.TO, new InternetAddress(toMail));
                if (ccMail != null) {
                    message.setRecipient(Message.RecipientType.CC, new InternetAddress(ccMail));
                }
                message.setSubject(mailTitle);
                // message.setText(mailContent); //send text only
                message.setContent(mailContent, "text/html;charset=utf-8");
                message.setSentDate(new Date());
                // javax.mail.Address[] replyTo = { new InternetAddress(_reply_address, _reply_address_name) };
                // message.setReplyTo(replyTo);

                message.saveChanges();

                Transport transport = session.getTransport("smtp");
                transport.connect(_account, _password);
                transport.sendMessage(message, message.getAllRecipients());

                transport.close();
            } else {
                log.info("send email to:" + toMail + " cc:" + ((ccMail != null) ? ccMail : "")
                        + " missing email account info");
            }
        }
    }
}
