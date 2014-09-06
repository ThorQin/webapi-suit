package com.github.thorqin.webapi.amq;

import com.github.thorqin.webapi.WebApplication;
import com.github.thorqin.webapi.utility.Serializer;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.ConfigurationException;

public class AMQ {
	private boolean enableTrace = false;
	private Connection connection = null;
	private String defaultAddress = null;
	private boolean defaultBroadcast = false;
	private final ThreadLocal<ProducerHolder> localProducer = new ThreadLocal<>();
	
	
	private class ProducerHolder {
		public Session session = null;
		public MessageProducer producer = null;
		public Destination replyQueue = null;
		public MessageConsumer replyConsumer = null;
		public ProducerHolder() throws JMSException {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			producer = session.createProducer(null);
			replyQueue = session.createTemporaryQueue();
			replyConsumer = session.createConsumer(replyQueue);
		}
		
		public void close() {
			closeResource(producer);
			closeResource(replyConsumer);
			closeResource(session);
		}
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
	}
	
	private ProducerHolder createHolder() throws JMSException {
		ProducerHolder obj = localProducer.get();
		if (obj == null) {
			obj = new ProducerHolder();
			localProducer.set(obj);
		}
		return obj;
	}
	
	public class IncomingMessage {
		private final String address;
		private final String subject;
		private final String contentType;
		private final int replyCode;
		private final boolean isJsonEncoding;
		private final byte[] body;
		private final Destination replyDestination;
		private final String correlationID;

		public IncomingMessage(
				String address, 
				String subject,
				String contentType,
				boolean isJsonEncoding,
				int replyCode,
				byte[] body,
				Destination replyDestination,
				String correlationID) {
			this.address = address;
			this.subject = subject;
			this.contentType = contentType;
			this.isJsonEncoding = isJsonEncoding;
			this.body = body;
			this.replyDestination = replyDestination;
			this.correlationID = correlationID;
			this.replyCode = replyCode;
		}
		public String getAddress() {
			return address;
		}
		public String getSubject() {
			return subject;
		}
		public String getContentType() {
			return contentType;
		}
		public boolean isJsonEncoding() {
			return isJsonEncoding;
		}
		public int getReplyCode() {
			return replyCode;
		}
		public byte[] getBodyBytes() {
			return body;
		}
		public <T> T getBody() throws IOException {
			if (isJsonEncoding) {
				return Serializer.fromJson(body);
			} else {
				return Serializer.fromKryo(body);
			}
		}
		public <T> T getBody(Type type) throws IOException {
			if (isJsonEncoding) {
				return Serializer.fromJson(body, type);
			} else {
				return Serializer.fromKryo(body);
			}
		}
		public <T> T getBody(Class<T> type) throws IOException {
			if (isJsonEncoding) {
				return Serializer.fromJson(body, type);
			} else {
				return Serializer.fromKryo(body);
			}
		}
		public String getCorrelationID() {
			return correlationID;
		}
		public boolean needReply() {
			return replyDestination != null;
		}
		public String getReplyAddress() throws JMSException {
			if (replyDestination == null)
				return null;
			else {
				if (replyDestination instanceof javax.jms.Queue) {
					javax.jms.Queue queue = (javax.jms.Queue)replyDestination;
					return queue.getQueueName();
				} else
					return null;
			}
		}
		public void reply(byte[] replyMessage, String contentType, boolean useJsonEncoding)
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyMessage, contentType, useJsonEncoding, replyDestination, correlationID);
		}
		public void reply(int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding)
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, contentType, useJsonEncoding, replyDestination, correlationID);
		}
		public void reply(byte[] replyMessage, String contentType, boolean useJsonEncoding, long timeToLive)
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, 0, replyMessage, contentType, useJsonEncoding, replyDestination, correlationID, timeToLive);
		}
		public void reply(int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding, long timeToLive)
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, contentType, useJsonEncoding, replyDestination, correlationID, timeToLive);
		}
		public <T> void reply(T replyMessage) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyMessage, replyDestination, correlationID);
		}
		public <T> void reply(int replyCode, T replyMessage) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, replyDestination, correlationID);
		}
		public <T> void reply(T replyMessage, long timeToLive) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyMessage, replyDestination, correlationID, timeToLive);
		}
		public <T> void reply(int replyCode, T replyMessage, long timeToLive) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, replyDestination, correlationID, timeToLive);
		}
		public <T> void reply(T replyMessage, boolean useJsonEncoding) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyMessage, useJsonEncoding, replyDestination, correlationID);
		}
		public <T> void reply(int replyCode, T replyMessage, boolean useJsonEncoding) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, useJsonEncoding, replyDestination, correlationID);
		}
		public <T> void reply(T replyMessage, boolean useJsonEncoding, long timeToLive) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyMessage, useJsonEncoding, replyDestination, correlationID, timeToLive);
		}
		public <T> void reply(int replyCode, T replyMessage, boolean useJsonEncoding, long timeToLive) 
				throws JMSException, IOException {
			if (replyDestination == null)
				return;
			Replier replier = createReplier();
			replier.reply(subject, replyCode, replyMessage, useJsonEncoding, replyDestination, correlationID, timeToLive);
		}
	}

	public interface MessageHandler {
		void onMessage(IncomingMessage message);
	}
	
	public class Replier {
		private ProducerHolder holder = null;
		private final int deliveryMode = DeliveryMode.NON_PERSISTENT;
		private final long defaultTimeToLive = 30000l;
		protected Replier() throws JMSException {
			if (connection == null)
				throw new JMSException("Connection not allocate.");
			holder = createHolder();
			holder.producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		}
		public <T> void reply(String subject, T replyMessage, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, destination, correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, T replyMessage, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, destination, correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, T replyMessage, Destination destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, 0, replyMessage, destination, correlationID, timeToLive);
		}
		public <T> void reply(String subject, T replyMessage, String destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, 0, replyMessage, holder.session.createQueue(destination), correlationID, timeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, String destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, holder.session.createQueue(destination), correlationID, timeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, Destination destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			byte[] bytes = Serializer.toKryo(replyMessage);
			reply(subject, replyCode, bytes, Serializer.getTypeName(replyMessage), false, destination, correlationID, timeToLive);
		}
		public <T> void reply(String subject, T replyMessage, boolean useJsonEncoding, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, useJsonEncoding, destination, correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, T replyMessage, boolean useJsonEncoding, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, useJsonEncoding, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, boolean useJsonEncoding, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, useJsonEncoding, destination, correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, boolean useJsonEncoding, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, useJsonEncoding, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public <T> void reply(String subject, T replyMessage, boolean useJsonEncoding, Destination destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, 0, replyMessage, useJsonEncoding, destination, correlationID, timeToLive);
		}
		public <T> void reply(String subject, T replyMessage, boolean useJsonEncoding, String destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, 0, replyMessage, useJsonEncoding, holder.session.createQueue(destination), correlationID, timeToLive);
		}
		public <T> void reply(String subject, int replyCode, T replyMessage, boolean useJsonEncoding, String destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, useJsonEncoding, 
					holder.session.createQueue(destination), correlationID, timeToLive);
		}
		
		public <T> void reply(String subject, int replyCode, T replyMessage, boolean useJsonEncoding, Destination destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(replyMessage);
			else
				bytes = Serializer.toKryo(replyMessage);
			reply(subject, replyCode, bytes, Serializer.getTypeName(replyMessage), useJsonEncoding, destination, correlationID, timeToLive);
		}
		public void reply(String subject, byte[] replyMessage, String contentType, boolean useJsonEncoding, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, contentType, useJsonEncoding, destination, correlationID, defaultTimeToLive);
		}
		public void reply(String subject, byte[] replyMessage, String contentType, boolean useJsonEncoding, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, 0, replyMessage, contentType, useJsonEncoding, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public void reply(String subject, int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding, Destination destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, contentType, useJsonEncoding, destination, correlationID, defaultTimeToLive);
		}
		public void reply(String subject, int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding, String destination,
				String correlationID) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, contentType, useJsonEncoding, holder.session.createQueue(destination), correlationID, defaultTimeToLive);
		}
		public void reply(String subject, int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding, Destination destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			BytesMessage bytesMessage = holder.session.createBytesMessage();
			bytesMessage.writeBytes(replyMessage);
			bytesMessage.setJMSCorrelationID(correlationID);
			bytesMessage.setStringProperty("subject", subject);
			bytesMessage.setStringProperty("contentType", contentType);
			bytesMessage.setBooleanProperty("jsonEncoding", useJsonEncoding);
			bytesMessage.setIntProperty("replyCode", replyCode);
			holder.producer.send(destination, bytesMessage, deliveryMode, 5, timeToLive);
		}
		public void reply(String subject, int replyCode, byte[] replyMessage, String contentType, boolean useJsonEncoding, String destination,
				String correlationID, long timeToLive) throws IOException, JMSException {
			reply(subject, replyCode, replyMessage, contentType, useJsonEncoding, 
					holder.session.createQueue(destination),correlationID, timeToLive);
		}
	}
	
	public class Sender {
		private ProducerHolder holder = null;
		private String address = null;
		private Destination dest = null;
		private int deliveryMode = DeliveryMode.NON_PERSISTENT;
		private final long defaultTimeout = 30000l;
		protected Sender(String address, boolean broadcast, boolean persistent) throws JMSException {
			if (connection == null)
				throw new JMSException("Connection not allocate.");
			this.address = address;
			holder = createHolder();
			if (broadcast)
				dest = holder.session.createTopic(address);
			else
				dest = holder.session.createQueue(address);
			deliveryMode = persistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT;
		}
		public String getAddress() {
			return address;
		}
		public <T> void send(String subject, T message) throws IOException, JMSException {
			byte[] bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), false, 0);
		}
		public <T> void send(String subject, T message, String replyAddress, String correlationID) throws IOException, JMSException {
			byte[] bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), replyAddress, correlationID, false, 0);
		}
		public <T> void send(String subject, T message, boolean useJsonEncoding) throws IOException, JMSException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(message);
			else
				bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), useJsonEncoding, 0);
		}
		public <T> void send(String subject, T message, String replyAddress, String correlationID, boolean useJsonEncoding) throws IOException, JMSException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(message);
			else
				bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), replyAddress, correlationID, useJsonEncoding, 0);
		}
		public <T> void send(String subject, T message, long timeToLive) throws IOException, JMSException {
			byte[] bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), false, timeToLive);
		}
		public <T> void send(String subject, T message, String replyAddress, String correlationID, long timeToLive) throws IOException, JMSException {
			byte[] bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), replyAddress, correlationID, false, timeToLive);
		}
		public <T> void send(String subject, T message, boolean useJsonEncoding, long timeToLive) throws IOException, JMSException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(message);
			else
				bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), useJsonEncoding, timeToLive);
		}
		public <T> void send(String subject, T message, String replyAddress, String correlationID, boolean useJsonEncoding, long timeToLive) throws IOException, JMSException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(message);
			else
				bytes = Serializer.toKryo(message);
			send(subject, bytes, Serializer.getTypeName(message), replyAddress, correlationID, useJsonEncoding, timeToLive);
		}
		public void send(String subject, byte[] message, String contentType, boolean useJsonEncoding, long timeToLive) 
				throws IOException, JMSException {
			BytesMessage bytesMessage = holder.session.createBytesMessage();
			bytesMessage.writeBytes(message);
			bytesMessage.setStringProperty("subject", subject);
			bytesMessage.setStringProperty("contentType", contentType);
			bytesMessage.setBooleanProperty("jsonEncoding", useJsonEncoding);
			holder.producer.send(dest, bytesMessage, deliveryMode, 4, timeToLive);
		}
		public void send(String subject, byte[] message, String contentType, String replyAddress, String correlationID, boolean useJsonEncoding, long timeToLive) 
				throws IOException, JMSException {
			BytesMessage bytesMessage = holder.session.createBytesMessage();
			bytesMessage.writeBytes(message);
			bytesMessage.setStringProperty("subject", subject);
			bytesMessage.setStringProperty("contentType", contentType);
			bytesMessage.setBooleanProperty("jsonEncoding", useJsonEncoding);
			if (replyAddress != null)
				bytesMessage.setJMSReplyTo(holder.session.createQueue(replyAddress));
			if (correlationID != null)
				bytesMessage.setJMSCorrelationID(correlationID);
			holder.producer.send(dest, bytesMessage, deliveryMode, 4, timeToLive);
		}
		
		public <T> IncomingMessage sendAndWaitForReply(String subject, T message) 
				throws IOException, JMSException, TimeoutException {
			return sendAndWaitForReply(subject, message, defaultTimeout);
		}
		
		public <T> IncomingMessage sendAndWaitForReply(String subject, T message, long timeout)  
				throws IOException, JMSException, TimeoutException {
			byte[] bytes = Serializer.toKryo(message);
			String contentType = Serializer.getTypeName(message);
			return sendAndWaitForReply(subject, bytes, contentType, false, timeout);
		}
		public <T> IncomingMessage sendAndWaitForReply(String subject, T message, boolean useJsonEncoding, long timeout)  
				throws IOException, JMSException, TimeoutException {
			byte[] bytes;
			if (useJsonEncoding)
				bytes = Serializer.toJsonBytes(message);
			else
				bytes = Serializer.toKryo(message);
			String contentType = Serializer.getTypeName(message);
			return sendAndWaitForReply(subject, bytes, contentType, useJsonEncoding, timeout);
		}

		public IncomingMessage sendAndWaitForReply(String subject, byte[] message, String contentType, 
				boolean useJsonEncoding, long timeout) throws IOException, 
				JMSException, TimeoutException {
			
			String correlationID = UUID.randomUUID().toString();

			BytesMessage bytesMessage = holder.session.createBytesMessage();
			bytesMessage.writeBytes(message);
			bytesMessage.setStringProperty("subject", subject);
			bytesMessage.setStringProperty("contentType", contentType);
			bytesMessage.setBooleanProperty("jsonEncoding", useJsonEncoding);
			bytesMessage.setJMSReplyTo(holder.replyQueue);
			bytesMessage.setJMSCorrelationID(correlationID);
			holder.producer.send(dest, bytesMessage, deliveryMode, 4, timeout);
			Message replyMessage = null;
			for (;;) { // Ignore other message
				replyMessage = holder.replyConsumer.receive(timeout);
				if (replyMessage == null) {
					break;
				}
				if (replyMessage.getJMSCorrelationID().equals(correlationID)) {
					break;
				}
			}
			if (replyMessage == null) {
				throw new TimeoutException("Reply timeout: " + correlationID);
			}
			byte[] replyBytes = getBytes(replyMessage);
			return new IncomingMessage(
					address, 
					replyMessage.getStringProperty("subject"), 
					replyMessage.getStringProperty("contentType"), 
					replyMessage.getBooleanProperty("jsonEncoding"), 
					getReplyCode(replyMessage),
					replyBytes, null, null);
		}
	}
	
	private static int getReplyCode(Message message) throws JMSException {
		if (message.propertyExists("replyCode")) {
			return message.getIntProperty("replyCode");
		} else
			return 0;
	}
	
	public class Receiver {
		private Session session = null;
		private MessageConsumer consumer = null;
		private String address = null;

		protected Receiver(String address, boolean broadcast, String filter) throws JMSException {
			if (connection == null)
				throw new JMSException("Connection not allocate.");
			this.address = address;
			session = connection.createSession(false, 
					Session.AUTO_ACKNOWLEDGE);
			Destination dest;
			if (broadcast)
				dest = session.createTopic(address);
			else
				dest = session.createQueue(address);
			if (filter != null)
				consumer = session.createConsumer(dest, filter);
			else
				consumer = session.createConsumer(dest);
		}
		
		public String getAddress() {
			return address;
		}
		
		public IncomingMessage receive() throws IOException,
				JMSException {
			Message message = consumer.receive();
			IncomingMessage inMessage = new IncomingMessage(
					address,
					message.getStringProperty("subject"), 
					message.getStringProperty("contentType"),
					message.getBooleanProperty("jsonEncoding"),
					getReplyCode(message),
					getBytes(message), 
					message.getJMSReplyTo(), 
					message.getJMSCorrelationID());
			return inMessage;
		}

		public IncomingMessage receive(long timout)
				throws IOException, JMSException {
			Message message = consumer.receive(timout);
			IncomingMessage inMessage = new IncomingMessage(
					address,
					message.getStringProperty("subject"),
					message.getStringProperty("contentType"),
					message.getBooleanProperty("jsonEncoding"),
					getReplyCode(message),
					getBytes(message), 
					message.getJMSReplyTo(), 
					message.getJMSCorrelationID());
			return inMessage;
		}

		public IncomingMessage receiveNoWait()
				throws IOException, JMSException {
			Message message = consumer.receiveNoWait();
			IncomingMessage inMessage = new IncomingMessage(
					address,
					message.getStringProperty("subject"), 
					message.getStringProperty("contentType"),
					message.getBooleanProperty("jsonEncoding"),
					getReplyCode(message),
					getBytes(message), 
					message.getJMSReplyTo(), 
					message.getJMSCorrelationID());
			return inMessage;
		}
		public void close() {
			closeResource(consumer);
			closeResource(session);
		}
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
	}
	
	public class AsyncReceiver {
		private Session session = null;
		private MessageConsumer consumer = null;
		private MessageHandler handler = null;
		private String address = null;
		
		public String getAddress() {
			return address;
		}
		
		private final MessageListener listener = new MessageListener() {
			@Override
			public void onMessage(Message message) {
				try {
					if (handler != null) {
						IncomingMessage inMessage = new IncomingMessage(
								address,
								message.getStringProperty("subject"), 
								message.getStringProperty("contentType"), 
								message.getBooleanProperty("jsonEncoding"),
								getReplyCode(message),
								getBytes(message), 
								message.getJMSReplyTo(), 
								message.getJMSCorrelationID());
						handler.onMessage(inMessage);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		protected AsyncReceiver(String address, boolean broadcast, String filter, 
				MessageHandler handler) throws JMSException {
			if (connection == null)
				throw new JMSException("Connection not allocate.");
			this.address = address;
			this.handler = handler; 
			session = connection.createSession(false, 
					Session.AUTO_ACKNOWLEDGE);
			Destination dest;
			if (broadcast)
				dest = session.createTopic(address);
			else
				dest = session.createQueue(address);
			if (filter != null)
				consumer = session.createConsumer(dest, filter);
			else
				consumer = session.createConsumer(dest);
			consumer.setMessageListener(listener);
		}
		public void close() {
			closeResource(consumer);
			closeResource(session);
		}
		@Override
		protected void finalize() throws Throwable {
			close();
			super.finalize();
		}
	}
	
	public AMQ(String uri, String user, String password) 
					throws ConfigurationException, JMSException {
		init(uri, user, password, "default", false);
	}
	public AMQ(String uri, String user, String password, String defaultAddress, boolean defaultBroadcast) 
					throws ConfigurationException, JMSException {
		init(uri, user, password, defaultAddress, defaultBroadcast);
	}
	public AMQ(WebApplication application, String configFile) throws ConfigurationException, JMSException, IOException, URISyntaxException {
		init(application, configFile);
	}
	
	public void setDefaultAddress(String address) {
		this.defaultAddress = address;
	}
	
	public String getDefaultAddress() {
		return this.defaultAddress;
	}
	
	public void setDefaultBroadcast(boolean broadcast) {
		this.defaultBroadcast = broadcast;
	}
	
	public boolean getDefaultBroadcast() {
		return this.defaultBroadcast;
	}
	
	private static void closeResource(MessageConsumer resource) {
		try {
			if (resource != null)
				resource.close();
		} catch (Exception e) {
		}
	}
	private static void closeResource(MessageProducer resource) {
		try {
			if (resource != null)
				resource.close();
		} catch (Exception e) {
		}
	}
	private static void closeResource(Session resource) {
		try {
			if (resource != null)
				resource.close();
		} catch (Exception e) {
		}
	}
	private static void closeResource(Connection resource) {
		try {
			if (resource != null) {
				resource.stop();
				resource.close();
			}
		} catch (Exception e) {
		}
	}

	private void init(WebApplication application, String configFile) throws ConfigurationException, JMSException, IOException, URISyntaxException {
		AMQConfig config = new AMQConfig(application, configFile);
		String uri = config.getActiveMQUri();
		String user = config.getActiveMQUser();
		String password = config.getActiveMQPassword();
		String defAddress = config.getDefaultAddress();
		boolean defBroadcast = config.getDefaultBroadcast();
		this.enableTrace = config.enableTrace();
		init(uri, user, password, defAddress, defBroadcast);
	}
	private void init(String uri, String user, String password, String defaultAddress, boolean defaultBroadcast) throws ConfigurationException, JMSException {
		ActiveMQConnectionFactory connectionFactory;
		if (uri == null || uri.trim().length() == 0)
			throw new ConfigurationException("Must provide the ActiveMQ URI info.");
		if (user != null && user.trim().length() > 0)
			connectionFactory = new ActiveMQConnectionFactory(user, password, uri);
		else
			connectionFactory = new ActiveMQConnectionFactory(uri);
		connection = connectionFactory.createConnection();
	    connection.start();
		this.defaultAddress = defaultAddress;
		this.defaultBroadcast = defaultBroadcast;
	}
	
	public boolean enableTrace() {
		return enableTrace;
	}
	
	public void stop() {
		closeResource(connection);
		connection = null;
	}
	
	private byte[] getBytes(Message message) throws IOException, JMSException {
		if (message == null)
			return null;
		else {
			if (message instanceof BytesMessage) {
				BytesMessage bytesMessage = (BytesMessage)message;
				byte[] content = new byte[(int) bytesMessage.getBodyLength()];
				bytesMessage.readBytes(content);
				return content;
			} else {
				throw new JMSException("Invalid message: wrong type.");
			}
		}
	}
	
	public Replier createReplier() throws JMSException {
		return new Replier();
	}
	public Sender createSender(boolean persistent) throws JMSException {
		return new Sender(defaultAddress, defaultBroadcast, persistent);
	}
	public Sender createSender(String address, boolean broadcast, boolean persistent) throws JMSException {
		return new Sender(address, broadcast, persistent);
	}
	public Sender createSender() throws JMSException {
		return new Sender(defaultAddress, defaultBroadcast, false);
	}
	public Sender createSender(String address, boolean broadcast) throws JMSException {
		return new Sender(address, broadcast, false);
	}
	public Sender createSender(String address) throws JMSException {
		return new Sender(address, false, false);
	}
	public Receiver createReceiver() throws JMSException {
		return new Receiver(defaultAddress, defaultBroadcast, null);
	}
	public Receiver createReceiver(String address,
			boolean broadcast) throws JMSException {
		return new Receiver(address, broadcast, null);
	}
	public Receiver createReceiver(String address) throws JMSException {
		return new Receiver(address, false, null);
	}
	public Receiver createReceiver(String address, String filter) 
			throws JMSException {
		return new Receiver(address, false, filter);
	}
	public Receiver createReceiver(String address,
			boolean broadcast, String filter) throws JMSException {
		return new Receiver(address, broadcast, filter);
	}
	public AsyncReceiver createAsyncReceiver(MessageHandler handler) throws JMSException {
		return new AsyncReceiver(defaultAddress, defaultBroadcast, null, handler);
	}
	public AsyncReceiver createAsyncReceiver(String address, boolean broadcast, 
			MessageHandler handler) throws JMSException {
		return new AsyncReceiver(address, broadcast, null, handler);
	}
	public AsyncReceiver createAsyncReceiver(String address,
			MessageHandler handler) throws JMSException {
		return new AsyncReceiver(address, false, null, handler);
	}
	public AsyncReceiver createAsyncReceiver(String address, String filter,
			MessageHandler handler) throws JMSException {
		return new AsyncReceiver(address, false, filter, handler);
	}
	public AsyncReceiver createAsyncReceiver(String address, boolean broadcast, 
			String filter, MessageHandler handler) throws JMSException {
		return new AsyncReceiver(address, broadcast, filter, handler);
	}

	@Override
	protected void finalize() throws Throwable {
		stop();
		super.finalize();
	}
}
