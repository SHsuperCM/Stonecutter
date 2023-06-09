package shcm.test.fabric.testmod;

import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? >=1.19.4 {
import net.minecraft.registry.Registries;
/*?}*//*? else {
import net.minecraft.util.registry.Registry;
//?}*/

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("testmod");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Hello Fabric world!");

		Item item;

		//? >=1.19.4 {
		item = Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?}*//*? else {
		item = Registry.ITEM.get(new Identifier("minecraft:flint"));
		//?}*/


		//? >=1.19.4 {
		item = Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?}*//*? ~1.19.3 {
		item = Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?}*//*? else {
		item = Registry.ITEM.get(new Identifier("minecraft:flint"));
		//?}*/
	}
}