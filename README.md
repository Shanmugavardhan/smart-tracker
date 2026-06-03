# AI Expense Tracker (Milestone 1)

This project is a personal finance Android application. Currently, it implements Milestone 1, which provides a fully functional local expense tracker.

## Features (Milestone 1)
- Add Expenses (with amount, description, and category)
- Edit Expenses
- Delete Expenses
- View Monthly Summary

## Architecture
This app follows Clean Architecture principles using:
- **Presentation Layer**: Jetpack Compose, ViewModels, Hilt
- **Domain Layer**: Core models (`Expense`, `Category`) and Use Cases/Repository interfaces
- **Data Layer**: Room Database, DAO implementations

## Testing
The application strictly requires unit and integration tests before a feature is considered complete. Tests are located in `src/test/` and `src/androidTest/`.
