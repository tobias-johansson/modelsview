package tojo.emf.modelsview.views

import scala.collection.JavaConversions._
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.viewers.IStructuredContentProvider
import org.eclipse.jface.viewers.ITreeContentProvider
import org.eclipse.jface.viewers.Viewer
import org.eclipse.sphinx.emf.model.ModelDescriptorRegistry
import org.eclipse.sphinx.emf.model.IModelDescriptor
import org.eclipse.swt.graphics.Image
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.sphinx.emf.model.IModelDescriptorChangeListener
import org.eclipse.jface.viewers.TreeViewer
import org.eclipse.emf.transaction.ResourceSetListenerImpl
import org.eclipse.emf.transaction.NotificationFilter
import org.eclipse.emf.transaction.ResourceSetChangeEvent
import org.eclipse.ui.progress.UIJob
import org.eclipse.core.runtime.IStatus
import org.eclipse.core.runtime.IProgressMonitor
import org.eclipse.core.runtime.Status
import tojo.emf.modelsview.Activator
import org.eclipse.ui.PlatformUI
import org.eclipse.core.resources.IContainer
import org.eclipse.sphinx.emf.workspace.saving.ModelSaveManager
import org.eclipse.sphinx.emf.util.EcorePlatformUtil
import java.util.Date
import org.eclipse.sphinx.emf.workspace.saving.IModelDirtyChangeListener
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.io.InputStream
import org.eclipse.jface.action.IMenuManager
import org.eclipse.jface.action.Action
import org.eclipse.ui.console.MessageConsole
import org.eclipse.ui.console.ConsolePlugin
import org.eclipse.jface.viewers.IStructuredSelection
import org.eclipse.emf.ecore.util.EcoreUtil
import org.eclipse.sphinx.emf.util.EcoreResourceUtil
import org.eclipse.sphinx.emf.util.EObjectUtil
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature
import java.util.Collection
import java.math.BigInteger
import org.eclipse.emf.ecore.ENamedElement
import org.eclipse.ui.internal.console.ConsolePluginImages
import org.eclipse.ui.console.IConsoleConstants

class Content(viewer: TreeViewer)
        extends LabelProvider
        with IStructuredContentProvider
        with ITreeContentProvider
        with IModelDescriptorChangeListener
        with IModelDirtyChangeListener {

    class Listener extends ResourceSetListenerImpl(NotificationFilter.READ) {
        override def resourceSetChanged(event: ResourceSetChangeEvent) = runInUI {
            viewer.refresh();
        }
    }
    val resourceSetListener = new Listener()

    def runInUI(block: => Unit) = {
        val job = new UIJob("") {
            override def runInUIThread(monitor: IProgressMonitor): IStatus = {
                block
                Status.OK_STATUS
            }
        }
        job.schedule()
    }

    def startListeningTo(reg: ModelDescriptorRegistry): Unit = {
        reg.addModelDescriptorChangeListener(this)
        reg.getAllModels() foreach { startListeningTo(_, false) }
    }

    def stopListeningTo(reg: ModelDescriptorRegistry): Unit = {
        reg.removeModelDescriptorChangeListener(this)
        reg.getAllModels() foreach { stopListeningTo(_, false) }
    }

    def startListeningTo(desc: IModelDescriptor, refresh: Boolean) = {
        desc.getEditingDomain().addResourceSetListener(resourceSetListener)
        if (refresh) triggerRefresh()
    }

    def stopListeningTo(desc: IModelDescriptor, refresh: Boolean) = {
        desc.getEditingDomain().removeResourceSetListener(resourceSetListener)
        if (refresh) triggerRefresh()
    }

    def triggerRefresh() = runInUI { if (!viewer.getControl.isDisposed) viewer.refresh() }

    def handleModelAdded(desc: IModelDescriptor) = startListeningTo(desc, true)

    def handleModelRemoved(desc: IModelDescriptor) = stopListeningTo(desc, true)

    def handleDirtyChangedEvent(desc: IModelDescriptor) = triggerRefresh()

    var currentInput: Option[ModelDescriptorRegistry] = None

    override def inputChanged(v: Viewer, oldInput: AnyRef, newInput: AnyRef) = {
        newInput match {
            case reg: ModelDescriptorRegistry =>
                startListeningTo(reg)
                currentInput = Some(reg)
            case _ =>
        }
        oldInput match {
            case reg: ModelDescriptorRegistry => stopListeningTo(reg)
            case _                            =>
        }
        ModelSaveManager.INSTANCE.removeModelDirtyChangedListener(this)
        ModelSaveManager.INSTANCE.addModelDirtyChangedListener(this)
        viewer.refresh()
    }

    override def dispose() = {
        for (reg <- currentInput) { stopListeningTo(reg) }
        ModelSaveManager.INSTANCE.removeModelDirtyChangedListener(this)
    }

    override def getElements(parent: Any): Array[AnyRef] = getChildren(parent)

    override def getParent(child: Any): AnyRef = null;

    override def getChildren(parent: Any): Array[AnyRef] = parent match {
        case reg: ModelDescriptorRegistry =>
            reg.getAllModels().toArray()

        case desc: IModelDescriptor =>
            val set = desc.getLoadedResources(false)
            val referenced = desc.getReferencedRoots().collect {
                case cont: IContainer => ModelDescriptorRegistry.INSTANCE.getModels(cont)
            }.flatten
            val all = (referenced ++ set)
            all.toArray

        case _ => Array()
    }

    override def hasChildren(parent: Any): Boolean =
        getChildren(parent).length > 0;

    override def getText(obj: Any): String = obj match {
        case desc: IModelDescriptor =>
            val mmName = desc.getMetaModelDescriptor().getName()
            val rootName = desc.getRoot().getName()
            val mark = if (ModelSaveManager.INSTANCE.isDirty(desc)) "*" else ""
            s"$mark$rootName [$mmName]"

        case res: Resource =>
            val file = EcorePlatformUtil.getFile(res)
            val path = file.getFullPath().toString()
            val format = new SimpleDateFormat("yyyy-MM-dd, HH:mm:ss")
            val stamp = format.format(new Date(res.getTimeStamp()))
            s"$path [$stamp]"

        case _ => obj.toString
    }

    override def getImage(obj: Any): Image = {
        val act = Activator.getDefault()
        obj match {
            case desc: IModelDescriptor =>
                act.getImage("database.png")
            case res: Resource =>
                val file = EcorePlatformUtil.getFile(res)
                act.getFileImage(file)
            case _ => null
        }
    }

    def fillContextMenu(manager: IMenuManager) = {
        manager.add(new WatchAction())
    }

    val consoleIcon = ConsolePluginImages.getImageDescriptor(IConsoleConstants.IMG_VIEW_CONSOLE)

    class WatchAction extends Action("Watch", consoleIcon) {

        override def run() = viewer.getSelection() match {
            case sel: IStructuredSelection => watch(sel.getFirstElement())
            case _                         =>
        }

        def watch(obj: AnyRef) = obj match {
            case mod: IModelDescriptor => new Watcher(mod, Content.this)
            case _                     =>
        }

    }

}