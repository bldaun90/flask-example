package com.acme.profile;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;


public class TestProfile extends TestCase {
	
	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	/** Tests every method in the counter API. */
	public void testCounterAPI() throws Exception {
		// setup names
		String profileName = this.getClass().getSimpleName();   // name of class
		String counterName = "testCounterAPI";
		
		// Configure Profile behavior
		Profile.ENABLED = true;
		Profile.DEFAULT_PROFILE_NAME = profileName;             // default to name of class
		Profile.SEPARATE_PROFILES_BY_NEWLINE = true;

		//
		// Test API
		//
		Profile.counterReset(counterName);
		assertTrue(Profile.counterGet(counterName) == 0);
		
		Profile.counterInc(counterName);
		for (int i = 0; i < 5; i++) {
			Profile.counterInc(this, counterName);
		}
		Profile.counterInc(profileName, counterName);
		
		assertTrue(Profile.counterGet(counterName) == 7);
		assertTrue(Profile.counterGet(this, counterName) == 7);
		assertTrue(Profile.counterGet(profileName, counterName) == 7);
		
		Profile.counterSet(counterName, 99);
		assertTrue(Profile.counterGet(counterName) == 99);
		Profile.counterReset(counterName);
		assertTrue(Profile.counterGet(counterName) == 0);
		
		Profile.counterSet(this, counterName, 88);
		assertTrue(Profile.counterGet(this, counterName) == 88);
		Profile.counterReset(this, counterName);
		assertTrue(Profile.counterGet(this, counterName) == 0);
		
		Profile.counterSet(this, counterName, 111);
		assertTrue(Profile.counterGet(this, counterName) == 111);
		Profile.resetAllCounters();
		assertTrue(Profile.counterGet(this, counterName) == 0);
		
		Profile.counterSet(profileName, counterName, 77);
		assertTrue(Profile.counterGet(profileName, counterName) == 77);
		Profile.counterReset(profileName, counterName);
		assertTrue(Profile.counterGet(profileName, counterName) == 0);
		
		Profile.counterSet(counterName, 1000);
		Profile.counterInc(counterName);
		Profile.counterInc(counterName);
		Profile.counterInc(counterName);
		assertTrue(Profile.counterGet(counterName) == 1003);
		
		String reportAllCountersStr = Profile.reportAllCounters();
		assertTrue(reportAllCountersStr.equals(Profile.reportProfileCounters(this)));
		assertTrue(reportAllCountersStr.equals(Profile.reportProfileCounters(profileName)));
		System.out.print("\n\nAll Counters:\n\n" + reportAllCountersStr + "\n\n");
	}
	
	
	/** Tests every method in the clock API. */
	public void testClockAPI() throws Exception {
		// setup names
		String profileName = this.getClass().getSimpleName();   // name of class
		String clockName = "testClockAPI";
		String clockName2 = "Clock Total";
		
		// Configure Profile behavior
		Profile.ENABLED = true;
		Profile.DEFAULT_PROFILE_NAME = profileName;             // default to name of class
		Profile.SEPARATE_PROFILES_BY_NEWLINE = true;

		//
		// Test API
		//
		Profile.clockStart(clockName2);
		
		Profile.clockReset(clockName);
		assertTrue(Profile.clockGetElapsed(clockName) == 0);
		
		for (int i = 0; i < 4; i++) {
			Profile.clockStart(clockName);
			Thread.sleep(101);
			Profile.clockStop(clockName);
			
			Profile.clockStart(this, clockName);
			Thread.sleep(101);
			Profile.clockStop(this, clockName);
			
			Profile.clockStart(profileName, clockName);
			Thread.sleep(101);
			Profile.clockStop(profileName, clockName);
		}

		assertTrue(Profile.clockGetElapsed(clockName) > 1200);
		assertTrue(Profile.clockGetElapsed(this, clockName) > 1200);
		assertTrue(Profile.clockGetElapsed(profileName, clockName) > 1200);
		
		assertTrue(Profile.clockGetStartNumber(clockName) == 12);
		assertTrue(Profile.clockGetStartNumber(this, clockName) == 12);
		assertTrue(Profile.clockGetStartNumber(profileName, clockName) == 12);
		
		assertTrue(Profile.clockGetStopNumber(clockName) == 12);
		assertTrue(Profile.clockGetStopNumber(this, clockName) == 12);
		assertTrue(Profile.clockGetStopNumber(profileName, clockName) == 12);

		Profile.clockStop(clockName2);
		assertTrue(Profile.clockGetElapsed(clockName2) > 1200);
		assertTrue(Profile.clockGetElapsed(this, clockName2) > 1200);
		assertTrue(Profile.clockGetElapsed(profileName, clockName2) > 1200);
		
		String reportAllClocksStr = Profile.reportAllClocks();
		assertTrue(reportAllClocksStr.equals(Profile.reportProfileClocks(this)));
		assertTrue(reportAllClocksStr.equals(Profile.reportProfileClocks(profileName)));
		System.out.print("\n\nAll Clocks:\n\n" + reportAllClocksStr + "\n\n");
		
		Profile.clockReset(clockName);
		assertTrue(Profile.clockGetStartNumber(clockName) == 0);
		assertTrue(Profile.clockGetStopNumber(clockName) == 0);
		Profile.clockStart(clockName);
		Profile.clockStop(clockName);
		assertTrue(Profile.clockGetStartNumber(clockName) == 1);
		assertTrue(Profile.clockGetStopNumber(clockName) == 1);
		
		Profile.clockReset(this, clockName);
		assertTrue(Profile.clockGetStartNumber(this, clockName) == 0);
		assertTrue(Profile.clockGetStopNumber(this, clockName) == 0);
		Profile.clockStart(this, clockName);
		Profile.clockStop(this, clockName);
		assertTrue(Profile.clockGetStartNumber(this, clockName) == 1);
		assertTrue(Profile.clockGetStopNumber(this, clockName) == 1);
		
		Profile.clockReset(profileName, clockName);
		assertTrue(Profile.clockGetStartNumber(profileName, clockName) == 0);
		assertTrue(Profile.clockGetStopNumber(profileName, clockName) == 0);
		Profile.clockStart(profileName, clockName);
		Profile.clockStop(profileName, clockName);
		assertTrue(Profile.clockGetStartNumber(profileName, clockName) == 1);
		assertTrue(Profile.clockGetStopNumber(profileName, clockName) == 1);
		
		Profile.resetAllClocks();
		assertTrue(Profile.clockGetStartNumber(profileName, clockName) == 0);
		assertTrue(Profile.clockGetStopNumber(profileName, clockName) == 0);	
	}
	
	/** Tests every method in the chain API. */
	public void testChainAPI() throws Exception {
		// setup names
		String profileName = this.getClass().getSimpleName();   // name of class
		String chainName = "testChainAPI";
		String gcBeforeTest = "GC-before-test";
		String gcAfterTest = "GC-after-test";
		int numObjects = 200000;
		
		// Configure Profile behavior
		Profile.ENABLED = true;
		Profile.DEFAULT_PROFILE_NAME = profileName;             // default to name of class
		Profile.SEPARATE_PROFILES_BY_NEWLINE = true;
		Profile.SEPARATE_CHAINS_BY_NEWLINE = true;
		Profile.CHAIN_TOTALS_ROW = true;
		Profile.CHAIN_MEGABYTE_LEGEND = true;
		
		// Force a garbage collect before starting the test
		Profile.chainAddLink(profileName, gcBeforeTest, "before");
		Profile.gc();
		Profile.chainAddLink(profileName, gcBeforeTest, "after");

		//
		// Test API
		//
		Profile.chainReset(chainName);
		Profile.chainReset(this, chainName);
		Profile.chainReset(profileName, chainName);
		
		Profile.chainAddLink(chainName, "A");
		List<Person> listA = createSomeObjects(numObjects);
		assertTrue(listA.size() == numObjects);
		Profile.chainAddLink(this, chainName, "B");
		List<Person> listB = createSomeObjects(numObjects);
		assertTrue(listB.size() == numObjects);
		Profile.chainAddLink(profileName, chainName, "C");
		
		String reportProfileChainsStr = Profile.reportProfileChains(this);
		assertTrue(reportProfileChainsStr.equals(Profile.reportProfileChains(profileName)));
		assertTrue(Profile.reportChain(this, chainName).equals(Profile.reportChain(profileName, chainName)));
		assertTrue(Profile.reportChain(this, chainName).equals(Profile.reportChain(chainName)));
		System.out.print("\n\nAll Chains:\n\n" + reportProfileChainsStr + "\n");
		
		Profile.resetAllChains();
		Profile.chainAddLink(chainName, "x");
		Profile.chainAddLink(chainName, "y");
		Profile.chainAddLink(chainName, "z");
		Profile.resetAllChains();    // not really a way to test this reset - header information always returned
		
		// try to reclaim the memory
		reportProfileChainsStr = null;
		listA = null;
		listB = null;

		// Force a garbage collect before after the test
		Profile.chainAddLink(profileName, gcAfterTest, "before");
		Profile.gc();
		Profile.chainAddLink(profileName, gcAfterTest, "after");
		System.out.print("\n" + Profile.reportChain(profileName, gcAfterTest) + "\n\n");
	}
	
	/** Tests every method in the cache API. */
	public void testCacheAPI() throws Exception {
		// setup names
		String profileName = this.getClass().getSimpleName();   // name of class
		String cacheName = "test-cache";
		
		// Configure Profile behavior
		Profile.ENABLED = true;
		Profile.DEFAULT_PROFILE_NAME = profileName;             // default to name of class
		Profile.CACHE_SHOW_OBJECT_HEADER = true;

		//
		// Test API
		//
		Profile.cacheReset(cacheName);
		Profile.cacheReset(this, cacheName);
		Profile.cacheReset(profileName, cacheName);

		// Test cache with String objects
		Profile.cacheAddObject(cacheName, "First", "My");
		Profile.cacheAddObject(this, cacheName, "Second", "name");
		Profile.cacheAddObject(profileName, cacheName, "Third", "is");
		Profile.cacheAddObject(cacheName, "Fourth", "HAL");
		Profile.cacheAddObject(cacheName, "Fifth", "!");
		//
		List<Object> list1 = Profile.cacheGetObjects(cacheName);
		List<Object> list2 = Profile.cacheGetObjects(this, cacheName);
		List<Object> list3 = Profile.cacheGetObjects(profileName, cacheName);
		assertTrue(list1.size() == list2.size());
		assertTrue(list1.size() == list3.size());
		//
		Object obj1_list1 = list1.get(0);
		Object obj1_list2 = list2.get(0);
		Object obj1_list3 = list3.get(0);
		assertTrue("My".equals((String)obj1_list1));
		assertTrue("My".equals((String)obj1_list2));
		assertTrue("My".equals((String)obj1_list3));
		//
		Object obj1 = Profile.cacheGetObject(cacheName, "Fourth");
		Object obj2 = Profile.cacheGetObject(this, cacheName, "Fourth");
		Object obj3 = Profile.cacheGetObject(profileName, cacheName, "Fourth");
		assertTrue("HAL".equals((String)obj1));
		assertTrue("HAL".equals((String)obj2));
		assertTrue("HAL".equals((String)obj3));

		String cacheReport = Profile.reportCache(cacheName);
		assertTrue(cacheReport.equals(Profile.reportCache(this, cacheName)));
		assertTrue(cacheReport.equals(Profile.reportCache(profileName, cacheName)));
		System.out.print("\n\n" + cacheReport + "\n");
		
		// Test Reset
		assertTrue(Profile.cacheGetObjects(cacheName).size() == 5);
		Profile.cacheReset(cacheName);
		assertTrue(Profile.cacheGetObjects(cacheName).size() == 0);
		
		// Test cache with Person objects
		Person a = new Person("Rip", "Van", "Winkle");
		Person b = new Person("George", "C", "Scott");
		Person c = new Person("James", "Tiberius", "Kirk");
		Person d = new Person("H", "A", "L");
		Person e = new Person("I", "B", "M");
		
		Profile.cacheAddObject(cacheName, null, a);
		Profile.cacheAddObject(cacheName, null, b);
		Profile.cacheAddObject(cacheName, null, c);
		Profile.cacheAddObject(cacheName, null, d);
		Profile.cacheAddObject(cacheName, null, e);
		
		Profile.CACHE_SHOW_OBJECT_HEADER = false;
		System.out.print("\n" + Profile.reportCache(cacheName) + "\n\n");
	}
	
	
	/** Creates some objects. */
	private List<Person> createSomeObjects(int number) {
		ArrayList<Person> pList = new ArrayList<Person>();
		String fname = "John";
		String mname = "Van";
		String lname = "Smith";
		for (int i = 0; i < number; i++) {
			Person p = new Person(fname + i, mname + i, lname + i);
			pList.add(p);
		}
		return pList;
	}
	
	/** 
	 * Tries to reclaim the memory for a list of objects.
	 * NOTE: It appears that this method does not increase the amount
	 *       of memory reclaimed.  I.E. - The garbage collector does
	 *       not need help reclaiming the strings for each object. 
	 */
	@SuppressWarnings("unused")
	private void reclaimObjects(List<Person> pList) {
		Iterator<Person> iter = pList.iterator();
		while (iter.hasNext()) {
			Person p = iter.next();
			p.destroy();
		}
	}

	/**
	 * Class for profiling chains.
	 */
	private static class Person {
		private String fname = null;
		private String mname = null;
		private String lname = null;
		@SuppressWarnings("unused")
		private String fullname = null;
		private Person(String first, String middle, String last) {
			this.fname = first;
			this.mname = middle;
			this.lname = last;
			this.fullname = first + middle + last;
		}
		private void destroy() {
			this.fname = null;
			this.mname = null;
			this.lname = null;
			this.fullname = null;
		}
		public String toString() {
			return this.fname + " " + this.mname + " " + this.lname;
		}
	}
}
