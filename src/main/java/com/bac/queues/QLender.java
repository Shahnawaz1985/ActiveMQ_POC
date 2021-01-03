package com.bac.queues;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

public class QLender implements MessageListener{
	
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue requestQ = null;
	
	public QLender(String queuecf, String requestQueue) {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory qFactory = (QueueConnectionFactory)ctx.lookup(queuecf);
			qConnect = qFactory.createQueueConnection();
			
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			requestQ = (Queue)ctx.lookup(requestQueue);
			qConnect.start();
			
			QueueReceiver qReceiver = qSession.createReceiver(requestQ);
			qReceiver.setMessageListener(this);
			
			System.out.println("Waiting for loan requests...");
		}catch(JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		}catch(NamingException jne) {
			jne.printStackTrace();
			System.exit(1);
		}
	}

	@Override
	public void onMessage(Message message) {
		try {
			boolean accepted = false;
			MapMessage msg = (MapMessage)message;
			double salary = msg.getDouble("Salary");
			double loanAmt = msg.getDouble("LoanAmount");
			
			if(loanAmt < 200000) {
				accepted = (salary/loanAmt) > .25;
			}else {
				accepted = (salary/loanAmt) > .33;
			}
			
			System.out.println("Percent = "+(salary/loanAmt)+", loan is "+(accepted ? "Accepted" : "Declined"));
			TextMessage tmsg = qSession.createTextMessage();
			tmsg.setText(accepted ? "Accepted" : "Declined");
			tmsg.setJMSCorrelationID(message.getJMSMessageID());
			
			QueueSender qSender = qSession.createSender((Queue)message.getJMSReplyTo());
			qSender.send(tmsg);
			
			System.out.println("\nWaiting for loan requests....");
		}catch(JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		}catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
	
	private void exit() {
		try {
			qConnect.close();
		}catch(JMSException jmse) {
			jmse.printStackTrace();
		}
		System.exit(1);
	}
	
	public static void main(String[] args) {
		String queuecf = null;
		String requestQ = null;
		
		queuecf = args[0];
		requestQ = args[1];
		
		QLender lender = new QLender(queuecf, requestQ);
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("QLender Application started..");
			reader.readLine();			
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}

}
