package com.friya.wurmonline.server.loot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.gotti.wurmunlimited.modsupport.ModSupportDb;

import com.wurmonline.server.Server;
import com.wurmonline.server.items.Item;

/**
 * A LootSet is one or more LootTables, depending on how many matched the particular creature 
 * that died. It will take care of how many items to drop and actually picking random items.
 * 
 * @author Friya
 */
public class LootSet
{
    private static Logger logger = Logger.getLogger(LootSet.class.getName());

    int maxNumLoot = 1;
	HashMap<Integer, LootTable> lootTables = new HashMap<Integer, LootTable>();
	private List<LootRule> lootRules = new ArrayList<LootRule>();
	
	
	public LootSet()
	{
	}

	LootSet(HashSet<Integer> tableIds, List<LootRule> rules, int maxNumLoot)
	{
		Connection con = ModSupportDb.getModSupportDb();
		PreparedStatement ps;
		String sql;

		this.maxNumLoot = maxNumLoot;
		this.lootRules = rules;

		LootTable lt = null;
		LootItem li = null;

		for(int id : tableIds) {
			if(LootSystem.lootTables.containsKey(id)) {
				logger.log(Level.FINE, "Cache hit on lootTable: " + id);
				lt = LootSystem.lootTables.get(id);

			} else {
				logger.log(Level.FINE, "Cache MISS on lootTable: " + id);
				try {
					// get from database and store in cache
					sql = "SELECT i.* FROM FriyaLootTables AS t "
							+ "INNER JOIN FriyaLootItems AS i ON (t.lootid = i.id) "
							+ "WHERE tableid = ?";
					ps = con.prepareStatement(sql);
					ps.setInt(1, id);
					ResultSet rs = ps.executeQuery();

					lt = new LootTable(id);
					li = null;
					while(rs.next()) {
						if(LootSystem.lootItems.containsKey(rs.getInt(1))) {
							logger.log(Level.FINE, "Cache hit on lootItem: " + rs.getInt(1) + " (of lootTable: " + id + ")");
							li = LootSystem.lootItems.get(rs.getInt(1));

						} else {
							logger.log(Level.FINE, "Cache MISS on lootItem: " + rs.getInt(1) + " (of lootTable: " + id + ")");
/*
							li = new LootItem(
								rs.getInt("id"),						// int id

								rs.getString("itemids"),				// String wurmItemIds
								rs.getString("name"),					// String name
								rs.getByte("material"),					// byte material
								rs.getFloat("startql"),					// float startQl
								rs.getFloat("endql"),					// float endQl
								rs.getByte("canberare"),				// byte canBeRare
								rs.getDouble("dropchance"),				// byte dropChance
								rs.getString("creator"),				// String creator
								rs.getInt("auxdata"),					// int auxData
								rs.getLong("decaytime"),				// long decayTime
								rs.getFloat("damage"),					// float damage
								
								rs.getString("custommethod"),			// String customMethod
								rs.getString("customargument"),			// String customArgument
								
								rs.getInt("clonecount"),
								rs.getInt("clonecountrandom"),
								
								rs.getString("enchants"),
								rs.getInt("enchantstrength"),
								rs.getInt("enchantstrengthrandom"),
								
								rs.getInt("realtemplate")
							);
*/
							li = LootItem.getFromDB(rs);
							LootSystem.lootItems.put(rs.getInt(1), li);
						}
						
						lt.addLootItem(li);
					}
					LootSystem.lootTables.put(id, lt);

				} catch (SQLException e) {
					logger.log(Level.SEVERE, "Failed to get loot-table", e);
					throw new RuntimeException(e);
				}
			}

			lootTables.put(id, lt);
			Stats.inc("table.triggered." + lt.getTableId());
		}
	}
	
	
	private int[] shuffleArray(int[] array)
	{
	    int index, temp;
	    Random random = new Random();
	    for (int i = array.length - 1; i > 0; i--)
	    {
	        index = random.nextInt(i + 1);
	        temp = array[index];
	        array[index] = array[i];
	        array[i] = temp;
	    }
	    return array;
	}
	
	
	public Item[] getLoot()
	{
		boolean onlyGuaranteedDrops = false;
		ArrayList<Item> result = new ArrayList<Item>();
		
		HashMap<Integer, LootItem> lootCandidates = new HashMap<Integer, LootItem>();
		HashMap<Integer, LootItem> lootDecisions = new HashMap<Integer, LootItem>();
		
		logger.log(Level.FINE, "Found " + lootTables.size() + " matching loot-tables!");

		if(maxNumLoot == 0) {
			logger.log(Level.INFO, "LootSet had maxNumLoot set to 0, will only return items with 100% drop chance for this one");
			onlyGuaranteedDrops = true;
		}

		for(LootTable lt : lootTables.values()) {
			//logger.log(Level.INFO, "loottable: " + lt);
			lootCandidates.putAll(lt.getLootItemCandidatesByWurmId());
		}
		
		// We want to randomize the order of our candidates since we might have a cap 
		// of number of drops, without this, we will have skewed odds of always dropping 
		// the same items.
		Object[] tmp = lootCandidates.keySet().toArray();
		int[] randomizedCandidates = new int[tmp.length];
		for(int x = 0; x < tmp.length; x++ ) {
			randomizedCandidates[x] = (int)tmp[x];
		}
		randomizedCandidates = shuffleArray(randomizedCandidates);

		logger.log(Level.FINE, "getLoot(), candidates: " + lootCandidates.toString());
		logger.log(Level.FINE, "getLoot(), candidates in randomized [Fisher–Yates] order: " + Arrays.toString(randomizedCandidates));

		// - if something has 100% drop-rate, will always drop and not look at maxNumLoot
		// - from candidates, look at item's drop-chance, max number of items to drop, then randomize them out and 
		//   put decided items into lootDecisions...
		// - then create them by calling the appropriate LootItem with wurmItemId as argument (iteration below)
		for(int n : randomizedCandidates) { // Integer n : lootCandidates.keySet()
			LootItem candidate = lootCandidates.get(n);
			
			if(candidate.getDropChance() >= 99.9999f) {
				// If 100% drop rate, always throw it into decisions.
				lootDecisions.put(n, candidate);
				Stats.inc("drop.total.guaranteed");
				
			} else if(onlyGuaranteedDrops == false) {
				// Okay, so "normal" drop-rate
				if(lootDecisions.size() < maxNumLoot) {
					double rnd = Server.rand.nextDouble() * 100;
					
					//rnd = candidate.getDropChance();
					
					logger.log(Level.FINEST, "Rolling for loot-item ID: " + candidate.getId() +  "[" + candidate.getWurmItemIds() + "] Drop chance is: " + String.format("%.4f", candidate.getDropChance()) + "% RNG rolled: " + String.format("%.4f", rnd));
					if(rnd <= candidate.getDropChance()) {
						logger.log(Level.FINEST, "RNG says this item should be dropped!");
						lootDecisions.put(n, candidate);
					} else {
						logger.log(Level.FINEST, "RNG says no drop this time.");
						Stats.inc("drop.missed." + candidate.getId() + "." + candidate.getName());
					}
				}
			} else {
				// We are in a 0 items should drop situation, so don't do anything here! The else is here for clarity.
			}
		}

		logger.log(Level.FINE, "getLoot(), decisions: " + lootDecisions.toString());

		for(int decision : lootDecisions.keySet()) {
			LootItem ld = lootDecisions.get(decision);

			int clones = ld.getCloneCount();

			if(ld.getCloneCountRandom() > 0) {
				clones += Server.rand.nextInt(ld.getCloneCountRandom());
			}
			
			for(int n = 0; n < clones; n++) {
				Item item = ld.create(decision);
				if(item != null) {
					result.add(item);
					
					if(item.getRarity() > 0) {
						Stats.inc("drop.total.rares");
					}
				}
			}

			Stats.inc("drop.triggered." + ld.getId() + "." + ld.getName());
			Stats.inc("drop.dropped." + ld.getId() + "." + ld.getName(), clones);
			Stats.inc("drop.total", clones);
		}

		logger.log(Level.FINE, "Returning CREATED items, PLACE in world OR DESTROY: " + result.toString());

		return result.toArray(new Item[0]);
	}

	List<LootRule> getLootRules()
	{
		return lootRules;
	}

}
