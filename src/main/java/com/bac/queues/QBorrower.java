package com.bac.queues;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import javax.jms.JMSException;
import javax.jms.MapMessage;
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

public class QBorrower {
	
	private QueueConnection qConnect = null;
	private QueueSession qSession = null;
	private Queue responseQ = null;
	private Queue requestQ = null;
	
	public QBorrower(String queuecf, String requestQueue, String responseQueue) {
		try {
			Context ctx = new InitialContext();
			QueueConnectionFactory qFactory = (QueueConnectionFactory)ctx.lookup(queuecf);
			qConnect = qFactory.createQueueConnection();
			
			qSession = qConnect.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
			requestQ = (Queue)ctx.lookup(requestQueue);
			responseQ = (Queue)ctx.lookup(responseQueue);
			
			qConnect.start();
		}catch(JMSException jmse) {
			jmse.printStackTrace();
			System.exit(1);
		}catch(NamingException jne) {
			jne.printStackTrace();
		}		
	}
	
	private void sendLoanRequest(double salary, double loanAmt) {
		try {
			MapMessage msg = qSession.createMapMessage();
			msg.setDouble("Salary", salary);
			msg.setDouble("LoanAmount", loanAmt);
			msg.setJMSReplyTo(responseQ);
			
			QueueSender qSender = qSession.createSender(requestQ);
			qSender.send(msg);
			
			String filter = "JMSCorrelationID = '"+ msg.getJMSMessageID() +"'";
			QueueReceiver qReceiver = qSession.createReceiver(responseQ, filter);
			TextMessage tmsg = (TextMessage)qReceiver.receive(30000);
			
			if(tmsg == null) {
				System.out.println("QLender not responding");
			}else {
				System.out.println("Loan request was "+ tmsg.getText());
			}
		}catch(JMSException jmse) {
			jmse.printStackTrace();
			System.exit(0);
		}
	}
	
	private void exit() {
		try {
			qConnect.close();
		}catch(JMSException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public static void main(String[] args) {
		
		String queuecf = null;
		String requestQ = null;
		String responseQ = null;
		
		if(args.length == 3) {
		queuecf = args[0];
		requestQ = args[1];
		responseQ = args[2];
		}else {
			System.exit(1);
		}
		
		QBorrower borrower = new QBorrower(queuecf, requestQ, responseQ);
		
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			
			while(true) {
				String loanRequest = reader.readLine();
				StringTokenizer tokenizer = new StringTokenizer(loanRequest, ",");
				double salary = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				double loanAmt = Double.valueOf(tokenizer.nextToken().trim()).doubleValue();
				borrower.sendLoanRequest(salary, loanAmt);
			}
		}catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
			

}
