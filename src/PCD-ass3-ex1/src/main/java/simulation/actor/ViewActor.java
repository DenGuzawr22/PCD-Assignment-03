package simulation.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import simulation.actor.CoordinatorActor.*;
import simulation.basic.P2d;
import simulation.gui.SimulationView;

import java.util.List;

/**
 * Visualize bodies
 */
public class ViewActor extends AbstractBehavior<ViewActor.UpdateViewMsg> {
    public record UpdateViewMsg(List<P2d> positions, double vt, long iter){}

    private final SimulationView viewer;
    private final ActorRef<CoordinatorMsg> coordinatorRef;

    private ViewActor(ActorContext<UpdateViewMsg> context, SimulationView viewer, ActorRef<CoordinatorMsg> coordinatorRef) {
        super(context);
        this.viewer = viewer;
        this.coordinatorRef = coordinatorRef;
        viewer.getFrame().setStopHandler(h -> getContext().getSystem().terminate());
    }

    public static Behavior<UpdateViewMsg> create(SimulationView viewer, ActorRef<CoordinatorMsg> coordinatorRef){
        return Behaviors.setup(ctx -> new ViewActor(ctx, viewer, coordinatorRef));
    }

    @Override
    public Receive<UpdateViewMsg> createReceive() {
        return newReceiveBuilder()
                .onMessage(UpdateViewMsg.class, this::onUpdateViewMsg)
                .build();
    }

    private Behavior<UpdateViewMsg> onUpdateViewMsg(UpdateViewMsg message) {
        this.viewer.display(message.positions, message.vt, message.iter);
        this.coordinatorRef.tell(new ViewUpdateFeedback());
        return this;
    }
}
