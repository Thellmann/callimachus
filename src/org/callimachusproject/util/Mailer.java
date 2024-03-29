package org.callimachusproject.util;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.naming.NamingException;

import org.callimachusproject.server.exceptions.BadRequest;
import org.callimachusproject.server.exceptions.NotImplemented;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mailer {
	private static final DomainNameSystemResolver resolver = DomainNameSystemResolver.getInstance();
	private static final Pattern HTML_TITLE = Pattern
			.compile("<title>\\s*([^<]*)\\s*<.title>");
	private final Logger logger = LoggerFactory
			.getLogger(Mailer.class);
	private final String fromUser;
	private final String fromEmail;
	private final MailProperties file;

	public Mailer() {
		this.fromEmail = null;
		this.fromUser = null;
		this.file = MailProperties.getInstance();
	}

	public Mailer(String fromName, String fromEmail) {
		this.fromEmail = fromEmail;
		this.fromUser = fromName == null ? null : (fromName + " <" + fromEmail + ">");
		this.file = MailProperties.getInstance();
	}

	public void sendMessage(String html, String recipient) throws IOException,
			MessagingException, NamingException {
		try {
			sendMessage(html, Collections.singleton(recipient));
		} catch (IOException e) {
			logger.error(e.toString(), e);
			throw e;
		} catch (MessagingException e) {
			logger.error(e.toString(), e);
			throw e;
		} catch (NamingException e) {
			logger.error(e.toString(), e);
			throw e;
		}
	}

	public void sendMessage(String html, Set<String> recipients)
			throws IOException, MessagingException, NamingException {
		if (recipients == null || recipients.isEmpty())
			throw new BadRequest("Missing to paramenter");
		Properties properties = file.loadMailProperties();
		Session session = Session.getInstance(properties);
		MimeMessage message = new MimeMessage(session);
		message.setSentDate(new Date());
		boolean fromSelf = false;
		if (recipients.size() == 1) {
			String to = recipients.iterator().next();
			message.addRecipients(RecipientType.TO, to);
			if (fromEmail != null) {
				fromSelf |= to.contains(fromEmail);
			}
		} else {
			for (String to : recipients) {
				message.addRecipients(RecipientType.BCC, to);
				if (fromEmail != null) {
					fromSelf |= to.contains(fromEmail);
				}
			}
		}
		if (fromEmail == null || fromSelf) {
			message.setFrom();
		} else {
			message.setFrom(new InternetAddress(fromUser));
		}
		Matcher m = HTML_TITLE.matcher(html);
		if (m.find()) {
			message.setSubject(m.group(1));
		} else if (!html.startsWith("<")) {
			message.setSubject(html.substring(0, html.indexOf('\n')));
		}
		if (html.startsWith("<")) {
			message.setText(html, "UTF-8", "html");
		} else {
			message.setText(html.substring(html.indexOf('\n') + 1));
		}
		/*
			MimeMultipart multipart = new MimeMultipart();
			MimeBodyPart part1 = new MimeBodyPart();
			if (html.startsWith("<")) {
				part1.setText(html, "UTF-8", "html");
			} else {
				part1.setText(html.substring(html.indexOf('\n') + 1));
			}
			multipart.addBodyPart(part1);
			MimeBodyPart part2 = new MimeBodyPart();
			ByteArrayDataSource source = new ByteArrayDataSource(in, mime);
			part2.setDataHandler(new DataHandler(source));
			part2.setDisposition("inline");
			multipart.addBodyPart(part2);
			message.setContent(multipart);
		*/
		message.saveChanges();
		if (properties.containsKey("mail.transport.protocol")) {
			String user = session.getProperty("mail.user");
			String password = session.getProperty("mail.password");
			Transport tr = session.getTransport();
			try {
				tr.connect(user, password);
				tr.sendMessage(message, message.getAllRecipients());
			} finally {
				tr.close();
			}
		} else {
			properties.setProperty("mail.transport.protocol", "smtp");
			for (String to : recipients) {
				String domain = extractDomain(to);
				String host = findMailServer(domain);
				if (host == null)
					throw new NotImplemented("No Mail Server Configured");
				Transport tr = session.getTransport();
				try {
					tr.connect(host, 25, null, null);
					tr.sendMessage(message, message.getAllRecipients());
					break;
				} finally {
					tr.close();
				}
			}
		}
		logger.info("Sent e-mail {} {} {}", new Object[] { recipients,
				message.getSubject(), message.getMessageID() });
	}

	private String extractDomain(String to) {
		if (to.indexOf('>') < 0)
			return to.substring(to.indexOf('@') + 1);
		return to.substring(to.indexOf('@') + 1, to.indexOf('>'));
	}

	private String findMailServer(String domain) throws NamingException {
		String value = resolver.lookup(domain, "MX");
		if (value.indexOf(' ') >= 0)
			return value.substring(value.indexOf(' ') + 1);
		return value;
	}
}
