package com.friya.wurmonline.server.loot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.wurmonline.server.FailedException;
import com.wurmonline.server.Server;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemFactory;
import com.wurmonline.server.items.ItemSpellEffects;
import com.wurmonline.server.items.NoSuchTemplateException;
import com.wurmonline.server.items.WurmColor;
import com.wurmonline.server.spells.SpellEffect;

/**
 * A LootItem is a link between what a certain item should look like and an item-template in Wurm.
 * 
 * @author Friya
 */
public class LootItem
{
    private static Logger logger = Logger.getLogger(LootItem.class.getName());

	private int lootId;
	private int[] wurmItemIds;
	private String name = "";
	private byte material = -1;
	private float startQl = 10;
	private float endQl = 40;
	private boolean canBeRare = true;
	private double dropChance = 1;
	private String creator = "";
	private int auxData = 0;
	private long decayTime = 0;
	private float damage = 0;
	private String customMethod;
	private String customArgument;

	private int cloneCount = 1;
	private int cloneCountRandom = 0;
	
	private String enchants = "";
	private int enchantStrength = 0;
	private int enchantStrengthRandom = 0;
	
	private int realTemplate = 0;
	
	private int colorR = -1;
	private int colorG = -1;
	private int colorB = -1;
	
	private int colorRrandom = 0;
	private int colorGrandom = 0;
	private int colorBrandom = 0;
	
	private int weight = -1;
	private int weightRandom = 0;
	
	private int forceRare = 0;		// 0 normal, 1 rare, 2 supreme, 3 fantastic
	
	private List<Byte> enchantIds = new ArrayList<Byte>();


	static LootItem getFromDB(ResultSet rs) throws SQLException
	{
		LootItem li = new LootItem(
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
			
			rs.getInt("realtemplate"),
			
			rs.getInt("colorr"),
			rs.getInt("colorg"),
			rs.getInt("colorb"),
			
			rs.getInt("colorrrandom"),
			rs.getInt("colorgrandom"),
			rs.getInt("colorbrandom"),
			
			rs.getInt("weight"),
			rs.getInt("weightrandom"),
			
			rs.getInt("forcerare")
		);
	
		return li;
	}


	int save(Connection con)
	{
		String sql;
		boolean update = false;

		// set this.lootId if it is an update
		if(lootId > 0) {
			sql = "UPDATE FriyaLootItems SET "
					+ "		itemids=?, name=?, material=?, startql=?, endql=?, canberare=?, dropchance=?, creator=?, auxdata=?, decaytime=?, damage=?, custommethod=?, "
					+ "		customargument=?, clonecount=?, clonecountrandom=?, enchants=?, enchantstrength=?, enchantstrengthrandom=?, realtemplate=?, "
					+ "		colorr=?, colorg=?, colorb=?, colorrrandom=?, colorgrandom=?, colorbrandom=?, weight=?, weightrandom=?, forcerare=?"
					+ " WHERE id = " + getId();
			update = true;
		} else {
			sql = "INSERT INTO FriyaLootItems("
					+ "		itemids, name, material, startql, endql, canberare, dropchance, creator, auxdata, decaytime, damage, custommethod, customargument, "
					+ "		clonecount, clonecountrandom, enchants, enchantstrength, enchantstrengthrandom, realtemplate,"
					+ "		colorr, colorg, colorb, colorrrandom, colorgrandom, colorbrandom, weight, weightrandom, forcerare"
					+ ") "
					+ "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
		
		PreparedStatement ps;
		
		try {
			if(Mod.sqlLogging) {
		    	ps = new LoggableStatement(con, sql);
		    } else {
		    	ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
		    }
			// 07apr2017, replaced this with above
			//ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

			int i = 1;
			ps.setString(i++, getWurmItemIds(true));
			ps.setString(i++, getName());
			ps.setByte(i++, material);
			ps.setFloat(i++, startQl);
			ps.setFloat(i++, endQl);
			ps.setByte(i++, (byte)(canBeRare ? 1 : 0));
			ps.setDouble(i++, dropChance);
			ps.setString(i++, creator);
			ps.setInt(i++, auxData);
			ps.setLong(i++, decayTime);
			ps.setFloat(i++, damage);
			ps.setString(i++, customMethod);
			ps.setString(i++, customArgument);
			
			ps.setInt(i++, cloneCount);
			ps.setInt(i++, cloneCountRandom);

			ps.setString(i++, enchants);
			ps.setInt(i++, enchantStrength);
			ps.setInt(i++, enchantStrengthRandom);

			ps.setInt(i++, realTemplate);
			
			// Below added 25 July 2017
			ps.setInt(i++, colorR);
			ps.setInt(i++, colorG);
			ps.setInt(i++, colorB);

			ps.setInt(i++, colorRrandom);
			ps.setInt(i++, colorGrandom);
			ps.setInt(i++, colorBrandom);

			ps.setInt(i++, weight);
			ps.setInt(i++, weightRandom);

			ps.setInt(i++, forceRare);

			if(Mod.sqlLogging) {
				logger.log(Level.INFO, "Executing: " + ((LoggableStatement)ps).getQueryString());
			}

			if(update) {
				ps.executeUpdate();
			} else {
				ps.execute();
			}
			
			ResultSet rs = ps.getGeneratedKeys();
			if(rs != null) {
				rs.next();
				this.lootId = rs.getInt(1);

				logger.log(Level.FINE, "Inserted item as: " + lootId);
			} else {
				logger.log(Level.WARNING, "no resultset back from getGeneratedKeys(), probably means nothing was created!");
			}
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to save LootItem", e);
			throw new RuntimeException(e);
		}

		return this.lootId;
	}
	
	
	public LootItem(int id, String wurmItemIds, String name, byte material, float startQl, float endQl, byte canBeRare, 
			double dropChance, String creator, int auxData, long decayTime, float damage, String customMethod, String customArgument,
			int cloneCount, int cloneCountRandom, String enchants, int enchantStrength, int enchantStrengthRandom, int realTemplate,
			int colorR, int colorG, int colorB, int colorRrandom, int colorGrandom, int colorBrandom, int weight, int weightRandom,
			int forceRare
		)
	{
		this.lootId = id;
		this.wurmItemIds = parseItemWurmIds(wurmItemIds);
		this.name = name;
		this.material = material;
		this.startQl = startQl;
		this.endQl = endQl;
		this.canBeRare = (canBeRare == 1 ? true : false);
		this.setDropChance(dropChance);
		this.creator = creator;
		this.auxData = auxData;
		this.decayTime = decayTime;
		this.damage = damage;
		this.customMethod = customMethod;
		this.customArgument = customArgument;
		
		this.cloneCount = cloneCount;
		this.cloneCountRandom = cloneCountRandom;
		
		this.enchants = enchants;
		this.enchantStrength = enchantStrength;
		this.enchantStrengthRandom = enchantStrengthRandom;
		
		this.realTemplate = realTemplate;
		
		this.colorR = colorR; 
		this.colorG = colorG;
		this.colorB = colorB;
		this.colorRrandom = colorRrandom; 
		this.colorGrandom = colorGrandom;
		this.colorBrandom = colorBrandom;
		this.weight = weight;
		this.weightRandom = weightRandom;
		this.forceRare = forceRare;
	}


	public LootItem(String wurmItemIds, double dropChance)
	{
		this.wurmItemIds = parseItemWurmIds(wurmItemIds);
		this.dropChance = dropChance;
	}
	
	public LootItem(String wurmItemIds, byte material, double dropChance, String creator)
	{
		this.wurmItemIds = parseItemWurmIds(wurmItemIds);
		this.material = material;
		this.dropChance = dropChance;
		this.creator = creator;
	}
	
	private int[] parseItemWurmIds(String wurmItemIds)
	{
		String[] tmp = wurmItemIds.split(",");
		int[] ret = new int[tmp.length];
		
		for(int x = 0; x < tmp.length; x++) {
			ret[x] = Integer.parseInt(tmp[x].trim());
		}
		
		return ret;
	}
	
	
	public int[] getWurmItemIds()
	{
		return wurmItemIds;
	}
	
	
	public String getWurmItemIds(boolean asString)
	{
		if(asString) {
			return Arrays.toString(getWurmItemIds()).replace(" ", "").replace("[", "").replace("]", ""); 
		}

		throw new RuntimeException("Use the other getWurmItemIds()");
	}
	
	
	public int getId()
	{
		return lootId;
	}

	
	// We should MAYBE check if this wurmItemId is within this loot-item, but I don't see any harm in 
	// reusing a loot-item's configuration for other items.
	public Item create(int wurmItemId)
	{
		Item item = null;
		
		try {
			float ql;
			
			if(endQl == startQl || startQl > endQl) {
				ql = endQl;
			} else {
				ql = (startQl + (endQl < startQl ? 0 : Server.rand.nextInt((int)(endQl - startQl))));
			}

			// Create an item according to the description of this item
			item = ItemFactory.createItem(
				wurmItemId,			// int templateId
				ql,					// float qualityLevel, 
				(byte)0,			// byte aRarity, 
				creator				// @Nullable String creator, 
			);
			
			if(getName() != null && getName().length() > 0 && wurmItemIds.length == 1) {
				item.setName(getName());
			}
			
			if(material > 0) {
				item.setMaterial(material);
			}
			item.setDamage(damage);
			item.setAuxData((byte)auxData);
			
			if(realTemplate > 0) {
				item.setRealTemplate(realTemplate);
			}
			
		} catch (FailedException | NoSuchTemplateException e) {
			logger.log(Level.SEVERE, "Failed to create item with template id: " + wurmItemId);
			e.printStackTrace();
		}
		
		setColor(item);
		
		setWeight(item);

		enchant(item);
		
		handleRarify(item);

		return item;
	}


	private void setWeight(Item item)
	{
		// Weight (can also be random)
		if(weight != -1 || weightRandom > 0) {
			int newWeight = (weight > 0 ? weight : item.getWeightGrams());

			if(weightRandom > 0) {
				newWeight += Server.rand.nextInt(weightRandom);
			}
			
			item.setWeight(newWeight, false);
		}
	}


	private void setColor(Item item)
	{
		int newR = 0, newG = 0, newB = 0;
		
		if(colorR == -1 && colorB == -1 && colorG == -1) {
			return;
		}
		
		if(colorR >= 0) newR = colorR;
		if(colorG >= 0) newG = colorG;
		if(colorB >= 0) newB = colorB;

		if(colorRrandom > 0) newR += Server.rand.nextInt(colorRrandom);
		if(colorGrandom > 0) newG += Server.rand.nextInt(colorGrandom);
		if(colorBrandom > 0) newB += Server.rand.nextInt(colorBrandom);
		
		if(newR != -1 || newG != -1 && newB != -1) {
			item.setColor(WurmColor.createColor( 
					newR < 0 ? 0 : newR & 255,
					newG < 0 ? 0 : newG & 255,
					newB < 0 ? 0 : newB & 255
			));
		}
	}


	private boolean enchant(Item item)
	{
		if(enchants == null && enchantIds.size() == 0) {
			return true;
		}

		if(enchantIds.size() == 0) {
			String[] enchantArr = enchants.trim().split(" ");
			
			if(enchantArr.length > 0) {
				for(String ench : enchantArr) {
					if(LootSystem.getInstance().enchantAbbreviations.containsKey(ench)) {
						enchantIds.add( LootSystem.getInstance().enchantAbbreviations.get(ench) );
						Stats.inc("drop.total.enchanted." + ench);
						
					} else if(isNumeric(ench)) {
						enchantIds.add( (byte)Integer.parseInt(ench) );
						Stats.inc("drop.total.enchanted." + ench);
						
					} else {
						logger.warning("Enchant " + ench + " for LootItem " + lootId + " is invalid");
					}
				}
			}
		}
		
		double power;
		for(byte ench : enchantIds) {
			power = enchantStrength;
			if(enchantStrengthRandom > 0) {
				power += Server.rand.nextInt(enchantStrengthRandom);
			}

			logger.fine("Enchanting " + item.getName() + " with " + ench + " [" + power + "]");
			cast(ench, power, item);
		}

		return true;
	}


	private void cast(byte enchant, double power, Item target)
	{
		ItemSpellEffects effs = target.getSpellEffects();
		
		if (effs == null) {
			effs = new ItemSpellEffects(target.getWurmId());
		}
		
		SpellEffect eff = new SpellEffect(target.getWurmId(), enchant, (float)power, 20000000);
		effs.addSpellEffect(eff);
	}


	private boolean isNumeric(String str)  
	{  
	  try  
	  {  
	    @SuppressWarnings("unused")
		double d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}


	void handleRarify(Item i)
	{
		if(forceRare > 0) {
			i.setRarity((byte)forceRare);
			return;
		}
		
		boolean isBoneCollar = i.getTemplateId() == 867;
		
		if(canBeRare == false && isBoneCollar == false) {
			return;
		}
		
		byte rrarity = (byte) (Server.rand.nextInt(100) == 0 || isBoneCollar ? 1 : 0);
		
		if (rrarity > 0) {
			rrarity = (byte) (Server.rand.nextInt(100) == 0 && isBoneCollar ? 2 : 1);
		}
		if (rrarity > 1) {
			rrarity = (byte) (Server.rand.nextInt(100) == 0 && isBoneCollar ? 3 : 2);
		}

		i.setRarity(rrarity);
	}


	double getDropChance() {
		return dropChance;
	}


	private void setDropChance(double dropChance) {
		this.dropChance = dropChance;
	}

	int getCloneCount() {
		return cloneCount;
	}

	int getCloneCountRandom() {
		return cloneCountRandom;
	}


	String getName()
	{
		return name;
	}
}
