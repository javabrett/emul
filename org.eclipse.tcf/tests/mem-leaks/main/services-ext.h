/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

/*
 * Services initialization code extension point.
 * If the agent is built with additional user-defined services,
 * a customized version of services-ext.h file can be added to compiler headers search paths.
 */

#include <services/memtrace.h>

static void ini_ext_services(Protocol * proto, TCFBroadcastGroup * bcg) {
    ini_mem_trace_service(proto);
}
