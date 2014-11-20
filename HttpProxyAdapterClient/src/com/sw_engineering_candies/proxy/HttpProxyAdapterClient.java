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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.UnrecoverableKeyException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HttpProxyAdapterClient {

	private static final Logger LOGGER = Logger.getLogger(HttpProxyAdapterClient.class.getName());

	static {
		System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tc] %4$s: %5$s%n");		
		LOGGER.setLevel(Level.INFO);
	}

	/**
	 * Must be initialized in settings.ini file
	 */
	private static Integer localProxyPort = -1;

	/**
	 * This is the socket that listens at localhost to serve your browser
	 */
	private static ServerSocket httpServerSocket;

	/**
	 * Reads the property file and sets all parameter
	 */
	public static void readPropertyFile() {
		final java.util.Properties prop = new Properties();
		try {
			prop.load(new FileInputStream("settings.ini"));

			setLocalProxyPort(Integer.valueOf(prop.getProperty("local.http.proxyPort")));

			if (Boolean.valueOf(prop.getProperty("https.proxySet"))) {

				final String password = prop.getProperty("https.proxyUserPwd");
				System.setProperty("https.proxyUserPwd", password);

				final String user = prop.getProperty("https.proxyUserName");
				System.setProperty("https.proxyUserName", user);

				final String host = prop.getProperty("https.proxyHost");
				System.setProperty("https.proxyHost", host);

				final String port = prop.getProperty("https.proxyPort");
				System.setProperty("https.proxyPort", port);

				LOGGER.info("SWEC-PROXY-ADAPTER use proxy host=" + host + ":" + port + " user=" + user + " password="
						+ String.format("%" + password.length() + "s", "").replace(' ', '*'));

			} else {
				// You may edit settings.ini in the case you have to use a proxy
				LOGGER.info("SWEC-PROXY-ADAPTER doesn't use proxy ");
			}

		} catch (final FileNotFoundException e1) {
			LOGGER.info(e1.getMessage());
		} catch (final IOException e1) {
			LOGGER.info(e1.getMessage());
		}
	}

	public static void main(final String[] args) {

		readPropertyFile();

		if (getLocalProxyPort() != -1) {

			LOGGER.info("SWEC-PROXY-ADAPTER is ready at localhost:" + getLocalProxyPort());

			try {
				httpServerSocket = new ServerSocket(getLocalProxyPort());
				final boolean listening = true;
				while (listening) {
					final Socket httpSocket = httpServerSocket.accept();
					final SocketThread httpThread = new SocketThread(httpSocket);
					httpThread.start();
				}
			} catch (final IOException e) {
				LOGGER.log(Level.SEVERE, "could not listen port " + getLocalProxyPort());
			}
		}
	}

	public static Integer getLocalProxyPort() {
		return HttpProxyAdapterClient.localProxyPort;
	}

	public static void setLocalProxyPort(final Integer port) {
		HttpProxyAdapterClient.localProxyPort = port;
	}

}
