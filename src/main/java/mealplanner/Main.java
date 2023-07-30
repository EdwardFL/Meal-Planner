package mealplanner;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String EXIT = "exit";
    private static final String DB_URL = "jdbc:postgresql:meals_db";
    private static final String USER = "postgres";
    private static final String PASS = "1111";
    private static List<Meal> meals = new ArrayList<>();
    public static void main(String[] args) throws SQLException {

        try (Connection connection = DriverManager.getConnection(DB_URL, USER, PASS)) {
            createTables(connection);
            loadMealsData(connection);

            Scanner scanner = new Scanner(System.in);
            String userInput;

            while (true) {
                System.out.println("What would you like to do (add, show, exit)?");
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
                        if (meals.isEmpty()) {
                            System.out.println("No meals saved. Add a meal first.");
                            break;
                        }
                        showMeals(meals);
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
        }
    }
    private static void createSequences(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS meal_id_sequence START WITH 1 INCREMENT BY 1");
            statement.executeUpdate("CREATE SEQUENCE IF NOT EXISTS ingredient_id_sequence START WITH 1 INCREMENT BY 1");
        }
    }

    private static List<String> loadIngredientsForMeal(Connection connection, String selectIngredientsSQL, int mealId) throws SQLException {
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
    private static void loadMealsData(Connection connection) throws SQLException {
        String selectMealsSQL = "SELECT * FROM meals";
        String selectIngredientsSQL = "SELECT * FROM ingredients";

        try (Statement statement = connection.createStatement()) {
            ResultSet mealResultSet = statement.executeQuery(selectMealsSQL);

            while (mealResultSet.next()) {
                int mealId = mealResultSet.getInt("meal_id");
                String category = mealResultSet.getString("category");
                String name = mealResultSet.getString("meal");
                List<String> ingredients = loadIngredientsForMeal(connection, selectIngredientsSQL, mealId);

                Meal meal = new Meal(name, category, ingredients.toArray(new String[0]));
                meals.add(meal);
            }
        }
    }

    public static void showMeals(List<Meal> meals) {
        for (Meal meal : meals) {
            System.out.println("\nCategory: " + meal.getCategory());
            System.out.println("Name: " + meal.getName());
            System.out.println("Ingredients:");
            for (int i = 0; i < meal.getIngredients().length; i++) {
                System.out.println(meal.getIngredients()[i]);
            }
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
            ingredients = scanner.nextLine().split(",");
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
                    ingredientId = ingredientIdResult.getInt(1);


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