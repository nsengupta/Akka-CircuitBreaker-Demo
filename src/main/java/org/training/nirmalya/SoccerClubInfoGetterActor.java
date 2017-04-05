package org.training.nirmalya;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.javalite.http.Http;
import org.training.nirmalya.InteractionProtocol.RetrievableClubIDMessage;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import static akka.pattern.PatternsCS.pipe;;

public class SoccerClubInfoGetterActor extends UntypedActor {
	
  private LoggingAdapter log = Logging.getLogger( getContext().system(), this );
	
  private final String targetRESTEndPoint;

  public void onReceive(Object arg0) throws Throwable {
		
	if (arg0 instanceof RetrievableClubIDMessage) {
		
		final RetrievableClubIDMessage m = (RetrievableClubIDMessage)arg0;
		int clubID = m.clubID;
		log.debug("Received request to retrieve information for club(" + clubID + ")");
		if (clubID != 0) {
			String clubInfoAskedFor = this.targetRESTEndPoint + clubID;
			
		pipe(
				CompletableFuture.supplyAsync(new Supplier<String>() {
					@Override
					public String get() {
						String s = Http.get(clubInfoAskedFor).text();
						return (s);
					}
				}),
				getContext().system().dispatcher()
			).to(getSender());
				
		}
		else { // Emulating a failed call to the external service
				Thread.sleep(2000);
				getSender().tell("Site is unresponsive",getSelf());
		}
	}
	
	else {
		log.info("Unknown Message [" + arg0 + "]");
		unhandled(arg0);
	}
  }
  
	public static Props props(final String targetRESTEndPoint) {
	    return Props.create(new Creator<SoccerClubInfoGetterActor>() {
	      private static final long serialVersionUID = 1L;
	 
	      public SoccerClubInfoGetterActor create() throws Exception {
	        return new SoccerClubInfoGetterActor(targetRESTEndPoint);
	      }
	    });
	} 
	
	public SoccerClubInfoGetterActor(final String targetRESTEndPoint) {
		this.targetRESTEndPoint = targetRESTEndPoint;
	}

}
