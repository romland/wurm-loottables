/**
 * Entry point if running as modloader-mod.
 */
package com.friya.wurmonline.server.loot;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.classhooks.InvocationHandlerFactory;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PlayerMessageListener;
import org.gotti.wurmunlimited.modloader.interfaces.ServerStartedListener;
import org.gotti.wurmunlimited.modloader.interfaces.WurmServerMod;

import com.wurmonline.server.Items;
import com.wurmonline.server.creatures.Communicator;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;

import javassist.CtClass;
import javassist.CtPrimitiveType;
import javassist.NotFoundException;
import javassist.bytecode.Descriptor;


// TODO: Paste lists from code

// TODO: Be able to drop objects with inscriptions (like papyrus), handy for quest/reward hints -- reward scrolls, say "Certificate of Bridge building" or "Certificate of Weapon Smithing [1.0 points]"

/*
25jul2017

Not sure if it's been completely clear, but you can have any Wurm item into the LootItem table (custom made items from mods too).
But the important bit here: By default I only insert a *subset* of Wurm items. Items that are not very plausible as drops like carts, boats, etc are not inserted.
I also excluded items like GM wands, mission ruler, etc. 
That said, you can easily insert any missing items yourself by looking up the Wurm template ID in the com/wurmonline/server/items/ItemTemplateCreator* classes.

- Ability to set weight of LootItems, you can also add a random factor to the weight. Use 'weight' and 'weightrandom' (this is in grams). As with other random parameters,
  if weight is set to 1000 (1kg) and you have a weightrandom set to 500, the weight will be a random value between 1000 and 1500 grams.

- Added ability to force an item to be rare. Do this by setting 'forcerare' to 0 for normal, 1 for rare, 2 for supreme and 3 for fantastic. This is different from how rarity works on LootItems otherwise, you could always set items to 'can-be-rare' and then it is done through randomness (bones were an exception, they would always be rare or better). This new option will *force* them to go rare, supreme or fantastic.

- Ability to set colour of LootItems. This can be useful for almost every item of course, but I set out to add it for the purpose of being able to drop
  properly configured scale and hide. Please note that some items will simply not be affected by colour -- sadly nothing that I can do about that.
  As is often the case when defining colours, RGB is used, you can get more information about that here: https://en.wikipedia.org/wiki/RGB_color_model

  Added fields to LootItems table: colorR / colorRrandom (redness), colorG / colorGrandom (greenness) and colorB / colorBrandom (blueness).
  So, setting coloR to 50 and colorRrandom to 205 will create a random 'redness' between 50 and 255. The final value of each color-component (R, G or B) must be between 0 and 255 or you may get an unpredictable result.

  As a side-note, dragon scale and drake hide colours are:
  Red dragon/drake: 215, 40, 40
  Black dragon/drake: 10, 10, 10
  Green dragon/drake: 10, 210, 10
  White dragon/drake: 255, 255, 255
  Blue dragon/drake: 40, 40, 215

	To test all three additions:

INSERT INTO FriyaLootItems(id, itemids, name, dropchance, colorr, colorg, colorb, weight) VALUES(150001, 372, "red dragon scale", 100.0, 215, 40, 40, 100);
INSERT INTO FriyaLootItems(id, itemids, name, dropchance, weight, weightrandom) VALUES(150002, 347, "tasty meal", 100.0, 500, 500);
INSERT INTO FriyaLootItems(id, itemids, name, dropchance, colorr, colorg, colorb, forcerare) VALUES(150003, 2, "sweet satchel", 100.0, 0, 0, 255, 2);

INSERT INTO FriyaLootTables values(123457, 150001);
INSERT INTO FriyaLootTables values(123457, 150002);
INSERT INTO FriyaLootTables values(123457, 150003);

INSERT INTO FriyaLootRules(rulename, loottable, creature, age) VALUES("Test of color, weight and forced rarity", 123457, "troll", "venerable");

The above examples will make all *venerable* trolls drop
a meal weighing between 0.5 and 1.0kg (named nomnom meal)
0.1kg red dragon scale
a blue satchel that is forced to become supreme

http://i.imgur.com/0PwS30a.png


	PS. I know I use 'color' in code and 'colour' in text. I am consistent in my inconsistency.


As always when we need to update the database: On your next restart of server, the new fields will be merged into the existing database automatically (no action required from your side). But yes, you do have to restart your server to see the new fields. :)


*/


/* Patch notes:
 * 	11jul2017
 * 		- Added functionality to enable/disable loot rules. In LootRules table, set to 0 to disable a rule, and 1 to re-enable it. As with everything else in the rules table, it can be done while server is running. Unless you specify otherwise, rules are by default enabled when created.
 * 
 * 		- Added ability to create e.g. the various runes as loot. Set the 'realtemplate' field in LootItems to
 * 			1102 = rift stone,
 * 			1103 = rift crystal,
 * 			1104 = rift wood
 * 		  After that it's just a matter of doing your normal thing and setting the item in 'itemids' (1289: rune of Magranon, 1290 Fo, 1291 Vyn, 1292 Lib, 1293 Jackal) and which material you want it to be (see https://wurmpedia.com/index.php/Runes)
 * 
 * 		- The 'realtemplate' field can also be used to modify food related items in various ways (I have not looked at the details)
 * 
 * An example:
 *	INSERT INTO FriyaLootItems(id, itemids, name, material, startql, endql, canberare, dropchance, realtemplate) VALUES(150000, 1292, "uber rune of Libila", 13, 80.0, 99.99, 1, 100.0, 1104);
 *	INSERT INTO FriyaLootTables values(123456, 150000);
 *	INSERT INTO FriyaLootRules(rulename, loottable, creature, enabled) VALUES("Rune test", 123456, "black wolf", 1);
 * 
 * This will make black wolves have a 100% chance to drop runes of Libila made out of zinc (13, see com.wurmonline.shared.constants.ItemMaterials for full list), and a 'realtemplate' specifying that it's a 'rift wood' subtype (1104).
 *
 * The example uses IDs 123456 for the loot table and 150000 for the loot item to make it easy to test and remove.
 * 
 * 
 * As always when we need to update the database: On your next restart of server, the new fields will be merged into the existing database automatically (no action required from your side). But yes, you do have to restart your server to see the new fields. :)
 */

// TODO:
//		- implement the config options specified in the config file
//		- Create a tool (in-game) to add rules
//		- some way of cleaning up: if something is left to rot, just destroy it after X minutes -- how do we know if it's left to rot?
//		- make an override setting in the actual loot-rule saying that it has X% for any one item to drop, thus overriding the individual item's drop setting
//		- keep stats on how often rules were triggered and how often (and what) they dropped
//		x be able to add enchants (start power +- random power -- 0 = no random)
//		x be able to set number of items it should drop (i.e. 5 eggs)
//
// refine creature further:
//		- which faith they have
//
// refine loot further:
//		x dye (color)
//		- whether all loot should spawn in a container (satchel, ...)
//		x forced rarity
//
// items:
//		- summerhat does it really get transferred to an NPC?
//		- mask of returner (etc) does not show up on body (?)
//		- bag of keeping cannot be on NPCs (or their corpse)?
//
// Extend:
//		- ability to assign loot to fishers, farmers, diggers, miners, forage, botanize
//

/*
 * in LootItem:
TODO		this.canBeRare = (canBeRare == 1 ? true : false);
TODO		this.decayTime = decayTime;
TODO		this.customMethod = customMethod;
TODO		this.customArgument = customArgument;
TODO: Force to rare
*/			


/*
You mean by using Java or by using database? I'll assume database: 

Provided you have a loot table with items already. Then as an example:

- open modsupport.db in your favorite SQLite editor
- run: insert into friyalootrules(rulename, loottable, creature, maxaltitude) values("Lowland Nogump Slaying", <<loot-table-id>>, "son of nogump", 200);
- write changes / save

If a Nogump is killed and they're below 200 dirt altitude, they will now use the loot table you just created.
Replace <<loot-table-id>> with the ID of the loot table containing the items you want the Nogump to drop.

That's it.


Some hints and tips:
[] The LootTables mod will only *read* from the database after server was started.
[] Changing rules 'runtime' is only practical if mods don't use modsupport.db to write data while server is running. As far as I know, no public ones do. If it turns out that modsupport is very much read/write at some point down the line, I'll probably just move the data to a separate SQLite database.
[] I do *not* load loot-rules into memory when server starts, this to allow for rule modification when server is running. Provided you don't have thousands of loot rules this will have no negative effect on your server.
[] I load loot table and item contents into memory when you start your server (for performance reasons). This is why loot tables and items must exist when server starts. You can still add rules/items when server is running, but they will not go into effect until after restart. I have considered adding a 'reload loot tables' action in the past, but never had the need so never got around to it.
[] If in doubt what the performance hit is on your server, enable logging of costs in the configuration.
[] There are no foreign key constraints; if your rule points to a loot table that does not exist you will not get any warnings, so make sure you have your IDs correct when you do your inserts :)
[] I consider the mod largely done until I run into something I need, but will fix bugs.
[] If you are testing loot rules as GM, remember to set yourself vulnerable or your kills will not trigger any loot drop.
[] A tip to insert new loot tables, use "select max", e.g.: insert into friyaloottables values((select max(tableid)+1 from friyaloottables), 694);
[] Clarification of the above insert: I know for instance that 694 is an adamantine lump (both in LootItems table and in Wurm). If you did not change it after installing the mod it should have been imported with a 25% drop chance.
[] Set sqlLogging to true in the configuration will make it easier to test your loot rules as the queries will be output in your server log. Note, though, that this can have a mild negative impact on performance of your server.
*/

/*
insert into friyalootrules(rulename, loottable, creature, maxaltitude) values("Lowland Nogump Slaying", 1358, "son of nogump", 200);


*/

/*
Early on in your log you should have:
INFO com.friya.wurmonline.server.loot.Mod: Loaded LootTable configuration

Three tables are created when you first install LootTables, if you have logging set to INFO or finer, on your first run you should see:
INFO com.friya.wurmonline.server.loot.LootSystem: Created FriyaLootRules
INFO com.friya.wurmonline.server.loot.LootSystem: Created FriyaLootTables
INFO com.friya.wurmonline.server.loot.LootSystem: Created FriyaLootItems

...the followed by a lot of statements like this:
INFO com.friya.wurmonline.server.loot.LootSystem: Importing: 1296, // 'lunchbox'
INFO com.friya.wurmonline.server.loot.LootSystem: Executing: INSERT INTO FriyaLootItems(id, itemids, name, material, startql, endql, canberare, dropchance, creator, decaytime, damage) VALUES(1296,'1296','lunchbox',-1,10.0,60.0,1,25.0,'Secret Santa',172800,90.0)
...

The tables are created in sqlite/modsupport.db. If tables and data was already created (subsequent runs) you will only see that it loaded configuration.

I just verified that it works (I dropped all tables and they were correctly recreated). Make sure the mod is correctly installed by checking the log.

If you copy files over with FTP or so, it might be a permission problem. Make sure that Wurm can write to the file. But I'd imagine you'd have a lot of crying in your server log if that was the case.

 */

/*
I had a bit of spare time to look at this on this lovely Sunday.

NOTE: This patch will perform changes to the database, it will automatically migrate the first time you start your server.

Changes to item customization in database:
- added ability to set how many clones/copies (can be random) should drop
- ability to enchant loot-items

That is, in addition to what was already in place:
- ql + randomness
- can-be-rare
- creator
- auxdata
- decaytime
- damage
- custom method (used by mods)
- custom argument (used by mods)

If you hook in with your own mod, you can do whatever you want with the items in onBeforeDrop().


You can test both these changes with the following INSERTS:
5 huge axes with enchants



Elaboration on enchants:

Set 'enchants' column to e.g: "coc woa nimb ms fosdemise" and Circle of Cunning, Wind of Ages, Nimbleness, Mind Stealer and Fo's Demise will be applied to the item.
Set 'enchantstrength' to 10 and all enchants will have that strength.
Given strength is set to 10, set 'enchantstrengthrandom' to 90 and all enchants will have a random strength between 10 and 100.

Abbreviations of enchants

	Enchants with strength
	- aosp          Aura of shared pain
	- botd          Blessing of the Dark
	- bt            Bloodthirst
	- coc           Circle of Cunning
	- courier       Courier
	- dm            Dark messenger
	- fa            Flaming aura
	- fb            Frostbrand
	- lt            Life Transfer
	- litdark       Lurker in the Dark
	- litdeep       Lurker in the Deep
	- litwoods      Lurker in the Woods
	- ms            Mind stealer
	- nimb          Nimbleness
	- nolo          Nolocate
	- opulence      Opulence
	- rt            Rotting Touch
	- venom         Venom
	- wa            Web Armour
	- woa           Wind of Ages

	Boolean enchants (strength is ignored)
	- animalsdemise
	- dragonsdemise 
	- humansdemise 
	- selfhealersdemise

	- fosdemise
	- libilasdemise
	- vynorasdemise
	- magranonssdemise

	- foscounter
	- libilascounter
	- vynorascounter
	- magranonscounter

Note:
	If you have non-standard spells on your server or want to add runes or imbues, you can also put in the ID of the spell in 'enchants'.
*/


/**
 * 
 * @author Friya
 */
public class Mod implements WurmServerMod, Initable, Configurable, ServerStartedListener, PlayerMessageListener
{
	private static Logger logger = Logger.getLogger(Mod.class.getName());

	static boolean enabled = true;
	static boolean everythingDropsLoot = false;
	static boolean playersDropLoot = false;
	static boolean createDefaultRules = false;
	static boolean deleteAllRules = false;
	static boolean logExecutionCost = false;
	static boolean sqlLogging = false;

	static boolean simulationOnStartup = false;
	static int simulateRule = 8;
	static int simulateAmount = 10000;

	public void configure(Properties properties)
	{
		Mod.enabled = Boolean.valueOf(properties.getProperty("enabled", String.valueOf(Mod.enabled))).booleanValue();
		Mod.everythingDropsLoot = Boolean.valueOf(properties.getProperty("everythingDropsLoot", String.valueOf(Mod.everythingDropsLoot))).booleanValue();
		Mod.playersDropLoot = Boolean.valueOf(properties.getProperty("playersDropLoot", String.valueOf(Mod.playersDropLoot))).booleanValue();
		Mod.createDefaultRules = Boolean.valueOf(properties.getProperty("createDefaultRules", String.valueOf(Mod.createDefaultRules))).booleanValue();
		Mod.deleteAllRules = Boolean.valueOf(properties.getProperty("deleteAllRules", String.valueOf(Mod.deleteAllRules))).booleanValue();
		Mod.logExecutionCost = Boolean.valueOf(properties.getProperty("logExecutionCost", String.valueOf(Mod.logExecutionCost))).booleanValue();
		Mod.sqlLogging = Boolean.valueOf(properties.getProperty("sqlLogging", String.valueOf(Mod.sqlLogging))).booleanValue();
		
		logger.info("Loaded LootTable configuration");
	}


	public void init()
	{
		if(!enabled) {
			logger.log(Level.WARNING, "Loot tables are DISABLED through configuration flag 'enabled'");
			return;
		}
		
		Stats.reset();
		setupInterceptDie();
	}

	
	private void setupInterceptDie()
	{
		String descriptor;
		try {
			descriptor = Descriptor.ofMethod(CtPrimitiveType.voidType, new CtClass[] { CtPrimitiveType.booleanType, HookManager.getInstance().getClassPool().get("java.lang.String") });
		} catch (NotFoundException e) {
			throw new RuntimeException("Failed to find String class -- this is hilarious!");
		}
		
		HookManager.getInstance().registerHook("com.wurmonline.server.creatures.Creature", "die", descriptor, new InvocationHandlerFactory()
		{
			@Override
			public InvocationHandler createInvocationHandler() {
				return new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						DecimalFormat df = null;
						long start = 0;

						if(Mod.logExecutionCost) {
							df = new DecimalFormat("#.#########");
							start = System.nanoTime();
						}

						interceptDie(proxy, args);

						if(Mod.logExecutionCost) {
							logger.log(Level.INFO, "LootTables interception spent " + df.format((System.nanoTime() - start) / 1000000000.0) + "s (this number include calls to " + LootSystem.getInstance().getListenerCount() + " mods that hook into LootTables)");
						}
						
						Object result = method.invoke(proxy, args);
						return result;
					}
				};
			}
		});
	}


	private void interceptDie(Object proxy, Object[] args)
	{
		try {
			Creature c = (Creature)proxy;

			if(c.isPlayer()) {
				Stats.inc("killed.players");
				Stats.inc("killed.player." + c.getName());
			} else {
				Stats.inc("killed.npcs");
				Stats.inc("killed.npc." + c.getTypeName());
			}
			
			// We want to allow other mods to listen to things dying.
			LootSystem ls = LootSystem.getInstance();
			LootResult lr = ls.getLootResult(c);
			if(ls.notifyLootListeners(lr) == false) {
				// Whoever says no to the dropping anything, we listen. This is no fucking democracy.
				return;
			}

			Item[] items = lr.getItems();
			
			// If you don't care about any fancy loot-results, skip the above and just get the items to drop
			//Item[] items = LootSystem.getInstance().getLoot(c);
	
			for(Item i : items) {
				c.getInventory().insertItem(i, true);
			}

			Stats.outputPeriodically();

		} catch(Exception e) {
			logger.log(Level.SEVERE, "some mod's onBeforeDrop() caused an exception, this is caught and thrown away to not prevent things from dying", e);
			Stats.inc("mods.exceptions");
		}
	}


	public boolean onPlayerMessage(Communicator c, String msg)
	{
		boolean intercepted = false;
		
		if(c.getPlayer() != null && c.getPlayer().getPower() > 0 && msg.startsWith("/lootstats")) {
			intercepted = true;
			Stats.output(c, false);
		}
		
		return intercepted;
	}
	

	public void onServerStarted() 
	{
		if(!enabled) {
			logger.log(Level.WARNING, "Loot tables are DISABLED through configuration flag 'enabled'");
			return;
		}

		if(simulationOnStartup) {
			logger.info("------------------------------------------------------------------------------------------------------");
			logger.info("                                      Starting simulation");
			logger.info("------------------------------------------------------------------------------------------------------");
			
			logger.info("Will trigger rule " + Mod.simulateRule + " exactly " + Mod.simulateAmount + " times");

			HashMap<String, Integer> dropAmount = new HashMap<String, Integer>();
			
			// Run simlation X times.
			for(int j = 0; j < Mod.simulateAmount; j++) {

				// This is the code that is used to trigger drops
				LootSystem ls = LootSystem.getInstance();
				LootResult lr = ls.getLootResult(null);
				Item[] items = lr.getItems();
	
				for(Item i : items) {
					String name = "template: " + i.getTemplateId() + " name: " + i.getName();
					if(dropAmount.containsKey(name)) {
						dropAmount.put(name, dropAmount.get(name) + 1);
					} else {
						dropAmount.put(name, 1);
					}

					// Make SURE we destroy items.
					Items.destroyItem(i.getWurmId());
				}
				
			}
			
			// Output result
		    @SuppressWarnings("rawtypes")
			Iterator it = dropAmount.entrySet().iterator();
		    while (it.hasNext()) {
		    	Map.Entry<String, Integer> pair = (Map.Entry<String, Integer>)it.next();
		    	logger.info(((float)((float)pair.getValue() / (float)simulateAmount) * 100) + "%\t" + pair.getKey() + " dropped " + pair.getValue() + " times" );
		    }
		    //logger.info("" + dropAmount);
			
			logger.info("------------------------------------------------------------------------------------------------------");
			logger.info("                                        Simulation done");
			logger.info("------------------------------------------------------------------------------------------------------");

			// MUST set this to false after simulation is done.
			simulationOnStartup = false;
		}

		if(deleteAllRules) {
			LootSystem.deleteAllLootRules();
		}
		
		if(createDefaultRules) {
			createLootRules();
		}
	}


	static void createLootRules() 
	{
		String ruleName;
		LootSystem ls = LootSystem.getInstance();

		ruleName = "[all NPCs] Rare stuff";
		ls.deleteRuleAndItsLootTable(ruleName);
		
		if(ls.hasLootRule(ruleName) == false) {
			// Most simple rule that can be done: Every NPC.
			LootRule lr = new LootRule(ruleName);

			// We treat Wurm's item IDs as strings so that we can support multiple IDs per SQL statement
			// for those who don't go near code. The drop chance (1%) is per item, we divide with number 
			// of items in this collection. Actually, this loot table shrunk over time and it is now a 
			// lot less than 100 items, but seeing it as a drop-rate reduction.
			LootItem[] li = new LootItem[] {
				new LootItem(
					"370,443,465,489,509,515,600,666,667,"
				  + "668,700,738,781,834,836,844,867,967,972," 
				  + "973,974,975,976,977,978,979,980,1014,1015,1032,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,"
				  + "1060,1061,1062,1063,1064,1065,1066,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,"
				  + "1090,1092,1093,1094,1095,1099",
					(1.0/100.0)
				)
			};

			ls.addLootRule(lr, li);
		}
	}
}
