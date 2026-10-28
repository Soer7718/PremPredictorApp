package Model;

public class MatchResult {
    String homeTeam;
    String awayTeam;
    int homeGoals;
    int awayGoals;

    public MatchResult(String homeTeam, String awayTeam, int homeGoals, int awayGoals) {
        this.homeTeam = homeTeam;
        this.awayTeam = awayTeam;
        this.homeGoals = homeGoals;
        this.awayGoals = awayGoals;
    }

    // Getters for at få adgang til kampens detaljer
    public String getHomeTeam() {
        return homeTeam;
    }

    public String getAwayTeam() {
        return awayTeam;
    }

    public int getHomeGoals() {
        return homeGoals;
    }

    public int getAwayGoals() {
        return awayGoals;
    }

    @Override
    public String toString() {
        return homeTeam + " " + homeGoals + " - " + awayGoals + " " + awayTeam;
    }
}