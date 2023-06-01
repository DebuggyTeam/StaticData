package gay.debuggy.staticdata.impl;

import gay.debuggy.staticdata.api.StaticDataItem;
import net.minecraft.util.Identifier;

public abstract class AbstractDataItem implements StaticDataItem {
	private final String modId;
	private Identifier resourceId;
	
	public AbstractDataItem(String modId, Identifier resourceId) {
		this.modId = modId;
		this.resourceId = resourceId;
	}
	
	@Override
	public String getModId() {
		return modId;
	}

	@Override
	public Identifier getResourceId() {
		return resourceId;
	}

}
