package tojo.emf.modelsview.views

import scala.collection.JavaConversions._
import org.eclipse.emf.transaction.ResourceSetChangeEvent
import org.eclipse.emf.transaction.ResourceSetListenerImpl
import org.eclipse.ui.console.MessageConsole
import org.eclipse.sphinx.emf.model.IModelDescriptor
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.emf.transaction.NotificationFilter
import org.eclipse.jface.viewers.ILabelProvider
import org.eclipse.emf.ecore.util.EcoreUtil
import java.math.BigInteger
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.swt.graphics.Color
import org.eclipse.swt.widgets.Display
import org.eclipse.emf.common.notify.Notification
import org.eclipse.ui.console.MessageConsoleStream

class Watcher(mod: IModelDescriptor, labelProvider: ILabelProvider) extends ResourceSetListenerImpl(NotificationFilter.ANY) {

    val console = new MessageConsole(labelProvider.getText(mod), null);
    ConsolePlugin.getDefault().getConsoleManager().addConsoles(Array(console))
    val streams = new ColorStreams(console)
    mod.getEditingDomain().addResourceSetListener(this)

    override def resourceSetChanged(event: ResourceSetChangeEvent) = {

        for (not <- event.getNotifications) {
            val doPrint = not.getNotifier() match {
                case eo: EObject => mod.belongsTo(eo.eResource(), true)
                case _           => false
            }

            if (doPrint) {
                val eventType = not.getEventType()
                val event = eventName(eventType)
                val notifier = describeNotifier(not.getNotifier())

                val oldVal = describeValue(not.getOldValue())
                val newVal = describeValue(not.getNewValue())
                val feature = describeValue(not.getFeature())

                val stream = streams.streamFor(eventType)
                stream.println(s"$event @ $notifier [$feature: $oldVal -> $newVal]")
            }
        }
    }

    def describeNotifier(obj: Any) = obj match {
        case eo: EObject => EcoreUtil.getURI(eo)
        case obj         => obj.toString()
    }

    def describeValue(value: Any) = value match {
        case null              => "null"
        case ne: ENamedElement => ne.getName()
        case eo: EObject       => eo.eClass().getName()
        case ob if (isSimple(ob)) =>
            val typ = ob.getClass().getSimpleName()
            val str = ob.toString()
            s"$str ($typ)"
        case ob =>
            ob.getClass().getSimpleName()
    }

    def isSimple(obj: Any) = obj match {
        case _: String     => true
        case _: Int        => true
        case _: Boolean    => true
        case _: Float      => true
        case _: Double     => true
        case _: Char       => true
        case _: Byte       => true
        case _: BigInteger => true
        case _             => false
    }

    def eventName(eid: Int) = eid match {
        case Notification.CREATE           => "CREATE"
        case Notification.SET              => "SET   "
        case Notification.UNSET            => "UNSET "
        case Notification.ADD              => "ADD   "
        case Notification.REMOVE           => "REMOVE"
        case Notification.ADD_MANY         => "ADD_MANY"
        case Notification.REMOVE_MANY      => "REMOVE_MANY"
        case Notification.MOVE             => "MOVE  "
        case Notification.REMOVING_ADAPTER => "REMOVING_ADAPTER"
        case Notification.RESOLVE          => "RESOLVE"
        case _                             => "<unknown>"
    }
}

object Colors {
    val red = new Color(Display.getCurrent(), 255, 0, 0)
    val green = new Color(Display.getCurrent(), 0, 210, 0)
    val blue = new Color(Display.getCurrent(), 0, 0, 255)
}

class ColorStreams(console: MessageConsole) {
    private def newStream(c: Color) = {
        val stream = console.newMessageStream()
        stream.setColor(c)
        stream
    }

    val default = console.newMessageStream()
    val red = newStream(Colors.red)
    val green = newStream(new Color(Display.getCurrent(), 0, 210, 0))
    val blue = newStream(Colors.blue)

    def streamFor(eid: Int): MessageConsoleStream = eid match {
        case Notification.SET | Notification.MOVE                                => blue
        case Notification.UNSET | Notification.REMOVE | Notification.REMOVE_MANY => red
        case Notification.ADD | Notification.ADD_MANY                            => green
        case _                                                                   => default
    }

}