package Model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import Model.MatchResult;

public class TeamStats {
    private final String teamName;

    // NYE: Separate Hjemme- og Udestatistikker
    private int homeGamesPlayed = 0;
    private int homeGoalsScored = 0;
    private int homeGoalsConceded = 0;
    private int homePoints = 0;

    private int awayGamesPlayed = 0;
    private int awayGoalsScored = 0;
    private int awayGoalsConceded = 0;
    private int awayPoints = 0;

    // Samlede Statistikker (nu beregnet fra hjemme/ude)
    private int totalWins = 0;
    private int totalDraws = 0;
    private int totalLosses = 0;

    private LinkedList<MatchResult> recentMatches;
    private final int MAX_RECENT_MATCHES = 10;

    public TeamStats(String teamName) {
        this.teamName = teamName;
        this.recentMatches = new LinkedList<>();
    }

    // --- OPDATERINGSMETODER (Opdateret) ---

    public void updateForHomeMatch(MatchResult match) {
        this.homeGamesPlayed++;
        this.homeGoalsScored += match.getHomeGoals();
        this.homeGoalsConceded += match.getAwayGoals();

        if (match.getHomeGoals() > match.getAwayGoals()) {
            this.totalWins++;
            this.homePoints += 3;
        } else if (match.getHomeGoals() < match.getAwayGoals()) {
            this.totalLosses++;
        } else {
            this.totalDraws++;
            this.homePoints += 1;
        }
        addRecentMatch(match);
    }

    public void updateForAwayMatch(MatchResult match) {
        this.awayGamesPlayed++;
        this.awayGoalsScored += match.getAwayGoals();
        this.awayGoalsConceded += match.getHomeGoals();

        if (match.getAwayGoals() > match.getHomeGoals()) {
            this.totalWins++;
            this.awayPoints += 3;
        } else if (match.getAwayGoals() == match.getHomeGoals()) {
            this.totalDraws++;
            this.awayPoints += 1;
        } else {
            this.totalLosses++;
        }
        addRecentMatch(match);
    }

    // Hjælpe-metode: Tilføjer nyeste kampe først for nemmere vægtning
    private void addRecentMatch(MatchResult match) {
        // Tilføj den nyeste kamp først (index 0)
        recentMatches.addFirst(match);
        // Hold listen på en fast størrelse
        while (recentMatches.size() > MAX_RECENT_MATCHES) {
            recentMatches.removeLast();
        }
    }

    /** Mål scoret pr. hjemmekamp. */
    public double getHomeAttackRating() {
        return homeGamesPlayed > 0 ? (double) homeGoalsScored / homeGamesPlayed : 0.0;
    }

    /** Mål indkasseret pr. hjemmekamp. */
    public double getHomeDefenseRating() {
        return homeGamesPlayed > 0 ? (double) homeGoalsConceded / homeGamesPlayed : 0.0;
    }

    /** Mål scoret pr. udekamp. */
    public double getAwayAttackRating() {
        return awayGamesPlayed > 0 ? (double) awayGoalsScored / awayGamesPlayed : 0.0;
    }

    /** Mål indkasseret pr. udekamp. */
    public double getAwayDefenseRating() {
        return awayGamesPlayed > 0 ? (double) awayGoalsConceded / awayGamesPlayed : 0.0;
    }

    /** Returnerer det vægtede gennemsnit af formpoint for de seneste 5 kampe. */
    public double getRecentFormPoints() {
        final int RECENT_MATCH_COUNT = 5;

        // Da recentMatches er sorteret nyeste først, kan vi slice de første 5
        int numMatchesToConsider = Math.min(recentMatches.size(), RECENT_MATCH_COUNT);

        double weightedPoints = 0;
        int totalWeight = 0;

        for (int i = 0; i < numMatchesToConsider; i++) {
            int weight = RECENT_MATCH_COUNT - i;
            MatchResult match = recentMatches.get(i);
            int points = calculatePointsForMatch(match);

            weightedPoints += (points * weight);
            totalWeight += weight;
        }

        if (totalWeight == 0) {
            return 0.0;
        }

        return weightedPoints / totalWeight;
    }

    private int calculatePointsForMatch(MatchResult match) {
        int teamGoals, opponentGoals;

        if (teamName.equals(match.getHomeTeam())) {
            teamGoals = match.getHomeGoals();
            opponentGoals = match.getAwayGoals();
        } else if (teamName.equals(match.getAwayTeam())) {
            teamGoals = match.getAwayGoals();
            opponentGoals = match.getHomeGoals();
        } else {
            return 0;
        }

        if (teamGoals > opponentGoals) {
            return 3;
        } else if (teamGoals == opponentGoals) {
            return 1;
        } else {
            return 0;
        }
    }

    public String getTeamName() {
        return teamName;
    }

    public int getGamesPlayed() {
        return homeGamesPlayed + awayGamesPlayed;
    }

    public int getWins() {
        return totalWins;
    }

    public int getDraws() {
        return totalDraws;
    }

    public int getLosses() {
        return totalLosses;
    }

    public int getGoalsScored() {
        return homeGoalsScored + awayGoalsScored;
    }

    public int getGoalsConceded() {
        return homeGoalsConceded + awayGoalsConceded;
    }

    public int getGoalDifference() {
        return getGoalsScored() - getGoalsConceded();
    }

    public int getPoints() {
        return homePoints + awayPoints;
    }

    public void setPoints(int points) {
        this.homePoints = points;
        this.awayPoints = 0;
    }

    public void setGamesPlayed(int gamesPlayed) {
        this.homeGamesPlayed = gamesPlayed;
        this.awayGamesPlayed = 0;
    }

    public List<MatchResult> getRecentMatches() {
        return new ArrayList<>(recentMatches);
    }

    @Override
    public String toString() {
        return String.format("%-20s | Kampe: %2d | V: %2d | U: %2d | T: %2d | Mål: %2d-%2d | MålF: %3d | P: %2d",
                teamName, getGamesPlayed(), getWins(), getDraws(), getLosses(),
                getGoalsScored(), getGoalsConceded(), getGoalDifference(), getPoints());
    }
}