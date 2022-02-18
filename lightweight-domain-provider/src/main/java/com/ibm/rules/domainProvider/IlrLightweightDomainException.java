/*
 *
 *   Copyright IBM Corp. 2022
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package com.ibm.rules.domainProvider;

import ilog.rules.teamserver.model.IlrApplicationException;

@SuppressWarnings("serial")
public class IlrLightweightDomainException extends IlrApplicationException {

	private String reason;
	private String parameter;
	private Exception causeException;

	public IlrLightweightDomainException(String reason, String parameter) {
		this.reason = reason;
		this.parameter = parameter;
	}

	public IlrLightweightDomainException(Exception causeException, String parameter) {
		this.parameter = parameter;
		this.causeException = causeException;
	}

	public String getReason() {
		return reason;
	}

	public String getParameter() {
		return parameter;
	}

	public Exception getCauseException() {
		return causeException;
	}
}
