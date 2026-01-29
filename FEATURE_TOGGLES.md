# ⚙️ COMPLETE FEATURE CONTROL - config.yml

## 🎛️ ALL AVAILABLE TOGGLES

CrossTPA gives you **FULL CONTROL** over every feature! Turn anything on or off!

---

## 📋 MASTER FEATURE SWITCHES

Located in `config.yml` under `features:` section:

```yaml
features:
  # Core Features
  teams: true                # Entire team system
  team-base: true            # Team base teleportation
  friends: true              # Friends system
  homes: true                # Personal homes
  economy: true              # Economy integration
  chat-formatting: true      # Chat formatting
  missions: true             # Mission board
  
  # Bank Features
  personal-bank: true        # Personal coin storage
  team-bank: true            # Team shared vault
  
  # GUI Features
  item-menu: true            # Right-click item menu
  bedrock-forms: true        # Bedrock forms
  java-gui: true             # Java GUIs
  
  # Social Features
  blocking: true             # Player blocking
  muting: true               # Player muting
  admin-spy: true            # Admin spy mode
```

---

## 🏆 DETAILED TEAM FEATURE TOGGLES

Located in `config.yml` under `teams.features:` section:

```yaml
teams:
  features:
    team-creation: true      # /team create
    team-chat: true          # /team chat
    team-coins: true         # Team coin system
    team-home: true          # /team sethome
    team-allies: true        # /team ally
    team-pvp-toggle: true    # /team pvp
    team-rename: true        # /team rename
    team-transfer: true      # /team transfer
    team-roles: true         # promote/demote
    team-color: true         # /team color
    team-kick: true          # /team kick
    team-invite: true        # /team invite
    team-leave: true         # /team leave
    team-disband: true       # /team disband
```

---

## 🔧 WHAT EACH TEAM TOGGLE DOES

### **`team-creation: true/false`**
- **Enables:** `/team create <name>`
- **Disables:** Creating new teams
- **Note:** Existing teams still work

### **`team-chat: true/false`**
- **Enables:** `/team chat` toggle
- **Disables:** Team chat feature
- **Note:** Players can't use team chat

### **`team-coins: true/false`**
- **Enables:** Team coin system, `/team coins`
- **Disables:** All coin-related features
- **Affects:** Personal bank, team bank, transfers

### **`team-home: true/false`**
- **Enables:** `/team sethome`, `/team home`
- **Disables:** Team home teleportation
- **Note:** Different from `team-base`

### **`team-allies: true/false`**
- **Enables:** `/team ally` commands
- **Disables:** Ally system
- **Affects:** Ally requests, ally list

### **`team-pvp-toggle: true/false`**
- **Enables:** `/team pvp` toggle
- **Disables:** Friendly fire toggle
- **Note:** Uses `default-friendly-fire` setting

### **`team-rename: true/false`**
- **Enables:** `/team rename <name>`
- **Disables:** Renaming teams
- **Note:** Team names become permanent

### **`team-transfer: true/false`**
- **Enables:** `/team transfer <player>`
- **Disables:** Ownership transfers
- **Note:** Only original owner can lead

### **`team-roles: true/false`**
- **Enables:** `/team promote`, `/team demote`
- **Disables:** Role management
- **Note:** All members have same rank

### **`team-color: true/false`**
- **Enables:** `/team color <color>`
- **Disables:** Changing team colors
- **Note:** Teams keep default color

### **`team-kick: true/false`**
- **Enables:** `/team kick <player>`
- **Disables:** Kicking members
- **Note:** Players can only leave voluntarily

### **`team-invite: true/false`**
- **Enables:** `/team invite <player>`
- **Disables:** Inviting new members
- **Note:** Teams can't grow

### **`team-leave: true/false`**
- **Enables:** `/team leave`
- **Disables:** Leaving teams
- **Note:** Players stuck in teams (use carefully!)

### **`team-disband: true/false`**
- **Enables:** `/team disband`
- **Disables:** Disbanding teams
- **Note:** Teams become permanent

---

## 💡 EXAMPLE CONFIGURATIONS

### **Locked Teams (No Changes):**
```yaml
teams:
  features:
    team-creation: true      # Can create
    team-chat: true
    team-coins: true
    team-home: true
    team-allies: true
    team-pvp-toggle: true
    team-rename: false       # Can't rename
    team-transfer: false     # Can't transfer
    team-roles: false        # No promotions
    team-color: false        # Can't change color
    team-kick: false         # Can't kick
    team-invite: false       # Can't invite
    team-leave: false        # Can't leave
    team-disband: false      # Can't disband
```

### **Casual Teams (Flexible):**
```yaml
teams:
  features:
    team-creation: true
    team-chat: true
    team-coins: false        # No economy
    team-home: true
    team-allies: false       # No allies
    team-pvp-toggle: false   # Always friendly
    team-rename: true
    team-transfer: true
    team-roles: true
    team-color: true
    team-kick: true
    team-invite: true
    team-leave: true
    team-disband: true
```

### **Competitive/PvP Teams:**
```yaml
teams:
  features:
    team-creation: true
    team-chat: true
    team-coins: true         # Full economy
    team-home: true
    team-allies: true        # Alliances allowed
    team-pvp-toggle: true    # Can toggle FF
    team-rename: true
    team-transfer: true
    team-roles: true         # Hierarchy
    team-color: true
    team-kick: true
    team-invite: true
    team-leave: true
    team-disband: true
```

### **Simple Social Teams:**
```yaml
teams:
  features:
    team-creation: true
    team-chat: true
    team-coins: false        # No coins
    team-home: false         # No homes
    team-allies: false       # No allies
    team-pvp-toggle: false   # No PvP
    team-rename: false       # Fixed names
    team-transfer: false     # Fixed owner
    team-roles: false        # Everyone equal
    team-color: true         # Can customize
    team-kick: true
    team-invite: true
    team-leave: true
    team-disband: true
```

---

## 🎯 QUICK DISABLE GUIDE

### **Disable Team Economy:**
```yaml
teams:
  features:
    team-coins: false
```

### **Disable Team Chat:**
```yaml
teams:
  features:
    team-chat: false
```

### **Disable PvP Features:**
```yaml
teams:
  features:
    team-pvp-toggle: false
```

### **Disable Allies:**
```yaml
teams:
  features:
    team-allies: false
```

### **Disable Team Changes:**
```yaml
teams:
  features:
    team-rename: false
    team-transfer: false
    team-color: false
```

---

## ⚠️ IMPORTANT NOTES

1. **Master Switch:** `features.teams: false` disables ALL team features
2. **Dependencies:** Some features require others:
   - `team-coins` requires `economy: true`
   - `team-base` requires `features.team-base: true`
   - `team-home` is separate from `team-base`

3. **Reload:** Use `/crosstpa reload` after changes

4. **Error Messages:** Disabled features show:
   - "This feature is currently disabled!"
   - Clear feedback to players

5. **Existing Data:** Disabling features doesn't delete data
   - Teams persist
   - Coins remain
   - Homes stay saved

---

## 🔄 HOW TO USE

1. Open `plugins/CrossTPA/config.yml`
2. Find the feature you want to control
3. Change `true` to `false` (or vice versa)
4. Save file
5. Run `/crosstpa reload`
6. ✅ Done!

---

## ✅ TESTING

After changing a toggle:

1. Try the command
2. If disabled: See error message
3. If enabled: Command works
4. Test with different players/roles
5. Verify behavior matches expectations

---

## 🎉 SUMMARY

**TOTAL CONTROL OVER CROSSTPA!**

✅ 16 master feature toggles  
✅ 14 detailed team toggles  
✅ **30+ individual controls!**  
✅ No code changes needed  
✅ Instant reload support  
✅ Mix and match features  
✅ Perfect for any server type  

**Configure CrossTPA EXACTLY how you want it!** 🚀
