package org.training.nirmalya;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;

import org.training.nirmalya.InteractionProtocol.AdminFYIMessage;
import org.training.nirmalya.InteractionProtocol.AdminQueryMessage;


public class SysAdminConsoleActor extends UntypedActor {

  private LoggingAdapter log = Logging.getLogger( getContext().system(), this );
  
  private String currentCircuitBreakerState = "CB:Closed";

  public SysAdminConsoleActor() { }
  
  @Override
  public void onReceive(Object arg0) throws Exception {
	  
    if ( arg0 instanceof AdminFYIMessage ) {
    	
    	AdminFYIMessage m = (AdminFYIMessage)arg0;
    	this.currentCircuitBreakerState = m.whatHappened;
    	log.info(" Administrator is notified of external service:[" + m.whatHappened + "]");

    }
    else if (arg0 instanceof AdminQueryMessage) {
    	getSender().tell(new InteractionProtocol.CBStateMessage(currentCircuitBreakerState),getSelf());
    }
    else {
    	log.info("Unknown Message [" + arg0 + "]");
		unhandled(arg0);	  
    }
    	  
  }
    
  public static Props props() {
	    return Props.create(new Creator<SysAdminConsoleActor>() {
	      private static final long serialVersionUID = 1L;
	 
	      public SysAdminConsoleActor create() throws Exception {
	        return new SysAdminConsoleActor();
	      }
	    });
	} 
}
