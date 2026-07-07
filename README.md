# LinkRouter

LinkRouter is an Android application that acts as a **smart default browser router**. Instead of opening links directly in a single browser, it intercepts all web links and routes them to different browsers based on user-defined rules.

It is designed to be:
- Fast (instant routing, no unnecessary processing)
- Private (no analytics or tracking)
- Fully local (no backend server required)
- Flexible (domain-based rules + optional redirect resolution)

---

## ✨ Features

### 🔀 Smart Browser Routing
Route links to different browsers depending on rules:
- `twitter.com` → Firefox
- `github.com` → Chrome
- `youtube.com` → Brave

### 🌐 Match Modes
- Exact URL match
- Domain match (includes subdomains)

### 🌍 Multi-Browser Support
Choose any installed browser per rule:
- Chrome
- Firefox
- Brave
- Samsung Internet
- Any browser supporting Android intents

### 🔗 Optional Redirect Resolution
Supports resolving shortened or redirect links before routing:
- `t.co`
- `bit.ly`
- `is.gd`

### ⚡ Fast Execution
- No background services
- No polling
- Event-driven only
- Instant intent handling

---

## 📱 How It Works

1. User clicks a link in any app
2. Android sends the link to LinkRouter (set as default browser)
3. LinkRouter evaluates routing rules
4. (Optional) resolves redirect links like `t.co`
5. Opens the link directly in that browser
6. Exits immediately

---

## 🧠 Rule System

Each rule consists of:

- Match type:
    - Exact URL
    - Domain (includes subdomains)
- Target browser
- Priority
- Enabled/disabled toggle
