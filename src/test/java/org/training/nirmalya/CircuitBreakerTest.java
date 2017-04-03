package org.training.nirmalya;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import scala.concurrent.duration.FiniteDuration;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.Status;
import akka.testkit.JavaTestKit;
import akka.testkit.TestActorRef;

public class CircuitBreakerTest {
	
	static ActorSystem system;

	@Before
	public void setUp() throws Exception {
		system = ActorSystem.create("SmartPongActorTestSystem");
	}

	@After
	public void tearDown() throws Exception {
		JavaTestKit.shutdownActorSystem(system);
		system = null;
	}

	@Test
	public void adminActorHasCorrectInitialCBStateTest() {
		
		final JavaTestKit testDriverEnv = new JavaTestKit(system);
		
		final Props propsSA = Props.create(SysAdminConsoleActor.class);
		
		final TestActorRef<SysAdminConsoleActor> testActorForSA = 
				TestActorRef.create(system, propsSA, "sysadminTestActor");
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m1 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker is not in the picture as yet. Therefore, SysAdmin actor's 
		// default state information should indicate that the CircuitBreaker is closed.
		assertThat(m1.stateDesc, equalTo("CB:Closed"));
		
	}
	
	@Test
	public void sysadminCBStateRemainsSameForSuccessfulCircuitBreakerOperationTest() {
		
		final JavaTestKit testDriverEnv = new JavaTestKit(system);
		
		final Props propsSA = Props.create(SysAdminConsoleActor.class);
		
		final TestActorRef<SysAdminConsoleActor> testActorForSA = 
				TestActorRef.create(system, propsSA, "sysadminTestActor");
		
		final Props propsCB = Props.create(
				CallWastePreventorActor.class,
				"http://api.football-data.org/v1/teams/",
				testActorForSA.underlyingActor().self()
			  );
		
		final TestActorRef<CallWastePreventorActor> testActorForCB= 
				TestActorRef.create(system, propsCB, "CircuitBreakerActor");
		
		testActorForCB.tell(
				new InteractionProtocol.RetrievableClubIDMessage(5), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.ClubDetailsFromXternalSource m1 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(4, TimeUnit.SECONDS),
						InteractionProtocol.ClubDetailsFromXternalSource.class
					);
		
		// Ideally, we should convert the JSON into a POJO and compare the fields; we understand
		// that we are being sketchy and lazy here! :-)
		assertThat(m1.clubInfoAsJSON.isEmpty(), equalTo(false));
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m2 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker should have worked as expected. Therefore, SysAdmin actor's 
		// default state information should indicate that the CircuitBreaker is closed.
		assertThat(m2.stateDesc, equalTo("CB:Closed"));
		
	}
	
	@Test
	public void sysadminCBStateChangesForFailedCircuitBreakerOperationTest() {
		
		final JavaTestKit testDriverEnv = new JavaTestKit(system);
		
		final Props propsSA = Props.create(SysAdminConsoleActor.class);
		
		final TestActorRef<SysAdminConsoleActor> testActorForSA = 
				TestActorRef.create(system, propsSA, "sysadminTestActor");
		
		final Props propsCB = Props.create(
				CallWastePreventorActor.class,
				"http://api.football-data.org/v1/teams/",
				testActorForSA.underlyingActor().self()
			  );
		
		final TestActorRef<CallWastePreventorActor> testActorForCB= 
				TestActorRef.create(system, propsCB, "CircuitBreakerActor");
		
		// MAX_FAILURES value of the CircuitBreaker is set to 2. So, we are forcing a failure for 
		// 2 successive calls.
		IntStream.of(0,0).forEach((i) -> {    // 0 == invalid club ID
			testActorForCB.tell(
					new InteractionProtocol.RetrievableClubIDMessage(i), 
					testDriverEnv.getRef()
			);
			Object m1 = 
					testDriverEnv.expectMsgClass(
							new FiniteDuration(4, TimeUnit.SECONDS),
							akka.actor.Status.Failure.class
						);
		});
		
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m3 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker should have failed, because of 2 successive calls with wrong club ID. 
		// Therefore, SysAdmin actor's state information should indicate that the 
		// CircuitBreaker is Open.
		assertThat(m3.stateDesc, equalTo("CB:Open"));
		
	}
	
	@Test
	public void circuitBreakerOpensHalfAfterStipulatedTimePeriodTest() {
		
		final JavaTestKit testDriverEnv = new JavaTestKit(system);
		
		final Props propsSA = Props.create(SysAdminConsoleActor.class);
		
		final TestActorRef<SysAdminConsoleActor> testActorForSA = 
				TestActorRef.create(system, propsSA, "sysadminTestActor");
		
		final Props propsCB = Props.create(
				CallWastePreventorActor.class,
				"http://api.football-data.org/v1/teams/",
				testActorForSA.underlyingActor().self()
			  );
		
		final TestActorRef<CallWastePreventorActor> testActorForCB= 
				TestActorRef.create(system, propsCB, "CircuitBreakerActor");
		
		// MAX_FAILURES value of the CircuitBreaker is set to 2. So, we are forcing a failure for 
		// 2 successive calls.
		IntStream.of(0,0).forEach((i) -> {    // 0 == invalid club ID
			testActorForCB.tell(
					new InteractionProtocol.RetrievableClubIDMessage(i), 
					testDriverEnv.getRef()
			);
			Object m1 = 
					testDriverEnv.expectMsgClass(
							new FiniteDuration(4, TimeUnit.SECONDS),
							akka.actor.Status.Failure.class
						);
		});
		
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m3 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker should have failed, because of 2 successive calls with wrong club ID. 
		// Therefore, SysAdmin actor's state information should indicate that the 
		// CircuitBreaker is Open.
		assertThat(m3.stateDesc, equalTo("CB:Open"));
		
		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m4 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker's Reset Timeout is 2 seconds. We have waited (slept) for 2.5 seconds. 
		// Therefore, SysAdmin actor's state information should indicate that the 
		// CircuitBreaker is HalfOpen.
		assertThat(m4.stateDesc, equalTo("CB:HalfOpen"));
		
	}

	@Test
	public void circuitBreakerMovesToCloseFromHalfOpenAsExpectedTest() {
		
		final JavaTestKit testDriverEnv = new JavaTestKit(system);
		
		final Props propsSA = Props.create(SysAdminConsoleActor.class);
		
		final TestActorRef<SysAdminConsoleActor> testActorForSA = 
				TestActorRef.create(system, propsSA, "sysadminTestActor");
		
		final Props propsCB = Props.create(
				CallWastePreventorActor.class,
				"http://api.football-data.org/v1/teams/",
				testActorForSA.underlyingActor().self()
			  );
		
		final TestActorRef<CallWastePreventorActor> testActorForCB= 
				TestActorRef.create(system, propsCB, "CircuitBreakerActor");
		
		// MAX_FAILURES value of the CircuitBreaker is set to 2. So, we are forcing a failure for 
		// 2 successive calls.
		IntStream.of(0,0).forEach((i) -> {    // 0 == invalid club ID
			testActorForCB.tell(
					new InteractionProtocol.RetrievableClubIDMessage(i), 
					testDriverEnv.getRef()
			);
			Object m1 = 
					testDriverEnv.expectMsgClass(
							new FiniteDuration(4, TimeUnit.SECONDS),
							akka.actor.Status.Failure.class
						);
		});
		
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m3 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker should have failed, because of 2 successive calls with wrong club ID. 
		// Therefore, SysAdmin actor's state information should indicate that the 
		// CircuitBreaker is Open.
		assertThat(m3.stateDesc, equalTo("CB:Open"));
		
		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m4 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker's Reset Timeout is 2 seconds. We have waited (slept) for 2.5 seconds. 
		// Therefore, SysAdmin actor's state information should indicate that the 
		// CircuitBreaker is HalfOpen.
		assertThat(m4.stateDesc, equalTo("CB:HalfOpen"));
		
		testActorForCB.tell(
				new InteractionProtocol.RetrievableClubIDMessage(6), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.ClubDetailsFromXternalSource m1 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(4, TimeUnit.SECONDS),
						InteractionProtocol.ClubDetailsFromXternalSource.class
				);
		
		testActorForSA.tell(
				new InteractionProtocol.AdminQueryMessage(), 
				testDriverEnv.getRef()
		);
		
		InteractionProtocol.CBStateMessage m5 = 
				testDriverEnv.expectMsgClass(
						new FiniteDuration(1, TimeUnit.SECONDS),
						InteractionProtocol.CBStateMessage.class
					);
		
		// CircuitBreaker was HalfOpen; after a successful call, it should switch over to Closed.
		assertThat(m5.stateDesc, equalTo("CB:Closed"));		
		
	}

}
