/*
  IMemory.java

  (c) 2011-2015 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.memory;

import v9t9.common.events.IEventNotifier;
import ejs.base.properties.IPersistable;
import ejs.base.settings.ISettingSection;

/**
 * @author ejs
 *
 */
public interface IMemory extends IPersistable {

	void loadMemory(IEventNotifier notifier, ISettingSection section);
	
	IMemoryEntryFactory getMemoryEntryFactory();
	void setMemoryEntryFactory(IMemoryEntryFactory factory);
	
	void addListener(IMemoryListener listener);

	void removeListener(IMemoryListener listener);

	void notifyListenersOfPhysicalChange(IMemoryEntry entry);

	void notifyListenersOfLogicalChange(IMemoryEntry entry);

	void addDomain(String key, IMemoryDomain domain);

	IMemoryDomain getDomain(String key);

	void addAndMap(IMemoryEntry entry);

	void removeAndUnmap(IMemoryEntry entry);

	void setModel(IMemoryModel model);
	
	IMemoryModel getModel();

	IMemoryDomain[] getDomains();

	/**
	 * 
	 */
	void save();

	/**
	 * 
	 */
	void clear();
	/**
	 * 
	 */
	void reset();

}