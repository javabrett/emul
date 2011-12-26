/**
 * 
 */
package v9t9.engine.modules;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.events.NotifyException;
import v9t9.common.modules.IModule;
import ejs.base.utils.StorageException;
import ejs.base.utils.StreamXMLStorage;
import ejs.base.utils.XMLUtils;

/**
 * @author ejs
 *
 */
public class ModuleLoader {

	/**
	 * @return
	 */
	public static List<IModule> loadModuleList(ISettingsHandler settings, InputStream is) throws NotifyException {
		
		StreamXMLStorage storage = new StreamXMLStorage();
		storage.setInputStream(is);
		List<IModule> modules = new ArrayList<IModule>();
		try {
			storage.load("modules");
		} catch (StorageException e) {
			if (e.getCause() instanceof StorageException)
				throw new NotifyException(null, "Error loading module database", e.getCause());
			throw new NotifyException(null, "Error parsing module database", e);
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
