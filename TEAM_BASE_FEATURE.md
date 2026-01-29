# 🏠 TEAM BASE FEATURE - COMPLETE GUIDE

## ✅ NEW FEATURE ADDED!

Team leaders and co-leaders can now **set and manage a team base** location!

---

## 🎮 COMMANDS AVAILABLE

### **For All Team Members:**

#### `/team base`
- **Teleports** you to your team's base location
- Works for all team members
- Requires team base to be set first

#### `/tpateambase`
- **Standalone command** - same as `/team base`
- Quick access to team base teleportation

---

### **For Leaders & Co-Leaders Only:**

#### `/team base set`
- **Sets** the team base at your current location
- Can be **changed anytime** by leaders/co-leaders
- Overwrites previous base location

#### `/team setbase`
- **Alternative command** - same as `/team base set`
- Sets base at current location

#### `/tpateambase set`
- **Standalone version** - sets base location
- Same permissions as above

---

## 🔑 PERMISSIONS

### **Who Can Set Base:**
- ✅ **Team Leader** (owner)
- ✅ **Co-Leaders** (promoted members)
- ❌ Regular members (cannot set)

### **Who Can Teleport:**
- ✅ **ALL team members** can use `/team base` or `/tpateambase`
- No special permission needed to teleport

---

## 📋 USAGE EXAMPLES

### **Setting Team Base (Leader/Co-Leader):**
```
1. Go to desired base location
2. Type: /team base set
   OR: /team setbase
   OR: /tpateambase set
3. ✅ "Team base set at your current location!"
```

### **Changing Base Location:**
```
Leaders/Co-leaders can change it anytime:
1. Go to new location
2. Type: /team base set
3. ✅ Old base overwritten with new location!
```

### **Teleporting to Base (Any Member):**
```
Type: /team base
  OR: /tpateambase
✅ Teleported to team base!
```

---

## 🏗️ HOW IT WORKS

### **Base Storage:**
- Team base uses the existing `home` field in team data
- Saved automatically to `teams.yml`
- Persists across server restarts
- One base per team

### **Permission System:**
- Uses existing team role system
- Leaders have `LEADER` role
- Co-leaders have `CO_LEADER` role
- Only these roles can set base

---

## 💡 USE CASES

### **1. Team Headquarters:**
Set your main HQ as the team base for quick access

### **2. Resource Gathering:**
Set base near resource-rich areas

### **3. PvP Staging:**
Quick rally point for team battles

### **4. Trading Post:**
Set base at your team's shop/market

### **5. Event Locations:**
Change base for special events

---

## 🔄 COMMAND ALIASES

All these work the same:

### **Teleport to Base:**
- `/team base`
- `/team home`
- `/tpateambase`

### **Set Base:**
- `/team base set`
- `/team setbase`
- `/team sethome`
- `/tpateambase set`

---

## ⚠️ IMPORTANT NOTES

1. **Team Required:** Must be in a team to use these commands
2. **Base Not Set:** If no base is set, members get a message telling them to ask leaders to set it
3. **Overwrite Warning:** Setting a new base **replaces** the old one (no confirmation)
4. **Cross-Dimensional:** Base can be in any world (Overworld, Nether, End)
5. **Instant Teleport:** No cooldown or cost (configurable in future)

---

## 📱 BEDROCK SUPPORT

✅ **Fully compatible** with Bedrock Edition!
- All commands work via chat
- GUI forms show team base options
- Same functionality as Java Edition

---

## 🎨 MESSAGES

### **Success Messages:**
- ✅ "Team base set at your current location!"
- ✅ "Teleported to team base!"

### **Error Messages:**
- ❌ "You are not in a team!"
- ❌ "Team base not set! Leaders can use /tpateambase set to set it."
- ❌ "Failed to set base. Only leaders and co-leaders can set the team base!"

---

## 🚀 DEPLOYMENT

**File:** `CrossTPA.jar` (in `target/` folder)

**Installation:**
1. Replace old `CrossTPA.jar` in server's `plugins/` folder
2. Restart server
3. Commands automatically available!

**No config changes needed!**

---

## ✅ TESTING CHECKLIST

- [ ] Leader can set base with `/team base set`
- [ ] Leader can set base with `/tpateambase set`
- [ ] Co-leader can set base
- [ ] Regular member **cannot** set base
- [ ] All members can teleport with `/team base`
- [ ] All members can teleport with `/tpateambase`
- [ ] Base persists after server restart
- [ ] Changing base overwrites old location
- [ ] Works in all dimensions
- [ ] Error messages show correctly

---

## 🎉 SUMMARY

**Team Base Feature is COMPLETE!**

✅ Leaders & co-leaders can set/change base anytime  
✅ All members can teleport to base  
✅ Multiple command options for convenience  
✅ Fully integrated with existing team system  
✅ Bedrock compatible  
✅ Persistent storage  

**Ready to use!** 🚀
