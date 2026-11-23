# AstroGestão (Android)

An executive business operations, client CRM, project & deliverable tracking, and cash-flow management Android application built with Kotlin, Jetpack Compose, Material 3, Room, DataStore, and Navigation Compose.

## Features

- **Operations Command Dashboard**: Live KPI metrics (Net Balance, Total Revenue, Active Projects, Open Tasks), custom Compose `Canvas` monthly Income vs. Expense cash-flow chart, active project deadlines, and interactive priority task checkoffs.
- **Projects & Service Orders**: Searchable, status-filtered project pipeline (`Planned`, `In Progress`, `Review`, `Completed`) with budgets, deadlines, completion sliders, deliverable task checklists, and direct revenue logging.
- **Client Directory (CRM)**: Searchable client accounts with segments, contact details, account notes, linked project tracking, and total billed metrics.
- **Cash-Flow Ledger**: Income and expense tracking with category breakdowns, net margin calculation, project linking, and transaction editing/deletion.
- **Adaptive UI & Theme**: Edge-to-edge Material 3 layout with `NavigationBar` on compact screens, `NavigationRail` on tablets/foldables, Celestial Dark & Ivory Light theme toggle, and customizable workspace name.
- **Local Persistence**: Offline-first Room SQLite database (`AstroRepository`) and Jetpack DataStore preferences, plus an optional one-tap sample workspace loader for exploring charts and workflows.
