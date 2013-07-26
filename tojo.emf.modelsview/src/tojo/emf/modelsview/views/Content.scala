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

class Content(viewer: TreeViewer)
        extends LabelProvider
        with IStructuredContentProvider
        with ITreeContentProvider
        with IModelDescriptorChangeListener {

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

    def handleModelAdded(desc: IModelDescriptor) = runInUI {
        desc.getEditingDomain().addResourceSetListener(resourceSetListener)
        viewer.refresh()
    }

    def handleModelRemoved(desc: IModelDescriptor) = runInUI {
        desc.getEditingDomain().removeResourceSetListener(resourceSetListener)
        viewer.refresh()
    }

    override def inputChanged(v: Viewer, oldInput: AnyRef, newInput: AnyRef) = {
        newInput match {
            case reg: ModelDescriptorRegistry =>
                reg.addModelDescriptorChangeListener(this)
                reg.getAllModels() foreach { desc => desc.getEditingDomain().addResourceSetListener(resourceSetListener) }
            case _ =>
        }
        oldInput match {
            case reg: ModelDescriptorRegistry =>
                reg.removeModelDescriptorChangeListener(this)
                reg.getAllModels() foreach { desc => desc.getEditingDomain().removeResourceSetListener(resourceSetListener) }
            case _ =>
        }
    }
    override def dispose() {}

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
            s"$rootName [$mmName]"

        case res: Resource =>
            val uri = res.getURI()
            val path = uri.path()
            val status = (res.isLoaded, res.isModified)
            val mark = status match {
                case (false, _)   => "-"
                case (true, true) => "*"
                case _            => ""
            }
            s"$mark$path"

        case _ => obj.toString
    }
    override def getImage(obj: Any): Image = {
        val act = Activator.getDefault()
        obj match {
            case desc: IModelDescriptor => act.getImage("database.png")
            case res: Resource          => act.getFileImage(res.getURI().fileExtension())
            case _                      => null
        }
    }

}