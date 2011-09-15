/**
 * 
 */
package org.ejs.coffee.core.settings;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ejs
 *
 */
public class SettingsSection implements ISettingSection {

	private Map<String, Object> data;

	/**
	 * 
	 */
	public SettingsSection() {
		data = new TreeMap<String, Object>();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "SettingsSection: " + data.toString();
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#addNewSection(java.lang.String)
	 */
	public ISettingSection addSection(String name) {
		SettingsSection newSection = new SettingsSection();
		data.put(name, newSection);
		return newSection;
	}

	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getSection(java.lang.String)
	 */
	public ISettingSection getSection(String sectionName) {
		Object value = data.get(sectionName);
		if (value instanceof ISettingSection)
			return (ISettingSection) value;
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.settings.ISettingSection#findOrAddSection(java.lang.String)
	 */
	public ISettingSection findOrAddSection(String sectionName) {
		ISettingSection section = getSection(sectionName);
		if (section != null)
			return section;
		return addSection(sectionName);
	}

	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getSections()
	 */
	public String[] getSectionNames() {
		List<String> names = new ArrayList<String>();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() instanceof ISettingSection)
				names.add(entry.getKey());
		}
		return (String[]) names.toArray(new String[names.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.settings.ISettingSection#getSections()
	 */
	public ISettingSection[] getSections() {
		List<ISettingSection> sections = new ArrayList<ISettingSection>();
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			if (entry.getValue() instanceof ISettingSection)
				sections.add((ISettingSection) entry.getValue());
		}
		return (ISettingSection[]) sections.toArray(new ISettingSection[sections.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.settings.ISettingSection#getSettingNames()
	 */
	public String[] getSettingNames() {
		return data.keySet().toArray(new String[data.size()]);
	}

	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#get(java.lang.String)
	 */
	public String get(String name) {
		Object value = data.get(name);
		if (value == null)
			return null;
		return value.toString();
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getBool(java.lang.String)
	 */
	public boolean getBoolean(String name) {
		Object value = data.get(name);
		if (value == null)
			return false;
		if (value instanceof Boolean)
			return (Boolean) value;
		return Boolean.TRUE.toString().equals(value.toString());
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getInt(java.lang.String)
	 */
	public int getInt(String name) {
		Object val = data.get(name);
		if (val == null)
			return 0;
		if (val instanceof Number)
			return ((Number) val).intValue();
		
		try {
			return Integer.parseInt(val.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getDouble(java.lang.String)
	 */
	public double getDouble(String name) {
		Object val = data.get(name);
		if (val == null)
			return 0;
		if (val instanceof Number)
			return ((Number) val).doubleValue();
		
		try {
			return Integer.parseInt(val.toString());
		} catch (NumberFormatException e) {
			return 0;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#getArray(java.lang.String)
	 */
	public String[] getArray(String name) {
		Object value = data.get(name);
		if (value instanceof String[])
			return (String[]) value;
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.settings.ISettingSection#getObject(java.lang.String)
	 */
	public Object getObject(String name) {
		return data.get(name);
	}

	private void doPut(String name, Object value) {
		if (value != null)
			data.put(name, value);
		else
			data.remove(name);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#put(java.lang.String, java.lang.String)
	 */
	public void put(String name, String value) {
		doPut(name, value);
	}
	
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#put(java.lang.String, boolean)
	 */
	public void put(String name, boolean value) {
		doPut(name, value);		
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#put(java.lang.String, double)
	 */
	public void put(String name, double value) {
		doPut(name, value);		
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#put(java.lang.String, int)
	 */
	public void put(String name, int value) {
		doPut(name, value);		
	}
	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.properties.IPropertyStorage#put(java.lang.String, java.lang.String[])
	 */
	public void put(String name, String[] array) {
		doPut(name, array);	
	}

	/* (non-Javadoc)
	 * @see org.ejs.coffee.core.settings.ISettingSection#put(java.lang.String, java.lang.Object)
	 */
	public void put(String name, Object value) {
		doPut(name, value);	
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<SettingEntry> iterator() {
		return new Iterator<SettingEntry>() {
			Iterator<Map.Entry<String, Object>> innerIter = data.entrySet().iterator();
			
			public boolean hasNext() {
				return innerIter.hasNext();
			}

			public SettingEntry next() {
				Map.Entry<String, Object> e = innerIter.next();
				SettingEntry entry = new SettingEntry();
				entry.name = e.getKey();
				entry.value = e.getValue();
				if (entry.value instanceof Boolean)
					entry.type = Type.Boolean;
				else if (entry.value instanceof Integer)
					entry.type = Type.Int;
				else if (entry.value instanceof Double)
					entry.type = Type.Double;
				else if (entry.value instanceof String)
					entry.type = Type.String;
				else if (entry.value instanceof String[])
					entry.type = Type.StringArray;
				else if (entry.value instanceof ISettingSection)
					entry.type = Type.Section;
				else {
					entry.type = Type.Object;
				}
				return entry;
			}

			public void remove() {
				innerIter.remove();
			}
			
		};
	}
}