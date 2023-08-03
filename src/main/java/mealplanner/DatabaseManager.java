package mealplanner;

import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {

    private static final String DB_URL = "jdbc:postgresql:meals_db";
    private static final String USER = "postgres";
    private static final String PASS = "1111";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void createTables() {
        try (Connection connection = getConnection()) {
            createMealsTable(connection);
            createIngredientsTable(connection);
            createPlanTable(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void createMealsTable(Connection connection) throws SQLException{
        String createMealsTableSQL = "CREATE TABLE meals (" +
                        "meal_id SERIAL PRIMARY KEY," +
                        "category VARCHAR," +
                        "meal VARCHAR)";
        try (Statement statement = connection.createStatement()) {
            ResultSet mealsTable = connection.getMetaData().getTables(null, null, "meals", null);
            if (!mealsTable.next()) {
                statement.executeUpdate(createMealsTableSQL);
            }
        }
    }

    private static void createIngredientsTable(Connection connection) throws SQLException{
        String createIngredientsTableSQL = "CREATE TABLE ingredients (" +
                        "ingredient_id SERIAL PRIMARY KEY ," +
                        "ingredient VARCHAR," +
                        "meal_id INTEGER REFERENCES meals (meal_id))";
        try (Statement statement = connection.createStatement()) {
            ResultSet mealsTable = connection.getMetaData().getTables(null, null, "ingredients", null);
            if (!mealsTable.next()) {
                statement.executeUpdate(createIngredientsTableSQL);
            }
        }
    }

    private static void createPlanTable(Connection connection) throws SQLException{
        String createPlanTableSQL = "CREATE TABLE plan (" +
                        "plan_id SERIAL PRIMARY KEY," +
                        "meal_day VARCHAR," +
                        "meal_option VARCHAR," +
                        "meal_category VARCHAR," +
                        "meal_id_meals INTEGER REFERENCES meals (meal_id))";
        try (Statement statement = connection.createStatement()) {
            ResultSet mealsTable = connection.getMetaData().getTables(null, null, "plan", null);
            if (!mealsTable.next()) {
                statement.executeUpdate(createPlanTableSQL);
            }
        }
    }

    public static void addMeal(Connection connection, String category, String mealName, String[] ingredients) {
        String insertIntoMealsSQL = "INSERT INTO meals (meal_id, category, meal) VALUES (?, ?, ?)";
        try {
            PreparedStatement insertMealStatement = connection.prepareStatement(
                    insertIntoMealsSQL,
                    Statement.RETURN_GENERATED_KEYS
            );
            insertMealStatement.setString(1, category);
            insertMealStatement.setString(2, mealName);
            insertMealStatement.executeUpdate();

            int mealId;
            try (ResultSet generatedKeys = insertMealStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    mealId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get the auto-generated meal_id.");
                }
            }

            String insertIntoIngredientsSQL = "INSERT INTO ingredients (ingredient_id, ingredient, meal_id) VALUES (?, ?, ?)";
            PreparedStatement insertIngredientsStatement = connection.prepareStatement(
                    insertIntoIngredientsSQL,
                    Statement.RETURN_GENERATED_KEYS);

            for (String ingredient : ingredients) {
                insertIngredientsStatement.setString(1, ingredient.trim());
                insertIngredientsStatement.setInt(2, mealId);
                insertIngredientsStatement.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void savePlan(Connection connection, List<Plan> plans) throws SQLException {
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

    public static boolean isPlanSaved(Connection connection) throws SQLException {
        String checkPlanSQL = "SELECT COUNT(*) AS count FROM plan";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(checkPlanSQL)) {
            resultSet.next();
            int rowCount = resultSet.getInt("count");
            return rowCount > 0;
        }
    }

    public static List<Meal> getMeals(Connection connection) throws SQLException {
        String selectMealsSQL = "SELECT * FROM meals";
        List<Meal> meals = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {
            ResultSet mealResultSet = statement.executeQuery(selectMealsSQL);

            while (mealResultSet.next()) {
                int mealId = mealResultSet.getInt("meal_id");
                String category = mealResultSet.getString("category");
                String name = mealResultSet.getString("meal");
                List<String> ingredients = getIngredientsForMeal(connection, mealId);

                Meal meal = new Meal(name, category, ingredients.toArray(new String[0]));
                meals.add(meal);
            }
        }
        return meals;
    }

    private static List<String> getIngredientsForMeal(Connection connection, int mealId) throws SQLException {
        String selectIngredientsSQL = "SELECT * FROM ingredients";
        List<String> ingredients = new ArrayList<>();

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
    public static int getMealIdByName(Connection connection, String mealName) throws SQLException {
        String getMealIdByNameSQL = "SELECT meal_id FROM meals WHERE meal = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(getMealIdByNameSQL)) {
            preparedStatement.setString(1, mealName);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("meal_id");
                }
            }
        }
        throw new SQLException("Meal not found: " + mealName);
    }

    public static List<Meal> getMealsByCategory(Connection connection, String categoryChoice) throws SQLException {
        String selectMealsSQL = "SELECT * FROM meals WHERE category = ?";
        List<Meal> mealsList = new ArrayList<>();

        try (PreparedStatement preparedStatement = connection.prepareStatement(selectMealsSQL)) {
            preparedStatement.setString(1, categoryChoice);

            try (ResultSet mealResultSet = preparedStatement.executeQuery()) {

                while (mealResultSet.next()) {
                    int mealId = mealResultSet.getInt("meal_id");
                    String category = mealResultSet.getString("category");
                    String name = mealResultSet.getString("meal");
                    List<String> ingredients = getIngredientsForMeal(connection, mealId);

                    Meal meal = new Meal(name, category, ingredients.toArray(new String[0]));
                    mealsList.add(meal);
                }
            }
        }
        return mealsList;
    }

    public static void getShoppingListForWeek(Connection connection, PrintWriter printWriter) throws SQLException {
        String getShoppingListForWeekSQL = "SELECT ingredient, SUM(1) as total_count FROM ingredients " +
                "JOIN plan ON ingredients.meal_id = plan.meal_id_meals " +
                "GROUP BY ingredient";
        try (PreparedStatement preparedStatement = connection.prepareStatement(getShoppingListForWeekSQL)) {
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

    public static String getMealOptionForDay(Connection connection, String day, String mealCategory) throws SQLException {
        String getMealOptionForDaySQL = "SELECT meal_option FROM plan WHERE meal_day = ? AND meal_category = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(getMealOptionForDaySQL)){
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
}
