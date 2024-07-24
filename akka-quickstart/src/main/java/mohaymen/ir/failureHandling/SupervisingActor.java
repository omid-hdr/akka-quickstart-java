package mohaymen.ir.failureHandling;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;


class SupervisingActor extends AbstractActor {

    public static void main(String[] args) throws InterruptedException {


        akka.actor.ActorSystem system = ActorSystem.create("testSystem");

        ActorRef supervisingActor = system.actorOf(SupervisingActor.props(), "supervising-actor");
        supervisingActor.tell("failChild", ActorRef.noSender());

        Thread.sleep(3000);

        system.terminate();
    }

    static Props props() {
        return Props.create(SupervisingActor.class, SupervisingActor::new);
    }

    ActorRef child = getContext().actorOf(SupervisedActor.props(), "supervised-actor");

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "failChild",
                        f -> {
                            child.tell("fail", getSelf());
                        })
                .build();
    }
}
