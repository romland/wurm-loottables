package com.friya.wurmonline.server.loot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Items;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.TimeConstants;
import com.wurmonline.server.WurmCalendar;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemTemplate;
import com.wurmonline.server.items.ItemTemplateFactory;
import com.wurmonline.server.players.Player;
import com.wurmonline.server.spells.Spell;
import com.wurmonline.server.zones.FaithZone;
import com.wurmonline.server.zones.FocusZone;
import com.wurmonline.server.zones.NoSuchZoneException;
import com.wurmonline.server.zones.Zones;
import com.wurmonline.shared.constants.Enchants;

/**
 * The LootSystem handles drops for various creatures.
 * 
 * @author Friya
 */
final public class LootSystem implements ExcludedItems, Enchants
{
	private static LootSystem instance;
	private static Logger logger = Logger.getLogger(LootSystem.class.getName());
	static HashMap<Integer, LootItem> lootItems = new HashMap<Integer, LootItem>();
	static HashMap<Integer, LootTable> lootTables = new HashMap<Integer, LootTable>();

	HashMap<String, Byte> enchantAbbreviations = new HashMap<String, Byte>();
	Spell[] spells;
	
	private List<BeforeDropListener> beforeDropListeners = new ArrayList<BeforeDropListener>();

	static String lootItemInsert = "INSERT INTO FriyaLootItems(id, itemids, name, material, startql, endql, canberare, dropchance, creator, decaytime, damage) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

	static String lootRuleQuery = "SELECT id, loottable, maxloot, rulename "
			+ "FROM FriyaLootRules "
			+ "WHERE "
			+ "\n		(creature = '*' OR creature = ?) "					// 1
			+ "\n		AND (age = '*' OR age = ?) "						// 2
			+ "\n		AND (type = '*' OR type = ?) "						// 3
			+ "\n		AND (gender = -1 OR gender = ?) "					// 4
			+ "\n		AND (wild = -1 OR wild = ?) "						// 5
			+ "\n		AND (surface = -1 OR surface = ?) "					// 6
			+ "\n		AND (kingdom = -1 OR kingdom = ?) "					// 7
			+ "\n		AND (tiletype = -1 OR tiletype = ?) "				// 8
			+ "\n		AND (zonename = '*' OR zonename = ?) "				// 9
			+ "\n		AND (starthour = -1 OR starthour >= ?) "			// 10
			+ "\n		AND (endhour = -1 OR endhour <= ?) "				// 11
			+ "\n		AND (minaltitude = -1 OR (minaltitude <= ?)) "		// 12
			+ "\n		AND (maxaltitude = -1 OR (maxaltitude >= ?)) "		// 13
			+ "\n		AND (fat = '*' OR fat = ?) "						// 14
			+ "\n		AND (diseased = -1 OR diseased = ?) "				// 15
			+ "\n		AND (isunique = -1 OR isunique = ?) "				// 16
			+ "\n		AND (isfromkingdom = -1 OR isfromkingdom = ?) "		// 17
			+ "\n		AND (ishumanoid = -1 OR ishumanoid = ?) "			// 18
			+ "\n		AND (iszombie = -1 OR iszombie = ?) "				// 19
			+ "\n		AND (isfromrift = -1 OR isfromrift = ?) "			// 20
			+ "\n		AND (minslope = -1 OR (minslope >= ?)) "			// 21
			+ "\n		AND (maxslope = -1 OR (maxslope <= ?)) "			// 22
			+ "\n		AND (minxpos = -1 OR (minxpos >= ?)) "				// 23
			+ "\n		AND (minypos = -1 OR (minypos <= ?)) "				// 24
			+ "\n		AND (maxxpos = -1 OR (maxxpos >= ?)) "				// 25
			+ "\n		AND (maxypos = -1 OR (maxypos <= ?)) "				// 26
			+ "\n		AND (weather = '*' OR weather = ?) "				// 27
			+ "\n		AND (windstrength = '*' OR windstrength = ?) "		// 28
			+ "\n		AND (winddirection = '*' OR winddirection = ?) "	// 29
			+ "\n		AND (season = '*' OR season = ?) "					// 30
			+ "\n		AND (deityinfluence = '*' OR deityinfluence = ?) "	// 31
			+ "\n		AND (neardeed = -1 OR neardeed = ?) "				// 32
			+ "\n		AND (neartower = -1 OR neartower = ?) "				// 33
			+ "\n		AND enabled = 1"									// -- (still 33)
			;

	static String lootRuleQuerySimulation = "SELECT id, loottable, maxloot, rulename "
			+ "FROM FriyaLootRules "
			+ "WHERE id = ?";
	
	
	public static LootSystem getInstance()
	{
		if(instance == null) {
			instance = new LootSystem();
		}

		return instance; 
	}


    public void listen(BeforeDropListener listener)
    {
        beforeDropListeners.add(listener);
    }
    

    public int getListenerCount()
    {
    	return beforeDropListeners.size();
    }


    /**
     * Any exceptions should be caught upstream by mod-loader.
     * 
     * @param lr
     * @return
     */
    boolean notifyLootListeners(LootResult lr)
    {
    	boolean shouldDropLoot = true;

    	for(BeforeDropListener ll : beforeDropListeners) { 
    		if(ll != null && ll.onBeforeDrop(lr) == false) {
    			logger.log(Level.INFO, "LootListener " + ll.toString() + " is preventing dropping of loot");
    			shouldDropLoot = false;
    		}
    		Stats.inc("mods.calls");
    	}

    	// One of the listeners are preventing us from dropping loot, destroy the items.
    	if(shouldDropLoot == false) {
    		Stats.inc("mods.cancelled");
    		Item[] items = lr.getItems();
    		for(Item i : items) {
    			logger.log(Level.INFO, "Destroying: " + i);
    			Items.destroyItem(i.getWurmId());
    		}
    		lr.setItems(new Item[]{});
    	}

    	return shouldDropLoot;
    }


	private LootSystem()
	{
		if(createTables()) {
			// If we just created tables...
			// Insert all item templates that exist (with some exceptions, see ExcludedItems)
			importItemTemplates();
		}
		initEnchants();
	}


	private void initEnchants()
	{
		//this.spells = Spells.getSpellsEnchantingItems();
		
		enchantAbbreviations.put("aosp", BUFF_SHARED_PAIN);
		enchantAbbreviations.put("botd", BUFF_BLESSINGDARK);
		enchantAbbreviations.put("bt", BUFF_BLOODTHIRST);
		enchantAbbreviations.put("coc", BUFF_CIRCLE_CUNNING);
		enchantAbbreviations.put("courier", BUFF_COURIER);
		enchantAbbreviations.put("dm", BUFF_DARKMESSENGER);
		enchantAbbreviations.put("fa", BUFF_FLAMING_AURA);
		enchantAbbreviations.put("fb", BUFF_FROSTBRAND);
		enchantAbbreviations.put("lt", BUFF_LIFETRANSFER);
		enchantAbbreviations.put("litdark", BUFF_LURKERDARK);
		enchantAbbreviations.put("litdeep", BUFF_LURKERDEEP);
		enchantAbbreviations.put("litwoods", BUFF_LURKERWOODS);
		enchantAbbreviations.put("ms", BUFF_MINDSTEALER);
		enchantAbbreviations.put("nimb", BUFF_NIMBLENESS);
		enchantAbbreviations.put("nolo", CRET_NOLOCATE);
		enchantAbbreviations.put("opulence", BUFF_OPULENCE);
		enchantAbbreviations.put("rt", BUFF_ROTTING_TOUCH);
		enchantAbbreviations.put("venom", BUFF_VENOM);
		enchantAbbreviations.put("wa", BUFF_WEBARMOUR);
		enchantAbbreviations.put("woa", BUFF_WIND_OF_AGES);

		enchantAbbreviations.put("animalsdemise", ENCHANT_ANIMAL_HATE);
		enchantAbbreviations.put("dragonsdemise", ENCHANT_DRAGON_HATE);
		enchantAbbreviations.put("humansdemise", ENCHANT_HUMAN_HATE);
		enchantAbbreviations.put("selfhealersdemise", ENCHANT_REGENERATION_HATE);
		
		enchantAbbreviations.put("fosdemise", ENCHANT_FO_HATE);
		enchantAbbreviations.put("libilasdemise", ENCHANT_LIBILA_HATE);
		enchantAbbreviations.put("vynorasdemise", ENCHANT_VYNORA_HATE);
		enchantAbbreviations.put("magranonssdemise", ENCHANT_MAGRANON_HATE);

		enchantAbbreviations.put("foscounter", ENCHANT_FO_PROT);
		enchantAbbreviations.put("libilascounter", ENCHANT_LIBILA_PROT);
		enchantAbbreviations.put("vynorascounter", ENCHANT_VYNORA_PROT);
		enchantAbbreviations.put("magranonscounter", ENCHANT_MAGRANON_PROT);
	}
	
	/*
	private void testSqlStatementParser()
	{
		ArrayList<String> set = LootSystem.getStatementsFromBatch(loadString("loot.txt"));
		for(String s : set) {
			logger.log(Level.INFO, "*** STATEMENT ***: " + s);
		}
	}
	*/
	
	
	/**
	 * Imports all item templates into the database with a lootId set to the same as their
	 * templateId (item ID). 
	 * 
	 * This will only run if we just created the tables, so we will not overwrite anything.
	 */
	private void importItemTemplates()
	{
		logger.log(Level.INFO, "For convenience, importing all Item Templates as loot-drops with a corresponding loot-id");

		PreparedStatement ps;
		Connection con = ModSupportDb.getModSupportDb();
		ItemTemplate[] itemTemplates = ItemTemplateFactory.getInstance().getMostMaintenanceUpdated();
		int[] excludedItemIds = getExcludedItems();
		
		//logger.log(Level.INFO, "Excluded length: " + excludedItemIds.length);
		//logger.log(Level.INFO, Arrays.toString(excludedItemIds));
		
		try {
			// We bump this so we know that it's forcefully inserted.
			int currentImportedId;

			for(ItemTemplate tpl : itemTemplates) {
				//
				// This will likely cause some confusion when explaining how "loot ids" and "wurm item ids" work:
				//
				// When we import item-templates, we give them the same loot-id as their template-id, this will
				// inevitably stop being true as soon as you import your own specified loot-items. 
				//
				// But out of an example perspective, I think it is kind of nice to have all 'default' items
				// imported in one shape or another. I might change my mind regarding this in the future.
				//
				currentImportedId = tpl.getTemplateId();
				
				if(IntStream.of(excludedItemIds).anyMatch(x -> x == tpl.getTemplateId())) {
					logger.log(Level.INFO, " Skipping: " + tpl.getTemplateId() + ", // '" + tpl.getName() + "'");
					continue;
				} else {
					logger.log(Level.INFO, "Importing: " + tpl.getTemplateId() + ", // '" + tpl.getName() + "'");
				}
				
				if(Mod.sqlLogging) {
			    	ps = new LoggableStatement(con, lootItemInsert);
			    } else {
			    	ps = con.prepareStatement(lootItemInsert);
			    }
	
			    // FriyaLootItems(id, itemids, name, material, startql, endql, canberare, dropchance, creator, decaytime, damage) 

				ps.setInt(1, currentImportedId);
				ps.setString(2,	"" + tpl.getTemplateId());
				ps.setString(3, tpl.getName());
				ps.setInt(4, -1);
				ps.setFloat(5, 10.0f);
				ps.setFloat(6, 60.0f);
				ps.setByte(7, (byte)1);
				ps.setDouble(8, 25.0f);
				ps.setString(9, "Secret Santa");
				ps.setLong(10, TimeConstants.DECAYTIME_FOOD);
				ps.setFloat(11, 90.0f);

				if(Mod.sqlLogging) {
					logger.log(Level.INFO, "Executing: " + ((LoggableStatement)ps).getQueryString());
				}
				
				ps.execute();
			}

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to import item templates", e);
			throw new RuntimeException(e);
		}
	}
	

	public int[] getExcludedItems()
	{
		// adminItems, balanceItems, functionalItems, fluidItems, largeItems
		int[] a1 = concat(concat(adminItems, balanceItems), concat(functionalItems, fluidItems));
		return concat(a1, largeItems);
	}


	private int[] concat(int[] a, int[] b)
	{
	   int aLen = a.length;
	   int bLen = b.length;
	   int[] c = new int[aLen+bLen];
	   System.arraycopy(a, 0, c, 0, aLen);
	   System.arraycopy(b, 0, c, aLen, bLen);
	   return c;
	}


	public boolean deleteRuleAndItsLootTable(String ruleName)
	{
		logger.log(Level.FINE, "Deleting loot rule: " + ruleName);


		try {
			Connection con = ModSupportDb.getModSupportDb();
			PreparedStatement ps;
			ResultSet rs;

			String sql = "SELECT id, loottable FROM FriyaLootRules WHERE rulename = ?";
			ps = con.prepareStatement(sql);
			ps.setString(1, ruleName);
			rs = ps.executeQuery();

			HashSet<Integer> deleteTables = new HashSet<Integer>();
			HashSet<Integer> deleteRules = new HashSet<Integer>();
			
			while(rs.next()) {
				deleteTables.add(rs.getInt("loottable"));
				deleteRules.add(rs.getInt("id"));
			}

			rs.close();
			ps.close();
			
			Statement statement = con.createStatement();

			con.setAutoCommit(false);

			for(int r : deleteRules) { 
				logger.log(Level.INFO, "Deleting loot rule #" + r);
				statement.addBatch("DELETE FROM FriyaLootRules WHERE id = " + r);
			}

			for(int t : deleteTables) { 
				logger.log(Level.INFO, "Deleting loot table #" + t);
				statement.addBatch("DELETE FROM FriyaLootTables WHERE tableid = " + t);
			}

			statement.executeBatch();

			con.commit();

			rs.close();
			ps.close();

			return true;

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to delete Loot Rules");
			throw new RuntimeException(e);
		}
	}


	static public boolean deleteAllLootRules()
	{
		logger.log(Level.FINE, "Deleting all loot rules...");

		try {
			Connection con = ModSupportDb.getModSupportDb();
			Statement statement = con.createStatement();

			con.setAutoCommit(false);
			
			statement.addBatch("DELETE FROM FriyaLootRules");
			statement.addBatch("DELETE FROM FriyaLootTables");
			statement.addBatch("DELETE FROM FriyaLootItems");

			statement.executeBatch();

			con.commit();

			return true;

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to delete Loot Rules");
			throw new RuntimeException(e);
		}
	}
	
	
	private boolean insertStatements(ArrayList<String> sqlStatements)
	{
		String currentSqlStatement = null;

		try {
			Connection con = ModSupportDb.getModSupportDb();
			Statement statement = con.createStatement();

			con.setAutoCommit(false);
			for(String sql : sqlStatements) {
				logger.log(Level.FINE, "Adding statement: " + sql);
				currentSqlStatement = sql;
				statement.addBatch(sql);
			}

			statement.executeBatch();
			con.commit();

			return true;

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to run Loot Rules batch at statement: " + currentSqlStatement);
			throw new RuntimeException(e);
		}
	}


	public boolean addLootRules(String sqlBatch)
	{
		ArrayList<String> sqlStatements = getStatementsFromBatch(sqlBatch);
		
		return insertStatements(sqlStatements);
	}


	public boolean hasLootRule(String ruleName)
	{
		int ret = 0;

		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps;
		ResultSet rs;
		String sql = "SELECT COUNT(*) AS cnt FROM FriyaLootRules WHERE rulename = ?";

		try {
			ps = con.prepareStatement(sql);
			ps.setString(1, ruleName);
			rs = ps.executeQuery();

			if(rs.next()) {
				ret = rs.getInt(1);
				ps.close();
			} else {
				ps.close();
				throw new RuntimeException("Could not query FriyaLootRules");
			}

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Not good", e);
			throw new RuntimeException("Could not get from resulset");
		}
		
		logger.log(Level.FINE, "hasRule(" + ruleName + "): " + ret);
		return ret > 0;
	}


	public boolean addLootRule(LootRule lootRule, LootItem[] lootItems)
	{
		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps;
		
		int newLootTableId = getNewLootTableId();
		
		for(LootItem li : lootItems) {
			li.save(con);	// This will also give them ID's

			String sql = "INSERT INTO FriyaLootTables(tableid, lootid) VALUES(" + newLootTableId + ", " + li.getId() + ")";
			try {
				ps = con.prepareStatement(sql);
				ps.execute();
				ps.close();

			} catch (SQLException e) {
				logger.log(Level.SEVERE, "Could not insert new loot-table row", e);
				throw new RuntimeException("Halting");
			}
		}
		
		// Insert the actual rule pointing to the loot-able and items we created above.
		lootRule.setLootTable(newLootTableId);
		int lootRuleId = lootRule.save(con);
		
		logger.log(Level.FINE, "New LootRule id: " + lootRuleId);
		
		return true;
	}


	private int getNewLootTableId()
	{
		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps;
		ResultSet rs;
		String sql = "SELECT MAX(tableid)+1 AS nextid FROM FriyaLootTables";

		try {
			ps = con.prepareStatement(sql);
			rs = ps.executeQuery();

			if(rs.next()) {
				int ret = rs.getInt(1);
				ps.close();
				return ret;

			} else {
				ps.close();
				throw new RuntimeException("Could not get new loot-table ID");
			}

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Not good", e);
			throw new RuntimeException("Could not get from resulset");
		}
	}


	/**
	 * Will simply return the items that should be dropped (MUST drop or be destroyed).
	 * 
	 * @param c
	 * @return
	 */
	public Item[] getLoot(Creature c)
	{
		if(canDropLoot(c) == false) {
			return new Item[]{};
		}

		LootSet ls = getLootSet(c);
		return ls.getLoot();
	}


	/**
	 * This will return a LootResult containing the rules which were triggered when Creature c died,
	 * as well as the items that should be dropped. 
	 * 
	 * IMPORTANT: The items have already been created, so if you decide to not distribute them in 
	 * the game you MUST iterate over them and destroy them.
	 * 
	 * @param c
	 * @return
	 */
	LootResult getLootResult(Creature c)
	{
		LootResult lr = new LootResult();

		if(Mod.simulationOnStartup || canDropLoot(c) == true) {
			LootSet ls = getLootSet(c);
			Item[] loot = ls.getLoot();
			lr.setLootCap(ls.maxNumLoot);
			lr.setLootRules(ls.getLootRules());
			lr.setItems(loot);
		} else {
			Stats.inc("killed.ignored");
		}

		lr.setCreature(c);
		return lr;
	}


	public boolean canDropLoot(Creature c)
	{
		if(c.isSuiciding()) {
			logger.log(Level.FINE, "canDropLoot() No loot. Creature is suiciding");
			return false;
		}

		if(Mod.everythingDropsLoot == false && (c.getLatestAttackers() == null || c.getLatestAttackers().length == 0 || hasHumanAttacker(c) == false)) {
			logger.log(Level.FINE, "canDropLoot() No loot. Creature did not have any valid attacker");
			return false;
		}

		if(Mod.playersDropLoot == false && c instanceof Player) {
			logger.log(Level.FINE, "canDropLoot() No loot. Creature was a player");
			return false;
		}

		return true;
	}


	/**
	 * This filtering is also done in LootResult when setting attackers, but we may not have a LootResult yet.
	 * 
	 * @param c
	 * @return
	 */
	private boolean hasHumanAttacker(Creature c)
	{
		long[] attackerIds = c.getLatestAttackers();

		if(attackerIds == null || attackerIds.length == 0) {
			return false;
		}

		for(long id : attackerIds) {
			if(Players.getInstance().getPlayerOrNull(id) != null) {
				return true;
			}
		}

		return false;
	}


	private LootSet getLootSet(Creature c)
	{
		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps;
		ResultSet rs;
		LootSet lootSet;
		
		try {
		    if(Mod.sqlLogging) {
		    	ps = new LoggableStatement(con, Mod.simulationOnStartup ? lootRuleQuerySimulation : lootRuleQuery);
		    } else {
		    	ps = con.prepareStatement(Mod.simulationOnStartup ? lootRuleQuerySimulation : lootRuleQuery);
		    }

			if(Mod.simulationOnStartup) {
				ps.setInt(1,		Mod.simulateRule);
			} else {
				FocusZone fs = getFocusZone(c);
				int slope = (int)(c.getMovementScheme().getTileSteepness(c.getTileX(), c.getTileY(), c.getLayer()));
				String[] windInfo = getWindInfo();
				String season = getSeason();
				String weather = getWeather();
				String rulingDeity = getRulingDeity(c);

				ps.setString(1,		c.getNameWithoutPrefixes().toLowerCase());
				ps.setString(2,		c.getStatus().getAgeString());
				ps.setString(3,		getTypeString(c));
				ps.setByte(4,		c.getSex());
				ps.setInt(5,		(c.isBred() ? 0 : 1));
				ps.setInt(6,		(c.isOnSurface() ? 1 : 2));
				ps.setByte(7,		Zones.getKingdom(c.getTileX(), c.getTileY()));
				ps.setInt(8,		Tiles.decodeType((int)Zones.getMesh(c.isOnSurface()).getTile(c.getTileX(), c.getTileY())));
				ps.setString(9,		(fs != null ? fs.getName() : "*"));
				ps.setInt(10,		WurmCalendar.getHour());
				ps.setInt(11,		WurmCalendar.getHour());
				ps.setInt(12,		c.getPosZDirts());
				ps.setInt(13,		c.getPosZDirts());
	
				ps.setString(14,	getFatString(c));
				ps.setInt(15,		(c.getDisease() == 0 ? 0 : 1));
				ps.setInt(16,		(c.isUnique() ? 1 : 0));
				ps.setInt(17,		c.getKingdomId());
				ps.setInt(18,		(c.isHuman() ? 1 : 0));				// TODO: this might need refining, human != humanoid (we can get humanoid from model-name)
				ps.setInt(19,		(c.isReborn() ? 1 : 0));
				ps.setInt(20,		(c.getTemplate().isRiftCreature() ? 1 : 0));
				
				ps.setInt(21,		slope);
				ps.setInt(22,		slope);
				
				ps.setInt(23,		c.getTileX());
				ps.setInt(24,		c.getTileY());
				ps.setInt(25,		c.getTileX());
				ps.setInt(26,		c.getTileY());
	
				ps.setString(27,	weather);
				ps.setString(28,	windInfo[0]);
				ps.setString(29,	windInfo[1]);
				ps.setString(30, 	season);
				
				ps.setString(31,	rulingDeity);
				ps.setInt(32,		-1);								// TODO: near-deed, need to write up some code for this...
				ps.setInt(33,		-1);								// TODO: near-tower, there should be code in Wurm for this...
			}
			
			if(Mod.sqlLogging) {
				logger.log(Level.INFO, "Executing: " + ((LoggableStatement)ps).getQueryString());
			}

			rs = ps.executeQuery();
			Stats.inc("query.total.lootrules");

			int maxNumLoot = 99;
			HashSet<Integer> tableIds = new HashSet<Integer>();
			List<LootRule> rules = new ArrayList<LootRule>();

			while (rs.next()) {
				// 1 id, 2 lootTable, 3 maxLoot, 4 ruleName
				if(rs.getInt(3) == 0) {
					logger.log(Level.INFO, "Found a LootRule with maxNumLoot set to 0, skipping it");
					Stats.inc("rule.cancelled." + rs.getInt(1) + "." + rs.getString(4));
					continue;
				}
				
				rules.add(new LootRule(
					rs.getInt(1),
					rs.getString(4),
					rs.getInt(2),
					rs.getByte(3)
				));
				tableIds.add(rs.getInt(2));

				// We will err on the cheap side here, whichever table has the lowest 
				// maxLoot dictates number of items we will drop.
				if(rs.getInt(3) < maxNumLoot) {
					maxNumLoot = rs.getInt(3);
				}

				Stats.inc("rule.triggered." + rs.getInt(1) + "." + rs.getString(4));
			}
			ps.close();

			lootSet = new LootSet(tableIds, rules, maxNumLoot);

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to get matching creatures", e);
			throw new RuntimeException(e);
		}

		return lootSet;
	}


	private FocusZone getFocusZone(Creature c) {
		// Get first zone we bump into, this will be a bit weird if the tile 
		// is in more than one FocusZone.
		FocusZone fs = null;
		for(FocusZone z : FocusZone.getZonesAt(c.getTileX(), c.getTileY())) {
			fs = z;
			break;
		}
		return fs;
	}


	private String getRulingDeity(Creature c) {
		String rulingDeity = "";
		try {
			FaithZone fz;
			fz = Zones.getFaithZone(c.getTileX(), c.getTileY(), c.isOnSurface());
			if(fz != null) {
				rulingDeity = fz.currentRuler.name.toLowerCase();
			}
		} catch (NoSuchZoneException e) { }
		return rulingDeity;
	}


	private String getWeather() {
		// 'precipitation' (snow/rain), 'clear' (sun), 'fog' or 'overcast' (cloudy)
		String weather;
		if(Server.getWeather().getFog() > 0.5) {
			weather = "fog";
		} else if(Server.getWeather().getRain() > 0.5) {
			weather = "precipitation";
		} else if(Server.getWeather().getCloudiness() > 0.5) {
			weather = "overcast";
		} else {
			weather = "clear";
		}
		return weather;
	}


	private String getSeason()
	{
		String season;
		
		if(WurmCalendar.isSpring()) { 
			season = "spring";
		} else if(WurmCalendar.isSummer()) {
			season = "summer";
		} else if(WurmCalendar.isAutumn()) {
			season = "autumn";
		} else {
			season = "winter";
		}
		
		return season;
	}

	/**
	 *  
	 * @return e.g. [ gale, northwest ]
	 */
	private String[] getWindInfo()
	{
		String str = Server.getWeather().getWeatherString(false).substring(2).replace(".", "");	// get rid of "A " and "."
		return str.split(" is coming from the ");
	}


	private String getTypeString(Creature c)
	{
		String pre = c.getPrefixes();

		if(c.isUnique()) {
			pre = pre.substring(4);
		}
		
		String[] parts = pre.trim().split(" ", 3);

		if(parts.length == 3) {
			return parts[2];
		} else if(parts.length == 2) {
			return parts[1];
		} else {
			return "";
		}
	}


	private String getFatString(Creature c)
	{
		String pre = c.getPrefixes();

		if(c.isUnique()) {
			pre = pre.substring(4);
		}
		
		String[] parts = pre.trim().split(" ", 3);
		
		//logger.log(Level.INFO, "Parts: " + Arrays.toString(parts));

		if(parts.length > 1 && (parts[1].equals("starving") || parts[1].equals("fat"))) {
			return parts[1];
		}

		return "";
	}
	
	
	/**
	 * 
	 * @return true if any new table was created, false if we're using existing
	 */
	private boolean createTables()
	{
		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps = null;
		String sql;
		boolean createdTable = false;

		// Get tile type: Tiles.TILE_TYPE_SNOW;

		try {
			// Main table, define which creatures should drop what.
			if(ModSupportDb.hasTable(con, "FriyaLootRules") == false) {
				sql = ""
						+ "CREATE TABLE FriyaLootRules"
						+ "("
						+ "		id					INTEGER			PRIMARY KEY AUTOINCREMENT,	"	// unique id for this critter
						+ "		rulename			VARCHAR(50)		NULL,					"		// Just a rule name so you can easily identify it, not actually used for anything
						+ "		loottable			INTEGER			NOT NULL,				"		// point to a loottable which links to items

						+ "		maxloot				TINYINT			NOT NULL DEFAULT 1,		"		// -1 = no limit, otherwise number of items a creature can drop max	

						+ "		creature			VARCHAR(50)		NOT NULL DEFAULT '*',	"		// * = any NPC, or e.g. 'goblin'
						+ "		age					VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * = any, or e.g. 'venerable'
						+ "		type				VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * = any, or e.g. 'champion'
						
						+ "		gender				TINYINT			NOT NULL DEFAULT -1,	"		// -1 = any, 0 = male, 1 = female
						+ "		wild				TINYINT			NOT NULL DEFAULT 1,		"		// -1 = any, 0 = must be bred in captivity, 1 = must be wild

						+ "		surface				TINYINT			NOT NULL DEFAULT -1,	"		// -1 = any, 1 = surface, 2 = cave
						+ "		kingdom				TINYINT			NOT NULL DEFAULT -1,	"		// is in land of: -1 = any, 1 = JK, 2 = MR, 3 = HOTS, 4 = Freedom
						+ "		tiletype			INTEGER			NOT NULL DEFAULT -1,	"		// -1 = any, for specific see com.wurmonline.mesh.Tiles.TILE_*
						+ "		zonename			VARCHAR(50)		NOT NULL DEFAULT '*',	"		// * = any, enter zone name for specific

						+ "		starthour			TINYINT			NOT NULL DEFAULT -1,	"		// -1 = any, e.g. 00 = midnight
						+ "		endhour				TINYINT			NOT NULL DEFAULT -1,	"		// -1 = any, 23 = 11pm

						+ "		minaltitude			INTEGER			NOT NULL DEFAULT -1,	"		// -1 = any, 0+ = above water
						+ "		maxaltitude			INTEGER			NOT NULL DEFAULT -1,	"		// -1 = any, e.g. 1000 = 1k dirt above sea

						+ "		fat					VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * = any, starving or fat
						+ "		diseased			TINYINT			NOT NULL DEFAULT -1,	"		// -1 = any, or 0 = not diseased, 1 = diseased

						+ "		isunique			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = no, 1 = yes
						+ "		isfromkingdom		TINYINT			NOT NULL DEFAULT -1,	"		// is member of: -1 any, , 1 = JK, 2 = MR, 3 = HOTS, 4 = Freedom
						+ "		ishumanoid			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = no, 1 = yes
						+ "		iszombie			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = no, 1 = yes
						+ "		isfromrift			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = no, 1 = yes
						
						+ "		minslope			INTEGER			NOT NULL DEFAULT -1,	"		// -1 = any, or any number that describes min. slope (e.g. 40)
						+ "		maxslope			INTEGER			NOT NULL DEFAULT -1,	"		// -1 = any, or any number that describes max. slope (e.g. 100)

						+ "		minxpos				INTEGER			NOT NULL DEFAULT -1,	"		// -1 any, or a start coordinate
						+ "		minypos				INTEGER			NOT NULL DEFAULT -1,	"		// see above
						+ "		maxxpos				INTEGER			NOT NULL DEFAULT -1,	"		// -1 any, or an end coordinate
						+ "		maxypos				INTEGER			NOT NULL DEFAULT -1,	"		// see above

						+ "		weather				VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * or 'precipitation' (snow/rain), 'clear' (sun), 'fog' or 'overcast' (cloudy)
						+ "		windstrength		VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * or gale, strong wind, strong breeze, breeze, light breeze
						+ "		winddirection		VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * or north, northwest, west, southwest, south, southeast, east, northeast
						+ "		season				VARCHAR(20)		NOT NULL DEFAULT '*',	"		// *, spring, summer, autumn or winter
						
						+ "		deityinfluence		VARCHAR(20)		NOT NULL DEFAULT '*',	"		// * any, vynora, fo, ...
						+ "		neardeed			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = must not be near deed, 1 = must be near deed
						+ "		neartower			TINYINT			NOT NULL DEFAULT -1,	"		// -1 any, 0 = must not be near tower, 1 = must be near tower

						+ "		enabled				TINYINT			NOT NULL DEFAULT 1		"		// Always 1, used to enable/disable rules
						+ ")";
				ps = con.prepareStatement(sql);
				ps.execute();

				createdTable = true;
				logger.log(Level.INFO, "Created FriyaLootRules");
			}

			// LootTables, which enables us to reuse item(sets)
			if(ModSupportDb.hasTable(con, "FriyaLootTables") == false) {
				sql = ""
						+ "CREATE TABLE FriyaLootTables"
						+ "("
						+ "		tableid				INTEGER			NOT NULL,"						// Not unique, can have several items for the same table
						+ "		lootid				INTEGER			NOT NULL"						// Points to FryaLootItems.id (NOT itemids)
						+ ")";
				ps = con.prepareStatement(sql);
				ps.execute();
				
				createdTable = true;
				logger.log(Level.INFO, "Created FriyaLootTables");
			}
			
			// Actual items that can be dropped
			if(ModSupportDb.hasTable(con, "FriyaLootItems") == false) {
				sql = ""
						+ "CREATE TABLE FriyaLootItems"
						+ "("
						+ "		id					INTEGER			PRIMARY KEY AUTOINCREMENT,"		// This is the ID that LootTables link to, NOT itemids (which is Wurm item ID(s))
						
						+ "		itemids				VARCHAR(255)	NOT NULL,				"		// ItemList id(s), this a string because you can have several items using same settings, comma separated ids
						+ "		name				VARCHAR(50)		NOT NULL DEFAULT '',	"		// if not default
						+ "		material			TINYINT			NOT NULL DEFAULT -1,	"		// shared.constants.ItemMaterials.* (14 = birchwood)
						
						+ "		startql				FLOAT			NOT NULL DEFAULT 10,	"		// start of ql range item can be
						+ "		endql				FLOAT			NOT NULL DEFAULT 40,	"		// end of ql range item can be
						
						+ "		canberare			TINYINT			NOT NULL DEFAULT 1,		"		// 0 = no, 1 = can be rare
						+ "		dropchance			REAL			NOT NULL DEFAULT 1,		"		// 0-100% chance per item in itemid

						+ "		creator				VARCHAR(50)		NULL,					"		// creator tag
						+ "		auxdata				INTEGER			NULL,					"		// custom data on item
						+ "		decaytime			BIGINT			NULL,					"		// how quickly it should decay
						+ "		damage				FLOAT			NOT NULL DEFAULT 0,		"		// num damage item should have
						
						+ "		clonecount			INTEGER			NOT NULL DEFAULT 1,		"		// number of clones we should create of this item if the rule matches
						+ "		clonecountrandom	INTEGER			NOT NULL DEFAULT 0,		"		// how much randomness should be applied to number, where 0 is no randomness

						+ "		enchants			VARCHAR(255)	NULL,					"		// Enchants
						+ "		enchantstrength		INTEGER			NOT NULL DEFAULT 0,		"		// Strength of the enchants
						+ "		enchantstrengthrandom	INTEGER		NOT NULL DEFAULT 0,		"		// how much randomness should be applied to enchant strength, where 0 is no randomness

						+ "		custommethod		VARCHAR(255)	NULL,					"		// call static method to create a custom item, e.g. com.friya.wurmonline.server.vamps.createItem()
						+ "		customargument		VARCHAR(255)	NULL					"		// the argument to pass in to the custom method
						+ ")";
				ps = con.prepareStatement(sql);
				ps.execute();
 
				createdTable = true;
				logger.log(Level.INFO, "Created FriyaLootItems");
			}

			upgradeTables(con, ps);

			return createdTable;

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to create tables", e);
			throw new RuntimeException(e);
		}
	}


	private void upgradeTables(Connection con, PreparedStatement ps)
	{
		try {
			if(columnExists(con, "FriyaLootItems", "clonecount") == false) {
				logger.info("Upgrading FriyaLootItems with 'clonecount' support...");
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN clonecount INTEGER NOT NULL DEFAULT 1");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN clonecountrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();
			}

			if(columnExists(con, "FriyaLootItems", "enchants") == false) {
				logger.info("Upgrading FriyaLootItems with 'enchants' support...");
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN enchants VARCHAR(255) NULL");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN enchantstrength INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN enchantstrengthrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();
			}

			if(columnExists(con, "FriyaLootRules", "enabled") == false) {
				logger.info("Upgrading FriyaLootRules with 'enabled' support...");
				ps = con.prepareStatement("ALTER TABLE FriyaLootRules ADD COLUMN enabled TINYINT NOT NULL DEFAULT 1");
				ps.execute();
				ps.close();
			}

			if(columnExists(con, "FriyaLootItems", "realtemplate") == false) {
				logger.info("Upgrading FriyaLootItems with 'realtemplate' support...");
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN realtemplate INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();
			}

			if(columnExists(con, "FriyaLootItems", "colorr") == false) {
				logger.info("Upgrading FriyaLootItems with 'color', 'weight' and 'forcerare' support...");

				// color
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorr INTEGER NOT NULL DEFAULT -1");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorg INTEGER NOT NULL DEFAULT -1");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorb INTEGER NOT NULL DEFAULT -1");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorrrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorgrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN colorbrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();

				// weight
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN weight INTEGER NOT NULL DEFAULT -1");
				ps.execute();
				ps.close();

				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN weightrandom INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();

				// force rarity
				ps = con.prepareStatement("ALTER TABLE FriyaLootItems ADD COLUMN forcerare INTEGER NOT NULL DEFAULT 0");
				ps.execute();
				ps.close();
			}


		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to upgrade tables", e);
			throw new RuntimeException(e);
		}
	}


	private boolean columnExists(Connection con, String table, String column)
	{
		boolean found = false;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			ps = con.prepareStatement("PRAGMA table_info(" + table + ")");
			rs = ps.executeQuery();
			while(rs.next()) {
				if(rs.getString("name").equals(column)) {
					found = true;
					break;
				}
			}
		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Could not determine whether a column existed in a table, this might cause problems later on....", e);

		} finally {
			try {
				rs.close();
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		return found;
	}

	
	static public ArrayList<String> getStatementsFromBatch(String sqlBatch)
	{
		ArrayList<String> ret = new ArrayList<String>(); 
		
		Scanner s = new Scanner(sqlBatch);
		//s.useDelimiter("(;(\r)?\n)|(--\n)|(--\r\n)");
		//s.useDelimiter("(;(\r)?\n)|((\r)?\n)?(--)?.*(--(\r)?\n)");
		//s.useDelimiter("/\\*[\\s\\S]*?\\*/|--[^\\r\\n]*|;");
		s.useDelimiter("/\\*[\\s\\S]*?\\*/|--[^\\r\\n]*");
		
		try {
			//Statement st = null;
			//st = conn.createStatement();
			StringBuffer currentStatement = new StringBuffer();
	
			while (s.hasNext())
			{
				String line = s.next();
				//logger.log(Level.INFO, "Got Line: " + line);
				
				/*
				if(line.startsWith("--")) {
					logger.log(Level.INFO, "SKIPPED");
					continue;
				}
				*/
				
				if (line.startsWith("/*!") && line.endsWith("*/"))
				{
					int i = line.indexOf(' ');
					line = line.substring(i + 1, line.length() - " */".length());
				}

				if (line.trim().length() > 0)
				{
					//st.execute(line);
					currentStatement.append(line);
	
					if(line.contains(";")) {
						String[] tmp = currentStatement.toString().split(";");
						for(String ln : tmp) {
							if(ln.trim().length() == 0) {
								continue;
							}
							//logger.log(Level.INFO, "ADDING: " + ln);
							ret.add(ln);
						}
						currentStatement.setLength(0);
					}
							
				}
			}
		} finally {
			s.close();
		}
		
		return ret;
	}
}
