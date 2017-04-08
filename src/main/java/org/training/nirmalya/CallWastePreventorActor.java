package org.training.nirmalya;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;

import akka.dispatch.OnFailure;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;

import akka.pattern.CircuitBreaker;
import akka.pattern.PatternsCS;
import akka.util.Timeout;

import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;


import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.javalite.http.*;
import org.training.nirmalya.InteractionProtocol.ClubDetailsFromXternalSource;
import org.training.nirmalya.InteractionProtocol.RetrievableClubIDMessageWithFinalDeliveryAddress;
import org.training.nirmalya.InteractionProtocol.AdminFYIMessage;
import org.training.nirmalya.InteractionProtocol.UnavailableClubDetails;

public class CallWastePreventorActor extends UntypedActor {

  private LoggingAdapter log = Logging.getLogger( getContext().system(), this );

  public static final int MAX_FAILURES = 2;
  public static final Timeout ASK_TIMEOUT = Timeout.apply(4000, TimeUnit.MILLISECONDS );
  public static final FiniteDuration CALL_TIMEOUT = Duration.create( 2000, TimeUnit.MILLISECONDS );
  public static final FiniteDuration RESET_TIMEOUT = Duration.create( 3, TimeUnit.SECONDS );

  private final CircuitBreaker circuitBreaker;
  private final ActorRef sysAdminService;

  private final String externalServiceEndPoint;
  

  @Override
  public void onReceive( Object arg0 ) throws Exception {
	  
    if ( arg0 instanceof RetrievableClubIDMessageWithFinalDeliveryAddress ) {
    	
    	
    	final RetrievableClubIDMessageWithFinalDeliveryAddress m = (RetrievableClubIDMessageWithFinalDeliveryAddress)arg0;
		int clubID = m.clubID;
		ActorRef originalSender = m.originalSender;
		log.info("Received request to retrieve information for club(" + clubID + ")");
		
		String clubInfo = this.externalServiceEndPoint + clubID;
    	
    	Callable <CompletionStage<ClubDetailsFromXternalSource>> workingCallable = 
    		new Callable<CompletionStage<ClubDetailsFromXternalSource>>() {
				@Override
		        public CompletionStage<ClubDetailsFromXternalSource> call() throws Exception {
					 return (
								CompletableFuture.supplyAsync(new Supplier<ClubDetailsFromXternalSource>() {
									@Override
								    public ClubDetailsFromXternalSource get() {
										
								        String s = Http.get(clubInfo).text();
								        return (new ClubDetailsFromXternalSource(s,originalSender));
								        
								    } // end of get()
								} // end of supplier constructor
						   )  // end of supplyAsync() function
					  );   // end of return
				 } // end of call()
				 
		 };
		 
		 
		Callable<CompletionStage<UnavailableClubDetails>> nonWorkingCallable = 
			new Callable<CompletionStage<UnavailableClubDetails>>() {
				@Override
				public CompletionStage<UnavailableClubDetails> call() throws Exception {
					return (CompletableFuture
							.supplyAsync(
								new Supplier<UnavailableClubDetails>() {
										@Override
										public UnavailableClubDetails get() {
			
											try {
												Thread.sleep(5000);
											} catch (InterruptedException e) {
												e.printStackTrace();
											}
											return (new UnavailableClubDetails("External Source Not Responding"));
			
										} // end of get()
								} // end of supplier
						    ) // end of supplyAsync
					); // end of return
				} // end of call()

		};
		
		if (clubID == 0) { // A forced 'fail' timeout situation, for demonstration
			
			PatternsCS
			.pipe(
					circuitBreaker.callWithCircuitBreakerCS(nonWorkingCallable),
					getContext().system().dispatcher()
				 )
			.to(getSender());	
		}
		else {
			
			PatternsCS
			.pipe(
					circuitBreaker.callWithCircuitBreakerCS(workingCallable),
					getContext().system().dispatcher()
				 )
			.to(getSender());	
		}
    }
    else {
    	  
    }
    	  
  }
 

  public void onOpen() {
    this.sysAdminService.tell(
    		new AdminFYIMessage("CB:Open"),
    		getSelf() );
  }

  public void onClose() {
	  this.sysAdminService.tell(
			new AdminFYIMessage("CB:Closed" ),
			getSelf());
  }

  public void onHalfOpen() {
	  this.sysAdminService.tell(
			new AdminFYIMessage("CB:HalfOpen"),
			getSelf() );
  }
  
  private OnFailure futureFailureHandler(){
	    return new OnFailure() {
	        @Override
	        public void onFailure(Throwable failure) throws Throwable {
	            log.debug("Failed: " + failure.getMessage());
	        }
	    };
	}
    
  public static Props props(final String   externalServiceEndpoint,
		                    final ActorRef sysAdminActor
		                   ) {
	    return Props.create(new Creator<CallWastePreventorActor>() {
	      private static final long serialVersionUID = 1L;
	 
	      public CallWastePreventorActor create() throws Exception {
	        return new CallWastePreventorActor(externalServiceEndpoint,sysAdminActor);
	      }
	    });
	}
  
  public CallWastePreventorActor(
		    final String   externalServiceEndpoint,
		    final ActorRef sysAdminService
		  ) {
	  
	this.sysAdminService         =   sysAdminService;
	this.externalServiceEndPoint =   externalServiceEndpoint;
	  
    this.circuitBreaker = new CircuitBreaker( getContext().dispatcher(),
                                         getContext().system().scheduler(),
                                         MAX_FAILURES,
                                         CALL_TIMEOUT,
                                         RESET_TIMEOUT )
        .onOpen( new Runnable() {
          public void run() {
            onOpen();
          }
        } )
        .onClose( new Runnable() {
          @Override
          public void run() {
            onClose();
          }
        } )
        .onHalfOpen( new Runnable() {
          @Override
          public void run() {
            onHalfOpen();
          }
        } );

  }

}
