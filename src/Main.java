import java.util.Scanner;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import Model.TeamStats;
import Model.MatchResult;
import Model.PredictedGoals;

public class Main {

    private static final Map<String, String> TEAM_NAME_MAPPING = new HashMap<>();

    static {
        TEAM_NAME_MAPPING.put("Leeds", "Leeds United");
        TEAM_NAME_MAPPING.put("Nott'm Forest", "Nottingham Forest");
        TEAM_NAME_MAPPING.put("Bournemouth", "AFC Bournemouth");
        TEAM_NAME_MAPPING.put("Man City", "Manchester City");
        TEAM_NAME_MAPPING.put("Man United", "Manchester United");
        TEAM_NAME_MAPPING.put("Wolves", "Wolverhampton Wanderers");
        TEAM_NAME_MAPPING.put("Spurs", "Tottenham");
        TEAM_NAME_MAPPING.put("West Ham Utd", "West Ham");
        TEAM_NAME_MAPPING.put("Leicester", "Leicester City");
    }

    public static void main(String[] args) {
        String resultsCsvPath = "PremierLeagueResultater2025_2026.csv";
        String fixturesCsvPath = "PremierLeagueFixtures2025_2026.csv";

        List<MatchResult> playedResults = readCsvFile(resultsCsvPath);

        if (playedResults.isEmpty()) {
            System.out.println("Ingen resultater blev indlæst fra " + resultsCsvPath + ". Opretter mock data.");
            playedResults.add(new MatchResult("Manchester United", "Liverpool", 2, 1));
            playedResults.add(new MatchResult("Arsenal", "Tottenham", 3, 1));
        }

        System.out.println("Indlæste resultater:");
        for (MatchResult match : playedResults) {
            System.out.println(match);
        }
        System.out.println("\n--- Beregner holdstatistik ---");

        Map<String, TeamStats> teamStatistics = new HashMap<>();

        for (MatchResult match : playedResults) {
            TeamStats homeTeamStats = teamStatistics.computeIfAbsent(match.getHomeTeam(), TeamStats::new);
            homeTeamStats.updateForHomeMatch(match);

            TeamStats awayTeamStats = teamStatistics.computeIfAbsent(match.getAwayTeam(), TeamStats::new);
            awayTeamStats.updateForAwayMatch(match);
        }

        double[] leagueAverages = calculateLeagueAverages(playedResults);
        double leagueAvgHomeGoals = leagueAverages[0];
        double leagueAvgAwayGoals = leagueAverages[1];

        System.out.println(String.format("Liga gennemsnit: Hjemme %.2f, Ude %.2f",
                leagueAvgHomeGoals, leagueAvgAwayGoals));

        addPromotedTeams(teamStatistics);

        System.out.println("\n--- Holdstatistikker (sidste runde) ---");
        teamStatistics.values().stream()
                .sorted((t1, t2) -> {
                    int pointComparison = Integer.compare(t2.getPoints(), t1.getPoints());
                    if (pointComparison != 0) {
                        return pointComparison;
                    }
                    return Integer.compare(t2.getGoalDifference(), t1.getGoalDifference());
                })
                .forEach(System.out::println);

        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\n--- Vælg handling ---");
            System.out.println("1. Forudsig næste hele runde (fra fil)");
            System.out.println("2. Forudsig en enkelt kamp");
            System.out.println("3. Afslut programmet");
            System.out.print("Indtast dit valg (1, 2 eller 3): ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    predictFullRound(fixturesCsvPath, teamStatistics, leagueAvgHomeGoals, leagueAvgAwayGoals);
                    break;
                case "2":
                    predictSingleMatch(scanner, teamStatistics, leagueAvgHomeGoals, leagueAvgAwayGoals);
                    break;
                case "3":
                    System.out.println("Programmet afsluttes.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Ugyldigt valg. Vælg venligst 1, 2 eller 3.");
            }
        }
    }

    private static void addPromotedTeams(Map<String, TeamStats> teamStatistics) {
        final int PROMOTED_TEAM_POINTS = 5;
        String[] promotedTeams = {"Burnley", "Leeds United", "Nottingham Forest", "Wolverhampton Wanderers"};

        for (String teamName : promotedTeams) {
            teamStatistics.computeIfAbsent(teamName, name -> {
                TeamStats newTeam = new TeamStats(name);
                newTeam.setGamesPlayed(1);
                newTeam.setPoints(PROMOTED_TEAM_POINTS);
                return newTeam;
            });
        }
    }

    private static double[] calculateLeagueAverages(List<MatchResult> playedResults) {
        if (playedResults.isEmpty()) {
            return new double[]{1.5, 1.2};
        }

        int totalHomeGoals = 0;
        int totalAwayGoals = 0;
        int numMatches = playedResults.size();

        for (MatchResult match : playedResults) {
            totalHomeGoals += match.getHomeGoals();
            totalAwayGoals += match.getAwayGoals();
        }

        double avgHome = (double) totalHomeGoals / numMatches;
        double avgAway = (double) totalAwayGoals / numMatches;

        return new double[]{avgHome, avgAway};
    }

    public static List<MatchResult> readFixtures(String filePath) {
        List<MatchResult> fixtures = new ArrayList<>();
        String line;
        boolean isFirstLine = true;
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length >= 2) {
                    // RETTET: Bruger index 0 og 1, da fixtures-filen kun har to kolonner
                    String homeTeam = standardizeTeamName(data[0].trim());
                    String awayTeam = standardizeTeamName(data[1].trim());
                    fixtures.add(new MatchResult(homeTeam, awayTeam, 0, 0));
                } else {
                    System.err.println("Ugyldigt format for kommende kamp: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Fejl ved læsning af kampprogramfil: " + filePath);
            e.printStackTrace();
            return new ArrayList<>();
        }
        return fixtures;
    }

    private static void predictFullRound(String fixturesCsvPath, Map<String, TeamStats> teamStatistics,
                                         double leagueAvgHomeGoals, double leagueAvgAwayGoals) {
        System.out.println("\n--- Forudsiger kommende kampe fra " + fixturesCsvPath + " ---");
        List<MatchResult> upcomingFixtures = readFixtures(fixturesCsvPath);

        if (upcomingFixtures.isEmpty()) {
            System.out.println("Ingen kampe at forudsige. Tjek filen for kampprogrammet.");
            return;
        }

        for (MatchResult fixture : upcomingFixtures) {
            System.out.println("\nForudsigelse for " + fixture.getHomeTeam() + " vs " + fixture.getAwayTeam() + ":");
            String prediction = predictMatch(fixture.getHomeTeam(), fixture.getAwayTeam(), teamStatistics, leagueAvgHomeGoals, leagueAvgAwayGoals);
            System.out.println("Resultat: " + prediction);
        }
    }

    private static void predictSingleMatch(Scanner scanner, Map<String, TeamStats> teamStatistics,
                                           double leagueAvgHomeGoals, double leagueAvgAwayGoals) {
        System.out.println("\n--- Forudsige en enkelt kamp ---");
        System.out.print("Indtast hjemmeholdets navn: ");
        String homeTeamInput = scanner.nextLine().trim();
        System.out.print("Indtast udeholdets navn: ");
        String awayTeamInput = scanner.nextLine().trim();

        homeTeamInput = standardizeTeamName(homeTeamInput);
        awayTeamInput = standardizeTeamName(awayTeamInput);

        String prediction = predictMatch(homeTeamInput, awayTeamInput, teamStatistics, leagueAvgHomeGoals, leagueAvgAwayGoals);
        System.out.println("Forudsigelse: " + prediction);
        System.out.println("---------------------------------");
    }

    public static List<MatchResult> readCsvFile(String filePath) {
        List<MatchResult> matchResults = new ArrayList<>();
        String line;
        boolean isFirstLine = true;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }
                String[] data = line.split(",");
                if (data.length >= 7) {
                    try {
                        String homeTeam = standardizeTeamName(data[3].trim());
                        String awayTeam = standardizeTeamName(data[4].trim());
                        int homeGoals = Integer.parseInt(data[5].trim());
                        int awayGoals = Integer.parseInt(data[6].trim());
                        matchResults.add(new MatchResult(homeTeam, awayTeam, homeGoals, awayGoals));
                    } catch (NumberFormatException e) {
                        System.err.println("Fejl ved parsing af tal i linje: " + line + ". Springes over.");
                    }
                } else {
                    System.err.println("Ugyldigt format i linje: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Fejl ved læsning af filen: " + filePath);
            return new ArrayList<>();
        }
        return matchResults;
    }

    private static String standardizeTeamName(String teamName) {
        return TEAM_NAME_MAPPING.getOrDefault(teamName, teamName);
    }

    public static PredictedGoals predictGoals(TeamStats homeStats, TeamStats awayStats,
                                              double leagueAvgHomeGoals, double leagueAvgAwayGoals) {

        if (leagueAvgHomeGoals == 0 || leagueAvgAwayGoals == 0) {
            return new PredictedGoals(0, 0);
        }

        double homeAttackStrength = homeStats.getHomeAttackRating() / leagueAvgHomeGoals;
        double awayDefenseCoefficient = awayStats.getAwayDefenseRating() / leagueAvgHomeGoals;

        if (awayDefenseCoefficient < 0.5) {
            awayDefenseCoefficient = 0.5;
        }

        double homeExpectedGoals = homeAttackStrength * awayDefenseCoefficient * leagueAvgHomeGoals;

        double awayAttackStrength = awayStats.getAwayAttackRating() / leagueAvgAwayGoals;
        double homeDefenseCoefficient = homeStats.getHomeDefenseRating() / leagueAvgAwayGoals;

        if (homeDefenseCoefficient > 1.5) {
            homeDefenseCoefficient = 1.5;
        }

        double awayExpectedGoals = awayAttackStrength * homeDefenseCoefficient * leagueAvgAwayGoals;

        return new PredictedGoals(homeExpectedGoals, awayExpectedGoals);
    }

    public static String predictMatch(String homeTeamName, String awayTeamName,
                                      Map<String, TeamStats> teamStatistics,
                                      double leagueAvgHomeGoals, double leagueAvgAwayGoals) {

        TeamStats homeStats = teamStatistics.get(homeTeamName);
        TeamStats awayStats = teamStatistics.get(awayTeamName);

        if (homeStats == null || awayStats == null) {
            return "Kan ikke forudsige: Et af holdene ('" + homeTeamName + "' eller '" + awayTeamName + "') har ingen statistik.";
        }

        PredictedGoals goals = predictGoals(homeStats, awayStats, leagueAvgHomeGoals, leagueAvgAwayGoals);
        double homeXG = goals.getHomeExpectedGoals();
        double awayXG = goals.getAwayExpectedGoals();

        final double XG_WEIGHT = 10.0;
        final double POINTS_WEIGHT = 0.4;
        final double FORM_WEIGHT = 5.5;

        double xgDifference = homeXG - awayXG;

        double homeStrength = (xgDifference * XG_WEIGHT)
                + (homeStats.getPoints() * POINTS_WEIGHT)
                + (homeStats.getRecentFormPoints() * FORM_WEIGHT);

        double awayStrength = (-xgDifference * XG_WEIGHT)
                + (awayStats.getPoints() * POINTS_WEIGHT)
                + (awayStats.getRecentFormPoints() * FORM_WEIGHT);

        System.out.println("\n--- Forudsigelsesgrundlag for " + homeTeamName + " vs " + awayTeamName + " ---");
        System.out.println(String.format("Forventet mål: %.2f - %.2f", homeXG, awayXG));
        System.out.println(homeTeamName + " Styrke: " + String.format("%.2f", homeStrength));
        System.out.println(awayTeamName + " Styrke: " + String.format("%.2f", awayStrength));

        final double TIE_THRESHOLD = 5.0;
        double strengthDifference = Math.abs(homeStrength - awayStrength);

        if (strengthDifference < TIE_THRESHOLD) {
            return "Det er en meget tæt kamp, sandsynligvis uafgjort.";
        } else if (homeStrength > awayStrength) {
            return homeTeamName + " vinder kampen.";
        } else {
            return awayTeamName + " vinder kampen.";
        }
    }
}