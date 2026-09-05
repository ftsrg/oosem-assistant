package hu.bme.mit.sysml.oosem.util.readers.uvl;

import java.util.Collection;
import java.util.Collections;
 
public class ConfigurationSelection {
 
	public ConfigurationSelection(Collection<String> selectedIds, Collection<String> unselectedIds) {
		this.selectedIds = Collections.unmodifiableCollection(selectedIds);
		this.unselectedIds = Collections.unmodifiableCollection(unselectedIds);
	}
 
	public Collection<String> getSelectedIds() {
		return selectedIds;
	}
 
	public Collection<String> getUnselectedIds() {
		return unselectedIds;
	}
 
	public boolean isSelected(String id) {
		return selectedIds.contains(id);
	}
	
	public boolean hasElement(String id) {
		return selectedIds.contains(id) || unselectedIds.contains(id);
	}
	
	public int getElementCount() {
		return selectedIds.size()+unselectedIds.size();
	}
 
	private final Collection<String> selectedIds;
	private final Collection<String> unselectedIds;
}