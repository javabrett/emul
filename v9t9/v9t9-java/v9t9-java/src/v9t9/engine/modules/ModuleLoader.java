/**
 * 
 */
package v9t9.engine.modules;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.ejs.coffee.core.utils.FileXMLStorage;
import org.ejs.coffee.core.utils.XMLUtils;
import org.w3c.dom.Element;

import v9t9.emulator.clients.builtin.NotifyException;
import v9t9.emulator.common.EmulatorSettings;
import v9t9.engine.files.DataFiles;

/**
 * @author ejs
 *
 */
public class ModuleLoader {

	/**
	 * @return
	 */
	public static List<IModule> loadModuleList(String name) throws NotifyException {
		
		
		File file;
		
		file = DataFiles.resolveFileAtPath(EmulatorSettings.INSTANCE.getConfigFilePath(), name);
		if (file == null) {
			file = DataFiles.resolveFile(name);
			if (file == null)
				throw new NotifyException(null, "Cannot locate module list " + name);
		}
		
		List<IModule> modules = new ArrayList<IModule>();
		FileXMLStorage storage = new FileXMLStorage(file);
		try {
			storage.load("modules");
		} catch (CoreException e) {
			if (e.getCause() instanceof IOException)
				throw new NotifyException(null, "Error loading module list " + name, e.getCause());
			throw new NotifyException(null, "Error parsing module list " + name, e);
		}
		for (Element el : XMLUtils.getChildElementsNamed(storage.getDocumentElement(), "module")) {
			Module module = new Module(
					el.getAttribute("name"));
			module.loadFrom(el);
			modules.add(module);
		}
		return modules;
	}
}