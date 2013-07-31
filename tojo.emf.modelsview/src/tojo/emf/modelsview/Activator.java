package tojo.emf.modelsview;

import java.net.URL;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "tojo.emf.modelsview"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;
	
	static String apa = "APA";
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	public Image getFileImage(IFile file) {
		try {	
			ImageRegistry registry = getDefault().getImageRegistry();

			String extension = file.getFileExtension();
			String contentType = file.getContentDescription().getContentType().getId();

			String id = extension + "-" + contentType;
			Image image = registry.get(id);
			if (image == null) {
				ImageDescriptor descriptor = 
						PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(
								file.getName(), file.getContentDescription().getContentType());
				if (descriptor == null) return null;
				registry.put(id, descriptor);
				return registry.get(id);
			} else {
				return image;
			}
		} catch (CoreException e) {
			return null;
		}
	}
	
	public ImageDescriptor getImageDescriptor(String id) {
		ImageRegistry registry = getDefault().getImageRegistry();
		ImageDescriptor descriptor = registry.getDescriptor(id);
		if (descriptor != null) return descriptor;
		URL url = getDefault().getBundle().getEntry("icons/" + id);
		if (url == null) return null;
		descriptor = ImageDescriptor.createFromURL(url);
		if (descriptor == null) return null;
		registry.put(id, descriptor);
		return registry.getDescriptor(id);
	}
	
	public Image getImage(String id) {
		ImageRegistry registry = getDefault().getImageRegistry();
		Image image = registry.get(id);
		if (image != null) return image;
		getImageDescriptor(id);
		return registry.get(id);
	}
	
	@Override
    protected void initializeImageRegistry(ImageRegistry registry) {
        super.initializeImageRegistry(registry);
        
    }

}
