package StevenDimDoors.mod_pocketDim.helpers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.regex.Pattern;

import net.minecraft.util.WeightedRandom;
import net.minecraft.world.World;
import StevenDimDoors.mod_pocketDim.DDProperties;
import StevenDimDoors.mod_pocketDim.DimData;
import StevenDimDoors.mod_pocketDim.DungeonGenerator;
import StevenDimDoors.mod_pocketDim.LinkData;
import StevenDimDoors.mod_pocketDim.mod_pocketDim;
import StevenDimDoors.mod_pocketDim.dungeon.DungeonSchematic;
import StevenDimDoors.mod_pocketDim.items.itemDimDoor;
import StevenDimDoors.mod_pocketDim.util.WeightedContainer;

public class DungeonHelper
{
	private static DungeonHelper instance = null;
	private static DDProperties properties = null;
	public static final Pattern SchematicNamePattern = Pattern.compile("[A-Za-z0-9_\\-]+");
	public static final Pattern DungeonNamePattern = Pattern.compile("[A-Za-z0-9\\-]+");

	public static final String SCHEMATIC_FILE_EXTENSION = ".schematic";
	private static final int DEFAULT_DUNGEON_WEIGHT = 100;
	public static final int MAX_DUNGEON_WEIGHT = 10000; //Used to prevent overflows and math breaking down
	private static final int MAX_EXPORT_RADIUS = 50;
	public static final short MAX_DUNGEON_WIDTH = 2 * MAX_EXPORT_RADIUS + 1;
	public static final short MAX_DUNGEON_HEIGHT = 2 * MAX_EXPORT_RADIUS + 1;
	public static final short MAX_DUNGEON_LENGTH = 2 * MAX_EXPORT_RADIUS + 1;
	
	public static final int FABRIC_OF_REALITY_EXPORT_ID = 1973;
	public static final int PERMAFABRIC_EXPORT_ID = 220;
	
	private static final String HUB_DUNGEON_TYPE = "Hub";
	private static final String TRAP_DUNGEON_TYPE = "Trap";
	private static final String SIMPLE_HALL_DUNGEON_TYPE = "SimpleHall";
	private static final String COMPLEX_HALL_DUNGEON_TYPE = "ComplexHall";
	private static final String EXIT_DUNGEON_TYPE = "Exit";
	private static final String DEAD_END_DUNGEON_TYPE = "DeadEnd";
	private static final String MAZE_DUNGEON_TYPE = "Maze";
	
	//The list of dungeon types will be kept as an array for now. If we allow new
	//dungeon types in the future, then this can be changed to an ArrayList.
	private static final String[] DUNGEON_TYPES = new String[] {
		HUB_DUNGEON_TYPE,
		TRAP_DUNGEON_TYPE,
		SIMPLE_HALL_DUNGEON_TYPE,
		COMPLEX_HALL_DUNGEON_TYPE,
		EXIT_DUNGEON_TYPE,
		DEAD_END_DUNGEON_TYPE,
		MAZE_DUNGEON_TYPE
	};
	
	private Random rand = new Random();

	public ArrayList<DungeonGenerator> customDungeons = new ArrayList<DungeonGenerator>();
	public ArrayList<DungeonGenerator> registeredDungeons = new ArrayList<DungeonGenerator>();
	
	private ArrayList<DungeonGenerator> simpleHalls = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> complexHalls = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> deadEnds = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> hubs = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> mazes = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> pistonTraps = new ArrayList<DungeonGenerator>();
	private ArrayList<DungeonGenerator> exits = new ArrayList<DungeonGenerator>();

	public DungeonGenerator defaultBreak = new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/somethingBroke.schematic", true);
	public DungeonGenerator defaultUp = new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/simpleStairsUp.schematic", true);
	
	private HashSet<String> dungeonTypeChecker;
	private HashMap<String, ArrayList<DungeonGenerator>> dungeonTypeMapping;
	
	private DungeonHelper()
	{
		//Load the dungeon type checker with the list of all types in lowercase.
		//Capitalization matters for matching in a hash set.
		dungeonTypeChecker = new HashSet<String>();
		for (String dungeonType : DUNGEON_TYPES)
		{
			dungeonTypeChecker.add(dungeonType.toLowerCase());
		}
		
		//Add all the basic dungeon types to dungeonTypeMapping
		//Dungeon type names must be passed in lowercase to make matching easier.
		dungeonTypeMapping = new HashMap<String, ArrayList<DungeonGenerator>>();
		dungeonTypeMapping.put(SIMPLE_HALL_DUNGEON_TYPE.toLowerCase(), simpleHalls);
		dungeonTypeMapping.put(COMPLEX_HALL_DUNGEON_TYPE.toLowerCase(), complexHalls);
		dungeonTypeMapping.put(HUB_DUNGEON_TYPE.toLowerCase(), hubs);
		dungeonTypeMapping.put(EXIT_DUNGEON_TYPE.toLowerCase(), exits);
		dungeonTypeMapping.put(DEAD_END_DUNGEON_TYPE.toLowerCase(), deadEnds);
		dungeonTypeMapping.put(MAZE_DUNGEON_TYPE.toLowerCase(), mazes);
		dungeonTypeMapping.put(TRAP_DUNGEON_TYPE.toLowerCase(), pistonTraps);
		
		//Load our reference to the DDProperties singleton
		if (properties == null)
			properties = DDProperties.instance();
		
		registerCustomDungeons();
	}
	
	private void registerCustomDungeons()
	{
		File file = new File(properties.CustomSchematicDirectory);
		if (file.exists() || file.mkdir())
		{
			copyfile.copyFile("/mods/DimDoors/text/How_to_add_dungeons.txt", file.getAbsolutePath() + "/How_to_add_dungeons.txt");
		}
		importCustomDungeons(properties.CustomSchematicDirectory);
		registerBaseDungeons();
	}
	
	public static DungeonHelper initialize()
	{
		if (instance == null)
		{
			instance = new DungeonHelper();
		}
		else
		{
			throw new IllegalStateException("Cannot initialize DungeonHelper twice");
		}
		
		return instance;
	}
	
	public static DungeonHelper instance()
	{
		if (instance == null)
		{
			//This is to prevent some frustrating bugs that could arise when classes
			//are loaded in the wrong order. Trust me, I had to squash a few...
			throw new IllegalStateException("Instance of DungeonHelper requested before initialization");
		}
		return instance;
	}
	
	public LinkData createCustomDungeonDoor(World world, int x, int y, int z)
	{
		//Create a link above the specified position. Link to a new pocket dimension.
		LinkData link = new LinkData(world.provider.dimensionId, 0, x, y + 1, z, x, y + 1, z, true, 3);
		link = dimHelper.instance.createPocket(link, true, false);
		
		//Place a Warp Door linked to that pocket
		itemDimDoor.placeDoorBlock(world, x, y, z, 3, mod_pocketDim.ExitDoor);
		
		return link;
	}
	
	public boolean validateDungeonType(String type)
	{
		//Check if the dungeon type is valid
		return dungeonTypeChecker.contains(type.toLowerCase());
	}
	
	public boolean validateSchematicName(String name)
	{
		String[] dungeonData;
		
		if (!name.endsWith(SCHEMATIC_FILE_EXTENSION))
			return false;
		
		dungeonData = name.substring(0, name.length() - SCHEMATIC_FILE_EXTENSION.length()).split("_");

		//Check for a valid number of parts
		if (dungeonData.length < 3 || dungeonData.length > 4)
			return false;

		//Check if the dungeon type is valid
		if (!dungeonTypeChecker.contains(dungeonData[0].toLowerCase()))
			return false;
		
		//Check if the name is valid
		if (!SchematicNamePattern.matcher(dungeonData[1]).matches())
			return false;
		
		//Check if the open/closed flag is present
		if (!dungeonData[2].equalsIgnoreCase("open") && !dungeonData[2].equalsIgnoreCase("closed"))
			return false;
		
		//If the weight is present, check that it is valid
		if (dungeonData.length == 4)
		{
			try
			{
				int weight = Integer.parseInt(dungeonData[3]);
				if (weight < 0 || weight > MAX_DUNGEON_WEIGHT)
					return false;
			}
			catch (NumberFormatException e)
			{
				//Not a number
				return false;
			}
		}
		return true;
	}
	
	public void registerCustomDungeon(File schematicFile)
	{
		String name = schematicFile.getName();
		String path = schematicFile.getAbsolutePath();
		try
		{
			if (validateSchematicName(name))
			{
				//Strip off the file extension while splitting the file name
				String[] dungeonData = name.substring(0, name.length() - SCHEMATIC_FILE_EXTENSION.length()).split("_");
				
				String dungeonType = dungeonData[0].toLowerCase();
				boolean isOpen = dungeonData[2].equalsIgnoreCase("open");
				int weight = (dungeonData.length == 4) ? Integer.parseInt(dungeonData[3]) : DEFAULT_DUNGEON_WEIGHT;
				
				//Add this custom dungeon to the list corresponding to its type
				DungeonGenerator generator = new DungeonGenerator(weight, path, isOpen);

				dungeonTypeMapping.get(dungeonType).add(generator);
				registeredDungeons.add(generator);
				customDungeons.add(generator);
				System.out.println("Imported " + name);
			}
			else
			{
				System.out.println("Could not parse dungeon filename, not adding dungeon to generation lists");
				customDungeons.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, path, true));
				System.out.println("Imported " + name);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Failed to import " + name);
		}
	}

	public void importCustomDungeons(String path)
	{
		File directory = new File(path);
		File[] schematicNames = directory.listFiles();

		if (schematicNames != null)
		{
			for (File schematicFile: schematicNames)
			{
				if (schematicFile.getName().endsWith(SCHEMATIC_FILE_EXTENSION))
				{
					registerCustomDungeon(schematicFile);
				}
			}
		}
	}

	public void registerBaseDungeons()
	{
		hubs.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/4WayBasicHall.schematic", false));
		hubs.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/4WayHallExit.schematic", false));
		hubs.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/doorTotemRuins.schematic", true));
		hubs.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/hallwayTrapRooms1.schematic", false));
		hubs.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/longDoorHallway.schematic", false));
		hubs.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallRotundaWithExit.schematic", false));
		hubs.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/fortRuins.schematic", true));
		hubs.add(new DungeonGenerator(10, "/schematics/Hub_SK-Claustrophobia_Open_10.schematic", true));
		hubs.add(new DungeonGenerator(50, "/schematics/Hub_SK-HeartOfDisorder_Open_50.schematic", true));

		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/collapsedSingleTunnel1.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/singleStraightHall1.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallBranchWithExit.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallSimpleLeft.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallSimpleRight.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/simpleStairsUp.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/simpleStairsDown.schematic", false));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/simpleSmallT1.schematic", false));
		simpleHalls.add(new DungeonGenerator(50, "/schematics/SimpleHall_SK-LeftDownStairs_Open_50.schematic", true));
		simpleHalls.add(new DungeonGenerator(50, "/schematics/SimpleHall_SK-LeftUpPath_Open_50.schematic", true));
		simpleHalls.add(new DungeonGenerator(50, "/schematics/SimpleHall_SK-RightDownStairs_Open_50.schematic", true));
		simpleHalls.add(new DungeonGenerator(50, "/schematics/SimpleHall_SK-RightUpPath_Open_50.schematic", true));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/SimpleHall_SK-SpiralHallway_Open_100.schematic", true));
		simpleHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/complexHall_largeBrokenHall_closed_100.schematic", false));

		
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/tntPuzzleTrap.schematic", false));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/brokenPillarsO.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/buggyTopEntry1.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/exitRuinsWithHiddenDoor.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/hallwayHiddenTreasure.schematic", false));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/mediumPillarStairs.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/ruinsO.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/pitStairs.schematic", true));
		complexHalls.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/ComplexHall_SK-HiddenStairs_Open_100.schematic", true));
		complexHalls.add(new DungeonGenerator(10, "/schematics/ComplexHall_SK-LostGarden_Open_10.schematic", true));

		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/azersDungeonO.schematic", false));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/diamondTowerTemple1.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/fallingTrapO.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/hiddenStaircaseO.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/lavaTrapO.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/randomTree.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallHiddenTowerO.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallSilverfishRoom.schematic", false));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/tntTrapO.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallDesert.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallPond.schematic", true));
		deadEnds.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/DeadEnd_SK-FarAwayInTheDark_Open_100.schematic", true));
		deadEnds.add(new DungeonGenerator(50, "/schematics/DeadEnd_SK-UnstableDesert_Open_50.schematic", true));

		pistonTraps.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/hallwayPitFallTrap.schematic", false));
		pistonTraps.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/pistonFloorHall.schematic", false));
		pistonTraps.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/wallFallcomboPistonHall.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/fakeTNTTrap.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/pistonFallRuins.schematic", true));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/pistonSmasherHall.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/simpleDropHall.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/fallingTNThall.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/lavaPyramid.schematic", true));
		pistonTraps.add(new DungeonGenerator(10, "/schematics/Trap_SK-RestlessCorridor_Open_10.schematic", true));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/trap_pistonFloorPlatform_closed_100.schematic", false));
		pistonTraps.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT/2, "/schematics/trap_pistonFloorPlatform2_closed_100.schematic", false));

		mazes.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallMaze1.schematic", false));
		mazes.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallMultilevelMaze.schematic", false));

		exits.add(new DungeonGenerator(2 * DEFAULT_DUNGEON_WEIGHT, "/schematics/lockingExitHall.schematic", false));
		exits.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/exitCube.schematic", true));
		exits.add(new DungeonGenerator(DEFAULT_DUNGEON_WEIGHT, "/schematics/smallExitPrison.schematic", true));
		
		registeredDungeons.addAll(simpleHalls);
		registeredDungeons.addAll(exits);
		registeredDungeons.addAll(pistonTraps);
		registeredDungeons.addAll(mazes);
		registeredDungeons.addAll(deadEnds);
		registeredDungeons.addAll(complexHalls);
		registeredDungeons.addAll(hubs);
	}

	public boolean exportDungeon(World world, int centerX, int centerY, int centerZ, String exportPath)
	{
		//Write schematic data to a file
		try
		{
			DungeonSchematic dungeon = DungeonSchematic.copyFromWorld(world,
					centerX - MAX_EXPORT_RADIUS, centerY - MAX_EXPORT_RADIUS, centerZ - MAX_EXPORT_RADIUS,
					MAX_DUNGEON_WIDTH, MAX_DUNGEON_HEIGHT, MAX_DUNGEON_LENGTH, true);
			dungeon.applyExportFilters(properties);
			dungeon.writeToFile(exportPath);
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public void generateDungeonLink(LinkData incoming)
	{
		DungeonGenerator dungeon;
		int depth = dimHelper.instance.getDimDepth(incoming.locDimID);
		int depthWeight = rand.nextInt(depth + 2) + rand.nextInt(depth + 2) - 2;

		
		int count = 10;
		boolean flag = true;
		try
		{
			
			if (incoming.destYCoord > 15)
			{
				do
				{
					count--;
					flag = true;
					//Select a dungeon at random, taking into account its weight
					dungeon = getRandomDungeon(rand, registeredDungeons);

					if (depth <= 1)
					{
						if(rand.nextBoolean())
						{
							dungeon = complexHalls.get(rand.nextInt(complexHalls.size()));

						}
						else if(rand.nextBoolean())
						{
							dungeon = hubs.get(rand.nextInt(hubs.size()));

						}
						else  if(rand.nextBoolean())
						{
							dungeon = hubs.get(rand.nextInt(hubs.size()));

						}
						else if(deadEnds.contains(dungeon)||exits.contains(dungeon))
						{
							flag=false;
						}
					}
					else if (depth <= 3 && (deadEnds.contains(dungeon) || exits.contains(dungeon) || rand.nextBoolean()))
					{
						if(rand.nextBoolean())
						{
							dungeon = hubs.get(rand.nextInt(hubs.size()));

						}
						else if(rand.nextBoolean())
						{
							dungeon = mazes.get(rand.nextInt(mazes.size()));
						}
						else if(rand.nextBoolean())
						{
							dungeon = pistonTraps.get(rand.nextInt(pistonTraps.size()));

						}
						else
						{
							flag = false;
						}
					}
					else if (rand.nextInt(3) == 0 && !complexHalls.contains(dungeon))
					{
						if (rand.nextInt(3) == 0)
						{
							dungeon = simpleHalls.get(rand.nextInt(simpleHalls.size()));
						}
						else if(rand.nextBoolean())
						{
							dungeon = pistonTraps.get(rand.nextInt(pistonTraps.size()));
						}
						else if (depth < 4)
						{
							dungeon = hubs.get(rand.nextInt(hubs.size()));
						}
					}
					else if (depthWeight - depthWeight / 2 > depth -4 && (deadEnds.contains(dungeon) || exits.contains(dungeon)))
					{
						if(rand.nextBoolean())
						{
							dungeon = simpleHalls.get(rand.nextInt(simpleHalls.size()));
						}
						else if(rand.nextBoolean())
						{
							dungeon = complexHalls.get(rand.nextInt(complexHalls.size()));
						}
						else if(rand.nextBoolean())
						{
							dungeon = pistonTraps.get(rand.nextInt(pistonTraps.size()));
						}
						else	
						{
							flag = false;
						}
					}
					else if (depthWeight > 7 && hubs.contains(dungeon))
					{
						if(rand.nextInt(12)+5<depthWeight)
						{
							if(rand.nextBoolean())
							{
								dungeon = exits.get(rand.nextInt(exits.size()));
							}
							else if(rand.nextBoolean())
							{
								dungeon = deadEnds.get(rand.nextInt(deadEnds.size()));
							}
							else
							{
								dungeon = pistonTraps.get(rand.nextInt(pistonTraps.size()));
							}
						}
						else
						{
							flag = false;
						}
					}
					else if (depth > 10 && hubs.contains(dungeon))
					{
						flag = false;
					}
					
					if(getDungeonDataInChain(dimHelper.dimList.get(incoming.locDimID)).contains(dungeon))
					{
						flag=false;
					}
				}
				while (!flag && count > 0);
			}
			else
			{
				dungeon = defaultUp;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			if (registeredDungeons.size() > 0)
			{
				//Select a random dungeon
				dungeon = getRandomDungeon(rand, registeredDungeons);
			}
			else
			{
				return;
			}
		}
		dimHelper.dimList.get(incoming.destDimID).dungeonGenerator = dungeon;
	}

	public Collection<String> getDungeonNames() {

		//Use a HashSet to guarantee that all dungeon names will be distinct.
		//This shouldn't be necessary if we keep proper lists without repetitions,
		//but it's a fool-proof workaround.
		HashSet<String> dungeonNames = new HashSet<String>();
		dungeonNames.addAll( parseDungeonNames(registeredDungeons) );
		dungeonNames.addAll( parseDungeonNames(customDungeons) );
		
		//Sort dungeon names alphabetically
		ArrayList<String> sortedNames = new ArrayList<String>(dungeonNames);
		Collections.sort(sortedNames);
		return sortedNames;
	}
	
	private static ArrayList<String> parseDungeonNames(ArrayList<DungeonGenerator> dungeons)
	{
		String name;
		File schematic;
		ArrayList<String> names = new ArrayList<String>(dungeons.size());
		
		for (DungeonGenerator dungeon : dungeons)
		{
			//Retrieve the file name and strip off the file extension
			schematic = new File(dungeon.schematicPath);
			name = schematic.getName();
			name = name.substring(0, name.length() - SCHEMATIC_FILE_EXTENSION.length());
			names.add(name);
		}
		return names;
	}
	
	private static DungeonGenerator getRandomDungeon(Random random, Collection<DungeonGenerator> dungeons)
	{
		//Use Minecraft's WeightedRandom to select our dungeon. =D
		ArrayList<WeightedContainer<DungeonGenerator>> weights =
				new ArrayList<WeightedContainer<DungeonGenerator>>(dungeons.size());
		for (DungeonGenerator dungeon : dungeons)
		{
			weights.add(new WeightedContainer<DungeonGenerator>(dungeon, dungeon.weight));
		}
		
		@SuppressWarnings("unchecked")
		WeightedContainer<DungeonGenerator> resultContainer = (WeightedContainer<DungeonGenerator>) WeightedRandom.getRandomItem(random, weights);
		return 	(resultContainer != null) ? resultContainer.getData() : null;
	}
	public static ArrayList<DungeonGenerator> getDungeonDataInChain(DimData dimData)
	{
		DimData startingDim = dimHelper.dimList.get(dimHelper.instance.getLinkDataFromCoords(dimData.exitDimLink.destXCoord, dimData.exitDimLink.destYCoord, dimData.exitDimLink.destZCoord, dimData.exitDimLink.destDimID).destDimID);

		return getDungeonDataBelow(startingDim);
	}
	private static ArrayList<DungeonGenerator> getDungeonDataBelow(DimData dimData)
	{
		ArrayList<DungeonGenerator> dungeonData = new ArrayList<DungeonGenerator>();
		if(dimData.dungeonGenerator!=null)
		{
			dungeonData.add(dimData.dungeonGenerator);
			
			for(LinkData link : dimData.getLinksInDim())
			{
				if(dimHelper.dimList.containsKey(link.destDimID))
				{
					if(dimHelper.dimList.get(link.destDimID).dungeonGenerator!=null&&dimHelper.instance.getDimDepth(link.destDimID)==dimData.depth+1)
					{
						for(DungeonGenerator dungeonGen :getDungeonDataBelow(dimHelper.dimList.get(link.destDimID)) )
						{
							if(!dungeonData.contains(dungeonGen))
							{
								dungeonData.add(dungeonGen);
							}
						}
					}
				}
			}
		}
		return dungeonData;
	}
}