package mealplanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final String EXIT = "exit";
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        List<Meal> meals = new ArrayList<>();
        String userInput;

        while (true) {
            System.out.println("What would you like to do (add, show, exit)?");
            userInput = scanner.nextLine();
            switch (userInput) {
                case EXIT:
                    System.out.println("Bye!");
                    return;
                case "add":
                    Meal meal = addMeal(scanner);
                    meals.add(meal);
                    System.out.println("The meal has been added!");
                    break;
                case "show":
                    if(meals.isEmpty()) {
                        System.out.println("No meals saved. Add a meal first.");
                        break;
                    }
                    showMeals(meals);
                    break;
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

    public static Meal addMeal(Scanner scanner) {
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
            if (!ingredient.trim().matches("[a-zA-Z ]+")) {
                return false;
            }
        }
        return true;
    }
}