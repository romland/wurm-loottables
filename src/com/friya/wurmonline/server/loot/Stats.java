package com.friya.wurmonline.server.loot;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.TreeMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.wurmonline.server.Players;
import com.wurmonline.server.creatures.Communicator;

/*
I started dabbling with WU again and wanted a bit of new functionality. Sooo.... here's an update to this ... thing.

Get some numbers on what is happening with your loot rules (etc).

You get statistics by typing /lootstats as a GM (power 1+). This will show the stats in the player's Event log as well as in the server log.

Any entries not shown when viewing the stats are still 0. That is, a stat will only appear if it's happened at least once.

Provided there is activity on the server, statistics will automatically be output to the server log (as INFO) every hour-ish (can be useful for auditing over time, or generate charts).

This screenshot gives an idea of what I added. This list will obviously become a lot longer on a live server, for this test I had a small number of rules and spent two minutes killing four creatures.
< https://i.imgur.com/bWWNQyy.png >


Tracked data points

killed.players                  Total number of times players died on the server
killed.npcs                     Total number of times NPCs died on the server
killed.player.[NAME]            Number of times a specific player was killed
killed.npc.[NAME]               Number of times a specific NPC-type was killed
killed.ignored                  Number of suicides or no [valid] players involved in the death of a creature/player 
                                or some other reason disqualifying it from giving loot (an invalid player can be e.g. 
                                an invisible or invulnerable GM. Think: the target must be 'red' before dying)

table.triggered.[ID]            Number of times this loot-table was triggered

rule.cancelled.[ID].[NAME]      Number of times the rule was ignored due to maximum number of items it could drop 
                                was set to 0
rule.triggered.[ID].[NAME]      Number of times the rule was triggered

drop.total                      Total number of items handed out
drop.total.enchanted.[NAME]     Total number of this type of enchantment on items
drop.total.guaranteed           Total number of items handed out that had drop chance set to 100%
drop.total.rares                Total number of items handed out that were rare or better

drop.dropped.[ID].[NAME] [*]    The number of items that dropped. This can differ from item.drop.triggered because more 
                                than one clone may drop.
drop.missed.[ID].[NAME]	[*]     How many times the item was a valid drop, but RNG screwed the player out of the item :)
drop.triggered.[ID].[NAME] [*]  Number of times (an) item was dropped

mods.calls                      Number of times external mods utilizing LootTables were notified about drops
mods.cancelled                  Number of times an external mod utilizing LootTables prevented an NPC from yielding loot
mods.exceptions                 Number of times an external mod threw an exception (thus preventing any drop)

query.total.lootrules           Number of queries to LootRules table

misc.players.online             Number of players online at the time of printing these stats. Not related to LootTables 
                                per se, but can be useful if you want to generate charts (or whatever) based on the 
                                periodical output to server log.


[*] For some of the drop.*, the [NAME] will only be included if the LootItem had a forced name. So, if empty, you'd use [ID] to find out which item it's about.


The link in the original post is updated with latest version.

And well, that's it. Enjoy :)
*/

public class Stats
{
	private static Logger logger = Logger.getLogger(Stats.class.getName());

	static long startTime = 0;
	static long lastLogOutput = 0;
	
	static Map<String, Long> miscStats = new TreeMap<String, Long>();
	final static private DateTimeFormatter compactDateTimeFormatter = DateTimeFormatter.ofPattern("ddMMMyyyy HH:mm");


	static void outputPeriodically()
	{
		if((lastLogOutput + (60 * 60 * 1000)) < System.currentTimeMillis()) {
			lastLogOutput = System.currentTimeMillis();
			output(null, true);
		}
	}


	static void inc(String statName)
	{
		if(miscStats.containsKey(statName) == false) {
			miscStats.put(statName, 0L);
		}

		miscStats.put(statName, miscStats.get(statName) + 1);
	}


	static void inc(String statName, int count)
	{
		if(miscStats.containsKey(statName) == false) {
			miscStats.put(statName, 0L);
		}

		miscStats.put(statName, miscStats.get(statName) + count);
	}
	
	static void set(String statName, long count)
	{
		miscStats.put(statName, count);
	}


	static void reset()
	{
		miscStats.clear();
		startTime = System.currentTimeMillis();
	}


	/**
	 * This is somewhat convoluted because in the log I want to use newlines. 
	 * When outputting to client, I do not have this option.
	 * 
	 * @param c
	 */
	static void output(Communicator c, boolean logOnly)
	{
		StringBuffer msgs = new StringBuffer();

		msgs.append("==== LootTable statistics at " + millisecondsToCompactDateTime(System.currentTimeMillis()) + ". Gathering started at " + millisecondsToCompactDateTime(startTime) + " ====\n");

		set("misc.players.online", Players.getInstance().getNumberOfPlayers());

		for (Map.Entry<String, Long> entry : miscStats.entrySet()) {
			msgs.append("    " + entry.getKey() + " = " + entry.getValue() + "\n");
		}

		msgs.append("==== End of LootTable statistics ====\n");

		if(!logOnly) {
			String lines[] = msgs.toString().split("\\n");
			for(int i = 0; i < lines.length; i++) { 
				if((i % 2) == 0) {
					c.sendNormalServerMessage(lines[i]);
				} else {
					c.sendSafeServerMessage(lines[i]);
				}
			}
		}

		logger.log(Level.INFO, "\n" + msgs.toString());
	}


	static String millisecondsToCompactDateTime(long ms)
	{
		return new Timestamp(ms).toLocalDateTime().format(compactDateTimeFormatter);
	}
}
