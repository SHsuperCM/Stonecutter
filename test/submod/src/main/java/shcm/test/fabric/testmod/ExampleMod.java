package shcm.test.fabric.testmod;

import net.fabricmc.api.ModInitializer;

import net.minecraft.item.Item;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*? >=1.19.4 {?*/
import net.minecraft.registry.Registries;
/*?} else {?*//*
import net.minecraft.util.registry.Registry;
/*?}?*/

public class ExampleMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("testmod");

	@Override
	public void onInitialize() {

		LOGGER.info("Example of inline versioned value getter");
		Item item = /*? >=1.19.4 {?*/ Registries.ITEM.get(new Identifier("minecraft:flint")) /*?} else {?*//* Registry.ITEM.get(new Identifier("minecraft:flint")) /*?}?*/;

		LOGGER.info("Example of 'only run in this version constraint' syntax");
		/*? ~1.20 {?*/
		item = Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?}?*/

		LOGGER.info("Example of 'if else' syntax");
		/*? >=1.19.4 {?*/
		item = Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?} else {?*//*
		item = Registry.ITEM.get(new Identifier("minecraft:flint"));
		/*?}?*/

		LOGGER.info("Example of 'if elseif else' syntax and how to avoid versioned imports");
		/*? >=1.19.4 {?*/
		item = net.minecraft.registry.Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?} else ~1.19.3 {?*//*
		item = net.minecraft.registry.Registries.ITEM.get(new Identifier("minecraft:flint"));
		/*?} else {?*//*
		item = net.minecraft.util.registry.Registry.ITEM.get(new Identifier("minecraft:flint"));
		/*?}?*/
	}


	{
		/*?$token disable test_token?*/
		System.out.println("the test was for 1.19.2");
		System.out.println("the test was for 1.19.2"); // this was the 1.19.2 thing?
		/*?$token enable test_token?*/
		System.out.println("the test is the 1.20.1");
		System.out.println("the test is for 1.20.1");
		System.out.println("This is for 1.20.1");
		System.out.println("the test is for 1.20.1");
		System.out.println("the test was lmaoXDKEKW  1.19.2");
		/*?$token disable test_token?*/
		System.out.println("the test was for 1.19.2");

		/*
		This was for 1.19.2
		 */
	}
}