package actors;


import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Random;

import messages.Messages.BGWNPoint;
import messages.Messages.BiprimalityTestResult;
import messages.Messages.CandidateN;
import messages.Messages.Participants;
import messages.Messages.QiTestForRound;
import messages.Messages.Thetai;
import messages.Messages.VerificationKey;
import paillierp.key.PaillierPrivateThresholdKey;
import protocol.BGWParameters.BGWPublicParameters;
import protocol.KeysDerivationParameters.KeysDerivationPublicParameters;
import protocol.ProtocolParameters;
import actordata.ProtocolData;
import actors.ProtocolActor.States;
import akka.actor.AbstractLoggingFSM;
import akka.actor.ActorRef;
import akka.actor.Props;

public class ProtocolActor extends AbstractLoggingFSM<States, ProtocolData> {
	
	public static enum States {INITIALIZATION,BGW,BIPRIMAL_TEST, KEYS_DERIVATION};
	
	private  ActorRef bgwActor;
	private  ActorRef biprimalTestActor;
	private  ActorRef keysDerivationActor;
	
	public ProtocolActor(ProtocolParameters protocolParams) {
		bgwActor = context().actorOf(Props.create(BGWProtocolActor.class, protocolParams,self()), "BGWActor");
		biprimalTestActor = context().actorOf(Props.create(BiprimalityTestActor.class, self()), "BiprimalityTestActor");
		keysDerivationActor = context().actorOf(Props.create(KeysDerivationActor.class, self(), protocolParams), "KeysDerivationActor");
		
		startWith(States.INITIALIZATION, ProtocolData.init());
		
		when(States.INITIALIZATION, matchEvent(Participants.class,
				(participants,data) -> {
					//bgwActor.tell(participants, self());
					biprimalTestActor.tell(participants, self());
					keysDerivationActor.tell(participants, self());
					return goTo(States.BGW).using(data.withParticipants(participants.getParticipants()));
				}));
		
		when(States.BGW, matchEvent(CandidateN.class, 
				(candidateN, data) -> {
					if(data.getParticipants().get(self())==1)
						System.out.println("TRY: N="+candidateN.N);
					return goTo(States.BIPRIMAL_TEST).using(data.withNewN(candidateN.N, candidateN.bgwPrivateParameters));
				}));

		
		when(States.BIPRIMAL_TEST, matchEvent(BiprimalityTestResult.class, 
				(result, data) -> {
					
					if(result.passes) {
						System.out.println("FOUND N="+result.N);
						return goTo(States.KEYS_DERIVATION).using(data.withNewN(result.N, result.bgwPrivateParameters));
					} else {
						if(data.getParticipants().get(self())==1)
							System.out.println("DID NOT PASS");
						//bgwActor.tell(new Participants(data.getParticipants()), self());
						return goTo(States.BGW).using(data);
					}
		}));
		
		onTransition(matchState(States.BIPRIMAL_TEST, States.KEYS_DERIVATION, () -> {
			ProtocolData data = nextStateData();
			keysDerivationActor.tell(new BiprimalityTestResult(data.N, data.bgwPrivateParameters, true),  self());
		}));
		
		when(States.KEYS_DERIVATION, matchEvent(PaillierPrivateThresholdKey.class, (key, data) -> {
			DataOutputStream out = new DataOutputStream(new FileOutputStream("keys/"+self().path().name()+".privkey"));
			out.write(key.toByteArray());
			out.close();
			
			System.out.println(key.getID()+" DONE !!!");
			return stop();
		}));
		
		
		
		
		whenUnhandled(matchAnyEvent((evt,data) -> {
			Random rand = new Random();
			if(evt instanceof BGWPublicParameters || evt instanceof BGWNPoint) {
				Thread.sleep(rand.nextInt(3));
				bgwActor.tell(evt, sender());
			}
			else if(evt instanceof QiTestForRound) {
				Thread.sleep(rand.nextInt(3));
				biprimalTestActor.tell(evt, sender());
			}
			else if(evt instanceof KeysDerivationPublicParameters || evt instanceof Thetai || evt instanceof VerificationKey) {
				Thread.sleep(rand.nextInt(3));
				keysDerivationActor.tell(evt, sender());
			}
			
			return stay();
		}));
		
		onTransition((from,to) -> {
			//System.out.println(String.format("%s transition %s -> %s", ActorUtils.nameOf(self()), from, to));
			
			if(to == States.BGW) {
				bgwActor.tell(new Participants(nextStateData().getParticipants()), self());
			}
			
			if(to == States.BIPRIMAL_TEST) {
				biprimalTestActor.tell(new CandidateN(nextStateData().N, nextStateData().bgwPrivateParameters), sender());
			}
			
		});
	}
}
