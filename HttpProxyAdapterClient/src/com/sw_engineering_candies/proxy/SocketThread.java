package com.sw_engineering_candies.proxy;

/*
 * Copyright (C) 2014, Markus Sprunck <sprunck.markus@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * - The name of its contributor may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import static com.google.common.base.Preconditions.*;

public class SocketThread extends Thread {

	private static final Logger LOGGER = Logger.getLogger(SocketThread.class.getName());

	// TODO: Change URL to your Google Appengine application
	private static final String REMOTE_PROXY_SERVER_HTTPS_URL = "https://http-proxy-adapter-server.appspot.com/resources/server";

	/**
	 * Ensures that messages from concurrent threads are logged in a block
	 */
	private final StringBuilder message = new StringBuilder();

	/**
	 * Connection to the local web client
	 */
	private final Socket socket;

	/**
	 * Helper to parse the HTTP request of the local web client
	 */
	private RequestHeader requestHeader;

	/**
	 * Connection to remote server
	 */
	private HttpsURLConnection remoteConnection;

	public SocketThread(final Socket socket) {
		checkNotNull(socket);
		this.socket = socket;
	}

	@Override
	public void run() {
		try {
			readRequestFormLocalClient();
			forwardLocalRequestToRemoteServer();
			sendResponseToLocalClient();
			loggingOfResult();
		} catch (final IOException | IllegalStateException e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		} finally {
			closeSocket();
		}
	}

	private void readRequestFormLocalClient() throws IOException {
		final DataInputStream socketInput = new DataInputStream(this.socket.getInputStream());
		final String httpMessage = RequestHeader.readHttpHeader(socketInput);
		requestHeader = new RequestHeader(httpMessage + SecretToken.TOKEN);

		checkState(requestHeader.getHttpVersion().startsWith("HTTP/"), "no valid request header");
		checkState("GET".equals(requestHeader.getType()) || "HEAD".equals(requestHeader.getType()),
				"not allowed type -> " + requestHeader.getFirstLine());
	}

	private void forwardLocalRequestToRemoteServer() throws MalformedURLException, IOException, ProtocolException {
		final URL url = new URL(REMOTE_PROXY_SERVER_HTTPS_URL);
		remoteConnection = (HttpsURLConnection) url.openConnection();
		checkNotNull(remoteConnection, "no valid remote connection");

		remoteConnection.setDoOutput(true);
		remoteConnection.setRequestMethod("POST");
		remoteConnection.setRequestProperty("Content-Type", "text/plain");
		final OutputStream os = remoteConnection.getOutputStream();
		os.write(requestHeader.getInput().getBytes());
		os.flush();
	}

	private void sendResponseToLocalClient() throws IOException {
		final DataOutputStream socketOutput = new DataOutputStream(this.socket.getOutputStream());
		if (remoteConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
			final DataInputStream remoteInput = new DataInputStream(remoteConnection.getInputStream());
			final byte[] data = new byte[(int) Short.MAX_VALUE];
			int index = remoteInput.read(data, 0, (int) Short.MAX_VALUE);
			while (index != -1) {
				socketOutput.write(data, 0, index);
				index = remoteInput.read(data, 0, (int) Short.MAX_VALUE);
			}
			socketOutput.flush();
		}
	}

	private void loggingOfResult() {
		this.message.append(requestHeader.getType()).append(" ");
		if (null != remoteConnection) {
			try {
				this.message.append(remoteConnection.getResponseCode());
				this.message.append(" (");
				this.message.append(remoteConnection.getResponseMessage());
				this.message.append(") -> ");
			} catch (IOException e) {
			}
		}
		this.message.append(requestHeader.getUrl());
		LOGGER.info(this.message.toString());
	}

	private void closeSocket() {
		try {
			this.socket.close();
		} catch (IOException e) {
			LOGGER.log(Level.WARNING, e.getMessage());
		}
	}

}
