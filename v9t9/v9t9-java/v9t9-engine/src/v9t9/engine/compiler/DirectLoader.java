/*
  DirectLoader.java

  (c) 2008-2013 Edward Swartz

  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
 */
package v9t9.engine.compiler;

import java.security.SecureClassLoader;

/** Simple-minded loader for constructed classes. */
class DirectLoader extends SecureClassLoader {
    protected DirectLoader() {
        super();
    }

    protected Class<?> load(String name, byte[] data) {
        return super.defineClass(name, data, 0, data.length);
    }
}