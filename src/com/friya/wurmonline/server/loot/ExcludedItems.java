package com.friya.wurmonline.server.loot;

/*
//excluded templates as loot:
//	0,										// body
//	9,										// log
//	10,11,12,13,14,15,16,17,18,19,			// bodyparts
//	50,51,52,53,54,55,56,57,58,59,60,61,	// coins
//	73,										// lye (liquid)
//	74 pile
//	128 water
//	142 milk
//	166 write of ownership
//	176	ebony wand
//	177 pile of items
//	178 oven
//	179 unfinished item
//	180 forge
//	186 cart
//	211 deed size
//	226 floor loom
//	234, 237,238,239,242,244,245,253,254	size deed
//	236 deed token
//	272 corpse
* 	289	raft
* 	315	ivory wand
* 	322,323,324,325	altars
* 	327, 328 huge altars
* 	329, 330, 331,332,333,334,335,336,337,338,339,340	artifacts
* 	344	marker
* 	345,346,348,352	fluid? food
* 	384 guardtower
* 	386 unfinished item
* 	387	illusionary item
* 	398,399,400,401,402,403,404,405,406,407,408		statues and other huge masonry items
* 	427,428 lemonade, jam = fluid
* 	430 guardtower
* 	431,432,433,434,435,438 = dye
* 	437 tannin
* 	442 julbord
* 	445	catapult
* 	458	archery target
* 	484 bed
* 	490 row boat
* 	491 sailing boat
* 	510,511,512,513	mailboxes
* 	514	whip of one (was to be an artifact)
* 	518	colossus
* 	520	firemarker
*	521 lair
*	528 guard tower
*	529,530,531,532,533,534,535,536,537	royal items
*	538 stone of the sword
*	539,540,541,542,543 cart and boats
*	552 tall mast
*	576	tub
*	580 market stall
*	584,585,586,587,588,589,590	rigs and masts
*	592,593,594,595,596	mine doors
*	603,604,605,606,607	portals
*	608	well
*	609	spring
*	635	ornate fountain
*	638	guard tower
*	654 transmutation liquid (fluid)
*	661,662	FSB, BSB
*	663 settlement form
*	669 bulk item
*	670 trash heap
*	671 deed border
*	672 = 'decayitem'
*	673 = 'Perimeter'
*	676 = 'mission ruler'
*	678 = 'Fo obelisk'
*	679 = 'Construction marker'
*	682 = 'declaration of independence'
*	714 = 'obelisk'
*	715 = 'temple'
*	716 = 'spirit gate'
*	717 = 'foundation pillar'
*	722 = 'bell tower'
*	726 = 'ring center'
*	727 = 'duelling ring'
*	728 = 'ring corner'
*	732 = 'epic portal'
*	733 = 'huge epic portal'
*	736 = 'pillar'
*	739 = 'Hota pillar'
*	741 = 'shrine of the rush'
*	742 = 'hota statue'
*	751 = 'mission ruler recharge'
*	759 = 'armour stand'
*	760 = 'outpost'
*	761 = 'battle camp'
*	762 = 'fortification'
*	763 = 'source'	(fluid)
*	766 = 'source fountain'
*	767 = 'source spring'
*	775 = 'staircase'
*	811 = 'statue of horse' 
*	823 = 'equipmentslot'
*	824 = 'group'
*	835 = 'village recruitment board'
*	842 = 'marble brazier pillar'
*	843 = 'name change certificate'
*	845 = 'water marker'
*	850 = 'wagon'
*	851 = 'small crate'
*	852 = 'large crate'
*	853 = 'ship transporter'
*	854 = 'tutorial object'
*	855 = 'steel portal'
*	869 = 'Colossus of Vynora'
*	870 = 'Colossus of Magranon'
*	889 = 'open fireplace'
*	890 = 'canopy bed'
*	891 = 'bench'
*	892 = 'wardrobe'
*	893 = 'coffer'
*	894 = 'royal throne'
*	905 = 'stone keystone'
*	906 = 'marble keystone'
*	907 = 'Colossus of Fo'
*	911 = 'high bookshelf'
*	912 = 'low bookshelf'
*	913 = 'fine high chair'
*	914 = 'high chair'
*	915 = 'paupers high chair'
*	916 = 'Colossus of Libila'
*	922 = 'spinning wheel'
*	923 = 'lounge chair'
*	924 = 'royal lounge chaise'
*	927 = 'cupboard'
*	928 = 'round marble table'
*	929 = 'rectangular marble table'
*	931 = 'siege shield'
*	936 = 'ballista'
*	937 = 'trebuchet'
*	938 = 'spike barrier'
*	939 = 'archery tower'
*	940,941,942,968	turrets
*	969,970,971	supply depots
*	981,982,983,984	challenge statues
*	995 = 'treasure chest'
*	996 = 'neutral guard tower'
*	1000 = 'ownership papers'
*	1016 = 'Stone of Soulfall'
*	1023 = 'kiln'
*	1026 = 'unstable source rift'
*	1045 = 'rift altar'
*	1046,1047,1048 = 'rift device'
*	1091 = 'metallic liquid' (fluid?)
*	1098 = 'returner tool chest'
*/

public interface ExcludedItems
{
	public static final int[] largeItems = new int[]{
			178, 179, 180, 186, 226, 289, 322, 323, 324, 325, 327, 328, 384, 385, 398, 399,		// 385 = felled tree, added late 
			400, 401, 402, 403, 404, 405, 406, 407, 408, 430, 445, 458,
			484, 490, 491, 510, 511, 512, 513, 518, 528,
			539, 540, 541, 542, 543, // boats
			552, 576, 580,
			584, 585, 586, 587, 588, 589, 590,	// rigs and masts
			592, 593, 594, 595, 596, // minedoors
			603, 604, 605, 606, 607,	// portals
			608,	// well
			609,	// spring
			635,	// ornate fountain
			638,	// guard tower
			661, 662,	// FSB, BSB
			670,
			678,
			714, //= 'obelisk'
			715, //= 'temple'
			716, //= 'spirit gate'
			717, //= 'foundation pillar'
			722, //= 'bell tower'
			732, //= 'epic portal'
			733, //= 'huge epic portal'
			736, //= 'pillar'
			739, //= 'Hota pillar'
			741, //= 'shrine of the rush'
			742, //= 'hota statue'
			759, //= 'armour stand'
			775, //= 'staircase'
			811, //= 'statue of horse' 
			835, //= 'village recruitment board'
			842, //= 'marble brazier pillar'
			850, //= 'wagon'
			851, //= 'small crate'
			852, //= 'large crate'
			853, //= 'ship transporter'
			855, //= 'steel portal'
			869, //= 'Colossus of Vynora'
			870, //= 'Colossus of Magranon'
			889, //= 'open fireplace'
			890, //= 'canopy bed'
			891, //= 'bench'
			892, //= 'wardrobe'
			893, //= 'coffer'
			894, //= 'royal throne'
			905, //= 'stone keystone'
			906, //= 'marble keystone'
			907, //= 'Colossus of Fo'
			911, //= 'high bookshelf'
			912, //= 'low bookshelf'
			913, //= 'fine high chair'
			914, //= 'high chair'
			915, //= 'paupers high chair'
			916, //= 'Colossus of Libila'
			922, //= 'spinning wheel'
			923, //= 'lounge chair'
			924, //= 'royal lounge chaise'
			927, //= 'cupboard'
			928, //= 'round marble table'
			929, //= 'rectangular marble table'
			931, //= 'siege shield'
			936, //= 'ballista'
			937, //= 'trebuchet'
			938, //= 'spike barrier'
			939, //= 'archery tower'
			940,941,942,968,	//turrets
			981,982,983,984,	//challenge statues
			996, //= 'neutral guard tower'
			1016, //= 'Stone of Soulfall'
			1023, //= 'kiln'
			1026, //= 'unstable source rift'
			1045, //= 'rift altar'
			1046,1047,1048 //= 'rift device'
	};

	public static final int[] fluidItems = new int[] {
			73, 128, 142, 345, 346, 348, 352, 427, 428, 431, 432, 433, 434, 435, 438, 437,
			654, // transmutation liquid (fluid)
			763, // = 'source'	(fluid)
			1091 // = 'metallic liquid' (fluid?)
	};
	
	public static final int[] functionalItems = new int[]{
			0, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,	// body parts
			74, 166, 177, 211, 234, 237, 238, 239, 242, 244, 245, 253, 254, 236, 272, 344, 386, 387, 520, 521, 538, 663, 669,
			671, // deed border
			672, //= 'decayitem'
			673, //= 'Perimeter'
			679,
			726, //= 'ring center'
			727, //= 'duelling ring'
			728, //= 'ring corner'
			731, // tree stump
			737, // valrei mission item
			760, //= 'outpost'
			761, //= 'battle camp'
			762, //= 'fortification'
			766, //= 'source fountain'
			767, //= 'source spring'
			823, //= 'equipmentslot'
			824, //= 'group'
			845, //= 'water marker'
			854, //= 'tutorial object'
			969,970,971,	//supply depots
			995, //= 'treasure chest'
			1000 //= 'ownership papers'
	};

	public static final int[] balanceItems = new int[]{
			50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,	// coins
			329, 330, 331, 332, 333, 334, 335, 336, 337, 338, 339, 340, // artifacts
			442,	// julbord
			514,	// whip of one (was to be an artifact, never made it into game)
			529, 530, 531, 532, 533, 534, 535, 536, 537,	// royal items
			682,		// decl of indipendance
			843, // = 'name change certificate'
			1098 // = 'returner tool chest'
		};
	
	public static final int[] adminItems = new int[]{
			176, 315,
			676, // = 'mission ruler'
			751 //= 'mission ruler recharge'
	};
}
