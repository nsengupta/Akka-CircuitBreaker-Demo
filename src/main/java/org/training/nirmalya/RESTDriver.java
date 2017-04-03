package org.training.nirmalya;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.javalite.http.*;

import scala.concurrent.duration.Duration;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;

public class RESTDriver {

	public static void main(String[] args) {
		
		ActorSystem system = ActorSystem.create("CircuitBreakerDemo");
	    
	    final ActorRef externalService = system.actorOf(
	    		SoccerClubInfoGetterActor.props("http://api.football-data.org/v1/teams/"),"SocccerInfoActor");
	    final ActorRef requestor = system.actorOf(
	    		RequestorActor.props(externalService),"RequestorActor");
	    
	    final Inbox inbox = Inbox.create(system);
	    
	    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(5));
	    
	    try {
			System.out.println("Received from external service: " + (String)inbox.receive(Duration.create(10, TimeUnit.SECONDS)));
		} catch (TimeoutException e1) {
		
			e1.printStackTrace();
		}

	    system.terminate();
	}

}
