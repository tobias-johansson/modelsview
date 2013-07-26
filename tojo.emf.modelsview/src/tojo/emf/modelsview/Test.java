package tojo.emf.modelsview;

import org.eclipse.sphinx.emf.model.IModelDescriptor;
import org.eclipse.sphinx.emf.model.ModelDescriptorRegistry;

public class Test {

	void apa() {
		ModelDescriptorRegistry reg = ModelDescriptorRegistry.INSTANCE;
		for (IModelDescriptor desc : reg.getAllModels()) {
			System.out.println(desc.getMetaModelDescriptor().getName());
			System.out.println(desc.getRoot().getName());
			
		}
	}
}
