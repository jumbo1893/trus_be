/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jumbo.trus.config.entrypoint;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.Assert;

import java.io.IOException;

/**
 * An {@link AuthenticationEntryPoint} that sends a generic {@link HttpStatus} as a
 * response. Useful for JavaScript clients which cannot use Basic authentication since the
 * browser intercepts the response.
 *
 * @author Rob Winch
 * @since 4.0
 */
public final class CustomHttpStatusEntryPoint implements AuthenticationEntryPoint {

	private final HttpStatus httpStatus;


	private final String message;

	/**
	 * Creates a new instance.
	 * @param httpStatus the HttpStatus to set
	 */
	public CustomHttpStatusEntryPoint(HttpStatus httpStatus, String message) {
		Assert.notNull(httpStatus, "httpStatus cannot be null");
		this.httpStatus = httpStatus;
		this.message = message;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
						 AuthenticationException authException) throws IOException {
		response.setStatus(this.httpStatus.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE); // Nastavíme Content-Type na application/json
		response.getWriter().write(message); // Napišeme JSON do těla odpovědi
	}

}
