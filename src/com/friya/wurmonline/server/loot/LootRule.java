package com.friya.wurmonline.server.loot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;


public class LootRule
{
	// a wrapper around a database row; we don't actually need it internally, but 
	// it's handy to pass in rules into the system with...

    private static Logger logger = Logger.getLogger(LootRule.class.getName());
	
	private int id;
	private String ruleName = "";
	private int lootTable = -1;
	private byte maxLoot = 1;
	
	private String creature = "*";
	private String age = "*";
	private String type = "*";
	private byte gender = -1;
	private byte wild = -1;
	private byte surface = -1;
	private byte kingdom = -1;
	private int tileType = -1;
	private String zoneName = "*";
	private byte startHour = -1;
	private byte endHour = -1;
	private int minAltitude = -1;
	private int maxAltitude = -1;
	private String fat = "*";
	private byte diseased = -1;
	private byte isUnique = -1;
	private byte isFromKingdom = -1;
	private byte isHumanoid = -1;
	private byte isZombie = -1;
	private byte isFromRift = -1;

	private int minSlope = -1;
	private int maxSlope = -1;
	
	private int minXpos = -1;
	private int minYpos = -1;
	private int maxXpos = -1;
	private int maxYpos = -1;
	
	private String weather = "*";
	private String windStrength = "*";
	private String windDirection = "*";
	private String season = "*";

	private String deityInfluence = "*";
	private byte nearDeed = -1;
	private byte nearTower = -1;
	
	public LootRule(String ruleName)
	{
		this.ruleName = ruleName;
	}

	public LootRule(int id, String ruleName, int lootTable, byte maxLoot)
	{
		this.id = id;
		this.lootTable = lootTable;
		this.maxLoot = maxLoot;
		this.ruleName = ruleName;
	}


	int save(Connection con)
	{
		String sql;
		boolean update = false;
		
		// set this.lootId if it is an update
		if(id > 0) {
			sql = "UPDATE FriyaLootRules SET "
					+ "rulename=?, loottable=?, maxloot=?, creature=?, age=?, type=?, gender=?, wild=?, surface=?, kingdom=?, tiletype=?, zonename=?, starthour=?, endhour=?,"
					+ "minaltitude=?, maxaltitude=?, fat=?, diseased=?, isunique=?, isfromkingdom=?, ishumanoid=?, iszombie=?, isfromrift=?, minslope=?, maxslope=?,"
					+ "minxpos=?, minypos=?, maxxpos=?, maxypos=?, weather=?, windstrength=?, winddirection=?, season=?, deityinfluence=?, neardeed=?, neartower=?"
					+ " WHERE id = " + getId();
			update = true;
		} else {
			sql = "INSERT INTO FriyaLootRules"
					+ "(rulename, loottable, maxloot, creature, age, type, gender, wild, surface, kingdom, tiletype, zonename, starthour, endhour,"
					+ " minaltitude, maxaltitude, fat, diseased, isunique, isfromkingdom, ishumanoid, iszombie, isfromrift, minslope, maxslope,"
					+ " minxpos, minypos, maxxpos, maxypos, weather, windstrength, winddirection, season, deityinfluence, neardeed, neartower)"
					+ " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		}
		
		PreparedStatement ps;
		
		try {
	    	ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
			
	    	int i = 1;
			ps.setString(i++, ruleName);
			ps.setInt(i++, lootTable);
			ps.setInt(i++, maxLoot);
			ps.setString(i++, creature);
			ps.setString(i++, age);
			ps.setString(i++, type);
			ps.setByte(i++, gender);
			ps.setByte(i++, wild);
			ps.setByte(i++, surface);
			ps.setByte(i++, kingdom);
			ps.setInt(i++, tileType);
			ps.setString(i++, zoneName);
			ps.setByte(i++, startHour);
			ps.setByte(i++, endHour);
			ps.setInt(i++, minAltitude);
			ps.setInt(i++, maxAltitude);
			ps.setString(i++, fat);
			ps.setByte(i++, diseased);
			ps.setByte(i++, isUnique);
			ps.setByte(i++, isFromKingdom);
			ps.setByte(i++, isHumanoid);
			ps.setByte(i++, isZombie);
			ps.setByte(i++, isFromRift);
			ps.setInt(i++, minSlope);
			ps.setInt(i++, maxSlope);
			ps.setInt(i++, minXpos);
			ps.setInt(i++, minYpos);
			ps.setInt(i++, maxXpos);
			ps.setInt(i++, maxYpos);
			ps.setString(i++, weather);
			ps.setString(i++, windStrength);
			ps.setString(i++, windDirection);
			ps.setString(i++, season);
			ps.setString(i++, deityInfluence);
			ps.setByte(i++, nearDeed);
			ps.setByte(i++, nearTower);

			if(update) {
				ps.executeUpdate();
			} else {
				ps.execute();
			}
			
			ResultSet rs = ps.getGeneratedKeys();
			if(rs != null) {
				rs.next();
				this.id = rs.getInt(1);

				logger.log(Level.FINE, "Inserted item as: " + id);
			} else {
				logger.log(Level.WARNING, "no resultset back from getGeneratedKeys(), probably means nothing was created!");
			}
			ps.close();

		} catch (SQLException e) {
			logger.log(Level.SEVERE, "Failed to save LootItem", e);
			throw new RuntimeException(e);
		}

		return this.id;
	}
	

	/**
	 * 
	 * @param id
	 */
	public void setId(int id) {
		this.id = id;
	}
	public int getId() {
		return id;
	}

	/**
	 * 
	 * @param ruleName
	 */
	public void setRuleName(String ruleName) {
		this.ruleName = ruleName;
	}
	public String getRuleName() {
		return ruleName;
	}

	/**
	 * 
	 * @param lootTable
	 */
	public void setLootTable(int lootTable) {
		this.lootTable = lootTable;
	}
	public int getLootTable() {
		return lootTable;
	}

	/**
	 * 
	 * @param maxLoot
	 */
	public void setMaxLoot(byte maxLoot) {
		this.maxLoot = maxLoot;
	}
	public byte getMaxLoot() {
		return maxLoot;
	}

	/**
	 * 
	 * @param creature
	 */
	public void setCreature(String creature) {
		this.creature = creature;
	}
	public String getCreature() {
		return creature;
	}
	
	/**
	 * 
	 * @param age
	 */
	public void setAge(String age) {
		this.age = age;
	}
	public String getAge() {
		return age;
	}
	
	/**
	 * 
	 * @param type
	 */
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}
	
	/**
	 * 
	 * @param gender
	 */
	public void setGender(byte gender) {
		this.gender = gender;
	}
	public byte getGender() {
		return gender;
	}
	
	/**
	 * 
	 * @param wild
	 */
	public void setWild(byte wild) {
		this.wild = wild;
	}
	public byte getWild() {
		return wild;
	}
	
	/**
	 * 
	 * @param surface
	 */
	public void setSurface(byte surface) {
		this.surface = surface;
	}
	public byte getSurface() {
		return surface;
	}
	
	/**
	 * 
	 * @param kingdom
	 */
	public void setKingdom(byte kingdom) {
		this.kingdom = kingdom;
	}
	public byte getKingdom() {
		return kingdom;
	}
	
	/**
	 * 
	 * @param tileType
	 */
	public void setTileType(int tileType) {
		this.tileType = tileType;
	}
	public int getTileType() {
		return tileType;
	}
	
	/**
	 * 
	 * @param zoneName
	 */
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}
	public String getZoneName() {
		return zoneName;
	}
	
	/**
	 * 
	 * @param startHour
	 */
	public void setStartHour(byte startHour) {
		this.startHour = startHour;
	}
	public byte getStartHour() {
		return startHour;
	}
	
	/**
	 * 
	 * @param endHour
	 */
	public void setEndHour(byte endHour) {
		this.endHour = endHour;
	}
	public byte getEndHour() {
		return endHour;
	}
	
	/**
	 * 
	 * @param minAltitude
	 */
	public void setMinAltitude(int minAltitude) {
		this.minAltitude = minAltitude;
	}
	public int getMinAltitude() {
		return minAltitude;
	}
	
	/**
	 * 
	 * @param maxAltitude
	 */
	public void setMaxAltitude(int maxAltitude) {
		this.maxAltitude = maxAltitude;
	}
	public int getMaxAltitude() {
		return maxAltitude;
	}
	
	/**
	 * 
	 * @param fat
	 */
	public void setFat(String fat) {
		this.fat = fat;
	}
	public String getFat() {
		return fat;
	}
	
	/**
	 * 
	 * @param diseased
	 */
	public void setDiseased(byte diseased) {
		this.diseased = diseased;
	}
	public byte getDiseased() {
		return diseased;
	}
	
	/**
	 * 
	 * @param isUnique
	 */
	public void setIsUnique(byte isUnique) {
		this.isUnique = isUnique;
	}
	public byte getIsUnique() {
		return isUnique;
	}
	
	/**
	 * 
	 * @param isFromKingdom
	 */
	public void setIsFromKingdom(byte isFromKingdom) {
		this.isFromKingdom = isFromKingdom;
	}
	public byte getIsFromKingdom() {
		return isFromKingdom;
	}
	
	/**
	 * 
	 * @param isHumanoid
	 */
	public void setIsHumanoid(byte isHumanoid) {
		this.isHumanoid = isHumanoid;
	}
	public byte getIsHumanoid() {
		return isHumanoid;
	}
	
	/**
	 * 
	 * @param isZombie
	 */
	public void setIsZombie(byte isZombie) {
		this.isZombie = isZombie;
	}
	public byte getIsZombie() {
		return isZombie;
	}
	
	/**
	 * 
	 * @param isFromRift
	 */
	public void setIsFromRift(byte isFromRift) {
		this.isFromRift = isFromRift;
	}
	public byte getIsFromRift() {
		return isFromRift;
	}
	
	/**
	 * 
	 * @param minSlope
	 */
	public void setMinSlope(int minSlope) {
		this.minSlope = minSlope;
	}
	public int getMinSlope() {
		return minSlope;
	}
	
	/**
	 * 
	 * @param maxSlope
	 */
	public void setMaxSlope(int maxSlope) {
		this.maxSlope = maxSlope;
	}
	public int getMaxSlope() {
		return maxSlope;
	}
	
	/**
	 * 
	 * @param minXpos
	 */
	public void setMinXpos(int minXpos) {
		this.minXpos = minXpos;
	}
	public int getMinXpos() {
		return minXpos;
	}
	
	/**
	 * 
	 * @param minYpos
	 */
	public void setMinYpos(int minYpos) {
		this.minYpos = minYpos;
	}
	public int getMinYpos() {
		return minYpos;
	}
	
	/**
	 * 
	 * @param maxXpos
	 */
	void setMaxXpos(int maxXpos) {
		this.maxXpos = maxXpos;
	}
	int getMaxXpos() {
		return maxXpos;
	}
	
	/**
	 * 
	 * @param maxYpos
	 */
	public void setMaxYpos(int maxYpos) {
		this.maxYpos = maxYpos;
	}
	public int getMaxYpos() {
		return maxYpos;
	}
	
	/**
	 * 
	 * @param weather
	 */
	void setWeather(String weather) {
		this.weather = weather;
	}
	String getWeather() {
		return weather;
	}
	
	/**
	 * 
	 * @param windStrength
	 */
	public void setWindStrength(String windStrength) {
		this.windStrength = windStrength;
	}
	public String getWindStrength() {
		return windStrength;
	}
	
	/**
	 * 
	 * @param windDirection
	 */
	public void setWindDirection(String windDirection) {
		this.windDirection = windDirection;
	}
	public String getWindDirection() {
		return windDirection;
	}
	
	/**
	 * 
	 * @param season
	 */
	public void setSeason(String season) {
		this.season = season;
	}
	public String getSeason() {
		return season;
	}
	
	/**
	 * 
	 * @param deityInfluence
	 */
	public void setDeityInfluence(String deityInfluence) {
		this.deityInfluence = deityInfluence;
	}
	public String getDeityInfluence() {
		return deityInfluence;
	}

	/**
	 * 
	 * @param nearDeed
	 */
	public void setNearDeed(byte nearDeed) {
		this.nearDeed = nearDeed;
	}
	public byte getNearDeed() {
		return nearDeed;
	}
	
	
	/**
	 * 
	 * @param nearTower
	 */
	public void setNearTower(byte nearTower) {
		this.nearTower = nearTower;
	}
	public byte getNearTower() {
		return nearTower;
	}
}
