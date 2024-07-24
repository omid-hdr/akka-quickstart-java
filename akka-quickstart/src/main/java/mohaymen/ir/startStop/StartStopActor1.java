package mohaymen.ir.startStop;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import mohaymen.ir.hello.HelloWorldMain;



class StartStopActor1 extends AbstractActor {

    public static void main(String[] args) throws InterruptedException {

        akka.actor.ActorSystem system = ActorSystem.create("testSystem");

        ActorRef first = system.actorOf(StartStopActor1.props(), "first");
        first.tell("stop", ActorRef.noSender());

        Thread.sleep(3000);

        system.terminate();
    }


    static Props props() {
        return Props.create(StartStopActor1.class, StartStopActor1::new);
    }

    @Override
    public void preStart() {
        System.out.println("first started");
        getContext().actorOf(StartStopActor2.props(), "second");
    }

    @Override
    public void postStop() {
        System.out.println("first stopped");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(
                        "stop",
                        s -> {
                            getContext().stop(getSelf());
                        })
                .build();
    }
}