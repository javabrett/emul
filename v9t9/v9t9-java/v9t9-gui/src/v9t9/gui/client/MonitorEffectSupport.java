/*
  MonitorEffectSupport.java

  (c) 2012-2013 Edward Swartz

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  This program is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
 */
package v9t9.gui.client;

import java.util.LinkedHashMap;
import java.util.Map;

import v9t9.common.client.IMonitorEffect;
import v9t9.common.client.IMonitorEffectSupport;

/**
 * @author ejs
 *
 */
public class MonitorEffectSupport implements IMonitorEffectSupport {

	public static MonitorEffectSupport INSTANCE = new MonitorEffectSupport();

	private Map<String, IMonitorEffect> monitorEffects = new LinkedHashMap<String, IMonitorEffect>();
	
	public void registerEffect(String id, IMonitorEffect effect) {
		monitorEffects.put(id, effect);
	}
	
	public String[] getIds() {
		return monitorEffects.keySet().toArray(new String[monitorEffects.keySet().size()]);
	}
	
	public IMonitorEffect getEffect(String id) {
		return monitorEffects.get(id);
	}
}
