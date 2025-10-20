package net.gitsaibot.af.data;


public class AfDataUpdateException extends Exception {

	public enum Reason {
		UNSPECIFIED,
		UNKNOWN,
		PARSE_ERROR,
		RATE_LIMITED, UNSUPPORTED_LOCATION,
	}

	public Reason reason;
	
	public AfDataUpdateException()
	{
		super();
		this.reason = Reason.UNSPECIFIED;
	}
	
	public AfDataUpdateException(String message)
	{
		super(message);
		this.reason = Reason.UNSPECIFIED;
	}

	public AfDataUpdateException(String message, Reason reason)
	{
		super(message);
		this.reason = reason;
	}
}
