package org.javacream.training.jms.transactional;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.javacream.training.util.jms.JmsUtil;

public class TransactionalDispatcher{

	private Session session;

	public TransactionalDispatcher() throws JMSException{
		Connection connection = JmsUtil.getConnectionFactory().createConnection();
		session = connection.createSession(false, Session.CLIENT_ACKNOWLEDGE);

		JmsUtil.setListener(session, JmsUtil.createQueue(session, TransactionalConstants.DESTINATION_AGGREGATOR), new DispatchingMessageListener());
		try {
			connection.start();
		} catch (JMSException e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) throws JMSException {
		new TransactionalDispatcher();
	}
	
	private class DispatchingMessageListener implements MessageListener {

		@Override
		public void onMessage(Message message) {
			try {
			System.out.println("received message " + message);
			if (message.getJMSRedelivered()){
				JmsUtil.send(session, JmsUtil.createQueue(session, TransactionalConstants.DESTINATION_CONSUMER), message);
				System.out.println("committing the redelivered message");
				session.commit();
			}else{
				JmsUtil.send(session, JmsUtil.createQueue(session, TransactionalConstants.DESTINATION_CONSUMER), message);
				System.out.println("rollback for original message");
				session.rollback();
				
			}
			} catch (JMSException e) {
				throw new RuntimeException(e);
			}
		}
	}

}
