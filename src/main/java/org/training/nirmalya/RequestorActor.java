package org.training.nirmalya;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.training.nirmalya.InteractionProtocol.RetrievableClubIDMessage;
import org.training.nirmalya.Driver.Tick;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.util.Timeout;

public class RequestorActor extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger( getContext().system(), this );
    
    public static final Timeout ASK_TIMEOUT = Timeout.apply(6000, TimeUnit.MILLISECONDS );
    
	private final ActorRef circuitBreakerJeeves;

    public RequestorActor(ActorRef circuitBreakerJeeves) {
      this.circuitBreakerJeeves = circuitBreakerJeeves;
    }
    
    @Override
    public void onReceive( Object arg0 ) throws Exception {
    	
      if ( arg0 instanceof RetrievableClubIDMessage) {
    	  log.debug("Received request to retrieve info: " + ((RetrievableClubIDMessage)arg0).toString());  
    	  
		  Patterns
    	  .pipe(
      		    (
	    		  Patterns.ask(circuitBreakerJeeves,arg0,ASK_TIMEOUT)
      		    ), 
      		    getContext().system().dispatcher()
      		   )
      	  .to(getSelf());
    	 
    	  
      } else if ( arg0 instanceof Status.Failure ) {
        log.info( "Received response " + arg0);
      }
      else {
    	  log.info("Received unknown message of type [{}]",arg0.getClass());
    	  unhandled(arg0);
      }
    }
    
    public static Props props(ActorRef circuitBreakerJeeves) {
	    return Props.create(new Creator<RequestorActor>() {
	      public RequestorActor create() throws Exception {
	        return new RequestorActor(circuitBreakerJeeves);
	      }
	    });
	} 
  }
