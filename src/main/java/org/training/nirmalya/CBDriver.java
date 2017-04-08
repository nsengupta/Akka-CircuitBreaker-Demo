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


public class CBDriver {

  public static void main( String[] args ) throws Exception {
	  
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
    
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(0));
    try {
		System.out.println("** Client asked for clubinfo(0), received [ " + 
                           (String)inbox.receive(
                        		   Duration.create(5, TimeUnit.SECONDS)
                           ) + "]");
    }catch (TimeoutException e1) {
	
		e1.printStackTrace();
	};
	
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(0));
    try {
		System.out.println("** Client asked for clubinfo(0), received [ " + 
                           (String)inbox.receive(
                        		   Duration.create(5, TimeUnit.SECONDS)
                           ) + "]");
    }catch (TimeoutException e1) {
	
		e1.printStackTrace();
	};
    
    Thread.sleep(2000);
    
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(3));
    try {
		System.out.println("** Client asked for clubinfo(3), received [ " + 
                           (String)inbox.receive(
                        		   Duration.create(5, TimeUnit.SECONDS)
                           ) + "]");
    }catch (TimeoutException e1) {
	
		e1.printStackTrace();
	};
	
	Thread.sleep(2500);
    
    inbox.send(requestor,new InteractionProtocol.RetrievableClubIDMessage(7));
    try {
		System.out.println("** Client asked for clubinfo(7), received [ " + 
                           (String)inbox.receive(
                        		   Duration.create(5, TimeUnit.SECONDS)
                           ) + "]");
    }catch (TimeoutException e1) {
	
		e1.printStackTrace();
	};
    
    Thread.sleep( 10000 ); // Hold the ActorSystem till all interactions are complete

    system.terminate();
  }
}
