/*
 * (c) Ed Swartz, 2005
 * 
 * Created on Dec 15, 2004
 *  
 */
package v9t9.engine.memory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IDialogSettings;

/*
 * @author ejs
 */
public class Memory {

    private List<MemoryListener> listeners;

	private final MemoryModel model;

	private Map<String, MemoryDomain> domains = new HashMap<String, MemoryDomain>();

    public void addListener(MemoryListener listener) {
		listeners.add(listener);
	}

	public void removeListener(MemoryListener listener) {
		listeners.remove(listener);
	}

	public void notifyListenersOfPhysicalChange(MemoryEntry entry) {
		if (listeners != null) {
			for (Iterator<MemoryListener> iter = listeners.iterator(); iter
					.hasNext();) {
				MemoryListener element = iter.next();
				element.physicalMemoryMapChanged(entry);
			}
		}
	}
	
	public void notifyListenersOfLogicalChange(MemoryEntry entry) {
		if (listeners != null) {
			for (Iterator<MemoryListener> iter = listeners.iterator(); iter
					.hasNext();) {
				MemoryListener element = iter.next();
				element.logicalMemoryMapChanged(entry);
			}
		}
	}
    
	public void addDomain(String key, MemoryDomain domain) {
		this.domains.put(key, domain);
	}
	
	public MemoryDomain getDomain(String key) {
		return domains.get(key);
	}
    public void addAndMap(MemoryEntry entry) {
        entry.domain.mapEntry(entry);
        notifyListenersOfPhysicalChange(entry);
    }
    
    public Memory(MemoryModel model) {
        this.model = model;
		listeners = new java.util.ArrayList<MemoryListener>();
    }

	public MemoryModel getModel() {
		return model;
	}

	public void saveState(IDialogSettings section) {
		for (Map.Entry<String, MemoryDomain> entry : domains.entrySet()) {
			entry.getValue().saveState(section.addNewSection(entry.getKey()));
		}
	}

	public void loadState(IDialogSettings section) {
		// XXX: we assume the domains are the same
		for (Map.Entry<String, MemoryDomain> entry : domains.entrySet()) {
			entry.getValue().loadState(section.getSection(entry.getKey()));
		}
	}

	public MemoryDomain[] getDomains() {
		return (MemoryDomain[]) domains.values().toArray(new MemoryDomain[domains.values().size()]);
	}

	/**
	 * 
	 */
	public void save() {
		for (MemoryDomain domain : domains.values()) {
			domain.save();
		}
	}

}

