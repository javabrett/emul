/*
  IModuleDetector.java

  (c) 2015 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.common.modules;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author ejs
 *
 */
public interface IModuleDetector {
	
	/** Clear collected results and start over.  Does not reset #set...() calls. */
	void clear();

	/** Scan the given directory and return modules found in that location */
	Collection<IModule> scan(File base);
	
	/** Get the cumulative detected modules after or more invocations of #scan() */
	Collection<IModule> getAllModules();

	/**
	 * If set, read the ROM headers even if there is another name
	 * available (e.g. from RPK)
	 * @param readHeaders
	 */
	void setReadHeaders(boolean readHeaders);

	/**
	 * If set, ignore stock module database, to see what the 
	 * module itself thinks the name is (e.g. auto-start or from RPK)
	 * @param ignoreStock
	 */
	void setIgnoreStock(boolean ignoreStock);

	/**
	 * From all the detected modules, group them by module MD5.
	 * @return map of MD5 to modules
	 */
	Map<String, List<IModule>> gatherDuplicatesByMD5();

	/**
	 * From all the detected modules, group them by module name.
	 * @return map of name to modules
	 */
	Map<String, List<IModule>> gatherDuplicatesByName();

	/**
	 * From all the detected modules, return a list of the
	 * unique modules
	 * @param removeFilenames if true, remove any filename or URI information. 
	 * @return list
	 */
	List<IModule> simplifyModules(boolean removeFilenames);

}
