package org.training.nirmalya;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.training.nirmalya.InteractionProtocol.ClubDetailsFromXternalSource;
import org.training.nirmalya.InteractionProtocol.RetrievableClubIDMessage;
import org.training.nirmalya.InteractionProtocol.RetrievableClubIDMessageWithFinalDeliveryAddress;
import org.training.nirmalya.Driver.Tick;
import org.training.nirmalya.InteractionProtocol.TimedOutClubDetails;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Status;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import akka.pattern.Patterns;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

public class RequestorActor extends UntypedActor {

    private LoggingAdapter log = Logging.getLogger( getContext().system(), this );
    
    public static final Timeout ASK_TIMEOUT = Timeout.apply(1000, TimeUnit.MILLISECONDS );
    
	private final ActorRef circuitBreakerJeeves;

    public RequestorActor(ActorRef circuitBreakerJeeves) {
      this.circuitBreakerJeeves = circuitBreakerJeeves;
    }
    
    @Override
    public void onReceive( Object arg0 ) throws Exception {
    	
    	
      if ( arg0 instanceof RetrievableClubIDMessage) {
    	  
    	  int clubID = ((RetrievableClubIDMessage)arg0).clubID;
    	  
    	  log.info("Received request to retrieve info for club [{}] " +clubID);
    	  
    	  final ActorRef infoIsSoughtBy = getSender();

    	  Function<Object, ClubDetailsFromXternalSource> converter = (o) -> {
    		  
    		  ClubDetailsFromXternalSource c = (ClubDetailsFromXternalSource) o;
    		  return (c);
    	  };
    	  
    	  PatternsCS.pipe(
	    			  PatternsCS.ask(
					  circuitBreakerJeeves,
					  new InteractionProtocol.RetrievableClubIDMessageWithFinalDeliveryAddress(
							  clubID, getSender()
					  ),
					  ASK_TIMEOUT
				    )
				    .handle((messageOK, exceptionReported) -> {
					  
					  if (messageOK != null) {
						  return (converter.apply(messageOK));
					  }
					  else {
						  log.info("Call to external source seems to have timedOut!");
						  return (new InteractionProtocol.TimedOutClubDetails(infoIsSoughtBy));
					  }
					  
				  }),
				  getContext().system().dispatcher())
				  .to(getSelf()); 
    	  
      } else if ( arg0 instanceof ClubDetailsFromXternalSource) {
          log.info("Received [{}]" + arg0);
          ClubDetailsFromXternalSource m = (ClubDetailsFromXternalSource)arg0;
          ActorRef originalSender = m.originallyAskedBy;
          String details          = m.clubInfoAsJSON;
          originalSender.tell (details, getSelf());
          
      } else if ( arg0 instanceof TimedOutClubDetails ) {
        log.info("Received [{}]" + arg0);
        
        TimedOutClubDetails m = (TimedOutClubDetails) arg0;
        
        m.toBSentTo.tell("Service unresponsive, try again later", getSelf());
   
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
