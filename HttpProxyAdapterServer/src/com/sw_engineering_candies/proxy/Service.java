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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Path("/server")
public class Service {

	@POST
	@Consumes("text/plain")
	public Response forwardRequestService(final String httpMessage) {
		try {

			// If the caller don't knows the token access is not allowed
			if (!httpMessage.contains(SecretToken.TOKEN_VALUE)) {
				return Response.status(Status.UNAUTHORIZED).build();
			}

			// Everything except GET & HEAD is not allowed
			final RequestHeader requestHeader = new RequestHeader(httpMessage);
			if (!"GET".equals(requestHeader.getType()) && !"HEAD".equals(requestHeader.getType())) {
				return Response.status(HttpURLConnection.HTTP_NOT_IMPLEMENTED).build();
			}

			final URL url = new URL(requestHeader.getUrl());
			final HttpURLConnection connectionHttp = (HttpURLConnection) url.openConnection();
			connectionHttp.setRequestMethod(requestHeader.getType());
			for (final String key : requestHeader.getParameter().keySet()) {
				if (isNotSecretToken(key)) {
					String value = requestHeader.getParameter().get(key);
					connectionHttp.addRequestProperty(key, value);
				}
			}

			// Send response back to client
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final DataInputStream dis = new DataInputStream(connectionHttp.getInputStream());

			// Add response header
			final StringBuilder sb = new StringBuilder();
			sb.append(requestHeader.getHttpVersion() + " " + connectionHttp.getResponseCode() + " "
					+ connectionHttp.getResponseMessage() + "\r\n");
			final Map<String, List<String>> map = connectionHttp.getHeaderFields();
			for (final Map.Entry<String, List<String>> entry : map.entrySet()) {
				final String key = entry.getKey();
				sb.append(key + " : " + entry.getValue().toString().replace("[", "").replace("]", "").replace(",", " ")
						+ "\r\n");
			}
			sb.append("\r\n");

			// Add response content
			baos.write(sb.toString().getBytes(), 0, sb.toString().getBytes().length);
			final byte[] data = new byte[(int) Short.MAX_VALUE];
			int index = dis.read(data, 0, (int) Short.MAX_VALUE);
			while (index != -1) {
				baos.write(data, 0, index);
				index = dis.read(data, 0, (int) Short.MAX_VALUE);
			}
			final byte[] result = baos.toByteArray();
			return Response.ok(result).build();

		} catch (final MalformedURLException e) {
			System.out.print("MalformedURLException " + e.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		} catch (final IOException e) {
			System.out.print("IOException " + e.getMessage());
			return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
		}
	}

	private boolean isNotSecretToken(final String key) {
		return !SecretToken.TOKEN_KEY.equals(key.trim());
	}
}