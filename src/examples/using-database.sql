-- ****************************************************************************
-- ***                                                                      ***
-- ***                         What is this?                                ***
-- ***                                                                      ***
-- ****************************************************************************
--
-- NOTE:
-- This file is not obsolete, but it lacks explanation of a lot of features
-- that were added at a later date. To read up on later additions, see:
-- https://forum.wurmonline.com/index.php?/topic/151741-released-server-mod-loot-tables/
--
-- This file is an example on how to create loot rules and loot tables that 
-- define what NPC's should drop, using just the database. This is handy if 
-- you are not into programming (or modding Wurm).
--
-- You can actually paste this entire file into your favorite SQLite database 
-- administration tool. You can of course just edit your database manually 
-- too, but at the very least, you probably want to examine the examples 
-- below to get a bit of a grasp.
--
-- The database you want to work with is in your World's sqlite folder: 
-- modsupport.db, after having installed this mod and started the server 
-- once, the tables will have been created in there.
--
-- Now, if you want to get your hands dirty or ...
-- If you want to go near code, you can just use the API from your own
-- mods. If you go this route you will probably be interested in the
-- LootResult object as that will enable you to create rule-based quest 
-- triggers.
-- 
--                                 -- Friya, 2016-2017, Discord: Friya#7934
--
-- ****************************************************************************
-- ***                                                                      ***
-- ***                        Explain yourself...                           ***
-- ***                                                                      ***
-- ****************************************************************************
-- You want NPC's to drop loot. But you want to be able to specify who drops
-- what and when.
--
-- So we have:
-- []  A loot-rule, this says what criteria an NPC has to fulfill to qualify,
--     it also enforces a maximum number of items this creature can drop.
--
-- []  A loot-table, this is simply put just a collection of items that a
--     rule will yield. Say, a 25% chance that a knife or a fork will drop
--     when this venerable wolf on the mountain top dies.
-- 
-- []  A loot-item, this is a "template" to create Wurm items, it specifies
--     what quality, if it has a chance to be rare, how damaged the item is,
--     etc, etc. In addition to this, it is also the place that decides how
--     big chance the item has to drop (i.e. it has a chance to drop 25%
--     of the times this rule was triggered; that is, creature died.
--
-- []  A loot-set is more an abstract 'thing', but it is a collection of 
--     loot-rules/tables. That is, any creature that die may qualify for 
--     many loot-rules and this could yield many different loot-tables. For 
--     instance, you may have a loot-rule for creatures that die in the 
--     night and another one for creatures that die in the winter -- these 
--     two will overlap and at the right time, both loot-tables are valid 
--     and at other times none. This is a loot-set; a collection of rules
--     and tables.
--
-- Loot rules can be modified and added while server is running. Adding new 
-- loot-tables and loot-items require a server restart.
--
-- Multiple loot-rules can match a kill, all items in all loot-tables will 
-- then be rolled between.
-- 
-- You can specify a maximum number of items a loot-rule should be able to 
-- give, if many loot-rules match this kill, the LOWEST max number of 
-- items will be picked (so we will err on the cheap side, so to speak).
--
-- If an item has 100% drop-chance, it will always drop from the NPC even
-- if it would exceed the maximum number of items it can drop. So you 
-- probably want to keep too broad 100% drops at a minimum or you may 
-- rarely see random items drop. There is really little reason to create
-- 100% drop-chance of something for all creatures in the game, though.
--

-- ****************************************************************************
-- ***                                                                      ***
-- *** 1. Create a loot-table                                               ***
-- ***                                                                      ***
-- ***      Let's create a loot-table (ID 1) that has two items in it       ***
-- ***                                                                      ***
-- ****************************************************************************
INSERT INTO FriyaLootTables(tableid, lootid) VALUES(1, 258);		-- this is a 'knife'
INSERT INTO FriyaLootTables(tableid, lootid) VALUES(1, 259);		-- this is a 'fork'

-- NOTE: The 'lootid' above will not always correspond to a 'wurm template id' 
-- since you are able create your own items and configurations of them.
--
-- However, for convenience, all available items in Wurm were imported
-- with somewhat sane configurations when you first started up the LootSystem.
-- All these template IDs correspond to the actual Wurm template ID. Nice eh?


-- ****************************************************************************
-- ***                                                                      ***
-- *** 2. The rule for dropping the knife and fork                          ***
-- ***                                                                      ***
-- ***   Let's specify which NPCs should drop the loot in our loot-table    ***
-- ***                                                                      ***
-- ****************************************************************************
INSERT INTO FriyaLootRules
	(loottable, creature) VALUES
	(
		1,				-- For this rule, use the loot-table we created above
		'goblin'		-- Goblins, they should drop knife and fork
	);
INSERT INTO FriyaLootRules
	(loottable, rulename, age) VALUES
	(
		1,				-- For this rule, use the loot-table we created above
		'Example 2',	-- Just a name we can identify with, not mandatory
		'venerable'		-- All venerable creatures should drop knife and fork
	);


-- ****************************************************************************
-- ***                                                                      ***
-- ***                      ...well, that was dull!                         ***
-- ***                                                                      ***
-- *** 3. More exciting rules                                               ***
-- ***                                                                      ***
-- ***             Now some more, eh, exciting (?) loot rules...            ***
-- ***                                                                      ***
-- ****************************************************************************
--
-- Ever wanted to make really specific creatures always drop something? Well, me 
-- neither ... but I could for instance say:
--
-- Elderly female champion wolves killed on the snowy mountain tops on them cold 
-- winter nights when the wind is coming in hard from the north.
--
-- ...they should drop a knife and a fork.
--
-- So:
--
INSERT INTO FriyaLootRules
	(
	 loottable, rulename, creature, age, type, gender, starthour, endhour, 
	 minaltitude, season, windstrength, winddirection, tiletype
	)
	VALUES(
		1,									-- The loot-table we want to use (we created loot-table 1 above)
		"Wolf killing on chilly nights",	-- A name of the rule that we can identify with
		"wolf",								-- creature type
		"venerable",						-- age
		"champion",							-- type
		1,									-- gender: 1 = female
		22,									-- start-hour: 10 PM
		05,									-- end-hour: 5 AM
		5000,								-- minimum altitude
		"winter",							-- season
		"gale",								-- wind strength
		"north",							-- wind direction
		30									-- tiletype; snow
	);


-- ****************************************************************************
-- ***                                                                      ***
-- *** 4. Custom items                                                      ***
-- ***                                                                      ***
-- ***  NOTE: A "loot ID" is NOT the same as a "Wurm template ID",          ***
-- ***        as a convenience, I have just chosen to import all Wurm       ***
-- ***        templates when you first installed the LootSystem.            ***
-- ***                                                                      ***
-- ***  We can add our own specific items to loot-tables, custom items      *** 
-- ***  or items with special configurations. This is done below.           ***
-- ***                                                                      ***
-- ****************************************************************************
-- This is a custom item called ancient amulet in my case, 1% drop-chance.
INSERT INTO FriyaLootItems
	(id, itemids, dropchance) VALUES(
		100001,					-- the LOOT id of this item
		'22763',				-- the actual Wurm ID of the item
		1						-- 1% drop chance
	);

-- Let's create a new loot-table for this amulet
INSERT INTO FriyaLootTables
	(tableid, lootid) VALUES
	(
		2,						-- loot-table 2
		100001					-- the id of the amulet we added to loot-items
	);

-- ... we could add more items to LootTable 2 here...


-- Let's create a rule that makes this item drop from EVERY NPC in the game
-- but of course, at a 1% chance.
INSERT INTO FriyaLootRules
	(loottable, rulename) VALUES
	(
		2,				-- For this rule, use the loot-table we created above
		'Ancient Amulet Global drop'
						-- Note the complete lack of ANY rules. This means
						-- every NPC in the game will be able to drop the
						-- items specified by this loot-table.
	);

/*
-- ****************************************************************************
-- ***                                                                      ***
-- *** 5. The options of rules                                              ***
-- ***                                                                      ***
-- *** A lot is possible, below are all the options to customize loot rules ***
-- ***                                                                      ***
-- ****************************************************************************

	rulename			Just a rule name so you can easily identify it
	loottable			point to a loot-table which then should link to items
	
	maxloot				-1 = no limit, otherwise number of items a creature can drop max	
	
	creature			* for any NPC, or e.g. 'goblin'
	age					* for any, or e.g. 'venerable'
	type				* for any, or e.g. 'champion'
	
	gender				-1 = any, 0 = male, 1 = female
	wild				-1 = any, 0 = must be bred in captivity, 1 = must be wild
	
	surface				-1 = any, 1 = surface, 2 = cave
	kingdom				is in land of: -1 = any, 1 = JK, 2 = MR, 3 = HOTS, 4 = Freedom
	tiletype			-1 = any, for specific see com.wurmonline.mesh.Tiles.TILE_*
	zonename			* = any, enter zone name for specific
	
	starthour			-1 = any, e.g. 00 = midnight
	endhour				-1 = any, 23 = 11pm
	
	minaltitude			-1 = any, 0+ = above water
	maxaltitude			-1 = any, e.g. 1000 = 1k dirt above sea
	
	fat					* for any, or starving or fat
	diseased			-1 = any, or 0 = not diseased, 1 = diseased
	
	isunique			-1 any, 0 = no, 1 = yes (this is a shortcut so you don't have to specify each unique in the game)
	isfromkingdom		is member of: -1 any, , 1 = JK, 2 = MR, 3 = HOTS, 4 = Freedom
	ishumanoid			-1 any, 0 = no, 1 = yes
	iszombie			-1 any, 0 = no, 1 = yes
	isfromrift			-1 any, 0 = no, 1 = yes
	
	minslope			-1 = any, or any number that describes min. slope (e.g. 40)
	maxslope			-1 = any, or any number that describes max. slope (e.g. 100)
	
	minxpos				-1 any, or a start coordinate. Say, you only want creatures living at the center of the map.
	minypos				see above
	maxxpos				-1 any, or an end coordinate
	maxypos				see above
	
	weather				* for any or 'precipitation' (snow/rain), 'clear' (sun), 'fog' or 'overcast' (cloudy)
	windstrength		* for any or gale, strong wind, strong breeze, breeze, light breeze
	winddirection		* for any or north, northwest, west, southwest, south, southeast, east, northeast
	season				* for any, spring, summer, autumn or winter
	
	deityinfluence		* for any, vynora, fo, ...
	neardeed			-1 any, 0 = must not be near deed, 1 = must be near deed
	neartower			-1 any, 0 = must not be near tower, 1 = must be near tower
*/

/*
-- ****************************************************************************
-- ***                                                                      ***
-- *** 6. The options for items (subject to further additions)              ***
-- ***                                                                      ***
-- ****************************************************************************

	int lootId;
	int[] wurmItemIds;			-- each loot-item can actually be many wurm-items (to save repetition)
	String name;
	byte material;
	float startQl;
	float endQl;
	boolean canBeRare;
	float dropChance;
	String creator;
	int auxData;
	long decayTime;
	float damage;

	String customMethod;		-- for callback when the item gets triggered
	String customArgument;

	int cloneCount;				-- number of clones/copies that should be created
	int cloneCountRandom;		-- randomness to the above

	String enchants;			-- enchants to be applied to the item
	int enchantStrength;		-- the strength of the enchants
	int enchantStrengthRandom;	-- how much randomness should be applied to the enchant strength

*/

/*
-- ****************************************************************************
-- ***                                                                      ***
-- *** 7. Enchants                                                          ***
-- ***                                                                      ***
-- ****************************************************************************
--
-- set 'enchants' to e.g: "coc woa nimb ms fosdemise" and Circle of Cunning,
-- Wind of Ages, Nimbleness, Mind Stealer and Fo's Demise will be applied to 
-- the item.
--
-- Set 'enchantstrength' to 10 and all enchants will have that strength.
-- Set 'enchantstrengthrandom' to 90 and all enchants will have a random 
-- strength between 10 and 100.
--
-- Abbreviations of enchants:
--
-- Enchants with strength
-- **********************
-- aosp          Aura of shared pain
-- botd          Blessing of the Dark
-- bt            Bloodthirst
-- coc           Circle of Cunning
-- courier       Courier
-- dm            Dark messenger
-- fa            Flaming aura
-- fb            Frostbrand
-- lt            Life Transfer
-- litdark       Lurker in the Dark
-- litdeep       Lurker in the Deep
-- litwoods      Lurker in the Woods
-- ms            Mind stealer
-- nimb          Nimbleness
-- nolo          Nolocate
-- opulence      Opulence
-- rt            Rotting Touch
-- venom         Venom
-- wa            Web Armour
-- woa           Wind of Ages
--
-- Boolean enchants
-- ****************
-- animalsdemise
-- dragonsdemise 
-- humansdemise 
-- selfhealersdemise
--
-- fosdemise
-- libilasdemise
-- vynorasdemise
-- magranonssdemise
--
-- foscounter
-- libilascounter
-- vynorascounter
-- magranonscounter
--


/*
-- ****************************************************************************
-- ***                                                                      ***
-- *** 8. Some queries (or a query for now) that might be handy...          ***
-- ***                                                                      ***
-- ****************************************************************************

-- Query to see which rule will yield which loot-table and which items 
SELECT lr.id AS LootRuleID, lr.rulename AS LootRuleName, lt.tableid AS LootTableID, lt.lootid AS LootItemID, li.itemids AS LootItemItems
	FROM FriyaLootRules AS lr
	INNER JOIN FriyaLootTables AS lt ON (lr.loottable = lt.tableid)
	INNER JOIN FriyaLootItems AS li ON (lt.lootid = li.id);
*/
