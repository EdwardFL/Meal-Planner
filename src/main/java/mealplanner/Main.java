package mealplanner;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String EXIT = "exit";
    public static void main(String[] args) throws SQLException {

        try (Connection connection = DatabaseManager.getConnection()) {
            Scanner scanner = new Scanner(System.in);

            DatabaseManager.createTables();
            while (true) {
                System.out.println("What would you like to do (add, show, plan, save, exit)?");
                String userInput = scanner.nextLine();

                switch (userInput) {
                    case EXIT:
                        System.out.println("Bye!");
                        return;
                    case "add":
                        addMealToDatabase(connection, scanner);
                        System.out.println("The meal has been added!");
                        break;
                    case "show":
                        printMeals(connection, scanner);
                        break;
                    case "plan":
                        printPlannedMeals(connection, scanner);
                        break;
                    case "save":
                        if(!DatabaseManager.isPlanSaved(connection))
                        {
                            System.out.println("Unable to save. Plan your meals first.");
                            break;
                        } else {
                            saveShoppingListToAFile(connection, scanner);
                        }
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void addMealToDatabase(Connection connection, Scanner scanner) {
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        String category = scanner.nextLine();
        while (!isValidCategory(category)) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            category = scanner.nextLine();
        }

        System.out.println("Input the meal's name:");
        String mealName = scanner.nextLine();
        while (!isValidName(mealName)) {
            System.out.println("Wrong format. Use letters only!");
            mealName = scanner.nextLine();
        }

        System.out.println("Input the ingredients:");
        String[] ingredients = scanner.nextLine().split(",");
        while (!isValidIngredients(ingredients)) {
            System.out.println("Wrong format. Use letters only!");
        }

        DatabaseManager.addMeal(connection, category, mealName, ingredients);
    }

    private static void saveShoppingListToAFile(Connection connection, Scanner scanner) {
        try {
            System.out.println("Input a filename:");
            String filename = scanner.nextLine();

            try (PrintWriter printWriter = new PrintWriter(filename)){
                DatabaseManager.getShoppingListForWeek(connection, printWriter);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("Saved!");
    }

    private static void printPlannedMeals(Connection connection, Scanner scanner) throws SQLException{
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            System.out.println(day);

            List<Meal> breakfastMeals = DatabaseManager.getMealsByCategory(connection, "breakfast");
            List<Meal> lunchMeals = DatabaseManager.getMealsByCategory(connection, "lunch");
            List<Meal> dinnerMeals = DatabaseManager.getMealsByCategory(connection, "dinner");

            Collections.sort(breakfastMeals);
            printMealsNameByCategory(breakfastMeals);
            System.out.println("Choose the breakfast for " + day + " from the list above:");
            String breakfastChoice = scanner.nextLine();
            while (!mealExists(breakfastChoice, breakfastMeals)) {
                System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                breakfastChoice = scanner.nextLine();
            }

            Collections.sort(lunchMeals);
            printMealsNameByCategory(lunchMeals);
            System.out.println("Choose the lunch for " + day + " from the list above:");
            String lunchChoice = scanner.nextLine();
            while (!mealExists(lunchChoice, lunchMeals)) {
                System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                lunchChoice = scanner.nextLine();
            }

            Collections.sort(dinnerMeals);
            printMealsNameByCategory(dinnerMeals);
            System.out.println("Choose the dinner for " + day + " from the list above:");
            String dinnerChoice = scanner.nextLine();
            while (!mealExists(dinnerChoice, dinnerMeals)) {
                System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                dinnerChoice = scanner.nextLine();
            }

            List<Plan> plans = new ArrayList<>();
            int breakfastId = DatabaseManager.getMealIdByName(connection, breakfastChoice);
            int lunchId = DatabaseManager.getMealIdByName(connection, lunchChoice);
            int dinnerId = DatabaseManager.getMealIdByName(connection, dinnerChoice);

            plans.add(new Plan(breakfastChoice, "breakfast", breakfastId, day));
            plans.add(new Plan(lunchChoice, "lunch", lunchId, day));
            plans.add(new Plan(dinnerChoice, "dinner", dinnerId, day));

            DatabaseManager.savePlan(connection, plans);
            System.out.println("Yeah! We planned the meals for " + day + ".\n");
        }
        printWeeklyPlan(connection);
    }

    private static boolean mealExists(String mealChoice, List<Meal> meals) {
        for (Meal meal : meals) {
            if (meal.getName().equalsIgnoreCase(mealChoice)) {
                return true;
            }
        }
        return false;
    }

    public static void printMeals(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        String categoryChoice = scanner.nextLine();
        while (!isValidCategory(categoryChoice)) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            categoryChoice = scanner.nextLine();
        }

        List<Meal> mealsByCategory = DatabaseManager.getMealsByCategory(connection, categoryChoice);
        showMeals(mealsByCategory, categoryChoice);
    }
    public static void showMeals(List<Meal> meals, String categoryChoice) throws SQLException{
        if (!meals.isEmpty()) {
            System.out.println("\nCategory: " + categoryChoice);
            for (Meal meal : meals) {
                System.out.println("Name: " + meal.getName());
                System.out.println("Ingredients:");
                for (int i = 0; i < meal.getIngredients().length; i++) {
                    System.out.println(meal.getIngredients()[i]);
                }

            }
        } else {
            System.out.println("No meals found.");
        }
    }
    public static void printMealsNameByCategory(List<Meal> meals) throws SQLException {
        for (Meal meal : meals) {
            System.out.println(meal.getName());
        }
    }

    public static void printWeeklyPlan(Connection connection) throws SQLException {
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            System.out.println(day);
            System.out.println("Breakfast: " + DatabaseManager.getMealOptionForDay(connection, day, "breakfast"));
            System.out.println("Lunch: " + DatabaseManager.getMealOptionForDay(connection, day, "lunch"));
            System.out.println("Dinner: " + DatabaseManager.getMealOptionForDay(connection, day, "dinner"));
            System.out.println();
        }
    }

    private static boolean isValidCategory(String category) {
        return category.equals("breakfast") || category.equals("lunch") || category.equals("dinner");
    }

    private static boolean isValidName(String name) {
        return name.matches("^[a-zA-Z ]+$");
    }

    private static boolean isValidIngredients(String[] ingredients) {
        for (String ingredient : ingredients) {
            ingredient = ingredient.trim();
            if (ingredient.isEmpty() || !ingredient.matches("[a-zA-Z ]+")) {
                return false;
            }
        }
        return true;
    }
}