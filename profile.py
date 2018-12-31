"""
profile.py
Author: Brian Daun
Disclaimer(s):
   The functionality in this object does not belong to any one company or entity.
   It is the combination of tools written over 20 years by the author.
   This object is for testing ONLY and should never be deployed in Production.
   Use at your own risk.

Description:
   Profile object based on a java tool (Profile.java) first written in the 1990s.
   The Profile object implements "Poor Man's Profiling" for time and memory.
   It is self-contained and can be inserted into any python code that is
   in-development for debugging and informational purposes.
   Please note this object is NOT a substitute for a real python profiler (product).

PROFILE ID:
   Each public API method is associated with a profile_id.  The profile_id can be
   associated explicitely in each API call or implicitely with the default-value.
   When the profile_id is implicit, it is associated by using the constant
   DEFAULT_PROFILE_ID.

CLOCK API
   A clock maintains the elapsed time (in milliseconds) between start
   and stop events. The elapsed time is accumulated between successive
   start and stops. For example, if a method is called 100 times and has
   a clock-start at its beginning and a clock-stop at its end, the elapsed
   time of all 100 calls will be stored by the clock.

   Each clock is uniquely identified by a profile_id and a clock_id.

   Example:
      profile.clock_start("get_datasets")
      ... other code executed here
      profile.clock_stop("get_datasets")
      print(profile.report())

"""
import time

ENABLED = True
DEFAULT_PROFILE_ID = "Profile1"
PROFILES = {}

# Return current milliseconds - round is needed because int performs a floor.
def cmillis():
   return int(round(time.time() * 1000))

#
# Profile API
#
def disable():
    global ENABLED
    ENABLED = False

def enable():
    global ENABLED
    ENABLED = True

def clear(profile_id=None):
    if (not ENABLED):
        return
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    profile = get_profile(profile_id)
    profile.clear()

def report(profile_id=None):
    if (not ENABLED):
        return ""
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    profile = get_profile(profile_id)
    return profile.get_report()

def get_clocks(profile_id=None):
    clist = []
    if (not ENABLED):
        return clist
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    profile = get_profile(profile_id)
    clist = profile.get_clocks()
    return clist

#
# Clock API
#
def clock_start(clock_id, profile_id=None):
    if (not ENABLED):
        return
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    millis = cmillis()
    clock = get_clock(profile_id, clock_id)
    clock.update_start(millis)

def clock_stop(clock_id, profile_id=None):
    if (not ENABLED):
        return
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    millis = cmillis()
    clock = get_clock(profile_id, clock_id)
    clock.update_stop(millis)

def clock_get_elapsed_time(clock_id, profile_id=None):
    if (not ENABLED):
        return
    if (profile_id is None):
        profile_id = DEFAULT_PROFILE_ID
    clock = get_clock(profile_id, clock_id)
    return clock.elapsed_time

#
# Profile Implementation
#
class Profile(object):
    def __init__(self, profile_id):
        self.profile_id = profile_id
        self.clocks = {}

    def clear(self):
        for clock_id in self.clocks:
            clock = self.clocks.get(clock_id)
            clock.release()
        self.clocks = {}

    def get_report(self):
        out = "\nProfile: "
        out += self.profile_id
        for clock_id in self.clocks:
            clock = self.clocks.get(clock_id)
            cstr = "\n  Clock: " + clock.clock_id + "  elapsed: " + str(clock.elapsed_time) + "  start/stop: " + str(clock.start_num) + "/" + str(clock.stop_num)
            out += cstr
        return out

    def get_clocks(self):
        clist = []
        for clock_id in self.clocks:
            clock = self.clocks.get(clock_id)
            clist.append(clock)
        return clist

def get_profile(profile_id):
    profile = None
    if (profile_id in PROFILES):
        profile = PROFILES[profile_id]
    else:
        profile = Profile(profile_id=profile_id)
        PROFILES[profile_id] = profile
    return profile

#
# Clock Implementation
#
class Clock(object):
    def __init__(self, profile_id, clock_id):
        self.profile_id = profile_id
        self.clock_id = clock_id
        self.start = -1
        self.stop = -1
        self.elapsed_time = 0
        self.start_num = 0
        self.stop_num = 0

    def release(self):
        self.profile_id = None
        self.clock_id = None

    def reset(self):
        self.start = -1
        self.stop = -1
        self.elapsed_time = 0
        self.start_num = 0
        self.stop_num = 0

    def update_start(self, millis):
        self.start = millis
        self.stop = -1
        self.start_num += 1

    def update_stop(self, millis):
        if (self.start != -1):
            self.stop = millis
            self.elapsed_time += (self.stop - self.start)
        self.stop_num += 1

def get_clock(profile_id, clock_id):
    clock = None
    profile = get_profile(profile_id)
    if (clock_id in profile.clocks):
        clock = profile.clocks[clock_id]
    else:
        clock = Clock(profile_id, clock_id)
        profile.clocks[clock_id] = clock
    return clock
