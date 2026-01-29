# ⚙️ CONFIGURATION GUIDE - Feature Toggles

## 📋 COMPLETE LIST OF FEATURE TOGGLES

All features can be enabled/disabled in `config.yml` under the `features:` section!

---

## 🎛️ MASTER SWITCHES

### **Core Features:**

```yaml
features:
  teams: true                # Enable/disable entire team system
  team-base: true            # Allow teams to set/teleport to base
  friends: true              # Enable friends system
  homes: true                # Enable home locations
  economy: true              # Enable economy integration
  chat-formatting: true      # Enable chat formatting
  missions: true             # Enable mission system
```

### **Bank Features:**

```yaml
  personal-bank: true        # Personal coin storage and transfers
  team-bank: true            # Team shared vault
```

### **GUI Features:**

```yaml
  item-menu: true            # Right-click item to open TPA menu
  bedrock-forms: true        # Bedrock Edition form menus
  java-gui: true             # Java Edition inventory GUIs
```

### **Social Features:**

```yaml
  blocking: true             # Allow players to block others
  muting: true               # Allow players to mute others
  admin-spy: true            # Admins see all TPA requests
```

---

## 🔧 WHAT EACH TOGGLE DOES

### **`teams: true/false`**
- **Enables:** All team commands (`/team create`, `/team invite`, etc.)
- **Disables:** Team creation, management, and all team features
- **Affects:** Team base, team bank, team chat, allies

### **`team-base: true/false`**
- **Enables:** `/team base`, `/team setbase`, `/tpateambase` commands
- **Disables:** Team base teleportation and setting
- **Note:** Teams feature must be enabled for this to work

### **`friends: true/false`**
- **Enables:** `/friend` command and friend management
- **Disables:** Friend system entirely
- **Affects:** Friend lists, friend requests

### **`homes: true/false`**
- **Enables:** `/home`, `/sethome`, `/delhome` commands
- **Disables:** Personal home locations
- **Note:** Separate from team homes

### **`economy: true/false`**
- **Enables:** Vault integration, costs for commands
- **Disables:** All economy features
- **Affects:** Command costs, bank systems

### **`chat-formatting: true/false`**
- **Enables:** Team chat formatting
- **Disables:** Custom chat formats
- **Affects:** Team chat appearance

### **`missions: true/false`**
- **Enables:** Mission board, mission creation
- **Disables:** All mission features
- **Affects:** `/team mission` commands

### **`personal-bank: true/false`**
- **Enables:** Personal coin storage
- **Disables:** Personal bank access
- **Affects:** Personal Bank GUI/forms

### **`team-bank: true/false`**
- **Enables:** Team shared vault
- **Disables:** Team bank access
- **Affects:** Team Bank GUI/forms, deposits/withdrawals

### **`item-menu: true/false`**
- **Enables:** Right-click compass to open menu
- **Disables:** Item menu functionality
- **Affects:** `/tpaitem` command

### **`bedrock-forms: true/false`**
- **Enables:** Form-based menus for Bedrock players
- **Disables:** Bedrock forms (falls back to chat)
- **Affects:** `/tpamenu` on Bedrock

### **`java-gui: true/false`**
- **Enables:** Inventory-based GUIs for Java players
- **Disables:** Java GUIs (falls back to chat)
- **Affects:** `/tpamenu` on Java

### **`blocking: true/false`**
- **Enables:** `/tpablock`, `/tpaunblock` commands
- **Disables:** Player blocking system
- **Affects:** Block list management

### **`muting: true/false`**
- **Enables:** `/tpamute`, `/tpaunmute` commands
- **Disables:** Player muting system
- **Affects:** Mute list management

### **`admin-spy: true/false`**
- **Enables:** Admins see all TPA requests
- **Disables:** Admin spy notifications
- **Affects:** Players with `crosstpa.admin.spy` permission

---

## 📝 EXAMPLE CONFIGURATIONS

### **Minimal Setup (TPA Only):**
```yaml
features:
  teams: false
  team-base: false
  friends: false
  homes: false
  economy: false
  chat-formatting: false
  missions: false
  personal-bank: false
  team-bank: false
  item-menu: true
  bedrock-forms: true
  java-gui: true
  blocking: true
  muting: true
  admin-spy: true
```

### **Full Featured Server:**
```yaml
features:
  teams: true
  team-base: true
  friends: true
  homes: true
  economy: true
  chat-formatting: true
  missions: true
  personal-bank: true
  team-bank: true
  item-menu: true
  bedrock-forms: true
  java-gui: true
  blocking: true
  muting: true
  admin-spy: true
```

### **PvP/Faction Server:**
```yaml
features:
  teams: true
  team-base: true          # For team HQ
  friends: false
  homes: true
  economy: true
  chat-formatting: true
  missions: true
  personal-bank: true
  team-bank: true
  item-menu: true
  bedrock-forms: true
  java-gui: true
  blocking: true
  muting: true
  admin-spy: true
```

### **Casual/Survival Server:**
```yaml
features:
  teams: false
  team-base: false
  friends: true
  homes: true
  economy: false
  chat-formatting: false
  missions: false
  personal-bank: false
  team-bank: false
  item-menu: true
  bedrock-forms: true
  java-gui: true
  blocking: true
  muting: false
  admin-spy: false
```

---

## ⚠️ IMPORTANT NOTES

1. **Restart Required:** Changes require `/crosstpa reload` or server restart
2. **Dependencies:** Some features depend on others:
   - `team-base` requires `teams: true`
   - `team-bank` requires `teams: true`
   - `personal-bank` and `team-bank` require `economy: true`
   - `missions` requires `teams: true`

3. **Default Values:** If a toggle is missing, it defaults to `true`

4. **Player Messages:** When disabled, players see:
   - "This feature is currently disabled!"
   - "Team base feature is disabled!"
   - etc.

---

## 🔄 HOW TO CHANGE SETTINGS

1. Open `plugins/CrossTPA/config.yml`
2. Find the `features:` section
3. Change `true` to `false` (or vice versa)
4. Save the file
5. Run `/crosstpa reload` or restart server
6. ✅ Changes applied!

---

## 🎯 RECOMMENDED SETTINGS

### **For Most Servers:**
- Keep all features **enabled** for full functionality
- Disable `admin-spy` if you don't want admins seeing requests
- Disable `missions` if you don't use the mission system

### **For Performance:**
- All toggles have minimal performance impact
- Disabling unused features slightly reduces memory usage

### **For Simplicity:**
- Disable `teams`, `friends`, `missions` for basic TPA only
- Keep `homes`, `blocking`, `muting` enabled

---

## ✅ TESTING TOGGLES

After changing a toggle, test it:

1. **Set to `false`**
2. Try the command (e.g., `/team base`)
3. Should see: "Feature is disabled!"
4. **Set back to `true`**
5. Run `/crosstpa reload`
6. Try command again
7. Should work normally!

---

## 🎉 SUMMARY

**ALL FEATURES ARE CONFIGURABLE!**

✅ 14 individual feature toggles  
✅ Easy on/off switches  
✅ No code changes needed  
✅ Instant reload support  
✅ Clear error messages  
✅ Flexible configurations  

**Customize CrossTPA to fit YOUR server!** 🚀
