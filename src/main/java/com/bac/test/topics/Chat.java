package com.bac.test.topics;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class Chat implements MessageListener{
	
	private TopicSession pubSession;
	private TopicPublisher publisher;
	private TopicConnection connection;
	private String userName;
	
	public Chat(String topicFactory, String topicName, String userName) throws NamingException, JMSException {
		InitialContext ctx = new InitialContext();
		TopicConnectionFactory connFactory = (TopicConnectionFactory)ctx.lookup(topicFactory);
		TopicConnection connection = connFactory.createTopicConnection();
		
		TopicSession pubSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		TopicSession subSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
		
		Topic topic = (Topic)ctx.lookup(topicName);		
		TopicPublisher publisher = pubSession.createPublisher(topic);
		TopicSubscriber subscriber = subSession.createSubscriber(topic);		
		subscriber.setMessageListener(this);
		
		this.connection = connection;
		this.pubSession = pubSession;
		this.publisher = publisher;
		this.userName = userName;		
		connection.start();
	}
	
	@Override
	public void onMessage(Message message) {
		try {
			TextMessage textMessage = (TextMessage)message;
			System.out.println(textMessage.getText());
		}catch(JMSException jmse) {
			jmse.printStackTrace();
		}
	}

	protected void writeMessage(String text) {
		TextMessage message = null;
		try {
			message = pubSession.createTextMessage();
			message.setText(userName+" : "+text);
			publisher.publish(message);
		} catch (JMSException e) {
			e.printStackTrace();
		}		
	}
	
	public void close() {
		try {
			connection.close();
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws NamingException, JMSException, IOException {
		//args[0] = TopicCF
		//args[1] = topic1
		//args[]2 = Shahnawaz
		Chat chat = new Chat(args[0], args[1], args[2]);
				
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			String s = reader.readLine();
			if(s.equalsIgnoreCase("exit")) {
				chat.close();
				System.exit(0);
			}else {
				chat.writeMessage(s);
			}
		}
	}
}
