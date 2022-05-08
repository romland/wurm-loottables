package com.friya.wurmonline.server.loot;

import java.util.HashMap;

/**
 * A LootTable is links to one or more items. It is used together with LootSets to determine
 * what should drop for a specific creature.
 * 
 * @author Friya
 */
public class LootTable
{
	private int tableId;
	boolean fetchedChildren = false;
	private HashMap<Integer, LootItem> lootItems = new HashMap<Integer, LootItem>();
	
	LootTable(int id)
	{
		setTableId(id);
	}

	void addLootItem(LootItem li)
	{
		lootItems.put(li.getId(), li);
	}


	public int getTableId()
	{
		return tableId;
	}

	private void setTableId(int tableId)
	{
		this.tableId = tableId;
	}
	
	public String toString()
	{
		return "LootTable#" + tableId;
	}
	
	
	public HashMap<Integer, LootItem> getLootItemCandidatesByWurmId()
	{
		HashMap<Integer, LootItem> wurmIdLootItems = new HashMap<Integer, LootItem>();

		for(LootItem li : lootItems.values()) {
			int[] wurmItemIds = li.getWurmItemIds();
			for(int x = 0; x < wurmItemIds.length; x++) {
				wurmIdLootItems.put(wurmItemIds[x], li);
			}
		}
		
		return wurmIdLootItems;
	}



	
/*
	// TODO:
	// http://forum.wurmonline.com/index.php?/topic/151741-released-server-mod-loot-tables/&do=findComment&comment=1566808
	public HashMap<Integer, LootItem> getLootItemCandidatesId()
	{
		HashMap<Integer, LootItem> lootIdLootItems = new HashMap<Integer, LootItem>();

		for(LootItem li : lootItems.values()) {
			int[] lootItemIds = li.getLootItemIds();
			for(int x = 0; x < lootItemIds.length; x++) {
				lootIdLootItems.put(lootItemIds[x], li);
			}
		}
		
		return lootIdLootItems;
	}
*/
}
