/*
  IModuleManager.java

  (c) 2011 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.modules;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import v9t9.common.client.ISettingsHandler;
import v9t9.common.events.NotifyException;
import v9t9.common.memory.IMemoryEntry;
import v9t9.common.settings.SettingSchema;
import ejs.base.properties.IPersistable;

/**
 * @author ejs
 *
 */
public interface IModuleManager extends IPersistable {

	SettingSchema settingModuleList = new SettingSchema(
			ISettingsHandler.MACHINE,
			"UserModuleLists", String.class, new ArrayList<String>());

	URL getStockDatabaseURL();
	
	void clearModules();
	void addModules(Collection<IModule> modList);

	//void loadModuleDatabases(String[] files, IEventNotifier notifier);

	IModule[] getModules();
	List<IModule> readModules(URI databaseURI) throws IOException;

	void switchModule(IModule module) throws NotifyException;

	void unloadAllModules();

	void loadModule(IModule module) throws NotifyException;

	void unloadModule(IModule loaded);

	void switchModule(String name) throws NotifyException;

	IModule findModuleByName(String string, boolean exact);

	/**
	 * @return
	 */
	IModule[] getLoadedModules();

	/**
	 * Get the memory entries for the module segments
	 * @param module
	 * @return collection of entries
	 * @throws NotifyException if any segment cannot be loaded
	 */
	Collection<IMemoryEntry> getModuleMemoryEntries(IModule module) throws NotifyException;

	/**
	 * @param stockDatabaseURL
	 */
	void registerModules(URI databaseURI);

	/**
	 * 
	 */
	void reload();

	/**
	 * @param module
	 */
	void removeModule(IModule module);

	/**
	 * @return
	 */
	ModuleInfoDatabase getModuleInfoDatabase();
}