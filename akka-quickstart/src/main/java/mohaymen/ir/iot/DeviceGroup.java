package mohaymen.ir.iot;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.Terminated;
import akka.event.Logging;
import akka.event.LoggingAdapter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class DeviceGroup extends AbstractActor {
    final String groupId;
    final Map<String, ActorRef> deviceIdToActor = new HashMap<>();
    final Map<ActorRef, String> actorToDeviceId = new HashMap<>();
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public DeviceGroup(String groupId) {
        this.groupId = groupId;
    }

    public static Props props(String groupId) {
        return Props.create(DeviceGroup.class, () -> new DeviceGroup(groupId));
    }

    @Override
    public void preStart() {
        log.info("DeviceGroup {} started", groupId);
    }

    @Override
    public void postStop() {
        log.info("DeviceGroup {} stopped", groupId);
    }

    private void onTrackDevice(DeviceManager.RequestTrackDevice trackMsg) {
        if (this.groupId.equals(trackMsg.groupId)) {
            ActorRef deviceActor = deviceIdToActor.get(trackMsg.deviceId);
            if (deviceActor != null) {
                deviceActor.forward(trackMsg, getContext());
            } else {
                log.info("Creating device actor for {}", trackMsg.deviceId);
                deviceActor =
                        getContext()
                                .actorOf(Device.props(groupId, trackMsg.deviceId), "device-" + trackMsg.deviceId);
                getContext().watch(deviceActor);
                actorToDeviceId.put(deviceActor, trackMsg.deviceId);
                deviceIdToActor.put(trackMsg.deviceId, deviceActor);
                deviceActor.forward(trackMsg, getContext());
            }
        } else {
            log.warning(
                    "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
                    groupId,
                    this.groupId);
        }
    }

    private void onDeviceList(RequestDeviceList r) {
        getSender().tell(new ReplyDeviceList(r.requestId, deviceIdToActor.keySet()), getSelf());
    }

    private void onTerminated(Terminated t) {
        ActorRef deviceActor = t.getActor();
        String deviceId = actorToDeviceId.get(deviceActor);
        log.info("Device actor for {} has been terminated", deviceId);
        actorToDeviceId.remove(deviceActor);
        deviceIdToActor.remove(deviceId);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(DeviceManager.RequestTrackDevice.class, this::onTrackDevice)
                .match(RequestDeviceList.class, this::onDeviceList)
                .match(Terminated.class, this::onTerminated)
                .build();
    }

    public enum TemperatureNotAvailable implements TemperatureReading {
        INSTANCE
    }


    public enum DeviceNotAvailable implements TemperatureReading {
        INSTANCE
    }

    public enum DeviceTimedOut implements TemperatureReading {
        INSTANCE
    }

    public static interface TemperatureReading {
    }

    public static final class RequestDeviceList {
        final long requestId;

        public RequestDeviceList(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class ReplyDeviceList {
        final long requestId;
        final Set<String> ids;

        public ReplyDeviceList(long requestId, Set<String> ids) {
            this.requestId = requestId;
            this.ids = ids;
        }
    }

    public static final class RequestAllTemperatures {
        final long requestId;

        public RequestAllTemperatures(long requestId) {
            this.requestId = requestId;
        }
    }

    public static final class RespondAllTemperatures {
        final long requestId;
        final Map<String, TemperatureReading> temperatures;

        public RespondAllTemperatures(long requestId, Map<String, TemperatureReading> temperatures) {
            this.requestId = requestId;
            this.temperatures = temperatures;
        }
    }

    public static final class Temperature implements TemperatureReading {
        public final double value;

        public Temperature(double value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Temperature that = (Temperature) o;

            return Double.compare(that.value, value) == 0;
        }

        @Override
        public int hashCode() {
            long temp = Double.doubleToLongBits(value);
            return (int) (temp ^ (temp >>> 32));
        }

        @Override
        public String toString() {
            return "Temperature{" + "value=" + value + '}';
        }
    }
}