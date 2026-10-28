package Model;

// PredictedGoals.java
public class PredictedGoals {
    private final double homeExpectedGoals;
    private final double awayExpectedGoals;

    public PredictedGoals(double homeExpectedGoals, double awayExpectedGoals) {
        this.homeExpectedGoals = homeExpectedGoals;
        this.awayExpectedGoals = awayExpectedGoals;
    }

    public double getHomeExpectedGoals() {
        return homeExpectedGoals;
    }

    public double getAwayExpectedGoals() {
        return awayExpectedGoals;
    }
}