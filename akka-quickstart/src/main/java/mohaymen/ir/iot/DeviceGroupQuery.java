package mohaymen.ir.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Cancellable;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import scala.concurrent.duration.FiniteDuration;

import java.util.Map;

public class DeviceGroupQuery extends AbstractActor {
    public static final class CollectionTimeout {}

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    final Map<ActorRef, String> actorToDeviceId;
    final long requestId;
    final ActorRef requester;

    Cancellable queryTimeoutTimer;

    public DeviceGroupQuery(
            Map<ActorRef, String> actorToDeviceId,
            long requestId,
            ActorRef requester,
            FiniteDuration timeout) {
        this.actorToDeviceId = actorToDeviceId;
        this.requestId = requestId;
        this.requester = requester;

        queryTimeoutTimer =
                getContext()
                        .getSystem()
                        .scheduler()
                        .scheduleOnce(
                                timeout,
                                getSelf(),
                                new CollectionTimeout(),
                                getContext().getDispatcher(),
                                getSelf());
    }

    public static Props props(
            Map<ActorRef, String> actorToDeviceId,
            long requestId,
            ActorRef requester,
            FiniteDuration timeout) {
        return Props.create(
                DeviceGroupQuery.class,
                () -> new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout));
    }

    @Override
    public void preStart() {
        for (ActorRef deviceActor : actorToDeviceId.keySet()) {
            getContext().watch(deviceActor);
            deviceActor.tell(new Device.ReadTemperature(0L), getSelf());
        }
    }

    @Override
    public void postStop() {
        queryTimeoutTimer.cancel();
    }

}