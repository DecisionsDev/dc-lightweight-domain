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
