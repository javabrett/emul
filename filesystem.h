/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0 
 * which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html 
 *  
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

/*
 * Target service implementation: file system access (TCF name FileSystem)
 */

#ifndef D_filesystem
#define D_filesystem

#include "protocol.h"
/*
 * Initialize file system service.
 */
extern void ini_file_system_service(Protocol *);

#endif
