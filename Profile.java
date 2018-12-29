package com.acme.profile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * The Profile class implements "Poor Man's Profiling" for time and memory.
 * The class is self-contained and can be inserted into any java code that 
 * is in-development for debugging and informational purposes.  No external
 * libraries are required by this class, only core java is used.
 * 
 * Please note that this class is NOT a substitute for a real java profiler
 * (like JProfiler).
 * 
 * 
 * PROFILE NAME:
 *   Each public API method in this class is associated with a profileName.
 *   The profileName can be associated explicitely in each API call or implicitely 
 *   with a default-value.  When the profileName is implicit, it is associated by 
 *   using the constant DEFAULT_PROFILE_NAME or by using the class name of an input
 *   object.
 * 
 * The following is a description of the five API sections of this class,
 * along with an example for each section.
 * 
 * 
 *   1. COUNTER API
 *   
 *      A counter manages the number of times that something occurs.  It can
 *      be used to store the number of times a method was called or used to 
 *      store the number of times a loop was executed, etc.
 * 
 *      Each counter is uniquely identified by a profileName and a counterName.
 *      
 *      Example:
 *                Profile.counterInc("counter-1");             // increment counter-1
 *                long val = Profile.counterGet("counter-1");  // get counter-1 value
 *                String report = Profile.reportAllCounters(); // get counter report
 *                Profile.counterReset("counter-1");           // reset counter-1 value
 * 
 *   
 *   2. CLOCK API
 *   
 *      A clock maintains the elapsed time (in milliseconds) between start
 *      and stop events.  The elapsed time is accumulated between successive
 *      start and stops.  For example, if a method is called 100 times and has
 *      a clock-start at its beginning and a clock-stop at its end, the elapsed
 *      time of all 100 calls will be stored by the clock.
 *      
 *      Each clock is uniquely identified by a profileName and a clockName.
 *      
 *      Example: 
 *                Profile.clockStart("clock-1");                     // start the clock
 *                ... other code executed here
 *                Profile.clockStop("clock-1");                      // stop the clock
 *                long elapsed = Profile.clockGetElapsed("clock-1"); // get elapsed time
 *                long numOfStarts = 
 *                          Profile.clockGetStartNumber("clock-1");  // get starts
 *                String report = Profile.reportAllClocks();         // get clock report
 *                Profile.clockReset("clock-1");                     // reset clock
 *   
 *   
 *   3. CHAIN API
 *   
 *      A chain maintains free-memory available in the JVM and elapsed time 
 *      between specific events called links.   A new link is always added to
 *      the END of the chain.  A link captures the free-memory availabe in the
 *      JVM and a timestamp.  This allows the chain/link reports to capture the
 *      differences in free-memory and elapsed time between the links.  The best
 *      use of a chain is to see how much memory was used in the JVM (or returned)
 *      in a method or program.   Note that a chain/link does not accumulate
 *      values during successive calls (like a clock does).  Each link added to 
 *      a chain is a separate event, even if it has the same name as a previous
 *      link.
 *      
 *      Each chain/link is uniquely identified by a profileName, chainName, and
 *      linkName.
 *      
 *      Example:
 *                // add a link at the start of the insertPurchaseOrder method
 *                Profile.chainAddLink("insertPurchaseOrder", "begin");
 *                
 *                ... other code executed here
 *                
 *                // add a link in the middle of the insertPurchaseOrder method
 *                Profile.chainAddLink("insertPurchaseOrder", "middle");
 *                
 *                ... other code executed here
 *                
 *                // add a link at the end of the insertPurchaseOrder method
 *                Profile.chainAddLink("insertPurchaseOrder", "end");
 *                
 *                // get the chain report for the insertPurchaseOrder method
 *                String report = reportChain("insertPurchaseOrder");
 *                
 *                // reset the chain values
 *                Profile.chainReset("insertPurchaseOrder");
 *   
 *   
 *   4. CACHE API
 *   
 *      A cache stores java objects in a HashMap for debugging.   Each object
 *      is added to the cache with a timestamp, which represents when the object
 *      was added.  Each object is represented in a cache report by the value 
 *      returned from toString().
 *      
 *      Each cache is uniquely identified by a profileName and a cacheName.
 *      
 *      Example:
 *                // add a new Person object to cache-1 with key "person-Bob"
 *                cacheAddObject("cache-1", "person-Bob", new Person("Bob"));
 *                
 *                // add a new Person object to cache-1 with no key
 *                cacheAddObject("cache-1", null, new Person("Mary"));
 * 
 *                // get objects from cache-1
 *                List<Object> oList = cacheGetObjects("cache-1");
 *                
 *                // get the cache report for cache-1
 *                String report = Profile.reportCache("cache-1");
 *    
 *    
 *   4. REPORT API
 *   
 *      The report api contains the method calls to retrieve report strings for
 *      counters, clocks, and chains.
 *      
 *      Example:
 *                String counterReport = Profile.reportAllCounters();
 *                String clockReport = Profile.reportAllClocks();
 *                String chainReport = Profile.reportProfileChains("profile-1");
 *                String chainReport = Profile.reportChain("profile-1", "chain-1");
 *   
 *   
 *   5. MISCELLANEOUS API
 * 
 *      The miscellaneous api contains methods for resetting an entire profile
 *      and a method for invoking a GC (Garbage Collection).
 * 
 *      Example:
 *                Profile.gc();                    // Perform Garbage Collection
 *                Profile.profileReset();          // reset values for DEFAULT_PROFILE_NAME
 *                Profile.profileReset("prof-1");  // reset values for prof-1
 *                Profile.resetAllProfiles();      // reset values for all profiles
 *   
 *   
 *   6. GLOBAL VARIABLES
 *   
 *      The Profile class utilizes the following global variables to affect behavior:
 *      
 *         A. ENABLED - Defaults to true.
 *            When false, disables the Profile class.
 *            
 *         B. DEFAULT_PROFILE_NAME - Defaults to "Profile1"
 *            The default profile name.
 *            
 *         C. SEPARATE_PROFILES_BY_NEWLINE - Defaults to true.
 *            When true, adds an extra blank line between profiles in reports.
 *             
 *         D. SEPARATE_CHAINS_BY_NEWLINE - Defaults to true.
 *            When true, adds an extra blank line between chains in reports.
 *            
 *         E. CHAIN_TOTALS_ROW - Defaults to true.
 *            When true, totals the values for free-memory differences 
 *            and elapsed-time differences in chain reports.
 *            
 *         F. CHAIN_MEGABYTE_LEGEND - Defaults to true.
 *            When true, adds an extra column (in Megabytes) for free-memory differences.
 *            
 *         G. CACHE_SHOW_OBJECT_HEADER - Defaults to true.
 *            When true, displays a header for each cache object in a Cache Report.
 * 
 * Accessor Analogies:
 *   1. clock   = time elapsed analogy - for storing elapsed time.
 *   2. counter = counting analogy - for storing the number of times something occurred.
 *   3. chain   = analogy for series of chain links - for storing memory/timestamps.
 */

public class Profile {
	
	/** Variables that can be modified at runtime. */
	public static boolean ENABLED = true;                      // Enable/Disable this class
	public static String DEFAULT_PROFILE_NAME = "Profile1";    // Default profile
	public static boolean SEPARATE_PROFILES_BY_NEWLINE = true; // For reporting
	public static boolean SEPARATE_CHAINS_BY_NEWLINE = true;   // For reporting
	public static boolean CHAIN_TOTALS_ROW = true;             // For reporting
	public static boolean CHAIN_MEGABYTE_LEGEND = true;        // For Reporting
	public static boolean CACHE_SHOW_OBJECT_HEADER = true;     // For Reporting

	/** Constants */
	private static final int LEFT = 1;
	private static final int RIGHT = 2;
	private static final String SPACE = " ";
	private static final char SPACE_CHAR = ' ';
	private static final String NEWLINE = "\n";
	private static final String DISABLED = "DISABLED";
	private static final String EMPTY_STR = "";
	private static final String DATE_FORMAT_HHMMSSS = "MM/dd/yyyy hh:mm:ss:S";
	private static final long BYTES_PER_MEGABYTE = 1048576;
	
	/** Static Fields */
	private static HashMap<String,ProfileObject> profileMap = new HashMap<String,ProfileObject>();

	
	/** Private Constructor - no instances */
	private Profile(String profileName) { }
	

	////////////////////////////////////////////////////////////
	//
	// COUNTER API - A counter manages the number of times something occurred.
	//
	
	/** Increments the value for a counter. */
	public static void counterInc(String counterName)                     { counterIncImpl(DEFAULT_PROFILE_NAME, counterName); }
	public static void counterInc(Object o, String counterName)           { counterIncImpl(getSimpleClassName(o), counterName); }
	public static void counterInc(String profileName, String counterName) { counterIncImpl(profileName, counterName); }
	
	/** Sets the value for a counter. */
	public static void counterSet(String counterName, long value)                     { counterSetImpl(DEFAULT_PROFILE_NAME, counterName, value); }
	public static void counterSet(Object o, String counterName, long value)           { counterSetImpl(getSimpleClassName(o), counterName, value); }
	public static void counterSet(String profileName, String counterName, long value) { counterSetImpl(profileName, counterName, value); }

	/** Returns the value for a counter. */
	public static long counterGet(String counterName)                     { return counterGetImpl(DEFAULT_PROFILE_NAME, counterName); }
	public static long counterGet(Object o, String counterName)           { return counterGetImpl(getSimpleClassName(o), counterName); }
	public static long counterGet(String profileName, String counterName) { return counterGetImpl(profileName, counterName); }
	
	/** Resets the value for a counter */
	public static void counterReset(String counterName)                     { counterResetImpl(DEFAULT_PROFILE_NAME, counterName); }
	public static void counterReset(Object o, String counterName)           { counterResetImpl(getSimpleClassName(o), counterName); }
	public static void counterReset(String profileName, String counterName) { counterResetImpl(profileName, counterName); }

	
	////////////////////////////////////////////////////////////
	//
	// CLOCK API - A clock manages time elapsed.
	//
	
	/** Starts time for a clock. */
	public static void clockStart(String clockName)                     { clockStartImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static void clockStart(Object o, String clockName)           { clockStartImpl(getSimpleClassName(o), clockName); }
	public static void clockStart(String profileName, String clockName) { clockStartImpl(profileName, clockName); }


	/** Stops time for a clock. */
	public static void clockStop(String clockName)                     { clockStopImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static void clockStop(Object o, String clockName)           { clockStopImpl(getSimpleClassName(o), clockName); }	
	public static void clockStop(String profileName, String clockName) { clockStopImpl(profileName, clockName); }


	/** Returns the elapsed time for a clock */
	public static long clockGetElapsed(String clockName)                     { return clockGetElapsedImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static long clockGetElapsed(Object o, String clockName)           { return clockGetElapsedImpl(getSimpleClassName(o), clockName); }
	public static long clockGetElapsed(String profileName, String clockName) { return clockGetElapsedImpl(profileName, clockName); }
	

	/** Returns the number of times clockStart was called for a clock. */
	public static long clockGetStartNumber(String clockName)                     { return clockGetStartNumImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static long clockGetStartNumber(Object o, String clockName)           { return clockGetStartNumImpl(getSimpleClassName(o), clockName); }
	public static long clockGetStartNumber(String profileName, String clockName) { return clockGetStartNumImpl(profileName, clockName); }	
	
	
	/** Returns the number of times clockStop was called for a clock. */
	public static long clockGetStopNumber(String clockName)                     { return clockGetStopNumberImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static long clockGetStopNumber(Object o, String clockName)           { return clockGetStopNumberImpl(getSimpleClassName(o), clockName); }
	public static long clockGetStopNumber(String profileName, String clockName) { return clockGetStopNumberImpl(profileName, clockName); }	
	
	
	/** Resets the values for a clock. */
	public static void clockReset(String clockName)                     { clockResetImpl(DEFAULT_PROFILE_NAME, clockName); }
	public static void clockReset(Object o, String clockName)           { clockResetImpl(getSimpleClassName(o), clockName); }
	public static void clockReset(String profileName, String clockName) { clockResetImpl(profileName, clockName); }

	
	////////////////////////////////////////////////////////////
	//
	// CHAIN/LINK API - A chain manages links.  Each link stores a timestamp and free memory in the JVM.
	//

	/** Adds a link to the end of a chain.  The link stores a timestamp and free memory in the JVM. */
	public static void chainAddLink(String chainName, String linkName)                     { chainAddLinkImpl(DEFAULT_PROFILE_NAME, chainName, linkName); }
	public static void chainAddLink(Object o, String chainName, String linkName)           { chainAddLinkImpl(getSimpleClassName(o), chainName, linkName); }
	public static void chainAddLink(String profileName, String chainName, String linkName) { chainAddLinkImpl(profileName, chainName, linkName); }

	/** Resets the values for a chain. */
	public static void chainReset(String chainName)                     { chainResetImpl(DEFAULT_PROFILE_NAME, chainName); }
	public static void chainReset(Object o, String chainName)           { chainResetImpl(getSimpleClassName(o), chainName); }
	public static void chainReset(String profileName, String chainName) { chainResetImpl(profileName, chainName); }

	
	////////////////////////////////////////////////////////////
	//
	// CACHE API - A cache manages objects for debugging.
	//
	
	/** Adds an object to the cache. */
	public static void cacheAddObject(String cacheName, String objectKey, Object obj)                     { cacheAddObjectImpl(DEFAULT_PROFILE_NAME, cacheName, objectKey, obj); }
	public static void cacheAddObject(Object o, String cacheName, String objectKey, Object obj)           { cacheAddObjectImpl(getSimpleClassName(o), cacheName, objectKey, obj); }
	public static void cacheAddObject(String profileName, String cacheName, String objectKey, Object obj) { cacheAddObjectImpl(profileName, cacheName, objectKey, obj); }
	
	/** Returns an object from the cache. */
	public static Object cacheGetObject(String cacheName, String objectKey)                     { return cacheGetObjectImpl(DEFAULT_PROFILE_NAME, cacheName, objectKey); }
	public static Object cacheGetObject(Object o, String cacheName, String objectKey)           { return cacheGetObjectImpl(getSimpleClassName(o), cacheName, objectKey); }
	public static Object cacheGetObject(String profileName, String cacheName, String objectKey) { return cacheGetObjectImpl(profileName, cacheName, objectKey); }

	/** Returns the list of objects in the cache. */
	public static List<Object> cacheGetObjects(String cacheName)                     { return cacheGetObjectsImpl(DEFAULT_PROFILE_NAME, cacheName); }
	public static List<Object> cacheGetObjects(Object o, String cacheName)           { return cacheGetObjectsImpl(getSimpleClassName(o), cacheName); }
	public static List<Object> cacheGetObjects(String profileName, String cacheName) { return cacheGetObjectsImpl(profileName, cacheName); }
	
	/** Resets the values for a cache. */
	public static void cacheReset(String cacheName)                     { cacheResetImpl(DEFAULT_PROFILE_NAME, cacheName); }
	public static void cacheReset(Object o, String cacheName)           { cacheResetImpl(getSimpleClassName(o), cacheName); }
	public static void cacheReset(String profileName, String cacheName) { cacheResetImpl(profileName, cacheName); }


	////////////////////////////////////////////////////////////
	//
	// REPORTING API
	//
		
	/** Returns a string representing all of the counter values. */
	public static String reportAllCounters() { return counterGetReportImpl(getAllCounters()); }
	
	/** Returns a string representing the counter values for a Profile. */
	public static String reportProfileCounters(Object o)           { return counterGetReportImpl(getAllProfileCounters(getOrCreateProfile(getSimpleClassName(o)))); }
	public static String reportProfileCounters(String profileName) { return counterGetReportImpl(getAllProfileCounters(getOrCreateProfile(profileName))); }	


	/** Returns a string representing all of the clock values. */
	public static String reportAllClocks() { return clockGetReportImpl(getAllClocks()); }
	
	/** Returns a string representing the clock values for a Profile. */
	public static String reportProfileClocks(Object o)           { return clockGetReportImpl(getAllProfileClocks(getOrCreateProfile(getSimpleClassName(o)))); }
	public static String reportProfileClocks(String profileName) { return clockGetReportImpl(getAllProfileClocks(getOrCreateProfile(profileName))); }


	/** Returns a string representing all chain values for a Profile. */
	public static String reportProfileChains(Object o)            { return chainGetReportImpl(getSortedProfileChains(getOrCreateProfile(getSimpleClassName(o)))); }
	public static String reportProfileChains(String profileName)  { return chainGetReportImpl(getSortedProfileChains(getOrCreateProfile(profileName))); }

	/** Returns a string representing the values for a chain. */
	public static String reportChain(String chainName)                     { return chainGetReportImpl(getChain(DEFAULT_PROFILE_NAME, chainName)); }
	public static String reportChain(Object o, String chainName)           { return chainGetReportImpl(getChain(getSimpleClassName(o), chainName)); }
	public static String reportChain(String profileName, String chainName) { return chainGetReportImpl(getChain(profileName, chainName)); }
	
	
	/** Returns a string representing cache objects (via toString) for a cache. */
	public static String reportCache(String cacheName)                     { return cacheGetReportImpl(getCache(DEFAULT_PROFILE_NAME, cacheName)); }
	public static String reportCache(Object o, String cacheName)           { return cacheGetReportImpl(getCache(getSimpleClassName(o), cacheName)); }
	public static String reportCache(String profileName, String cacheName) { return cacheGetReportImpl(getCache(profileName, cacheName)); }
	
	
	////////////////////////////////////////////////////////////
	//
	// MISCELLANEOUS API
	//
	
	/** Forces a Garbage Collection. */
	public static void gc() { if (!ENABLED) { Runtime.getRuntime().gc(); } }
	
	/** Closes (deletes) all objects for all profiles. */
	public static void closeAllProfiles() { closeAllProfilesImpl(); };
	
	/** Resets a profile */
	public static void profileReset()                   { profileResetImpl(DEFAULT_PROFILE_NAME); };
	public static void profileReset(Object o)           { profileResetImpl(getSimpleClassName(o)); }
	public static void profileReset(String profileName) { profileResetImpl(profileName); }
	
	/** Reset methods for profiles, counters, clocks, chains, caches */
	public static void resetAllProfiles() { resetAllProfilesImpl(); }
	public static void resetAllCounters() { resetAllCountersImpl(); }	
	public static void resetAllClocks() { resetAllClocksImpl(); }	
	public static void resetAllChains() { resetAllChainsImpl(); }	
	public static void resetAllCaches() { resetAllCachesImpl(); }
		
	
	//
	// Implementation -- Impl Methods
	//                -- Each Impl method should implement a check for ENABLED
	//
	
	/** Increments the value of a counter. */
	private static void counterIncImpl(String profileName, String counterName) {
		if (!ENABLED) return;
		Counter c = getOrCreateCounter(profileName, counterName);
		c.incrementValue();
	}

	/** Sets the value of a counter. */
	private static void counterSetImpl(String profileName, String counterName, long value) {
		if (!ENABLED) return;
		Counter c = getOrCreateCounter(profileName, counterName);
		c.setValue(value);
	}
	
	/** Returns the value of a counter. */
	private static long counterGetImpl(String profileName, String counterName) {
		if (!ENABLED) return 0;
		Counter c = getOrCreateCounter(profileName, counterName);
		return c.getValue();
	}

	/** Resets the value for a counter */
	private static void counterResetImpl(String profileName, String counterName) {
		if (!ENABLED) return;
		Counter c = getOrCreateCounter(profileName, counterName);
		c.reset();
	}
	
	/** Starts time for a clock. */
	private static void clockStartImpl(String profileName, String clockName) {
		if (!ENABLED) return;
		long millis = System.currentTimeMillis();
		Clock c = getOrCreateClock(profileName, clockName);
		c.updateStart(millis);
	}
	
	/** Stops time for a clock. */
	private static void clockStopImpl(String profileName, String clockName) {
		if (!ENABLED) return;
		long millis = System.currentTimeMillis();
		Clock c = getClock(profileName, clockName);
		if (c != null) {
			c.updateStop(millis);
		}
	}
	
	/** Returns the elapsed time for a clock */
	private static long clockGetElapsedImpl(String profileName, String clockName) {
		if (!ENABLED) return 0;
		long elapsedTime = 0;
		Clock c = getClock(profileName, clockName);
		if (c != null) {
			elapsedTime = c.getElapsedTime();
		}
		return elapsedTime;
	}
	
	/** Returns the number of times clockStop was called for a clock. */	
	private static long clockGetStopNumberImpl(String profileName, String clockName) {
		if (!ENABLED) return 0;
		int stopNum = 0;
		Clock c = getClock(profileName, clockName);
		if (c != null) {
			stopNum = c.getStopNum();
		}
		return stopNum;
	}
	
	/** Returns the number of times clockStart was called for a clock. */	
	private static long clockGetStartNumImpl(String profileName, String clockName) {
		if (!ENABLED) return 0;
		int startNum = 0;
		Clock c = getClock(profileName, clockName);
		if (c != null) {
			startNum = c.getStartNum();
		}
		return startNum;
	}
	
	/** Resets the values for a clock. */
	private static void clockResetImpl(String profileName, String clockName) {
		if (!ENABLED) return;
		Clock c = getClock(profileName, clockName);
		if (c != null) {
			c.reset();
		}
	}
	
	/** Marks the amount of free memory in the JVM by appending a link to a memory chain. */
	private static void chainAddLinkImpl(String profileName, String chainName, String linkName) { 
		if (!ENABLED) return;
		Chain c = getOrCreateChain(profileName, chainName);
		c.appendLink(linkName);
	}
	
	/** Resets the values for a chain. */
	private static void chainResetImpl(String profileName, String chainName) {
		if (!ENABLED) return;
		Chain c = getChain(profileName, chainName);
		if (c != null) {
			c.reset();
		}
	}
	
	/** Adds an object to the cache. */
	private static void cacheAddObjectImpl(String profileName, String cacheName, String objectKey, Object obj) { 
		if (!ENABLED) return;
		Cache c = getOrCreateCache(profileName, cacheName);
		c.addCacheObject(objectKey, obj);
	}
	
	/** Returns an object from the cache. */
	private static Object cacheGetObjectImpl(String profileName, String cacheName, String objectKey) {
		if (!ENABLED) return null;
		Object obj = null;
		Cache c = getCache(profileName, cacheName);
		if (c != null) {
			obj = c.getCacheObject(objectKey);
		}
		return obj;
	}

	/** Returns the list of objects in the cache. */
	private static List<Object> cacheGetObjectsImpl(String profileName, String cacheName) {
		if (!ENABLED) return null;
		Cache c = getOrCreateCache(profileName, cacheName);
		return c.getObjects();
	}
	
	/** Resets the values for a cache. */
	private static void cacheResetImpl(String profileName, String cacheName) {
		if (!ENABLED) return;
		Cache c = getCache(profileName, cacheName);
		if (c != null) {
			c.reset();
		}
	}
	
	/** Returns a counter report string for an input list of counters */
	private static String counterGetReportImpl(List<Counter> cList) {
		if (!ENABLED) return DISABLED;
		
		// return empty string when no counters exist
		if (cList.size() < 1) {
			return EMPTY_STR;
		}
			
		int[] maxNameLengths = getMaxNameLengths(cList);
		int minProfileColLen = 7;
		int minCounterColLen = 7;
		int profileNameCol = (maxNameLengths[0] > minProfileColLen) ? maxNameLengths[0] : minProfileColLen;
		int counterNameCol = (maxNameLengths[1] > minCounterColLen) ? maxNameLengths[1] : minCounterColLen;
		int countCol = 12;
		int bufferCol = 2;
		StringBuffer buf = new StringBuffer();

		// report header
		buf.append(buildField(profileNameCol, LEFT, "Profile"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(counterNameCol, LEFT, "Counter"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(countCol, RIGHT, "Count"));
		buf.append(NEWLINE);
		buf.append(buildField(profileNameCol, LEFT, "-------"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(counterNameCol, LEFT, "-------"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(countCol, RIGHT, "-----"));

		sortCounterList(cList);
		
		Iterator<Counter> cIter = cList.iterator();
		String lastProfileName = "";
		while (cIter.hasNext()) {
			Counter c = cIter.next();
			ProfileObject p = c.getProfile();
			String profileName = p.getName();
			
			// separate the Profiles with a space
			if (SEPARATE_PROFILES_BY_NEWLINE) {
				if (lastProfileName.length() > 0 && !lastProfileName.equals(profileName)) {
					buf.append(NEWLINE);				
				}
			}
			
			// report row
			buf.append(NEWLINE);
			buf.append(buildField(profileNameCol, LEFT, profileName));
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(counterNameCol, LEFT, c.getName()));
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(countCol, RIGHT, String.valueOf(c.getValue())));
			
			lastProfileName = profileName;
		}

		return buf.toString();
	}
	
	/** Returns a clock report string for an input list of clocks */
	private static String clockGetReportImpl(List<Clock> cList) {
		if (!ENABLED) return DISABLED;
		
		// return empty string when no clocks exist
		if (cList.size() < 1) {
			return EMPTY_STR;
		}
		
		int[] maxNameLengths = getMaxNameLengths(cList);
		int minProfileColLen = 7;
		int minClockColLen = 5;
		int profileNameCol = (maxNameLengths[0] > minProfileColLen) ? maxNameLengths[0] : minProfileColLen;
		int clockNameCol = (maxNameLengths[1] > minClockColLen) ? maxNameLengths[1] : minClockColLen;
		int elapsedCol = 12;
		int startsCol = 12;
		int stopsCol = 12;
		int bufferCol = 2;
		StringBuffer buf = new StringBuffer();

		// report header
		buf.append(buildField(profileNameCol, LEFT, "Profile"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(clockNameCol, LEFT, "Clock"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(elapsedCol, RIGHT, "Elapsed"));
		buf.append(buildField(startsCol, RIGHT, "Starts"));
		buf.append(buildField(stopsCol, RIGHT, "Stops"));
		buf.append(NEWLINE);
		buf.append(buildField(profileNameCol, LEFT, "-------"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(clockNameCol, LEFT, "-----"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(elapsedCol, RIGHT, "-------"));
		buf.append(buildField(startsCol, RIGHT, "------"));
		buf.append(buildField(stopsCol, RIGHT, "-----"));
		
		sortClockList(cList);

		Iterator<Clock> cIter = cList.iterator();
		String lastProfileName = "";
		while (cIter.hasNext()) {
			Clock c = cIter.next();
			ProfileObject p = c.getProfile();
			String profileName = p.getName();
			
			// separate the Profiles with a space
			if (SEPARATE_PROFILES_BY_NEWLINE) {
				if (lastProfileName.length() > 0 && !lastProfileName.equals(profileName)) {
					buf.append(NEWLINE);				
				}
			}
			
			// report row
			buf.append(NEWLINE);
			buf.append(buildField(profileNameCol, LEFT, profileName));
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(clockNameCol, LEFT, c.getName()));
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(elapsedCol, RIGHT, String.valueOf(c.getElapsedTime())));
			buf.append(buildField(startsCol, RIGHT, String.valueOf(c.getStartNum())));
			buf.append(buildField(stopsCol, RIGHT, String.valueOf(c.getStopNum())));
			
			lastProfileName = profileName;
		}

		return buf.toString();
	}
	
	/** Returns a string representing a list of chain values. */
	private static String chainGetReportImpl(Chain chain) {
		if (!ENABLED) return DISABLED;
		List<Chain> cList = new ArrayList<Chain>();
		cList.add(chain);
		return chainGetReportImpl(cList);
	}
	
	/** Returns a string representing a list of chain values. */
	private static String chainGetReportImpl(List<Chain> cList) {
		if (!ENABLED) return DISABLED;

		// return empty string when no chains exist
		if (cList.size() < 1) {
			return EMPTY_STR;
		}

		int[] maxNameLengths = getChainMaxNameLengths(cList);
		int minProfileColLen = 7;
		int minChainColLen = 5;
		int minLinkColLen = 4;
		int profileNameCol = (maxNameLengths[0] > minProfileColLen) ? maxNameLengths[0] : minProfileColLen;
		int chainNameCol = (maxNameLengths[1] > minChainColLen) ? maxNameLengths[1] : minChainColLen;
		int linkNameCol = (maxNameLengths[2] > minLinkColLen) ? maxNameLengths[2] : minLinkColLen;
		int timestampCol = 25;
		int freeMemoryCol = 12;
		int bufferCol = 2;
		int preDiffCols = (bufferCol * 4) + profileNameCol + chainNameCol + linkNameCol + timestampCol + freeMemoryCol;
		int memDiffCol = 12;
		int memDiffMegCol = 11;
		int elapsedDiffCol = 13;
		StringBuffer buf = new StringBuffer();

		// report header
		buf.append(buildField(profileNameCol, LEFT, "Profile"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(chainNameCol, LEFT, "Chain"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(linkNameCol, LEFT, "Link"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(timestampCol, LEFT, "Timestamp"));
		buf.append(buildField(freeMemoryCol, RIGHT, "Free Memory"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(memDiffCol, RIGHT, "Memory Diff"));
		if (CHAIN_MEGABYTE_LEGEND) {
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(memDiffMegCol, LEFT, "(MEGABYTES)"));
		}
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(elapsedDiffCol, RIGHT, "Elapsed Diff"));
		buf.append(NEWLINE);
		buf.append(buildField(profileNameCol, LEFT, "-------"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(chainNameCol, LEFT, "-----"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(linkNameCol, LEFT, "----"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(timestampCol, LEFT, "---------"));
		buf.append(buildField(freeMemoryCol, RIGHT, "-----------"));
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(memDiffCol, RIGHT, "-----------"));
		if (CHAIN_MEGABYTE_LEGEND) {
			buf.append(buildField(bufferCol, LEFT, SPACE));
			buf.append(buildField(memDiffMegCol, LEFT, "-----------"));
		}
		buf.append(buildField(bufferCol, LEFT, SPACE));
		buf.append(buildField(elapsedDiffCol, RIGHT, "------------"));

		Iterator<Chain> cIter = cList.iterator();
		while (cIter.hasNext()) {
			Chain c = cIter.next();
			ProfileObject p = c.getProfile();
			long freeMemoryDiffTotal = 0;
			long elapsedDiffTotal = 0;
			
			Iterator<Link> linkIter = c.getLinks().iterator();
			Link prevLink = null;
			while (linkIter.hasNext()) {
				Link link = linkIter.next();

				String timestampStr = getDateStr(new Date(link.getTimestamp()), DATE_FORMAT_HHMMSSS);

				String memDiffStr = "...";
				String memDiffMegStr = "(...)";
				String elapsedDiffStr = "...";
				if (prevLink != null) {
					long memDiff = link.getFreeMemory() - prevLink.getFreeMemory();
					memDiffStr = String.valueOf(memDiff);
					freeMemoryDiffTotal += memDiff;
					
					double memDiffMeg = (1.0 * memDiff) / (1.0 * BYTES_PER_MEGABYTE);
					memDiffMegStr = buildField((memDiffMegCol - 2), LEFT, String.valueOf(memDiffMeg));
					memDiffMegStr = memDiffMegStr.trim();
					memDiffMegStr = "(" + memDiffMegStr + ")";
					
					long elapsedDiff = link.getTimestamp() - prevLink.getTimestamp();
					elapsedDiffStr = String.valueOf(elapsedDiff);
					elapsedDiffTotal += elapsedDiff;
				}
				
				// report row
				buf.append(NEWLINE);
				buf.append(buildField(profileNameCol, LEFT, p.getName()));
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(chainNameCol, LEFT, c.getName()));
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(linkNameCol, LEFT, link.getName()));
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(timestampCol, LEFT, timestampStr));
				buf.append(buildField(freeMemoryCol, RIGHT, String.valueOf(link.getFreeMemory())));
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(memDiffCol, RIGHT, memDiffStr));
				if (CHAIN_MEGABYTE_LEGEND) {
					buf.append(buildField(bufferCol, LEFT, SPACE));
					buf.append(buildField(memDiffMegCol, LEFT, memDiffMegStr));
				}
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(elapsedDiffCol, RIGHT, elapsedDiffStr));

				prevLink = link;
			}

			if (CHAIN_TOTALS_ROW) {
				buf.append(NEWLINE);
				buf.append(buildField(preDiffCols, LEFT, SPACE));
				buf.append(buildField(memDiffCol, RIGHT, "-----------"));
				if (CHAIN_MEGABYTE_LEGEND) {
					buf.append(buildField(bufferCol, LEFT, SPACE));
					buf.append(buildField(memDiffMegCol, LEFT, SPACE));
				}
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(elapsedDiffCol, RIGHT, "------------"));
				buf.append(NEWLINE);
				buf.append(buildField(preDiffCols, LEFT, SPACE));
				buf.append(buildField(memDiffCol, RIGHT, String.valueOf(freeMemoryDiffTotal)));
				if (CHAIN_MEGABYTE_LEGEND) {					
					double freeMemoryDiffTotalMeg = (1.0 * freeMemoryDiffTotal) / (1.0 * BYTES_PER_MEGABYTE);
					String megStr = buildField((memDiffMegCol - 2), LEFT, String.valueOf(freeMemoryDiffTotalMeg));
					megStr = megStr.trim();
					megStr = "(" + megStr + ")";
					buf.append(buildField(bufferCol, LEFT, SPACE));
					buf.append(buildField(memDiffMegCol, LEFT, megStr));
				}
				buf.append(buildField(bufferCol, LEFT, SPACE));
				buf.append(buildField(elapsedDiffCol, RIGHT, String.valueOf(elapsedDiffTotal)));
				buf.append(NEWLINE);
			} else if (SEPARATE_CHAINS_BY_NEWLINE) {
				buf.append(NEWLINE);
			}
		}

		return buf.toString();
	}
	
	/** Returns a string representing cache objects (via toString) for a cache. */
	private static String cacheGetReportImpl(Cache cache) {
		if (!ENABLED) return DISABLED;
		
		// return empty string when no CacheObjects exist
		if (cache.getNumberOfCacheObjects() < 1) {
			return EMPTY_STR;
		}
		
		StringBuffer buf = new StringBuffer();
		buf.append("Cache: ");
		buf.append(cache.getName());
		buf.append("  --  Profile: ");
		buf.append(cache.getProfile().getName());
		buf.append(NEWLINE);
		Iterator<CacheObject> iter = cache.getSortedCacheObjects().iterator();
		while (iter.hasNext()) {
			CacheObject co = iter.next();
			Object o = co.getObject();
			if (CACHE_SHOW_OBJECT_HEADER) {
				buf.append(NEWLINE);
				buf.append("(objectKey=");
				buf.append(co.getObjectKey());
				buf.append("  ts=");
				buf.append(getDateStr(new Date(co.getTimestamp()), DATE_FORMAT_HHMMSSS));
				buf.append(")");
			}
			buf.append(NEWLINE);
			buf.append(o.toString());
			buf.append(NEWLINE);
		}
		
		return buf.toString();
	}
	
	/** Closes (deletes) all objects for all profiles. */
	private static void closeAllProfilesImpl() {
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.destroy();
		}
		profileMap.clear();
	}
	
	/** Resets a profile. */
	private static void profileResetImpl(String profileName) {
		if (!ENABLED) return;
		ProfileObject p = getOrCreateProfile(profileName);
		p.reset();
	}
	
	/** Resets all profiles */
	private static void resetAllProfilesImpl() {
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.reset();
		}
	}
	
	/** Resets all clocks */
	private static void resetAllClocksImpl () { 
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.resetClocks();
		}
	}
	
	/** Resets all counters */
	private static void resetAllCountersImpl () { 
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.resetCounters();
		}
	}
	
	/** Resets all chains */
	private static void resetAllChainsImpl () { 
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.resetChains();
		}
	}
	
	/** Resets all caches */
	private static void resetAllCachesImpl () { 
		if (!ENABLED) return;
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			p.resetCaches();
		}
	}
	
	//
	// General Implementation
	//
	
	/** 
	 * Returns MAX length of the ProfileObject names and ProfileChild names
	 * from an input ProfileChild list.  The first value in the array is
	 * for Profile names, the second is for Counter/Clock.
	 * 
	 * This method is used to dynamically determine the length/size of 
	 * the Profile and Counter/Clock columns for the reports.
	 */
	@SuppressWarnings("unchecked")
	private static int[] getMaxNameLengths(List cList) {
		int[] maxLengths = new int[2];
		maxLengths[0] = 0;
		maxLengths[1] = 0;
		Iterator<ProfileChild> iter = cList.iterator();
		while (iter.hasNext()) {
			ProfileChild c = iter.next();
			ProfileObject p = c.getProfile();
			if (p.getName().length() > maxLengths[0]) {
				maxLengths[0] = p.getName().length();
			}
			if (c.getName().length() > maxLengths[1]) {
				maxLengths[1] = c.getName().length();
			}
		}
		return maxLengths;
	}
 
	/** 
	 * Returns MAX length of the ProfileObject names, Chain names, and Link names
	 * from an input Chain list.  The first value in the array is for Profile names, 
	 * the second is for Chain, the third is for Link.
	 * 
	 * This method is used to dynamically determine the length/size of 
	 * the Profile, Chain, and Link columns for the Chain reports. 
	 */
	private static int[] getChainMaxNameLengths(List<Chain> cList) {
		int[] maxLengths = new int[3];
		maxLengths[0] = 0;
		maxLengths[1] = 0;
		maxLengths[2] = 0;
		Iterator<Chain> iter = cList.iterator();
		while (iter.hasNext()) {
			Chain c = iter.next();
			ProfileObject p = c.getProfile();
			if (p.getName().length() > maxLengths[0]) {
				maxLengths[0] = p.getName().length();
			}
			if (c.getName().length() > maxLengths[1]) {
				maxLengths[1] = c.getName().length();
			}
			Iterator<Link> linkIter = c.getLinks().iterator();
			while (linkIter.hasNext()) {
				Link link = linkIter.next();
				if (link.getName().length() > maxLengths[2]) {
					maxLengths[2] = link.getName().length();
				}
			}
		}
		return maxLengths;
	}
	
	/** Sorts a list of Counter objects. */
	private static void sortCounterList(List<Counter> cList)
	{
		Collections.sort(cList, new ProfileChildComparator());
	}
	
	/** Sorts a list of Clock objects. */
	private static void sortClockList(List<Clock> cList)
	{
		Collections.sort(cList, new ProfileChildComparator());
	}
	
	/** Sorts a list of Chain objects. */
	private static void sortChainList(List<Chain> cList)
	{
		Collections.sort(cList, new ProfileChildComparator());
	}
	
	/** Returns all of the counters for a Profile */
	private static List<Counter> getAllProfileCounters(ProfileObject profile) {
		List<Counter> cList = new ArrayList<Counter>();
		Iterator<Counter> cIter = profile.counterMap.values().iterator();
		while (cIter.hasNext()) {
			Counter c = cIter.next();
			cList.add(c);
		}
		return cList;
	}
	
	/** Returns all counters for all Profiles */
	private static List<Counter> getAllCounters() {
		List<Counter> cList = new ArrayList<Counter>();
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			Iterator<Counter> cIter = p.counterMap.values().iterator();
			while (cIter.hasNext()) {
				Counter c = cIter.next();
				cList.add(c);
			}
		}
		return cList;
	}
	
	/** Returns all of the clocks for a Profile */
	private static List<Clock> getAllProfileClocks(ProfileObject profile) {
		List<Clock> cList = new ArrayList<Clock>();
		Iterator<Clock> cIter = profile.clockMap.values().iterator();
		while (cIter.hasNext()) {
			Clock c = cIter.next();
			cList.add(c);
		}
		return cList;
	}
	
	/** Returns all clocks for all Profiles */
	private static List<Clock> getAllClocks() {
		List<Clock> cList = new ArrayList<Clock>();
		Iterator<ProfileObject> pIter = profileMap.values().iterator();
		while (pIter.hasNext()) {
			ProfileObject p = pIter.next();
			Iterator<Clock> cIter = p.clockMap.values().iterator();
			while (cIter.hasNext()) {
				Clock c = cIter.next();
				cList.add(c);
			}
		}
		return cList;
	}
	
	/** Returns a sorted list of all Chains in a profile. */
	private static List<Chain> getSortedProfileChains(ProfileObject profile) {
		List<Chain> cList = new ArrayList<Chain>();
		Iterator<Chain> cIter = profile.chainMap.values().iterator();		
		while (cIter.hasNext()) {
			Chain c = cIter.next();
			cList.add(c);
		}
		sortChainList(cList);
		return cList;
	}
	
	/** 
	 * Returns a Counter for a profileName and counterName.
	 * Creates a new Counter if necessary. 
	 */
	private static Counter getOrCreateCounter(String profileName, String counterName) {
		ProfileObject p = getOrCreateProfile(profileName);
		Counter c = p.counterMap.get(counterName);
		if (c == null) {
			c = new Counter(p, counterName);
			p.counterMap.put(counterName, c);
		}
		return c;
	}
	
	/** 
	 * Returns a Clock for a profileName and clockName.
	 * Creates a new Clock if necessary. 
	 */
	private static Clock getOrCreateClock(String profileName, String clockName) {
		ProfileObject p = getOrCreateProfile(profileName);
		Clock c = p.clockMap.get(clockName);
		if (c == null) {
			c = new Clock(p, clockName);
			p.clockMap.put(clockName, c);
		}
		return c;
	}
	
	/** 
	 * Returns a Clock for a profileName and counterName.
	 * Returns null if the clock does not exist.
	 */
	private static Clock getClock(String profileName, String clockName) {
		ProfileObject p = getOrCreateProfile(profileName);
		return p.clockMap.get(clockName);
	}
	
	/** 
	 * Returns a Chain for a profileName and chainName.
	 * Creates a new Chain if necessary. 
	 */
	private static Chain getOrCreateChain(String profileName, String chainName) {
		ProfileObject p = getOrCreateProfile(profileName);
		Chain c = p.chainMap.get(chainName);
		if (c == null) {
			c = new Chain(p, chainName);
			p.chainMap.put(chainName, c);
		}
		return c;
	}
	
	/** 
	 * Returns a Chain for a profileName and chainName.
	 * Returns null if the chain does not exist.
	 */
	private static Chain getChain(String profileName, String chainName) {
		ProfileObject p = getOrCreateProfile(profileName);
		return p.chainMap.get(chainName);
	}
	
	/**
	 * Returns a Cache for a profileName and cacheName.
	 * Creates a new Cache if necessary.
	 */
	private static Cache getOrCreateCache(String profileName, String cacheName) {
		ProfileObject p = getOrCreateProfile(profileName);
		Cache c = p.cacheMap.get(cacheName);
		if (c == null) {
			c = new Cache(p, cacheName);
			p.cacheMap.put(cacheName, c);
		}
		return c;
	}
	
	/**
	 * Returns a Cache for a profileName and cacheName.
	 * Returns null if the cache does not exist.
	 */
	private static Cache getCache(String profileName, String cacheName) {
		ProfileObject p = getOrCreateProfile(profileName);
		return p.cacheMap.get(cacheName);
	}
	
	/** 
	 * Returns a Profile instance for a profileName.
	 * Creates a new Profile if necessary. 
	 */
	private static ProfileObject getOrCreateProfile(String profileName) {
		profileName = resolveProfileName(profileName);
		ProfileObject p = profileMap.get(profileName);
		if (p == null) {
			p = new ProfileObject(profileName);
			profileMap.put(profileName, p);
		}
		return p;
	}
	
	/** Resolves the profile name by returning the default for null or input value. */
	private static String resolveProfileName(String profileName) {
		return (profileName == null) ? DEFAULT_PROFILE_NAME : profileName;
	}
	
	/** Returns the short name of an object's class. */
	private static String getSimpleClassName(Object o) {
		return o.getClass().getSimpleName();
	}
	
	/** Returns a date string in the input date format. */
	private static String getDateStr(Date d, String dateFormat) {
		SimpleDateFormat format = new SimpleDateFormat(dateFormat);
		return format.format(d);
	}
	
	/** Returns a string formatted according to length and left/right justification. */
	private static String buildField(int fLen, int justification, String val) {
		// return empty string when the input is null or empty
		if (val == null || val.equals("")) {
			return "";
		}
		
		String field = null;
		int vLen = val.length();
		
		// return input string when it matches the field length
		if (vLen == fLen) {
			return val;
		}
		
		if (justification == LEFT) {
			if (vLen > fLen) {
				// truncate from the end
				field = val.substring(0, fLen);
			}
			else if (vLen < fLen) {
				// pad onto the end
				field = padRight(val, SPACE_CHAR, (fLen - vLen));
			}
		}
		else {
			if (vLen > fLen) {
				// truncate from the beginning
				field = val.substring(vLen - fLen);
			}
			else if (vLen < fLen) {
				// pad onto the beginning
				field = padLeft(val, SPACE_CHAR, (fLen - vLen));
			}
		}

		return field;
	}
	
	/** Pads characters on the left (beginning) of a string. */
	private static String padLeft(String str, char padChar, int num) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < num; i++) {
			buf.append(padChar);
		}
		buf.append(str);
		return buf.toString();
	}
	
	/** Pads characters on the right (end) of a string. */
	private static String padRight(String str, char padChar, int num) {
		StringBuffer buf = new StringBuffer();
		buf.append(str);
		for (int i = 0; i < num; i++) {
			buf.append(padChar);
		}
		return buf.toString();
	}
	
	
	////////////////////////////////////////////////////////////
	//
	// Inner Classes - These classes support the Profile implementation.
	//
	
	/**
	 * ProfileObject - A named profile object that is the parent for Counter, Clock, and Chain.
	 */
	private static class ProfileObject {
		
		private String name;
		private HashMap<String,Counter> counterMap = new HashMap<String,Counter>();
		private HashMap<String,Clock> clockMap = new HashMap<String,Clock>();
		private HashMap<String,Chain> chainMap = new HashMap<String,Chain>();
		private HashMap<String,Cache> cacheMap = new HashMap<String,Cache>();
		
		/** Constructor */
		private ProfileObject(String profileName) {
			this.name = profileName;
		}
		
		private void destroy() {
			this.name = null;
			this.counterMap.clear();
			this.clockMap.clear();
			this.chainMap.clear();
			this.cacheMap.clear();
		}

		private void reset() {
			resetCounters();
			resetClocks();
			resetChains();
			resetCaches();
		}
		
		private void resetCounters() {
			Iterator<Counter> counterIter = this.counterMap.values().iterator();
			while (counterIter.hasNext()) {
				Counter c = counterIter.next();
				c.reset();
			}			
		}
		
		private void resetClocks() {
			Iterator<Clock> clockIter = this.clockMap.values().iterator();
			while (clockIter.hasNext()) {
				Clock c = clockIter.next();
				c.reset();
			}
		}
		
		private void resetChains() {
			Iterator<Chain> chainIter = this.chainMap.values().iterator();
			while (chainIter.hasNext()) {
				Chain c = chainIter.next();
				c.reset();
			}
		}
		
		private void resetCaches() {
			Iterator<Cache> cacheIter = this.cacheMap.values().iterator();
			while (cacheIter.hasNext()) {
				Cache c = cacheIter.next();
				c.reset();
			}
		}
		
		private String getName() { return this.name; }
	}
	
	/**
	 * ProfileChild - Base class for Counter, Clock, and Chain.
	 */
	private static class ProfileChild {
		
		protected ProfileObject profile = null;
		protected String name = null;
		
		protected ProfileObject getProfile() { return this.profile; }
		protected String getName() { return this.name; }
	}
	
	/**
	 * Counter Class - Used to capture a number.
	 */
	private static class Counter extends ProfileChild {

		private long value = 0;
		
		/** Constructor */
		private Counter(ProfileObject profile, String counterName) {
			this.profile = profile;
			this.name = counterName;
		}
		
		private void reset() {
			this.value = 0;
		}
		
		private long getValue() { return this.value; }
		private void setValue(long val) { this.value = val; }
		private void incrementValue() { this.value++; }
	}
	
    /**
     * Clock Class - Used to capture elapsed time.
     */
	private static class Clock extends ProfileChild {

		private long start = -1;
		private long stop = -1;
		private long elapsedTime = 0;
		private int startNum = 0;
		private int stopNum = 0;
		
		/** Constructor */
		private Clock(ProfileObject profile, String clockName) {
			this.profile = profile;
			this.name = clockName;
		}
		
		private void reset() {
			this.start = -1;
			this.stop = -1;
			this.elapsedTime = 0;
			this.startNum = 0;
			this.stopNum = 0;
		}
		
		private void updateStart(long millis) {
			this.start = millis;
			this.stop = -1;
			this.startNum++;
		}
		
		private void updateStop(long millis) {
			if (this.start != -1) {
				this.stop = millis;
				this.elapsedTime += (this.stop - this.start);
			}
			this.stopNum++;
		}
		
		private long getElapsedTime() { return this.elapsedTime; }
		private int getStartNum() { return this.startNum; }		
		private int getStopNum() { return this.stopNum; }
	}
	
	/**
	 * Chain Class - Used to capture (and report) timestamps and free memory in the JVM.
	 *             - Is the parent/container of Link(s).
	 */
	private static class Chain extends ProfileChild {

		private List<Link> links = new ArrayList<Link>();
		
		/** Constructor */
		private Chain(ProfileObject profile, String chainName) {
			this.profile = profile;
			this.name = chainName;
		}
		
		private void reset() {
			this.links.clear();
		}
		
		private void appendLink(String linkName) {
			long freeMemory = Runtime.getRuntime().freeMemory();
			long timestamp = System.currentTimeMillis();
			this.links.add(new Link(linkName, freeMemory, timestamp));
		}

		private List<Link> getLinks() { return this.links; }
	}
	
	/**
	 * Link Class - Used to capture (and report) timestamps and free memory in the JVM.
	 *            - Represents one individual "link" in a chain.
	 */
	private static class Link {
		
		private String name = null;
		private long freeMemory = 0;
		private long timestamp = 0;
		
		/** Constructor */
		private Link(String linkName, long freeMemory, long timestamp) {
			this.name = linkName;
			this.freeMemory = freeMemory;
			this.timestamp = timestamp;
		}
		
		private String getName() { return this.name; }
		private long getFreeMemory() { return this.freeMemory; }
		private long getTimestamp() { return this.timestamp; }
	}
	
	/**
	 * Cache Class - Used to store objects for debugging.
	 *             - Is the parent/container of CacheObject(s).
	 */
	private static class Cache extends ProfileChild {
		
		private long orderIndex = 0;        // orders the CacheObject(s) for sorting
		private long collisionIndex = 1;    // used for collisions (when objectKeys are equal).
		private HashMap<String,CacheObject> cacheObjects = new HashMap<String,CacheObject>();

		/** Constructor */
		private Cache(ProfileObject profile, String cacheName) {
			this.profile = profile;
			this.name = cacheName;
		}
		
		private void reset () {
			this.orderIndex = 0;
			this.collisionIndex = 1;
			Iterator<CacheObject> iter = this.cacheObjects.values().iterator();
			while (iter.hasNext()) {
				CacheObject co = iter.next();
				co.reset();
			}
			this.cacheObjects.clear();
		}
		
		private void addCacheObject(String objectKey, Object obj) {
			long timestamp = System.currentTimeMillis();
			
			// guarantee uniqueness when objectKey is empty
			if (objectKey == null || objectKey.length() < 1) {
				objectKey = String.valueOf(this.collisionIndex);
				this.collisionIndex++;
			}
			
			// guarantee uniqueness when a collision occurs
			if (this.cacheObjects.get(objectKey) != null) {
				objectKey += String.valueOf(this.collisionIndex);
				this.collisionIndex++;
			}

			this.orderIndex++;
			CacheObject co = new CacheObject(this.orderIndex, objectKey, obj, timestamp);
			this.cacheObjects.put(objectKey, co);
		}
		
		private Object getCacheObject(String objectKey) {
			Object obj = null;
			CacheObject co = this.cacheObjects.get(objectKey);
			if (co != null) {
				obj = co.getObject();
			}
			return obj;
		}

		private List<Object> getObjects() {
			List<Object> oList = new ArrayList<Object>();
			Iterator<CacheObject> iter = getSortedCacheObjects().iterator();
			while (iter.hasNext()) {
				CacheObject co = iter.next();
				oList.add(co.getObject());
			}
			return oList;
		}

		private List<CacheObject> getSortedCacheObjects() {
			List<CacheObject> coList = new ArrayList<CacheObject>();
			coList.addAll(this.cacheObjects.values());
			Collections.sort(coList, new CacheObjectComparator());
			return coList;
		}
		
		private int getNumberOfCacheObjects() {
			return this.cacheObjects.size();
		}
	}

	/**
	 * CacheObject Class - Used to store an object for debugging.
	 *                   - Represents one individual CacheObject for a Cache.
	 */
	private static class CacheObject {

		private long id = 0;                 // sort order
		private String objectKey = null;     // hash key
		private Object object = null;
		private long timestamp = 0;
		
		/** Constructor */
		private CacheObject(long id, String objectKey, Object obj, long timestamp) {
			this.id = id;
			this.objectKey = objectKey;
			this.object = obj;
			this.timestamp = timestamp;
		}
		
		private void reset() {
			this.objectKey = null;
			this.object = null;
		}
		
		private long getId() { return this.id; }
		private String getObjectKey() { return this.objectKey; }
		private Object getObject() { return this.object; }
		private long getTimestamp() { return this.timestamp; }
	}
	
	/**
	 * Class Comparator for CacheObject.
	 */
	private static class CacheObjectComparator implements Comparator<CacheObject> {
		
		public int compare(CacheObject co1, CacheObject co2) {
    		if (co1 == co2)
    			return 0;
    		else if (co1 == null)
    			return -1;
    		else if (co2 == null)
    			return 1;
    		
    		long id1 = co1.getId();
    		long id2 = co2.getId();
 
    		if (id1 < id2) {
    			return -1;
    		}

    		if (id1 > id2) {
    			return 1;
    		}
    		
    		return 0;   // should never happen - ids are unique accross all CacheObjects
		}
	}

	/**
	 * Class Comparator for ProfileChild.
	 */
    private static class ProfileChildComparator implements Comparator<ProfileChild> {

    	public int compare(ProfileChild child1, ProfileChild child2) {
    		if (child1 == child2)
    			return 0;
    		else if (child1 == null)
    			return -1;
    		else if (child2 == null)
    			return 1;
    		
    		ProfileObject p1 = child1.getProfile();
    		ProfileObject p2 = child2.getProfile();

    		String profileName1 = p1.getName() == null ? "" : p1.getName();
    		String profileName2 = p2.getName() == null ? "" : p2.getName();
    		if (!profileName1.equals(profileName2)) {
    			return profileName1.compareTo(profileName2);
    		}

    		String name1 = child1.getName() == null ? "" : child1.getName();
    		String name2 = child2.getName() == null ? "" : child2.getName();
    		if (!name1.equals(name2)) {
    			return name1.compareTo(name2);
    		}

    		return 0;
    	}
    }
}
