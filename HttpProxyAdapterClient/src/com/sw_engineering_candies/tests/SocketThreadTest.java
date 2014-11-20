package com.sw_engineering_candies.tests;

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

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;

import com.sw_engineering_candies.proxy.HttpProxyAdapterClient;

public class SocketThreadTest {

	private static final String HTTP_PROXY_ADAPTER_SERVER_TESTS = "http://http-proxy-adapter-server.appspot.com/resources/tests";

	@BeforeClass
	public static void prepare() {
		HttpProxyAdapterClient.readPropertyFile();
	}

	@Test
	public void testExcuteHEAD() {
		// ARRANGE
		final String expected = "";

		// ACT
		final String result = excuteCallHttp(HTTP_PROXY_ADAPTER_SERVER_TESTS + "/head-test", "HEAD");

		// ASSERT
		assertEquals(expected, result);
	}

	@Test
	public void testExcuteGET() {
		// ARRANGE
		final String expected = "Hello from testGETService!";

		// ACT
		final String result = excuteCallHttp(HTTP_PROXY_ADAPTER_SERVER_TESTS + "/get-test", "GET");

		// ASSERT
		assertEquals(expected, result);
	}

	public static String excuteCallHttp(final String targetURL, final String method) {
		System.setProperty("http.proxyHost", "localhost");
		System.setProperty("http.proxyPort", HttpProxyAdapterClient.getLocalProxyPort().toString());

		URL url;
		HttpURLConnection connection = null;
		try {
			// Create connection
			url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod(method);

			// Get Response
			final InputStream is = connection.getInputStream();
			final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			String line;
			final StringBuffer response = new StringBuffer();
			while ((line = rd.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			rd.close();
			return response.toString().trim();

		} catch (final Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

}
