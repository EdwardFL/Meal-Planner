package mealplanner;

public class Plan {
    private String mealOption;
    private String mealCategory;
    private int mealIdMeals;
    private String mealDay;

    public Plan(String mealOption, String mealCategory, int mealIdMeals, String mealDay) {
        this.mealOption = mealOption;
        this.mealCategory = mealCategory;
        this.mealIdMeals = mealIdMeals;
        this.mealDay = mealDay;
    }

    public String getMealOption() {
        return mealOption;
    }

    public String getMealCategory() {
        return mealCategory;
    }

    public int getMealIdMeals() {
        return mealIdMeals;
    }

    public String getMealDay() {
        return mealDay;
    }

    public void setMealDay(String mealDay) {
        this.mealDay = mealDay;
    }
}
