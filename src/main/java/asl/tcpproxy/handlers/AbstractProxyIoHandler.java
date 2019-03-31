/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package asl.tcpproxy.handlers;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of {@link org.apache.mina.core.service.IoHandler} classes which
 * handle proxied connections.
 *
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public abstract class AbstractProxyIoHandler extends IoHandlerAdapter {
	//private static final Charset CHARSET = Charset.forName("iso8859-1");
	public static final String OTHER_IO_SESSION = AbstractProxyIoHandler.class.getName() + ".OtherIoSession";

	private final static Logger log = LoggerFactory.getLogger(AbstractProxyIoHandler.class);

	@Override
	public void sessionCreated(IoSession session) throws Exception {
		session.suspendRead();
		session.suspendWrite();
	}

	@Override
	public void sessionClosed(IoSession session) throws Exception {
		if (log.isWarnEnabled()) {
			log.warn("Closing session:" + session.getAttribute(OTHER_IO_SESSION));
		}
		if (session.getAttribute(OTHER_IO_SESSION) != null) {
			IoSession sess = (IoSession) session.getAttribute(OTHER_IO_SESSION);
			sess.setAttribute(OTHER_IO_SESSION, null);
			sess.closeOnFlush();
			session.setAttribute(OTHER_IO_SESSION, null);
		}
	}

	@Override
	public void messageReceived(IoSession session, Object message) throws Exception {
		IoBuffer rb = (IoBuffer) message;
		IoBuffer wb = IoBuffer.allocate(rb.remaining());
		rb.mark();
		wb.put(rb);
		wb.flip();
		((IoSession) session.getAttribute(OTHER_IO_SESSION)).write(wb);
		rb.reset();

//		if (log.isDebugEnabled()) {
//			log.debug(rb.getString(CHARSET.newDecoder()));
//		}
	}

//	@Override
//	public void messageSent(IoSession session, Object message) throws Exception {
//		super.messageSent(session, message);
//
//		if (log.isDebugEnabled()) {
//			IoBuffer rb = (IoBuffer) message;
//			log.debug("Message sent for " + session.getAttribute(OTHER_IO_SESSION).toString());
//			log.debug(rb.getString(CHARSET.newDecoder()));
//		}
//	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {

		if (log.isErrorEnabled()) {
			log.error("Exception Caught for " + session.getAttribute(OTHER_IO_SESSION).toString());
			log.error(cause.toString());
		}

		super.exceptionCaught(session, cause);

	}

//	@Override
//	public void inputClosed(IoSession session) throws Exception {
//		if (log.isDebugEnabled()) {
//			log.debug(String.format("Input closed for session: %s", session.toString()));
//		}
//		super.inputClosed(session);
//	}
}