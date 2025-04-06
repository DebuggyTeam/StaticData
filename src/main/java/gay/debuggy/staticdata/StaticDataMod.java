package gay.debuggy.staticdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public class StaticDataMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("StaticData");
	
	@Override
	public void onInitialize() {
		// we don't really need to do anything here for a library mod, this is just here to satisfy assumptions about mods.
	}
	
}
