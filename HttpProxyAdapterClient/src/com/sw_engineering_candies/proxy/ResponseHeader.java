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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

public class ResponseHeader {

	private String header = "";

	private String firstLine = "";

	private final Map<String, String> parameter = new HashMap<String, String>();

	public ResponseHeader(final String header) {
		
		this.header = header;
		
		final BufferedReader buff = new BufferedReader(new StringReader(this.header));
		String line;
		boolean isfirstLine = true;
		try {
			line = buff.readLine();
			while (line != null) {
				if (!line.trim().isEmpty()) {
					if (!isfirstLine) {
						final String key = line.substring(0, line.indexOf(':'));
						final String value = line.substring(line.indexOf(':') + 1, line.length());
						getParameter().put(key, value);
					}
				} else {
					break;
				}
				if (isfirstLine) {
					this.firstLine = line;
					isfirstLine = false;
				}
				line = buff.readLine();
			}
		} catch (final IOException e) {
		}
	}

	public String getFirstLine() {
		return this.firstLine;
	}

	public Map<String, String> getParameter() {
		return this.parameter;
	}

	public String getHeader() {
		return this.header;
	}

}
