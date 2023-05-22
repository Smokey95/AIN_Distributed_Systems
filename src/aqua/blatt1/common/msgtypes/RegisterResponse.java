package aqua.blatt1.common.msgtypes;

import java.io.Serializable;

@SuppressWarnings("serial")
public final class RegisterResponse implements Serializable {
	private final String id;
	private final long lease_time;


	public RegisterResponse(String id, long lease_time) {
		this.id = id;
		this.lease_time = lease_time / 2;
	}

	public String getId() {
		return id;
	}
	
	public long getLeaseTime() {
		return lease_time;
	}

}
