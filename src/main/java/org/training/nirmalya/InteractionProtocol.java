package org.training.nirmalya;

import java.io.Serializable;

public class InteractionProtocol {
	
	public static class NoCircuitBreakerYetMessage {

		private static final long serialVersionUID = 1L;
	}

	public static class CBStateMessage implements Serializable {
		
		private static final long serialVersionUID = 1L;
		
		public final String stateDesc;

		public CBStateMessage(String stateDesc) {
			super();
			this.stateDesc = stateDesc;
		}	
	}

	public static class AdminQueryMessage implements Serializable { }
	
	

	public static class RetrievableClubIDMessage implements Serializable { 
	
		private static final long serialVersionUID = 1L;
		
		public final int clubID;
		
		public RetrievableClubIDMessage(int clubID) {
			this.clubID = clubID;
		}

		public String toString() {
			return ("RetrievableClubIDMessage");
		}
	}
	
	public static class AdminFYIMessage implements Serializable { 
		
		private static final long serialVersionUID = 1L;
		
		public final String whatHappened;
		
		public AdminFYIMessage(String whatHappened) {
			this.whatHappened = whatHappened;
		}

		public String toString() {
			return ("AdminFYIMessage(" + whatHappened + ")");
		}
	}
	
    public static class ClubDetailsFromXternalSource implements Serializable { 
		
		private static final long serialVersionUID = 1L;
		
		public final String clubInfoAsJSON;
		
		public ClubDetailsFromXternalSource(String clubInfoAsJSON) {
			this.clubInfoAsJSON = clubInfoAsJSON;
		}

		public String toString() {
			return ("ClubDetailsFromXternalSource(" + clubInfoAsJSON + ")");
		}
	}
    
    public static class UnavailableClubDetails implements Serializable { 
		
		private static final long serialVersionUID = 1L;
		
		public final String reasons;
		
		public UnavailableClubDetails(String reasons) {
			this.reasons = reasons;
		}

		public String toString() {
			return ("UnavailableClubDetails(" + reasons + ")");
		}
	}
	
}
