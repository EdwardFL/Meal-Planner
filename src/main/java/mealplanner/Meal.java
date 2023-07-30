package mealplanner;

public class Meal {
    private String name;
    private String category;
    private String[] ingredients;

    public Meal(String name, String category, String[] ingredients) {
        this.name = name;
        this.category = category;
        this.ingredients = ingredients;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String[] getIngredients() {
        return ingredients;
    }
}
