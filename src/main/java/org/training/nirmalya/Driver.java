package org.training.nirmalya;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Inbox;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import scala.concurrent.duration.Duration;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class Driver {

  public static void main( String[] args ) throws Exception {
	  
	ArrayList<Integer> clubIDsToSearchFor = new ArrayList<Integer>();
	
	clubIDsToSearchFor.add(5);
	clubIDsToSearchFor.add(0);
	clubIDsToSearchFor.add(3);
	clubIDsToSearchFor.add(1);
	clubIDsToSearchFor.add(0);
	  
    ActorSystem system = ActorSystem.create("CircuitBreakerDemo");
    
    final ActorRef externalService = system.actorOf(
    		SoccerClubInfoGetterActor.props("http://api.football-data.org/v1/teams/"),"SocccerInfoActor");
    
    final ActorRef sysAdmin = system.actorOf(
    		SysAdminConsoleActor.props(),"SystemAdministratorActor");
    
    final ActorRef circuitBreaker = system.actorOf(
    		CallWastePreventorActor.props("http://api.football-data.org/v1/teams/",sysAdmin),"CircuitBreakerActor");
    
    final ActorRef requestor = system.actorOf(
    		RequestorActor.props(circuitBreaker),"RequestorActor");
    
    final Inbox inbox = Inbox.create(system);
    
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(5));
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(0));
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(0));
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(5));
    try {
		System.out.println("Clubinfo received: " + 
                           (String)inbox.receive(
                        		   Duration.create(5, TimeUnit.SECONDS)
                           ));
		System.out.println("Clubinfo received: " + 
                (String)inbox.receive(
             		   Duration.create(5, TimeUnit.SECONDS)
                ));
		System.out.println("Clubinfo received: " + 
                (String)inbox.receive(
             		   Duration.create(5, TimeUnit.SECONDS)
                ));
		System.out.println("Clubinfo received: " + 
                (String)inbox.receive(
             		   Duration.create(5, TimeUnit.SECONDS)
                ));
	} catch (TimeoutException e1) {
	
		e1.printStackTrace();
	}

    
    Thread.sleep( 10000 );

    system.terminate();
  }

  

  public static class Tick {

  }

}
