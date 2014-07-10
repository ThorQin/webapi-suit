/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.mail;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author nuo.qin
 */
public class MailService {

	public static class Mail {
		public String from = null;
		public String[] to = null;
		public String subject = null;
		public String htmlBody;
		public String textBody;
		private final List<String> attachments = new LinkedList<>();
		public void addAttachment(String filePath) {
			attachments.add(filePath);
		}
		public void clearAttachment() {
			attachments.clear();
		}
	}
	
	private static final Logger logger = Logger.getLogger(MailService.class.getName());
	private final LinkedBlockingQueue<Mail> mailQueue = new LinkedBlockingQueue<>();
	private Thread sendThread = null;
	private boolean alive = false;
	private MailConfig serverConfig = null;
	private final Mail stopMail = new Mail();
	
	
	public MailService(String config) throws IOException {
		serverConfig = new MailConfig(config);
	}
	
	private void doSendMail(Mail mail) {
		Properties props = new Properties();
		Session session;
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.host", serverConfig.getHost());
		if (serverConfig.getPort() != null) {
			props.put("mail.smtp.port", serverConfig.getPort());
		}
		if (serverConfig.getSecure().equals(MailConfig.SECURE_STARTTLS)) {
			props.put("mail.smtp.starttls.enable", "true");
			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(serverConfig.getUsername(), serverConfig.getPassword());
						}
					});
		} else if (serverConfig.getSecure().equals(MailConfig.SECURE_SSL)) {
			props.put("mail.smtp.socketFactory.port", serverConfig.getPort());
			props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			props.put("mail.smtp.socketFactory.fallback", "false");
			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(serverConfig.getUsername(), serverConfig.getPassword());
						}
					});
		} else {
			session = Session.getInstance(props,
					new javax.mail.Authenticator() {
						@Override
						protected PasswordAuthentication getPasswordAuthentication() {
							return new PasswordAuthentication(serverConfig.getUsername(), serverConfig.getPassword());
						}
					});
		}
		
		// Uncomment to show SMTP protocal
		// session.setDebug(true);

		MimeMessage message = new MimeMessage(session);
		StringBuilder mailTo = new StringBuilder();
		try {
			if (mail.from != null)
				message.setFrom(new InternetAddress(mail.from));
			else if (serverConfig.getFrom() != null)
				message.setFrom(new InternetAddress(serverConfig.getFrom()));
			if (mail.to != null) {
				for (String to : mail.to) {
					if (mailTo.length() > 0)
						mailTo.append(",");
					mailTo.append(to);
					message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
				}
			}
			if (mail.subject != null)
				message.setSubject("=?UTF-8?B?" +
							Base64.encodeBase64String(mail.subject.getBytes("utf-8"))
							+ "?=");
			message.setSentDate(new Date());
			
			BodyPart bodyPart = new MimeBodyPart();
			if (mail.htmlBody != null)
				bodyPart.setContent(mail.htmlBody, "text/html;charset=utf-8");
			else if (mail.textBody != null)
				bodyPart.setText(mail.textBody);
			Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(bodyPart);
			
			if (mail.attachments != null) {
				for (String attachment : mail.attachments) {
					BodyPart attachedBody = new MimeBodyPart();
					File attachedFile = new File(attachment);
					DataSource source = new FileDataSource(attachedFile);
					attachedBody.setDataHandler(new DataHandler(source));
					attachedBody.setDisposition(MimeBodyPart.ATTACHMENT);
					String filename = attachedFile.getName();
					attachedBody.setFileName("=?UTF-8?B?" +
							Base64.encodeBase64String(filename.getBytes("utf-8"))
							+ "?=");
					multipart.addBodyPart(attachedBody);
				}
			}
			
			message.setContent(multipart);
			message.saveChanges();
			Transport transport = session.getTransport("smtp");
			transport.connect(serverConfig.getHost(), 
					serverConfig.getPort(), 
					serverConfig.getUsername(), 
					serverConfig.getPassword());
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();
			logger.log(Level.INFO, "Mail sent: {0}", mailTo.toString());
		} catch (Exception ex) {
			logger.log(Level.SEVERE, "Send mail failed!", ex);
		}
	}
	
	public void start() {
		if (alive)
			return;
		alive = true;
		sendThread = new Thread(new Runnable() {
			@Override
			public void run() {
				System.out.println("Mail service started!");
				while (alive) {
					Mail mail = null;
					try {
						mail = mailQueue.take();
					} catch (InterruptedException e) {
					}
					try {
						if (mail != stopMail)
							doSendMail(mail);
						else if (!alive)
							break;
					} catch (Exception ex) {
						logger.log(Level.SEVERE, null, ex);
					}
				}
			}
		});
		// After server shutdown keep the thread running until all task is finished.
		// sendThread.setDaemon(false);
		sendThread.start();
	}
	public void stop() {
		if (!alive)
			return;
		alive = false;
		mailQueue.offer(stopMail);
		try {
			sendThread.join(30000);
		} catch (InterruptedException e) {
		}
		System.out.println("Mail service stopped!!");
	}
	
	public void sendMail(Mail mail) {
		if (mailQueue != null && alive)
			mailQueue.offer(mail);
	}
	
	public static Mail createHtmlMailFromTemplate(String templatePath, Map<String,String> replaced) {
		Mail mail = new Mail();
		try (InputStream in = MailService.class.getClassLoader().getResourceAsStream(templatePath)) {
			InputStreamReader reader = new InputStreamReader(in, "utf-8");
			char[] buffer = new char[1024];
			StringBuilder builder = new StringBuilder();
			while (reader.read(buffer) != -1)
				builder.append(buffer);
			String mailBody = builder.toString();
			builder.setLength(0);
			Pattern pattern = Pattern.compile(
					"<%\\s*(.+?)\\s*%>",
					Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(mailBody);
			int scanPos = 0;
			while (matcher.find()) {
				builder.append(mailBody.substring(scanPos, matcher.start()));
				scanPos = matcher.end();
				String key = matcher.group(1);
				if (replaced != null) {
					String value = replaced.get(key);
					if (value != null) {
						builder.append(value);
					}
				}
			}
			builder.append(mailBody.substring(scanPos, mailBody.length()));
			mail.htmlBody = builder.toString();
			pattern = Pattern.compile("<title>(.*)</title>", 
					Pattern.MULTILINE | Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
			matcher = pattern.matcher(mail.htmlBody);
			if (matcher.find()) {
				mail.subject = matcher.group(1);
			}
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Create mail from template error: {0}, {1}", 
					new Object[]{templatePath, ex});
		}
		return mail;
	}
}
