package com.friya.wurmonline.server.loot;

import java.util.ArrayList;
import java.util.List;
//import java.util.logging.Logger;

import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.creatures.Creatures;
import com.wurmonline.server.items.Item;


public class LootResult
{
//	private static Logger logger = Logger.getLogger(LootResult.class.getName());

	private int maxNumberOfLoot = 0;
	private List<LootRule> lootRules = new ArrayList<LootRule>();
	private Item[] items;
	private Creature creature;
	private Creature[] attackerObs;
	private boolean fetchedAttackers = false;
	private float creatureStrength = 0f;


	LootResult()
	{
	}


	public List<LootRule> getLootRules()
	{
		return lootRules;
	}
	

	void setLootRules(List<LootRule> lootRules)
	{
		this.lootRules = lootRules;
	}
	

	public Item[] getItems()
	{
		if(items == null) {
			items = new Item[]{};
		}
		
		return items;
	}


	void setItems(Item[] items)
	{
		this.items = items;
	}


	public Creature getCreature()
	{
		return creature;
	}


	void setCreature(Creature creature)
	{
		if(Mod.simulationOnStartup) {
			return;
		}

		this.creature = creature;
		this.creatureStrength = creature.getBaseCombatRating() + creature.getBonusCombatRating();
		setKillers();
	}


	private void setKillers()
	{
		Creature c;

		List<Creature> tmpAttackers = new ArrayList<Creature>();
		long[] rawAttackerIds = creature.getLatestAttackers();

		for(long a : rawAttackerIds) {
			c = Players.getInstance().getPlayerOrNull(a);

			if(c == null) {
				c = Creatures.getInstance().getCreatureOrNull(a);
			}
			
			if(c == null) {
				continue;
			}

			tmpAttackers.add(c);
		}

		attackerObs = tmpAttackers.toArray(new Creature[]{});
		fetchedAttackers = true;
	}


	public boolean isKilledByPlayer()
	{
		Creature[] attackers = getKillers();
		for(Creature c : attackers) {
			if(Players.getInstance().getPlayerOrNull(c.getWurmId()) != null) {
				return true;
			}
		}

		return false;
	}

	
	public Creature[] getKillers()
	{
		if(fetchedAttackers == false) {
			setKillers();
		}

		return attackerObs;
	}


	public LootRule getLootRule(String ruleName)
	{
		for(LootRule lr : lootRules) {
			if(lr.getRuleName().equals(ruleName)) {
				return lr;
			}
		}
		
		return null;
	}


	public boolean hasLootRule(String ruleName)
	{
		return getLootRule(ruleName) != null;
	}


	public int getLootCap()
	{
		return maxNumberOfLoot;
	}


	void setLootCap(int maxNumberOfLoot)
	{
		this.maxNumberOfLoot = maxNumberOfLoot;
	}


	public float getCreatureStrength()
	{
		return creatureStrength;
	}


	public void setCreatureStrength(float creatureStrength)
	{
		this.creatureStrength = creatureStrength;
	}
}
