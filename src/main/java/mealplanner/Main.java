package mealplanner;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String EXIT = "exit";
    private static final String DB_URL = "jdbc:postgresql:meals_db";
    private static final String USER = "postgres";
    private static final String PASS = "1111";
    private static List<Meal> meals = new ArrayList<>();
    private static List<Meal> mealsByCategory = new ArrayList<>();
    private static boolean IS_PLAN_SAVED = false;
    public static void main(String[] args) throws SQLException {

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
            createTables(connection);
            loadMealsData(connection);

            Scanner scanner = new Scanner(System.in);
            String userInput;

            while (true) {
                System.out.println("What would you like to do (add, show, plan, save, exit)?");
                userInput = scanner.nextLine();
                switch (userInput) {
                    case EXIT:
                        System.out.println("Bye!");
                        return;
                    case "add":
                        Meal meal = addMeal(connection, scanner);
                        meals.add(meal);
                        System.out.println("The meal has been added!");
                        break;
                    case "show":
                        printMeals(connection, scanner);
                        break;
                    case "plan":
                        printPlannedMeals(connection, scanner);
                        break;
                    case "save":
                        if(!isPlanSaved(connection))
                        {
                            System.out.println("Unable to save. Plan your meals first.");
                            break;
                        } else {
                            saveShoppingListToAFile(connection, scanner);
                        }
                        break;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createTables(Connection connection) throws SQLException {

        try (Statement statement = connection.createStatement()) {
            ResultSet mealsTable = connection.getMetaData().getTables(null, null, "meals", null);
            if (!mealsTable.next()) {
                statement.executeUpdate("CREATE TABLE meals (" +
                        "meal_id integer PRIMARY KEY," +
                        "category VARCHAR," +
                        "meal VARCHAR)");
                statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS meals_seq START 1");
            }

            ResultSet ingredientsTable = connection.getMetaData().getTables(null, null, "ingredients", null);
            if (!ingredientsTable.next()) {
                statement.executeUpdate("CREATE TABLE ingredients (" +
                        "ingredient_id integer PRIMARY KEY ," +
                        "ingredient VARCHAR," +
                        "meal_id INTEGER REFERENCES meals (meal_id))");
                statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS ingredients_seq START 1");
            }

            ResultSet planTable = connection.getMetaData().getTables(null, null, "plan", null);
            if (!planTable.next()) {
                statement.executeUpdate("CREATE TABLE plan (" +
                        "plan_id SERIAL PRIMARY KEY," +
                        "meal_day VARCHAR," +
                        "meal_option VARCHAR," +
                        "meal_category VARCHAR," +
                        "meal_id_meals INTEGER REFERENCES meals (meal_id))");
                statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS plan_seq START 1");
            }
        }
    }
    private static void createSequences(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS meal_id_sequence START WITH 1 INCREMENT BY 1");
            statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS ingredient_id_sequence START WITH 1 INCREMENT BY 1");
        }
    }

    private static List<String> loadIngredientsForMeal(Connection connection, int mealId) throws SQLException {
        List<String> ingredients = new ArrayList<>();
        String selectIngredientsSQL = "SELECT * FROM ingredients";

        try (PreparedStatement ingredientsStatement = connection.prepareStatement(selectIngredientsSQL)) {
            ResultSet ingredientsResultSet = ingredientsStatement.executeQuery();

            while (ingredientsResultSet.next()) {
                int ingredientMealId = ingredientsResultSet.getInt("meal_id");
                if (ingredientMealId == mealId) {
                    String ingredient = ingredientsResultSet.getString("ingredient");
                    ingredients.add(ingredient);
                }
            }
        }
        return ingredients;
    }

    private static void saveShoppingListToAFile(Connection connection, Scanner scanner) {
        try {
            System.out.println("Input a filename:");
            String filename = scanner.nextLine();

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

            try (PrintWriter printWriter = new PrintWriter(filename)){
                getShoppingListForWeek(connection, printWriter);
            }
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        System.out.println("Saved!");
    }

    private static boolean isPlanSaved(Connection connection) throws SQLException {
        String checkPlanSQL = "SELECT COUNT(*) AS count FROM plan";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(checkPlanSQL)) {
            resultSet.next();
            int rowCount = resultSet.getInt("count");
            return rowCount > 0;
        }
    }

    private static void getShoppingListForWeek(Connection connection, PrintWriter printWriter) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT ingredient, SUM(1) as total_count FROM ingredients " +
                        "JOIN plan ON ingredients.meal_id = plan.meal_id_meals " +
                        "GROUP BY ingredient"
        )) {
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    String ingredient = resultSet.getString("ingredient");
                    int totalCount = resultSet.getInt("total_count");
                    String shoppingListItem = totalCount > 1 ? ingredient + " x" + totalCount : ingredient;
                    printWriter.println(shoppingListItem);
                }
            }
        }
    }
    private static List<Meal> getMealsByCategory(Connection connection, String categoryChoice) throws SQLException {
        String selectMealsSQL = "SELECT * FROM meals WHERE category = ?";
        List<Meal> mealsList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectMealsSQL)) {
            preparedStatement.setString(1, categoryChoice);

            try (ResultSet mealResultSet = preparedStatement.executeQuery()) {

                while (mealResultSet.next()) {
                    int mealId = mealResultSet.getInt("meal_id");
                    String category = mealResultSet.getString("category");
                    String name = mealResultSet.getString("meal");
                    List<String> ingredients = loadIngredientsForMeal(connection, mealId);

                    Meal meal = new Meal(name, category, ingredients.toArray(new String[0]));
                    mealsList.add(meal);
                }
            }
        }
        return mealsList;
    }

    private static int getMealIdByName(Connection connection, String mealName) throws SQLException {
        String query = "SELECT meal_id FROM meals WHERE meal = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setString(1, mealName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("meal_id");
                }
            }
        }
        throw new SQLException("Meal not found: " + mealName);
    }

    private static void loadMealsData(Connection connection) throws SQLException {
        String selectMealsSQL = "SELECT * FROM meals";

        try (Statement statement = connection.createStatement()) {
            ResultSet mealResultSet = statement.executeQuery(selectMealsSQL);

            while (mealResultSet.next()) {
                int mealId = mealResultSet.getInt("meal_id");
                String category = mealResultSet.getString("category");
                String name = mealResultSet.getString("meal");
                List<String> ingredients = loadIngredientsForMeal(connection, mealId);

                Meal meal = new Meal(name, category, ingredients.toArray(new String[0]));
                meals.add(meal);
            }
        }
    }

    private static void savePlan(Connection connection, List<Plan> plans) throws SQLException {
        String insertPlanSQL = "INSERT INTO plan (meal_day, meal_option, meal_category, meal_id_meals) " +
                "VALUES (?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(insertPlanSQL)) {
            for (Plan plan : plans) {
                preparedStatement.setString(1, plan.getMealDay());
                preparedStatement.setString(2, plan.getMealOption());
                preparedStatement.setString(3, plan.getMealCategory());
                preparedStatement.setInt(4, plan.getMealIdMeals());
                preparedStatement.executeUpdate();
            }
        }
    }

    private static void printPlannedMeals(Connection connection, Scanner scanner) throws SQLException{
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};

        for (String day : days) {
            System.out.println(day);

            List<Meal> breakfastMeals = getMealsByCategory(connection, "breakfast");
            List<Meal> lunchMeals = getMealsByCategory(connection, "lunch");
            List<Meal> dinnerMeals = getMealsByCategory(connection, "dinner");

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
            int breakfastId = getMealIdByName(connection, breakfastChoice);
            int lunchId = getMealIdByName(connection, lunchChoice);
            int dinnerId = getMealIdByName(connection, dinnerChoice);

            plans.add(new Plan(breakfastChoice, "breakfast", breakfastId, day));
            plans.add(new Plan(lunchChoice, "lunch", lunchId, day));
            plans.add(new Plan(dinnerChoice, "dinner", dinnerId, day));

            savePlan(connection, plans);
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

    private static String getMealOptionForDay(Connection connection, String day, String mealCategory) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT meal_option FROM plan WHERE meal_day = ? AND meal_category = ?"
        )){
            preparedStatement.setString(1, day);
            preparedStatement.setString(2, mealCategory);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("meal_option");
                }
            }
        }
        return "Not planned";
    }


    public static void printMeals(Connection connection, Scanner scanner) throws SQLException {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        String categoryChoice = scanner.nextLine();
        while (!isValidCategory(categoryChoice)) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            categoryChoice = scanner.nextLine();
        }
        mealsByCategory = getMealsByCategory(connection, categoryChoice);
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
            System.out.println("Breakfast: " + getMealOptionForDay(connection, day, "breakfast"));
            System.out.println("Lunch: " + getMealOptionForDay(connection, day, "lunch"));
            System.out.println("Dinner: " + getMealOptionForDay(connection, day, "dinner"));
            System.out.println();
        }
    }

    public static Meal addMeal(Connection connection, Scanner scanner) {

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

        try {
            int mealId;
            try (Statement statement = connection.createStatement()) {
                ResultSet mealIdResult = statement.executeQuery("SELECT nextval('meals_seq')");
                mealIdResult.next();
                mealId = mealIdResult.getInt(1);
            }
            PreparedStatement insertMealStatement = connection.prepareStatement(
                    "INSERT INTO meals (meal_id, category, meal) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            insertMealStatement.setInt(1, mealId);
            insertMealStatement.setString(2, category);
            insertMealStatement.setString(3, mealName);
            insertMealStatement.executeUpdate();

            int ingredientId;
            try (Statement statement = connection.createStatement()) {
                for (String ingredient : ingredients) {
                    ResultSet ingredientIdResult = statement.executeQuery("SELECT nextval('ingredients_seq')");
                    ingredientIdResult.next();
                    ingredientId = ingredientIdResult.getInt(1) + 1;


                    PreparedStatement insertIngredientsStatement = connection.prepareStatement(
                            "INSERT INTO ingredients (ingredient_id, ingredient, meal_id) VALUES (?, ?, ?)"
                    );

                    insertIngredientsStatement.setInt(1, ingredientId);
                    insertIngredientsStatement.setString(2, ingredient.trim());
                    insertIngredientsStatement.setInt(3, mealId);
                    insertIngredientsStatement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return new Meal(mealName, category, ingredients);
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