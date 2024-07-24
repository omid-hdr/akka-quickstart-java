package mohaymen.ir.hello;


import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;


public class HelloWorld extends AbstractBehavior<HelloWorld.Greet> {

    private HelloWorld(ActorContext<Greet> context) {
        super(context);
    }

    public static Behavior<Greet> create() {
        return Behaviors.setup(HelloWorld::new);
    }

    @Override
    public Receive<Greet> createReceive() {
        return newReceiveBuilder().onMessage(Greet.class, this::onGreet).build();
    }

    private Behavior<Greet> onGreet(Greet command) {
        getContext().getLog().info("Hello {}!", command.whom);
        command.replyTo.tell(new Greeted(command.whom, getContext().getSelf()));
        return this;
    }

    public record Greet(String whom, ActorRef<Greeted> replyTo) {
    }

    public record Greeted(String whom, ActorRef<Greet> from) {
    }
}

